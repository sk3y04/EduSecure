# CSRF Browser Evidence Capture Note

This note explains how to capture **report-ready browser evidence** for EduSecure's implemented CSRF protection without overstating what the automated backend tests alone prove.

Use it together with:
- `docs/pack-09/final-implementation-evidence-map.md`
- `docs/pack-09/manual-test-coverage-summary.md`
- `docs/pack-09/test-evidence-collection-template.md`
- `docs/pack-09/test-evidence-worked-examples.md`
- `docs/pack-11/security-test-scenarios-matrix.md`
- `docs/pack-11/manual-security-testing-playbook.md`

## 1. Purpose

The repository already proves that:
- Spring Security CSRF protection is enabled for unsafe methods
- `GET /api/auth/csrf` can issue the readable `XSRF-TOKEN` cookie
- the browser client is coded to mirror that cookie into the `X-XSRF-TOKEN` header on unsafe requests
- backend integration tests accept valid CSRF tokens and reject missing ones on unsafe routes

What the repository does **not** automatically prove is the exact behavior of a real browser in a hostile-origin situation.

This note exists so you can capture that browser-layer evidence cleanly for:
- Section 8 technical artefact summary
- the appendix
- demo/viva preparation
- final screenshot selection

## 2. Scenarios this note supports

Primary scenarios:
- `CSRF-01` from `docs/pack-11/security-test-scenarios-matrix.md`
- `AUTH-15` from `docs/pack-11/security-test-scenarios-matrix.md`

Recommended target actions for capture:
- `POST /api/auth/logout`
- `POST /api/auth/mfa/setup`
- a lecturer-only unsafe endpoint such as grade creation
- a student unsafe endpoint such as submission upload

You do **not** need to capture every unsafe endpoint.
One or two good hostile-origin examples plus one normal in-app browser trace are usually enough for the report.

## 3. Scope boundary to describe honestly

Safe wording:
- EduSecure now enforces server-side CSRF protection for unsafe browser requests
- the SPA bootstraps `GET /api/auth/csrf` and then sends the `X-XSRF-TOKEN` header matching the readable `XSRF-TOKEN` cookie
- browser evidence is still valuable because backend tests do not fully prove real hostile-origin browser behavior

Avoid saying:
- "the browser can never be abused cross-site"
- "manual browser proof is unnecessary because the backend tests passed"
- "CSRF is impossible"

## 4. Minimum setup before capture

Record these items first:
- backend base URL
- frontend URL
- browser name/version
- active cookie/security profile if known
- whether HTTPS is enabled in the environment being demonstrated

Recommended preconditions:
1. sign in to the normal EduSecure frontend in the browser
2. open developer tools
3. clear old noise from the Network tab
4. keep the Application/Storage cookie view visible
5. prepare one unsafe action you can trigger from the real frontend

## 5. Browser evidence to capture for the normal in-app flow

This is the simplest positive proof that the implemented browser client is using the CSRF design as intended.

### Capture checklist

1. Trigger the first unsafe action from the normal EduSecure UI
2. Confirm a bootstrap request to `GET /api/auth/csrf` appears if no CSRF cookie already exists
3. Confirm the response sets `XSRF-TOKEN`
4. Confirm the later unsafe request includes:
   - the auth cookie
   - the `X-XSRF-TOKEN` header
5. Confirm the unsafe request succeeds when initiated from the real frontend

### Best screenshots or exports

Capture at least:
- Network entry for `GET /api/auth/csrf`
- response headers showing `Set-Cookie: XSRF-TOKEN=...`
- Network entry for the unsafe request showing `X-XSRF-TOKEN`
- optional cookie-store screenshot showing `XSRF-TOKEN` present and readable while the auth cookie remains `HttpOnly`

## 6. Browser evidence to capture for hostile-origin review

This is the most valuable manual proof for the report because it demonstrates the difference between:
- a valid in-app request
- and a hostile-origin attempt that should not be able to satisfy the CSRF requirement properly

### Suggested hostile-origin method

Use a simple page hosted from a different origin that attempts one of the following:
- HTML form `POST`
- JavaScript `fetch` with credentials
- XHR/fetch without access to the EduSecure origin cookies

### What to observe

Check whether the hostile page can:
- trigger the request at all
- include the victim auth cookie
- read the CSRF cookie value
- send a valid matching `X-XSRF-TOKEN` header
- cause the unsafe state change to succeed server-side

### Expected secure result

The hostile-origin attempt should fail to produce a successful unsafe action unless it can somehow supply the valid CSRF token/header pair.

In report wording, that means the key point is:
- EduSecure now has a server-side CSRF check
- and the hostile page should not be able to complete the required token/header pairing in the normal threat model

## 7. Evidence filenames to use

Recommended examples:

```text
2026-04-06_CSRF-01_csrf-bootstrap_browser-network.png
2026-04-06_CSRF-01_xsrf-cookie_application-tab.png
2026-04-06_CSRF-01_unsafe-request_with-xsrf-header_browser-network.png
2026-04-06_CSRF-01_hostile-origin-post_blocked-or-forbidden_browser-network.png
2026-04-06_AUTH-15_logout-cross-site_attempt_notes.md
```

## 8. Minimum record to store in the appendix or evidence folder

For each chosen CSRF browser scenario, keep:
- scenario ID
- actor/session context
- origin of page making the request
- target endpoint and method
- whether cookies were sent
- whether `XSRF-TOKEN` was present
- whether `X-XSRF-TOKEN` was present
- final HTTP status/result
- whether any state change actually happened
- screenshot/export filenames

If you want a reusable structure, record it using:
- `docs/pack-09/test-evidence-collection-template.md`
- especially the Browser Security Check mini-template

If you want an appendix-ready example to copy from immediately, use the `CSRF-01` worked record in `docs/pack-09/test-evidence-worked-examples.md`.

## 9. How to interpret the result safely

### Strong result

A strong result looks like this:
- the real frontend first obtains `XSRF-TOKEN`
- the real frontend sends `X-XSRF-TOKEN` on unsafe requests
- the legitimate unsafe request succeeds
- the hostile-origin request fails or remains ineffective

### Weak or incomplete result

A weak result looks like this:
- only Postman evidence was captured
- no real browser network trace was saved
- the reviewer cannot tell whether the hostile request actually changed server state

## 10. Report-ready wording

Short version:

> Browser evidence complemented the automated Spring Boot tests by showing that the EduSecure SPA bootstraps a readable `XSRF-TOKEN` cookie through `GET /api/auth/csrf` and then sends the matching `X-XSRF-TOKEN` header on unsafe requests. A hostile-origin browser review was also used to check that cross-site state-changing attempts did not succeed under the implemented cookie-backed session model.

More cautious version:

> Automated tests prove the server-side CSRF contract and the acceptance/rejection behavior for valid versus missing tokens. Manual browser inspection was additionally used to capture how the real frontend bootstraps the CSRF cookie/header flow and to review hostile-origin behavior in a realistic browser context.

## 11. Bottom line

Use this note when you want one concise Pack 09 artefact that explains:
- why browser CSRF evidence still matters
- what to capture
- how to name and interpret the evidence
- how to write it up honestly in the final report

