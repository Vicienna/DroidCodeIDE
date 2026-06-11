#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# Fix Gradle Wrapper & Test Build Lokal
# ============================================================

set -e
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
cd "$(dirname "$0")"

echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}🔧 FIX GRADLE WRAPPER & TEST BUILD${NC}"
echo -e "${BLUE}==================================================${NC}"

# 1. Download Wrapper Resmi (Binary + Properties)
echo -e "${YELLOW}[1/4] Download gradlew & gradle-wrapper.jar (v8.7)...${NC}"
mkdir -p gradle/wrapper
curl -sL https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradlew -o gradlew
curl -sL https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar -o gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew

# 2. Pastikan gradle-wrapper.properties benar
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

# 3. TEST BUILD LOKAL (Ini yang paling penting - buktiin bisa compile)
echo -e "${YELLOW}[2/4] TEST BUILD LOKAL (./gradlew assembleDebug)...${NC}"
echo -e "${YELLOW}   (Ini butuh 5-10 menit download Gradle & Compile Kotlin)${NC}"
./gradlew assembleDebug --no-daemon --stacktrace

# 4. Kalau sampai sini berarti BUILD SUCCESS
echo -e "${GREEN}==================================================${NC}"
echo -e "${GREEN}✅ BUILD LOKAL BERHASIL! APK SIAP DI cloud.${NC}"
echo -e "${GREEN}==================================================${NC}"

# 5. Commit & Push Wrapper
echo -e "${YELLOW}[3/4] Commit & Push gradlew...${NC}"
git add gradlew gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties
git commit -m "chore: add gradle wrapper binary (fix CI)" || echo "No changes to commit."
git push origin main

echo -e "${YELLOW}[4/4] Siap Tag Release!${NC}"
echo -e "${BLUE}Jalankan perintah ini:${NC}"
echo -e "  git tag -d v1.0.0 2>/dev/null; git push origin --delete v1.0.0 2>/dev/null; git tag v1.0.1 && git push origin v1.0.1"