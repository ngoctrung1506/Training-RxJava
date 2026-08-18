package gooner.demo.tranning_rxandroid.editor

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.IOException

/**
 * Plays the sound attached to the picture. Deliberately small: one clip at a time,
 * looping, owned by the view model which mirrors its state into the UI state.
 */
class SoundPlayer(context: Context) {

    private val appContext = context.applicationContext

    private var player: MediaPlayer? = null

    /** Called when playback stops on its own. */
    var onPlaybackFinished: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = player?.isPlaying == true

    /** Loads a new clip, replacing the previous one. Returns false when it cannot be read. */
    fun load(uri: Uri): Boolean {
        release()
        return try {
            val created = MediaPlayer()
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            created.setDataSource(appContext, uri)
            created.isLooping = true
            created.prepare()
            created.setOnCompletionListener { onPlaybackFinished?.invoke() }
            player = created
            true
        } catch (error: IOException) {
            Log.w(TAG, "Unable to load the sound: " + error.message)
            release()
            false
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Unable to load the sound: " + error.message)
            release()
            false
        }
    }

    /** Starts or pauses the clip. Returns the new playing state. */
    fun togglePlayback(): Boolean {
        val current = player ?: return false
        if (current.isPlaying) {
            current.pause()
            return false
        }
        current.start()
        return true
    }

    fun pause() {
        val current = player ?: return
        if (current.isPlaying) {
            current.pause()
        }
    }

    fun release() {
        player?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        player = null
    }

    private companion object {
        const val TAG = "SoundPlayer"
    }
}
