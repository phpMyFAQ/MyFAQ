#!/usr/bin/env bash
# Regenerates the Kotlin API client from the pinned OpenAPI spec.
#
# Prerequisites:
#   - openapi-generator-cli installed (npm i -g @openapitools/openapi-generator-cli)
#     or available via Docker
#
# Usage:
#   mobile/scripts/generate-api-client.sh
#
# The generated sources land under:
#   mobile/shared/src/commonMain/kotlin/app/myfaq/shared/api/generated/
#
# Generated code is committed to the repo. The build does NOT
# regenerate on each run — reproducibility beats freshness.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOBILE_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

SPEC_FILE="$MOBILE_ROOT/spec/openapi/v3.2.yaml"
OUTPUT_DIR="$MOBILE_ROOT/shared/src/commonMain/kotlin/app/myfaq/shared/api/generated"

if [ ! -f "$SPEC_FILE" ]; then
    echo "Error: OpenAPI spec not found at $SPEC_FILE"
    echo "Download it first:"
    echo "  curl -fsSL https://raw.githubusercontent.com/thorsten/phpMyFAQ/4.2.0/docs/openapi.yaml -o $SPEC_FILE"
    exit 1
fi

echo "Cleaning previous generated output..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

echo "Generating Kotlin client from $SPEC_FILE ..."

if command -v openapi-generator-cli &>/dev/null; then
    openapi-generator-cli generate \
        -i "$SPEC_FILE" \
        -g kotlin \
        -o "$OUTPUT_DIR" \
        --package-name app.myfaq.shared.api.generated \
        --additional-properties=library=multiplatform,serializationLibrary=kotlinx_serialization,enumPropertyNaming=UPPERCASE,dateLibrary=kotlinx-datetime,useCoroutines=true \
        --global-property models,apis,supportingFiles=false
elif command -v docker &>/dev/null; then
    docker run --rm \
        -v "$MOBILE_ROOT:/work" \
        -w /work \
        openapitools/openapi-generator-cli:latest generate \
        -i /work/spec/openapi/v3.2.yaml \
        -g kotlin \
        -o "/work/shared/src/commonMain/kotlin/app/myfaq/shared/api/generated" \
        --package-name app.myfaq.shared.api.generated \
        --additional-properties=library=multiplatform,serializationLibrary=kotlinx_serialization,enumPropertyNaming=UPPERCASE,dateLibrary=kotlinx-datetime,useCoroutines=true \
        --global-property models,apis,supportingFiles=false
else
    echo "Error: neither openapi-generator-cli nor docker found."
    echo "Install one of them and re-run this script."
    exit 1
fi

echo "Generated client written to $OUTPUT_DIR"
echo "Review the diff, then commit."
