package util

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class StringUtilTest {
    @Serializable
    private data class JSONObject(val test: String)

    @Test
    fun checkToDistanceString() {
        assertEquals("6.0km", StringUtil.toDistanceString(6000L, listOf(1L to "m", 1000L to "km")))
        assertEquals("6.00km", StringUtil.toDistanceString(6000L, listOf(1L to "m", 1000L to "km"), decimalCount = 2))
        assertEquals("6.05km", StringUtil.toDistanceString(6052L, listOf(1L to "m", 1000L to "km"), decimalCount = 2))
        assertEquals("6>1km", StringUtil.toDistanceString(6052L, listOf(1L to "m", 1000L to "km"), separator = ">"))
        assertEquals("6>052km", StringUtil.toDistanceString(6052L, listOf(1L to "m", 1000L to "km"), decimalCount = 3, separator = ">"))
    }

    @Test
    fun checkToFormattedString() {
        assertEquals(StringUtil.toTimeStyleString(6000L, listOf(1L to "s", 60L to "m", 60L to "h")), "1h 40m")
        assertEquals(StringUtil.toTimeStyleString(6000L, listOf(1L to "s", 60L to "m", 60L to "h"), separator = "   "), "1h   40m")
        assertEquals(StringUtil.toTimeStyleString(6000L, listOf(1L to "s", 60L to "m", 60L to "h"), excludeEndingZeroes = false), "1h 40m 0s")
        assertEquals(StringUtil.toTimeStyleString(6000L, listOf(1L to "s", 60L to "m", 60L to "h"), minimumValueCount = 2), "01h 40m")
        assertEquals(StringUtil.toTimeStyleString(6000L, listOf(1L to "s", 60L to "m", 60L to "h"), minimumValueCount = 2, excludeEndingZeroes = false),
            "01h 40m 00s")
    }

    @Test
    fun checkSkipJSONObject() {
        val json = "    skip = {test:\"test\"}     "

        val obj = StringUtil.extractJSONFromString<JSONObject>(json, "skip = ")

        assert(obj == JSONObject("test"))
    }

    @Test
    fun checkUnquotedJSONObject() {
        val json = "{test:\"test\"}"

        val obj = StringUtil.extractJSONFromString<JSONObject>(json)

        assert(obj == JSONObject("test"))
    }

    @Test
    fun checkQuotedJSONObject() {
        val json = "{\"test\":\"test\"}"

        val obj = StringUtil.extractJSONFromString<JSONObject>(json)

        assert(obj == JSONObject("test"))
    }

    @Test
    fun checkRandomPrefixJSONObject() {
        val json = "          {\"test\":\"test\"}"

        val obj = StringUtil.extractJSONFromString<JSONObject>(json)

        assert(obj == JSONObject("test"))
    }

    @Test
    fun checkRandomSuffixJSONObject() {
        val json = "{\"test\":\"test\"}           "

        val obj = StringUtil.extractJSONFromString<JSONObject>(json)

        assert(obj == JSONObject("test"))
    }

    @Test
    fun checkRandomPrefixSuffixJSONObject() {
        val json = "          {\"test\":\"test\"}           "

        val obj = StringUtil.extractJSONFromString<JSONObject>(json)

        assert(obj == JSONObject("test"))
    }
}