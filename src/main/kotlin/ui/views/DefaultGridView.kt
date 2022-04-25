package ui.views

import tornadofx.View
import tornadofx.borderpane
import tornadofx.vbox


class DefaultGridView: View() {
    private var currentView = object: View() {
        override val root = vbox {  }
    }

    override val root = borderpane {
        center = currentView.root
    }

    fun setRoot(view: View) {
        if (isAlreadyBound(view)) return

        root.center = view.root
    }

    private fun isAlreadyBound(view: View): Boolean {
        return currentView == view
    }
}
