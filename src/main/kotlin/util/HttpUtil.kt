package util

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import util.constants.GenericConstants.GSON

object HttpUtil {
    fun makeGetRequest(url: String, headers: Headers? = null): String {
        val client = OkHttpClient()

        var requestBuilder = Request.Builder()
            .url(url)

        if (headers != null) {
            requestBuilder = requestBuilder.headers(headers)
        }

        val request = requestBuilder.get().build()

        val response = client.newCall(request).execute()
        val responseString = response.body?.string() ?: ""

        return responseString
    }

    inline fun <reified S : Class<T>, T> makeGetRequestJson(url: String, headers: Headers? = null): Class<T> {
        val stringResponse = makeGetRequest(url, headers = headers)

        return GSON.fromJson(stringResponse, S::class.java)
    }
}