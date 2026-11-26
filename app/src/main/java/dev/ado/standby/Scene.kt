package dev.ado.standby

import android.graphics.Bitmap

/** The data for one frame. The view fills it, the renderer reads it. */
class Scene {
    var hour = 0
    var minute = 0
    var second = 0
    var secFrac = 0f
    var is24 = true
    var epochMinute = 0L
    var dateText = ""
    var battery = 100
    var charging = false
    var musicTitle: String? = null
    var musicArtist: String? = null
    var musicPlaying = false
    var icons: List<Bitmap> = emptyList()
    /** True when the screen redraws many times each second. */
    var animated = false
    /** Seconds from the start of the view. Used for animation. */
    var t = 0f
    var hint: String? = null

    val hh get() = two(hourShown(hour))
    val mm get() = two(minute)
    val ss get() = two(second)

    fun hourShown(h: Int) = if (is24) h else (h + 11) % 12 + 1

    companion object {
        fun two(n: Int) = if (n < 10) "0$n" else "$n"
    }
}
