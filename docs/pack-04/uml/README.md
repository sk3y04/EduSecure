# Pack 04 UML Addendum

These PlantUML files extend the earlier Pack 02 UML with the more specific design decisions frozen in Pack 04.

## Included diagrams

- `class-diagram-submission-addendum.puml`
- `sequence-submission-secure-pack04.puml`
- `sequence-submission-aes-at-rest-retrieval-pack04.puml`
- `sequence-lecturer-submission-decryption.puml`
- `sequence-audit-integrity-secure.puml`

## Purpose

These diagrams do not replace the broader Pack 02 UML set. They refine it for the next coding phase by making the following Pack 04 decisions concrete:

- `Assignment`, `Submission`, `Grade`, and `AuditLog` responsibilities
- `hashDigest` and `digitalSignature` stored on `Submission`
- immediate verification during submission creation
- simulated signing model for the study project
- `HMAC-SHA-256` for append-oriented audit integrity support

The dedicated `sequence-submission-aes-at-rest-retrieval-pack04.puml` diagram also acts as a focused bridge between the Pack 04 AES-at-rest design and the later implemented retrieval flow documented in `../../pack-06/submission-content-protection-and-retrieval.md`.

For an even narrower explanation of how a lecturer reads protected submission content, use `sequence-lecturer-submission-decryption.puml`. It isolates the server-side retrieval path: authorization, ciphertext read, wrapped-key restoration, AES-GCM decryption, audit recording, and the final attachment response.

These UML files should be read as **design-level abstractions**, not exact code-generated mirrors of every later implementation refinement.

For the current final assessment on whether they need updating after the later AES-at-rest submission work, see `docs/pack-09/uml-refresh-assessment.md`.

