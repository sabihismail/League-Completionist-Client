package util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import util.KotlinExtensionUtil.length

object StringUtil {
    val JSON_FORMAT = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    fun toTimeStyleString(value: Int, lst: List<Pair<Int, String>>, excludeEndingZeroes: Boolean = true, separator: String = " ", minimumValueCount: Int = 1): String {
        return toTimeStyleString(value.toLong(), lst.map { Pair(it.first.toLong(), it.second) }, excludeEndingZeroes, separator, minimumValueCount)
    }

    fun toTimeStyleString(value: Long, lst: List<Pair<Long, String>>, excludeEndingZeroes: Boolean = true, separator: String = " ", minimumValueCount: Int = 1): String {
        val s = StringBuilder()

        val reversedStr = lst.reversed()
        var currentOffset = lst.map { it.first }.reduce { sum, element -> sum * element }
        var currentValue = value
        reversedStr.forEach { (num, str) ->
            val diff = currentValue / currentOffset

            val diffLength = diff.length()
            val diffStr = if (diffLength >= minimumValueCount) {
                diff.toString()
            } else {
                "0".repeat(minimumValueCount - diffLength) + diff
            }

            if (!(excludeEndingZeroes && diff == 0L)) {
                s.append("$diffStr$str$separator")
            }

            currentValue -= diff * currentOffset
            currentOffset /= num
        }

        val toStr = s.toString()
        return toStr.substring(0, toStr.length - separator.length)
    }

    fun toDistanceString(value: Int, lst: List<Pair<Int, String>>, separator: String = ".", decimalCount: Int = 1): String {
        return toDistanceString(value.toLong(), lst.map { Pair(it.first.toLong(), it.second) }, separator, decimalCount)
    }

    fun toDistanceString(value: Long, lst: List<Pair<Long, String>>, separator: String = ".", decimalCount: Int = 1): String {
        var surpassedIndex = lst.indexOfFirst { it.first > value }
        if (surpassedIndex == -1) {
            surpassedIndex = lst.size
        }

        val (num, str) = lst[surpassedIndex - 1]

        val divided = value / num.toDouble()
        val formatted = "%.${decimalCount}f".format(divided).replace(".", separator)

        return "$formatted$str"
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