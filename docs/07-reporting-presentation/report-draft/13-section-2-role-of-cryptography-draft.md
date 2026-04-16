# Section 2 Draft — Role of Cryptography in EduSecure

This file is a **report-ready working draft** for Section 2.

Use it together with:
- `docs/02-architecture-crypto/crypto-decision-matrix.md`
- `docs/02-architecture-crypto/cia-evaluation.md`
- `docs/03-features/authentication/mfa-cryptography-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`
- `docs/07-reporting-presentation/high-mark-report-blueprint.md`
- `docs/07-reporting-presentation/report-section-to-evidence-map.md`
- `docs/07-reporting-presentation/report-draft/04-report-visual-allocation-plan.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`

## How to use this draft

- keep the section explanatory rather than comparative
- use this section to explain **what each primitive is for** in EduSecure, not why it was selected over alternatives
- keep only the **cryptographic primitives and roles table** in the main body unless more space remains
- leave most "why not X instead" material to Section 5
- merge the strongest final text into `docs/07-reporting-presentation/final-report-draft-sections-1-to-5.md`

## 2. Role of Cryptography in EduSecure

Cryptography in EduSecure serves several different purposes, and the report is strongest when those purposes are kept separate rather than collapsed into a generic idea of security. The platform needs to protect stored credentials, strengthen authentication, detect modification of academic data, support a proof-of-authorship narrative for submissions, protect selected secrets and content at rest, and reduce the chance that sensitive actions occur without trustworthy evidence. No single primitive can do all of those jobs, which is why the system uses several complementary mechanisms instead of relying on one algorithm alone.

[TABLE PLACEHOLDER — Table X. Cryptographic primitives and their roles in EduSecure
Source: writer-created summary based on `docs/02-architecture-crypto/crypto-decision-matrix.md`, `docs/02-architecture-crypto/cia-evaluation.md`, and `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`
Purpose: distinguish the roles of `bcrypt`, TOTP, `AES-GCM`, `ECDH`, `HKDF-SHA-256`, `SHA-256`, `HMAC-SHA-256`, ECC signatures, and `TLS 1.3` in the EduSecure case study
Placement: after the opening paragraph introducing multiple cryptographic roles
Priority: core
Status: not inserted yet]

The table should simplify the explanation rather than repeat it. In the final report, one short sentence after the table should make clear that different primitives solve different security problems and should not be described as interchangeable.

## 2.1 Why cryptography is necessary in this case study

The EduSecure brief begins from several failures that are fundamentally trust and cryptography problems: plaintext password storage, insecure communication, tampered submissions, altered grades, and missing verifiable records for sensitive actions. These are not solved by general software structure alone. They require mechanisms that protect secrets, authenticate actions, detect tampering, and preserve trustworthy evidence of what happened.

This is why cryptography matters so directly in the project. It is not included as a decorative extra. Instead, it supports the confidentiality of stored password verifiers and selected application secrets, strengthens authentication beyond a password-only baseline, supports integrity and authorship logic for submissions, and protects audit evidence for grade-sensitive actions. In other words, cryptography is central to restoring trust in the most sensitive academic workflows.

## 2.2 Symmetric encryption

Symmetric encryption in EduSecure has more than one role and those roles should be kept clearly separate. At the transport level, TLS 1.3 via Certbot/Let’s Encrypt is the intended deployment-side mechanism for protecting credentials, session cookies, and API traffic in transit. At the application level, `AES-GCM` is used both to protect specific sensitive assets at rest and to protect encrypted chat content inside a browser-mediated communication workflow.

This distinction matters academically because TLS and `AES-GCM` do not solve the same problem. TLS protects data while it moves between client and server, whereas `AES-GCM` protects selected assets within the application model. In the current repository, `AES-GCM` appears in three real business roles: MFA-secret protection at rest, submission-content protection at rest, and encrypted space-chat room-key/message protection. The report should therefore explain symmetric encryption in terms of these concrete uses rather than implying a blanket system-wide encryption claim.

## 2.3 End-to-end encrypted chat cryptography

EduSecure also applies cryptography to protected communication content through its encrypted space-chat workflow. In that slice, each user generates a browser-side `ECDH P-256` key pair and keeps the private key on the client device, while only the public key is registered with the backend. A per-space `AES-GCM` room key is then created for message encryption and wrapped separately for each authorised participant using an `ECDH`-derived shared secret processed through `HKDF-SHA-256`.

This is a major report point because it shows that the project uses cryptography not only for password protection, signatures, and at-rest secrecy, but also for browser-side ciphertext messaging in an academic collaboration workflow. The backend still manages membership, wrapped-key metadata, and ciphertext storage, yet routine message plaintext is moved away from normal backend handling. That gives the report a distinct communication-confidentiality story, while still leaving honest limits around metadata visibility, browser trust, and single-device-first key lifecycle.

## 2.4 Asymmetric cryptography and digital signatures

Asymmetric cryptography is relevant in EduSecure mainly because of its role in digital signatures rather than because of public-key encryption of ordinary application traffic. The key report point is that signatures support tamper-evidence and a proof-of-authorship narrative for submissions, which is much closer to the assignment brief than using asymmetric cryptography merely as an abstract theory topic.

In the current artefact, the submission workflow computes a digest and applies ECC-based signing and verification logic using a stable configured demo keypair. This strengthens integrity and authorship reasoning for submissions by showing that the file evidence can be checked rather than assumed. The report should keep the scope honest: this is a bounded study-project signing model, not a full enterprise PKI or user-held private-key deployment. Even so, signatures play an important role because they give stronger authorship and tamper-evidence support than a digest alone.

## 2.5 Hashes and MACs

Hashes and MACs should also be differentiated carefully. `SHA-256` is used in EduSecure to produce stable digest evidence over submitted plaintext content. This supports integrity checking because any change to the file bytes changes the resulting digest. However, a digest alone does not prove who created the content or whether a trusted system component produced the record.

That is why EduSecure also uses `HMAC-SHA-256` in the audit model. An HMAC adds a shared-secret integrity check, which is stronger than a plain hash for internal trusted-system records. In report terms, this means `SHA-256` supports digest-based integrity evidence for submissions, while `HMAC-SHA-256` supports tamper-evident internal audit integrity. Making that distinction clearly is one of the strongest ways to show mature cryptographic understanding.

## 2.6 Password hashing and MFA

Password hashing and MFA solve different parts of the authentication problem and should not be described as substitutes for one another. `bcrypt` protects stored password verifiers after registration and directly addresses the brief’s plaintext-password problem. MFA, by contrast, strengthens the login event itself by requiring a second proof derived from a time-based cryptographic secret. In EduSecure, this second factor is implemented through TOTP-based MFA with recovery codes.

This distinction is important because a password can be protected well at rest and still be stolen, reused, guessed, or phished at use time. The current MFA design therefore complements `bcrypt` rather than replacing it. It also demonstrates a strong cryptographic point: passwords are hashed because the server only needs verification, while TOTP seeds are encrypted because the server must later recover them to recompute valid codes. That difference between non-recoverable verification data and recoverable protected secret material is one of the strongest academic points in the whole project.

## Mini-conclusion

Overall, cryptography in EduSecure is not a single mechanism but a coordinated set of control types with different purposes. `bcrypt` protects password verifiers, TOTP strengthens authentication, `AES-GCM` protects selected secrets and stored submission content at rest, browser-side chat E2EE combines `ECDH P-256`, `HKDF-SHA-256`, and `AES-GCM` to protect collaboration messages, `SHA-256` supports digest-based integrity evidence, ECC-based signatures strengthen authorship and tamper-evidence logic, `HMAC-SHA-256` protects internal audit integrity, and TLS 1.3 is the intended control for secure transport in deployment. This section is strongest when it explains these distinct roles clearly, while leaving the deeper compare-and-select justification to Section 5.

## Safe wording reminders for this section

Prefer wording such as:
- "Cryptography in EduSecure serves several different purposes..."
- "This control supports..."
- "Within the study-project scope..."
- "The repository evidences..."
- "TLS remains the intended deployment-side transport control..."

Avoid wording such as:
- "JWT is encryption"
- "AES-GCM and TLS do the same job"
- "end-to-end encrypted chat removes all trust from the server"
- "a digest alone proves authorship"
- "the platform is fully encrypted"
- "the ECC workflow proves enterprise non-repudiation"
- "MFA replaces password hashing"

## Trim-first notes if the word count becomes tight

Cut in this order:
1. repeated explanatory examples after the table is inserted
2. repeated contrast sentences between TLS and AES-GCM after one clear statement remains
3. repeated explanation of hash versus HMAC after one clear paragraph remains
4. secondary MFA detail once the password-hash versus encrypted-seed distinction is retained

Keep until the end:
- the cryptographic-primitives table placeholder
- one clear paragraph on the browser-side encrypted-chat workflow
- one clear paragraph distinguishing password hashing from MFA
- one clear paragraph distinguishing TLS from AES-GCM
- one clear paragraph distinguishing digest from HMAC and signatures
- the mini-conclusion that explains Section 2 is about roles, while Section 5 is about selection

