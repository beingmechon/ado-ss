package dev.ado.standby

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val AA = Paint.ANTI_ALIAS_FLAG
private const val TAU = (2.0 * PI).toFloat()

/** Theme 6: digits made of round dots, as on a dot-matrix sign. */
class Dots : Theme {
    private val p = Paint(AA)

    // Each digit is 5 dots wide and 7 dots high.
    private val font = arrayOf(
        "01110100011001110101110011000101110", "00100011000010000100001000010001110",
        "01110100010000100010001000100011111", "11110000010000101110000010000111110",
        "00010001100101010010111110001000010", "11111100001111000001000011000101110",
        "00110010001000011110100011000101110", "11111000010001000100010000100001000",
        "01110100011000101110100011000101110", "01110100011000101111000010001001100",
    )

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val wide = r.width() > r.height()
        val cols = if (wide) 25 else 11
        val rows = if (wide) 7 else 16
        val extra = if (cfg.showSeconds) 2 else 0
        val cell = min(r.width() * 0.96f / cols, r.height() * 0.9f / (rows + extra))
        val x0 = r.centerX() - cols * cell / 2f
        val y0 = r.centerY() - (rows + extra) * cell / 2f
        p.style = Paint.Style.FILL
        val h = s.hh
        val m = s.mm
        if (wide) {
            digit(c, x0, y0, cell, h[0] - '0', pal.fg, pal)
            digit(c, x0 + 6 * cell, y0, cell, h[1] - '0', pal.fg, pal)
            digit(c, x0 + 14 * cell, y0, cell, m[0] - '0', pal.accent, pal)
            digit(c, x0 + 20 * cell, y0, cell, m[1] - '0', pal.accent, pal)
            val on = !s.animated || s.secFrac < 0.5f
            for (row in 0 until 7) {
                val lit = on && (row == 2 || row == 4)
                dot(c, x0 + 12.5f * cell, y0 + (row + 0.5f) * cell, cell, lit, pal.dim, pal)
            }
        } else {
            digit(c, x0, y0, cell, h[0] - '0', pal.fg, pal)
            digit(c, x0 + 6 * cell, y0, cell, h[1] - '0', pal.fg, pal)
            digit(c, x0, y0 + 9 * cell, cell, m[0] - '0', pal.accent, pal)
            digit(c, x0 + 6 * cell, y0 + 9 * cell, cell, m[1] - '0', pal.accent, pal)
        }
        if (cfg.showSeconds) {
            // A row of dots that fills in one minute.
            val y = y0 + (rows + 1.5f) * cell
            val lit = (s.second + 1) * cols / 60
            for (i in 0 until cols) dot(c, x0 + (i + 0.5f) * cell, y, cell * 0.7f, i < lit, pal.accent, pal)
        }
    }

    private fun digit(c: Canvas, x: Float, y: Float, cell: Float, d: Int, color: Int, pal: Pal) {
        val f = font[d.coerceIn(0, 9)]
        for (row in 0 until 7) for (col in 0 until 5) {
            dot(c, x + (col + 0.5f) * cell, y + (row + 0.5f) * cell, cell, f[row * 5 + col] == '1', color, pal)
        }
    }

    private fun dot(c: Canvas, x: Float, y: Float, cell: Float, lit: Boolean, color: Int, pal: Pal) {
        p.color = if (lit) color else pal.faint
        c.drawCircle(x, y, cell * (if (lit) 0.41f else 0.17f), p)
    }
}

/** Theme 7: a 7-segment LED clock. */
class Led : Theme {
    private val p = Paint(AA)
    private val path = Path()

    // Segments a b c d e f g for the digits 0 to 9.
    private val map = intArrayOf(0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F)

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val wide = r.width() > r.height()
        val cx = r.centerX()
        val small = if (cfg.showSeconds) 0.34f else 0f
        val time = s.hh + s.mm
        c.save()
        // Lean the digits, as on a real LED clock.
        c.translate(cx, r.centerY())
        c.skew(-0.07f, 0f)
        c.translate(-cx, -r.centerY())
        val dw: Float
        val dh: Float
        val bottom: Float
        if (wide) {
            dh = min(r.height() * 0.62f / (1f + small * 1.25f), r.width() * 0.9f / 5.1f * 1.85f)
            dw = dh / 1.85f
            val gap = dw * 0.28f
            val mid = dw * 0.75f
            val total = 4 * dw + 2 * gap + mid
            val top = r.centerY() - dh * (1f + small * 1.25f) / 2f
            var x = cx - total / 2f
            for (i in 0 until 4) {
                digit(c, x, top, dw, dh, time[i] - '0', if (i < 2) pal.fg else pal.accent, pal, u)
                x += dw + if (i == 1) mid else gap
            }
            val on = !s.animated || s.secFrac < 0.5f
            p.style = Paint.Style.FILL
            p.color = if (on) pal.accent else pal.faint
            if (on && pal.glow) G.glow(p, pal.accent, u, 1.8f)
            c.drawCircle(cx, top + dh * 0.32f, dw * 0.085f, p)
            c.drawCircle(cx, top + dh * 0.68f, dw * 0.085f, p)
            p.clearShadowLayer()
            bottom = top + dh
        } else {
            dw = min(r.width() * 0.38f, r.height() * 0.84f / (2.15f + small * 1.25f) / 1.85f)
            dh = dw * 1.85f
            val gap = dw * 0.28f
            val vgap = dh * 0.15f
            val top = r.centerY() - (2 * dh + vgap + dh * small * 1.25f) / 2f
            for (i in 0 until 4) {
                val x = cx + if (i % 2 == 0) -dw - gap / 2f else gap / 2f
                val y = top + if (i < 2) 0f else dh + vgap
                digit(c, x, y, dw, dh, time[i] - '0', if (i < 2) pal.fg else pal.accent, pal, u)
            }
            bottom = top + 2 * dh + vgap
        }
        if (cfg.showSeconds) {
            val sw = dw * small
            val sh = dh * small
            val y = bottom + sh * 0.25f
            val ss = s.ss
            digit(c, cx - sw * 1.14f, y, sw, sh, ss[0] - '0', pal.dim, pal, u)
            digit(c, cx + sw * 0.14f, y, sw, sh, ss[1] - '0', pal.dim, pal, u)
        }
        c.restore()
    }

    private fun digit(c: Canvas, x: Float, y: Float, w: Float, h: Float, d: Int, color: Int, pal: Pal, u: Float) {
        val bits = map[d.coerceIn(0, 9)]
        val t = w * 0.19f
        val lh = w - t
        val lv = (h - t) / 2f
        p.style = Paint.Style.FILL
        for (pass in 0..1) {
            // Pass 0: the segments that are off. Pass 1: the segments that are on.
            if (pass == 1 && pal.glow && color != pal.dim) G.glow(p, color, u, 1.8f)
            p.color = if (pass == 0) 0xFF161616.toInt() else color
            for (i in 0 until 7) {
                val on = bits and (1 shl i) != 0
                if (on != (pass == 1)) continue
                when (i) {
                    0 -> seg(c, x + w / 2f, y + t / 2f, lh, t, true)
                    1 -> seg(c, x + w - t / 2f, y + t / 2f + lv / 2f, lv, t, false)
                    2 -> seg(c, x + w - t / 2f, y + h - t / 2f - lv / 2f, lv, t, false)
                    3 -> seg(c, x + w / 2f, y + h - t / 2f, lh, t, true)
                    4 -> seg(c, x + t / 2f, y + h - t / 2f - lv / 2f, lv, t, false)
                    5 -> seg(c, x + t / 2f, y + t / 2f + lv / 2f, lv, t, false)
                    else -> seg(c, x + w / 2f, y + h / 2f, lh, t, true)
                }
            }
            p.clearShadowLayer()
        }
    }

    /** One segment with pointed ends. */
    private fun seg(c: Canvas, cx: Float, cy: Float, len: Float, t: Float, horizontal: Boolean) {
        val l = len / 2f - t * 0.08f
        val k = t / 2f
        path.reset()
        if (horizontal) {
            path.moveTo(cx - l, cy); path.lineTo(cx - l + k, cy - k); path.lineTo(cx + l - k, cy - k)
            path.lineTo(cx + l, cy); path.lineTo(cx + l - k, cy + k); path.lineTo(cx - l + k, cy + k)
        } else {
            path.moveTo(cx, cy - l); path.lineTo(cx + k, cy - l + k); path.lineTo(cx + k, cy + l - k)
            path.lineTo(cx, cy + l); path.lineTo(cx - k, cy + l - k); path.lineTo(cx - k, cy - l + k)
        }
        path.close()
        c.drawPath(path, p)
    }
}

/** Theme 8: the time in words. */
class Words : Theme {
    private val tp = TextPaint(AA).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val p = Paint(AA)
    private val grid = arrayOf(
        "ITLISASAMPM", "ACQUARTERDC", "TWENTYFIVEX", "HALFSTENFTO", "PASTERUNINE",
        "ONESIXTHREE", "FOURFIVETWO", "EIGHTELEVEN", "SEVENTWELVE", "TENSEOCLOCK",
    )

    // 1 = a lit word, 2 = the lit hour word.
    private val lit = Array(10) { IntArray(11) }
    private var litKey = -1

    // Row, start and length of each hour word. Index 0 is twelve.
    private val hours = arrayOf(
        intArrayOf(8, 5, 6), intArrayOf(5, 0, 3), intArrayOf(6, 8, 3), intArrayOf(5, 6, 5),
        intArrayOf(6, 0, 4), intArrayOf(6, 4, 4), intArrayOf(5, 3, 3), intArrayOf(8, 0, 5),
        intArrayOf(7, 0, 5), intArrayOf(4, 7, 4), intArrayOf(9, 0, 3), intArrayOf(7, 5, 6),
    )

    private fun mark(row: Int, start: Int, len: Int, v: Int = 1) {
        for (i in start until start + len) lit[row][i] = v
    }

    private fun build(hour: Int, minute: Int) {
        for (row in lit) row.fill(0)
        mark(0, 0, 2); mark(0, 3, 2) // IT IS
        val m5 = minute / 5
        when (m5) {
            0 -> mark(9, 5, 6) // OCLOCK
            1, 11 -> mark(2, 6, 4) // FIVE
            2, 10 -> mark(3, 5, 3) // TEN
            3, 9 -> { mark(1, 0, 1); mark(1, 2, 7) } // A QUARTER
            4, 8 -> mark(2, 0, 6) // TWENTY
            5, 7 -> { mark(2, 0, 6); mark(2, 6, 4) } // TWENTY FIVE
            6 -> mark(3, 0, 4) // HALF
        }
        if (m5 in 1..6) mark(4, 0, 4) // PAST
        if (m5 > 6) mark(3, 9, 2) // TO
        val hw = hours[(hour + if (m5 > 6) 1 else 0) % 12]
        mark(hw[0], hw[1], hw[2], 2)
    }

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val key = s.hour * 60 + s.minute
        if (key != litKey) { litKey = key; build(s.hour, s.minute) }
        val cell = min(r.width() / 11f, r.height() / 11.2f)
        val x0 = r.centerX() - 5.5f * cell
        val y0 = r.centerY() - 5.6f * cell
        tp.textSize = cell * 0.6f
        tp.textAlign = Paint.Align.CENTER
        // Draw the letters that are off first, then the lit letters with the glow.
        for (pass in 0..2) {
            when (pass) {
                0 -> { G.plain(tp); tp.color = pal.faint }
                1 -> G.ink(tp, pal, pal.fg, 0f, 1f, u * 0.6f)
                else -> G.ink(tp, pal, pal.accent, y0, y0 + 10 * cell, u * 0.6f)
            }
            for (row in 0 until 10) for (col in 0 until 11) {
                if (lit[row][col] != pass) continue
                G.text(c, grid[row][col].toString(), x0 + (col + 0.5f) * cell, y0 + (row + 0.5f) * cell, tp)
            }
        }
        G.plain(tp)
        // Four dots show the minutes after the last 5-minute step.
        val extra = s.minute % 5
        p.style = Paint.Style.FILL
        for (i in 0 until 4) {
            p.color = if (i < extra) pal.accent else pal.faint
            c.drawCircle(r.centerX() + (i - 1.5f) * cell * 0.7f, y0 + 10.7f * cell, cell * 0.09f, p)
        }
    }
}

/** Theme 9: a clean watch face with hands. */
class Analog : Theme {
    private val p = Paint(AA)
    private val tp = TextPaint(AA).apply { typeface = Typeface.create("sans-serif-light", Typeface.NORMAL) }

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val cx = r.centerX()
        val cy = r.centerY()
        val rad = min(r.width(), r.height()) / 2f - 2f * u
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 60) {
            val a = i * TAU / 60f
            val major = i % 5 == 0
            val inner = rad - (if (major) (if (i % 15 == 0) 5.5f else 3.6f) else 1.4f) * u
            p.strokeWidth = (if (major) 0.7f else 0.25f) * u
            p.color = if (major) pal.fg else pal.dim
            c.drawLine(cx + inner * sin(a), cy - inner * cos(a), cx + rad * sin(a), cy - rad * cos(a), p)
        }
        tp.textSize = rad * 0.13f
        tp.textAlign = Paint.Align.CENTER
        tp.color = pal.dim
        for (i in 0 until 4) {
            val a = i * TAU / 4f
            val d = rad - 10.5f * u
            G.text(c, arrayOf("12", "3", "6", "9")[i], cx + d * sin(a), cy - d * cos(a), tp)
        }
        val sec = s.second + if (s.animated) s.secFrac else 0f
        val am = (s.minute + sec / 60f) / 60f * TAU
        val ah = ((s.hour % 12) + s.minute / 60f) / 12f * TAU
        if (pal.glow) G.glow(p, pal.fg, u, 1.2f)
        p.color = pal.fg
        p.strokeWidth = 2.2f * u
        c.drawLine(cx, cy, cx + rad * 0.5f * sin(ah), cy - rad * 0.5f * cos(ah), p)
        p.strokeWidth = 1.4f * u
        c.drawLine(cx, cy, cx + rad * 0.8f * sin(am), cy - rad * 0.8f * cos(am), p)
        p.clearShadowLayer()
        if (cfg.showSeconds) {
            val a = sec / 60f * TAU
            if (pal.glow) G.glow(p, pal.accent, u, 1.4f)
            p.color = pal.accent
            p.strokeWidth = 0.5f * u
            c.drawLine(
                cx - rad * 0.16f * sin(a), cy + rad * 0.16f * cos(a),
                cx + rad * 0.9f * sin(a), cy - rad * 0.9f * cos(a), p,
            )
            p.clearShadowLayer()
        }
        p.style = Paint.Style.FILL
        p.color = pal.accent
        c.drawCircle(cx, cy, 1.5f * u, p)
        p.color = 0xFF000000.toInt()
        c.drawCircle(cx, cy, 0.55f * u, p)
    }
}

/** Theme 10: glass tubes that glow, as on a neon sign. */
class Neon : Theme {
    private val face = Typeface.create("sans-serif", Typeface.BOLD)
    private val tp = TextPaint(AA)
    private val p = Paint(AA)

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val wide = r.width() > r.height()
        val cx = r.centerX()
        val cy = r.centerY() - 3f * u
        tp.typeface = pal.face(face)
        tp.style = Paint.Style.STROKE
        tp.strokeWidth = 0.75f * u
        tp.strokeJoin = Paint.Join.ROUND
        // Sometimes the first tube flickers.
        val n = if (s.animated) sin(s.t * 13f) + sin(s.t * 7.3f) else 0f
        val a1 = if (n > 1.8f) 90 else 255
        val below: Float
        if (wide) {
            G.fit(tp, "88:88", r.width() * 0.88f, r.height() * 0.5f)
            tp.textAlign = Paint.Align.LEFT
            val wh = tp.measureText(s.hh)
            val wc = tp.measureText(":")
            var x = cx - (wh + wc + tp.measureText(s.mm)) / 2f
            tube(c, s.hh, x, cy, pal.accentA(a1), u)
            x += wh
            tube(c, ":", x, cy, pal.fgA(200), u)
            x += wc
            tube(c, s.mm, x, cy, pal.accent2, u)
            below = cy + G.capHeight(tp) / 2f
        } else {
            G.fit(tp, "88", r.width() * 0.74f, r.height() * 0.33f)
            tp.textAlign = Paint.Align.CENTER
            val cap = G.capHeight(tp) / 2f
            val off = cap + r.height() * 0.045f
            tube(c, s.hh, cx, cy - off, pal.accentA(a1), u)
            tube(c, s.mm, cx, cy + off, pal.accent2, u)
            below = cy + off + cap
        }
        tp.clearShadowLayer()
        tp.style = Paint.Style.FILL
        // A straight tube below the time. It fills in one minute.
        val half = r.width() * (if (wide) 0.36f else 0.3f)
        val y = below + 7f * u
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 0.75f * u
        p.color = pal.faint
        c.drawLine(cx - half, y, cx + half, y, p)
        val f = if (cfg.showSeconds) (s.second + if (s.animated) s.secFrac else 0f) / 60f else 1f
        G.glow(p, pal.fg, u, 2.2f)
        p.color = pal.fg
        c.drawLine(cx - half, y, cx - half + 2f * half * f, y, p)
        p.clearShadowLayer()
    }

    private fun tube(c: Canvas, text: String, x: Float, cy: Float, color: Int, u: Float) {
        tp.color = color
        G.glow(tp, color, u, 3.4f)
        G.text(c, text, x, cy, tp)
    }
}

/** Theme 11: very large digits that fill the area. */
class Mega : Theme {
    private val face = Typeface.create("sans-serif-black", Typeface.NORMAL)
    private val tp = TextPaint(AA)
    private val p = Paint(AA)

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val wide = r.width() > r.height()
        val cx = r.centerX()
        tp.typeface = pal.face(face)
        tp.letterSpacing = -0.03f
        val barY: Float
        if (wide) {
            G.fit(tp, "8888", r.width() * 0.97f, r.height() * 0.78f)
            val cy = r.centerY() - 2f * u
            val cap = G.capHeight(tp) / 2f
            val wh = tp.measureText(s.hh)
            val gap = 2f * u
            val x = cx - (wh + gap + tp.measureText(s.mm)) / 2f
            tp.textAlign = Paint.Align.LEFT
            tp.color = pal.fg
            G.text(c, s.hh, x, cy, tp)
            fill(c, s.mm, x + wh + gap, cy, cap, pal, u)
            barY = cy + cap + 4f * u
        } else {
            G.fit(tp, "88", r.width() * 0.97f, r.height() * 0.45f)
            val cap = G.capHeight(tp) / 2f
            val off = cap + r.height() * 0.015f
            val cy = r.centerY() - 2f * u
            tp.textAlign = Paint.Align.CENTER
            tp.color = pal.fg
            G.text(c, s.hh, cx, cy - off, tp)
            fill(c, s.mm, cx, cy + off, cap, pal, u)
            barY = cy + off + cap + 4f * u
        }
        tp.letterSpacing = 0f
        if (cfg.showSeconds) {
            val f = (s.second + if (s.animated) s.secFrac else 0f) / 60f
            p.style = Paint.Style.FILL
            p.color = pal.faint
            c.drawRect(r.left, barY, r.right, barY + 0.8f * u, p)
            p.color = pal.accent
            c.drawRect(r.left, barY, r.left + r.width() * f, barY + 0.8f * u, p)
        }
    }

    /** The minutes always have a colour gradient in this theme. */
    private fun fill(c: Canvas, text: String, x: Float, cy: Float, cap: Float, pal: Pal, u: Float) {
        tp.color = pal.accent
        tp.shader = G.gradient(cy - cap, cy + cap, pal.accent, pal.accent2)
        if (pal.glow) G.glow(tp, pal.accent, u, 2f)
        G.text(c, text, x, cy, tp)
        G.plain(tp)
    }
}

/** Theme 12: a radar screen. The line goes around one time each minute, or faster when it charges. */
class Radar : Theme {
    private val face = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    private val tp = TextPaint(AA)
    private val mono = TextPaint(AA).apply { typeface = Typeface.MONOSPACE }
    private val p = Paint(AA)
    private val oval = RectF()
    private val blipA = floatArrayOf(0.7f, 1.9f, 2.6f, 3.9f, 4.4f, 5.6f)
    private val blipD = floatArrayOf(0.55f, 0.8f, 0.35f, 0.7f, 0.45f, 0.88f)

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val wide = r.width() > r.height()
        val rad: Float
        val cx: Float
        val cy: Float
        val tx: Float
        val ty: Float
        val tw: Float
        val th: Float
        if (wide) {
            rad = min(r.height() * 0.46f, r.width() * 0.25f)
            cx = r.left + rad + 2f * u
            cy = r.centerY()
            tw = r.right - (cx + rad) - 6f * u
            tx = cx + rad + 6f * u + tw / 2f
            ty = cy
            th = r.height() * 0.4f
        } else {
            rad = min(r.width() * 0.44f, r.height() * 0.32f)
            cx = r.centerX()
            cy = r.top + rad + 2f * u
            tw = r.width() * 0.8f
            tx = cx
            th = (r.bottom - (cy + rad)) * 0.5f
            ty = cy + rad + (r.bottom - (cy + rad)) * 0.5f
        }
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.BUTT
        p.strokeWidth = 0.25f * u
        p.color = pal.accentA(120)
        for (k in 1..3) c.drawCircle(cx, cy, rad * k / 3f, p)
        p.color = pal.accentA(60)
        c.drawLine(cx - rad, cy, cx + rad, cy, p)
        c.drawLine(cx, cy - rad, cx, cy + rad, p)
        p.color = pal.accentA(200)
        for (i in 0 until 36) {
            val a = i * TAU / 36f
            val o = rad + (if (i % 3 == 0) 2f else 1f) * u
            c.drawLine(cx + rad * sin(a), cy - rad * cos(a), cx + o * sin(a), cy - o * cos(a), p)
        }
        // The angle of the line. 0 is up, and it turns clockwise.
        val a = if (s.animated) (s.t % 5f) / 5f * TAU else s.second / 60f * TAU
        val deg = Math.toDegrees(a.toDouble()).toFloat() - 90f
        oval.set(cx - rad, cy - rad, cx + rad, cy + rad)
        p.style = Paint.Style.FILL
        for (k in 0 until 12) {
            p.color = pal.accentA(66 - k * 5)
            c.drawArc(oval, deg - (k + 1) * 5f, 5.2f, true, p)
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.4f * u
        p.color = pal.accent
        c.drawLine(cx, cy, cx + rad * sin(a), cy - rad * cos(a), p)
        p.style = Paint.Style.FILL
        for (i in blipA.indices) {
            // A blip is bright immediately after the line goes across it.
            var d = (a - blipA[i]) % TAU
            if (d < 0f) d += TAU
            val b = 1f - d / TAU
            p.color = pal.fgA((b * b * b * 255).toInt())
            c.drawCircle(cx + rad * blipD[i] * sin(blipA[i]), cy - rad * blipD[i] * cos(blipA[i]), 0.8f * u, p)
        }
        p.color = pal.accent
        c.drawCircle(cx, cy, 0.7f * u, p)

        tp.typeface = pal.face(face)
        G.fit(tp, "88:88", tw, th)
        tp.textAlign = Paint.Align.CENTER
        val cap = G.capHeight(tp) / 2f
        G.ink(tp, pal, pal.fg, ty - cap, ty + cap, u)
        G.text(c, "${s.hh}:${s.mm}", tx, ty - 2f * u, tp)
        G.plain(tp)
        mono.textSize = 2.8f * u
        mono.letterSpacing = 0.14f
        mono.textAlign = Paint.Align.CENTER
        mono.color = pal.accentA(220)
        val label = if (cfg.showSeconds) "SCAN ${s.ss}  //  6 CONTACTS" else "SCAN  //  6 CONTACTS"
        c.drawText(label, tx, ty + cap + 4.5f * u, mono)
    }
}
