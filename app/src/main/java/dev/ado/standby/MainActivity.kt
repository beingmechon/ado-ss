package dev.ado.standby

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

/** The full-screen stand display. */
class MainActivity : Activity() {
    private lateinit var view: StandView
    private val handler = Handler(Looper.getMainLooper())
    private val release = Runnable {
        // Let the phone turn the screen off with its usual timeout.
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        view = StandView(this, true)
        view.onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
        view.onStateChanged = { applyWindow() }
        setContentView(view)
    }

    override fun onStart() {
        super.onStart()
        view.start()
        applyWindow()
    }

    override fun onStop() {
        handler.removeCallbacks(release)
        view.stop()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.insetsController?.let {
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                it.hide(WindowInsets.Type.systemBars())
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // A touch starts the screen-off timer again.
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) applyWindow()
        return super.dispatchTouchEvent(ev)
    }

    private fun applyWindow() {
        val cfg = view.cfg
        val lp = window.attributes
        val b = Prefs.BRIGHTNESS_VALUES[cfg.brightness]
        if (lp.screenBrightness != b) {
            lp.screenBrightness = b
            window.attributes = lp
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.removeCallbacks(release)
        val minutes = Prefs.OFF_MINUTES[cfg.offAfter]
        if (!view.charging && minutes > 0) handler.postDelayed(release, minutes * 60_000L)
    }
}
