@file:OptIn(UnstableApi::class)

package com.viral.gamingenhancer.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.viral.gamingenhancer.data.EditorSettings
import com.viral.gamingenhancer.data.Preset
import com.viral.gamingenhancer.media.VideoExportManager
import com.viral.gamingenhancer.media.VideoInfo
import com.viral.gamingenhancer.media.VideoMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { HOME, EDITOR, EXPORT }

sealed interface ExportUi {
    data object Idle : ExportUi
    data class Running(val percent: Int) : ExportUi
    data class Done(val uri: Uri) : ExportUi
    data class Failed(val message: String) : ExportUi
}

class EditorViewModel(private val app: Application) : AndroidViewModel(app) {

    var screen by mutableStateOf(Screen.HOME)
    var info by mutableStateOf<VideoInfo?>(null)
    var settings by mutableStateOf(EditorSettings())
    var presetName by mutableStateOf<String?>(null)
    var pendingPreset: Preset? = null
    var error by mutableStateOf<String?>(null)
    var export by mutableStateOf<ExportUi>(ExportUi.Idle)
    var canUndo by mutableStateOf(false)

    private val undoStack = ArrayDeque<EditorSettings>()
    private var dragStart: EditorSettings? = null
    private val manager = VideoExportManager(app)
    private var tmpFile: File? = null

    init { // clean leftovers from a crashed export
        app.cacheDir.listFiles { f -> f.name.startsWith("export_") }?.forEach { it.delete() }
    }

    fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            try {
                val i = withContext(Dispatchers.IO) { VideoMetadataReader.read(app, uri) }
                info = i
                val base = EditorSettings(trimStartMs = 0, trimEndMs = i.durationMs)
                settings = pendingPreset?.apply?.invoke(base) ?: base
                presetName = pendingPreset?.name
                pendingPreset = null
                undoStack.clear(); canUndo = false
                export = ExportUi.Idle
                screen = Screen.EDITOR
            } catch (e: Exception) {
                error = e.message ?: "Could not open that video."
            }
        }
    }

    // --- editing with undo ---
    private fun pushUndo(s: EditorSettings) {
        undoStack.addLast(s); if (undoStack.size > 50) undoStack.removeFirst(); canUndo = true
    }
    /** Sliders: call while dragging. */
    fun change(new: EditorSettings) { if (dragStart == null) dragStart = settings; settings = new }
    /** Sliders: call when the finger lifts. */
    fun commit() { dragStart?.let { if (it != settings) pushUndo(it) }; dragStart = null }
    /** Buttons/chips: one step. */
    fun applyStep(new: EditorSettings) { dragStart = null; if (new != settings) pushUndo(settings); settings = new }
    fun applyPreset(p: Preset) { presetName = p.name; applyStep(p.apply(settings)) }
    fun undo() {
        if (undoStack.isEmpty()) return
        settings = undoStack.removeLast(); canUndo = undoStack.isNotEmpty()
    }
    fun reset() {
        val i = info ?: return
        presetName = null
        applyStep(EditorSettings(trimStartMs = 0, trimEndMs = i.durationMs))
    }

    // --- export ---
    fun exportSecondsEstimate(): Long = ((settings.trimEndMs - settings.trimStartMs) / 1000).coerceAtLeast(1)

    fun startExport(dest: Uri) {
        val i = info ?: return
        if (Build.VERSION.SDK_INT >= 29) {
            val pm = app.getSystemService(PowerManager::class.java)
            if (pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
                export = ExportUi.Failed("Your phone is too hot. Let it cool down, then try again."); return
            }
        }
        val needed = settings.bitrateMbps * 1_000_000L / 8 * exportSecondsEstimate() * 12 / 10 + 50L * 1024 * 1024
        val free = StatFs(app.cacheDir.path).availableBytes
        if (free < needed) {
            export = ExportUi.Failed("Not enough free space. Need about ${needed / 1_048_576} MB, have ${free / 1_048_576} MB."); return
        }
        val tmp = File(app.cacheDir, "export_${System.currentTimeMillis()}.mp4")
        tmpFile = tmp
        export = ExportUi.Running(0)
        try {
            manager.start(i.uri, settings, i, tmp, object : VideoExportManager.Callback {
                override fun onProgress(percent: Int) { if (export is ExportUi.Running) export = ExportUi.Running(percent) }
                override fun onDone() { copyToDestination(tmp, dest) }
                override fun onError(message: String) { tmp.delete(); export = ExportUi.Failed(message) }
            })
        } catch (e: Exception) {
            tmp.delete(); export = ExportUi.Failed(e.message ?: "Could not start export.")
        }
    }

    private fun copyToDestination(tmp: File, dest: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(dest, "w")!!.use { out ->
                        tmp.inputStream().use { it.copyTo(out) }
                    }
                }
                export = ExportUi.Done(dest)
            } catch (e: Exception) {
                export = ExportUi.Failed("Could not save to the chosen location: ${e.message}")
            } finally { tmp.delete() }
        }
    }

    fun cancelExport() { manager.cancel(); tmpFile?.delete(); export = ExportUi.Idle }

    override fun onCleared() { manager.cancel(); tmpFile?.delete() }
}
