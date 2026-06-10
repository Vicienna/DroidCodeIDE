#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# DroidCodeIDE - Auto Fix & Secure Push Script
# ============================================================

# Warna untuk output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Memulai proses perbaikan project DroidCodeIDE...${NC}"

# 1. Pastikan berada di root project
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# 2. Buat folder yang mungkin hilang
echo -e "${BLUE}📁 Menyiapkan folder structure...${NC}"
mkdir -p app/src/main/java/com/droidcode/ide/data/db/entity
mkdir -p app/src/main/java/com/droidcode/ide/data/db/dao

# 3. Tulis file yang hilang (SettingEntity.kt)
echo -e "${BLUE}📝 Menulis SettingEntity.kt...${NC}"
cat > app/src/main/java/com/droidcode/ide/data/db/entity/SettingEntity.kt << 'EOF'
package com.droidcode.ide.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
EOF

# 4. Tulis file yang hilang (SettingsDao.kt)
echo -e "${BLUE}📝 Menulis SettingsDao.kt...${NC}"
cat > app/src/main/java/com/droidcode/ide/data/db/dao/SettingsDao.kt << 'EOF'
package com.droidcode.ide.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.droidcode.ide.data.db.entity.SettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(setting: SettingEntity)

    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getString(key: String): String?

    @Query("SELECT * FROM settings")
    fun getAll(): Flow<List<SettingEntity>>

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun delete(key: String)
}
EOF

# 5. Buat .gitignore untuk keamanan (SANGAT PENTING)
echo -e "${BLUE}🛡️ Membuat .gitignore...${NC}"
cat > .gitignore << 'EOF'
# Keystore & Secrets
*.jks
*.keystore
local.properties
*.properties.secret

# Build Outputs
/build
/app/build
/*.apk
/*.aab

# IDE & OS
.idea/
*.iml
.gradle/
.DS_Store
EOF

# 6. Hapus keystore dari tracking Git (agar tidak public)
echo -e "${BLUE}🧹 Membersihkan keystore dari Git tracking...${NC}"
git rm --cached droidcode-release.jks 2>/dev/null || true
git rm --cached *.jks 2>/dev/null || true

# 7. Commit dan Push
echo -e "${BLUE}📦 Melakukan Commit dan Push...${NC}"
git add .
git commit -m "chore: fix missing db files, add gitignore, secure keystore" || echo "Tidak ada perubahan untuk di-commit."
git push origin main

echo -e "\n${GREEN}==================================================${NC}"
echo -e "${GREEN}✅ SEMUA SELESAI!${NC}"
echo -e "--------------------------------------------------"
echo -e "Total file tracked: $(git ls-files | wc -l)"
echo -e "Keystore di repo: $(git ls-files | grep -c '\.jks$') (Harus 0)"
echo -e "Gitignore: $(git ls-files | grep '.gitignore' || echo 'TIDAK ADA')"
echo -e "--------------------------------------------------"
echo -e "Sekarang lo bisa jalanin tag release:"
echo -e "${BLUE}git tag v1.0.0 && git push origin v1.0.0${NC}"
echo -e "${GREEN}==================================================${NC}"