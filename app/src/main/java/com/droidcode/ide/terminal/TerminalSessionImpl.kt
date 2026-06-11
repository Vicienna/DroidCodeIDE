package com.droidcode.ide.terminal

import android.util.Log
import com.hierynomus.sshj.SSHClient
import com.hierynomus.sshj.transport.verification.PromiscuousVerification
import com.hierynomus.sshj.userauth.keyprovider.FileKeyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream

class TerminalSessionImpl : TerminalSession {

    companion object { private const val TAG = "TerminalSession" }

    private var localProcess: Process? = null
    private var localOutputStream: OutputStream? = null
    private var sshClient: SSHClient? = null
    private var sshShell: com.hierynomus.sshj.connection.channel.direct.Session? = null
    private var sshOutputStream: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startLocal(onOutput: (String) -> Unit, onExit: (Int) -> Unit) {
        scope.launch {
            try {
                val shell = if (isTermuxAvailable()) "/data/data/com.termux/files/usr/bin/bash" else "/system/bin/sh"
                localProcess = Runtime.getRuntime().exec(arrayOf(shell, "-i", "-l"))
                localOutputStream = localProcess!!.outputStream
                Thread(startReader(localProcess!!.inputStream, onOutput, "STDOUT")).start()
                Thread(startReader(localProcess!!.errorStream, onOutput, "STDERR")).start()
                onExit(localProcess!!.waitFor())
            } catch (e: Exception) { Log.e(TAG, "Local Shell Error", e); onExit(-1) }
        }
    }

    private fun isTermuxAvailable(): Boolean = try { Runtime.getRuntime().exec("ls /data/data/com.termux/files/usr/bin/bash").waitFor() == 0 } catch (e: Exception) { false }

    private fun startReader(inputStream: java.io.InputStream, onOutput: (String) -> Unit, prefix: String): Runnable = Runnable {
        inputStream.bufferedReader().use { reader -> var line: String?; while (reader.readLine().also { line = it } != null) onOutput("$prefix: $line\n") }
    }

    override fun startSsh(host: String, port: Int, user: String, password: String?, keyPath: String?, onOutput: (String) -> Unit, onExit: (Int) -> Unit) {
        scope.launch {
            sshClient = SSHClient()
            try {
                sshClient!!.addHostKeyVerifier(PromiscuousVerification.INSTANCE)
                sshClient!!.connect(host, port)
                if (keyPath != null && keyPath.isNotBlank()) sshClient!!.authPublickey(user, FileKeyProvider(keyPath))
                else if (password != null) sshClient!!.authPassword(user, password)
                else throw IllegalArgumentException("SSH Auth required")
                sshShell = sshClient!!.startSession().apply { allocateDefaultPTY(); startShell() }
                sshOutputStream = sshShell!!.outputStream
                Thread(startReader(sshShell!!.inputStream, onOutput)).start()
                Thread(startReader(sshShell!!.errorStream, onOutput)).start()
                onExit(sshShell!!.join()?.exitStatus ?: 0)
            } catch (e: Exception) { Log.e(TAG, "SSH Error", e); onOutput("\r\n[SSH Error] ${e.message}\r\n"); onExit(-1) }
            finally { disconnectSsh() }
        }
    }

    override fun write(input: String) = scope.launch { try { localOutputStream?.write(input.toByteArray()); localOutputStream?.flush(); sshOutputStream?.write(input.toByteArray()); sshOutputStream?.flush() } catch (e: Exception) { Log.e(TAG, "Write failed", e) } }

    override fun resize(cols: Int, rows: Int) { sshShell?.resize(cols, rows) }

    override fun kill() = scope.launch { localProcess?.destroyForcibly(); disconnectSsh() }

    private fun disconnectSsh() { try { sshShell?.close() } catch (e: Exception) {}; try { sshClient?.disconnect() } catch (e: Exception) {}; sshShell = null; sshClient = null; sshOutputStream = null }
}
