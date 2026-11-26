package io.github.beingmechon.stand

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Stars that twinkle. The Space theme and the Stars background use it. */
class StarField(private val n: Int, seed: Int) {
    private val sx = FloatArray(n)
    private val sy = FloatArray(n)
    private val sz = FloatArray(n)
    private val ph = FloatArray(n)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        val rnd = Random(seed)
        for (i in 0 until n) {
            sx[i] = rnd.nextFloat()
            sy[i] = rnd.nextFloat()
            sz[i] = 0.3f + rnd.nextFloat() * rnd.nextFloat() * 0.9f
            ph[i] = rnd.nextFloat() * 6.28f
        }
    }

    fun draw(c: Canvas, l: Float, t: Float, w: Float, h: Float, s: Scene, pal: Pal, u: Float) {
        p.style = Paint.Style.FILL
        for (i in 0 until n) {
            var x = sx[i]
            var a = 0.35f + 0.5f * abs(sin(ph[i]))
            if (s.animated) {
                x = (x + s.t * 0.0015f * sz[i]) % 1f
                a = 0.25f + 0.75f * abs(sin(s.t * (0.3f + sz[i] * 0.5f) + ph[i]))
            }
            p.color = if (i % 9 == 0) pal.accentA((a * 255).toInt()) else pal.fgA((a * 215).toInt())
            c.drawCircle(l + x * w, t + sy[i] * h, sz[i] * 0.42f * u, p)
        }
    }
}

/** An optional background behind the clock. All of them light few pixels. */
class Backdrop {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stars = StarField(90, 21)
    private val fx = FloatArray(16)
    private val fy = FloatArray(16)
    private val fp = FloatArray(16)

    init {
        val rnd = Random(3)
        for (i in 0 until 16) {
            fx[i] = rnd.nextFloat(); fy[i] = rnd.nextFloat(); fp[i] = rnd.nextFloat() * 6.28f
        }
    }

    fun draw(c: Canvas, w: Float, h: Float, s: Scene, pal: Pal, u: Float, kind: Int) {
        val t = if (s.animated) s.t else 0f
        when (kind) {
            1 -> stars.draw(c, 0f, 0f, w, h, s, pal, u)
            2 -> fireflies(c, w, h, t, pal, u)
            3 -> grid(c, w, h, t, pal, u)
            4 -> waves(c, w, h, t, pal, u)
            5 -> pulse(c, w, h, t, pal, u)
        }
    }

    private fun fireflies(c: Canvas, w: Float, h: Float, t: Float, pal: Pal, u: Float) {
        p.style = Paint.Style.FILL
        for (i in 0 until 16) {
            val x = (fx[i] + 0.06f * sin(t * (0.11f + i * 0.013f) + fp[i])) * w
            val y = (fy[i] + 0.05f * sin(t * (0.09f + i * 0.017f) + fp[i] * 2f)) * h
            val a = 0.35f + 0.65f * abs(sin(t * (0.5f + i * 0.07f) + fp[i]))
            val color = if (i % 3 == 0) pal.accent2 else pal.accent
            p.color = (color and 0x00FFFFFF) or ((a * 22).toInt() shl 24)
            c.drawCircle(x, y, 2.2f * u, p)
            p.color = (color and 0x00FFFFFF) or ((a * 60).toInt() shl 24)
            c.drawCircle(x, y, 1.1f * u, p)
            p.color = (color and 0x00FFFFFF) or ((a * 230).toInt() shl 24)
            c.drawCircle(x, y, 0.4f * u, p)
        }
    }

    /** A floor of lines that go to the horizon, as in 1980s art. */
    private fun grid(c: Canvas, w: Float, h: Float, t: Float, pal: Pal, u: Float) {
        val y0 = h * (if (w > h) 0.60f else 0.70f)
        val cx = w / 2f
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.BUTT
        p.strokeWidth = 0.22f * u
        p.color = pal.accent2A(70)
        val spread = max(w, h) * 0.16f
        for (i in -12..12) {
            c.drawLine(cx + i * spread * 0.04f, y0, cx + i * spread, h, p)
        }
        val n = 9
        val phase = (t * 0.22f) % 1f
        for (k in 0 until n) {
            val f = (k + phase) / n
            val y = y0 + (h - y0) * f * f
            p.color = pal.accent2A((20 + 80 * f).toInt())
            c.drawLine(0f, y, w, y, p)
        }
        p.strokeWidth = 0.35f * u
        p.color = pal.accentA(170)
        c.drawLine(0f, y0, w, y0, p)
    }

    private fun waves(c: Canvas, w: Float, h: Float, t: Float, pal: Pal, u: Float) {
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        val steps = 56
        for (k in 0 until 3) {
            p.strokeWidth = (0.42f - k * 0.08f) * u
            p.color = if (k == 1) pal.accent2A(95) else pal.accentA(120 - k * 30)
            val base = h * (0.90f + k * 0.022f)
            val amp = (2.6f - k * 0.5f) * u
            var px = 0f
            var py = 0f
            for (i in 0..steps) {
                val x = w * i / steps
                val a = i * (0.24f + k * 0.07f) + t * (0.9f - k * 0.25f) + k * 1.7f
                val y = base + amp * sin(a) + amp * 0.5f * sin(a * 0.43f + 1f)
                if (i > 0) c.drawLine(px, py, x, y, p)
                px = x; py = y
            }
        }
    }

    private fun pulse(c: Canvas, w: Float, h: Float, t: Float, pal: Pal, u: Float) {
        p.style = Paint.Style.STROKE
        val maxR = max(w, h) * 0.6f
        val cy = if (w > h) h / 2f else h * 0.39f
        val cx = if (w > h) w * 0.34f else w / 2f
        for (k in 0 until 4) {
            val f = (t / 9f + k / 4f + 0.1f) % 1f
            p.strokeWidth = 0.3f * u
            p.color = pal.accentA(((1f - f) * 85).toInt())
            c.drawCircle(cx, cy, f * maxR, p)
        }
    }
}
