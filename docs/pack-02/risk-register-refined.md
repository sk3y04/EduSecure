# Refined Risk Register

This register applies the method described in `risk-methodology.md` and strengthens the propositional-logic treatment.

## Symbol legend

Common symbols used below:

- `D`: database compromise occurs
- `P`: passwords are stored in plaintext
- `B`: passwords are stored using bcrypt
- `T`: transport uses TLS
- `N`: transport is not protected by TLS
- `I`: attacker intercepts traffic/token
- `S`: submission signature is verified
- `H`: submission hash is verified
- `A`: audit record exists
- `M`: audit record integrity is protected
- `U`: unauthorised modification succeeds
- `Q`: parameterised/JPA-backed data access is used
- `V`: unsafe input/query handling remains

## Refined entries

| ID | Asset | Threat event | Likelihood | Impact | Priority | Controls |
|---|---|---|---|---|---|---|
| R1 | Credentials | attacker obtains reusable passwords from a database breach | High | High | Critical | `bcrypt`, role restriction, secure password policy |
| R2 | Session token / login traffic | attacker intercepts token or credentials over public Wi-Fi | High | High | Critical | `TLS 1.3`, short token lifetime, minimal token claims |
| R3 | Grade data | attacker reads or alters grades using injection or unsafe data handling | Medium | High | High | JPA/parameterised access, validation, RBAC |
| R4 | Assignment submission | submitted file is tampered with or authorship is disputed | High | High | Critical | `SHA-256` digest, `RSA` signature verification |
| R5 | Grade-change record | unauthorised or untraceable modification occurs | Medium | High | High | audit logging, `HMAC`/hash integrity, RBAC |
| R6 | Cryptographic secrets | secret exposure weakens signatures, tokens, or integrity controls | Medium | High | High | env-managed secrets, key separation, documented key handling |
| R7 | System deliverable quality | over-complex design causes incomplete or weakly justified artefact | High | Medium | High | bounded scope, traceability, documentation-first process |

## Logic justification details

## R1: Plaintext password exposure

### Symbols
- `D`: database compromise occurs
- `P`: passwords are stored in plaintext
- `B`: passwords are stored using bcrypt
- `C`: attacker learns reusable credentials directly

### Formal statements
- `(D ∧ P) -> C`
- `(D ∧ B) -> ¬C`

### Interpretation
If the database is breached and passwords are stored in plaintext, the attacker directly learns usable credentials. If the same breach occurs but passwords are stored with bcrypt, plaintext disclosure does not follow directly, so the risk is significantly reduced.

### Residual risk
Password reuse, phishing, or weak user-chosen passwords may still cause compromise.

## R2: Token interception / unprotected transport

### Symbols
- `N`: transport is not protected by TLS
- `T`: transport is protected by TLS
- `I`: attacker intercepts useful credential or token data

### Formal statements
- `N -> higher(I)`
- `T -> lower(I)`

### Interpretation
If traffic is sent over an unprotected channel, interception becomes much easier. If TLS is used, interception risk is reduced because traffic confidentiality and integrity are improved in transit.

### Residual risk
TLS does not prevent compromise on infected endpoints or misuse of stolen tokens after local exposure.

## R3: Injection against grades/results

### Symbols
- `V`: unsafe input/query handling remains
- `Q`: parameterised/JPA-backed access is used
- `G`: grade data disclosure or alteration succeeds

### Formal statements
- `V -> higher(G)`
- `Q -> lower(G)`

### Interpretation
Unsafe query handling increases the chance of grade exposure or modification. Parameterised data access and validation reduce this risk, although they are secure-coding controls rather than cryptographic controls.

### Residual risk
Logic flaws or excessive privileges may still expose grade data.

## R4: Submission tampering / weak authorship proof

### Symbols
- `H`: file hash is verified
- `S`: digital signature is verified
- `W`: submission tampering or authorship dispute succeeds

### Formal statements
- `¬H -> higher(W)`
- `(H ∧ S) -> lower(W)`

### Interpretation
Without integrity verification, tampering or disputed authorship is harder to detect. When the file hash and signature are both verified, integrity and authorship confidence are significantly improved.

### Residual risk
If the private signing key is stolen, forged submissions may still appear valid.

## R5: Untraceable sensitive actions

### Symbols
- `A`: audit record exists
- `M`: audit record integrity is protected
- `U`: unauthorised modification cannot be reliably investigated

### Formal statements
- `¬A -> higher(U)`
- `(A ∧ M) -> lower(U)`

### Interpretation
If sensitive actions are not logged, later investigation is weak. If actions are logged and the log integrity is protected, accountability and tamper detection are improved.

### Residual risk
An attacker with sufficient privilege may still attempt log suppression or correlated misuse.

## R6: Key or secret exposure

### Symbols
- `K`: secrets are hardcoded or poorly protected
- `E`: key exposure occurs
- `R`: cryptographic protection reliability is reduced
- `X`: secrets are externalised and separated by purpose

### Formal statements
- `K -> higher(E)`
- `(E) -> R`
- `X -> lower(E)`

### Interpretation
Poor secret handling increases the chance of key exposure. If keys are exposed, the reliability of dependent controls drops. Externalising and separating secrets reduces exposure risk.

### Residual risk
Development environments may still be weaker than production-grade key-management environments.

## R7: Over-complex project scope

### Symbols
- `O`: unnecessary complexity is introduced
- `F`: project becomes incomplete or weakly justified
- `L`: scope is kept bounded and traceable

### Formal statements
- `O -> higher(F)`
- `L -> lower(F)`

### Interpretation
If too much complexity is added, the chance of an unfinished or poorly justified artefact rises. If scope remains bounded and traceable to the brief, deliverable quality is more likely to remain strong.

### Residual risk
Time constraints may still affect polish even when scope is controlled.


