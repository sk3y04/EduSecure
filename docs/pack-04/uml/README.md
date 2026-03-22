# Pack 04 UML Addendum

These PlantUML files extend the earlier Pack 02 UML with the more specific design decisions frozen in Pack 04.

## Included diagrams

- `class-diagram-submission-addendum.puml`
- `sequence-submission-secure-pack04.puml`
- `sequence-audit-integrity-secure.puml`

## Purpose

These diagrams do not replace the broader Pack 02 UML set. They refine it for the next coding phase by making the following Pack 04 decisions concrete:

- `Assignment`, `Submission`, `Grade`, and `AuditLog` responsibilities
- `hashDigest` and `digitalSignature` stored on `Submission`
- immediate verification during submission creation
- simulated signing model for the study project
- `HMAC-SHA-256` for append-oriented audit integrity support

These UML files should be read as **design-level abstractions**, not exact code-generated mirrors of every later implementation refinement.

For the current final assessment on whether they need updating after the later AES-at-rest submission work, see `docs/pack-09/uml-refresh-assessment.md`.

