package dev.ado.standby

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** The colours and the style for one frame. */
class Pal(cfg: Config, night: Boolean) {
    val accent: Int
    val accent2: Int
    val fg: Int
    val dim: Int
    val faint: Int
    val glow = cfg.glow
    val gradient = cfg.gradient
    private val customFace: Typeface? = when (cfg.font) {
        1 -> Typeface.create("sans-serif-thin", Typeface.NORMAL)
        2 -> Typeface.create("sans-serif-light", Typeface.NORMAL)
        3 -> Typeface.create("sans-serif", Typeface.BOLD)
        4 -> Typeface.create("sans-serif-black", Typeface.NORMAL)
        5 -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
        6 -> Typeface.MONOSPACE
        7 -> Typeface.create("serif", Typeface.NORMAL)
        else -> null
    }

    init {
        if (night && cfg.night == 2) {
            // Red light is easy on the eyes in a dark room.
            accent = 0xFFC02A1C.toInt()
            accent2 = 0xFF7A1A10.toInt()
            fg = 0xFFD8402E.toInt()
            dim = 0xFF7A2A20.toInt()
            faint = 0xFF2A0D0A.toInt()
        } else {
            val hsv = FloatArray(3)
            if (cfg.accent == Prefs.ACCENT_COLORS.size - 1) {
                hsv[0] = cfg.hue.toFloat(); hsv[1] = 0.78f; hsv[2] = 1f
                accent = Color.HSVToColor(hsv)
            } else {
                accent = Prefs.ACCENT_COLORS[cfg.accent]
                Color.colorToHSV(accent, hsv)
            }
            if (hsv[1] < 0.12f) {
                accent2 = 0xFF6FA8FF.toInt()
            } else {
                hsv[0] = (hsv[0] + 55f) % 360f
                accent2 = Color.HSVToColor(hsv)
            }
            fg = 0xFFE9E9E9.toInt()
            dim = 0xFF8C8C8C.toInt()
            faint = 0xFF2B2B2B.toInt()
        }
    }

    fun face(default: Typeface) = customFace ?: default
    fun accentA(a: Int) = (accent and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
    fun accent2A(a: Int) = (accent2 and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
    fun fgA(a: Int) = (fg and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
}

/** Small drawing helpers. */
object G {
    private val b = Rect()
    private var grad: LinearGradient? = null
    private var gTop = 0f
    private var gBottom = 0f
    private var gA = 0
    private var gB = 0

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

    fun gradient(top: Float, bottom: Float, a: Int, b: Int): Shader {
        val g = grad
        if (g != null && gTop == top && gBottom == bottom && gA == a && gB == b) return g
        val n = LinearGradient(0f, top, 0f, bottom, a, b, Shader.TileMode.CLAMP)
        grad = n; gTop = top; gBottom = bottom; gA = a; gB = b
        return n
    }

    /**
     * Sets the colour of a large item. It adds the glow and the gradient if the user selected them.
     * Call [plain] after the draw.
     */
    fun ink(p: Paint, pal: Pal, color: Int, top: Float, bottom: Float, u: Float) {
        p.color = color
        p.shader = if (pal.gradient && color == pal.accent) gradient(top, bottom, pal.accent, pal.accent2) else null
        // White items get a weaker glow, or they look out of focus.
        if (pal.glow) glow(p, color, u, if (color == pal.fg) 1.5f else 2.6f) else p.clearShadowLayer()
    }

    fun glow(p: Paint, color: Int, u: Float, radius: Float = 2.6f) {
        p.setShadowLayer(radius * u, 0f, 0f, (color and 0x00FFFFFF) or (0xA0 shl 24))
    }

    fun plain(p: Paint) {
        p.shader = null
        p.clearShadowLayer()
    }
}

interface Theme {
    fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config)
}

class Renderer {
    private val themes: List<Theme> = listOf(
        Tactical(), Orbit(), Flip(), Minimal(), Space(), Dots(),
        Led(), Words(), Analog(), Neon(), Mega(), Radar(),
    )
    private val info = Info()
    private val backdrop = Backdrop()
    private val clock = RectF()
    private val side = RectF()
    private val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val shade = Paint()
    private var pal: Pal? = null
    private var palCfg: Config? = null
    private var palNight = false

    /** Draws one frame into a w x h area. The canvas is already rotated. */
    fun draw(c: Canvas, w: Float, h: Float, s: Scene, cfg: Config) {
        c.drawColor(Color.BLACK)
        val u = min(w, h) / 100f
        val night = cfg.night != 0 && (s.hour >= 22 || s.hour < 6)
        var pal = this.pal
        if (pal == null || palCfg !== cfg || palNight != night) {
            pal = Pal(cfg, night)
            this.pal = pal; palCfg = cfg; palNight = night
        }
        val wide = w > h
        val pad = 6f * u
        val hasInfo = cfg.showDate || cfg.showBattery || cfg.customText.isNotBlank() ||
            (cfg.showMusic && !s.musicTitle.isNullOrBlank()) ||
            (cfg.showNotifs && s.icons.isNotEmpty())

        c.save()
        if (cfg.pixelShift) {
            // Move the picture a small distance each minute to prevent screen burn-in.
            val k = s.epochMinute
            c.translate(((k % 5) - 2) * 0.6f * u, (((k / 5) % 5) - 2) * 0.6f * u)
        }
        backdrop.draw(c, w, h, s, pal, u, cfg.backdrop)
        if (wide) {
            val split = if (hasInfo) w * 0.64f else w - pad
            clock.set(pad * 1.6f, pad, split, h - pad)
            side.set(split + 5f * u, pad, w - pad * 1.6f, h - pad)
        } else {
            val split = if (hasInfo) h * 0.69f else h * 0.92f
            clock.set(pad, h * 0.09f, w - pad, split)
            side.set(pad, split + 5f * u, w - pad, h * 0.96f)
        }
        var index = cfg.theme
        val every = Prefs.AUTO_MINUTES[cfg.autoTheme]
        if (every > 0) index = ((index + s.epochMinute / every) % themes.size).toInt()
        themes[index.coerceIn(0, themes.size - 1)].draw(c, clock, s, pal, u, cfg)
        if (hasInfo) info.draw(c, side, s, cfg, pal, u, wide)
        c.restore()

        if (night && cfg.night == 1) {
            shade.color = 0x99000000.toInt()
            c.drawRect(0f, 0f, w, h, shade)
        }
        val hint = s.hint
        if (hint != null) {
            hintPaint.textSize = 2.7f * u
            val hw = hintPaint.measureText(hint)
            if (hw > w * 0.94f) hintPaint.textSize *= w * 0.94f / hw
            hintPaint.color = pal.dim
            hintPaint.textAlign = Paint.Align.CENTER
            c.drawText(hint, w / 2f, h - 3f * u, hintPaint)
        }
    }
}

/** Date, battery, music, notification icons and the user text. */
class Info {
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var iconColor = 0
    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val regular = Typeface.create("sans-serif", Typeface.NORMAL)
    private val dst = RectF()

    fun draw(c: Canvas, r: RectF, s: Scene, cfg: Config, pal: Pal, u: Float, wide: Boolean) {
        val showMusic = cfg.showMusic && !s.musicTitle.isNullOrBlank()
        val showIcons = cfg.showNotifs && s.icons.isNotEmpty()
        val showText = cfg.customText.isNotBlank()
        val hDate = if (cfg.showDate) 8.5f * u else 0f
        val hBat = if (cfg.showBattery) 8.5f * u else 0f
        val hMus = if (showMusic) 14f * u else 0f
        val hIco = if (showIcons) 10f * u else 0f
        val hTxt = if (showText) 8f * u else 0f
        val center = !wide
        var y = if (wide) r.centerY() - (hDate + hBat + hMus + hIco + hTxt) / 2f else r.top
        val ax = if (center) r.centerX() else r.left

        if (cfg.showDate) {
            tp.typeface = medium
            tp.letterSpacing = 0.16f
            tp.textSize = 4.6f * u
            // Make a long date smaller, so that it stays in the area.
            val dw = tp.measureText(s.dateText)
            if (dw > r.width()) tp.textSize *= r.width() / dw
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
            var fill = (bw - 2f * inset) * s.battery.coerceIn(0, 100) / 100f
            if (s.charging && s.animated && s.battery < 100) {
                // The level moves up again and again while the phone charges.
                val f = (s.t % 2.4f) / 2.4f
                fill += ((bw - 2f * inset) - fill) * f
                p.color = pal.accentA((255 * (1f - f * 0.6f)).toInt())
            }
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
            if (iconColor != pal.dim) {
                iconColor = pal.dim
                iconPaint.colorFilter = PorterDuffColorFilter(pal.fgA(255), PorterDuff.Mode.SRC_IN)
                iconPaint.alpha = 200
            }
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
            y += hIco
        }

        if (showText) {
            tp.typeface = regular
            tp.textSize = 3.5f * u
            tp.letterSpacing = 0.08f
            tp.color = pal.accent
            tp.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            val t = TextUtils.ellipsize(cfg.customText, tp, r.width(), TextUtils.TruncateAt.END).toString()
            G.text(c, t, ax, y + hTxt / 2f, tp)
        }
    }
}
