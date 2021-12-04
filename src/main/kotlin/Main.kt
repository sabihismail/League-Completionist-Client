
import tornadofx.launch
import ui.MainApp


const val DEBUG_LOG_ENDPOINTS = false
const val DEBUG_FAKE_UI_DATA = false

fun main(args: Array<String>) {
    launch<MainApp>(args)
}
