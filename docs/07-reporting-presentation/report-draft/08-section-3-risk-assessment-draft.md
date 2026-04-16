# Section 3 Draft — Risk Assessment

This file is a **report-ready working draft** for Section 3.

Use it together with:
- `docs/01-governance-risk-traceability/risk-methodology.md`
- `docs/01-governance-risk-traceability/risk-register-refined.md`
- `docs/01-governance-risk-traceability/risk-register.md`
- `docs/01-governance-risk-traceability/cvss-risk-register.md`
- `docs/07-reporting-presentation/report-draft/01-methodology-report-ready-paragraph.md`
- `docs/07-reporting-presentation/report-draft/02-methodology-comparison-table.md`
- `docs/07-reporting-presentation/report-draft/04-report-visual-allocation-plan.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`
- `docs/07-reporting-presentation/report-draft/06-report-core-vs-appendix-evidence-budget.md`

## How to use this draft

- keep the prose concise and analytical rather than turning Section 3 into a full register dump
- keep only the **methodology table** and **prioritised risk table** in the main body unless more space remains
- keep the risk discussion focused on the highest-value entries rather than every risk in the register
- treat secondary risks and expanded CVSS detail as appendix-first material if word pressure rises
- merge the strongest final text into `docs/07-reporting-presentation/final-report-draft-sections-1-to-5.md`

## 3. Risk Assessment

## 3.1 Chosen assessment method

EduSecure uses a simplified `NIST SP 800-30`-style risk assessment because it is structured, academically recognisable, and appropriate for a study-project artefact. The method identifies key assets, maps vulnerabilities to threat events, estimates likelihood and impact, prioritises treatment, and records residual risk. This gives the report a clearer analytical structure than an informal list of security problems, because each later control choice can be linked back to a defined risk.

The risk section is strengthened further by using complementary frameworks rather than relying on one method alone. OWASP provides the web-application framing for common risk categories, while CVSS v3.1 helps prioritise the technical seriousness of the most important scenarios. This means NIST supplies the main assessment structure, OWASP improves application-security interpretation, and CVSS supports severity-focused prioritisation without replacing the broader risk method.

[TABLE PLACEHOLDER — Table X. Security methodologies used in the report
Source: `docs/07-reporting-presentation/report-draft/02-methodology-comparison-table.md`
Purpose: summarise the roles of OWASP, NIST SP 800-30, CVSS, STRIDE, and CIS in the report methodology
Placement: immediately after the methodology introduction
Priority: core
Status: ready to adapt]

The table should compress the methodology explanation rather than duplicate it. In the final report, one sentence after the table should explain why NIST remains the primary risk method while the other frameworks play supporting roles.

## 3.2 Key assets

The most important assets in EduSecure are user credentials, session/authentication state, assignment submissions, grades, and audit records. Credentials and session state matter because compromise can lead directly to unauthorised access. Submissions and grades matter because the assignment brief is centred on tampering, disputed authorship, and academic trust. Audit records are also sensitive assets because, without trustworthy logging, later investigation of sensitive actions such as grading or content access becomes weak or impossible.

## 3.3 Main vulnerabilities and threat events

The main threat conditions are already visible in both the assignment brief and the refined register: plaintext password exposure, interception of credentials or tokens over unprotected transport, submission tampering or weak authorship proof, and untraceable sensitive actions such as grade changes. These risks should not be rated unrealistically low because they are explicitly embedded in the EduSecure case study. They also explain why later sections emphasise password hashing, TLS-oriented transport design, signature-backed submission integrity, and HMAC-backed audit accountability rather than a broader but less coherent control set.

[TABLE PLACEHOLDER — Table X. Prioritised EduSecure risk summary
Source: writer-created summary based on `risk-register-refined.md` and `cvss-risk-register.md`
Purpose: compress the most important assets, threat events, priorities, and selected controls into one main-body table
Placement: after the key asset and threat-event discussion
Priority: core
Status: not inserted yet]

## 3.4 Prioritised risk discussion

### Risk 1 — Credential disclosure from weak password storage

The first critical risk is direct credential disclosure following database compromise. In the refined register, this appears as `R1`, where the protected asset is user credentials and the weakness is plaintext password storage. The likelihood and impact are both high because disclosure is immediate if the verifier is stored unsafely.

[LOGIC PLACEHOLDER — R1 formal reasoning
Symbols: `D` = database compromise, `P` = plaintext password storage, `B` = bcrypt storage, `C` = direct credential disclosure
Formal statements: `(D ∧ P) -> C`; `(D ∧ B) -> ¬C`
Plain-English interpretation: if the database is breached and passwords are plaintext, reusable credentials are exposed directly; if bcrypt is used instead, plaintext disclosure does not follow directly
Residual risk: phishing, password reuse, and weak user-chosen passwords remain possible]

This is a strong first risk to discuss because it maps directly to one of the clearest incidents in the brief and justifies `bcrypt` as a primary control rather than a generic improvement.

### Risk 2 — Interception of credentials or tokens over unprotected transport

The second critical risk is interception of login traffic or session material over insecure transport, represented as `R2` in the refined register. This is especially important because the brief explicitly describes token interception over public Wi‑Fi. Even strong backend logic is weakened if credentials or sessions are exposed in transit.

[LOGIC PLACEHOLDER — R2 formal reasoning
Symbols: `N` = no TLS protection, `T` = TLS protection, `I` = useful interception succeeds
Formal statements: `N -> higher(I)`; `T -> lower(I)`
Plain-English interpretation: if transport is unprotected, interception becomes much easier; if TLS is applied, confidentiality and integrity in transit reduce that risk
Residual risk: infected endpoints, stolen sessions after login, or unsafe deployment can still weaken the posture]

This risk is useful in the report because it links the case-study problem directly to the secure deployment narrative and makes clear why TLS should be treated as a serious control even when it is deployment-oriented rather than code-only evidence.

### Risk 3 — Submission tampering or weak authorship proof

The third critical risk is that a student submission may be modified before lecturer review or become difficult to attribute credibly to its author. In the refined register, this is `R4`, and it is central to the academic-integrity theme of the whole project. Without integrity checking and a stronger authorship mechanism, lecturers cannot rely on the authenticity of the work they are grading.

[LOGIC PLACEHOLDER — R4 formal reasoning
Symbols: `H` = file hash is verified, `S` = digital signature is verified, `W` = tampering or authorship dispute succeeds
Formal statements: `¬H -> higher(W)`; `(H ∧ S) -> lower(W)`
Plain-English interpretation: without integrity verification, tampering or disputed authorship is harder to detect; when both the digest and signature are verified, confidence in integrity and authorship improves materially
Residual risk: the bounded signing model still depends on the security of the signing key and should not be overclaimed as full enterprise PKI]

This is one of the strongest risks to foreground because it justifies both the digest/signature workflow and the report’s later emphasis on integrity as the most central property in the artefact.

### Risk 4 — Untraceable sensitive actions and grade-related misuse

The fourth high-value risk is that sensitive academic actions, especially grade creation or modification, may occur without trustworthy investigation evidence. In the refined register, this appears as `R5`. Even if access controls exist, the absence of trustworthy logging weakens accountability and makes disputes harder to resolve.

[LOGIC PLACEHOLDER — R5 formal reasoning
Symbols: `A` = audit record exists, `M` = audit integrity is protected, `U` = unauthorised modification cannot be reliably investigated
Formal statements: `¬A -> higher(U)`; `(A ∧ M) -> lower(U)`
Plain-English interpretation: if sensitive actions are not logged, later investigation is weak; if they are logged and the log integrity is protected, accountability and tamper detection improve
Residual risk: a sufficiently privileged attacker may still attempt suppression or coordinated misuse]

This risk is especially useful because it connects access control, audit design, and HMAC-backed integrity into a single accountability argument rather than treating logging as a secondary implementation detail.

## Mini-conclusion

Overall, the EduSecure risk assessment shows that the highest priorities are credential protection, secure transport, submission integrity/authorship, and accountable handling of grade-sensitive actions. These priorities explain why later sections emphasise `bcrypt`, TLS-oriented deployment design, `SHA-256` plus ECC-based signing, and `HMAC-SHA-256` audit integrity. They also show why the report should remain tightly scoped: a smaller set of well-justified, well-evidenced controls is academically stronger than a broader but weakly defended security narrative.

## Trim-first notes if the word count becomes tight

Cut in this order:
1. the extra explanatory sentence after the methodology table
2. repeated asset descriptions once the prioritised risk table is present
3. the weakest of the four risk discussions if another section becomes longer than expected
4. expanded CVSS discussion beyond one short prioritisation sentence

Keep until the end:
- the methodology comparison table placeholder
- the prioritised risk table placeholder
- at least three high-value risks with concise logic reasoning
- residual-risk statements for each retained major risk
- the mini-conclusion that links risk priorities to later control selection

