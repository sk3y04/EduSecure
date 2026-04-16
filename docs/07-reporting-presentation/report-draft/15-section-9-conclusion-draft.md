# Section 9 Draft — Conclusion

This file is a **report-ready working draft** for Section 9.

Use it together with:
- `docs/02-architecture-crypto/cia-evaluation.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`
- `docs/07-reporting-presentation/report-claims-audit-note.md`
- `docs/07-reporting-presentation/high-mark-report-blueprint.md`

## How to use this draft

- keep the conclusion evaluative rather than analytical
- summarise the strongest implemented improvements only
- acknowledge the main limits clearly and briefly
- end with one confident but bounded final sentence
- merge the strongest final text into `docs/07-reporting-presentation/final-report-draft-sections-6-to-9.md`

## 9. Conclusion

EduSecure demonstrates how a cryptography-focused redesign can materially improve trust in an online education platform that originally suffered from plaintext password storage, insecure communication, tamperable submissions, altered grades, and missing accountability for sensitive actions. The implemented artefact does not rely on one security mechanism alone. Instead, it combines multiple cryptographic controls with supporting architectural decisions to address the most serious risks identified in the assignment brief.

The strongest implemented improvements are clear. `bcrypt` replaces plaintext password storage with an appropriate password-hashing mechanism, optional TOTP-based MFA strengthens authentication assurance, `SHA-256` digest generation and ECC-based signing strengthen submission integrity and authorship logic, `AES-GCM` protects MFA secrets and stored submission content at rest, and `HMAC-SHA-256` supports tamper-evident audit records for sensitive actions. Together, these measures make the artefact strongest in integrity, followed by confidentiality, while availability is addressed more modestly through bounded scope, maintainable architecture, and reproducible schema delivery.

At the same time, the final evaluation must remain honest about the project’s limits. EduSecure does not claim production deployment maturity, a user-held PKI signing model, full public audit-review tooling, whole-database encryption, or enterprise-grade operational resilience. These are appropriate boundaries for a study-project artefact. In conclusion, EduSecure now addresses the brief’s core cryptographic problems through a layered set of implemented controls—password hashing, MFA hardening, digest/signature logic, AES-based confidentiality measures, and tamper-evident auditing—while remaining technically honest about the limits of its scope and evidence.

## Safe wording reminders for this section

Prefer wording such as:
- "EduSecure materially improves..."
- "The implemented artefact demonstrates..."
- "Within the study-project scope..."
- "The strongest implemented improvements are..."
- "Residual limitations remain because..."

Avoid wording such as:
- "fully secure"
- "production-ready"
- "complete non-repudiation"
- "enterprise PKI"
- "the platform is finished"

## Trim-first notes if the word count becomes tight

Cut in this order:
1. repeated listing of controls once the strongest set is stated once
2. repeated CIA wording after the integrity/confidentiality/availability sentence remains
3. extra limitation examples after the main boundary list is clear

Keep until the end:
- the overall improvement claim
- the strongest-controls sentence
- the bounded-limitations sentence
- the final marker-facing concluding sentence

