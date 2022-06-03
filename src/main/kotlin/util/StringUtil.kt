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

    fun parseSecondsToHMS(value: Int, space: Boolean = false, hours: String = "h", minutes: String = "m", seconds: String = "s"): String {
        val h = value / 3600
        val m = value % 3600 / 60
        val s = value % 60

        val spaceValue = if (space) " " else ""
        val hoursStr = if (h > 0) "$h$spaceValue$hours" else ""
        val minuteStr = (if (m in 1..9 && h > 0) "0" else "") + if (m > 0) if (h > 0 && s == 0) "$m$spaceValue$minutes" else "$m$spaceValue$minutes" else ""
        val secondsStr = if (s == 0 && (h > 0 || m > 0)) "" else (if (s < 10 && (h > 0 || m > 0)) "0" else "") + "$s$spaceValue$seconds"

        return (hoursStr + (if (m > 0) " " else "") + minuteStr + (if (s > 0) " " else "") + secondsStr).trim()
    }

    data class Tee(val text: String, val prefixZeroes: Boolean = false)

    fun toFormattedString(lst: HashMap<Long, Tee>, value: Long, separator: String = " "): String {
        val s = StringBuilder()

        val reversedKeys = lst.keys.reversed()
        var currentValue = lst.keys.sum()
        reversedKeys.forEach {


            currentValue /= it
        }

        return s.toString()
    }

    fun getSafeRegex(regex: Regex, text: String, group: Int = 1, default: String = ""): String {
        return if (regex.matches(text)) regex.find(text)!!.groups[group]!!.value + " " else default
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

    inline fun <reified T> extractJSONFromString(s: String, trimUntilStr: String? = null): T {
        val jsonStr = extractJSON(s, trimUntilStr)

        return JSON_FORMAT.decodeFromString(jsonStr)
    }

    inline fun <reified T> extractJSONMapFromString(s: String, trimUntilStr: String? = null): HashMap<String, T> {
        val jsonStr = extractJSON(s, trimUntilStr)
        val json = JSON_FORMAT.parseToJsonElement(jsonStr)

        val hashMap = hashMapOf<String, T>()
        for ((key, value) in json.jsonObject) {
            hashMap[key] = JSON_FORMAT.decodeFromJsonElement(value)
        }

        return hashMap
    }
}