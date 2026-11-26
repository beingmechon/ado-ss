package dev.ado.standby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * The stand display. It draws the clock, reads the tilt sensor and the battery,
 * and turns the picture itself. Thus it does not use the auto-rotate setting of the phone.
 */
class StandView(ctx: Context, private val interactive: Boolean, private val preview: Boolean = false) :
    View(ctx), SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {

    var onOpenSettings: (() -> Unit)? = null
    /** Called when the power source or the settings change. */
    var onStateChanged: (() -> Unit)? = null

    var cfg = Prefs.load(ctx)
        private set
    var charging = false
        private set

    private val renderer = Renderer()
    private val scene = Scene()
    private val handler = Handler(Looper.getMainLooper())
    private val sensors = ctx.getSystemService(SensorManager::class.java)
    private val media = MediaWatcher(ctx) { invalidate() }
    private var cal = Calendar.getInstance()
    private var dateDay = -1
    private var dateFormat = -1
    private var running = false
    private var sensorOn = false
    private var startAt = 0L

    /** 0 = vertical, 1 = right edge up, 2 = upside down, 3 = left edge up. */
    private var quad = 0
    private var pendingQuad = 0
    private var pendingCount = 0

    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            schedule()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                Intent.ACTION_BATTERY_CHANGED -> readBattery(i)
                else -> cal = Calendar.getInstance()
            }
            invalidate()
        }
    }

    private val gestures = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            // Change the screen speed to the speed in the turned picture.
            val v = when (quad) { 1 -> vy; 2 -> -vx; 3 -> -vy; else -> vx }
            val other = when (quad) { 1 -> -vx; 2 -> -vy; 3 -> vx; else -> vy }
            if (abs(v) < 800f && abs(other) < 800f) return false
            if (abs(v) >= abs(other)) {
                // Left or right: the next or the previous theme.
                val n = Prefs.THEMES.size
                val next = (cfg.theme + (if (v < 0) 1 else n - 1)) % n
                Prefs.sp(context).edit().putInt(Prefs.THEME, next).putInt(Prefs.AUTO_THEME, 0).apply()
            } else {
                // Up or down: the next or the previous colour. The custom colour is not in this cycle.
                val n = Prefs.ACCENTS.size - 1
                val cur = cfg.accent.coerceAtMost(n - 1)
                val next = (cur + (if (other < 0) 1 else n - 1)) % n
                Prefs.sp(context).edit().putInt(Prefs.ACCENT, next).apply()
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onOpenSettings?.invoke()
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            media.toggle()
            return true
        }
    })

    fun start() {
        if (running) return
        running = true
        startAt = SystemClock.elapsedRealtime()
        cfg = Prefs.load(context)
        cal = Calendar.getInstance()
        val f = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        val sticky = if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, f)
        }
        sticky?.let { readBattery(it) }
        Prefs.sp(context).registerOnSharedPreferenceChangeListener(this)
        Hub.onChange = { invalidate() }
        media.start()
        applyOrientation()
        invalidate()
        schedule()
        // Remove the help text after some seconds.
        handler.postDelayed({ invalidate() }, HINT_MS + 50)
    }

    fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacksAndMessages(null)
        try { context.unregisterReceiver(receiver) } catch (e: Exception) { }
        Prefs.sp(context).unregisterOnSharedPreferenceChangeListener(this)
        Hub.onChange = null
        media.stop()
        setSensor(false)
    }

    private fun animated() = cfg.motion == 1 || (cfg.motion == 0 && charging)

    private fun schedule() {
        handler.removeCallbacks(tick)
        if (!running) return
        val now = System.currentTimeMillis()
        val delay = when {
            animated() -> FRAME_MS
            cfg.showSeconds -> 1000L - now % 1000L + 5L
            else -> 60000L - now % 60000L + 5L
        }
        handler.postDelayed(tick, delay)
    }

    private fun readBattery(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0 && scale > 0) scene.battery = level * 100 / scale
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        if (plugged != charging) {
            charging = plugged
            schedule()
            onStateChanged?.invoke()
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        cfg = Prefs.load(context)
        applyOrientation()
        invalidate()
        schedule()
        onStateChanged?.invoke()
    }

    private fun applyOrientation() {
        if (preview) { quad = 0; return }
        when (cfg.orientation) {
            1 -> { quad = 0; setSensor(false) }
            2 -> { quad = 1; setSensor(false) }
            3 -> { quad = 3; setSensor(false) }
            else -> setSensor(true)
        }
    }

    private fun setSensor(on: Boolean) {
        if (on == sensorOn) return
        sensorOn = on
        if (on) {
            val accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
            // Two samples each second are sufficient. This uses very little power.
            sensors.registerListener(this, accel, 500_000, 1_000_000)
        } else {
            sensors.unregisterListener(this)
        }
    }

    override fun onSensorChanged(e: SensorEvent) {
        val x = e.values[0]
        val y = e.values[1]
        // The axis that points up shows a value near +9.8.
        val raw = when {
            abs(y) > 5.5f && abs(y) > abs(x) + 2f -> if (y > 0) 0 else 2
            abs(x) > 5.5f && abs(x) > abs(y) + 2f -> if (x > 0) 1 else 3
            else -> return // The device is flat or between two positions. Keep the picture.
        }
        // The sensor uses the natural position of the device. On some tablets and foldable
        // devices the system already turned the screen, so subtract that turn.
        val q = (raw - (display?.rotation ?: 0) + 4) % 4
        if (q == pendingQuad) pendingCount++ else { pendingQuad = q; pendingCount = 1 }
        if (pendingCount >= 2 && q != quad) {
            quad = q
            invalidate()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive) return super.onTouchEvent(event)
        return gestures.onTouchEvent(event)
    }

    private val hint = if (Hub.available(ctx)) HINT else HINT_LITE

    private fun updateScene() {
        val now = System.currentTimeMillis()
        val s = scene
        cal.timeInMillis = now
        s.hour = cal.get(Calendar.HOUR_OF_DAY)
        s.minute = cal.get(Calendar.MINUTE)
        s.second = cal.get(Calendar.SECOND)
        s.secFrac = (now % 1000L) / 1000f
        s.epochMinute = now / 60000L
        s.is24 = when (cfg.hourMode) {
            1 -> false
            2 -> true
            else -> DateFormat.is24HourFormat(context)
        }
        val day = cal.get(Calendar.DAY_OF_YEAR)
        if (day != dateDay || cfg.dateFormat != dateFormat) {
            dateDay = day
            dateFormat = cfg.dateFormat
            s.dateText = SimpleDateFormat(Prefs.DATE_PATTERNS[dateFormat], Locale.getDefault()).format(Date(now)).uppercase()
        }
        s.charging = charging
        s.musicTitle = media.title
        s.musicArtist = media.artist
        s.musicPlaying = media.playing
        s.icons = Hub.icons
        s.animated = animated()
        val up = SystemClock.elapsedRealtime() - startAt
        s.t = up / 1000f
        s.hint = if (interactive && up < HINT_MS) hint else null
    }

    override fun onDraw(canvas: Canvas) {
        updateScene()
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.save()
        when (quad) {
            1 -> { canvas.translate(w, 0f); canvas.rotate(90f); renderer.draw(canvas, h, w, scene, cfg) }
            3 -> { canvas.translate(0f, h); canvas.rotate(-90f); renderer.draw(canvas, h, w, scene, cfg) }
            2 -> { canvas.rotate(180f, w / 2f, h / 2f); renderer.draw(canvas, w, h, scene, cfg) }
            else -> renderer.draw(canvas, w, h, scene, cfg)
        }
        canvas.restore()
    }

    companion object {
        private const val FRAME_MS = 42L
        private const val HINT_MS = 7000L
        private const val HINT_LITE = "Swipe left/right: theme  \u00B7  up/down: colour  \u00B7  Hold: settings"
        private const val HINT = "Swipe left/right: theme  \u00B7  up/down: colour  \u00B7  Hold: settings  \u00B7  Double-tap: music"
    }
}
