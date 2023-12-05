package util

import java.nio.file.FileSystemNotFoundException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.spi.FileSystemProvider

object ResourceUtils {
    fun getResource(classLoader: ClassLoader, name: String): Path {
        val uri = classLoader.getResource(name)!!.toURI()
        if ("jar" == uri.scheme) {
            for (provider in FileSystemProvider.installedProviders()) {
                if (provider.scheme.equals("jar", ignoreCase = true)) {
                    try {
                        provider.getFileSystem(uri)
                    } catch (e: FileSystemNotFoundException) {
                        // in this case we need to initialize it first:
                        provider.newFileSystem(uri, mapOf<String, Int>())
                    }
                }
            }
        }

        return Paths.get(uri)
    }
}