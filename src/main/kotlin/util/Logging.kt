package util

import DEBUG_LOG_ALL_ENDPOINTS
import DEBUG_LOG_HANDLED_ENDPOINTS
import org.apache.commons.lang3.builder.RecursiveToStringStyle
import org.apache.commons.lang3.builder.ReflectionToStringBuilder

object Logging {
    private val LOG_MODE = if (DEBUG_LOG_ALL_ENDPOINTS)
        LogType.ALL
    else if (DEBUG_LOG_HANDLED_ENDPOINTS)
        LogType.DEBUG
    else
        LogType.INFO

    fun log(obj: Any, logType: LogType, header: String? = null) {
        val s = ReflectionToStringBuilder.reflectionToString(obj, RecursiveToStringStyle())

        var headerValue = header
        if (headerValue == null) {
            headerValue = obj::class.simpleName
        }

        log(s, logType, headerValue)
    }

    fun log(str: String, logType: LogType, header: String? = null) {
        if (logType < LOG_MODE) return
        
        val s = StringBuilder()
        if (!header.isNullOrBlank()) {
            s.append("$header: ")
        }

        s.append(str)

        println(s.toString())
    }
}