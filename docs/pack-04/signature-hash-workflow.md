# Signature and Hash Workflow

This document defines the cryptographic workflow for the first secure submission implementation phase.

## 1. Selected primitives

From the existing decision matrix:
- `SHA-256` for digest generation
- `ECC + SHA-256` for digital signature creation and verification
- `TLS 1.3` in the secure design narrative for transport protection

## 2. Workflow goal

The workflow must demonstrate two things clearly:
1. the exact submitted content can be checked for integrity
2. the submission has proof-of-authorship within the study-project model

## 3. Chosen first-phase verification model

### Selected model
Use a **server-managed simulation of student signing identity**.

### Meaning in practice
- each student has signing identity metadata in the study-project model
- the system can access configured signing material needed for the demonstration
- the report must explicitly state that this is a simulated authorship environment suitable for an educational artefact

### Current implementation refinement
The current backend now uses a **stable configured demo ECC keypair** loaded from externalisable resource locations rather than generating a fresh signing keypair on each application startup.

This improves:
- repeatability across runs
- consistency of signature-verification evidence
- the realism of the simulated signing environment

It does **not** change the core scope boundary:
- the signing model remains server-managed for the artefact
- it is still not a full user-held PKI deployment

This is acceptable because the assignment asks for a functional demonstration, not a production key-management system.

## 4. Secure submission workflow

### Step 1: student is authenticated
The student authenticates using the already-implemented auth foundation.

### Step 2: submitted content is received
The system accepts the assignment content and metadata.

### Step 3: `SHA-256` digest is generated
The backend computes a digest over the exact submitted content.

### Step 4: digital signature is created or validated
Depending on the implementation boundary chosen for the first artefact:
- either the system simulates student signing and generates the signature
- or the system accepts a provided signature and verifies it immediately

### Step 5: signature verification occurs immediately
The backend verifies the signature against the digest and the student's associated verification material.

### Step 6: submission record is persisted
The following are stored together:
- submission metadata
- file reference
- `hashDigest`
- `digitalSignature`
- `signatureAlgorithm`
- `verificationStatus`
- `verificationMessage`

### Step 7: audit record is created
The submission event and verification result are added to the audit trail.

## 5. Design decisions settled here

### Decision 1: digest is generated on the backend
Reason:
- ensures a consistent trusted calculation path for the first implementation
- easier to test and explain
- avoids frontend inconsistency issues

### Decision 2: verification happens during submission creation
Reason:
- cleaner API contract
- simpler state model
- stronger evidence for the report

### Decision 3: store verification result with the submission
Reason:
- easy lecturer-facing visibility
- simple test assertions
- avoids needing an immediate secondary verification table

## 6. Failure-handling model

If verification fails, one of two approaches is possible:

### Option A: reject invalid submissions immediately
Pros:
- simpler integrity guarantees
- easier to explain

### Option B: accept but mark as failed verification
Pros:
- stronger demonstration of auditability and review workflow

### Selected recommendation
Prefer **Option B for the study project** if it can be implemented cleanly, because it gives richer evidence:
- successful verified submission
- failed verification case
- stored verification state
- lecturer/admin review possibilities

If scope becomes tight, Option A is acceptable as the fallback.

## 7. Key-handling note for the report

The report must clearly state:
- the system uses a simulated signing model for the artefact
- private-key protection is simplified for study purposes, even though the demo keypair is now stable and externally configurable
- this demonstrates authorship logic, not a full enterprise key-management deployment

## 8. Evidence expected from implementation

The later implementation should be able to demonstrate:
- same content -> stable digest
- modified content -> different digest
- valid signature -> verification success
- tampered content or mismatched signature -> verification failure
- persisted verification result retrievable through the API

