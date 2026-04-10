# EduSecure Planning Pack 09

Pack 09 is the final documentation-support phase after the main backend cryptographic artefact has been implemented.

## Purpose

This pack shifts the project focus from core backend feature delivery to:
- report-ready evidence mapping
- frontend evidence and remaining Vue.js alignment work
- appendix planning for CI/CD and deployment
- final submission readiness

## Why this pack exists

The backend now already covers the main cryptographic artefact areas:
- `bcrypt` password hashing
- JWT-backed auth foundation with `HttpOnly` cookie transport for browser clients
- `SHA-256` digest generation
- ECC-based submission signing and verification
- `HMAC-SHA-256` audit integrity
- `AES-GCM` protection for MFA secrets and submission content at rest

What still needs disciplined planning is:
- how to present this evidence in the report
- how to supplement the earlier UML with a proper current-state data flow diagram
- how to phrase cryptographic claims precisely without over-claiming
- whether the existing UML diagrams need refresh after later implementation refinements
- how to present the implemented Vue frontend and its credentialed API integration without scope drift
- how to describe CI/CD and home-server deployment in the appendix without overstating production readiness
- how PostgreSQL setup, hardening, and verified Liquibase/Testcontainers evidence should be documented honestly
- how to capture the new cookie-auth hardening and startup-validation evidence clearly in the final report

## Contents

- `platform-feature-matrix-and-prioritized-backlog.md`
- `report-ready-section-1-and-8-scope-paragraphs.md`
- `current-state-data-flow-diagram.md`
- `unit-test-coverage-summary.md`
- `integration-test-coverage-summary.md`
- `manual-test-coverage-summary.md`
- `csrf-browser-evidence-capture-note.md`
- `test-evidence-collection-template.md`
- `test-evidence-worked-examples.md`
- `security-review-evidence-log.md`
- `lecturer-feedback-alignment-guide.md`
- `final-implementation-evidence-map.md`
- `final-cryptography-claims-matrix.md`
- `report-section-to-evidence-map.md`
- `report-claims-audit-note.md`
- `uml-refresh-assessment.md`
- `uml/`
- `vue-frontend-mvp-and-api-integration-plan.md`
- `appendix-cicd-and-deployment-plan.md`
- `postgresql-setup-and-security.md`
- `final-submission-checklist.md`

## Outcome expected from Pack 09

After this pack, the remaining work should be mostly:
- optional Vue frontend polish rather than first-time implementation
- collecting screenshots and outputs
- final report writing and appendix assembly
- optional polish, not major redesign

This now includes documenting the boundary between what the repository proves locally (Liquibase-backed PostgreSQL delivery and a dedicated Testcontainers smoke test) and what it still does not prove (full production hardening).

It also includes documenting what the repository now proves about browser-session hardening: `HttpOnly` cookie auth transport, logout cookie clearing, CSRF token bootstrap/header behavior for unsafe browser requests, and fail-fast startup validation for unsafe production cookie settings.

It also includes a lecturer-facing alignment guide that maps common marker expectations, such as use-case coverage, security-focused sequence diagrams, explicit risk treatment, and run/execution explanation, to the existing EduSecure evidence set.

