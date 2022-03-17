package util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object StringUtil {
    val JSON_FORMAT = Json {
        isLenient = true
    }

    inline fun <reified T> extractJSONFromString(s: String, trim_until_str: String? = null): T {
        var str = s

        if (!trim_until_str.isNullOrBlank()) {
            str = str.substring(str.indexOf(trim_until_str))
        }

        var count = 1
        val start = str.indexOf('{')
        var i = start + 1
        while (count > 0) {
            if (str[i] == '{') {
                count++
            } else if (str[i] == '}') {
                count--
            }

            i++
        }

        val jsonStr = str.substring(start, i)

        return JSON_FORMAT.decodeFromString(jsonStr)
    }
}