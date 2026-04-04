# Report Section to Evidence Map

This document maps the recommended report structure to concrete EduSecure evidence already present in the repository.

For a section-by-section writing sequence and top-band emphasis guide, use `docs/pack-09/high-mark-report-blueprint.md` alongside this map.

## Section 1. Introduction and case-study framing

Use:
- `docs/assignment_brief.md`
- `docs/pack-01/assignment-traceability.md`
- `docs/receipt+plan.md`

Purpose:
- explain EduSecure as a cryptography case study
- summarise the security incidents from the brief

## Section 2. Role of cryptography in EduSecure

Use:
- `docs/pack-02/crypto-decision-matrix.md`
- `docs/pack-03/mfa-cryptography-implementation.md`
- `docs/pack-02/cia-evaluation.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`
- implemented examples from Pack 06 / 07 evidence notes plus Pack 09 claim-control docs

Purpose:
- explain how AES, RSA, ECC, SHA-256, bcrypt, and HMAC are compared, and why bcrypt, SHA-256, ECC, HMAC, and AES-GCM were implemented
- distinguish confidentiality, integrity, and authenticity roles

## Section 3. Risk assessment

Use:
- `docs/pack-01/risk-register.md`
- `docs/pack-02/risk-methodology.md`
- `docs/pack-02/risk-register-refined.md`

Purpose:
- present NIST-style assessment
- show asset-threat-vulnerability mapping
- include propositional logic justifications

## Section 4. Secure system design

Use:
- `docs/pack-01/uml-documentation-plan.md`
- `docs/pack-02/uml/*.puml`
- `docs/pack-04/uml/*.puml`
- `docs/pack-05/uml/*.puml`
- `docs/pack-09/current-state-data-flow-diagram.md`
- `docs/pack-09/uml/*.puml`
- `docs/pack-09/final-doc-alignment-summary.md`
- `docs/pack-09/uml-refresh-assessment.md`

Suggested diagram emphasis:
- context and level-1 DFD for current trust boundaries and data stores
- use case diagram
- deployment diagram insecure/secure
- class diagram
- secure login sequence
- secure submission sequence
- grade integrity sequence
- ~~retired standalone symmetric-transport sequence~~ *(removed — TLS deployment via Certbot/Let's Encrypt is the transmission control)*

## Section 5. Cryptographic controls and algorithm selection

Use:
- `docs/pack-02/crypto-decision-matrix.md`
- `docs/pack-03/mfa-cryptography-implementation.md`
- `docs/pack-09/report-claims-audit-note.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`

Purpose:
- compare alternatives
- justify final selections
- explain why AES-GCM, bcrypt, ECC + SHA-256, and HMAC-SHA-256 were chosen

## Section 6. Implementation plan and considerations

Use:
- `docs/pack-02/implementation-plan-and-considerations.md`
- `docs/pack-03/mfa-cryptography-implementation.md`
- `docs/pack-09/report-claims-audit-note.md`
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`
- `docs/pack-09/postgresql-setup-and-security.md`
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/src/main/java/edusecure/edusecure/config/LiquibaseConfig.java`

Purpose:
- explain Java/Spring choices
- discuss randomness, nonce handling, key assumptions, and secure coding boundaries
- explain why schema delivery is now controlled through Liquibase rather than `ddl-auto=update`
- distinguish verified local PostgreSQL delivery from broader production-hardening claims

## Section 7. CIA evaluation

Use:
- `docs/pack-02/cia-evaluation.md`
- evidence from implemented backend phases in `docs/pack-06/`, `docs/pack-07/`, and Pack 09 wording-control docs

Purpose:
- show how each control supports confidentiality, integrity, or availability
- discuss residual limitations honestly

## Section 8. Technical artefact summary

Use:
- `docs/pack-09/final-implementation-evidence-map.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`
- `docs/pack-09/report-claims-audit-note.md`
- `frontend/README.md`
- selected files under `frontend/src/`
- `docs/pack-06/submission-phase-status-and-evidence.md`
- `docs/pack-06/submission-content-protection-and-retrieval.md`
- `docs/pack-07/grade-phase-status-and-evidence.md`
- test classes under `backend/src/test/java/edusecure/edusecure/`
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`

Purpose:
- show exactly what was implemented
- show which endpoints and tests demonstrate the artefact
- show that the repository now contains both fast H2-based tests and a narrower real-PostgreSQL verification path
- optionally show the implemented frontend MVP as presentation/evidence support for the backend artefact

## Section 9. Conclusion

Use:
- residual risks from `docs/pack-02/cia-evaluation.md`
- implementation limits from Packs 06–07 and Pack 09 claim-boundary notes
- appendix planning from this pack if useful

Purpose:
- summarise the final security posture honestly
- emphasise study-project scope and limitations

## Suggested appendix material

Use:
- `docs/pack-09/vue-frontend-mvp-and-api-integration-plan.md`
- `docs/pack-09/appendix-cicd-and-deployment-plan.md`
- selected test outputs or screenshots

Purpose:
- keep core report focused while still demonstrating broader engineering thinking and distinguishing implemented frontend evidence from optional remaining UI polish

