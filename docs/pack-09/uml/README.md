# Pack 09 UML / DFD Additions

This folder contains **current-state diagram supplements** added after the earlier Pack 02/04/05 UML baseline.

## Included diagrams

- `dfd-context-current-state.puml`
- `dfd-level-1-current-state.puml`

## Why these files are here

The earlier documentation packs already covered:
- use case modelling
- deployment views
- sequence diagrams for login, submission, audit, and grade flows
- class/domain abstractions

What they did not include was a dedicated **data flow diagram** showing the implemented trust boundaries and data stores in one place.

These Pack 09 files fill that gap without rewriting the older UML artefacts.

## Relationship to earlier UML

These DFDs:
- supplement `docs/pack-02/uml/`
- do not replace the earlier secure/insecure sequence diagrams
- do not change the historical interpretation recorded in `docs/pack-09/uml-refresh-assessment.md`
- should be cited as a **current-state explanatory layer** for the final report

## Scope reminder

Keep the diagrams aligned to current safe claims:
- browser auth uses a backend-issued `HttpOnly` cookie
- MFA is optional and TOTP-based
- submission confidentiality at rest uses AES-GCM plus wrapped per-submission keys
- grade integrity depends on verified submissions and audit recording
- secure transmission claims are TLS/deployment based, not the deprecated Pack 08 AES demo

