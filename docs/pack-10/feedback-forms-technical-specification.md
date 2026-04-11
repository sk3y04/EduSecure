# Feedback Forms Technical Specification

## 1. Feature goal

Introduce a reusable structured feedback-form workflow tied to the implemented exam domain so EduSecure can support staff-managed feedback collection beyond free-text grading comments.

The initial delivery scope is intentionally narrow:
- lecturers create and update feedback forms only for exams they can manage
- admins create and update feedback forms for any exam
- each form contains structured questions rather than one generic comment box
- students can submit one response per published form if they currently belong to the exam's space
- staff can review submitted responses and simple aggregate summaries for rating questions
- feedback-form publication is separate from exam-result publication and does not expose other students' responses

This feature inherits the current cookie-backed session model, exam ownership model, and space-membership visibility boundary.

## 2. Problem analysis

EduSecure now has exam scheduling and exam results, but it still lacks a reusable feedback mechanism. Existing grade feedback is free-text and tied to one submission or result record, which is not equivalent to a structured form that can collect the same questions across a cohort.

The smallest coherent extension is to attach feedback forms to exams. That choice reuses:
- the existing exam ownership rule for lecturer and admin management
- the existing student membership rule for who can submit a response
- the existing audit service for staff actions
- the current typed REST and page-based frontend pattern

This first feedback-form slice does not attempt to solve:
- anonymous surveys
- reusable cross-exam template libraries
- question branching or conditional logic
- export to CSV or spreadsheet tools
- moderation workflows for deleting or hiding submitted responses

## 3. Roles and permissions

### 3.1 Role definitions

- `STUDENT`: view published forms for accessible exams and submit one own response per form
- `LECTURER`: create, update, list, and review feedback forms for manageable exams
- `ADMIN`: create, update, list, and review feedback forms for any exam

### 3.2 Permission matrix

| Action | ADMIN | LECTURER | STUDENT |
|---|---|---|---|
| Create form | Any exam | Managed exams only | No |
| Update form | Any form | Managed exam forms only | No |
| List forms for exam | Any exam | Managed exams only | Published forms for accessible exams only |
| Read form by id | Any form | Managed exam forms only | Published form for accessible exam only |
| Submit response | No | No | One own response for published accessible form |
| Review responses and summary | Any form | Managed form only | No |

### 3.3 Authorization rules

- Students never create or update forms.
- Lecturers can manage forms only where the linked exam belongs to a space they own.
- Admins can manage forms globally.
- Students can submit only if the form is published, the exam is published, and they currently belong to the exam's space.
- Each student may submit at most one response per form.

## 4. Domain model

### 4.1 FeedbackForm entity

Purpose: a structured questionnaire attached to one exam.

Fields:
- `id: UUID`
- `examId: UUID` required
- `title: String` required, trimmed, 3 to 160 chars
- `description: String` optional, trimmed, 0 to 2000 chars
- `published: boolean` required
- `createdByUserId: UUID` required
- `createdAt: Instant` required
- `updatedAt: Instant` required

### 4.2 FeedbackFormQuestion entity

Purpose: one prompt inside a feedback form.

Fields:
- `id: UUID`
- `formId: UUID` required
- `prompt: String` required, trimmed, 3 to 300 chars
- `questionType: FeedbackQuestionType` required
- `required: boolean` required
- `displayOrder: Integer` required

Supported question types for this delivery:
- `RATING`: integer answer in range `1..5`
- `TEXT`: free-text answer up to 2000 chars

### 4.3 FeedbackFormSubmission entity

Purpose: one student's response envelope for one form.

Fields:
- `id: UUID`
- `formId: UUID` required
- `studentUserId: UUID` required
- `submittedAt: Instant` required

Constraint:
- only one submission may exist for `(formId, studentUserId)`

### 4.4 FeedbackFormAnswer entity

Purpose: one answer for one question inside one submission.

Fields:
- `id: UUID`
- `submissionId: UUID` required
- `questionId: UUID` required
- `ratingValue: Integer` optional, used only for `RATING`
- `textValue: String` optional, used only for `TEXT`

### 4.5 Read model expectations

Staff form response must expose:
- form metadata
- ordered question list
- response count

Student form response must expose:
- form metadata
- ordered question list
- `alreadySubmitted`

Staff review response must expose:
- per-question summaries for rating questions including average and counts
- full submitted responses with student identity and submitted timestamp

## 5. Backend design

### 5.1 Packages

Extend the existing `exam` feature package with:
- `entity.exam`
- `repository.exam`
- `dto.exam`
- `service.exam`
- `controller.exam`

### 5.2 Persistence

Liquibase change sets add:
- `feedback_forms`
- `feedback_form_questions`
- `feedback_form_submissions`
- `feedback_form_answers`
- foreign keys to `exams`, `users`, form, question, and submission tables
- a unique constraint on `(form_id, student_user_id)` in submissions
- indexes for form-by-exam reads and response-by-form reads

### 5.3 Service responsibilities

`FeedbackFormService` must handle:
- creating forms with ordered questions
- replacing form questions on update
- listing forms for an exam with role-aware visibility
- retrieving one form with visibility rules
- accepting one student submission with validation against question types
- rejecting duplicate submissions
- reviewing responses and computing rating summaries for staff
- writing audit entries for create and update actions

### 5.4 Validation rules

- form title is required and limited to 160 characters after trim
- description is optional and limited to 2000 characters after trim
- a form must contain between 1 and 10 questions
- question prompt is required and limited to 300 characters
- `displayOrder` must be unique within the form payload
- `RATING` answers must be integers in `1..5`
- `TEXT` answers must be trimmed and up to 2000 characters
- published forms cannot receive structural updates after the first student submission exists
- students cannot submit if the form is unpublished, the exam is unpublished, or membership is gone

### 5.5 Audit requirements

Add audit actions:
- `FEEDBACK_FORM_CREATED`
- `FEEDBACK_FORM_UPDATED`

Minimum audit detail payloads:
- create: `examId=<UUID>,questionCount=<INT>,published=<BOOLEAN>`
- update: `formId=<UUID>,questionCount=<INT>,published=<BOOLEAN>`

Audit payloads must avoid storing student responses or free-text answers.

## 6. API design

All endpoints require authentication.

### 6.1 Create feedback form

`POST /api/exams/{examId}/feedback-forms`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "title": "Exam experience feedback",
  "description": "Help us improve the assessment process.",
  "published": true,
  "questions": [
    {
      "prompt": "How clear were the exam instructions?",
      "questionType": "RATING",
      "required": true,
      "displayOrder": 1
    },
    {
      "prompt": "What could be improved for future cohorts?",
      "questionType": "TEXT",
      "required": false,
      "displayOrder": 2
    }
  ]
}
```

### 6.2 List forms for exam

`GET /api/exams/{examId}/feedback-forms`

Allowed roles:
- staff for managed exam
- students for published accessible exam

### 6.3 Update feedback form

`PUT /api/feedback-forms/{formId}`

Allowed roles:
- `LECTURER`
- `ADMIN`

### 6.4 Read form by id

`GET /api/feedback-forms/{formId}`

Allowed roles:
- staff for managed form
- students for published accessible form

### 6.5 Submit response

`POST /api/feedback-forms/{formId}/responses`

Allowed roles:
- `STUDENT`

Request body contains ordered question answers by `questionId`.

### 6.6 Review responses and summaries

`GET /api/feedback-forms/{formId}/responses`

Allowed roles:
- `LECTURER`
- `ADMIN`

## 7. Frontend design

### 7.1 Route and navigation

Add a top-level authenticated route:
- `/feedback-forms`

Add a navigation item in the main app shell:
- visible to all authenticated users

### 7.2 Screen behavior

Single page `FeedbackForms` should:
- show staff a form-management workspace bound to a selected exam
- allow staff to create and update forms with ordered questions
- show staff response summaries and submitted answers for the selected form
- show students a list of visible published forms
- allow students to submit one structured response for a selected form
- clearly indicate whether a student has already submitted

### 7.3 Frontend API client

Add `feedbackFormsService` with:
- `listForExam(examId)`
- `create(examId, payload)`
- `update(formId, payload)`
- `getById(formId)`
- `submitResponse(formId, payload)`
- `listResponses(formId)`

Add feedback-form types in `frontend/src/types/feedbackForm.ts`.

## 8. Test design and acceptance criteria

### 8.1 Backend integration tests

Add a feedback-form flow integration test class covering at minimum:
- lecturer creates and updates a form for a managed exam
- student can list and submit one published accessible form
- duplicate student submission is rejected with `409 Conflict`
- student cannot submit unpublished or inaccessible form
- staff can review summaries and responses
- unrelated lecturer cannot manage another lecturer's form
- audit rows exist for create and update actions

### 8.2 Acceptance criteria

The feature is acceptable when:
- staff can define a structured form with rating and text questions
- students can submit one response to a published accessible form
- staff can review both individual responses and simple rating summaries
- duplicate responses are rejected
- unauthorized reads and writes are denied
- the frontend exposes one working feedback-form page and backend tests pass

## 9. Out-of-scope for this delivery

- anonymous surveys
- reusable template libraries
- branching questionnaires
- response export
- moderation or deletion workflows for submitted responses# Feedback Forms Technical Specification

## 1. Feature goal

Introduce a reusable structured feedback-form workflow tied to the implemented exam domain so EduSecure can support staff-managed feedback collection beyond free-text grading comments.

The initial delivery scope is intentionally narrow:
- lecturers create and update feedback forms only for exams they can manage
- admins create and update feedback forms for any exam
- each form contains structured questions rather than one generic comment box
- students can submit one response per published form if they currently belong to the exam's space
- staff can review submitted responses and simple aggregate summaries for rating questions
- feedback-form publication is separate from exam-result publication and does not expose other students' responses

This feature inherits the current cookie-backed session model, exam ownership model, and space-membership visibility boundary.

## 2. Problem analysis

EduSecure now has exam scheduling and exam results, but it still lacks a reusable feedback mechanism. Existing grade feedback is free-text and tied to one submission or result record, which is not equivalent to a structured form that can collect the same questions across a cohort.

The smallest coherent extension is to attach feedback forms to exams. That choice reuses:
- the existing exam ownership rule for lecturer and admin management
- the existing student membership rule for who can submit a response
- the existing audit service for staff actions
- the current typed REST and page-based frontend pattern

This first feedback-form slice does not attempt to solve:
- anonymous surveys
- reusable cross-exam template libraries
- question branching or conditional logic
- export to CSV or spreadsheet tools
- moderation workflows for deleting or hiding submitted responses

## 3. Roles and permissions

### 3.1 Role definitions

- `STUDENT`: view published forms for accessible exams and submit one own response per form
- `LECTURER`: create, update, list, and review feedback forms for manageable exams
- `ADMIN`: create, update, list, and review feedback forms for any exam

### 3.2 Permission matrix

| Action | ADMIN | LECTURER | STUDENT |
|---|---|---|---|
| Create form | Any exam | Managed exams only | No |
| Update form | Any form | Managed exam forms only | No |
| List forms for exam | Any exam | Managed exams only | Published forms for accessible exams only |
| Read form by id | Any form | Managed exam forms only | Published form for accessible exam only |
| Submit response | No | No | One own response for published accessible form |
| Review responses and summary | Any form | Managed exam forms only | No |

### 3.3 Authorization rules

- Students never create or update forms.
- Lecturers can manage forms only where the linked exam belongs to a space they own.
- Admins can manage forms globally.
- Students can submit only if the form is published, the exam is published, and they currently belong to the exam's space.
- Each student may submit at most one response per form.

## 4. Domain model

### 4.1 FeedbackForm entity

Purpose: a structured questionnaire attached to one exam.

Fields:
- `id: UUID`
- `examId: UUID` required
- `title: String` required, trimmed, 3 to 160 chars
- `description: String` optional, trimmed, 0 to 2000 chars
- `published: boolean` required
- `createdByUserId: UUID` required
- `createdAt: Instant` required
- `updatedAt: Instant` required

### 4.2 FeedbackFormQuestion entity

Purpose: one prompt inside a feedback form.

Fields:
- `id: UUID`
- `formId: UUID` required
- `prompt: String` required, trimmed, 3 to 300 chars
- `questionType: FeedbackQuestionType` required
- `required: boolean` required
- `displayOrder: Integer` required

Supported question types for this delivery:
- `RATING`: integer answer in range `1..5`
- `TEXT`: free-text answer up to 2000 chars

### 4.3 FeedbackFormSubmission entity

Purpose: one student's response envelope for one form.

Fields:
- `id: UUID`
- `formId: UUID` required
- `studentUserId: UUID` required
- `submittedAt: Instant` required

Constraint:
- only one submission may exist for `(formId, studentUserId)`

### 4.4 FeedbackFormAnswer entity

Purpose: one answer for one question inside one submission.

Fields:
- `id: UUID`
- `submissionId: UUID` required
- `questionId: UUID` required
- `ratingValue: Integer` optional, used only for `RATING`
- `textValue: String` optional, used only for `TEXT`

### 4.5 Read model expectations

Staff form response must expose:
- form metadata
- ordered question list
- response count

Student form response must expose:
- form metadata
- ordered question list
- `alreadySubmitted`

Staff review response must expose:
- per-question summaries for rating questions including average and counts
- full submitted responses with student identity and submitted timestamp

## 5. Backend design

### 5.1 Packages

Extend the existing `exam` feature package with:
- `entity.exam`
- `repository.exam`
- `dto.exam`
- `service.exam`
- `controller.exam`

### 5.2 Persistence

Liquibase change sets add:
- `feedback_forms`
- `feedback_form_questions`
- `feedback_form_submissions`
- `feedback_form_answers`
- foreign keys to `exams`, `users`, form, question, and submission tables
- a unique constraint on `(form_id, student_user_id)` in submissions
- indexes for form-by-exam reads and response-by-form reads

### 5.3 Service responsibilities

`FeedbackFormService` must handle:
- creating forms with ordered questions
- replacing form questions on update
- listing forms for an exam with role-aware visibility
- retrieving one form with visibility rules
- accepting one student submission with validation against question types
- rejecting duplicate submissions
- reviewing responses and computing rating summaries for staff
- writing audit entries for create and update actions

### 5.4 Validation rules

- form title is required and limited to 160 characters after trim
- description is optional and limited to 2000 characters after trim
- a form must contain between 1 and 10 questions
- question prompt is required and limited to 300 characters
- `displayOrder` must be unique within the form payload
- `RATING` answers must be integers in `1..5`
- `TEXT` answers must be trimmed and up to 2000 characters
- published forms cannot receive structural updates after the first student submission exists
- students cannot submit if the form is unpublished, the exam is unpublished, or membership is gone

### 5.5 Audit requirements

Add audit actions:
- `FEEDBACK_FORM_CREATED`
- `FEEDBACK_FORM_UPDATED`

Minimum audit detail payloads:
- create: `examId=<UUID>,questionCount=<INT>,published=<BOOLEAN>`
- update: `formId=<UUID>,questionCount=<INT>,published=<BOOLEAN>`

Audit payloads must avoid storing student responses or free-text answers.

## 6. API design

All endpoints require authentication.

### 6.1 Create feedback form

`POST /api/exams/{examId}/feedback-forms`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "title": "Exam experience feedback",
  "description": "Help us improve the assessment process.",
  "published": true,
  "questions": [
    {
      "prompt": "How clear were the exam instructions?",
      "questionType": "RATING",
      "required": true,
      "displayOrder": 1
    },
    {
      "prompt": "What could be improved for future cohorts?",
      "questionType": "TEXT",
      "required": false,
      "displayOrder": 2
    }
  ]
}
```

### 6.2 List forms for exam

`GET /api/exams/{examId}/feedback-forms`

Allowed roles:
- staff for managed exam
- students for published accessible exam

### 6.3 Update feedback form

`PUT /api/feedback-forms/{formId}`

Allowed roles:
- `LECTURER`
- `ADMIN`

### 6.4 Read form by id

`GET /api/feedback-forms/{formId}`

Allowed roles:
- staff for managed form
- students for published accessible form

### 6.5 Submit response

`POST /api/feedback-forms/{formId}/responses`

Allowed roles:
- `STUDENT`

Request body contains ordered question answers by `questionId`.

### 6.6 Review responses and summaries

`GET /api/feedback-forms/{formId}/responses`

Allowed roles:
- `LECTURER`
- `ADMIN`

## 7. Frontend design

### 7.1 Route and navigation

Add a top-level authenticated route:
- `/feedback-forms`

Add a navigation item in the main app shell:
- visible to all authenticated users

### 7.2 Screen behavior

Single page `FeedbackForms` should:
- show staff a form-management workspace bound to a selected exam
- allow staff to create and update forms with ordered questions
- show staff response summaries and submitted answers for the selected form
- show students a list of visible published forms
- allow students to submit one structured response for a selected form
- clearly indicate whether a student has already submitted

### 7.3 Frontend API client

Add `feedbackFormsService` with:
- `listForExam(examId)`
- `create(examId, payload)`
- `update(formId, payload)`
- `getById(formId)`
- `submitResponse(formId, payload)`
- `listResponses(formId)`

Add feedback-form types in `frontend/src/types/feedbackForm.ts`.

## 8. Test design and acceptance criteria

### 8.1 Backend integration tests

Add a feedback-form flow integration test class covering at minimum:
- lecturer creates and updates a form for a managed exam
- student can list and submit one published accessible form
- duplicate student submission is rejected with `409 Conflict`
- student cannot submit unpublished or inaccessible form
- staff can review summaries and responses
- unrelated lecturer cannot manage another lecturer's form
- audit rows exist for create and update actions

### 8.2 Acceptance criteria

The feature is acceptable when:
- staff can define a structured form with rating and text questions
- students can submit one response to a published accessible form
- staff can review both individual responses and simple rating summaries
- duplicate responses are rejected
- unauthorized reads and writes are denied
- the frontend exposes one working feedback-form page and backend tests pass

## 9. Out-of-scope for this delivery

- anonymous surveys
- reusable template libraries
- branching questionnaires
- response export
- moderation or deletion workflows for submitted responses