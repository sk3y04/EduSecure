# Appendix CI/CD and Deployment Plan

This document defines how the report appendix can describe CI/CD and deployment without overstating the platform as a production system.

## 1. Positioning in the report

This material should be treated as **appendix/supporting engineering evidence**, not as the main cryptography discussion.

Its purpose is to show:
- reproducibility
- deployment awareness
- secure delivery thinking
- how EduSecure could be packaged beyond local development

## 2. Current verified local foundation

What is already verified in this repository:
- Spring Boot backend
- PostgreSQL dev compose file in `compose.yaml`
- backend test suite passing through Gradle

What is not yet verified here:
- a complete GitHub Actions workflow in this repository
- a production deployment from this repository to the home server
- a finished Nginx reverse-proxy setup for EduSecure itself

For PostgreSQL-specific setup, security, and schema-management guidance, see `docs/pack-09/postgresql-setup-and-security.md`.

For the appendix, this distinction should stay explicit.

## 3. GitHub Actions appendix plan

The appendix can describe a practical GitHub Actions pipeline such as:

### Backend workflow
- trigger on push / pull request
- set up Java
- run Gradle tests
- optionally build artefact or container image

### Future frontend workflow
Once the Vue frontend exists:
- install Node dependencies
- run frontend type-check / build
- optionally publish build artefact or container image

### Security-oriented CI notes
Useful appendix items:
- fail pipeline on test failure
- optionally run dependency/vulnerability checks
- ensure secrets are injected through GitHub repository secrets

## 4. Home-server deployment appendix plan

### Intended deployment posture
The appendix should describe a **study-project deployment** to a home server, not a full production platform.

### Suggested topology
- domain/subdomain
- Nginx reverse proxy
- HTTPS certificate management
- backend application service
- PostgreSQL service
- optional frontend static hosting or reverse-proxied frontend app

The appendix should stay explicit that HTTPS at the reverse proxy does **not automatically prove** PostgreSQL transport encryption or at-rest database encryption; those controls need their own documented configuration.

### HTTPS note
The report should make clear:
- HTTPS via Certbot/Let's Encrypt is the deployed transport security control
- it protects all client-server traffic in transit and satisfies the "secure file/message transmission" artefact requirement
- application-layer AES-GCM is used separately for MFA secrets and submission content at rest, not for general traffic encryption

## 5. Relationship to `homelab-blueprint`

The appendix may explicitly state that deployment and CI/CD structure will be informed by the user's external infrastructure reference:
- `https://github.com/sk3y04/homelab-blueprint`

Important wording recommendation:
- describe it as a **reference repository and deployment blueprint**
- avoid claiming exact implementation details from it here unless they are later verified and evidenced in EduSecure itself

## 6. Nginx reverse proxy appendix points

The appendix can explain a typical secure reverse-proxy role:
- terminate HTTPS
- forward requests to backend service
- optionally serve or proxy the Vue frontend
- keep certificate handling outside the application code

## 7. Suggested appendix structure

### Appendix A: CI/CD overview
- GitHub Actions workflow outline
- backend test/build stages
- future frontend stages
- secrets handling notes

### Appendix B: Deployment overview
- server topology diagram or concise bullet list
- domain and HTTPS note
- Nginx reverse proxy role
- database/application separation note

### Appendix C: Limitations
- home-server deployment is not enterprise production
- appendix content supports reproducibility and engineering reflection
- the assignment's core assessed value still comes from the cryptography analysis and artefact

## 8. What to collect later if you actually implement the appendix

If you later set up CI/CD and deployment for real, useful appendix evidence would be:
- GitHub Actions workflow YAML screenshots or snippets
- successful pipeline run screenshot
- Nginx site config excerpt
- certificate/HTTPS confirmation screenshot
- deployed application screenshot behind the domain

## 9. Scope-control reminder

Do not let appendix engineering work derail the main submission.

If time becomes tight, the appendix should remain supportive and concise, while the core report and cryptographic artefact stay the priority.

