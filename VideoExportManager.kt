@file:OptIn(UnstableApi::class)

package com.viral.gamingenhancer.media

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.viral.gamingenhancer.data.EditorSettings
import com.viral.gamingenhancer.effects.EffectChain
import java.io.File

/** Must be created and used on the main thread. */
class VideoExportManager(private val context: Context) {

    interface Callback {
        fun onProgress(percent: Int)
        fun onDone()
        fun onError(message: String)
    }

    private var transformer: Transformer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val holder = ProgressHolder()

    private val poll = object : Runnable {
        override fun run() {
            val t = transformer ?: return
            if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) cb?.onProgress(holder.progress)
            handler.postDelayed(this, 300)
        }
    }
    private var cb: Callback? = null

    fun start(source: Uri, s: EditorSettings, info: VideoInfo, out: File, callback: Callback) {
        cb = callback
        val clip = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(s.trimStartMs)
            .apply { if (s.trimEndMs in 1 until info.durationMs) setEndPositionMs(s.trimEndMs) }
            .build()
        val item = MediaItem.Builder().setUri(source).setClippingConfiguration(clip).build()

        val effects: List<Effect> = EffectChain.exportEffects(s, info.fps)
        val edited = EditedMediaItem.Builder(item)
            .setEffects(Effects(emptyList<AudioProcessor>(), effects))
            .build()

        val encoder = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder().setBitrate(s.bitrateMbps * 1_000_000).build()
            ).build()

        val t = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setEncoderFactory(encoder)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    stopPolling(); callback.onProgress(100); callback.onDone()
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    stopPolling()
                    callback.onError("Export failed: ${exportException.errorCodeName}. " +
                        "Try a lower resolution/bitrate or a shorter clip.")
                }
            })
            .build()
        transformer = t
        t.start(edited, out.absolutePath)
        handler.post(poll)
    }

    fun cancel() { transformer?.cancel(); stopPolling() }

    private fun stopPolling() { handler.removeCallbacks(poll); transformer = null }
}
