package util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

object StringUtil {
    val JSON_FORMAT = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    fun extractJSON(s: String, trim_until_str: String? = null): String {
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

        return str.substring(start, i)
    }

    inline fun <reified T> extractJSONFromString(s: String, trim_until_str: String? = null): T {
        val jsonStr = extractJSON(s, trim_until_str)

        return JSON_FORMAT.decodeFromString(jsonStr)
    }

    inline fun <reified T> extractJSONMapFromString(s: String, trim_until_str: String? = null): HashMap<String, T> {
        val jsonStr = extractJSON(s, trim_until_str)
        val json = JSON_FORMAT.parseToJsonElement(jsonStr)

        val hashMap = hashMapOf<String, T>()
        for ((key, value) in json.jsonObject) {
            hashMap[key] = JSON_FORMAT.decodeFromJsonElement(value)
        }

        return hashMap
    }
}