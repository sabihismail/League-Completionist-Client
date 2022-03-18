
import tornadofx.launch
import ui.MainApp


const val DEBUG_FAKE_UI_DATA_ARAM = false
const val DEBUG_FAKE_UI_DATA_NORMAL = true
const val DEBUG_LOG_ALL_ENDPOINTS = false
@Suppress("SimplifyBooleanWithConstants")
const val DEBUG_LOG_HANDLED_ENDPOINTS = !DEBUG_LOG_ALL_ENDPOINTS && true

fun main(args: Array<String>) {
    launch<MainApp>(args)
}
