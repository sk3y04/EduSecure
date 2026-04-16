# Methodology Comparison Table

## Paste-ready table

| Methodology / framework | What it is | Best use in the EduSecure report | Strength for this project | Limitation / caution |
|---|---|---|---|---|
| OWASP Top 10 | A widely used summary of the most common web-application risk categories | Frame major application risks such as broken access control, cryptographic failures, injection, security misconfiguration, and logging weaknesses | Familiar to markers and easy to map to the brief's insecure baseline | Too high-level to act as the only methodology |
| OWASP ASVS | A structured application security verification standard | Define and justify security requirements for authentication, session management, access control, cryptography, file handling, and configuration | Strong for showing that security claims are verifiable rather than vague | More detailed than a short report can cover fully, so only the most relevant areas should be cited |
| OWASP WSTG | A practical web security testing guide | Justify hostile-use-case testing for login abuse, authorization bypass, CSRF, file-upload validation, and browser-facing checks | Strengthens the testing section by showing that security testing was systematic | Best used as a testing method, not as the whole risk model |
| NIST SP 800-30-style risk assessment | A structured risk methodology based on assets, vulnerabilities, threat events, likelihood, impact, treatment, and residual risk | Serve as the main risk-assessment method in Section 3 | Academically strong and already aligned with the repository's existing risk material | Less specific than OWASP for web-application failure patterns |
| CVSS v3.1 | A standard severity scoring system for technical vulnerabilities and abuse scenarios | Prioritise issues such as fallback secrets, login abuse risk, and transport/configuration weaknesses | Adds defensible severity ranking and treatment urgency | It prioritises severity but does not replace broader risk reasoning |
| STRIDE | A threat-modelling method covering spoofing, tampering, repudiation, information disclosure, denial of service, and elevation of privilege | Support the secure-design section by mapping architecture threats to controls around auth, submissions, grades, and audit evidence | Very useful for analysing trust boundaries and data flows | Best kept concise so the report does not become too diagram-heavy |
| CIS Controls / CIS Benchmarks | Operational and configuration-hardening guidance | Support deployment discussion for secrets, container hardening, secure configuration, least privilege, and TLS posture | Helps the report look stronger on operational security, not just application code | Should be presented as hardening guidance, not as proof of full enterprise compliance |

## Suggested one-line lead-in

Table X summarises the complementary security methodologies used in this report and shows how each contributes a distinct role in risk assessment, threat modelling, verification, testing, or operational hardening.

