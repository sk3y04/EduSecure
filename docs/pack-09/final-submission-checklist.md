# Final Submission Checklist

Use this checklist near the end of the project to avoid missing report, artefact, or appendix items.

Checked items below reflect the **repository-verified state as of 2026-03-15**, based on current documentation, source files, and recent local test runs. They do **not** imply that the final written report, appendix wording, screenshots, or packaging tasks are finished.

## 1. Core report checklist

Use `docs/pack-09/high-mark-report-blueprint.md` as the main writing-order and section-emphasis guide for this checklist.

- [ ] title page includes full name, student number, module code, and assignment title
- [ ] introduction clearly frames EduSecure as a cryptography case study
- [ ] cryptography section compares and justifies AES, RSA, ECC, SHA-256, bcrypt, and HMAC
- [ ] risk assessment uses the documented NIST-style method
- [ ] propositional logic justifications are included
- [ ] secure system design diagrams are included and explained
- [ ] CIA evaluation is included with limitations/residual risks
- [ ] conclusion is concise and honest about scope

## 2. Artefact/evidence checklist

- [x] auth evidence is available
- [x] submission/signature evidence is available
- [x] audit-integrity evidence is available
- [x] grade-integrity evidence is available
- [x] AES-GCM evidence for implemented at-rest controls is available
- [x] Liquibase schema-migration evidence is available
- [x] PostgreSQL/Testcontainers verification evidence is available
- [x] cookie-auth hardening evidence is available
- [x] implemented frontend MVP evidence is available
- [x] automated test results are available and readable
- [ ] screenshots or sample outputs are selected if helpful
- [ ] manual security-review evidence records are filled in using `docs/pack-09/test-evidence-collection-template.md` where relevant
- [x] worked example evidence records are available in `docs/pack-09/test-evidence-worked-examples.md`
- [ ] the live review log in `docs/pack-09/security-review-evidence-log.md` is populated for the scenarios actually executed

## 3. Documentation consistency checklist

Use `docs/pack-09/report-claims-audit-note.md` as the main wording-control reference for this section.
Use `docs/pack-09/final-cryptography-claims-matrix.md` for control-by-control claim wording, `docs/pack-09/final-doc-alignment-summary.md` for the corrected doc/UML positioning, and `docs/pack-09/uml-refresh-assessment.md` for the broader design-abstraction conclusion.

- [ ] report claims match the implemented backend
- [ ] diagrams match the current design
- [ ] appendix claims are clearly separated from verified implementation
- [ ] no future work is written as if it is already complete
- [ ] PostgreSQL wording distinguishes verified local schema delivery from unverified production hardening
- [ ] database-encryption claims are made only where the exact mechanism is evidenced

## 4. Frontend checklist

- [x] frontend scope remains MVP-level
- [x] implemented frontend pages support the artefact/report rather than product-style sprawl
- [x] auth integration follows the current cookie-backed session contract
- [x] implemented submission/auth/MFA pages reflect the real API contract
- [ ] optional grade pages are added only if they improve report evidence

## 5. Appendix checklist

- [ ] CI/CD appendix is clearly labelled as appendix/support material
- [ ] deployment appendix is clearly labelled as appendix/support material
- [ ] any GitHub Actions or Nginx examples are accurate to the final setup used
- [ ] `homelab-blueprint` is referenced honestly as a deployment/infra reference, not as unverified proof of EduSecure deployment

## 6. Final technical verification checklist

- [x] backend tests pass
- [x] the PostgreSQL Liquibase smoke test passes
- [x] core endpoints still behave as expected
- [ ] configuration values are externalised where intended
- [ ] no obvious secrets are committed accidentally
- [x] `.gitignore` still excludes `docs/` and `Christialattion/` as intended for the main repo

## 7. Packaging checklist

- [ ] final codebase is readable and reasonably commented
- [ ] report and artefact tell the same security story
- [ ] references are formatted consistently
- [ ] appendix material is concise and relevant
- [ ] final submission contents match the brief requirements

