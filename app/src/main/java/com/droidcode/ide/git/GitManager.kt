package com.droidcode.ide.git

import com.droidcode.ide.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

interface GitManager {
    fun cloneRepo(uri: String, repoUrl: String, branch: String?, credentials: GitCredentials?, progress: (String) -> Unit): Boolean
    fun openRepo(uri: String): GitRepoHandle?
    fun getStatus(handle: GitRepoHandle): Flow<GitStatusResult>
    fun stage(handle: GitRepoHandle, filePaths: List<String>)
    fun unstage(handle: GitRepoHandle, filePaths: List<String>)
    fun commit(handle: GitRepoHandle, message: String, author: GitAuthor): Boolean
    fun push(handle: GitRepoHandle, remote: String, branch: String, credentials: GitCredentials?): Boolean
    fun pull(handle: GitRepoHandle, remote: String, branch: String, credentials: GitCredentials?): Boolean
    fun getBranches(handle: GitRepoHandle): List<GitBranch>
    fun checkout(handle: GitRepoHandle, branchName: String, createNew: Boolean): Boolean
    fun getDiff(handle: GitRepoHandle, filePath: String, cached: Boolean): String
    fun getLog(handle: GitRepoHandle, maxCount: Int): List<GitCommit>

    data class GitCredentials(
        val username: String?,
        val password: String?,
        val sshKeyPath: String?,
        val sshPassphrase: String?
    )

    data class GitAuthor(val name: String, val email: String)

    data class GitStatusResult(
        val modified: List<String>,
        val staged: List<String>,
        val untracked: List<String>,
        val deleted: List<String>
    )

    data class GitBranch(val name: String, val isRemote: Boolean, val isCurrent: Boolean)

    data class GitCommit(
        val hash: String,
        val shortHash: String,
        val message: String,
        val author: String,
        val time: Long
    )

    interface GitRepoHandle {
        val rootUri: String
        fun close()
    }
} 