package util

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

    fun extractJSON(s: String, trimUntilStr: String? = null, elementLocation: Int = 0, skipStartCount: Int = 0): String {
        var str = s
        val finalJsonValues = mutableListOf<String>()
        var start = 0
        while (start >= 0) {
            if (!trimUntilStr.isNullOrBlank()) {
                val trimIndex = str.indexOf(trimUntilStr)
                if (trimIndex == -1) break

                str = str.substring(trimIndex)
            }

            str = str.substring(skipStartCount)
            start = str.indexOf('{')
            if (start == -1) break

            var count = 1
            var i = start + 1
            while (count > 0) {
                if (str[i] == '{') {
                    count++
                } else if (str[i] == '}') {
                    count--
                }

                i++
            }

            val finalStr = str.substring(start, i)
            if (finalStr != "{}") {
                finalJsonValues.add(finalStr)
            }

            str = str.substring(i)
        }

        val index = if (elementLocation == -1) finalJsonValues.size - 1 else elementLocation
        return finalJsonValues[index]
    }

    inline fun <reified T> extractAndAggregateJson(s: String, starters: Array<String>, prefix: String = ""): T {
        val combinedJson = starters.map { prefix + it }
            .joinToString(",") {
                "\"$it\": " + extractJSON(s, trimUntilStr = it, skipStartCount = it.length)
            }

        val combinedJsonStr = "{$combinedJson}"

        return JSON_FORMAT.decodeFromString(combinedJsonStr)
    }

    @Suppress("unused")
    inline fun <reified T> extractJSONFromFirstString(s: String, trimStarting: Array<String>, prefix: String = "", elementLocation: Int = 0): T {
        val earliestIndexStr = trimStarting.map { prefix + it to s.indexOf(it) }.minBy { it.second }.first

        val jsonStr = extractJSON(s, trimUntilStr=earliestIndexStr, elementLocation=elementLocation, skipStartCount=earliestIndexStr.length)

        return JSON_FORMAT.decodeFromString(jsonStr)
    }

    inline fun <reified T> extractJSONFromString(s: String, trimUntilStr: String? = null, elementLocation: Int = 0, skipStartCount: Int = 0): T {
        val jsonStr = extractJSON(s, trimUntilStr=trimUntilStr, elementLocation=elementLocation, skipStartCount=skipStartCount)

        return JSON_FORMAT.decodeFromString(jsonStr)
    }

    inline fun <reified T> extractJsonMapFromString(s: String, trimUntilStr: String? = null, elementLocation: Int = 0): HashMap<String, T> {
        val jsonStr = extractJSON(s, trimUntilStr=trimUntilStr, elementLocation=elementLocation)
        val json = JSON_FORMAT.parseToJsonElement(jsonStr)

        val hashMap = hashMapOf<String, T>()
        for ((key, value) in json.jsonObject) {
            hashMap[key] = JSON_FORMAT.decodeFromJsonElement(value)
        }

        return hashMap
    }
}