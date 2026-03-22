# CIA Evaluation

This document evaluates the planned EduSecure controls against Confidentiality, Integrity, and Availability.

## 1. Evaluation principle

The brief requires the selected controls to be justified using the CIA model. For EduSecure, this should not become a vague checklist. Each control should be linked to:

- what it protects
- how it protects it
- what it does not protect
- what residual risk remains

## 2. CIA control table

| Control | Confidentiality | Integrity | Availability | Limitation / residual risk |
|---|---|---|---|---|
| `bcrypt` password hashing | protects stored passwords from direct plaintext disclosure after database compromise | indirectly supports trust in account authentication | little direct availability effect | does not stop phishing, password reuse, or credential theft outside the database |
| TOTP-based MFA | limits unauthorised account access even if a password alone is exposed | strongly improves authentication integrity by requiring a second proof at login | can reduce availability if users lose their authenticator device or recovery codes | does not fully stop phishing proxies, session hijack after login, or endpoint compromise |
| `TLS 1.3` for client-server communication | protects credentials, tokens, and sensitive data in transit | protects against many in-transit modification attacks | supports reliable secure communication but may add small operational complexity | does not protect compromised endpoints or bad authorisation logic |
| `AES-GCM` secure message/file protection | protects selected file/message content from unauthorised disclosure | authenticated encryption also detects tampering of encrypted payload | minimal direct contribution to availability | depends on correct key and nonce handling |
| `RSA + SHA-256` digital signature verification | limited direct confidentiality role | strongly supports integrity and authorship / non-repudiation for submissions | little direct availability effect | depends on secure private-key control and correct verification workflow |
| `SHA-256` digesting | no direct confidentiality | detects accidental or intentional content modification when compared against expected digest | minimal direct effect | hash alone does not prove who created or changed the data |
| `HMAC-SHA-256` for audit integrity | shared-secret protection helps keep internal audit integrity mechanisms trustworthy | stronger authenticated integrity than plain hash for trusted-system records | minimal direct effect | requires secure shared-secret handling |
| RBAC with Student/Lecturer/Admin roles | limits unauthorised exposure of records | limits unauthorised modification paths | supports stable operation by reducing misuse | cryptography cannot compensate for badly designed permissions |
| audit logging for sensitive actions | may protect confidential records by restricting review paths | improves accountability and tamper investigation | supports operational recovery and investigation | logging without integrity protection can still be altered |
| bounded study-project scope and simplified architecture | no direct confidentiality effect | improves implementation correctness by reducing accidental security mistakes | strongest availability contribution in this study context because a smaller system is easier to complete and maintain | does not replace genuine resilience engineering |

## 3. Confidentiality discussion

EduSecure confidentiality is mainly supported by:
- `bcrypt` for passwords at rest
- TOTP-based MFA as an extra barrier against account takeover after password compromise
- `TLS 1.3` for traffic in transit
- `AES-GCM` for selected file/message confidentiality demonstrations
- RBAC to restrict who can access which academic records

Important limitation:
- confidentiality is not guaranteed by JWT itself
- confidentiality also depends on secure coding, proper response filtering, and avoiding overexposure of DTO fields

## 4. Integrity discussion

Integrity is the strongest and most central property in this assignment because the brief explicitly highlights tampered submissions, altered grades, and missing verification.

EduSecure integrity is mainly supported by:
- TOTP-based MFA for stronger login assurance
- `SHA-256` digests
- `RSA` digital signatures for authorship and tamper detection
- `HMAC-SHA-256` for protected audit integrity where appropriate
- audit logging of sensitive actions
- role checks on grade-changing operations

Important limitation:
- integrity claims fail if key handling is weak or if privileged actors misuse their rights

## 5. Availability discussion

Availability is less cryptography-centric than confidentiality and integrity, but it still matters.

In this study project, availability is supported by:
- using a bounded scope that can be delivered reliably
- keeping the architecture simple enough to test and explain
- using PostgreSQL persistence rather than fragile in-memory-only design
- avoiding unnecessary dependency complexity
- documenting what is out of scope instead of pretending to provide production resilience

Availability trade-off note for MFA:
- MFA improves account protection, but it also introduces user-lockout risk if enrollment, recovery-code handling, and disable flows are poorly designed

Important limitation:
- EduSecure is not intended to demonstrate enterprise high availability, disaster recovery, or large-scale resilience

## 6. Overall CIA judgement

### Strongest coverage
- Integrity
- Confidentiality

### Adequate but intentionally limited coverage
- Availability

### Why this is acceptable
The assignment is primarily concerned with cryptographic controls for securing sensitive education-platform processes. A study-project implementation can therefore prioritise confidentiality and integrity while discussing availability in a bounded and honest way.

