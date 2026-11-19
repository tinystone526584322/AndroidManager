package com.diy.androidmanager

import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

@Keep
class ShellUserService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                "OK:$stdout"
            } else {
                "ERR($exitCode):${stderr.ifBlank { stdout }}"
            }
        } catch (e: Exception) {
            "ERR(-1):${e.message ?: "unknown error"}"
        }
    }
}
