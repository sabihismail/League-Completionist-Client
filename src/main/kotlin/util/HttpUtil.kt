package util

import league.util.LeagueConnectionUtil
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.io.entity.EntityUtils
import util.constants.GenericConstants
import java.net.URI

object HttpUtil {
    private val HTTP_CLIENT = LeagueConnectionUtil.createHttpClient()

    fun makeGetRequest(url: String, headers: List<Header> = listOf()): String {
        val method = HttpGet(URI(url))
        for (header in headers) {
            method.addHeader(header)
        }

        HTTP_CLIENT.execute(method).use { response ->
            if (response.code != 200) {
                println("[makeGetRequest] Failed querying $url, headers=${headers.map { "${it.name}:${it.value}" }}")
            } else {
                val t = LeagueConnectionUtil.dumpStream(response.entity.content)
                EntityUtils.consume(response.entity)

                return t ?: ""
            }
        }

        return ""
    }

    inline fun <reified T: Any> makeGetRequestJson(url: String, headers: List<Header> = listOf()): T? {
        val stringResponse = makeGetRequest(url, headers = headers)

        if (stringResponse.isBlank()) return null

        return GenericConstants.GSON_PRETTY.fromJson(stringResponse, T::class.java)
    }
}