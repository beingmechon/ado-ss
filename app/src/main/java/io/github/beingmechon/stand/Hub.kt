package io.github.beingmechon.stand

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Shared data from the notification listener. All access is on the main thread. */
object Hub {
    var icons: List<Bitmap> = emptyList()
    var onChange: (() -> Unit)? = null

    /** False in the "lite" version, which has no notification listener. */
    fun available(ctx: Context): Boolean = try {
        ctx.packageManager.getServiceInfo(ComponentName(ctx, NotifService::class.java), 0)
        true
    } catch (e: Exception) {
        false
    }

    fun hasAccess(ctx: Context): Boolean {
        val list = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return list.contains(ctx.packageName + "/")
    }
}

/** Reads the notification icons. The user must give "notification access" in the phone settings. */
class NotifService : NotificationListenerService() {
    private val cache = HashMap<String, Bitmap>()

    override fun onListenerConnected() = refresh()

    override fun onListenerDisconnected() {
        Hub.icons = emptyList()
        Hub.onChange?.invoke()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val list = try { activeNotifications } catch (e: Exception) { null } ?: return
        val ranking = try { currentRanking } catch (e: Exception) { null }
        val r = Ranking()
        val out = LinkedHashMap<String, Bitmap>()
        for (sbn in list.sortedByDescending { it.postTime }) {
            val pkg = sbn.packageName
            if (sbn.isOngoing || pkg == packageName || out.containsKey(pkg)) continue
            // Ignore silent notifications.
            if (ranking != null && ranking.getRanking(sbn.key, r) && r.importance < NotificationManager.IMPORTANCE_DEFAULT) continue
            val bmp = cache[pkg] ?: render(sbn.notification)?.also {
                if (cache.size > 40) cache.clear()
                cache[pkg] = it
            } ?: continue
            out[pkg] = bmp
            if (out.size >= 7) break
        }
        Hub.icons = out.values.toList()
        Hub.onChange?.invoke()
    }

    private fun render(n: Notification): Bitmap? = try {
        val d = n.smallIcon?.loadDrawable(this)
        if (d == null) null else {
            val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, 96, 96)
            d.draw(Canvas(bmp))
            bmp
        }
    } catch (e: Exception) {
        null
    }
}

/** Reads the song that plays now. It uses the same permission as the notification listener. */
class MediaWatcher(ctx: Context, private val onChange: () -> Unit) {
    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val comp = ComponentName(ctx, NotifService::class.java)
    private var controller: MediaController? = null
    private var listening = false

    var title: String? = null
        private set
    var artist: String? = null
        private set
    var playing = false
        private set

    private val sessions = MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    private val cb = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = read()
        override fun onMetadataChanged(metadata: MediaMetadata?) = read()
        override fun onSessionDestroyed() = refresh()
    }

    fun start() {
        if (listening) return
        try {
            msm.addOnActiveSessionsChangedListener(sessions, comp)
            listening = true
        } catch (e: SecurityException) {
            // The user did not give notification access.
        }
        refresh()
    }

    fun stop() {
        if (listening) {
            try { msm.removeOnActiveSessionsChangedListener(sessions) } catch (e: Exception) { }
            listening = false
        }
        controller?.unregisterCallback(cb)
        controller = null
    }

    fun toggle() {
        val c = controller ?: return
        try {
            if (playing) c.transportControls.pause() else c.transportControls.play()
        } catch (e: Exception) { }
    }

    private fun refresh() {
        val list = try { msm.getActiveSessions(comp) } catch (e: SecurityException) { null }
        pick(list)
    }

    private fun pick(list: List<MediaController>?) {
        val next = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(cb)
            controller = next
            next?.registerCallback(cb)
        }
        read()
    }

    private fun read() {
        val c = controller
        val md = c?.metadata
        title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
        artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        playing = c?.playbackState?.state == PlaybackState.STATE_PLAYING
        onChange()
    }
}
