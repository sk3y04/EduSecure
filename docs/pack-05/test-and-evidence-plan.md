# Test and Evidence Plan

> **Historical planning note:** The AES-demo-specific expectations in this file describe an earlier artefact direction. The current repository's active symmetric-encryption evidence comes from AES-GCM protection of MFA secrets and submission content at rest.

This document defines what the next grade-integrity and earlier AES-demo phases needed to prove through tests and report evidence.

## 1. Purpose

The next implementation phase should not only work technically. It should also produce evidence that can be reused directly in the report.

## 2. Grade-integrity evidence expectations

## Positive cases
- creating a grade for a verified submission succeeds
- updating a grade succeeds for an authorised lecturer/admin actor
- student can retrieve only their own grade
- audit entries are created for grade creation and update
- audit entries contain non-empty integrity values

## Negative cases
- unauthenticated grade access is rejected
- student cannot update grade records
- grade creation against missing submission is rejected
- grade creation for non-verified submission is rejected if that rule is enforced
- changing the canonical audit payload changes the HMAC result

## Report evidence for grade integrity
The report should later be able to show:
- API response for grade creation/update
- API response for student grade retrieval
- evidence of audit creation
- explanation of how the audit record supports tamper-evident accountability

## 3. Historical standalone symmetric-crypto evidence expectations

## Positive cases
- encrypting a plaintext message produces ciphertext that differs from the plaintext
- decrypting the ciphertext with the correct key/nonce restores the original plaintext
- ciphertext packaging includes the nonce/IV needed for decryption

## Negative cases
- modified ciphertext or authentication tag fails decryption
- wrong key fails decryption
- invalid input is rejected cleanly

## Report evidence for the retired standalone symmetric-crypto slice
The report should later be able to show:
- plaintext example
- encrypted output example
- successful decryption example
- failure example for tampered ciphertext
- concise explanation of why AES-GCM was selected

## 4. Reusable test artefacts

The next implementation phase should aim to leave behind:
- integration tests for grade create/update/retrieve
- tests for audit record creation/integrity behavior
- unit or integration tests for AES encrypt/decrypt behavior
- sample outputs suitable for screenshots or appendix use

## 5. Evidence quality rule

A feature should not be considered complete unless the repository contains:
- documented purpose
- runnable implementation
- automated test or equivalent demonstrable evidence
- report-ready explanation of what the evidence proves

