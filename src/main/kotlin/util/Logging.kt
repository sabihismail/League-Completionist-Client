package util

import org.apache.commons.lang3.builder.RecursiveToStringStyle
import org.apache.commons.lang3.builder.ReflectionToStringBuilder

object Logging {
    private val LOG_MODE = LogType.INFO

    private var lastMessageLogged = ""

    fun log(obj: Any, logType: LogType, header: String? = null, ignorableDuplicate: Boolean = false) {
        if (logType < LOG_MODE) return

        val s = ReflectionToStringBuilder.reflectionToString(obj, RecursiveToStringStyle())

        var headerValue = header
        if (headerValue == null) {
            headerValue = obj::class.simpleName
        }

        log(s, logType, header = headerValue, ignorableDuplicate = ignorableDuplicate)
    }

    fun log(str: String, logType: LogType, header: String? = null, ignorableDuplicate: Boolean = false, carriageReturn: Int = -1, messageType: LogMessageType? = null) {
        if (logType < LOG_MODE) return
        
        val s = StringBuilder()
        if (messageType != null) {
            s.append("[${messageType.name}] ")
        }

        if (!header.isNullOrBlank()) {
            s.append("$header: ")
        }

        s.append(str)

        val toStr = s.toString()
        if (ignorableDuplicate && toStr == lastMessageLogged) return

        lastMessageLogged = toStr

        if (carriageReturn != -1) {
            print(toStr + if (carriageReturn == 0) "\r" else "\n")
        } else {
            println(toStr)
        }
    }
}