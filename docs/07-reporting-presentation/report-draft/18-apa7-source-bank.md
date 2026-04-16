# APA 7 Source Bank for the EduSecure Report

This file is a **report-writing source bank** for adding APA 7 in-text citations and a final reference list to the EduSecure report.

Use it together with:
- `docs/07-reporting-presentation/report-claims-audit-note.md`
- `docs/07-reporting-presentation/report-section-to-evidence-map.md`
- `docs/07-reporting-presentation/report-assembly-plan.md`
- `docs/07-reporting-presentation/report-draft/17-space-chat-e2ee-cross-section-draft.md`

## 1. How to use this source bank

Use these references for:
- standards, best practice, and security-methodology claims
- algorithm properties and protocol explanations
- threat-modelling, risk-assessment, and evaluation framing

Do **not** use external references as the only proof that EduSecure implements something.

Keep this distinction clear in the report:
- **external authority** = what good practice, standards, or cryptography literature says
- **repository evidence** = what EduSecure actually implements and proves

A strong sentence pattern is:

> The control is consistent with recognised guidance on authenticated encryption and secure transport (Dworkin, 2007; Rescorla, 2018), while the specific EduSecure implementation is evidenced in the repository by `frontend/src/services/chatCrypto.ts` and `backend/src/main/java/...`.

## 2. Recommended minimum citation strategy

For a strong report, aim to use at least:
- 2 to 3 sources in Section 2
- 2 to 3 sources in Section 3
- 2 to 3 sources in Section 4
- 3 to 4 sources in Section 5
- 2 to 3 sources in Section 6
- 1 to 2 sources in Section 7
- 1 to 2 sources in Section 8

That will comfortably exceed the minimum requirement of 15 in-text citations without making the report read like a literature review.

## 3. Suggested APA 7 reference list

The list below is intentionally standards-heavy because that fits the EduSecure report better than using too many generic textbooks.

### Risk assessment, methodology, testing, and threat modelling

**[R1]**
Joint Task Force Transformation Initiative. (2012). *Guide for conducting risk assessments* (NIST SP 800-30 Rev. 1). National Institute of Standards and Technology. https://doi.org/10.6028/NIST.SP.800-30r1
- In-text citation: `(Joint Task Force Transformation Initiative, 2012)`
- Best used for: risk methodology, asset-threat-vulnerability analysis, residual risk
- Best report sections: 3, 7

**[R2]**
OWASP Foundation. (2021). *OWASP top 10: The ten most critical web application security risks*. https://owasp.org/Top10/
- In-text citation: `(OWASP Foundation, 2021a)`
- Best used for: web-application risk framing, familiar marker-facing risk categories
- Best report sections: 3, 4

**[R3]**
OWASP Foundation. (2021). *OWASP application security verification standard 4.0.3*. https://owasp.org/www-project-application-security-verification-standard/
- In-text citation: `(OWASP Foundation, 2021b)`
- Best used for: secure design requirements, control validation language
- Best report sections: 4, 5, 8

**[R4]**
OWASP Foundation. (2021). *Web security testing guide*. https://owasp.org/www-project-web-security-testing-guide/
- In-text citation: `(OWASP Foundation, 2021c)`
- Best used for: security review and testing posture
- Best report sections: 3, 8

**[R5]**
Forum of Incident Response and Security Teams. (2019). *Common vulnerability scoring system version 3.1: Specification document*. https://www.first.org/cvss/specification-document
- In-text citation: `(Forum of Incident Response and Security Teams, 2019)`
- Best used for: severity prioritisation, CVSS support for risk ranking
- Best report sections: 3

**[R6]**
Shostack, A. (2014). *Threat modeling: Designing for security*. Wiley.
- In-text citation: `(Shostack, 2014)`
- Best used for: STRIDE-style reasoning, trust boundaries, design threat thinking
- Best report sections: 3, 4

### Authentication, passwords, and MFA

**[R7]**
Grassi, P. A., Garcia, M. E., & Fenton, J. L. (2017). *Digital identity guidelines: Authentication and lifecycle management* (NIST SP 800-63B). National Institute of Standards and Technology. https://doi.org/10.6028/NIST.SP.800-63b
- In-text citation: `(Grassi et al., 2017)`
- Best used for: MFA, authentication assurance, password policy boundaries
- Best report sections: 2, 5, 6

**[R8]**
Provos, N., & Mazieres, D. (1999). *A future-adaptable password scheme*. In *Proceedings of the FREENIX Track: 1999 USENIX Annual Technical Conference* (pp. 81–91). USENIX Association. https://www.usenix.org/legacy/events/usenix99/provos/provos.pdf
- In-text citation: `(Provos & Mazieres, 1999)`
- Best used for: password hashing rationale, why bcrypt is suitable
- Best report sections: 2, 5

**[R9]**
M'Raihi, D., Bellare, M., Hoornaert, F., Naccache, D., & Ranen, O. (2005). *HOTP: An HMAC-based one-time password algorithm* (RFC 4226). Internet Engineering Task Force. https://doi.org/10.17487/RFC4226
- In-text citation: `(M'Raihi et al., 2005)`
- Best used for: one-time password foundations, HMAC-backed OTP logic
- Best report sections: 2, 5

**[R10]**
M'Raihi, D., Machani, S., Pei, M., & Rydell, J. (2011). *TOTP: Time-based one-time password algorithm* (RFC 6238). Internet Engineering Task Force. https://doi.org/10.17487/RFC6238
- In-text citation: `(M'Raihi et al., 2011)`
- Best used for: TOTP-based MFA explanation
- Best report sections: 2, 5

### Cryptographic primitives and protocol references

**[R11]**
Dworkin, M. (2007). *Recommendation for block cipher modes of operation: Galois/Counter Mode (GCM) and GMAC* (NIST SP 800-38D). National Institute of Standards and Technology. https://doi.org/10.6028/NIST.SP.800-38D
- In-text citation: `(Dworkin, 2007)`
- Best used for: authenticated encryption, AES-GCM properties, IV/nonce sensitivity
- Best report sections: 2, 5, 6

**[R12]**
Krawczyk, H., & Eronen, P. (2010). *HMAC-based Extract-and-Expand Key Derivation Function (HKDF)* (RFC 5869). Internet Engineering Task Force. https://doi.org/10.17487/RFC5869
- In-text citation: `(Krawczyk & Eronen, 2010)`
- Best used for: HKDF in encrypted chat, purpose-specific key derivation
- Best report sections: 2, 5, 6

**[R13]**
Krawczyk, H., Bellare, M., & Canetti, R. (1997). *HMAC: Keyed-hashing for message authentication* (RFC 2104). Internet Engineering Task Force. https://doi.org/10.17487/RFC2104
- In-text citation: `(Krawczyk et al., 1997)`
- Best used for: HMAC rationale, audit-integrity explanation
- Best report sections: 2, 5, 7

**[R14]**
National Institute of Standards and Technology. (2015). *Secure hash standard (SHS)* (FIPS PUB 180-4). U.S. Department of Commerce. https://doi.org/10.6028/NIST.FIPS.180-4
- In-text citation: `(National Institute of Standards and Technology, 2015)`
- Best used for: SHA-256, digest-generation claims
- Best report sections: 2, 5

**[R15]**
Rescorla, E. (2018). *The Transport Layer Security (TLS) Protocol Version 1.3* (RFC 8446). Internet Engineering Task Force. https://doi.org/10.17487/RFC8446
- In-text citation: `(Rescorla, 2018)`
- Best used for: TLS 1.3, secure transport, deployment-side transmission protection
- Best report sections: 2, 4, 5

**[R16]**
World Wide Web Consortium. (2017). *Web Cryptography API*. https://www.w3.org/TR/WebCryptoAPI/
- In-text citation: `(World Wide Web Consortium, 2017)`
- Best used for: browser cryptography support, Web Crypto grounding for E2EE chat
- Best report sections: 2, 5, 6, 8

### Security architecture and design-boundary references

**[R17]**
Saltzer, J. H., & Schroeder, M. D. (1975). The protection of information in computer systems. *Proceedings of the IEEE, 63*(9), 1278–1308. https://doi.org/10.1109/PROC.1975.9939
- In-text citation: `(Saltzer & Schroeder, 1975)`
- Best used for: least privilege, separation of responsibility, secure design principles
- Best report sections: 4, 6, 7

**[R18]**
Anderson, R. (2020). *Security engineering: A guide to building dependable distributed systems* (3rd ed.). Wiley.
- In-text citation: `(Anderson, 2020)`
- Best used for: system trust boundaries, security trade-offs, bounded real-world evaluation
- Best report sections: 4, 6, 7

## 4. Best sources for the new space-chat E2EE slice

If you only cite a few external sources for encrypted chat, make them these:
- `Krawczyk and Eronen (2010)` for `HKDF`
- `Dworkin (2007)` for `AES-GCM`
- `World Wide Web Consortium (2017)` for browser Web Crypto grounding
- `Rescorla (2018)` if you need to distinguish application-layer message encryption from transport protection

Remember: the actual claim that **EduSecure implements** browser-side encrypted chat should still point to repository evidence such as:
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `frontend/src/services/chatCrypto.ts`
- `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
- `backend/src/main/java/edusecure/edusecure/service/spacechatkey/SpaceChatE2eeService.java`

## 5. Fast APA 7 writing reminders

- Use **author-date** format for in-text citations.
- Use `et al.` for three or more authors in in-text citations.
- If the same organisation has multiple sources in the same year, use `2021a`, `2021b`, `2021c` consistently.
- Use sentence-case for book, report, webpage, and RFC titles in the reference list.
- Keep the final reference list in **alphabetical order by author/organisation**.
- Before submission, do one final pass to make sure every in-text citation appears in the reference list and every listed source is cited at least once.

## 6. Final honesty check

Use external citations to justify:
- why a method or algorithm is recognised
- why a control category is appropriate
- why a limitation matters

Use repository evidence to justify:
- that EduSecure actually implements the control
- how the control is wired into the system
- what the frontend, backend, tests, and diagrams actually prove

