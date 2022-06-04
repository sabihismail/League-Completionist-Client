package util

import kotlin.math.abs
import kotlin.math.log10

@Suppress("unused")
object KotlinExtensionUtil {
    fun Int.toCommaSeparatedNumber(): String {
        return "%,d".format(this)
    }

    fun Long.toCommaSeparatedNumber(): String {
        return "%,d".format(this)
    }

    fun Int.length() = when(this) {
        0 -> 1
        else -> log10(abs(toDouble())).toInt() + 1
    }

    fun Long.length() = when(this) {
        0L -> 1
        else -> log10(abs(toDouble())).toInt() + 1
    }
}