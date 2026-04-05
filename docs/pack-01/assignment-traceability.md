# Assignment Traceability Matrix

This document maps the requirements in `docs/assignment_brief.md` to planned outputs for the EduSecure study project.

## 1. Assignment framing

- **Module:** `500IT Cryptography`
- **Title:** `Designing and Implementing Cryptographic Solutions for Securing an Online Education Platform: A Case Study of EduSecure`
- **Assessment type:** report + artefact
- **Design principle:** the application is evidence for the report, not an end in itself

## 2. EduSecure case-study assets

Primary assets identified from the brief:

1. user credentials
2. student personal records
3. grades and exam results
4. assignment submission files
5. lecturer feedback files
6. attendance and course records
7. session tokens
8. audit trails for sensitive actions

## 3. Actor model

Planned system roles for EduSecure:

- **Student**
  - register/login
  - complete MFA challenge if enabled
  - access course materials
  - submit assignments visible through current space membership
  - view own grades while the related assignment remains visible through current space membership
- **Lecturer**
  - upload materials
  - view submissions
  - provide feedback
  - record grades
- **Admin**
  - manage users and courses
  - oversee sensitive actions
  - review audit records

## 4. Traceability table

| Brief requirement | Planned repository output | Notes |
|---|---|---|
| Explain role of cryptography in EduSecure | report sections supported by `risk-register.md`, `scope-assumptions.md`, and later architecture docs | Must connect controls to real incidents in the brief |
| Discuss symmetric/asymmetric encryption, hashes, MACs, digital signatures | later crypto decision matrix and implementation docs | Minimum algorithm comparison: AES, RSA, ECC, SHA-256, bcrypt, HMAC |
| Identify assets and vulnerabilities | `risk-register.md` | Includes plaintext passwords, password-only account takeover risk, no HTTPS, tampering, authenticated-session theft risk, weak auditability |
| Apply a recognised risk standard | `risk-register.md` uses NIST SP 800-30 style wording and prioritisation | Keep one standard consistent throughout report |
| Add propositional logic justification to mitigations | `risk-register.md` | Use short logic statements per risk entry |
| Produce secure system UML sequence diagrams | `uml-documentation-plan.md` then actual diagrams | Must show insecure vs secure flows; secure login may include an MFA branch where authenticated-session establishment happens only after second-factor verification |
| Justify cryptographic selections | later design and implementation documents | Choice must be explained, not only listed |
| Describe implementation plan using libraries | later backend planning docs | Java/Spring Boot libraries are appropriate for this repo |
| Evaluate CIA | later evaluation/test document | CIA must be linked to each selected control |
| Implement at least three cryptographic techniques in artefact | later backend implementation plan | Recommended: bcrypt, AES, digital signature, HMAC/audit integrity |

Current documentation priority note:
- the next auth-hardening phase is TOTP-based MFA so password-only login risk is addressed before more feature growth

## 5. Learning outcome alignment

| Learning outcome | Planned demonstration |
|---|---|
| LO1: propositional logic, sets, functions and relations | propositional logic mitigation statements in risk register; role and trust-boundary reasoning in UML |
| LO2: evaluate hashing and cryptography methods | algorithm comparison and rationale sections |
| LO3: implement ciphers, public-key methods, MACs and digital signatures | technical artefact with at least three cryptographic capabilities |
| LO4: apply cryptography in software development | Spring Boot implementation using standard Java libraries |

## 6. CIA alignment for EduSecure

| CIA property | Planned control examples |
|---|---|
| Confidentiality | bcrypt for password storage, TOTP-based MFA as an account-takeover barrier, TLS in design, AES-based secure file/message demonstration |
| Integrity | TOTP-based MFA for stronger login assurance, SHA-256 digests, HMAC, digital signatures, tamper-evident audit records |
| Availability | simple study-project deployment, controlled dependency set, database persistence, bounded scope |

## 7. Minimum artefact target

To keep the implementation achievable while still supporting the report, the current target is to demonstrate at least these four capabilities:

1. secure password hashing with `bcrypt`
2. symmetric encryption for file/message protection using `AES`
3. digital signature creation and verification using `RSA` or `ECC`
4. integrity protection for sensitive actions using `SHA-256` and/or `HMAC`

If scope must be reduced later, retain at least three of the above while preserving a coherent narrative.

