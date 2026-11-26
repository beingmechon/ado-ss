package dev.ado.standby

import android.content.Context
import android.content.SharedPreferences

/** All user settings. Each value is an index into a list in [Prefs]. */
data class Config(
    val theme: Int = 0,
    val accent: Int = 0,
    val showSeconds: Boolean = true,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val showMusic: Boolean = true,
    val showNotifs: Boolean = true,
    val motion: Int = 0,
    val brightness: Int = 0,
    val offAfter: Int = 3,
    val orientation: Int = 0,
    val hourMode: Int = 0,
    val pixelShift: Boolean = true,
)

object Prefs {
    const val THEME = "theme"
    const val ACCENT = "accent"
    const val SECONDS = "seconds"
    const val DATE = "date"
    const val BATTERY = "battery"
    const val MUSIC = "music"
    const val NOTIFS = "notifs"
    const val MOTION = "motion"
    const val BRIGHTNESS = "brightness"
    const val OFF_AFTER = "offAfter"
    const val ORIENTATION = "orientation"
    const val HOUR_MODE = "hourMode"
    const val PIXEL_SHIFT = "pixelShift"

    val THEMES = listOf("Tactical", "Orbit", "Flip", "Minimal", "Space")
    val ACCENTS = listOf("Olive", "Amber", "Cyan", "Red", "White", "Violet")
    val ACCENT_COLORS = intArrayOf(
        0xFFA3BA6C.toInt(), 0xFFFFB000.toInt(), 0xFF35D6E6.toInt(),
        0xFFFF5050.toInt(), 0xFFF2F2F2.toInt(), 0xFFB48CFF.toInt(),
    )
    val MOTIONS = listOf("Auto (smooth only when charging)", "Always smooth", "Off (lowest power)")
    val BRIGHTNESS_NAMES = listOf("Same as phone", "Low", "Medium", "High")
    val BRIGHTNESS_VALUES = floatArrayOf(-1f, 0.06f, 0.3f, 0.8f)
    val OFF_NAMES = listOf("Never", "5 minutes", "15 minutes", "30 minutes", "1 hour")
    val OFF_MINUTES = intArrayOf(0, 5, 15, 30, 60)
    val ORIENTATIONS = listOf("Auto (tilt sensor)", "Vertical", "Horizontal, top to the left", "Horizontal, top to the right")
    val HOUR_MODES = listOf("Same as phone", "12 hour", "24 hour")

    fun sp(ctx: Context): SharedPreferences = ctx.getSharedPreferences("stand", Context.MODE_PRIVATE)

    fun load(ctx: Context): Config {
        val s = sp(ctx)
        val d = Config()
        return Config(
            theme = s.getInt(THEME, d.theme).coerceIn(0, THEMES.size - 1),
            accent = s.getInt(ACCENT, d.accent).coerceIn(0, ACCENTS.size - 1),
            showSeconds = s.getBoolean(SECONDS, d.showSeconds),
            showDate = s.getBoolean(DATE, d.showDate),
            showBattery = s.getBoolean(BATTERY, d.showBattery),
            showMusic = s.getBoolean(MUSIC, d.showMusic),
            showNotifs = s.getBoolean(NOTIFS, d.showNotifs),
            motion = s.getInt(MOTION, d.motion).coerceIn(0, 2),
            brightness = s.getInt(BRIGHTNESS, d.brightness).coerceIn(0, 3),
            offAfter = s.getInt(OFF_AFTER, d.offAfter).coerceIn(0, OFF_MINUTES.size - 1),
            orientation = s.getInt(ORIENTATION, d.orientation).coerceIn(0, 3),
            hourMode = s.getInt(HOUR_MODE, d.hourMode).coerceIn(0, 2),
            pixelShift = s.getBoolean(PIXEL_SHIFT, d.pixelShift),
        )
    }
}
