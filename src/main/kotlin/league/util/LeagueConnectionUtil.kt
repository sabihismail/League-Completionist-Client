package league.util

import com.stirante.lolclient.ProcessWatcher
import com.stirante.lolclient.libs.org.apache.http.client.methods.HttpGet
import com.stirante.lolclient.libs.org.apache.http.conn.ssl.SSLConnectionSocketFactory
import com.stirante.lolclient.libs.org.apache.http.impl.client.CloseableHttpClient
import com.stirante.lolclient.libs.org.apache.http.impl.client.HttpClients
import com.stirante.lolclient.libs.org.apache.http.util.EntityUtils
import com.stirante.lolclient.utils.SSLUtil
import java.io.*
import java.net.URI
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern
import javax.net.ssl.HostnameVerifier

object LeagueConnectionUtil {
    private val INSTALL_DIR = Pattern.compile(".+\"--install-directory=([^\"]+)\".+")

    fun tryLcuRequest(logConsumer: Consumer<String>) {
        logConsumer.accept("Using " + ProcessWatcher.getInstance().javaClass.name)
        val s1 = ProcessWatcher.getInstance().installDirectory.get()
        logConsumer.accept("Result from ProcessWatcher: $s1")
        var target: String? = null
        var found = false
        var process = Runtime.getRuntime().exec("WMIC PROCESS WHERE name='LeagueClientUx.exe' GET commandline")
        var inputStream = process.inputStream
        var sc = Scanner(inputStream)
        while (sc.hasNextLine()) {
            val s = sc.nextLine()
            logConsumer.accept(s)
            if (s.contains("LeagueClientUx.exe") && s.contains("--install-directory=")) {
                logConsumer.accept("Found correct process")
                found = true
                target = s
                break
            }
        }
        inputStream.close()
        process.destroy()
        if (!found) {
            process = Runtime.getRuntime().exec("WMIC PROCESS GET name,commandline /format:csv")
            inputStream = process.inputStream
            sc = Scanner(inputStream)
            while (sc.hasNextLine()) {
                val s = sc.nextLine()
                logConsumer.accept(s)
            }
            inputStream.close()
            process.destroy()
        } else {
            val matcher = INSTALL_DIR.matcher(target!!)
            if (matcher.find()) {
                val clientPath = File(matcher.group(1)).absolutePath
                val path = File(File(clientPath), "lockfile").absolutePath
                val lockfile = readFile(path)
                if (lockfile == null) {
                    logConsumer.accept("Lockfile not found!")
                } else {
                    logConsumer.accept("Lockfile found: $lockfile")
                    val split = lockfile.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val password = split[3]
                    val token = String(Base64.getEncoder().encode("riot:$password".toByteArray()))
                    val port = split[2].toInt()
                    logConsumer.accept("Token: $token")
                    logConsumer.accept("Port: $port")
                    logConsumer.accept("Executing test request")

                    val client = createHttpClient()
                    val method = HttpGet()
                    method.uri = URI("https://127.0.0.1:$port/system/v1/builds")
                    method.addHeader("Authorization", "Basic $token")
                    method.addHeader("Accept", "*/*")
                    client.execute(method).use { response ->
                        val b = response.statusLine.statusCode == 200
                        if (!b) {
                            logConsumer.accept("Status code: " + response.statusLine.statusCode)
                        } else {
                            val t = dumpStream(response.entity.content)
                            EntityUtils.consume(response.entity)
                            logConsumer.accept("Response: $t")
                        }
                    }
                }
            }
        }
    }

    private fun createHttpClient(): CloseableHttpClient {
        return HttpClients.custom()
            .setSSLSocketFactory(SSLConnectionSocketFactory(SSLUtil.getSocketFactory(), null as HostnameVerifier?))
            .build()
    }

    private fun readFile(path: String): String? {
        try {
            val scanner = Scanner(InputStreamReader(FileInputStream(path)))
            val sb = StringBuilder()
            while (scanner.hasNextLine()) {
                if (sb.toString().isNotEmpty()) {
                    sb.append("\n")
                }
                sb.append(scanner.nextLine())
            }
            scanner.close()
            return sb.toString()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    private fun dumpStream(inputStream: InputStream): String? {
        val s = Scanner(inputStream).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }
}