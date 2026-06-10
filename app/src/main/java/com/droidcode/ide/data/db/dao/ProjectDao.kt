package com.droidcode.ide.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.droidcode.ide.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE uri = :uri")
    suspend fun delete(uri: String)

    @Query("SELECT * FROM projects ORDER BY lastOpened DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE uri = :uri")
    suspend fun getProject(uri: String): ProjectEntity?
}

$$WRITE:projects/DroidCodeIDE/app/src/main/java/com/droidcode/ide/data/db/dao/SettingsDao.kt$$package com.droidcode.ide.data.db.dao

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

$$WRITE:projects/DroidCodeIDE/app/src/main/java/com/droidcode/ide/data/db/AppDatabase.kt$$package com.droidcode.ide.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.droidcode.ide.data.db.dao.ProjectDao
import com.droidcode.ide.data.db.dao.SettingsDao
import com.droidcode.ide.data.db.entity.ProjectEntity
import com.droidcode.ide.data.db.entity.SettingEntity

@Database(
    entities = [ProjectEntity::class, SettingEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun settingsDao(): SettingsDao
} 