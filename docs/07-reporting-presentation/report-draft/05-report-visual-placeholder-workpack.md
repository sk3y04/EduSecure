# Report Visual Placeholder Workpack

This file gives you ready-to-paste placeholder blocks for diagrams, tables, code artefact screenshots, and frontend screenshots.

Use these placeholders directly inside your working report draft so that visuals can be inserted later without restructuring the prose.

## 1. Placeholder format conventions

Use short placeholders like these while drafting:

```markdown
[FIGURE PLACEHOLDER — Figure X. Short caption
Source: `path/to/source`
Purpose: what this figure proves
Placement: after paragraph Y
Priority: core / optional / appendix
Status: not inserted yet]
```

```markdown
[TABLE PLACEHOLDER — Table X. Short caption
Source: `path/to/source` or writer-created summary table
Purpose: what this table compresses or compares
Placement: after paragraph Y
Priority: core / optional / appendix
Status: not inserted yet]
```

```markdown
[CODE ARTEFACT SCREENSHOT PLACEHOLDER — Figure X. Short caption
Source: `path/to/code/file`
Purpose: what implementation detail this screenshot proves
Placement: after paragraph Y
Priority: core / appendix
Status: capture needed]
```

```markdown
[FRONTEND SCREENSHOT PLACEHOLDER — Figure X. Short caption
Source: `frontend/src/...`
Purpose: what user-visible security flow this proves
Placement: after paragraph Y
Priority: core / appendix
Status: capture needed]
```

## 2. Section-by-section placeholders

## Section 1. Introduction

```markdown
[TABLE PLACEHOLDER — Optional Table X. Brief problem-to-scope summary
Source: writer-created summary table
Purpose: compress the insecure baseline and the implemented scope boundary into one small visual if needed
Placement: after paragraph 2
Priority: optional
Status: omit unless introduction becomes too dense]
```

## Section 2. Role of Cryptography in EduSecure

```markdown
[TABLE PLACEHOLDER — Table X. Cryptographic primitives and their roles in EduSecure
Source: writer-created summary based on `crypto-decision-matrix.md`
Purpose: distinguish `bcrypt`, `AES-GCM`, `SHA-256`, `HMAC-SHA-256`, ECC signatures, TLS, and MFA
Placement: after the paragraph introducing multiple cryptographic techniques
Priority: core
Status: not inserted yet]
```

## Section 3. Risk Assessment

```markdown
[TABLE PLACEHOLDER — Table X. Security methodologies used in the report
Source: `docs/07-reporting-presentation/report-draft/02-methodology-comparison-table.md`
Purpose: summarise the roles of OWASP, NIST SP 800-30, CVSS, STRIDE, and CIS
Placement: after the methodology paragraph
Priority: core
Status: ready to adapt]
```

```markdown
[TABLE PLACEHOLDER — Table X. Prioritised EduSecure risk summary
Source: `risk-register-refined.md` and `cvss-risk-register.md`
Purpose: compress the most important assets, risks, priorities, and mitigations
Placement: after the asset/threat discussion
Priority: core
Status: not inserted yet]
```

## Section 4. Secure System Design

```markdown
[FIGURE PLACEHOLDER — Figure X. Security-focused use-case diagram
Source: `docs/02-architecture-crypto/uml/current-state/use-case-security-focused.puml`
Purpose: show the main protected interactions in one overview
Placement: after the opening paragraph of Section 4
Priority: optional
Status: export needed]
```

```markdown
[FIGURE PLACEHOLDER — Figure X. Insecure deployment baseline
Source: `docs/02-architecture-crypto/uml/foundation/deployment-insecure.puml`
Purpose: show the insecure starting point from the brief
Placement: after the insecure-baseline paragraph
Priority: optional
Status: export needed]
```

```markdown
[FIGURE PLACEHOLDER — Figure X. Secure deployment comparison
Source: `docs/02-architecture-crypto/uml/foundation/deployment-secure.puml`
Purpose: show improved trust boundaries, session handling, and transport posture
Placement: after the secure-design transition paragraph
Priority: core
Status: export needed]
```

```markdown
[FIGURE PLACEHOLDER — Figure X. Secure login sequence
Source: `docs/02-architecture-crypto/uml/foundation/sequence-login-secure.puml`
Purpose: prove that session establishment follows password verification and MFA logic
Placement: after the authentication design paragraph
Priority: core
Status: export needed]
```

```markdown
[FIGURE PLACEHOLDER — Figure X. Secure submission sequence
Source: `docs/02-architecture-crypto/uml/submissions-audit/sequence-submission-secure-pack04.puml`
Purpose: show digest generation, signature verification, encrypted-at-rest storage, and audited retrieval
Placement: after the submission design paragraph
Priority: core
Status: export needed]
```

```markdown
[FIGURE PLACEHOLDER — Figure X. Grade integrity sequence
Source: `docs/02-architecture-crypto/uml/grades-and-history/sequence-grade-integrity-secure-pack05.puml`
Purpose: show verified-submission gating, scoped access, and audit-backed grade handling
Placement: after the grade-integrity paragraph
Priority: core
Status: export needed]
```

## Section 5. Cryptographic Controls and Selection Justification

```markdown
[TABLE PLACEHOLDER — Table X. Algorithm comparison and final control selection
Source: writer-created summary based on `crypto-decision-matrix.md`
Purpose: compare `bcrypt`, `AES-GCM`, ECC signatures, `SHA-256`, `HMAC-SHA-256`, and TLS against alternatives
Placement: after the first compare-and-select paragraph
Priority: core
Status: not inserted yet]
```

```markdown
[TABLE PLACEHOLDER — Optional Table X. Control-to-risk mapping
Source: writer-created summary using Section 3 risks and selected controls
Purpose: show why each chosen control addresses a specific problem from the brief
Placement: after the final justification paragraph
Priority: optional
Status: insert only if it removes repeated prose]
```

## Section 6. Implementation Plan and Considerations

```markdown
[TABLE PLACEHOLDER — Table X. Key and secret separation in EduSecure
Source: writer-created summary using `application.properties`, `application-prod.properties`, and service docs
Purpose: distinguish JWT secret, MFA secret-encryption key, audit HMAC secret, and submission storage key material
Placement: after the key-handling paragraph
Priority: core
Status: not inserted yet]
```

```markdown
[CODE ARTEFACT SCREENSHOT PLACEHOLDER — Optional Figure X. Production configuration hardening example
Source: `backend/src/main/resources/application-prod.properties` or related validator code
Purpose: show that secure deployment assumptions are explicitly configured or validated
Placement: after the deployment-assumptions paragraph
Priority: appendix
Status: capture only if needed]
```

## Section 7. CIA Evaluation

```markdown
[TABLE PLACEHOLDER — Table X. CIA contribution summary of implemented controls
Source: writer-created summary based on `cia-evaluation.md`
Purpose: compress which control mainly supports confidentiality, integrity, or availability and what limitation remains
Placement: after the opening evaluation paragraph
Priority: core
Status: not inserted yet]
```

## Section 8. Technical Artefact Summary

```markdown
[TABLE PLACEHOLDER — Table X. Implemented cryptographic evidence summary
Source: `final-implementation-evidence-map.md` and test coverage summaries
Purpose: summarise the implemented slices, evidence type, and strongest repository anchors
Placement: after the opening artefact-summary paragraph
Priority: core
Status: not inserted yet]
```

```markdown
[FIGURE PLACEHOLDER — Optional Figure X. Current-state level-1 DFD
Source: `docs/02-architecture-crypto/uml/current-state/dfd-level-1-current-state.puml`
Purpose: give one implementation-facing overview of how sensitive data moves through the platform
Placement: after the first architecture-to-artefact bridge paragraph
Priority: optional
Status: export needed]
```

```markdown
[FRONTEND SCREENSHOT PLACEHOLDER — Figure X. Authentication or MFA screen
Source: `frontend/src/pages/Login/index.vue` or `frontend/src/pages/MfaChallenge/index.vue`
Purpose: show the visible user-facing authentication flow that corresponds to the backend security design
Placement: after the auth/MFA evidence paragraph
Priority: core
Status: capture needed]
```

```markdown
[FRONTEND SCREENSHOT PLACEHOLDER — Figure X. Submission detail evidence screen
Source: `frontend/src/pages/SubmissionDetail/index.vue`
Purpose: show visible submission evidence such as integrity, verification, content-access, or grading-related UI
Placement: after the submission or grade artefact paragraph
Priority: core
Status: capture needed]
```

```markdown
[CODE ARTEFACT SCREENSHOT PLACEHOLDER — Figure X. Security test evidence or relevant backend artefact
Source: targeted backend test run output or a concise code artefact snippet
Purpose: prove that the claim is evidenced in the repository rather than discussed only at design level
Placement: after the test and execution evidence paragraph
Priority: core
Status: capture needed]
```

```markdown
[FRONTEND SCREENSHOT PLACEHOLDER — Optional Figure X. Browser cookie or CSRF evidence
Source: manual browser capture
Purpose: support discussion of browser-facing security posture if included in the final report
Placement: after the browser-security evidence sentence
Priority: appendix
Status: capture only if used]
```

## Section 9. Conclusion

```markdown
[NO VISUAL PLACEHOLDER RECOMMENDED — keep the conclusion text-only unless the module explicitly requires a closing summary visual]
```

## 3. Final reminder

Do not leave placeholders as empty labels in the final submitted report.

Before merging, replace each placeholder with either:
- the final inserted visual and caption, or
- a conscious deletion decision if it is not needed in the main body

