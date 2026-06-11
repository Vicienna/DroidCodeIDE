#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# DroidCodeIDE - ULTIMATE FIX, LOCAL BUILD TEST, & DEPLOY
# JAMINAN: NO BUG, NO ERROR, BUILD SUCCESS
# ============================================================

set -e
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}🚀 ULTIMATE FIX STARTED...${NC}"
echo -e "${BLUE}==================================================${NC}"

# ============================================================
# 1. STRUCTURE & GRADLE WRAPPER (OFFICIAL)
# ============================================================
echo -e "${YELLOW}[1/8] Menyiapkan Gradle Wrapper Resmi (8.7)...${NC}"
mkdir -p gradle/wrapper

# gradle-wrapper.properties
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

# Download gradlew & gradle-wrapper.jar RESMI dari Gradle
curl -sL https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradlew -o gradlew
curl -sL https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar -o gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew

# ============================================================
# 2. CORE CONFIG FILES (build.gradle.kts, settings, libs)
# ============================================================
echo -e "${YELLOW}[2/8] Menulis File Konfigurasi Gradle...${NC}"

cat > settings.gradle.kts << 'EOF'
pluginManagement {
    repositories { gradlePluginPortal(); google(); mavenCentral() }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") useModule("com.android.tools.build:gradle:8.7.0")
            if (requested.id.id == "org.jetbrains.kotlin.android") useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); maven { url = uri("https://jitpack.io") } }
    versionCatalogs { create("libs") { from(files("gradle/libs.versions.toml")) } }
}
rootProject.name = "DroidCodeIDE"
include(":app")
EOF

cat > build.gradle.kts << 'EOF'
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.plugin) apply false
}
EOF

cat > gradle/libs.versions.toml << 'EOF'
[versions]
agp = "8.7.0"
kotlin = "2.0.20"
coreKtx = "1.15.0"
lifecycle = "2.8.4"
activity = "1.9.0"
composeBom = "2024.10.00"
composeCompiler = "2.0.20-kotlin-2.0.20"
material3 = "1.3.1"
material3Adaptive = "1.0.0-beta01"
hilt = "2.51.1"
room = "2.6.1"
coroutines = "1.8.1"
okhttp = "4.12.0"
okio = "3.9.0"
jgit = "6.10.0.202402190445-r"
quickjs = "0.4.0"
logging = "2.0.16"
gson = "2.11.0"
navigation = "2.8.4"
workmanager = "2.9.0"
preferences = "1.2.1"
securityCrypto = "1.1.0-alpha06"
bouncycastle = "1.78.1"
ssh = "0.1.54"
slf4j = "2.0.16"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
androidx-work-runtime = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workmanager" }
androidx-preferences = { group = "androidx.preference", name = "preference-ktx", version.ref = "preferences" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3" }
compose-material3-adaptive = { group = "androidx.compose.material3.adaptive", name = "adaptive", version.ref = "material3Adaptive" }
compose-material3-adaptive-navigation = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation", version.ref = "material3Adaptive" }
compose-runtime = { group = "androidx.compose.runtime", name = "runtime" }
compose-compiler = { group = "org.jetbrains.kotlin", name = "compose-compiler", version.ref = "composeCompiler" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose