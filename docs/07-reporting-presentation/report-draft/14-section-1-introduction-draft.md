# Section 1 Draft — Introduction

This file is a **report-ready working draft** for Section 1.

Use it together with:
- `docs/00-overview/assignment_brief.md`
- `docs/01-governance-risk-traceability/assignment-traceability.md`
- `docs/00-overview/platform-feature-matrix-and-prioritized-backlog.md`
- `docs/07-reporting-presentation/report-ready-section-1-and-8-scope-paragraphs.md`
- `docs/07-reporting-presentation/high-mark-report-blueprint.md`
- `docs/07-reporting-presentation/report-claims-audit-note.md`

## How to use this draft

- keep the introduction short and case-study specific
- do not open with generic statements about cybersecurity importance
- frame EduSecure as a bounded cryptography case study rather than a complete LMS
- finish with a one-sentence roadmap only
- merge the strongest final text into `docs/07-reporting-presentation/final-report-draft-sections-1-to-5.md`

## 1. Introduction

EduSecure is presented in the assignment brief as an online education platform handling user accounts, coursework submissions, grades, and other sensitive academic records. In that scenario, the platform also includes broader learning-management capabilities such as course registration, access to teaching materials, live virtual classrooms, communication, attendance, exam scheduling, and feedback workflows. However, the strongest implemented part of the current repository is not a complete end-to-end LMS. Instead, it is a security-focused coursework and academic-integrity core that concentrates on the protection of the most security-sensitive assets and actions.

The insecure baseline described in the brief contains several serious failures: passwords are stored in plaintext, communication is not protected properly in transit, assignment submissions can be tampered with before lecturer review, grades can be altered without trustworthy integrity checks, and sensitive actions lack reliable logging and verification. These weaknesses directly threaten confidentiality, integrity, and trust in academic processes. They are particularly serious in an education platform because compromised authentication, disputed authorship, manipulated grades, and missing accountability all undermine both system security and academic legitimacy.

This report therefore focuses on the repository areas where cryptographic controls have been implemented most clearly and evidenced most strongly. The current EduSecure artefact secures authentication through `bcrypt`, cookie-backed session handling, and optional TOTP-based MFA; protects submissions through `SHA-256` digest generation, ECC-based signature logic, and `AES-GCM` encrypted-at-rest storage; and strengthens grade accountability through owner-scoped authorisation and `HMAC-SHA-256` tamper-evident audit records. The wider platform brief remains important as the case-study context, but the implemented artefact should be understood as a bounded security-focused study project rather than a complete production-ready education platform. The report first explains the role of cryptography in EduSecure, then analyses the key risks, justifies the chosen controls, describes the secure design, and evaluates the implemented artefact against the assignment requirements.

## Safe wording reminders for this section

Prefer wording such as:
- "EduSecure is presented in the assignment brief as..."
- "The current repository implements the most security-sensitive subset..."
- "Within the study-project scope..."
- "The implemented artefact should be understood as..."

Avoid wording such as:
- "the whole platform is implemented"
- "production-ready"
- "complete LMS"
- "fully secure"
- "all brief features are delivered"

## Trim-first notes if the word count becomes tight

Cut in this order:
1. repeated examples of broader LMS features beyond the current core
2. repeated explanation of why academic integrity matters after one clear sentence remains
3. extra wording about scope once the bounded-artefact statement is clear

Keep until the end:
- the case-study framing sentence
- the list of core failures from the brief
- the bounded scope statement
- the one-sentence roadmap into the rest of the report

