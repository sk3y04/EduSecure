# Report Core vs Appendix Evidence Budget

This note helps decide what should stay in the **main report body** and what should move into the **appendix** so the final submission stays readable and within the word limit.

## 1. Main-body principle

Keep in the main body only the visuals that are necessary to support the argument of the section.

A visual belongs in the main report if it does at least one of these jobs:
- directly proves a central claim
- replaces a paragraph that would otherwise be long and repetitive
- helps the marker understand a design or workflow quickly
- anchors a discussion that would be weak without visible evidence

## 2. Appendix principle

Move a visual to the appendix if it is mainly:
- supporting evidence rather than core argument
- a second example of something already shown once
- too detailed for the flow of the section
- long, repetitive, or mainly useful for auditability rather than explanation

## 3. Recommended split by visual type

| Visual type | Keep in main body when... | Move to appendix when... |
|---|---|---|
| UML or DFD diagrams | the section is explaining secure design or trust boundaries | the figure is supplementary or repeats an already stronger figure |
| Comparison tables | they replace multiple sentences of explanation | the same point is already made clearly in prose |
| Risk tables | they summarise prioritisation or methodology | they reproduce the full register rather than a compact summary |
| Code screenshots | they prove a very specific implementation detail in one glance | they are long, dense, or mainly a record of source code that is already cited |
| Frontend screenshots | they show a visible security workflow or evidence screen | they are mostly cosmetic or duplicate another screen |
| Terminal/test output screenshots | they show successful execution evidence succinctly | they are long, repetitive, or better represented by a short citation |
| Browser devtools screenshots | they support cookie/CSRF/CORS discussion directly | they are interesting but not central to the main argument |

## 4. Recommended main-body set

If the final report needs a disciplined evidence budget, keep roughly this set in the core report:

### Section 2
- one cryptographic primitive comparison table

### Section 3
- one methodology comparison table
- one prioritised risk summary table

### Section 4
- secure deployment comparison diagram
- secure login sequence diagram
- secure submission sequence diagram
- grade-integrity sequence diagram

### Section 5
- one algorithm comparison and selection table

### Section 6
- one key/secret separation table

### Section 7
- one CIA summary table

### Section 8
- one implementation evidence summary table
- one frontend authentication or MFA screenshot
- one frontend submission or grade evidence screenshot
- one concise code/test evidence screenshot

## 5. Recommended appendix set

Place these in the appendix first if you need to trim the main report:
- security-focused use-case diagram
- insecure deployment baseline diagram
- current-state DFD if the main text already explains the architecture clearly
- browser devtools cookie screenshot
- extra frontend screens beyond the strongest two
- longer code artefact screenshots
- additional test-output screenshots
- expanded risk tables or full risk-register extracts
- optional control-to-risk mapping table

## 6. Paragraph-to-visual discipline

A good pattern is:
- one paragraph introduces the point
- one visual supports it
- one sentence interprets the visual

If the prose still needs another full paragraph to explain the same thing, the visual may not be carrying enough value.

## 7. Final compression rule

If the report is over the word limit or too visually crowded, remove items in this order:
1. extra frontend screenshots
2. browser devtools screenshots
3. optional diagrams
4. optional summary tables that repeat prose
5. supplementary code artefact screenshots

Keep until the end:
- Section 3 methodology/risk tables
- Section 4 core workflow diagrams
- Section 5 comparison table
- Section 8 strongest evidence screenshots

## 8. Final submission checklist

Before final assembly, ask:
- Is every main-body visual referenced in the surrounding text?
- Does every visual have a short caption and source basis?
- Is any screenshot included only because it looks good rather than because it proves something?
- Could any repeated evidence move to the appendix without weakening the argument?
- Does the final balance still look like a report rather than a slide deck?

