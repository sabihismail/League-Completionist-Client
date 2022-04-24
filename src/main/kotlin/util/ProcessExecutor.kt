package util

import java.io.PrintWriter
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Function


class ProcessExecutor(private val executable: String, private val endMarker: String) {
    private val running = AtomicBoolean(true)
    private val lock = ReentrantLock()
    private val lineByLineListener: MutableList<Function<String, Boolean>> = ArrayList()
    private val finalOutputListener: MutableList<(String) -> Unit> = ArrayList()
    private var process: Process? = null
    private var cmdOutput: Scanner? = null
    private var cmdInput: PrintWriter? = null

    init {
        process = ProcessBuilder(executable).start()
        cmdOutput = Scanner(process!!.inputStream)
        cmdInput = PrintWriter(process!!.outputStream)
    }

    fun run() {
        val stringBuilder = StringBuilder()

        while (running.get()) {
            while (running.get() && process!!.isAlive && cmdOutput!!.hasNextLine()) {
                val s = cmdOutput!!.nextLine()
                stringBuilder.append(s)

                lock.lock()
                lineByLineListener.removeIf { stringBooleanFunction: Function<String, Boolean> ->
                    stringBooleanFunction.apply(s)
                }
                lock.unlock()

                if (s == endMarker) {
                    running.set(false)
                }
            }

            if (!process!!.isAlive && running.get()) {
                process = ProcessBuilder(executable).start()
                cmdOutput = Scanner(process!!.inputStream)
                cmdInput = PrintWriter(process!!.outputStream)
            }
        }

        val str = stringBuilder.toString()
        finalOutputListener.forEach { it.invoke(str) }

        close()
    }

    @Suppress("unused")
    fun addOutputListener(listener: Function<String, Boolean>) {
        lock.lock()
        lineByLineListener.add(listener)
        lock.unlock()
    }

    fun addFinalOutputListener(listener: (String) -> Unit) {
        lock.lock()
        finalOutputListener.add(listener)
        lock.unlock()
    }

    fun writeCommand(s: String?) {
        cmdInput?.println(s)
        cmdInput?.flush()
    }

    private fun close() {
        running.set(false)
        cmdInput?.close()
        cmdOutput?.close()
        process?.destroyForcibly()

        cmdInput = null
        cmdOutput = null
        process = null
    }
}
