package com.droidcode.ide.terminal

import android.util.Log
import com.hierynomus.sshj.SSHClient
import com.hierynomus.sshj.transport.verification.PromiscuousVerification
import com.hierynomus.sshj.userauth.keyprovider.FileKeyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream

class TerminalSessionImpl(private val scope: CoroutineScope) : TerminalSession {

    companion object {
        private const val TAG = "TerminalSession"
    }

    private var localProcess: Process? = null
    private var localOutputStream: OutputStream? = null
    private var sshClient: SSHClient? = null
    private var sshShell: com.hierynomus.sshj.connection.channel.direct.Session? = null
    private var sshOutputStream: OutputStream? = null

    override fun startLocal(onOutput: (String) -> Unit, onExit: (Int) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val shell = if (isTermuxAvailable()) "/data/data/com.termux/files/usr/bin/bash" else "/system/bin/sh"
                localProcess = Runtime.getRuntime().exec(arrayOf(shell, "-i", "-l"))
                localOutputStream = localProcess!!.outputStream

                Thread(startLocalReader(localProcess!!.inputStream, onOutput, "STDOUT")).start()
                Thread(startLocalReader(localProcess!!.errorStream, onOutput, "STDERR")).start()

                val exitCode = localProcess!!.waitFor()
                onExit(exitCode)
            } catch (e: Exception) {
                Log.e(TAG, "Local Shell Error", e)
                onExit(-1)
            }
        }
    }

    private fun isTermuxAvailable(): Boolean {
        return try {
            Runtime.getRuntime().exec("ls /data/data/com.termux/files/usr/bin/bash").waitFor() == 0
        } catch (e: Exception) { false }
    }

    private fun startLocalReader(inputStream: java.io.InputStream, onOutput: (String) -> Unit, prefix: String): Runnable {
        return Runnable {
            inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    onOutput("$prefix: $line\n")
                }
            }
        }
    }

    override fun startSsh(
        host: String, port: Int, user: String,
        password: String?, keyPath: String?,
        onOutput: (String) -> Unit, onExit: (Int) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            sshClient = SSHClient()
            try {
                sshClient!!.addHostKeyVerifier(PromiscuousVerification.INSTANCE)
                sshClient!!.connect(host, port)

                if (keyPath != null && keyPath.isNotBlank()) {
                    val keyProvider = FileKeyProvider(keyPath)
                    sshClient!!.authPublickey(user, keyProvider)
                } else if (password != null) {
                    sshClient!!.authPassword(user, password)
                } else {
                    throw IllegalArgumentException("SSH Auth required: password or key")
                }

                sshShell = sshClient!!.startSession()
                sshShell!!.allocateDefaultPTY()
                sshShell!!.startShell()
                sshOutputStream = sshShell!!.getOutputStream()

                Thread(startSshReader(sshShell!!.getInputStream(), onOutput)).start()
                Thread(startSshReader(sshShell!!.getErrorStream(), onOutput)).start()

                sshShell!!.join()
                onExit(sshShell!!.getExitStatus() ?: 0)

            } catch (e: Exception) {
                Log.e(TAG, "SSH Error", e)
                onOutput("\r\n[SSH Error] ${e.message}\r\n")
                onExit(-1)
            } finally {
                disconnectSsh()
            }
        }
    }

    private fun startSshReader(inputStream: java.io.InputStream, onOutput: (String) -> Unit): Runnable {
        return Runnable {
            val buffer = ByteArray(4096)
            var bytesRead: Int
            try {
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    onOutput(String(buffer, 0, bytesRead))
                }
            } catch (e: Exception) {
                // Stream closed
            }
        }
    }

    override fun write(input: String) {
        scope.launch(Dispatchers.IO) {
            try {
                localOutputStream?.write(input.toByteArray())
                localOutputStream?.flush()
                sshOutputStream?.write(input.toByteArray())
                sshOutputStream?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Write failed", e)
            }
        }
    }

    override fun resize(cols: Int, rows: Int) {
        sshShell?.resize(cols, rows)
    }

    override fun kill() {
        scope.launch(Dispatchers.IO) {
            localProcess?.destroyForcibly()
            disconnectSsh()
        }
    }

    private fun disconnectSsh() {
        try { sshShell?.close() } catch (e: Exception) {}
        try { sshClient?.disconnect() } catch (e: Exception) {}
        sshShell = null
        sshClient = null
        sshOutputStream = null
    }
} 