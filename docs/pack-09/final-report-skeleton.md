# EduSecure Final Report Skeleton

> Replace the bracketed prompts with your final writing.
> Keep the finished report within the assignment word count.
> Use `docs/pack-09/high-mark-report-blueprint.md` and `docs/pack-09/final-cryptography-claims-matrix.md` while drafting.

---

# Title Page

**Full Name:** [Your full name]  
**Student Number:** [Your student number]  
**Module Code:** 500IT  
**Assignment Title:** Designing and Implementing Cryptographic Solutions for Securing an Online Education Platform: A Case Study of EduSecure

---

# 1. Introduction

EduSecure is [brief case-study framing]. The platform handles [credentials, submissions, grades, academic records]. The assignment brief identifies several serious security failures, including [plaintext password storage, insecure communication, tampered submissions, grade alteration, and lack of logging/verification].

This report aims to [analyze risks, justify cryptographic control selection, explain the secure system design, and evaluate the implemented artefact].

**Close this section with a one-sentence roadmap.**

---

# 2. Role of Cryptography in EduSecure

## 2.1 Why cryptography is necessary in this case study

[Explain the platform’s assets and why cryptography is needed for confidentiality, integrity, authenticity, and limited availability support.]

## 2.2 Symmetric encryption

[Explain symmetric encryption generally, then connect it to TLS in design and AES-GCM in the artefact.]

## 2.3 Asymmetric cryptography and digital signatures

[Explain RSA/ECC, then connect signatures to proof-of-authorship/tamper-evidence logic for submissions.]

## 2.4 Hashes and MACs

[Differentiate hashing from authenticated integrity. Explain SHA-256 vs HMAC.]

## 2.5 Password hashing and MFA

[Explain why bcrypt is used for password storage and why TOTP-based MFA improves authentication integrity.]

**Mini-conclusion:** [Summarize how different cryptographic primitives serve different EduSecure security goals.]

---

# 3. Risk Assessment

## 3.1 Chosen assessment method

[State that a simplified NIST SP 800-30-style method is used. Briefly explain the workflow: assets, vulnerabilities, threat events, likelihood, impact, prioritization, residual risk.]

## 3.2 Key assets

[List the key assets and explain their sensitivity briefly.]

## 3.3 Main vulnerabilities and threat events

[Discuss the main risks from the brief and from the implemented platform boundary.]

## 3.4 Prioritized risk discussion

### Risk 1: [Title]
- Asset: [ ]
- Vulnerability: [ ]
- Threat event: [ ]
- Likelihood: [ ]
- Impact: [ ]
- Priority: [ ]
- Selected control(s): [ ]
- Logic statement: [ ]
- Plain-English justification: [ ]
- Residual risk: [ ]

### Risk 2: [Title]
- Asset: [ ]
- Vulnerability: [ ]
- Threat event: [ ]
- Likelihood: [ ]
- Impact: [ ]
- Priority: [ ]
- Selected control(s): [ ]
- Logic statement: [ ]
- Plain-English justification: [ ]
- Residual risk: [ ]

### Risk 3: [Title]
- Asset: [ ]
- Vulnerability: [ ]
- Threat event: [ ]
- Likelihood: [ ]
- Impact: [ ]
- Priority: [ ]
- Selected control(s): [ ]
- Logic statement: [ ]
- Plain-English justification: [ ]
- Residual risk: [ ]

**Mini-conclusion:** [Explain which risks are most important and why the later controls focus especially on integrity-sensitive processes.]

---

# 4. Secure System Design

## 4.1 Insecure baseline

[Briefly describe the insecure baseline from the brief.]

## 4.2 Secure authentication design

[Explain the implemented auth model: bcrypt, JWT-backed stateless session after authentication, HttpOnly cookie transport, optional MFA branch.]

**Refer to:** [secure login UML]

## 4.3 Secure submission design

[Explain digest generation, signature workflow, immediate verification, AES-at-rest storage, metadata/content split, and audited plaintext retrieval.]

**Refer to:** [submission sequence UML + class diagram + Pack 06 evidence note]

## 4.4 Grade-integrity design

[Explain verified-submission-only grading, assignment-owner-scoped lecturer restrictions with admin override, student own-grade access while the related assignment remains visible through current space membership, and append-oriented audit evidence.]

**Refer to:** [grade-integrity UML]

## 4.5 Secure transmission design

[Explain that TLS 1.3 via Certbot/Let's Encrypt is the intended deployment-side transport control for protecting client-server traffic in transit. If you do not include direct HTTPS deployment evidence, present this as a secure design/deployment control rather than as repository-proven enforcement.]

**Mini-conclusion:** [Summarize how the secure design addresses the brief’s major incidents.]

---

# 5. Cryptographic Controls and Selection Justification

## 5.1 Password storage choice

[Compare bcrypt with inappropriate alternatives such as plain SHA-256 for password storage.]

## 5.2 Symmetric encryption choice

[Explain why AES was selected and why AES-GCM is the chosen mode.]

## 5.3 Asymmetric/signature choice

[Compare RSA and ECC. Justify ECC + SHA-256 for this artefact, and note that the implemented signing flow uses a stable configured demo keypair within a bounded study-project simulation rather than a full production PKI.]

## 5.4 Hashing and MAC choice

[Explain the different roles of SHA-256 and HMAC-SHA-256.]

## 5.5 Transport security design choice

[Explain the role of TLS 1.3 in the secure design narrative and clarify that this is a design/deployment control rather than the main artefact proof.]

**Mini-conclusion:** [Summarize why these controls are appropriate for EduSecure’s specific risks and study-project scope.]

---

# 6. Implementation Plan and Considerations

## 6.1 Technology stack

[Briefly justify Java/Spring Boot, Spring Security, Validation, JPA, Liquibase, and PostgreSQL.]

## 6.2 Randomness and nonce handling

[Explain SecureRandom and why nonce reuse in GCM must be avoided.]

## 6.3 Key handling assumptions

[Differentiate JWT secret, MFA secret-encryption key, audit HMAC secret, and submission storage key material. Emphasize that these secrets should remain separated by purpose.]

## 6.4 Error handling and secure coding boundaries

[Explain structured validation/auth errors, no secret leakage, role checks, and safe failure handling.]

## 6.5 Schema delivery and database verification

[Explain Liquibase, PostgreSQL smoke verification, and why this improves reproducibility without overclaiming production maturity.]

**Mini-conclusion:** [Summarize the main implementation trade-offs and why they are reasonable.]

---

# 7. CIA Evaluation

## 7.1 Confidentiality

[Explain how bcrypt, MFA secret encryption, TLS 1.3 via Certbot/Let's Encrypt (transport), and submission AES-at-rest protection contribute to confidentiality. Note that the retired standalone symmetric-crypto slice has been removed — AES-GCM appears only in real business roles.]

## 7.2 Integrity

[Explain why integrity is the strongest theme in the artefact: signatures, hashes, HMAC-backed audit values, verified-submission-only grading, audited access/actions.]

## 7.3 Availability

[Explain bounded availability support honestly: limited complexity, maintainable scope, database persistence, focused verification, but no enterprise HA claim.]

## 7.4 Residual limitations

[List the main residual risks and limitations.]

**Mini-conclusion:** [State which CIA properties are strongest and why.] 

---

# 8. Technical Artefact Summary

## 8.1 Implemented authentication and MFA evidence

[Summarize the auth/MFA slice and reference the strongest tests or screenshots.]

## 8.2 Implemented submission integrity/confidentiality evidence

[Summarize the submission slice and reference digest/signature/verification and encrypted-at-rest behavior.]

## 8.3 Implemented grade-integrity evidence

[Summarize the grade slice and reference audit-backed integrity/accountability.]

## 8.4 Secure transmission evidence (TLS via Certbot/Let's Encrypt)

[Explain that TLS 1.3 via Certbot/Let's Encrypt is the intended deployment-side transport control. Reference browser HTTPS evidence only if you actually include direct deployment proof. Note that the retired standalone symmetric-crypto slice was removed, so the repository's implemented symmetric-encryption evidence now comes from the AES-GCM at-rest controls.]

## 8.5 Test and execution evidence

[State that backend tests pass and frontend evidence build/type-check succeeds if you include frontend support in the report.] 

**Mini-conclusion:** [State clearly that the artefact demonstrates more than the minimum three cryptographic techniques required by the brief.]

---

# 9. Conclusion

[Summarize the final security posture, emphasize the strongest implemented controls, and acknowledge the bounded study-project limitations honestly.]

A strong final sentence should state that EduSecure now addresses the brief’s core cryptographic problems through a combination of password hashing, MFA hardening, integrity/authorship controls, AES-based confidentiality measures, and tamper-evident auditing.

---

# References

[Insert final academic and technical references in the required style.]

---

# Appendix (Optional / Support Material)

## Appendix A. Selected API/test evidence
[Insert screenshots or short output snippets.]

## Appendix B. UML figures
[Insert exported diagrams with captions.]

## Appendix C. Optional deployment/support notes
[Keep clearly labelled as support material rather than implemented proof.]

