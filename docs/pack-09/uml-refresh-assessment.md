# UML Refresh Assessment

This note records whether the existing EduSecure UML files need updating after the later submission confidentiality work:
- AES-GCM protection of submission content at rest
- metadata/content endpoint separation for submissions
- audited plaintext retrieval through a separate endpoint

## 1. Conclusion

### Final assessment
**The main design UMLs remain usable, and selected current-state diagrams have now been refreshed to reflect owner-scoped access for submissions, grades, and spaces.**

The existing UML set can still be used safely **if it is presented as design-level abstraction rather than exact post-implementation code generation**, while the refreshed current-state sequence/DFD artefacts capture the latest authorization posture more explicitly.

That is the correct interpretation of the repository’s UML files:
- Pack 02 diagrams are broad secure-system design baselines
- Pack 04 diagrams freeze early submission/authorship/audit design decisions
- Pack 05 diagrams document the later grade flow and a now-removed standalone symmetric-transport concept

The recent implementation changes refine the submission confidentiality boundary, but they do **not** invalidate the main pedagogic points already shown by the diagrams.

## 2. Why no mandatory redraw is required

## A. The UML files are already phase-design diagrams, not exact code snapshots
The current UML README in Pack 04 already frames the files as a design addendum for the coding phase.

That means they are meant to show:
- responsibilities
- interaction flow
- security decisions
- architectural reasoning

They are not required to mirror every later implementation refinement at field-for-field or endpoint-for-endpoint level.

## B. The main report value of the existing diagrams still holds
The important design claims already captured in the UML remain correct:
- submissions are authenticated and role-aware
- digest generation occurs during submission handling
- digital-signature verification occurs during submission creation
- audit records are append-oriented and integrity-protected
- grades attach to submissions and are integrity-sensitive
- ~~the retired standalone symmetric-transport slice remains a separate artefact capability~~ *(removed — secure transmission is handled by TLS 1.3 via Certbot/Let's Encrypt)*

Those are still true in the current implementation.

## C. The latest changes are best explained in text, not by mandatory redraw
The latest implementation refinements are real, but they are more naturally documented in:
- `docs/pack-06/submission-content-protection-and-retrieval.md`
- `docs/pack-06/submission-phase-status-and-evidence.md`
- `docs/pack-04/api-submission-contract.md`

This is especially true for:
- hiding `storedFileReference` from the normal metadata response
- splitting metadata retrieval and content retrieval into separate endpoints
- recording `SUBMISSION_CONTENT_ACCESSED`

Those are important implementation details, but they do not force a UML rewrite if the report already explains them in the implementation/evidence sections.

## 3. Diagram-by-diagram assessment

## `docs/pack-04/uml/class-diagram-submission-addendum.puml`
### Assessment
No mandatory refresh required.

### Reason
This diagram’s report value is to show the core domain relationship between:
- `Assignment`
- `Submission`
- `Grade`
- `AuditLog`
- submission verification state

It still succeeds at that.

### Known abstraction gap
The current implementation now stores additional submission encryption metadata and uses `storedFileReference` as an internal ciphertext locator.

The diagram uses a simplified submission storage field (`storedFilePath`) and does not list every newer encryption metadata field.

### Why this is still acceptable
For the final report, the diagram can still be used as a design abstraction as long as the text explains that the current implementation adds AES-at-rest metadata internally.

## `docs/pack-04/uml/sequence-submission-secure-pack04.puml`
### Assessment
Refreshed.

### Reason
Its main value is still accurate:
- student submits work
- backend hashes content
- backend signs/verifies
- verification result is stored
- audit events are written
- lecturer later retrieves submission review metadata

### Current update
The diagram now also names the lecturer actor as the assignment-owning lecturer and shows admin oversight explicitly alongside the AES-at-rest retrieval path.

## `docs/pack-04/uml/sequence-audit-integrity-secure.puml`
### Assessment
No refresh required.

### Reason
The audit model remains append-oriented and HMAC-backed. The newer `SUBMISSION_CONTENT_ACCESSED` event extends the same audit pattern rather than changing it.

## `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`
### Assessment
Refreshed.

### Reason
The grade-integrity flow remains unchanged in principle, but the diagram now reflects owner-scoped lecturer grade access with admin override.

## `docs/pack-09/uml/dfd-context-current-state.puml` and `docs/pack-09/uml/dfd-level-1-current-state.puml`
### Assessment
Refreshed.

### Reason
These current-state diagrams now separate lecturer and admin actors and make the owner-scoped boundaries explicit for submissions, grades, and spaces.

## `docs/pack-05/uml/sequence-aes-secure-transmission-demo.puml`
### Assessment
**Superseded.** The standalone symmetric-transport flow it described has been removed. Secure transmission is handled by TLS 1.3 via Certbot/Let's Encrypt. This diagram should not be cited in the report as active evidence. It is retained as a historical planning artefact only.

## 4. Safe wording for the report

If these diagrams are used in the final report, the safest wording is:

- "The UML diagrams represent the security design and main interaction logic of EduSecure rather than a code-generated mirror of every final field or endpoint refinement."
- "Later implementation details, such as encrypted-at-rest submission storage and separate plaintext retrieval, are documented in the implementation evidence notes."

## 5. Unsafe wording to avoid

Do **not** say:
- "The UML diagrams exactly mirror every final field and endpoint in the implementation."
- "The Pack 04 class diagram is a complete up-to-date entity dump from the codebase."
- "No implementation detail has changed since the early design packs."

## 6. Practical recommendation

For the final report:
- keep the existing design UMLs
- use the refreshed current-state sequence/DFD diagrams when you want explicit owner-scoped authorization wording
- explain the latest submission confidentiality refinement in prose using Pack 06 evidence docs

This is the best balance between:
- technical honesty
- report clarity
- scope control
- not doing diagram churn that adds little mark value

## 7. Supporting references

- `docs/pack-04/uml/README.md`
- `docs/pack-06/submission-content-protection-and-retrieval.md`
- `docs/pack-06/submission-phase-status-and-evidence.md`
- `docs/pack-09/report-claims-audit-note.md`

