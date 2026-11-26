package dev.ado.standby

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** The settings screen. It is built in code, so the app needs no layout files. */
class SettingsActivity : Activity() {
    private lateinit var sp: SharedPreferences
    private lateinit var root: LinearLayout
    private var d = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sp = Prefs.sp(this)
        d = resources.displayMetrics.density
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(40))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = true
            addView(root)
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun dp(v: Int) = (v * d).toInt()

    private fun build() {
        val c = Prefs.load(this)
        root.removeAllViews()
        title("Stand display settings")

        header("Look")
        choice("Theme", Prefs.THEME, Prefs.THEMES, c.theme)
        choice("Accent colour", Prefs.ACCENT, Prefs.ACCENTS, c.accent)
        choice("Clock format", Prefs.HOUR_MODE, Prefs.HOUR_MODES, c.hourMode)

        header("Show")
        toggle("Seconds", Prefs.SECONDS, c.showSeconds)
        toggle("Date", Prefs.DATE, c.showDate)
        toggle("Battery level", Prefs.BATTERY, c.showBattery)
        if (Hub.available(this)) {
            toggle("Music that plays now", Prefs.MUSIC, c.showMusic)
            toggle("Notification icons", Prefs.NOTIFS, c.showNotifs)
        }

        header("Power")
        choice("Motion", Prefs.MOTION, Prefs.MOTIONS, c.motion)
        choice("Brightness", Prefs.BRIGHTNESS, Prefs.BRIGHTNESS_NAMES, c.brightness)
        choice("On battery, let the screen go off after", Prefs.OFF_AFTER, Prefs.OFF_NAMES, c.offAfter)
        toggle("Pixel shift (prevents screen burn-in)", Prefs.PIXEL_SHIFT, c.pixelShift)

        header("Position")
        choice("Orientation", Prefs.ORIENTATION, Prefs.ORIENTATIONS, c.orientation)

        header("Access")
        if (Hub.available(this)) {
            val access = Hub.hasAccess(this)
            action(
                "Notification access",
                if (access) "On. Music and notification icons can show." else "Off. Tap to open the phone setting. Music and icons need it.",
            ) { open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
            note(
                "If the phone shows \"Restricted setting\": open Settings > Apps > Stand > the 3 dots > " +
                    "Allow restricted settings. Then try again.",
            )
        } else {
            note("This is the Lite version. It cannot show music or notification icons. See the README for the Full version.")
        }
        action("Use as screensaver", "Tap to open the phone screensaver setting. It starts when the phone charges.") {
            open(Settings.ACTION_DREAM_SETTINGS)
        }
    }

    private fun open(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: Exception) {
            try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (e2: Exception) { }
        }
    }

    private fun text(s: String, size: Float, color: Int) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
    }

    private fun title(s: String) {
        root.addView(text(s, 22f, Color.WHITE).apply { setPadding(0, dp(12), 0, dp(4)) })
    }

    private fun header(s: String) {
        root.addView(text(s.uppercase(), 12f, Prefs.ACCENT_COLORS[0]).apply {
            letterSpacing = 0.15f
            setPadding(0, dp(26), 0, dp(6))
        })
    }

    private fun note(s: String) {
        root.addView(text(s, 13f, 0xFF8C8C8C.toInt()).apply { setPadding(0, dp(14), 0, 0) })
    }

    private fun toggle(label: String, key: String, value: Boolean) {
        val sw = Switch(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = value
            minHeight = dp(52)
            setOnCheckedChangeListener { _, on -> sp.edit().putBoolean(key, on).apply() }
        }
        root.addView(sw, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun choice(label: String, key: String, names: List<String>, value: Int) {
        action(label, names[value]) {
            AlertDialog.Builder(this)
                .setTitle(label)
                .setSingleChoiceItems(names.toTypedArray(), value) { dialog, which ->
                    sp.edit().putInt(key, which).apply()
                    dialog.dismiss()
                    build()
                }
                .show()
        }
    }

    private fun action(label: String, value: String, onClick: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            setPadding(0, dp(8), 0, dp(8))
            isClickable = true
            setOnClickListener(View.OnClickListener { onClick() })
            addView(text(label, 16f, Color.WHITE))
            addView(text(value, 14f, 0xFF9A9A9A.toInt()))
        }
        root.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }
}
