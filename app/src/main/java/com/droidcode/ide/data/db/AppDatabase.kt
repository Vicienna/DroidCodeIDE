package com.droidcode.ide.data.db

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