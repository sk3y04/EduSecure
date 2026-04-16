# Final Report Merge and Assembly Checklist

This file is the final assembly guide for turning the `report-draft` workspace into one finished report document.

Use it together with:
- `docs/07-reporting-presentation/final-report-skeleton.md`
- `docs/07-reporting-presentation/final-report-draft-sections-1-to-5.md`
- `docs/07-reporting-presentation/final-report-draft-sections-6-to-9.md`
- `docs/07-reporting-presentation/report-assembly-plan.md`
- `docs/07-reporting-presentation/high-mark-report-blueprint.md`
- `docs/07-reporting-presentation/report-claims-audit-note.md`
- `docs/07-reporting-presentation/final-submission-checklist.md`
- `docs/07-reporting-presentation/report-draft/04-report-visual-allocation-plan.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`
- `docs/07-reporting-presentation/report-draft/06-report-core-vs-appendix-evidence-budget.md`

## 1. Section merge order

Draft in the order that best supports reasoning, but assemble in the final report order.

### Best drafting order
1. Section 5
2. Section 3
3. Section 4
4. Section 6
5. Section 7
6. Section 8
7. Section 2
8. Section 1
9. Section 9

### Final assembly order
1. `14-section-1-introduction-draft.md`
2. `13-section-2-role-of-cryptography-draft.md`
3. `08-section-3-risk-assessment-draft.md`
4. `09-section-4-secure-system-design-draft.md`
5. `07-section-5-cryptographic-controls-selection-draft.md`
6. `10-section-6-implementation-plan-and-considerations-draft.md`
7. `11-section-7-cia-evaluation-draft.md`
8. `12-section-8-technical-artefact-summary-draft.md`
9. `15-section-9-conclusion-draft.md`

## 2. Merge targets

### Merge into `final-report-draft-sections-1-to-5.md`
- Section 1 introduction draft
- Section 2 role-of-cryptography draft
- Section 3 risk-assessment draft
- Section 4 secure-system-design draft
- Section 5 cryptographic-controls draft

### Merge into `final-report-draft-sections-6-to-9.md`
- Section 6 implementation draft
- Section 7 CIA evaluation draft
- Section 8 technical artefact summary draft
- Section 9 conclusion draft

## 3. Placeholder triage rules

Before final submission, every placeholder must fall into one of four states.

| Placeholder type | Action required before submission | Typical status |
|---|---|---|
| Core table placeholder | replace with a final table or consciously remove only if the text still works without it | should usually stay |
| Core figure placeholder | replace with the actual exported figure and caption if it is part of the main-body design/evidence set | should usually stay |
| Optional placeholder | keep only if it removes prose or materially strengthens the argument | cut first under pressure |
| Appendix-first placeholder | move to appendix or delete from main body | should rarely stay in core report |

## 4. Main-body visual set to prefer

If you need a disciplined final set, keep approximately this main-body visual budget:
- Section 2: one primitives-role table
- Section 3: methodology comparison table and prioritised risk table
- Section 4: secure deployment comparison, secure login sequence, secure submission sequence, grade-integrity sequence
- Section 5: one algorithm comparison and selection table
- Section 6: one key/secret separation table
- Section 7: one CIA contribution summary table
- Section 8: one implementation evidence summary table, one auth/MFA screenshot, one submission/grade screenshot, one concise evidence screenshot if space permits

## 5. Visuals to move out first

If the report becomes too long or visually crowded, move these out first:
1. Section 4 use-case diagram
2. Section 4 insecure deployment baseline
3. Section 4 optional DFD
4. Section 4 submission class diagram
5. Section 8 optional browser cookie/CSRF screenshot
6. Section 8 optional DFD
7. optional code/config screenshots in Section 6
8. optional control-to-risk table in Section 5

## 6. Section-by-section final checks

### Section 1
- keep it short
- keep it incident-driven
- state the bounded scope clearly
- end with a one-sentence roadmap only

### Section 2
- explain what each primitive is for
- do not turn this section into Section 5 comparison logic
- do not blur JWT with encryption
- do not blur TLS with AES-GCM-at-rest usage

### Section 3
- keep NIST as the main risk method
- use OWASP/CVSS as supporting lenses
- retain at least three strong risks if trimming
- keep residual risk statements

### Section 4
- interpret every figure briefly
- do not use diagrams without commentary
- keep the diagram set bounded
- keep TLS as intended deployment posture unless separately evidenced

### Section 5
- explain why the chosen control fits better than alternatives
- keep ECC and TLS wording bounded
- keep the control set coherent rather than overly broad

### Section 6
- explain engineering trade-offs
- keep production-hardening claims bounded
- keep the PostgreSQL evidence phrased as focused schema-delivery verification

### Section 7
- keep it evaluative rather than descriptive
- state clearly that integrity is strongest
- keep availability honest and bounded

### Section 8
- prove what is implemented
- distinguish implemented, partial, and future-scope features
- avoid turning the section into endpoint-by-endpoint narration

### Section 9
- do not introduce new analysis
- keep the ending confident but constrained
- retain one strong final marker-facing sentence

## 7. Honesty gates before export

Before final export, confirm that the report does **not** overclaim any of the following:
- production deployment maturity
- full PostgreSQL hardening
- whole-database encryption
- enterprise PKI
- complete audit-review tooling
- deployed HTTPS proof without separate deployment evidence
- complete LMS feature coverage

Safe rule:
- implementation evidence first
- exact code/test/docs second
- design/UML support after that

## 8. Caption and appendix pass

Before the final document is frozen:
- replace every `Figure X` and `Table X` with the final numbering
- make captions short and marker-facing
- ensure each figure/table is referenced in nearby prose
- label appendix visuals clearly as support material if they are not part of the main argument
- ensure screenshots have a short explanation of what they prove

## 9. Title page and submission pass

Confirm the final report includes:
- full name
- student number
- module code
- assignment title
- consistent citation style
- correctly referenced appendices if used
- no live placeholder blocks left anywhere in the document

## 10. Final readiness questions

Use this final gate before export:
- Does each major section answer a specific brief requirement directly?
- Does every strong implementation claim point to a real evidence source?
- Are cryptographic terms used correctly throughout?
- Are diagrams and screenshots helping reduce prose rather than repeating it?
- Is the report still within the word limit after visual insertion?
- Does the final document read like a report rather than a slide deck or changelog?
- Are the strongest pages still Sections 3, 4, and 5, as they should be for marks?

