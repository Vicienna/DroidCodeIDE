package com.droidcode.ide.git

import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.*
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitManagerImpl @Inject constructor() : GitManager {

    companion object {
        private const val TAG = "GitManager"
        private const val GIT_DIR_NAME = ".git"
    }

    private val repoCache = ConcurrentHashMap<String, Git>()

    override fun cloneRepo(uri: String, repoUrl: String, branch: String?, credentials: GitCredentials?, progress: (String) -> Unit): Boolean {
        return try {
            progress("Clone to Internal Storage first. Then link to SAF folder.")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Clone failed", e)
            progress("Error: ${e.message}")
            false
        }
    }

    override fun openRepo(uri: String): GitRepoHandle? {
        return try {
            val file = uriToFile(uri) ?: return null
            val gitDir = File(file, GIT_DIR_NAME)
            if (!gitDir.exists()) return null
            
            val repo = Git.open(gitDir)
            repoCache[uri] = repo
            return object : GitRepoHandle {
                override val rootUri: String = uri
                override fun close() { repo.close(); repoCache.remove(uri) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Open Repo failed", e)
            null
        }
    }

    private fun uriToFile(uri: String): File? {
        return try {
            val u = Uri.parse(uri)
            if (u.scheme == "file") return File(u.path)
            null
        } catch (e: Exception) { null }
    }

    override fun getStatus(handle: GitRepoHandle): Flow<GitStatusResult> {
        return kotlinx.coroutines.flow.flowOf(GitStatusResult(emptyList(), emptyList(), emptyList(), emptyList()))
    }

    override fun stage(handle: GitRepoHandle, filePaths: List<String>) {
        repoCache[handle.rootUri]?.add()?.addFilepattern(*filePaths.toTypedArray())?.call()
    }

    override fun unstage(handle: GitRepoHandle, filePaths: List<String>) {
        repoCache[handle.rootUri]?.reset()?.setMode(ResetCommand.ResetType.MIXED)?.addPath(*filePaths.toTypedArray())?.call()
    }

    override fun commit(handle: GitRepoHandle, message: String, author: GitAuthor): Boolean {
        return try {
            repoCache[handle.rootUri]?.commit()
                ?.setMessage(message)
                ?.setAuthor(PersonIdent(author.name, author.email))
                ?.call()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Commit failed", e)
            false
        }
    }

    override fun push(handle: GitRepoHandle, remote: String, branch: String, credentials: GitCredentials?): Boolean {
        return try {
            val cmd = repoCache[handle.rootUri]?.push()?.setRemote(remote)?.setRefSpecs(RefSpec(branch))
            credentials?.let { setCredentials(cmd!!, it) }
            cmd?.call()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Push failed", e)
            false
        }
    }

    override fun pull(handle: GitRepoHandle, remote: String, branch: String, credentials: GitCredentials?): Boolean {
        return try {
            val cmd = repoCache[handle.rootUri]?.pull()?.setRemote(remote)?.setRemoteBranchName(branch)
            credentials?.let { setCredentials(cmd!!, it) }
            cmd?.call()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Pull failed", e)
            false
        }
    }

    private fun setCredentials(cmd: TransportCommand, cred: GitCredentials) {
        val provider = object : CredentialsProvider() {
            override fun get(uri: String?, host: String?, path: String?, prompt: String?): CredentialsProvider.Credentials? {
                return if (cred.sshKeyPath != null) {
                    try {
                        SshSessionFactory.getInstance().createSshSessionFactory().createSession(cred.username ?: "git", host!!, cred.sshKeyPath, cred.sshPassphrase?.toCharArray())
                    } catch (e: Exception) { null }
                } else if (cred.username != null && cred.password != null) {
                    UsernamePasswordCredentialsProvider(cred.username, cred.password)
                } else null
            }
            override fun isInteractive(): Boolean = false
        }
        cmd.setCredentialsProvider(provider)
    }

    override fun getBranches(handle: GitRepoHandle): List<GitBranch> {
        return try {
            repoCache[handle.rootUri]?.branchList()?.call()?.map { ref ->
                GitBranch(ref.name.replace("refs/heads/", ""), false, ref.isCurrent)
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    override fun checkout(handle: GitRepoHandle, branchName: String, createNew: Boolean): Boolean {
        return try {
            repoCache[handle.rootUri]?.checkout()?.setName(branchName)?.setCreateBranch(createNew)?.call()
            true
        } catch (e: Exception) { false }
    }

    override fun getDiff(handle: GitRepoHandle, filePath: String, cached: Boolean): String {
        return try {
            val repo = repoCache[handle.rootUri] ?: return ""
            val baos = ByteArrayOutputStream()
            val formatter = DiffFormatter(OutputStreamWriter(baos))
            formatter.setRepository(repo.repository)
            formatter.setDiffComparator(RawTextComparator.DEFAULT)
            formatter.setContext(3)
            
            val head = repo.repository.resolve(Constants.HEAD)
            val treeParser = CanonicalTreeParser()
            val reader = repo.repository.newObjectReader()
            
            if (cached) {
                treeParser.reset(reader, repo.repository.resolve(Constants.HEAD + "^{tree}"))
                formatter.format(treeParser, filePath)
            } else {
                formatter.format(filePath)
            }
            reader.close()
            baos.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Diff failed", e)
            ""
        }
    }

    override fun getLog(handle: GitRepoHandle, maxCount: Int): List<GitCommit> {
        return try {
            val walk = RevWalk(repoCache[handle.rootUri]!!.repository)
            val head = repoCache[handle.rootUri]!!.repository.resolve(Constants.HEAD)
            walk.markStart(walk.parseCommit(head))
            walk.limit(maxCount)
            walk.map { c ->
                GitCommit(c.name, c.abbreviate(7).name, c.shortMessage, c.authorIdent.name, c.commitTime * 1000L)
            }.toList()
        } catch (e: Exception) { emptyList() }
    }
} 