@file:Suppress("unused")

package util

import oshi.SystemInfo
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter


object Crypto {
    private fun generateHardwareHash(): String {
        val systemInfo = SystemInfo()
        val operatingSystem = systemInfo.operatingSystem
        val hardwareAbstractionLayer = systemInfo.hardware
        val centralProcessor = hardwareAbstractionLayer.processor
        val computerSystem = hardwareAbstractionLayer.computerSystem
        val vendor: String = operatingSystem.manufacturer
        val processorSerialNumber: String = computerSystem.serialNumber
        val processorIdentifier = centralProcessor.processorIdentifier
        val processors: Int = centralProcessor.logicalProcessorCount

        val lst = listOf(vendor, processorSerialNumber, processorIdentifier, processors)
        return lst.joinToString("#")
    }

    fun encrypt(str: String): String {
        val hardwareHash = generateHardwareHash()

        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val spec: KeySpec = PBEKeySpec(hardwareHash.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secret: SecretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secret)
        val params = cipher.parameters
        val iv = params.getParameterSpec(IvParameterSpec::class.java).iv
        val encryptedText = cipher.doFinal(str.toByteArray())

        val outputStream = ByteArrayOutputStream()
        outputStream.write(salt)
        outputStream.write(iv)
        outputStream.write(encryptedText)

        return DatatypeConverter.printBase64Binary(outputStream.toByteArray())
    }

    fun decrypt(str: String): String {
        val hardwareHash = generateHardwareHash()

        val ciphertext = DatatypeConverter.parseBase64Binary(str)
        if (ciphertext.size < 48) {
            throw Exception("Ciphertext too small")
        }

        val salt = Arrays.copyOfRange(ciphertext, 0, 16)
        val iv = Arrays.copyOfRange(ciphertext, 16, 32)
        val ct = Arrays.copyOfRange(ciphertext, 32, ciphertext.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val spec: KeySpec = PBEKeySpec(hardwareHash.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secret: SecretKey = SecretKeySpec(tmp.encoded, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        cipher.init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(iv))
        val plaintext = cipher.doFinal(ct)

        return String(plaintext, Charsets.UTF_8)
    }
}
