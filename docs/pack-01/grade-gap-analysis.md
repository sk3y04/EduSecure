# Grade-Focused Brief Audit

This document audits the current EduSecure proposal and `docs/pack-01/` against `docs/assignment_brief.md`.

## Verdict

The current proposal is a **strong and correct foundation**, but it is **not yet sufficient on its own to claim exceptional / highest-grade coverage**.

It successfully establishes:
- assignment alignment
- sensible scope control
- reference-project reuse boundaries
- an initial risk register
- a UML plan
- a documentation-first workflow

However, several brief-critical deliverables are still only planned, not yet documented in enough depth to support top-band performance.

## Coverage summary

| Brief area | Status | Audit note |
|---|---|---|
| Cryptographic role analysis | Partial | The pack frames the role of cryptography well, but does not yet contain the actual analytical discussion expected in the report. |
| Risk assessment | Partial-Strong | Assets, threats, vulnerabilities, and mitigation direction are present in `risk-register.md`, but the register is still first-pass and needs likelihood/impact reasoning depth and clearer prioritisation rationale. |
| Recognised standard usage | Partial | `risk-register.md` states NIST SP 800-30 style wording, but does not yet explicitly explain the method, assessment criteria, or risk-scoring approach in a report-ready way. |
| Propositional logic requirement | Partial | Logic-style statements exist, but they are currently brief heuristics rather than formalised propositions with explicit variables and reasoning steps. |
| Secure system design with UML | Partial | `uml-documentation-plan.md` is good, but actual diagrams do not yet exist. The brief expects sequence-diagram evidence, especially insecure vs secure comparisons. |
| Cryptographic controls and selection justification | Partial | The intended controls are identified, but the algorithm comparison and final justification document are not yet written. |
| Implementation plan and considerations | Partial | Scope and direction are present, but there is not yet a dedicated implementation-plan document covering libraries, random number generation, nonce reuse, key distribution, and error handling. |
| CIA evaluation | Partial | CIA mapping appears in `assignment-traceability.md`, but a full CIA evaluation document is still missing. |
| Technical artefact planning | Strong as planning | The target artefact set is sensible and aligned, but there is no actual implementation or detailed artefact specification yet. |
| Submission/report requirements | Weak-Partial | The pack does not yet cover title page, citation strategy, evidence handling, screenshot plan, or report structure. |
| Academic integrity/originality | Strong | The reuse matrix and instructions handle this well. |

## What is already very good

## 1. The proposal is aligned to the real nature of the assignment

The project is correctly positioned as a **cryptography case study** rather than a generic full-stack application. This is one of the strongest current decisions.

## 2. Scope control is appropriate

`scope-assumptions.md` correctly keeps the project small and reportable rather than over-engineered.

## 3. The reuse policy is academically safe

`reference-project-reuse-matrix.md` clearly distinguishes structural reuse from domain copying, which is important for originality.

## 4. The selected security directions fit the brief

The current intended controls match the incidents and requirements in the brief well:
- `bcrypt` for passwords
- `AES` for confidentiality demonstrations
- `SHA-256` / `HMAC` for integrity
- `RSA` or `ECC` for digital signatures

## 5. The UML planning is now strong

The plan includes:
- use case diagram
- component diagram
- class diagram
- ERD/domain model
- sequence diagrams
- insecure/secure deployment diagram

This is a strong structure for the report.

## Highest-mark risks in the current documentation

## 1. The pack does not yet contain the actual cryptographic comparison section

The brief specifically expects comparison and justification of:
- AES
- RSA
- ECC
- SHA-256
- bcrypt
- HMAC

At the moment, this is only referenced as a future document. For high marks, this needs to exist explicitly.

## 2. The formal logic requirement is not yet strong enough

The current logic statements are useful, but for a stronger academic showing they should be rewritten in a more formal style, for example:
- define symbols
- state propositions
- show implication or conjunction clearly
- connect the proposition to the mitigation claim in plain English

## 3. The risk methodology is not yet report-ready

The pack says it uses NIST SP 800-30 style wording, but a top-grade report normally needs:
- an explicit method statement
- risk criteria definitions
- explanation of how likelihood and impact were determined
- clear prioritisation rationale

## 4. Actual UML deliverables are still missing

A plan is not the same as evidence. The brief will ultimately reward the actual diagrams, especially secure vs insecure sequence diagrams.

## 5. CIA evaluation is only sketched

The current CIA table is a useful seed, but a top-grade report usually needs a deeper evaluation of:
- what each control protects
- what limitations remain
- what trade-offs are accepted in a study-project implementation

## 6. The implementation considerations section is still too thin

The brief explicitly mentions implementation challenges such as:
- random number generation
- nonce reuse
- key distribution

These are not yet properly documented in Pack 01.

## 7. Report-construction requirements are under-documented

The brief also requires or implies:
- proper scholarly citation
- title page information
- ethically sound framing
- readable, well-commented artefact
- screenshots if needed

These are not yet captured in the planning pack.

## 8. Availability is the least developed part of the CIA narrative

Confidentiality and integrity are well represented. Availability is currently present only at a high level. Even for a study project, the report should say what practical availability measures are in scope and what is intentionally excluded.

## Is anything currently incorrect?

Nothing in the current pack is fundamentally wrong, but a few areas should be tightened.

### 1. `bcrypt` and `salt`
The UML class plan mentions optional `salt`. In practice, modern password encoders like `bcrypt` store the salt within the hash format automatically. This is not wrong, but the later documentation should avoid implying that a separate application-managed salt field is always required.

### 2. Data-at-rest wording
The deployment plan refers to AES-based protection for sensitive data at rest. This is fine for the assignment, but later documents should be careful not to overclaim whole-database encryption unless that is genuinely what the artefact implements. It may be more accurate to describe encryption of selected sensitive records/files.

### 3. JWT scope
The current pack handles JWT responsibly, but later report text must keep repeating that JWT is not a confidentiality mechanism.

## Coverage by current Pack 01 files

| File | Value | Limitation |
|---|---|---|
| `receipt+plan.md` | Good high-level orientation | Not a full proposal document |
| `assignment-traceability.md` | Strong mapping foundation | Several rows point to future docs rather than present evidence |
| `scope-assumptions.md` | Very good scope control | Needs more explicit implementation constraints and limitations later |
| `reference-project-reuse-matrix.md` | Strong originality and reuse policy | Not directly mark-bearing by itself |
| `risk-register.md` | Good first-pass register | Needs methodology depth and stronger formal logic treatment |
| `uml-documentation-plan.md` | Strong plan for evidence production | Actual diagrams still absent |
| `agent-instructions.md` | Strong process control | Internal guidance, not report evidence on its own |

## Overall judgement

### Is the proposal correct?
Yes. The proposal direction is correct.

### Is the first documentation pack strong?
Yes. It is a strong Pack 01 and a good foundation.

### Does it already cover everything exceptionally for the highest grade?
No, not yet.

The key reason is that Pack 01 is still mainly a **planning baseline**, while the assignment rewards a **full analysis, explicit justification, and actual design evidence**.

## Priority next documents needed before coding proceeds far

1. **Cryptographic decision matrix**
   - compare AES, RSA, ECC, SHA-256, bcrypt, HMAC
   - justify final selections

2. **Risk methodology and refined risk register**
   - explicit NIST SP 800-30 method statement
   - refined likelihood/impact rationale
   - stronger propositional logic formatting

3. **CIA evaluation document**
   - confidentiality, integrity, availability mapped per control
   - limitations and residual risks

4. **Implementation plan and considerations**
   - libraries
   - random number generation
   - nonce/IV handling
   - key distribution/storage assumptions
   - secure error handling

5. **Actual UML deliverables**
   - use case
   - deployment insecure/secure
   - class diagram
   - sequence diagrams

6. **Report assembly plan**
   - section structure
   - reference/citation strategy
   - evidence and screenshot plan
   - title page checklist

## Bottom-line recommendation

Treat Pack 01 as **excellent groundwork**, not as final top-band documentation.

If the next phase fills the missing analytical documents and produces actual UML artefacts before serious coding begins, the project will be much closer to a highest-grade path.

