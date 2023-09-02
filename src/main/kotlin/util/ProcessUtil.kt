package util

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


object ProcessUtil {
    fun runCommand(command: String, workingDir: File = File(".")): String {
        try {
            val process = ProcessBuilder("cmd.exe", "/c", command).directory(workingDir).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lines = mutableListOf<String>()
            while (true) {
                line = reader.readLine()

                if (line == null) break

                if (line.isNotBlank()) {
                    lines.add(line)
                }
            }

            return lines.joinToString("\n")
        } catch(e: IOException) {
            e.printStackTrace()
            return ""
        }
    }
}