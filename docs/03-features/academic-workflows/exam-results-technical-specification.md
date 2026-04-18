# Exam Results Technical Specification

## 1. Feature goal

Introduce a separate exam-results domain that sits on top of the implemented exam timetable and gives EduSecure a distinct exam-assessment record beyond coursework submission grades.

The initial delivery scope is intentionally narrow:
- lecturers create and update exam results only for exams they can manage
- admins create and update exam results for any exam
- each exam can hold one result per student
- staff can mark results as published or unpublished
- students can view only their own published exam results
- student result visibility remains bounded by current space membership and exam visibility
- result publication is separate from timetable publication so staff can prepare results before release

This feature inherits the existing cookie-backed session model, role model, and the current space or exam access boundaries.

## 2. Problem analysis

The repository now has an exam scheduling layer but still lacks a separate result record for exams. That leaves a gap against the brief because assignment grades are not a substitute for formal exam outcomes.

The smallest technically honest extension is to add a new `ExamResult` entity linked to `Exam`. That choice reuses:
- the existing exam ownership and admin override logic
- the current space membership boundary for student reads
- the audit service already used for grade-sensitive actions
- the frontend pattern of a dedicated authenticated page backed by a typed REST client

This first exam-results slice does not attempt to solve:
- moderation boards or second marking
- transcript generation
- GPA aggregation
- bulk CSV import or export
- appeals workflow

## 3. Roles and permissions

### 3.1 Role definitions

- `STUDENT`: read only own published exam results
- `LECTURER`: create, update, and list exam results for manageable exams
- `ADMIN`: create, update, and list exam results for any exam

### 3.2 Permission matrix

| Action | ADMIN | LECTURER | STUDENT |
|---|---|---|---|
| Create result | Any exam | Managed exams only | No |
| Update result | Any result | Managed exam results only | No |
| List results for an exam | Any exam | Managed exams only | No |
| Read result by id | Any result | Managed exam results only | Own published result only |
| List own results | No | No | Yes |

### 3.3 Authorization rules

- Students never create or update exam results.
- Lecturers can manage exam results only for exams in spaces they own.
- Admins can manage exam results globally.
- Students can read only results where `studentUserId == currentUser.id`.
- Student visibility additionally requires:
  - `examResult.published == true`
  - `exam.published == true`
  - current space membership for the exam's space

## 4. Domain model

### 4.1 ExamResult entity

Purpose: an assessment outcome for one student on one exam.

Fields:
- `id: UUID`
- `examId: UUID` required
- `studentUserId: UUID` required
- `value: Integer` required, range `0..100`
- `feedback: String` optional, trimmed, 0 to 2000 chars
- `published: boolean` required
- `gradedByUserId: UUID` required
- `gradedAt: Instant` required
- `lastModifiedAt: Instant` optional
- `publishedAt: Instant` optional

Constraints:
- only one result may exist for a given `(examId, studentUserId)` pair
- the target student must currently belong to the exam's space at create time
- `publishedAt` is null while unpublished
- when an update changes `published` from `false` to `true`, `publishedAt` is set to now
- when an update changes `published` from `true` to `false`, `publishedAt` is cleared

### 4.2 Read model expectations

Staff exam-result response must expose:
- `id`
- `examId`
- `examTitle`
- `spaceId`
- `spaceCode`
- `spaceName`
- `studentUserId`
- `studentEmail`
- `studentFullName`
- `value`
- `feedback`
- `published`
- `gradedByUserId`
- `gradedAt`
- `lastModifiedAt`
- `publishedAt`

Student exam-result response must expose:
- `id`
- `examId`
- `examTitle`
- `spaceCode`
- `spaceName`
- `value`
- `feedback`
- `publishedAt`
- `lastModifiedAt`

## 5. Backend design

### 5.1 Packages

Add new feature packages following current conventions:
- `entity.exam`
- `repository.exam`
- `dto.exam`
- `service.exam`
- `controller.exam`

This feature extends the existing exam package rather than creating a parallel top-level module.

### 5.2 Persistence

Liquibase change set adds:
- `exam_results`
- foreign keys to `exams`, `users(student_user_id)`, and `users(graded_by_user_id)`
- a unique constraint on `(exam_id, student_user_id)`
- an index on `(exam_id, published)` for staff exam-result lists
- an index on `(student_user_id, published)` for student own-result reads

### 5.3 Service responsibilities

`ExamResultService` must handle:
- creating results for managed exams
- preventing duplicates per student per exam
- validating target student membership in the exam's space
- listing results for one exam to staff
- retrieving a result by id for staff or the owning student under publication rules
- listing own published results for a student
- updating value, feedback, and publication state
- writing audit entries for create and update actions

### 5.4 Validation rules

- `studentEmail` is required for result creation
- `value` must stay inside `0..100`
- `feedback` is optional and limited to 2000 characters after trim
- result creation fails with `409 Conflict` on duplicate `(examId, studentUserId)`
- result creation fails with `422 Unprocessable Content` when the user is not a student or not a current member of the exam's space
- student reads fail with `403 Forbidden` if the result is unpublished, the exam is unpublished, or current membership is gone

### 5.5 Audit requirements

Add audit actions:
- `EXAM_RESULT_CREATED`
- `EXAM_RESULT_UPDATED`

Minimum audit detail payloads:
- create: `examId=<UUID>,studentUserId=<UUID>,value=<INT>,published=<BOOLEAN>`
- update: `examId=<UUID>,studentUserId=<UUID>,value=<INT>,published=<BOOLEAN>`

Audit payloads must avoid storing free-text feedback.

## 6. API design

All endpoints require authentication.

### 6.1 Create exam result

`POST /api/exams/{examId}/results`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "studentEmail": "student@example.com",
  "value": 78,
  "feedback": "Strong cryptography reasoning with minor notation errors.",
  "published": false
}
```

Response `201 Created` returns the staff result response.

### 6.2 List results for an exam

`GET /api/exams/{examId}/results`

Allowed roles:
- `LECTURER`
- `ADMIN`

### 6.3 Update exam result

`PUT /api/exam-results/{examResultId}`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "value": 82,
  "feedback": "Revised after moderation review.",
  "published": true
}
```

### 6.4 Staff read by id

`GET /api/exam-results/{examResultId}`

Allowed roles:
- `LECTURER`
- `ADMIN`

### 6.5 Student own results list

`GET /api/my/exam-results`

Allowed roles:
- `STUDENT`

Returns published own results only.

### 6.6 Student own result by exam

`GET /api/my/exams/{examId}/result`

Allowed roles:
- `STUDENT`

## 7. Frontend design

### 7.1 Route and navigation

Add a top-level authenticated route:
- `/exam-results`

Add a navigation item in the main app shell:
- visible to all authenticated users

### 7.2 Screen behavior

Single page `ExamResults` should:
- show students a list of their published results
- show staff a result-management form tied to a selected exam
- show staff current results for the selected exam
- allow staff to create new results by student email
- allow staff to update existing results inline
- show clear publication status badges

### 7.3 Frontend API client

Add `examResultsService` with:
- `listForExam(examId)`
- `create(examId, payload)`
- `update(examResultId, payload)`
- `listMine()`

Add exam-result types in `frontend/src/types/examResult.ts`.

## 8. Test design and acceptance criteria

### 8.1 Backend integration tests

Add an exam-result flow integration test class covering at minimum:
- lecturer creates and updates an exam result for a managed exam
- duplicate result creation for the same student and exam fails with `409 Conflict`
- student can see only own published result
- student cannot see unpublished result
- student loses access after space membership removal
- unrelated lecturer cannot manage another lecturer's exam results
- audit rows exist for create and update actions

### 8.2 Acceptance criteria

The feature is acceptable when:
- staff can create one result per student per exam
- students can read only their own published results
- staff can update result value, feedback, and publication state
- duplicate results are rejected
- unauthorized reads and writes are denied
- the frontend exposes one working exam-results page and backend tests pass

## 9. Out-of-scope for this delivery

- moderation workflows
- transcript calculation
- cohort analytics dashboards
- bulk import/export
- appeals and re-mark requests