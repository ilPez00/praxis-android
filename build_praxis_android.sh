#!/bin/bash
#
# build_praxis_android.sh
# Builds the Praxis webapp and packages it into the Android app
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBAPP_DIR="$SCRIPT_DIR/../praxis_webapp/client"
ANDROID_DIR="$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo "══════════════════════════════════════════"
echo "     PRAXIS ANDROID BUILD SCRIPT"
echo "══════════════════════════════════════════"
echo ""

# Step 1: Build webapp
log_step "Building Praxis webapp..."
cd "$WEBAPP_DIR"
npm run build
log_info "Webapp built successfully!"

# Step 2: Copy to Android assets
log_step "Copying webapp to Android assets..."
rm -rf "$ANDROID_DIR/app/src/main/assets/webapp"
mkdir -p "$ANDROID_DIR/app/src/main/assets"
cp -r "$WEBAPP_DIR/dist" "$ANDROID_DIR/app/src/main/assets/webapp"
log_info "Webapp copied to Android assets!"

# Step 3: Build Android APK
log_step "Building Android APK..."
cd "$ANDROID_DIR"

# Check if gradlew exists
if [[ ! -f "./gradlew" ]]; then
    log_warn "gradlew not found, using system gradle"
    gradle assembleDebug
else
    chmod +x ./gradlew
    ./gradlew assembleDebug
fi

log_info "══════════════════════════════════════════"
log_info "          BUILD COMPLETED"
log_info "══════════════════════════════════════════"
log_info ""
log_info "APK location: app/build/outputs/apk/debug/app-debug.apk"
log_info ""
log_info "To install on device:"
log_info "  adb install app/build/outputs/apk/debug/app-debug.apk"
log_info ""
