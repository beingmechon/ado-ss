package dev.ado.standby

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val AA = Paint.ANTI_ALIAS_FLAG

/** Theme 1: a rugged head-up display. It agrees with the olive case. */
class Tactical : Theme {
    private val face = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    private val big = TextPaint(AA)
    private val mono = TextPaint(AA).apply { typeface = Typeface.MONOSPACE }
    private val p = Paint(AA)

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val wide = r.width() > r.height()
        big.typeface = pal.face(face)
        // Corner brackets.
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.5f * u
        p.strokeCap = Paint.Cap.SQUARE
        p.color = pal.accent
        val l = 6f * u
        for (sx in 0..1) for (sy in 0..1) {
            val x = if (sx == 0) r.left else r.right
            val y = if (sy == 0) r.top else r.bottom
            c.drawLine(x, y, x + (if (sx == 0) l else -l), y, p)
            c.drawLine(x, y, x, y + (if (sy == 0) l else -l), p)
        }
        // Ruler marks on the left and right edges.
        p.strokeWidth = 0.25f * u
        p.color = pal.accentA(90)
        var ty = r.top + 10f * u
        var i = 0
        while (ty < r.bottom - 10f * u) {
            val len = if (i % 4 == 0) 2.2f * u else 1.1f * u
            c.drawLine(r.left, ty, r.left + len, ty, p)
            c.drawLine(r.right, ty, r.right - len, ty, p)
            ty += 3f * u
            i++
        }
        // Labels.
        mono.textSize = 2.6f * u
        mono.letterSpacing = 0.12f
        mono.color = pal.accentA(210)
        mono.textAlign = Paint.Align.LEFT
        c.drawText("// STAND-BY", r.left + 3.5f * u, r.top + 5.4f * u, mono)
        mono.textAlign = Paint.Align.RIGHT
        c.drawText(if (s.charging) "PWR:EXT" else "PWR:BAT", r.right - 3.5f * u, r.top + 5.4f * u, mono)

        val cx = r.centerX()
        val barH = if (cfg.showSeconds) 9f * u else 2f * u
        val top = r.top + 8f * u
        val bottom = r.bottom - barH
        val cy = (top + bottom) / 2f
        val availH = bottom - top
        if (wide) {
            G.fit(big, "88:88", r.width() * 0.82f, availH * 0.74f)
            big.textAlign = Paint.Align.LEFT
            val wh = big.measureText(s.hh)
            val wc = big.measureText(":")
            val wm = big.measureText(s.mm)
            var x = cx - (wh + wc + wm) / 2f
            val cap = G.capHeight(big) / 2f
            G.ink(big, pal, pal.fg, cy - cap, cy + cap, u)
            G.text(c, s.hh, x, cy, big)
            x += wh
            G.ink(big, pal, if (s.animated) pal.accentA((150 + 105 * sin(s.t * PI.toFloat())).toInt()) else pal.accent, cy - cap, cy + cap, u)
            G.text(c, ":", x, cy, big)
            x += wc
            G.ink(big, pal, pal.fg, cy - cap, cy + cap, u)
            G.text(c, s.mm, x, cy, big)
        } else {
            G.fit(big, "88", r.width() * 0.74f, availH * 0.41f)
            big.textAlign = Paint.Align.CENTER
            val cap = G.capHeight(big) / 2f
            val off = cap + availH * 0.035f
            G.ink(big, pal, pal.fg, cy - off - cap, cy - off + cap, u)
            G.text(c, s.hh, cx, cy - off, big)
            G.ink(big, pal, pal.accent, cy + off - cap, cy + off + cap, u)
            G.text(c, s.mm, cx, cy + off, big)
        }
        G.plain(big)

        if (cfg.showSeconds) {
            // 60 small blocks. One block comes on each second.
            val x0 = r.left + 4f * u
            val step = (r.width() - 8f * u) / 60f
            val y = r.bottom - 4f * u
            p.style = Paint.Style.FILL
            for (k in 0 until 60) {
                p.color = when {
                    k < s.second -> pal.accentA(150)
                    k == s.second -> pal.fg
                    else -> pal.faint
                }
                val hgt = if (k % 5 == 0) 3f * u else 1.8f * u
                c.drawRect(x0 + k * step, y - hgt, x0 + k * step + step * 0.55f, y, p)
            }
        }
        if (s.animated) {
            // A faint scan line that moves down.
            val f = (s.t % 7f) / 7f
            val y = r.top + f * r.height()
            p.style = Paint.Style.FILL
            p.color = pal.accentA(50)
            c.drawRect(r.left + u, y, r.right - u, y + 0.3f * u, p)
        }
    }
}

/** Theme 2: three rings for hours, minutes and seconds. */
class Orbit : Theme {
    private val face = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val tp = TextPaint(AA)
    private val p = Paint(AA)
    private val oval = RectF()

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val cx = r.centerX()
        val cy = r.centerY()
        val rad = min(r.width(), r.height()) / 2f - 3f * u
        val sec = s.second + if (s.animated) s.secFrac else 0f
        val fs = sec / 60f
        val fm = (s.minute + sec / 60f) / 60f
        val fh = ((s.hour % 12) + s.minute / 60f) / 12f
        var ring = rad
        if (cfg.showSeconds) {
            arc(c, cx, cy, ring, fs, 0.9f * u, pal.accentA(150), pal, u)
            ring -= 5f * u
        }
        arc(c, cx, cy, ring, fm, 1.7f * u, pal.fg, pal, u)
        ring -= 5.5f * u
        arc(c, cx, cy, ring, fh, 2.5f * u, pal.accent, pal, u)

        if (s.animated) {
            // Two small moons that go around slowly.
            p.style = Paint.Style.FILL
            for (k in 0..1) {
                val a = s.t * (2f * PI.toFloat()) / (if (k == 0) 23f else -37f) + k * 2f
                val rr = rad + 2.6f * u
                p.color = pal.accentA(if (k == 0) 200 else 120)
                c.drawCircle(cx + rr * cos(a), cy + rr * sin(a), (if (k == 0) 0.6f else 0.45f) * u, p)
            }
        }
        val inner = (ring - 3f * u) * 2f
        tp.typeface = pal.face(face)
        G.fit(tp, "88:88", inner * 0.8f, inner * 0.3f)
        tp.textAlign = Paint.Align.CENTER
        G.ink(tp, pal, pal.fg, 0f, 1f, u)
        G.text(c, "${s.hh}:${s.mm}", cx, cy, tp)
        G.plain(tp)
    }

    private fun arc(c: Canvas, cx: Float, cy: Float, rad: Float, f: Float, w: Float, color: Int, pal: Pal, u: Float) {
        oval.set(cx - rad, cy - rad, cx + rad, cy + rad)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = w
        p.color = pal.faint
        c.drawCircle(cx, cy, rad, p)
        p.color = color
        c.drawArc(oval, -90f, 360f * f, false, p)
        val a = (f * 2f * PI.toFloat()) - PI.toFloat() / 2f
        p.style = Paint.Style.FILL
        p.color = pal.fg
        c.drawCircle(cx + rad * cos(a), cy + rad * sin(a), w * 0.85f + 0.2f * u, p)
    }
}

/** Theme 3: cards that turn over, like an old desk clock. */
class Flip : Theme {
    private val tp = TextPaint(AA).apply { typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD) }
    private val p = Paint(AA)

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val wide = r.width() > r.height()
        val now = s.hh + s.mm
        // The time one minute before. It shows which cards must turn.
        var pm = s.minute - 1
        var ph = s.hour
        if (pm < 0) { pm = 59; ph = (ph + 23) % 24 }
        val old = Scene.two(s.hourShown(ph)) + Scene.two(pm)
        val turn = if (s.animated && s.second == 0 && s.secFrac < 0.6f) s.secFrac / 0.6f else -1f

        val cw: Float
        val ch: Float
        val cx = r.centerX()
        val cy = r.centerY() - if (cfg.showSeconds) 2f * u else 0f
        if (wide) {
            ch = min(r.height() * 0.66f, (r.width() / 4.9f) / 0.66f)
            cw = ch * 0.66f
            val gap = cw * 0.08f
            val mid = cw * 0.42f
            val total = 4f * cw + 2f * gap + mid
            var x = cx - total / 2f
            for (i in 0 until 4) {
                card(c, x, cy - ch / 2f, cw, ch, now[i], old[i], turn, pal, u)
                x += cw + if (i == 1) mid else gap
            }
            p.style = Paint.Style.FILL
            p.color = pal.accent
            c.drawCircle(cx, cy - ch * 0.17f, cw * 0.06f, p)
            c.drawCircle(cx, cy + ch * 0.17f, cw * 0.06f, p)
        } else {
            cw = min(r.width() * 0.41f, r.height() * 0.43f * 0.7f)
            ch = cw / 0.7f
            val gap = cw * 0.08f
            for (i in 0 until 4) {
                val x = cx + if (i % 2 == 0) -cw - gap / 2f else gap / 2f
                val y = cy + if (i < 2) -ch - gap else gap
                card(c, x, y, cw, ch, now[i], old[i], turn, pal, u)
            }
        }
        if (cfg.showSeconds) {
            // A thin line that fills in one minute.
            val half = if (wide) 2f * cw + cw * 0.29f else cw + cw * 0.04f
            val y = cy + (if (wide) ch / 2f else ch + cw * 0.08f) + 4f * u
            val f = (s.second + if (s.animated) s.secFrac else 0f) / 60f
            p.style = Paint.Style.FILL
            p.color = pal.faint
            c.drawRect(cx - half, y, cx + half, y + 0.5f * u, p)
            p.color = pal.accent
            c.drawRect(cx - half, y, cx - half + 2f * half * f, y + 0.5f * u, p)
        }
    }

    private fun card(
        c: Canvas, x: Float, y: Float, w: Float, h: Float,
        now: Char, old: Char, turn: Float, pal: Pal, u: Float,
    ) {
        val turning = turn >= 0f && now != old
        val digit = if (turning && turn < 0.5f) old else now
        val cy = y + h / 2f
        c.save()
        if (turning) c.scale(1f, 0.04f + 0.96f * abs(cos(turn * PI.toFloat())), x + w / 2f, cy)
        p.style = Paint.Style.FILL
        p.color = 0xFF181818.toInt()
        c.drawRoundRect(x, y, x + w, y + h, w * 0.1f, w * 0.1f, p)
        G.fit(tp, "8", w * 0.62f, h * 0.62f)
        tp.textAlign = Paint.Align.CENTER
        tp.color = pal.fg
        G.text(c, digit.toString(), x + w / 2f, cy, tp)
        p.color = 0xFF000000.toInt()
        c.drawRect(x, cy - 0.25f * u, x + w, cy + 0.25f * u, p)
        c.restore()
    }
}

/** Theme 4: thin digits and nothing more. It uses the least power. */
class Minimal : Theme {
    private val face = Typeface.create("sans-serif-thin", Typeface.NORMAL)
    private val big = TextPaint(AA)
    private val small = TextPaint(AA).apply { typeface = Typeface.create("sans-serif-light", Typeface.NORMAL) }

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val wide = r.width() > r.height()
        big.typeface = pal.face(face)
        val cx = r.centerX()
        val cy = r.centerY()
        val accent = pal.accent
        val below: Float
        if (wide) {
            G.fit(big, "88:88", r.width() * 0.9f, r.height() * 0.6f)
            big.textAlign = Paint.Align.LEFT
            val wh = big.measureText(s.hh)
            val wc = big.measureText(":")
            var x = cx - (wh + wc + big.measureText(s.mm)) / 2f
            val cap = G.capHeight(big) / 2f
            G.ink(big, pal, pal.fg, cy - cap, cy + cap, u)
            G.text(c, s.hh, x, cy, big)
            x += wh
            G.plain(big)
            big.color = pal.dim
            G.text(c, ":", x, cy, big)
            x += wc
            G.ink(big, pal, accent, cy - cap, cy + cap, u)
            G.text(c, s.mm, x, cy, big)
            below = cy + cap
        } else {
            G.fit(big, "88", r.width() * 0.8f, r.height() * 0.36f)
            big.textAlign = Paint.Align.CENTER
            val cap = G.capHeight(big) / 2f
            val off = cap + r.height() * 0.04f
            G.ink(big, pal, pal.fg, cy - off - cap, cy - off + cap, u)
            G.text(c, s.hh, cx, cy - off, big)
            G.ink(big, pal, accent, cy + off - cap, cy + off + cap, u)
            G.text(c, s.mm, cx, cy + off, big)
            below = cy + off + cap
        }
        G.plain(big)
        if (cfg.showSeconds) {
            small.textSize = 4.2f * u
            small.letterSpacing = 0.3f
            small.textAlign = Paint.Align.CENTER
            small.color = pal.dim
            G.text(c, s.ss, cx + small.textSize * 0.15f, below + 6.5f * u, small)
        }
    }
}

/** Theme 5: the time above a planet, with stars. */
class Space : Theme {
    private val face = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val tp = TextPaint(AA)
    private val p = Paint(AA)
    private val stars = StarField(70, 7)

    override fun draw(c: Canvas, r: RectF, s: Scene, pal: Pal, u: Float, cfg: Config) {
        val wide = r.width() > r.height()
        val cx = r.centerX()
        tp.typeface = pal.face(face)
        c.save()
        c.clipRect(r)
        stars.draw(c, r.left, r.top, r.width(), r.height(), s, pal, u)
        if (s.animated) {
            // One shooting star each 15 seconds.
            val k = (s.t / 15f).toInt()
            val f = (s.t % 15f) / 1.1f
            if (f < 1f) {
                val rnd = Random(k)
                val x0 = r.left + (0.15f + rnd.nextFloat() * 0.6f) * r.width()
                val y0 = r.top + rnd.nextFloat() * 0.35f * r.height()
                val len = 16f * u
                val hx = x0 + f * 34f * u
                val hy = y0 + f * 15f * u
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                for (j in 0 until 6) {
                    p.strokeWidth = (0.45f - j * 0.06f) * u
                    p.color = pal.fgA(((1f - f) * (200 - j * 32)).toInt())
                    val a0 = j / 6f
                    val a1 = (j + 1) / 6f
                    c.drawLine(hx - len * 0.9f * a0, hy - len * 0.4f * a0, hx - len * 0.9f * a1, hy - len * 0.4f * a1, p)
                }
                p.style = Paint.Style.FILL
            }
        }
        // The edge of a planet at the bottom.
        val pr = r.width() * (if (wide) 0.9f else 1.25f)
        val py = r.bottom + pr - (if (wide) 9f else 14f) * u
        p.style = Paint.Style.FILL
        p.color = 0xFF000000.toInt()
        c.drawCircle(cx, py, pr, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.45f * u
        p.color = pal.accentA(230)
        c.drawCircle(cx, py, pr, p)
        p.strokeWidth = 1.6f * u
        p.color = pal.accentA(40)
        c.drawCircle(cx, py, pr - 1.2f * u, p)
        c.restore()

        val cy = r.top + (r.height() - (if (wide) 9f else 14f) * u) * 0.47f
        tp.textAlign = Paint.Align.CENTER
        if (wide) {
            G.fit(tp, "88:88", r.width() * 0.8f, r.height() * 0.46f)
            G.ink(tp, pal, pal.fg, 0f, 1f, u)
            G.text(c, "${s.hh}:${s.mm}", cx, cy, tp)
        } else {
            G.fit(tp, "88", r.width() * 0.7f, r.height() * 0.3f)
            val cap = G.capHeight(tp) / 2f
            val off = cap + r.height() * 0.035f
            G.ink(tp, pal, pal.fg, 0f, 1f, u)
            G.text(c, s.hh, cx, cy - off, tp)
            G.ink(tp, pal, pal.accent, cy + off - cap, cy + off + cap, u)
            G.text(c, s.mm, cx, cy + off, tp)
        }
        G.plain(tp)
        if (cfg.showSeconds) {
            // A small dot that moves along the planet edge one time each minute.
            val f = (s.second + if (s.animated) s.secFrac else 0f) / 60f
            val span = Math.asin((r.width() * 0.5f / pr).toDouble().coerceAtMost(1.0)).toFloat()
            val a = -PI.toFloat() / 2f - span + 2f * span * f
            p.style = Paint.Style.FILL
            p.color = pal.fg
            c.drawCircle(cx + pr * cos(a), py + pr * sin(a), 0.8f * u, p)
        }
    }
}
