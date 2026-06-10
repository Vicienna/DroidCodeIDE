package com.droidcode.ide.terminal

interface TerminalSession {
    fun startLocal(onOutput: (String) -> Unit, onExit: (Int) -> Unit)
    fun startSsh(host: String, port: Int, user: String, password: String?, keyPath: String?, onOutput: (String) -> Unit, onExit: (Int) -> Unit)
    fun write(input: String)
    fun resize(cols: Int, rows: Int)
    fun kill()
} 