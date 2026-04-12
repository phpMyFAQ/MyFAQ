# Pinned OpenAPI Specification

This directory holds the pinned copy of the phpMyFAQ `v3.2` OpenAPI
specification from which the Kotlin API client is generated.

- `v3.2.yaml` — the spec itself. Captured from the phpMyFAQ tag
  recorded in `VERSION`.
- `VERSION` — the phpMyFAQ git tag this spec was captured from.

## How to update

Run `mobile/scripts/generate-api-client.sh` after placing a new
`v3.2.yaml` here, or trigger the `mobile-openapi-sync` GitHub
Actions workflow with the desired phpMyFAQ ref.
