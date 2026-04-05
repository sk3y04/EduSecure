# Manual Test Coverage Summary

This document explains what the **manual security testing artefacts** currently cover in EduSecure, and how that coverage complements the unit and integration test layers.

Related companion notes:
- `docs/pack-09/unit-test-coverage-summary.md`
- `docs/pack-09/integration-test-coverage-summary.md`
- `docs/pack-09/test-evidence-collection-template.md`
- `docs/pack-09/test-evidence-worked-examples.md`

Primary manual/security-review artefacts:
- `docs/pack-11/README.md`
- `docs/pack-11/security-sensitive-modules-inventory.md`
- `docs/pack-11/security-test-scenarios-matrix.md`
- `docs/pack-11/security-test-gaps-and-next-tests.md`
- `docs/pack-11/manual-security-testing-playbook.md`
- `docs/pack-11/postman/README.md`
- `docs/pack-11/postman/EduSecure-Pack-11.postman_collection.json`

## 1. Purpose

The repository already contains:
- unit-focused tests for cryptographic and configuration building blocks
- integration tests for end-to-end API and persistence behavior

What those automated layers do **not** fully prove is the browser, deployment, and policy-assurance layer.

That is where the Pack 11 manual-security artefacts matter.

This document explains:
- what the manual security review material is designed to cover
- which risks still require human execution and interpretation
- why Postman/browser/configuration review remains necessary even with a strong automated suite

## 2. Classification rule used in this document

For this summary, a manual-security artefact is anything that:
- guides a reviewer through scenario execution rather than asserting outcomes automatically in code
- focuses on browser behavior, policy interpretation, deployment posture, or evidence capture
- uses Postman as a human-driven API client rather than as a CI test harness
- distinguishes between implemented behavior and intended policy

This layer is important because some security questions are not only technical.
They are also about:
- browser context
- cookie transport behavior
- deployment configuration
- institutional policy choices
- evidence capture and reviewer judgement

## 3. Main manual-testing artefacts and what they cover

## 3.1 `docs/pack-11/security-sensitive-modules-inventory.md`

### What it covers
This file maps the implemented backend to the major security boundaries.

It identifies and explains the current rules around:
- global authentication boundaries
- session and auth-cookie controls
- managed-user role restrictions
- MFA challenge and disablement behavior
- submission metadata and content access checks
- submission upload validation boundaries
- grade authorization rules
- space-management ownership rules
- audit integrity behavior
- current assignment visibility and post-membership revocation behavior for student-facing submission and grade access

### What this proves
This file does not prove behavior by itself.
Instead, it gives the reviewer the **trust-boundary model** needed to test the system intelligently.

### Why manual review still needs it
Automated tests can show pass/fail results, but they do not automatically explain which access-control rule or policy boundary is being exercised.
This inventory turns the codebase into an understandable test target.

## 3.2 `docs/pack-11/security-test-scenarios-matrix.md`

### What it covers
This is the Pack 11 master scenario list.

It organizes security review items by area, including:
- auth and session checks
- MFA challenge/replay/lockout checks
- submission access-control checks
- grade privilege-abuse checks
- space-authorization checks
- audit/configuration/CORS/CSRF review scenarios

It also labels each scenario by:
- scenario ID
- priority
- actor
- target
- expected secure result
- current evidence status

### What this proves
Again, this file is not itself proof of behavior.
Its role is to define a **reviewable scenario inventory** so you can see:
- what has already been automated
- what still requires manual execution
- which checks are policy-sensitive rather than purely technical

### Why manual review still needs it
Without a matrix, security review easily becomes ad hoc and incomplete.
This file provides coverage discipline and prioritization.

## 3.3 `docs/pack-11/security-test-gaps-and-next-tests.md`

### What it covers
This file captures the most important unresolved or policy-sensitive areas after code review.

It highlights in particular:
- student assignment visibility through current space membership
- CSRF posture review needs
- CORS allowlist testing needs
- production secret override checks
- remaining candidates for future automated tests

### What this proves
It proves that the repository documentation now distinguishes between:
- tested controls
- current behavior
- unresolved policy questions
- missing assurance areas

### Why manual review still needs it
Some security outcomes cannot be labeled simply "secure" or "insecure" until you decide whether the current business rule is intended.
This file is the bridge between code review and governance/policy review.

## 3.4 `docs/pack-11/manual-security-testing-playbook.md`

### What it covers
This is the main procedural review guide.

It provides step-by-step human execution guidance for:
- environment preparation
- actor/persona setup
- session and cookie handling
- evidence capture
- submission security review
- grade integrity review
- space-management review
- MFA review
- browser-only CSRF and CORS review
- cookie/configuration/secret-management review

### What this proves
It does not create automated evidence on its own.
What it does provide is a **repeatable manual-review method**.

That matters because repeatability is the difference between:
- a vague statement like "I tested it manually"
- and a defendable statement like "I followed a documented review process with named scenarios, actors, and expected results"

### Why manual review still needs it
This is the key artefact for:
- browser-only checks
- deployment posture checks
- evidence screenshots
- manual attack simulation
- appendix/report walkthrough evidence

## 3.5 `docs/pack-11/postman/README.md` and the Pack 11 Postman bundle

### What it covers
The Pack 11 Postman bundle provides a human-run API-client structure for the API-executable portion of the security review.

It covers:
- persona setup and seed-data requests
- auth and session boundary checks
- MFA challenge and disable checks
- submission access-control checks
- grade privilege checks
- space-authorization checks

The README also explains what Postman is **not** suitable for proving, especially:
- true browser `SameSite` behavior
- browser-enforced CORS
- real hostile-origin CSRF behavior
- database audit review
- deployment secret handling

### What this proves
The Postman bundle gives you a reusable execution scaffold for manual API testing.
It is best understood as **manual API-review tooling**, not as automated repository proof.

### Why manual review still needs it
It allows a reviewer to:
- execute negative tests consistently
- switch actors cleanly in a cookie-auth model
- reproduce priority security scenarios quickly
- capture response evidence in a structured way

## 4. What the manual layer covers overall

Taken together, the Pack 11 artefacts cover these areas strongly:

### Browser and session assurance
- cookie attribute inspection
- real-browser session behavior
- browser-oriented CORS review
- browser-oriented CSRF review

### Human-run abuse-case execution
- student-versus-student access-control probing
- student-versus-privileged grade manipulation attempts
- lecturer-versus-lecturer ownership and visibility checks
- MFA replay, reuse, and disable-abuse checks

### Policy-sensitive interpretation
- whether immediate loss of student submission/grade self-service after space removal matches the intended academic model

### Deployment/configuration review
- production cookie hardening posture
- secret override expectations
- allowed-origin review
- boundary between development-safe defaults and production-safe settings

### Evidence capture and report support
- scenario IDs and priorities
- actor-driven steps
- expected results
- screenshot/report appendix guidance
- a repeatable reviewer workflow

## 5. What the manual layer covers that automated tests do not fully prove

This is the most important reason these docs exist.

### 5.1 Browser-only behavior
Automated backend tests cannot truly prove:
- whether browsers send cookies in cross-site contexts under a given `SameSite` setting
- whether an unapproved origin is blocked by real browser CORS behavior
- whether a hostile-origin form/script request can trigger state changes in a real browser session

### 5.2 Deployment and operational assumptions
Automated repository tests do not fully prove:
- that real environments override all fallback secrets
- that production profiles are being used correctly in deployment
- that operators have applied the intended cookie/CORS settings consistently

### 5.3 Policy interpretation
Automated tests can prove what the system **does**.
They cannot decide whether that behavior matches the intended institutional rule.

This matters especially for:
- whether historical student access should remain revoked immediately after assignment-space membership removal

### 5.4 Audit review as evidence, not just side effect
Integration tests can prove that audit rows are created.
Manual review is still useful for checking:
- whether audit details remain non-secret
- whether audit evidence is suitable for screenshots/report appendix use
- whether operational access to audit storage is appropriately restricted

## 6. Relationship to unit and integration tests

The best way to describe the full testing story honestly is:
- **unit tests prove the security-critical building blocks in isolation**
- **integration tests prove the implemented cross-layer API and persistence behavior**
- **manual security review covers browser, deployment, policy, and reviewer-evidence assurance**

This third layer is not weaker because it is manual.
It is necessary because some security questions are simply outside the scope of ordinary automated backend tests.

## 7. Practical evidence status of the manual layer

It is important to describe this layer precisely.

### What the repository already contains
The repository already contains:
- scenario definitions
- execution guidance
- Postman review structure
- priority ordering
- manual-review caveats
- evidence-capture guidance
- a reusable evidence recording template in `docs/pack-09/test-evidence-collection-template.md`

### What the repository does not automatically contain
The repository does **not** automatically contain the outcome of those manual checks.
In other words, Pack 11 currently provides:
- **manual testing design and execution assets**

but not yet necessarily:
- a complete stored set of screenshots/results for every scenario in every target environment

That distinction is important for honest report claims.

## 8. Suggested wording for your report

If you want a concise way to explain this in the report, you can say:

> In addition to unit and Spring Boot integration testing, the repository contains a dedicated manual-security review pack that defines abuse-case scenarios, browser/deployment checks, and a Postman-based execution structure. This manual layer is used for questions that automated backend tests do not fully prove, such as browser cookie behavior, hostile-origin CSRF/CORS posture, deployment-secret discipline, and policy-sensitive authorization outcomes.

## 9. Bottom line

The Pack 11 manual-security artefacts provide the repository's **assurance layer beyond automation**.

They do not replace unit or integration tests.
Instead, they complete the evidence story by covering:
- browser behavior
- deployment posture
- policy-sensitive authorization interpretation
- reviewer-driven evidence capture

That makes them important not only for security testing itself, but also for producing honest, well-scoped report claims about what EduSecure really proves and what still requires human review.

