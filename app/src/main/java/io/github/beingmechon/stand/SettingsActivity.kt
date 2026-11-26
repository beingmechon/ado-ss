package io.github.beingmechon.stand

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
import android.widget.EditText
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/** The settings screen. It is built in code, so the app needs no layout files. */
class SettingsActivity : Activity() {
    private lateinit var sp: SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var preview: StandView
    private var d = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sp = Prefs.sp(this)
        d = resources.displayMetrics.density
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(40))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        // A live preview stays at the top. It shows each change immediately.
        preview = StandView(this, false, true)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = true
            addView(preview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(190)))
            addView(View(this@SettingsActivity).apply { setBackgroundColor(0xFF2B2B2B.toInt()) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(page)
    }

    override fun onResume() {
        super.onResume()
        preview.start()
        build()
    }

    override fun onPause() {
        preview.stop()
        super.onPause()
    }

    private fun dp(v: Int) = (v * d).toInt()

    private fun build() {
        val c = Prefs.load(this)
        root.removeAllViews()
        title("Stand display settings")

        header("Look")
        choice("Theme", Prefs.THEME, Prefs.THEMES, c.theme)
        choice("Change the theme automatically", Prefs.AUTO_THEME, Prefs.AUTO_NAMES, c.autoTheme)
        choice("Background", Prefs.BACKDROP, Prefs.BACKDROPS, c.backdrop)
        choice("Clock font", Prefs.FONT, Prefs.FONTS, c.font)
        toggle("Glow", Prefs.GLOW, c.glow)
        toggle("Colour gradient", Prefs.GRADIENT, c.gradient)

        header("Colour")
        choice("Accent colour", Prefs.ACCENT, Prefs.ACCENTS, c.accent)
        slider("Custom colour", Prefs.HUE, c.hue)
        choice("Night mode", Prefs.NIGHT, Prefs.NIGHTS, c.night)

        header("Show")
        toggle("Seconds", Prefs.SECONDS, c.showSeconds)
        toggle("Date", Prefs.DATE, c.showDate)
        choice("Date format", Prefs.DATE_FORMAT, Prefs.DATE_FORMATS, c.dateFormat)
        choice("Clock format", Prefs.HOUR_MODE, Prefs.HOUR_MODES, c.hourMode)
        textRow("Your text", Prefs.CUSTOM_TEXT, c.customText)
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

    /** A slider for the hue of the custom colour. A touch selects the custom colour. */
    private fun slider(label: String, key: String, value: Int) {
        root.addView(text(label, 16f, Color.WHITE).apply { setPadding(0, dp(8), 0, 0) })
        val bar = SeekBar(this).apply {
            max = 360
            progress = value
            minimumHeight = dp(48)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, v: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    sp.edit().putInt(key, v).putInt(Prefs.ACCENT, Prefs.ACCENTS.size - 1).apply()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) { build() }
            })
        }
        root.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun textRow(label: String, key: String, value: String) {
        action(label, if (value.isBlank()) "None. Tap to add a name or a short message." else value) {
            val edit = EditText(this).apply {
                setText(value)
                setSingleLine()
                hint = "For example: STAY FOCUSED"
            }
            AlertDialog.Builder(this)
                .setTitle(label)
                .setView(edit)
                .setPositiveButton("OK") { _, _ ->
                    sp.edit().putString(key, edit.text.toString().trim().take(60)).apply()
                    build()
                }
                .setNegativeButton("Cancel", null)
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
