package gooner.demo.tranning_rxandroid.editor

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import gooner.demo.tranning_rxandroid.editor.model.SoundTrack
import java.io.IOException

/**
 * Plays the sound attached to the picture. Kept deliberately small: one clip at a time,
 * looping, with a callback so the UI can keep its play/pause button in sync.
 */
class SoundPlayer(context: Context) {

    private val appContext = context.applicationContext

    private var player: MediaPlayer? = null

    var track: SoundTrack? = null
        private set

    /** Called with `true` while the clip is playing and `false` when it stops. */
    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null

    val isPlaying: Boolean
        get() = player?.isPlaying == true

    /** Loads a new clip, replacing the previous one. Returns false when it cannot be read. */
    fun attach(uri: Uri, title: String): Boolean {
        detach()
        return try {
            val created = MediaPlayer()
            @Suppress("DEPRECATION")
            created.setAudioStreamType(AudioManager.STREAM_MUSIC)
            created.setDataSource(appContext, uri)
            created.isLooping = true
            created.prepare()
            created.setOnCompletionListener { onPlaybackStateChanged?.invoke(false) }
            player = created
            track = SoundTrack(uri, title)
            true
        } catch (error: IOException) {
            Log.w(TAG, "Unable to load the sound: " + error.message)
            detach()
            false
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Unable to load the sound: " + error.message)
            detach()
            false
        }
    }

    /** Starts or pauses the clip. Returns the new playing state. */
    fun togglePlayback(): Boolean {
        val current = player ?: return false
        if (current.isPlaying) {
            current.pause()
            onPlaybackStateChanged?.invoke(false)
            return false
        }
        current.start()
        onPlaybackStateChanged?.invoke(true)
        return true
    }

    fun pause() {
        val current = player ?: return
        if (current.isPlaying) {
            current.pause()
            onPlaybackStateChanged?.invoke(false)
        }
    }

    fun detach() {
        player?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        player = null
        track = null
        onPlaybackStateChanged?.invoke(false)
    }

    fun release() {
        onPlaybackStateChanged = null
        detach()
    }

    private companion object {
        const val TAG = "SoundPlayer"
    }
}
