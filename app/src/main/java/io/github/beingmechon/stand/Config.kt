package io.github.beingmechon.stand

import android.content.Context
import android.content.SharedPreferences

/** All user settings. Most values are an index into a list in [Prefs]. */
data class Config(
    val theme: Int = 0,
    val accent: Int = 0,
    val hue: Int = 200,
    val font: Int = 0,
    val glow: Boolean = true,
    val gradient: Boolean = false,
    val backdrop: Int = 0,
    val autoTheme: Int = 0,
    val night: Int = 0,
    val dateFormat: Int = 0,
    val customText: String = "",
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
    const val HUE = "hue"
    const val FONT = "font"
    const val GLOW = "glow"
    const val GRADIENT = "gradient"
    const val BACKDROP = "backdrop"
    const val AUTO_THEME = "autoTheme"
    const val NIGHT = "night"
    const val DATE_FORMAT = "dateFormat"
    const val CUSTOM_TEXT = "customText"
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

    val THEMES = listOf(
        "Tactical", "Orbit", "Flip", "Minimal", "Space", "Dots",
        "LED", "Words", "Analog", "Neon", "Mega", "Radar",
    )
    val ACCENTS = listOf(
        "Army green", "Amber", "Cyan", "Red", "White", "Violet",
        "Lime", "Pink", "Orange", "Blue", "Mint", "Custom (use the slider)",
    )
    val ACCENT_COLORS = intArrayOf(
        0xFF86994A.toInt(), 0xFFFFB000.toInt(), 0xFF35D6E6.toInt(),
        0xFFFF5050.toInt(), 0xFFF2F2F2.toInt(), 0xFFB48CFF.toInt(),
        0xFFB6FF3C.toInt(), 0xFFFF4FA3.toInt(), 0xFFFF7A1A.toInt(),
        0xFF4D8DFF.toInt(), 0xFF3DFFB0.toInt(), 0,
    )
    val FONTS = listOf("Theme default", "Thin", "Light", "Bold", "Black", "Condensed", "Mono", "Serif")
    val BACKDROPS = listOf("None", "Stars", "Fireflies", "Neon grid", "Waves", "Pulse")
    val AUTO_NAMES = listOf("Off", "Each minute", "Each 5 minutes", "Each 15 minutes", "Each hour")
    val AUTO_MINUTES = intArrayOf(0, 1, 5, 15, 60)
    val NIGHTS = listOf("Off", "Dim (22:00 to 06:00)", "Red (22:00 to 06:00)")
    val DATE_FORMATS = listOf("THU 26 NOV", "THURSDAY", "26 NOVEMBER 2026", "THURSDAY, 26 NOV")
    val DATE_PATTERNS = listOf("EEE d MMM", "EEEE", "d MMMM yyyy", "EEEE, d MMM")
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
            hue = s.getInt(HUE, d.hue).coerceIn(0, 360),
            font = s.getInt(FONT, d.font).coerceIn(0, FONTS.size - 1),
            glow = s.getBoolean(GLOW, d.glow),
            gradient = s.getBoolean(GRADIENT, d.gradient),
            backdrop = s.getInt(BACKDROP, d.backdrop).coerceIn(0, BACKDROPS.size - 1),
            autoTheme = s.getInt(AUTO_THEME, d.autoTheme).coerceIn(0, AUTO_MINUTES.size - 1),
            night = s.getInt(NIGHT, d.night).coerceIn(0, 2),
            dateFormat = s.getInt(DATE_FORMAT, d.dateFormat).coerceIn(0, DATE_PATTERNS.size - 1),
            customText = s.getString(CUSTOM_TEXT, d.customText) ?: "",
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
