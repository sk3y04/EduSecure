# Platform Feature Matrix and Prioritized Backlog

This document converts the EduSecure case-study feature list into a report-friendly status matrix and a practical implementation backlog.

It is based on the repository's current implemented state rather than the full aspirational learning-platform brief.

## 1. Feature matrix against the assignment brief

| Audience | Requested platform capability | Current status | What the repository currently provides | Gap remaining |
|---|---|---|---|---|
| Students | Register for courses | Partial | Students can access only the academic spaces they belong to. Staff-managed space membership controls visibility of spaces, assignments, submissions, and grades. | There is no self-service student course-registration flow. Access is assigned by staff through space membership. |
| Students | Access learning materials | Partial | Students can access assignment records in spaces they belong to and can open submission-related pages. | There is no dedicated lecture-material or resource-library module for notes, documents, or video learning content. |
| Students | Submit assignments | Implemented | Students can upload assignment submissions, retrieve their own submission metadata, and access controlled plaintext retrieval where authorized. Submission integrity and confidentiality controls are implemented. | The core submission flow exists, but it is bounded to the coursework slice rather than a broader learning-material workflow. |
| Students | Join live virtual classrooms via video | Not implemented | No classroom, meeting, streaming, or video-session module is present. | A real-time classroom integration layer is still required. |
| Students | View exam results and grades | Partial | Students can view grades for their own assignment submissions while space membership remains valid. | There is no separate exam domain, exam-result entity, exam board workflow, or exam-results UI. |
| Lecturers and Admins | Upload lecture notes and video content | Partial | Lecturers and admins can create spaces and assignments, which act as coursework containers. | There is no dedicated upload and retrieval workflow for lecture notes, recorded lectures, or other reusable teaching materials. |
| Lecturers and Admins | Record attendance | Not implemented | No attendance entity, API, or frontend screen is present. | Attendance capture, storage, reporting, and authorization rules still need to be implemented. |
| Lecturers and Admins | Grade submissions | Implemented | Lecturers and admins can create, update, and retrieve grades for verified submissions. Students can view their own grade. | This covers assignment grading only, not full assessment administration. |
| Lecturers and Admins | Communicate with students | Not implemented | No messaging, announcement, chat, or notification module is present. | A communication workflow still needs domain design, persistence, delivery rules, and UI. |
| Lecturers and Admins | Manage exam scheduling | Not implemented | No exam timetable or scheduling model is present. | Exam-event creation, scheduling conflicts, publication, and student visibility are still absent. |
| Lecturers and Admins | Manage feedback forms | Not implemented | Lecturer free-text feedback exists only as part of the grade record for a submission. | There is no reusable feedback-form model, survey flow, or response-management feature. |

## 2. What is strongly implemented already

The repository currently delivers a secure coursework-management slice rather than a full LMS.

Strongly implemented areas include:
- authentication, cookie-backed session handling, and optional TOTP MFA
- staff-managed academic spaces with roster control
- assignment creation and visibility scoping by space membership
- student submission upload and lecturer review
- submission hashing, signing, verification state, and encrypted-at-rest content protection
- grade creation, update, retrieval, and student own-grade viewing
- tamper-evident audit logging for security-sensitive actions

## 3. Important scope boundary for the report

The report should describe EduSecure honestly as:
- a security-focused education-platform case study
- with a real implemented coursework and grading core
- but without full parity with the complete learning-platform scenario in the brief

That framing is technically stronger than claiming that every LMS capability has already been implemented.

## 4. Prioritized implementation backlog

The recommended order below prioritizes finishing the current coursework slice cleanly before adding larger platform modules.

### Priority 0: close the existing coursework-slice gaps

| Priority | Work item | Why it comes first | Main outputs |
|---|---|---|---|
| P0 | Fix frontend assignment creation to include `spaceId` correctly | The backend already requires space-scoped assignments, so this is a coherence gap inside existing scope rather than a new feature area. | corrected assignment-create payload, validated lecturer/admin flow, updated frontend evidence |
| P0 | Expose clearer course or space membership language in docs and UI | The current system behaves like staff-managed membership, and the wording should not imply student self-registration. | aligned terminology, cleaner report wording, reduced marker-facing contradiction risk |

### Priority 1: complete the core academic workflow

| Priority | Work item | Why now | Main outputs |
|---|---|---|---|
| P1 | Student self-service course registration requests | This is the closest missing feature to the current space model and can reuse existing roles, authorization, and membership structures. | registration-request entity, approve or reject workflow, lecturer/admin review screen |
| P1 | Learning-material repository for each space | Students currently reach assignments but not real course materials. This is the most visible LMS gap after enrolment. | material entity, file metadata, upload/download endpoints, space-scoped material list UI |
| P1 | Lecturer upload flow for notes and teaching files | This makes the material repository usable and directly addresses one of the lecturer/admin brief requirements. | lecturer/admin upload UI, authorization checks, audit coverage |

### Priority 2: extend the assessment domain beyond coursework

| Priority | Work item | Why now | Main outputs |
|---|---|---|---|
| P2 | Exam scheduling module | The brief explicitly includes exam administration, and scheduling is the foundation for later exam-related features. | exam entity, timetable fields, staff management API, student-visible schedule page |
| P2 | Exam results domain | Current grade support is submission-based only. A distinct exam-results model is needed for honest feature coverage. | exam-result entity, result publication rules, student result view, audit coverage |
| P2 | Structured feedback forms | Once assessment events exist, reusable feedback forms become more coherent than ad hoc implementation. | form templates, response capture, aggregation or review screens |

### Priority 3: add operational teaching features

| Priority | Work item | Why now | Main outputs |
|---|---|---|---|
| P3 | Attendance recording | Attendance depends on spaces and usually relates to timetabled sessions, so it fits better after the academic scheduling layer exists. | attendance records, session association, lecturer/admin attendance screen, reporting |
| P3 | Student communication and announcements | Communication is important but can be added after the academic objects it refers to already exist. | announcement or messaging model, delivery rules, inbox or noticeboard UI |

### Priority 4: add real-time delivery features

| Priority | Work item | Why last | Main outputs |
|---|---|---|---|
| P4 | Live virtual classroom integration | This is the largest architectural jump because it introduces real-time sessions, external video infrastructure, and more operational risk than the current CRUD and crypto flows. | meeting provider integration, session links or tokens, role-aware join flow, operational security notes |

## 5. Suggested implementation strategy

If the goal is to strengthen the project as an assessed artefact rather than build a full production LMS, the best sequence is:

1. finish the existing space and assignment workflow cleanly
2. add one convincing enrolment flow
3. add one convincing learning-material flow
4. add one clearly modelled exam-scheduling or exam-results flow
5. leave live classroom delivery as future work unless there is time for meaningful evidence and testing

That approach keeps the project academically credible and avoids diluting the already strong security-focused implementation.

## 6. Repository evidence supporting this matrix

Primary implementation evidence:
- `backend/src/main/java/edusecure/edusecure/controller/space/SpaceController.java`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceService.java`
- `backend/src/main/java/edusecure/edusecure/controller/assignment/AssignmentController.java`
- `backend/src/main/java/edusecure/edusecure/service/assignment/AssignmentService.java`
- `backend/src/main/java/edusecure/edusecure/controller/submission/SubmissionController.java`
- `backend/src/main/java/edusecure/edusecure/controller/grade/GradeController.java`
- `frontend/src/router/index.ts`
- `frontend/src/pages/SpaceDetail/index.vue`
- `frontend/src/pages/AssignmentList/index.vue`
- `frontend/src/pages/SubmissionCreate/index.vue`
- `frontend/src/pages/SubmissionDetail/index.vue`
- `frontend/src/pages/AssignmentSubmissions/index.vue`
- `frontend/src/pages/UserManagement/index.vue`

Primary status and evidence documents:
- `docs/pack-03/implementation-status-and-evidence.md`
- `docs/pack-06/submission-phase-status-and-evidence.md`
- `docs/pack-07/grade-phase-status-and-evidence.md`
- `docs/pack-09/final-doc-alignment-summary.md`

Primary automated evidence:
- `backend/src/test/java/edusecure/edusecure/SpaceFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`