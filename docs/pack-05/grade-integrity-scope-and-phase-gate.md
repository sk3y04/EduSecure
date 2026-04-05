# Grade Integrity Scope and Phase Gate

This document defines the design boundary for the grade-integrity phase before any grade-related coding begins.

## 1. Why grade integrity needs its own phase

The assignment brief explicitly highlights:
- leaked exam results
- altered grades in transit
- missing integrity checks
- missing logging and verification for sensitive actions

For this reason, grade handling must be treated as a separate sensitive workflow, not just an extra field attached to submissions.

## 2. Goal of the grade-integrity phase

The grade-integrity phase should demonstrate that:
- only authorised actors can create or update grades
- grade changes are auditable
- sensitive grade actions are recorded with tamper-evident integrity support
- students can retrieve their own grades through an authenticated, role-aware flow while the related assignment remains visible through current space membership

## 3. In-scope items

### Core grade workflow
- create a grade for a verified submission
- update a grade as an authorised assignment-owning lecturer or admin action
- retrieve a grade for the owning student while the related assignment remains visible to that student
- retrieve grade details for the owning lecturer or admin where authorised

### Integrity and audit scope
- create audit records for grade creation and updates
- protect audit records with `HMAC-SHA-256`
- record enough metadata to support later dispute review

### Access-control scope
- Student: can view only own grade data while the related assignment remains visible through current space membership
- Lecturer: can create/update/read grades only for assignments they own/manage
- Admin: can review grade-related audit history and sensitive actions

## 4. Explicitly out of scope

The first grade-integrity implementation should not attempt:
- full moderation workflow
- grade appeals system
- fine-grained academic rubrics
- notification engine
- production analytics/reporting dashboards
- full version-control history for every grade field unless absolutely necessary

## 5. Selected grade model strategy

### Selected recommendation
Use a **single current `Grade` record plus append-oriented audit records**.

### Why
- simpler than maintaining a full grade-version entity model
- easier to implement and test in a study project
- still supports accountability through audit logging
- aligns well with the existing `AuditLog` design in Pack 04

## 6. Grade-update governance rules

### Rule 1: grade creation should only occur for an existing submission
A grade must reference a valid submission record.

### Rule 2: grade creation should preferably require verified submission state
Recommended rule:
- create grades only for `SubmissionVerificationStatus.VERIFIED`

This keeps the integrity story stronger.

### Rule 3: every grade creation/update must create an audit record
The audit event should include:
- actor identity
- entity type and entity ID
- action type
- timestamp
- concise details sufficient for later review
- integrity value

### Rule 4: no silent overwrite
Every grade update must be visible through audit history.

## 7. Risks addressed

This phase primarily addresses:
- `R3` grade exposure or alteration through weak handling
- `R5` untraceable sensitive actions
- `R7` scope drift, by keeping the grade workflow deliberately small and auditable

## 8. Go condition for coding

Coding may begin only when all of the following are accepted:
- grade API contract is accepted
- role rules are accepted
- audit expectations are accepted
- evidence/test plan is accepted
- the retired standalone symmetric-transport design is documented separately in this pack

## 9. No-Go condition for coding

Do not start the grade phase if any of the following remain unresolved:
- whether grades can be attached to unverified submissions
- who is allowed to update grades
- what audit events must be created
- what evidence will prove that updates are tamper-evident and reviewable

