# Report Diagram Figure Map

This note maps the recommended EduSecure diagrams to the report sections where they add the most value.

Use it together with:
- `docs/pack-09/high-mark-report-blueprint.md`
- `docs/pack-09/report-section-to-evidence-map.md`
- `docs/pack-09/uml-refresh-assessment.md`
- `docs/pack-09/final-doc-alignment-summary.md`

## 1. Recommended core figure set

These are the best diagrams to include if the goal is to maximise marks while avoiding unnecessary repetition.

| Suggested figure label | Source file | Best report section | One-sentence commentary to use in the report |
|---|---|---|---|
| **Figure 1. Insecure deployment baseline** | `docs/pack-02/uml/deployment-insecure.puml` | Section 4: Secure System Design | This figure establishes the insecure baseline described in the assignment brief by showing weak transport and trust-boundary assumptions before cryptographic controls are applied. |
| **Figure 2. Secure deployment comparison** | `docs/pack-02/uml/deployment-secure.puml` | Section 4: Secure System Design | This figure shows how the EduSecure architecture is strengthened through protected session handling, clearer trust boundaries, and deployment-side transport security assumptions. |
| **Figure 3. Secure login sequence** | `docs/pack-02/uml/sequence-login-secure.puml` | Section 4: Secure System Design | This sequence diagram shows that authentication is not complete until the secure login flow finishes, supporting the report’s explanation of password protection, MFA, and authenticated session establishment. |
| **Figure 4. Secure submission sequence** | `docs/pack-04/uml/sequence-submission-secure-pack04.puml` | Section 4: Secure System Design | This figure is the strongest interaction-level diagram for the submission workflow because it ties together hashing, signature verification, encrypted-at-rest storage, and audited plaintext retrieval. |
| **Figure 5. Grade integrity sequence** | `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml` | Section 4: Secure System Design | This figure shows that grade handling is treated as an integrity-sensitive workflow with role restrictions, verified-submission gating, and HMAC-backed audit recording. |
| **Figure 6. Current-state level-1 DFD** | `docs/pack-09/uml/dfd-level-1-current-state.puml` | Section 4 or Section 8 | This DFD gives the clearest current-state view of how sensitive data moves through EduSecure, where cryptographic controls are applied, and how metadata, ciphertext, grades, and audit records are separated. |

## 2. Recommended usage pattern by section

### Section 4: Secure System Design

Use the strongest design-story sequence here:
- `docs/pack-02/uml/deployment-insecure.puml`
- `docs/pack-02/uml/deployment-secure.puml`
- `docs/pack-02/uml/sequence-login-secure.puml`
- `docs/pack-04/uml/sequence-submission-secure-pack04.puml`
- `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`

### Section 8: Technical Artefact Summary

Use the current-state DFD here if you want one implementation-facing figure that complements the interaction diagrams:
- `docs/pack-09/uml/dfd-level-1-current-state.puml`

## 3. Optional seventh figure

If the report still has space and a data-model figure would strengthen the discussion, the best optional addition is:

| Suggested figure label | Source file | Best report section | Commentary |
|---|---|---|---|
| **Optional Figure 7. Submission integrity class model** | `docs/pack-04/uml/class-diagram-submission-addendum.puml` | Section 4 or appendix | This class diagram is useful when the report needs to explain how `Submission`, `Grade`, `AuditLog`, verification state, and encrypted-at-rest metadata relate at the domain level. |

## 4. How to describe these diagrams safely

Use wording like:

> These UML and DFD artefacts represent the security design and main interaction logic of EduSecure. Current implementation-specific refinements are corroborated by the implementation evidence notes and automated tests.

Avoid wording like:
- "the diagrams exactly mirror the final codebase"
- "the diagrams prove production deployment"
- "the diagrams alone prove complete end-to-end encryption"

## 5. Deprecated / do not cite as active evidence

Do **not** use the following as an active report figure:

| File | Status | Why it should not be cited |
|---|---|---|
| `docs/pack-05/uml/sequence-aes-secure-transmission-demo.puml` | **Superseded / historical only** | `docs/pack-09/uml-refresh-assessment.md` explicitly marks this diagram as superseded because the standalone transport-demo flow was removed from the codebase. |

## 6. Practical figure-order recommendation

If you want a clean report flow, present the diagrams in this order:

1. insecure deployment baseline
2. secure deployment comparison
3. secure login sequence
4. secure submission sequence
5. grade integrity sequence
6. current-state level-1 DFD
7. optional class diagram in appendix or later in Section 4

That order gives the marker a clear progression from insecure baseline to secured architecture, then to the most important protected workflows, and finally to the implemented current-state data movement view.

