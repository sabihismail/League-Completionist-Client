package util

object KotlinExtensionUtil {
    fun Int.toReadableNumber(): String {
        return "%,d".format(this)
    }
}