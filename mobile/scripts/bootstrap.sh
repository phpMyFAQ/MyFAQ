#!/usr/bin/env bash
# One-shot check that the local dev environment meets Phase 0
# prerequisites. Run once after cloning, or after switching machines.
#
# Usage:
#   mobile/scripts/bootstrap.sh

set -euo pipefail

ok=0
fail=0

check() {
    local name="$1"
    local cmd="$2"
    local min_version="${3:-}"
    if eval "$cmd" &>/dev/null; then
        version=$(eval "$cmd" 2>/dev/null | head -1)
        printf "  %-22s OK  (%s)\n" "$name" "$version"
        ((ok++))
    else
        printf "  %-22s MISSING\n" "$name"
        ((fail++))
    fi
}

echo "Checking prerequisites..."
echo ""

check "Java (JDK 17+)" "java -version 2>&1 | head -1"
check "JAVA_HOME" "echo \$JAVA_HOME"
check "Android SDK" "echo \$ANDROID_HOME"
check "Xcode" "xcodebuild -version 2>/dev/null | head -1" || true
check "XcodeGen" "xcodegen --version"
check "Git" "git --version"

echo ""
echo "Results: $ok passed, $fail missing"

if [ "$fail" -gt 0 ]; then
    echo ""
    echo "Fix the missing items above, then re-run this script."
    echo "See docs/mobile/build.md for install instructions."
    exit 1
fi

echo ""
echo "Environment OK. To build:"
echo "  cd mobile && ./gradlew :shared:allTests :androidApp:assembleDebug"
echo ""
echo "For iOS:"
echo "  cd mobile/iosApp && xcodegen generate"
echo "  open iosApp.xcodeproj"
