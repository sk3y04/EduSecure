# EduSecure Presentation Tomorrow Checklist

Use this as the final prep sheet before building or delivering the talk.

## 1. Slide build order
- Primary deck: `docs/edusecure-presentation-script-13-slide.md`
- Evidence companion: `docs/pack-09/presentation-evidence-appendix.md`
- Live speaking aid: `docs/pack-09/presenter-crib-sheet-one-page.md`
- Backup Q&A: `docs/edusecure-security-presentation-qa-notes.md`

## 2. Must-capture visuals
- [ ] Login screen from `frontend/src/pages/Login/index.vue`
- [ ] MFA challenge screen from `frontend/src/pages/MfaChallenge/index.vue`
- [ ] MFA setup/status from `frontend/src/pages/AccountSecurity/index.vue`
- [ ] Submission detail evidence view from `frontend/src/pages/SubmissionDetail/index.vue`
- [ ] Grade panel from `frontend/src/pages/SubmissionDetail/index.vue`
- [ ] Browser devtools screenshot showing `EDUSECURE_AUTH`
- [ ] Browser devtools screenshot showing `XSRF-TOKEN`
- [ ] Terminal screenshot showing successful targeted backend security tests
- [ ] PowerPoint-made problem-to-control summary table
- [ ] PowerPoint-made implemented-now vs bounded-claims table

## 3. Slide-by-slide visual pairing
- Slide 1: use-case diagram + login screen
- Slide 2: insecure deployment diagram + problem/control table
- Slide 3: context DFD + post-login workspace
- Slide 4: level-1 DFD + browser devtools/network view
- Slide 5: secure login sequence + MFA challenge
- Slide 6: MFA screen + explain encrypted secret vs hashed recovery code
- Slide 7: secure deployment diagram + cookie screenshot
- Slide 8: submission sequence + submission evidence screen
- Slide 9: AES-at-rest retrieval sequence + retrieval/download flow
- Slide 10: grade integrity sequence + grade panel
- Slide 11: audit integrity sequence + audit-event summary table
- Slide 12: no diagram + successful test-run screenshot
- Slide 13: secure deployment diagram + implemented/bounded summary table

## 4. Five lines not to forget
- `bcrypt` fixes plaintext-password storage.
- MFA session is issued only after full verification.
- Browser auth uses `HttpOnly` cookie transport and CSRF protection.
- Submissions get both integrity/authorship evidence and confidentiality at rest.
- Audit and grade controls make sensitive actions more trustworthy and reviewable.

## 5. Honesty boundaries to keep live
- [ ] Say TLS is the **intended deployment-side transport control**.
- [ ] Say the signing workflow uses a **stable demo ECC keypair**, not full PKI.
- [ ] Say EduSecure is **strong within study-project scope**, not production-complete.
- [ ] Do not say **fully secure**, **end-to-end encrypted**, or **repository proves deployed HTTPS**.

## 6. Demo order if asked to show the app
1. Login screen
2. MFA challenge or MFA status screen
3. Submission detail showing digest/signature/verification
4. Grade panel
5. Browser devtools cookie evidence
6. Terminal screenshot of tests

## 7. Best fallback if time is cut
Use the 10-slide fallback in `docs/edusecure-presentation-script-13-slide.md`:
- `1, 2, 3, 5, 6, 8, 9, 10, 12, 13`

## 8. Likely questions to rehearse once tonight
- Why `bcrypt` instead of encryption for passwords?
- Why encrypt TOTP secrets but hash recovery codes?
- Where is the MITM protection?
- Why use `AES-GCM` for submissions?
- Why say strong study-project signing instead of full non-repudiation?
- How do you know the implementation actually works?

## 9. Final 30-second self-check before presenting
- [ ] Open the deck and presenter crib sheet side by side
- [ ] Confirm every diagram path is correct
- [ ] Confirm screenshots are readable when projected
- [ ] Rehearse Slide 7 MITM wording once
- [ ] Rehearse Slide 13 limitations wording once
- [ ] End with the safe closing sentence, not an overclaim

