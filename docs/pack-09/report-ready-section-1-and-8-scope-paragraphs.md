# Report-Ready Section 1 and 8 Scope Paragraphs

This note provides ready-to-paste paragraph sets for the final report sections that most need careful scope wording:
- **Section 1: Introduction**
- **Section 8: Technical Artefact Summary**

The purpose is to help you describe the implemented repository honestly while still connecting it to the broader EduSecure platform scenario in the assignment brief.

## 1. Section 1 introduction paragraphs

### Option A: concise introduction with honest scope boundary

EduSecure is presented in the assignment brief as an online education platform used to manage student access, coursework, grades, and other sensitive academic information. In that scenario, the platform also includes wider learning-management features such as course registration, teaching-material access, video classrooms, communication, attendance handling, exam scheduling, and feedback processes. However, the strongest implemented part of the repository is not a full end-to-end learning-management system. Instead, it is a security-focused coursework and academic-integrity slice that concentrates on the protection of the most security-sensitive platform assets and actions.

The insecure baseline described in the brief contains multiple serious failures: passwords are stored in plaintext, communication is not protected properly in transit, student submissions can be tampered with before lecturer review, grades can be altered without trustworthy verification, and sensitive academic actions lack reliable logging and accountability. These weaknesses threaten confidentiality, integrity, and trust across the platform. They are particularly serious in educational systems because disputed authorship, manipulated grades, and weak authentication undermine both security and academic legitimacy.

This report therefore focuses on the repository areas where cryptographic controls have been implemented most clearly and evidenced most strongly. The current EduSecure artefact secures authentication through `bcrypt`, cookie-backed session handling, and optional TOTP-based MFA; protects assignment submissions through `SHA-256` digest generation, digital-signature verification logic, and `AES-GCM` encrypted-at-rest storage; and strengthens grade accountability through owner-scoped authorization and `HMAC-SHA-256` tamper-evident audit records. The wider platform brief remains important as the case-study context, but the implemented artefact should be understood as a bounded, security-driven coursework-management core rather than a complete LMS.

This report first analyses the EduSecure security problem, then justifies the selected cryptographic controls, explains the secure-system design, and evaluates the implemented artefact against the assignment requirements and the repository’s actual feature scope.

### Option B: slightly stronger marker-facing introduction

EduSecure is described in the assignment brief as an online education platform supporting students, lecturers, and administrators across teaching, submission, grading, and broader academic-management activities. In full scenario terms, this includes course registration, access to learning materials, live virtual classes, exam-related processes, and staff-student interaction. The current repository does not implement all of those platform capabilities. Instead, it implements the most security-critical subset: secure authentication, staff-managed academic spaces, assignment visibility, student submission handling, grade integrity, and tamper-evident auditing.

This bounded implementation choice is defensible because it aligns directly with the most serious incidents in the brief. The case study highlights plaintext password storage, intercepted login/session traffic, tampered submissions, altered grades, and missing verification for sensitive actions. These are fundamentally cryptographic and trust-boundary problems. As a result, the artefact prioritises controls that materially reduce those risks: password hashing with `bcrypt`, second-factor authentication with TOTP, submission digest and signature handling, `AES-GCM` protection of recoverable secrets and stored submission content, and `HMAC-SHA-256` integrity values for security-sensitive audit records.

The aim of this report is therefore not to claim that EduSecure is already a complete production-ready education platform. Rather, it is to show how a carefully selected set of cryptographic controls can strengthen the most integrity-sensitive and confidentiality-sensitive academic workflows within a realistic study-project scope. The report analyses the case-study risks, explains the secure design, justifies the algorithm choices, and evaluates the implemented artefact with explicit attention to what has been fully built, what is only partially covered, and what remains future work.

## 2. Section 8 technical artefact summary paragraphs

### Option A: artefact summary with explicit feature-boundary paragraph

The EduSecure technical artefact should be understood as a secure academic-workflow core rather than a complete implementation of every platform feature in the assignment scenario. The repository provides strong evidence for authentication hardening, staff-managed space or course-like membership, assignment visibility, student submission handling, grade integrity, and audit protection. By contrast, broader learning-management features such as live virtual classrooms, attendance recording, messaging, exam scheduling, and reusable feedback-form administration are not yet implemented as full repository features. This distinction is important because a technically honest artefact summary is stronger than an over-extended claim of complete platform delivery.

Within its implemented scope, the artefact exceeds the brief’s minimum technical expectation. The authentication slice demonstrates password protection with `bcrypt`, cookie-backed session establishment, CSRF-aware browser interaction, and optional TOTP-based MFA with recovery codes. The submission slice demonstrates `SHA-256` digest generation, digital-signature creation and verification logic, controlled plaintext retrieval, and `AES-GCM` encryption of submission content at rest. The grade slice demonstrates verified-submission-only grading, role-restricted lecturer/admin marking rights, student own-grade retrieval, and HMAC-backed tamper-evident audit records for sensitive actions. Together, these features show multiple distinct cryptographic techniques working in one coherent system rather than as disconnected laboratory exercises.

It is also important to distinguish what is fully implemented from what is only partially represented. EduSecure currently supports student access to assignments and grades through staff-managed space membership, which means it partially covers the brief’s ideas of course access and result visibility. However, there is no self-service student course-registration workflow, no dedicated learning-material repository for lecture notes or reusable teaching files, and no separate exam-results domain beyond assignment grading. The artefact therefore demonstrates a convincing secure coursework-management foundation, but not full feature parity with the entire education-platform scenario.

From a report perspective, this is still a strong technical artefact because it implements more than three required cryptographic areas and ties them directly to the brief’s highest-risk incidents. Passwords are no longer stored as plaintext verifiers, submissions gain tamper-evidence and protected-at-rest storage, grade changes become accountable, and authentication assurance is strengthened beyond a password-only baseline. The artefact is further supported by integration tests, frontend evidence pages, and implementation-status notes that document the repository’s current behavior in a form suitable for marker review.

### Option B: section 8 closing paragraph for feature coverage discussion

When discussing feature coverage in the artefact summary, the most accurate conclusion is that EduSecure now implements the secure core of a coursework-centric academic platform. Students can authenticate securely, access the assignments visible through their staff-managed academic spaces, submit work, and view their own grades. Lecturers and administrators can manage spaces, create assignments, review submissions, and record grades under stronger integrity and audit controls than the insecure baseline in the brief. The remaining platform capabilities from the wider case-study scenario, including live video classes, attendance, messaging, exam scheduling, and formal feedback-form workflows, should be presented as future expansion rather than as implemented evidence.

## 3. Short bridging sentences you can reuse

Use these if you need a compact transition sentence in the final report.

- The implemented artefact focuses on the most security-sensitive academic workflows rather than claiming complete LMS feature coverage.
- EduSecure currently delivers a secure coursework-management core, with broader learning-platform capabilities remaining outside the implemented repository scope.
- This bounded scope improves technical honesty and keeps the cryptographic evidence aligned with the actual codebase.
- The repository partially addresses course access and result visibility, but it does not yet implement self-service enrolment, live teaching, or exam-administration features.

## 4. Best placement guidance

Use the Section 1 paragraphs when you need to:
- frame the case study honestly
- explain why the report focuses on the implemented security core
- avoid implying full platform completion

Use the Section 8 paragraphs when you need to:
- summarise what the artefact actually demonstrates
- distinguish implemented, partial, and future-scope features
- connect the feature matrix to the final artefact discussion without turning Section 8 into a changelog

## 5. Related supporting documents

- `docs/pack-09/platform-feature-matrix-and-prioritized-backlog.md`
- `docs/pack-09/report-section-to-evidence-map.md`
- `docs/pack-09/final-report-draft-sections-1-to-5.md`
- `docs/pack-09/final-report-draft-sections-6-to-9.md`