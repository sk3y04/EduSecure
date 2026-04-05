# Pack 11 Postman Fixtures

This folder contains small sample files for the Pack 11 Postman collection.

## Files

- `student-a.txt`
  - intended for `studentAUploadPath`
  - use this for the normal valid submission upload in `00 Persona & Seed Data`

- `traversal.txt`
  - intended for `studentTraversalUploadPath`
  - use this as the source file for the traversal-style filename test in the submission folder

## Important traversal-testing note

For the traversal scenario, the **file contents are not the attack**.
The important part is the **filename sent in the multipart request**.

That means you may need to do one of these in Postman:
- manually rename the multipart file part to `../secret.txt`, or
- manually rename it to `..\\secret.txt`

The file on disk can stay a harmless text file such as `traversal.txt`.

## Suggested variable mapping

For local testing, map:
- `studentAUploadPath` -> `docs/pack-11/postman-fixtures/student-a.txt`
- `studentTraversalUploadPath` -> `docs/pack-11/postman-fixtures/traversal.txt`

## Why these files exist

These fixtures make the Pack 11 Postman bundle usable without having to create ad hoc test files first.

