package dev.ado.standby

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Pal(val accent: Int) {
    val fg = 0xFFE9E9E9.toInt()
    val dim = 0xFF8C8C8C.toInt()
    val faint = 0xFF2B2B2B.toInt()
    fun accentA(a: Int) = (accent and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
}

/** Small drawing helpers. */
object G {
    private val b = Rect()

    /** Sets the text size so that [template] fits in maxW x maxH. */
    fun fit(p: Paint, template: String, maxW: Float, maxH: Float) {
        p.textSize = 200f
        val w = p.measureText(template)
        p.getTextBounds(template, 0, template.length, b)
        val s = min(maxW / max(w, 1f), maxH / max(b.height(), 1).toFloat())
        p.textSize = 200f * s
    }

    fun capHeight(p: Paint): Float {
        p.getTextBounds("0", 0, 1, b)
        return b.height().toFloat()
    }

    /** Draws text with the middle of its digits at cy. */
    fun text(c: Canvas, s: String, x: Float, cy: Float, p: Paint) {
        c.drawText(s, x, cy + capHeight(p) / 2f, p)
    }
}

interface Theme {
    fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config)
}

class Renderer {
    private val themes: List<Theme> = listOf(Tactical(), Orbit(), Flip(), Minimal(), Space())
    private val info = Info()
    private val clock = RectF()
    private val side = RectF()
    private val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    /** Draws one frame into a w x h area. The canvas is already rotated. */
    fun draw(c: Canvas, w: Float, h: Float, s: Scene, cfg: Config) {
        c.drawColor(Color.BLACK)
        val u = min(w, h) / 100f
        val pal = Pal(Prefs.ACCENT_COLORS[cfg.accent])
        val wide = w > h
        val pad = 6f * u
        val hasInfo = cfg.showDate || cfg.showBattery ||
            (cfg.showMusic && !s.musicTitle.isNullOrBlank()) ||
            (cfg.showNotifs && s.icons.isNotEmpty())

        c.save()
        if (cfg.pixelShift) {
            // Move the picture a small distance each minute to prevent screen burn-in.
            val k = s.epochMinute
            c.translate(((k % 5) - 2) * 0.6f * u, (((k / 5) % 5) - 2) * 0.6f * u)
        }
        if (wide) {
            val split = if (hasInfo) w * 0.64f else w - pad
            clock.set(pad * 1.6f, pad, split, h - pad)
            side.set(split + 5f * u, pad, w - pad * 1.6f, h - pad)
        } else {
            val split = if (hasInfo) h * 0.69f else h * 0.92f
            clock.set(pad, h * 0.09f, w - pad, split)
            side.set(pad, split + 5f * u, w - pad, h * 0.96f)
        }
        themes[cfg.theme.coerceIn(0, themes.size - 1)].draw(c, clock, s, pal, u, cfg)
        if (hasInfo) info.draw(c, side, s, cfg, pal, u, wide)
        c.restore()

        val hint = s.hint
        if (hint != null) {
            hintPaint.textSize = 2.9f * u
            hintPaint.color = pal.dim
            hintPaint.textAlign = Paint.Align.CENTER
            c.drawText(hint, w / 2f, h - 3f * u, hintPaint)
        }
    }
}

/** Date, battery, music and notification icons. */
class Info {
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = PorterDuffColorFilter(0xFFC4C4C4.toInt(), PorterDuff.Mode.SRC_IN)
    }
    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val regular = Typeface.create("sans-serif", Typeface.NORMAL)
    private val dst = RectF()

    fun draw(c: Canvas, r: RectF, s: Scene, cfg: Config, pal: Pal, u: Float, wide: Boolean) {
        val showMusic = cfg.showMusic && !s.musicTitle.isNullOrBlank()
        val showIcons = cfg.showNotifs && s.icons.isNotEmpty()
        val hDate = if (cfg.showDate) 8.5f * u else 0f
        val hBat = if (cfg.showBattery) 8.5f * u else 0f
        val hMus = if (showMusic) 14f * u else 0f
        val hIco = if (showIcons) 10f * u else 0f
        val center = !wide
        var y = if (wide) r.centerY() - (hDate + hBat + hMus + hIco) / 2f else r.top
        val ax = if (center) r.centerX() else r.left

        if (cfg.showDate) {
            tp.typeface = medium
            tp.textSize = 4.6f * u
            tp.letterSpacing = 0.16f
            tp.color = pal.fg
            tp.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            G.text(c, s.dateText, ax, y + hDate / 2f, tp)
            y += hDate
        }

        if (cfg.showBattery) {
            val bw = 8f * u
            val bh = 3.8f * u
            val gap = 2.2f * u
            val label = "${s.battery}%" + if (s.charging) "  CHARGING" else ""
            tp.typeface = regular
            tp.textSize = 3.7f * u
            tp.letterSpacing = 0.1f
            tp.color = pal.dim
            tp.textAlign = Paint.Align.LEFT
            val total = bw + 0.9f * u + gap + tp.measureText(label)
            val x0 = if (center) ax - total / 2f else ax
            val cy = y + hBat / 2f
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.35f * u
            p.color = pal.dim
            c.drawRoundRect(x0, cy - bh / 2f, x0 + bw, cy + bh / 2f, 0.8f * u, 0.8f * u, p)
            p.style = Paint.Style.FILL
            c.drawRect(x0 + bw + 0.3f * u, cy - bh * 0.22f, x0 + bw + 0.9f * u, cy + bh * 0.22f, p)
            p.color = if (s.battery <= 15 && !s.charging) 0xFFFF5050.toInt() else pal.accent
            val inset = 0.8f * u
            val fill = (bw - 2f * inset) * s.battery.coerceIn(0, 100) / 100f
            c.drawRoundRect(
                x0 + inset, cy - bh / 2f + inset, x0 + inset + fill, cy + bh / 2f - inset,
                0.3f * u, 0.3f * u, p,
            )
            G.text(c, label, x0 + bw + 0.9f * u + gap, cy, tp)
            y += hBat
        }

        if (showMusic) {
            val eqW = 3.6f * u
            val gap = 2.4f * u
            val maxText = r.width() - eqW - gap
            tp.textAlign = Paint.Align.LEFT
            tp.letterSpacing = 0.02f
            tp.typeface = medium
            tp.textSize = 4f * u
            val title = TextUtils.ellipsize(s.musicTitle ?: "", tp, maxText, TextUtils.TruncateAt.END).toString()
            val wTitle = tp.measureText(title)
            tp.typeface = regular
            tp.textSize = 3.3f * u
            val artist = TextUtils.ellipsize(s.musicArtist ?: "", tp, maxText, TextUtils.TruncateAt.END).toString()
            val wArtist = tp.measureText(artist)
            val total = eqW + gap + max(wTitle, wArtist)
            val x0 = if (center) ax - total / 2f else ax
            val base = y + 8.6f * u
            p.style = Paint.Style.FILL
            p.color = pal.accent
            for (i in 0 until 3) {
                val hgt = when {
                    !s.musicPlaying -> 0.9f * u
                    s.animated -> (1.2f + 3.6f * abs(sin(s.t * (3.1f + i * 1.3f) + i * 2f))) * u
                    else -> floatArrayOf(2.6f, 4.6f, 1.8f)[i] * u
                }
                val bx = x0 + i * 1.3f * u
                c.drawRect(bx, base - hgt, bx + 0.85f * u, base, p)
            }
            val tx = x0 + eqW + gap
            tp.typeface = medium
            tp.textSize = 4f * u
            tp.color = pal.fg
            G.text(c, title, tx, y + 4.6f * u, tp)
            if (artist.isNotEmpty()) {
                tp.typeface = regular
                tp.textSize = 3.3f * u
                tp.color = pal.dim
                G.text(c, artist, tx, y + 10f * u, tp)
            }
            y += hMus
        }

        if (showIcons) {
            val size = 5.6f * u
            val gap = 2.6f * u
            val n = min(s.icons.size, 7)
            val total = n * size + (n - 1) * gap
            var x = if (center) ax - total / 2f else ax
            val top = y + (hIco - size) / 2f
            for (i in 0 until n) {
                dst.set(x, top, x + size, top + size)
                c.drawBitmap(s.icons[i], null, dst, iconPaint)
                x += size + gap
            }
        }
    }
}
