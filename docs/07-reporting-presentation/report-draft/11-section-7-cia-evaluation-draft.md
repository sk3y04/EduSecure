# Section 7 Draft — CIA Evaluation

This file is a **report-ready working draft** for Section 7.

Use it together with:
- `docs/02-architecture-crypto/cia-evaluation.md`
- `docs/04-evidence-testing/testing-support/final-implementation-evidence-map.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/07-reporting-presentation/report-section-to-evidence-map.md`
- `docs/07-reporting-presentation/report-claims-audit-note.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`
- `docs/07-reporting-presentation/report-draft/04-report-visual-allocation-plan.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`
- `docs/07-reporting-presentation/report-draft/06-report-core-vs-appendix-evidence-budget.md`

## How to use this draft

- keep the section evaluative rather than descriptive
- keep only the **CIA contribution summary table** in the main body unless more space remains
- use each subsection to assess what improved, what remained limited, and why that matters
- avoid repeating the full Section 4 design story or Section 5 algorithm comparison
- merge the strongest final text into `docs/07-reporting-presentation/final-report-draft-sections-6-to-9.md`

## 7. CIA Evaluation

The selected EduSecure controls can only be evaluated strongly through the CIA model if each one is tied to a specific protection goal, a realistic limitation, and a residual-risk statement. In the current artefact, the strongest results are achieved in **integrity**, followed by **confidentiality**, while **availability** is treated in a more bounded and honest way. This balance is appropriate because the assignment brief is centred on tampered submissions, altered grades, weak authentication trust, and missing accountability rather than on enterprise-scale resilience.

[TABLE PLACEHOLDER — Table X. CIA contribution summary of implemented controls
Source: writer-created summary based on `docs/02-architecture-crypto/cia-evaluation.md` and `docs/04-evidence-testing/testing-support/final-implementation-evidence-map.md`
Purpose: compress which implemented controls mainly support confidentiality, integrity, or availability and what limitation remains for each
Placement: after the opening evaluation paragraph
Priority: core
Status: not inserted yet]

The table should summarise the control groupings rather than replace the final judgement. In the final report, one short sentence after the table should explain that the overall CIA profile is strongest in integrity, then confidentiality, with availability deliberately bounded by project scope.

## 7.1 Confidentiality

Confidentiality in EduSecure is improved through a layered combination of controls rather than a single encryption claim. `bcrypt` protects stored password verifiers from direct plaintext disclosure after database compromise, AES-GCM protects recoverable MFA secrets at rest, submission content is encrypted at rest before durable storage, and the encrypted space-chat workflow stores browser-produced ciphertext envelopes in MongoDB instead of routine plaintext message bodies. In addition, the submission metadata/content split reduces unnecessary exposure because normal review flows do not automatically return plaintext submission content, while role-based access control limits who can view sensitive academic records.

The encrypted-chat contribution is especially useful in this section because it protects a different asset class from the earlier controls: collaboration content. However, the report must state the limits clearly. The backend still sees metadata such as sender, space, time, and plaintext length, and the browser-served delivery model means frontend integrity remains part of the trust assumption. The transport story also contributes to confidentiality, but it must be phrased carefully. TLS 1.3 via Certbot/Let’s Encrypt is the intended deployment-side control for protecting credentials, cookies, and API traffic in transit, yet it should remain a design/deployment claim unless separate HTTPS evidence is included. This means the safest overall confidentiality judgement is that EduSecure materially improves confidentiality for several important assets, but does so through bounded, specifically evidenced mechanisms rather than a blanket claim that the whole system or database is encrypted.

## 7.2 Integrity

Integrity is the strongest and most central property in the current artefact, which is appropriate because the brief explicitly highlights tampered submissions, altered grades, and missing verification for sensitive actions. EduSecure improves integrity through several connected controls: `SHA-256` digest generation for submission content, ECC-based signing and verification logic, verified-submission-only grading, role-restricted grade handling, and HMAC-backed audit integrity values for sensitive actions. These mechanisms strengthen not only tamper detection, but also confidence that academic actions occur within a traceable and reviewable workflow.

This integrity story is especially strong because the controls reinforce one another. Submission metadata can show digest, signature, and verification state without automatically exposing plaintext content; plaintext retrieval becomes a separate audited event; grades depend on prior verification state; and grade-sensitive actions are recorded with protected audit integrity. A useful overall judgement is therefore that EduSecure treats integrity as the core trust property of the artefact rather than as a secondary feature, which makes this one of the strongest sections in the final report.

## 7.3 Availability

Availability is addressed more modestly, and that is acceptable provided the report explains the boundary honestly. In EduSecure, availability is supported less by cryptographic primitives themselves and more by maintainable architecture, bounded scope, persistent storage, health-check behaviour, and reproducible schema delivery. Using a small, testable backend structure, versioned Liquibase changesets, and a focused PostgreSQL verification path makes the artefact easier to complete, explain, and run reliably within study-project constraints.

At the same time, availability remains intentionally limited. EduSecure does not claim enterprise high availability, disaster recovery, or production-grade operational resilience. Some security controls also introduce normal trade-offs: MFA strengthens authentication integrity, but can reduce ease of access if the user loses their authenticator device or recovery codes. The most accurate evaluation is therefore that availability is **adequate but intentionally bounded**, which is stronger academically than pretending the artefact solves resilience at an enterprise level when the repository does not prove that.

## 7.4 Residual limitations

The CIA evaluation is strongest when it acknowledges what the controls do **not** solve. `bcrypt` does not stop phishing or password reuse. The ECC signing workflow remains a bounded study-project simulation rather than a user-held PKI deployment. HMAC-backed audit integrity improves accountability, but does not eliminate all insider misuse risk. TLS is an intended deployment control, not automatically repository-proven runtime enforcement. Similarly, the repository proves protection of selected secrets and files at rest, but not whole-database encryption or full operational hardening of every environment.

These limitations do not weaken the report if they are presented properly. On the contrary, they make the evaluation more credible by showing that EduSecure improves confidentiality and integrity materially while leaving some operational and deployment issues outside the implemented scope. The encrypted-chat feature is a strong example of this pattern: it materially reduces routine backend plaintext exposure for one workflow without pretending to solve metadata secrecy, malicious frontend delivery, or seamless multi-device recovery.

## Mini-conclusion

Overall, EduSecure’s CIA profile is well aligned to the assignment. Integrity is the strongest and most developed property because it directly addresses submission tampering, grade integrity, and audit trust. Confidentiality is also materially improved through password protection, AES-GCM-based protection of selected secrets and stored submission content, browser-side encrypted space chat, controlled retrieval paths, and transport-design planning. Availability is adequate but deliberately limited to a study-project scope. This is an academically defensible outcome because the artefact prioritises the properties most relevant to the brief rather than making weak claims about enterprise-complete coverage.

## Safe wording reminders for this section

Prefer wording such as:
- "Integrity is the strongest and most central property in the current artefact..."
- "Confidentiality is materially improved through specific evidenced controls..."
- "Availability is adequate but intentionally bounded..."
- "Within the study-project scope..."
- "TLS remains the intended deployment-side transport control unless separate HTTPS evidence is included..."

Avoid wording such as:
- "the platform is fully secure"
- "the database is encrypted" unless the exact mechanism is evidenced
- "end-to-end encryption is proven"
- "chat metadata is hidden from the server"
- "the ECC workflow provides complete enterprise non-repudiation"
- "availability is fully solved"
- "the repository proves production deployment maturity"

## Trim-first notes if the word count becomes tight

Cut in this order:
1. repeated examples within the confidentiality paragraph once the CIA table is inserted
2. repeated explanation of why integrity is strongest after one clear judgement sentence
3. extended availability trade-off detail beyond the MFA example
4. repeated residual-limitations wording once one bounded paragraph remains

Keep until the end:
- the CIA contribution summary table placeholder
- one clear integrity judgement paragraph
- one clear confidentiality paragraph with precise boundaries
- the bounded availability paragraph
- the residual-limitations paragraph that keeps the section honest

