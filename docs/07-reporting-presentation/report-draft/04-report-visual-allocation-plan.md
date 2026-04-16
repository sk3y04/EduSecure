# Report Visual Allocation Plan

This note explains **where** diagrams, tables, code screenshots, and frontend screenshots should appear in the final report so that they reduce word pressure instead of increasing it.

Use it together with:
- `docs/07-reporting-presentation/high-mark-report-blueprint.md`
- `docs/07-reporting-presentation/report-diagram-figure-map.md`
- `docs/07-reporting-presentation/report-section-to-evidence-map.md`
- `docs/07-reporting-presentation/presentation-evidence-appendix.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`

## 1. Visual strategy for staying within the word limit

The report should not try to prove every claim with a full paragraph.

Instead, use visuals to do one of four jobs:
1. **replace long explanation** with one compact diagram or table
2. **summarise comparisons** that would otherwise take many sentences
3. **show concrete implementation evidence** without turning the report into a code dump
4. **move overflow detail into the appendix** while keeping the main report readable

## 2. Core rule

For each major section, prefer:
- **one main argument block** in prose
- **one supporting visual** if it removes repetition
- **at most one secondary visual** in the main body unless the section is strongly architecture- or evidence-driven

The sections where multiple visuals are justified are:
- **Section 4** because it is the design-story section
- **Section 8** because it is the artefact-evidence section

Additional rule after the encrypted-chat implementation was added:
- if space-chat E2EE is one of the report’s differentiators, reserve either **one Section 4 sequence figure** or **one Section 8 screenshot/evidence capture** for it; do not omit the feature entirely from both sections

## 3. Section-by-section visual placement

| Section | Recommended visual budget in main body | Best visual types | Why they belong there | Keep out of main body if word pressure is high |
|---|---:|---|---|---|
| Section 1. Introduction | 0 to 1 | optional very small context figure or no visual | introduction should stay short; visuals are usually not needed here | frontend screenshots, large architecture diagrams |
| Section 2. Role of cryptography | 1 | one compact comparison table of cryptographic primitives and their roles | reduces repetitive explanation of hash vs HMAC vs signature vs encryption | multiple screenshots or detailed code artefacts |
| Section 3. Risk assessment | 1 to 2 | methodology table and compact prioritised risk table | these replace long narrative lists and make the risk method look structured | full risk-register screenshots |
| Section 4. Secure system design | 4 to 6 | UML/DFD figures, including encrypted-chat sequence if used | this is the strongest place for diagrams because the section explains architecture and secure workflows | large code screenshots |
| Section 5. Cryptographic controls and selection | 1 to 2 | algorithm comparison table and optional control-to-risk table | this section benefits from concise compare-and-select visuals | frontend screenshots |
| Section 6. Implementation plan and considerations | 0 to 1 | secret/key-handling table or one small code artefact screenshot | keeps the section engineering-focused without turning it into a build log | multiple code screenshots |
| Section 7. CIA evaluation | 0 to 1 | one control-by-CIA summary table | helps summarise a lot of evaluative content compactly | screenshots and repeated diagrams |
| Section 8. Technical artefact summary | 2 to 4 | evidence table, frontend screenshots, encrypted-chat screenshot, test/code artefact screenshot, optional DFD | this is the correct place for repository evidence and visible implementation proof | too many duplicate screenshots of similar pages |
| Section 9. Conclusion | 0 | normally no visual | the conclusion should be short and final | all figures and screenshots |

## 4. Recommended diagram placement

### Section 4 core figure set

Use these as the main-body diagram set if space allows:
1. `docs/02-architecture-crypto/uml/current-state/use-case-security-focused.puml`
2. `docs/02-architecture-crypto/uml/foundation/deployment-insecure.puml`
3. `docs/02-architecture-crypto/uml/foundation/deployment-secure.puml`
4. `docs/02-architecture-crypto/uml/foundation/sequence-login-secure.puml`
5. `docs/02-architecture-crypto/uml/submissions-audit/sequence-submission-secure-pack04.puml`
6. `docs/02-architecture-crypto/uml/grades-and-history/sequence-grade-integrity-secure-pack05.puml`
7. `docs/02-architecture-crypto/uml/academic-workflows/sequence-space-chat-e2ee-implemented.puml`

If the section becomes too heavy, keep these in the main body first:
- secure deployment comparison
- secure login sequence
- secure submission sequence
- grade-integrity sequence

Then choose **one** of these depending on emphasis:
- secure space-chat E2EE sequence if encrypted chat is discussed as a main differentiator
- or the current-state DFD if an overall architecture figure is more valuable

Move these out first if space becomes tight:
- security-focused use-case diagram
- insecure deployment baseline

## 5. Recommended table placement

### Best tables for the main report

- **Section 2:** cryptographic primitive purpose table
- **Section 3:** methodology comparison table and compact prioritised risk table
- **Section 5:** algorithm comparison and selection table
- **Section 6:** key/secret separation table
- **Section 7:** CIA summary table
- **Section 8:** implementation evidence summary table

Optional chat-specific add-on if the generic Section 5 table becomes too crowded:
- **Section 5 appendix or overflow table:** encrypted-chat cryptographic stack summary using `ECDH`, `HKDF-SHA-256`, `AES-GCM`, and SHA-256 fingerprints

Tables are especially useful because they compress content without needing long prose transitions.

## 6. Recommended screenshot placement

### Best main-body screenshot use

Only place screenshots where they prove a concrete implementation point that the text alone cannot show quickly.

### Screenshots best suited to Section 8

- login or MFA frontend screen
- submission detail screen showing integrity/confidentiality evidence fields
- grade panel or grade-related frontend evidence
- encrypted space-chat screen showing key setup, rekey-required state, or decrypted-message display
- terminal screenshot of targeted test success
- one short code artefact screenshot only if it proves a specific configuration or security control clearly

### Screenshots usually better moved to appendix

- multiple frontend pages showing similar layouts
- long source-code screenshots
- repetitive test output
- browser devtools screenshots unless the report explicitly discusses cookie/CSRF posture in detail

## 7. Paragraph-level placement rule

When placing a visual, insert it **immediately after the paragraph that introduces its purpose**.

Use this pattern:
1. paragraph states the claim
2. visual appears next
3. one short follow-up sentence interprets what the visual proves

Do not:
- stack several visuals before explaining them
- insert screenshots before the related argument appears
- place a figure at the end of a section without commentary

## 8. Safe distribution recommendation

A realistic main-body distribution is:
- Section 1: 0 visuals
- Section 2: 1 table
- Section 3: 2 tables
- Section 4: 4 to 5 figures
- Section 5: 1 table
- Section 6: 1 table or code artefact screenshot
- Section 7: 1 table
- Section 8: 3 to 4 visuals
- Section 9: 0 visuals

That gives a strong but still manageable report with approximately **12 to 14 main visuals**, while everything else can move to the appendix. If the count feels high, the first trade-off should be choosing either the chat sequence in Section 4 or the extra chat screenshot in Section 8, rather than keeping both.

## 9. Appendix guidance

Use the appendix for:
- extra UML figures not essential to the argument
- browser devtools captures
- additional frontend screens
- longer code artefact screenshots
- extended test outputs
- supplementary tables that support but do not drive the report argument

## 10. Final rule before inserting any visual

Ask four questions:
1. Does this visual replace or compress prose?
2. Does it support a claim in the exact paragraph beside it?
3. Is it stronger than simply citing a file or test?
4. If removed, would the main argument become weaker?

If the answer to question 4 is no, move it to the appendix.

