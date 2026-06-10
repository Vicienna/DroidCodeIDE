package com.droidcode.ide.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val lastOpened: Long = System.currentTimeMillis(),
    val rootPath: String? = null,
    val isRemote: Boolean = false,
    val remoteConfigJson: String? = null
) 