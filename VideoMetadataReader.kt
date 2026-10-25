package com.viral.gamingenhancer.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns

data class VideoInfo(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fps: Float,        // 0f when the file does not say
    val sizeBytes: Long,
    val mime: String
)

object VideoMetadataReader {
    const val MAX_IMPORT_MB = 4096L   // change this to limit import size

    fun read(ctx: Context, uri: Uri): VideoInfo {
        var name = "video"
        var size = 0L
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val s = c.getColumnIndex(OpenableColumns.SIZE)
                if (n >= 0) name = c.getString(n) ?: name
                if (s >= 0) size = c.getLong(s)
            }
        }
        if (size > MAX_IMPORT_MB * 1024 * 1024) {
            throw IllegalArgumentException("File is larger than the ${MAX_IMPORT_MB} MB import limit.")
        }

        val ex = MediaExtractor()
        try {
            try {
                ex.setDataSource(ctx, uri, null)
            } catch (e: Exception) {
                throw IllegalArgumentException("This phone cannot open that file. Try an MP4 (H.264/H.265).")
            }
            var found: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) { found = f; break }
            }
            val f = found ?: throw IllegalArgumentException("No video track found in this file.")

            var w = f.getInteger(MediaFormat.KEY_WIDTH)
            var h = f.getInteger(MediaFormat.KEY_HEIGHT)
            val rot = if (f.containsKey(MediaFormat.KEY_ROTATION)) f.getInteger(MediaFormat.KEY_ROTATION) else 0
            if (rot == 90 || rot == 270) { val t = w; w = h; h = t }

            val durMs = if (f.containsKey(MediaFormat.KEY_DURATION)) f.getLong(MediaFormat.KEY_DURATION) / 1000 else 0L
            if (durMs <= 0) throw IllegalArgumentException("Could not read the video duration.")

            val fps = if (f.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                try { f.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
                catch (e: ClassCastException) { f.getFloat(MediaFormat.KEY_FRAME_RATE) }
            } else 0f

            return VideoInfo(uri, name, durMs, w, h, fps, size, f.getString(MediaFormat.KEY_MIME) ?: "video/*")
        } finally {
            ex.release()
        }
    }
}
