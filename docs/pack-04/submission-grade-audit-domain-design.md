# Submission, Grade, and Audit Domain Design

This document defines the minimum domain model for the next secure-submission feature phase.

## 1. Design principle

The domain model should be just large enough to support:
- secure assignment submission
- proof of authorship
- verification status tracking
- later grade attachment
- tamper-evident auditability

It should not become a full education-platform schema at this stage.

## 2. Proposed entities

## A. `Assignment`

### Responsibility
Represents a specific academic task that students can submit work for.

### Minimum fields
- `id`
- `title`
- `description`
- `dueAt`
- `createdByLecturerId` or lecturer relationship
- optional `isOpen` / status field

### Why it exists
It separates the assignment definition from the student's actual submission and keeps the domain model academically meaningful.

## B. `Submission`

### Responsibility
Represents one student's submitted work for a specific assignment, together with integrity and authorship metadata.

### Minimum fields
- `id`
- `assignmentId`
- `studentUserId`
- `submittedAt`
- `fileName`
- `contentType`
- `storedFilePath` or equivalent file reference
- `hashDigest`
- `digitalSignature`
- `signatureAlgorithm`
- `verificationStatus`
- optional `verificationMessage`

### Why `hashDigest` belongs here
The digest is part of the evidence about the exact submitted content. It should therefore live with the submission record, not separately.

### Why `digitalSignature` belongs here
The signature is evidence linked to the specific submitted artefact. For the first implementation phase, keeping it on `Submission` is simpler and more reportable than introducing a separate signature entity.

## C. `Grade`

### Responsibility
Represents the assessment outcome associated with a verified submission.

### Minimum fields
- `id`
- `submissionId`
- `value`
- `feedback`
- `gradedByLecturerId`
- `gradedAt`
- optional `lastModifiedAt`

### Why `Grade` should be separate from `Submission`
Grades are a distinct academic decision and are one of the sensitive records highlighted in the brief. Keeping them separate supports later auditability and integrity discussions.

## D. `AuditLog`

### Responsibility
Represents a sensitive action record that supports accountability and tamper detection.

### Minimum fields
- `id`
- `actionType`
- `actorUserId`
- `entityType`
- `entityId`
- `eventTimestamp`
- `detailsJson` or concise event details
- `integrityValue`
- optional `previousIntegrityValue` if using chained integrity

### Why it must be separate
Audit data has a different purpose from the business entities. It should remain append-oriented and analyzable as evidence for integrity and accountability.

## 3. Supporting enums / value types

## `SubmissionVerificationStatus`
Suggested values:
- `PENDING`
- `VERIFIED`
- `FAILED_VERIFICATION`
- `REJECTED`

## `AuditActionType`
Suggested first-phase values:
- `SUBMISSION_CREATED`
- `SUBMISSION_VERIFIED`
- `SUBMISSION_VERIFICATION_FAILED`
- `GRADE_CREATED`
- `GRADE_UPDATED`

## 4. Relationship model

Recommended minimum relationships:
- one `Assignment` to many `Submission`
- one `User` (student) to many `Submission`
- one `Submission` to zero or one `Grade`
- one `User` (lecturer) to many created `Assignment` records
- many `AuditLog` entries may reference the same business entity over time

## 5. Design choices settled by this document

### Decision 1: `Assignment` and `Submission` are separate entities
Reason:
- better academic meaning
- cleaner report explanation
- easier lifecycle management

### Decision 2: `hashDigest` is stored on `Submission`
Reason:
- it belongs to the actual submitted content record
- easier to show in the class diagram and API responses

### Decision 3: `digitalSignature` is stored on `Submission`
Reason:
- simpler first artefact implementation
- avoids unnecessary entity expansion

### Decision 4: `Grade` is separate from `Submission`
Reason:
- grades are sensitive records with their own audit needs
- cleaner later integrity model

### Decision 5: `AuditLog` is append-oriented and separate
Reason:
- improves traceability
- supports tamper-evidence design
- fits the brief's auditing requirement

## 6. Deferred decisions

The following may still be deferred until the actual implementation ticket for that phase, but only within the boundaries set here:
- exact file-storage implementation
- whether `detailsJson` is free-text or structured JSON
- whether `integrityValue` is plain hash, HMAC, or part of a chain for each event type
- whether key metadata is stored directly on `User` or in a later `UserKeyMetadata` type

## 7. Recommendation for the first implementation pass

Keep the next implementation pass intentionally simple:
- implement `Assignment`, `Submission`, and `AuditLog` first
- delay a full `Grade` workflow unless it is necessary for the chosen audit demonstration
- if grade integrity is not implemented in the same pass, retain the `Grade` design here but defer the code

