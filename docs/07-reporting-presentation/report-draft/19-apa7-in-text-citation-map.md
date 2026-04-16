# APA 7 In-Text Citation Map for the EduSecure Report

This file shows **where** to use external APA 7 citations in the report and how to keep them separate from repository proof.

Use it together with:
- `docs/07-reporting-presentation/report-section-to-evidence-map.md`
- `docs/07-reporting-presentation/report-claims-audit-note.md`
- `docs/07-reporting-presentation/report-draft/18-apa7-source-bank.md`
- `docs/07-reporting-presentation/report-draft/17-space-chat-e2ee-cross-section-draft.md`

## 1. Core rule for citation placement

Use this pattern throughout the report:

1. make the EduSecure-specific claim
2. support the *theory / standard / best-practice* part with an external APA 7 citation
3. support the *implementation* part with repository evidence

Example:

> EduSecure uses a simplified NIST-style risk-assessment approach to prioritise the most important threats to credentials, submissions, grades, and audit evidence (Joint Task Force Transformation Initiative, 2012; Forum of Incident Response and Security Teams, 2019). The specific risk treatment is then evidenced in `docs/01-governance-risk-traceability/risk-register-refined.md`.

## 2. Section-by-section citation map

## Section 1. Introduction

### External citations
You can keep this section lightly cited.

Best options:
- no citation if the paragraph is purely case-study framing from the assignment brief
- `(OWASP Foundation, 2021a)` only if you briefly frame the platform as a web-application security case study

### Repository evidence
- `docs/00-overview/assignment_brief.md`
- `docs/01-governance-risk-traceability/assignment-traceability.md`

### Best use
- keep this section mostly repo- and brief-driven
- do not overload the introduction with references

## Section 2. Role of Cryptography in EduSecure

### External citations
Use 3 to 5 citations across the section.

#### For why multiple primitives are needed
- `(Anderson, 2020)`
- `(Saltzer & Schroeder, 1975)`

#### For password hashing and MFA
- `(Provos & Mazieres, 1999)`
- `(Grassi et al., 2017)`
- `(M'Raihi et al., 2011)`

#### For hashes, MACs, and signatures
- `(National Institute of Standards and Technology, 2015)`
- `(Krawczyk et al., 1997)`

#### For symmetric encryption and transport
- `(Dworkin, 2007)`
- `(Rescorla, 2018)`

#### For browser-side encrypted chat
- `(Krawczyk & Eronen, 2010)`
- `(World Wide Web Consortium, 2017)`
- `(Dworkin, 2007)`

### Repository evidence
- `docs/03-features/authentication/mfa-cryptography-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`

### Good sentence types
- why `bcrypt` is different from a fast hash
- why `AES-GCM` is different from TLS
- why `HMAC` is different from a plain digest
- why chat E2EE uses `ECDH` + `HKDF` + `AES-GCM`

## Section 3. Risk Assessment

### External citations
Use 3 to 4 citations here.

#### For the main method
- `(Joint Task Force Transformation Initiative, 2012)`

#### For web-application risk framing
- `(OWASP Foundation, 2021a)`

#### For severity support
- `(Forum of Incident Response and Security Teams, 2019)`

#### For threat-modelling logic
- `(Shostack, 2014)`

### Repository evidence
- `docs/01-governance-risk-traceability/risk-methodology.md`
- `docs/01-governance-risk-traceability/risk-register-refined.md`
- `docs/01-governance-risk-traceability/cvss-risk-register.md`

### Good sentence types
- why NIST-style assessment was chosen
- why CVSS supports prioritisation rather than replacing risk analysis
- why application-security framing matters in EduSecure

## Section 4. Secure System Design

### External citations
Use 2 to 4 citations here.

#### For secure design and trust boundaries
- `(Saltzer & Schroeder, 1975)`
- `(Shostack, 2014)`
- `(OWASP Foundation, 2021b)`

#### For transport design
- `(Rescorla, 2018)`

#### For encrypted chat workflow boundaries
- `(World Wide Web Consortium, 2017)`
- `(Krawczyk & Eronen, 2010)`
- `(Dworkin, 2007)`

### Repository evidence
- `docs/02-architecture-crypto/uml/`
- `docs/02-architecture-crypto/space-chat-e2ee-design.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/07-reporting-presentation/report-diagram-figure-map.md`

### Good sentence types
- why trust boundaries matter
- why MFA and session establishment are staged
- why the chat design reduces routine backend plaintext exposure
- why TLS should be described as a deployment-side control

## Section 5. Cryptographic Controls and Selection Justification

### External citations
This is the most citation-friendly section. Use 4 to 6 citations.

#### Password storage
- `(Provos & Mazieres, 1999)`
- `(Grassi et al., 2017)`

#### AES-GCM selection
- `(Dworkin, 2007)`

#### Hashes and MACs
- `(National Institute of Standards and Technology, 2015)`
- `(Krawczyk et al., 1997)`

#### MFA and OTP reasoning
- `(M'Raihi et al., 2005)`
- `(M'Raihi et al., 2011)`

#### TLS 1.3
- `(Rescorla, 2018)`

#### Chat E2EE stack
- `(Krawczyk & Eronen, 2010)`
- `(World Wide Web Consortium, 2017)`
- `(Dworkin, 2007)`

### Repository evidence
- `docs/02-architecture-crypto/crypto-decision-matrix.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`

### Good sentence types
- why `bcrypt` was selected over a fast digest
- why `AES-GCM` is appropriate instead of weaker/more misuse-prone modes
- why the chat workflow uses `HKDF` after `ECDH`
- why TLS belongs in transport design rather than as a replacement for at-rest or message-layer controls

## Section 6. Implementation Plan and Considerations

### External citations
Use 2 to 4 citations.

#### For nonce / AEAD handling
- `(Dworkin, 2007)`

#### For browser cryptography support
- `(World Wide Web Consortium, 2017)`

#### For authentication and password handling boundaries
- `(Grassi et al., 2017)`

#### For secure design trade-offs
- `(Anderson, 2020)`
- `(Saltzer & Schroeder, 1975)`

### Repository evidence
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`
- `frontend/src/services/chatCrypto.ts`
- `frontend/src/services/chatKeyStore.ts`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`

### Good sentence types
- why fresh nonces matter in `AES-GCM`
- why browser-stored chat keys are a bounded but defensible trade-off
- why key separation matters
- why local defaults should not be described as production-safe

## Section 7. CIA Evaluation

### External citations
Use 2 to 3 citations.

Best options:
- `(Anderson, 2020)`
- `(Saltzer & Schroeder, 1975)`
- `(Joint Task Force Transformation Initiative, 2012)` if linking residual risk back to the risk method

### Repository evidence
- `docs/02-architecture-crypto/cia-evaluation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/04-evidence-testing/testing-support/final-implementation-evidence-map.md`

### Good sentence types
- why integrity is the strongest property in the artefact
- why confidentiality is improved but still bounded
- why chat E2EE improves content confidentiality but not metadata confidentiality

## Section 8. Technical Artefact Summary

### External citations
Keep this section light. Use 1 to 2 citations maximum.

Best options:
- `(OWASP Foundation, 2021c)` if discussing testing/evidence posture
- `(World Wide Web Consortium, 2017)` if briefly grounding browser cryptography support for the E2EE chat slice

### Repository evidence
- `docs/04-evidence-testing/testing-support/final-implementation-evidence-map.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `frontend/src/services/chatCrypto.ts`
- selected backend/frontend test and implementation files

### Good sentence types
- that the repository demonstrates multiple cryptographic techniques
- that encrypted space chat is implemented, not merely proposed
- that tests and docs corroborate the implementation story

## Section 9. Conclusion

### External citations
Usually optional.

Best approach:
- no more than 1 citation, and only if the conclusion briefly restates a methodological or design principle

### Repository evidence
- strongest claims already established earlier in the report

## 3. Fast paste-ready citation suggestions

Use these when drafting quickly.

### Risk method
- `EduSecure uses a simplified NIST-style assessment to identify and prioritise the most important risks to the platform (Joint Task Force Transformation Initiative, 2012).`

### OWASP framing
- `This risk picture is also consistent with familiar OWASP web-application categories such as broken authentication, security misconfiguration, and cryptographic failures (OWASP Foundation, 2021a).`

### Password hashing
- `A dedicated password hashing scheme is more appropriate than a fast general-purpose digest for stored password verification (Provos & Mazieres, 1999).`

### MFA / TOTP
- `The MFA workflow is based on time-based one-time password generation rather than reusable static second-factor data (M'Raihi et al., 2011).`

### AES-GCM
- `AES-GCM is suitable here because it provides authenticated encryption rather than confidentiality alone (Dworkin, 2007).`

### HMAC
- `HMAC is more appropriate than a plain hash where keyed integrity protection is required (Krawczyk et al., 1997).`

### TLS 1.3
- `Transport protection is addressed separately through TLS 1.3 rather than by reusing an application-layer encryption mechanism for network security (Rescorla, 2018).`

### Chat E2EE key derivation
- `The encrypted-chat workflow derives purpose-specific wrapping keys using HKDF after shared-secret derivation, which is a standard way to turn secret material into a context-bound symmetric key (Krawczyk & Eronen, 2010).`

### Browser cryptography
- `The browser-side cryptography design is also grounded in the Web Cryptography API support model used by modern web applications (World Wide Web Consortium, 2017).`

## 4. Minimum 15-citation route

If you want the easiest path to a defensible minimum, use at least these in the final report:

1. Joint Task Force Transformation Initiative (2012)
2. OWASP Foundation (2021a)
3. OWASP Foundation (2021b)
4. Forum of Incident Response and Security Teams (2019)
5. Shostack (2014)
6. Grassi et al. (2017)
7. Provos and Mazieres (1999)
8. M'Raihi et al. (2011)
9. Dworkin (2007)
10. Krawczyk and Eronen (2010)
11. Krawczyk et al. (1997)
12. National Institute of Standards and Technology (2015)
13. Rescorla (2018)
14. World Wide Web Consortium (2017)
15. Saltzer and Schroeder (1975)

That set already covers methodology, testing/security framing, passwords, MFA, AES-GCM, HKDF, HMAC, SHA-256, TLS, browser cryptography, and secure-design principles.

## 5. Final check before submission

Before finalising the reference list, confirm:
- every in-text citation appears in the final references section
- every final reference is cited at least once
- no external citation is being used to pretend EduSecure implements something the repo does not prove
- the heaviest citation density is in Sections 2 to 5, not in the conclusion
- repository files still do the evidential work for implementation claims

