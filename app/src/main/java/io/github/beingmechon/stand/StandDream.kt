package io.github.beingmechon.stand

import android.service.dreams.DreamService

/** The system screensaver. The phone starts it when it charges, if the user selects it. */
class StandDream : DreamService() {
    private var view: StandView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = Prefs.load(this).brightness != 1
        val v = StandView(this, false)
        view = v
        setContentView(v)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        view?.start()
    }

    override fun onDreamingStopped() {
        view?.stop()
        super.onDreamingStopped()
    }
}
