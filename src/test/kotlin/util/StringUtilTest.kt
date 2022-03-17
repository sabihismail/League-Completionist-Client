package util

import org.junit.jupiter.api.Test

internal class StringUtilTest {
    @kotlinx.serialization.Serializable
    private data class JSONObject(val test: String)

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