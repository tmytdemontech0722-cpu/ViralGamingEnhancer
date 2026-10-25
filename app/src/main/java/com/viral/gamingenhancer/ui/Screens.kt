@file:OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)

package com.viral.gamingenhancer.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.viral.gamingenhancer.data.EditorSettings
import com.viral.gamingenhancer.data.OutputAspect
import com.viral.gamingenhancer.data.PresetManager
import com.viral.gamingenhancer.effects.EffectChain
import com.viral.gamingenhancer.media.VideoInfo
import kotlinx.coroutines.delay

@Composable
fun AppRoot(vm: EditorViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.loadVideo(uri)
    }
    val launchPicker = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }

    val exporting = vm.export is ExportUi.Running
    val view = LocalView.current
    SideEffect { view.keepScreenOn = exporting }   // keep screen awake while rendering

    BackHandler(enabled = vm.screen != Screen.HOME && !exporting) {
        vm.screen = if (vm.screen == Screen.EXPORT) Screen.EDITOR else Screen.HOME
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.statusBarsPadding().navigationBarsPadding()) {
            when (vm.screen) {
                Screen.HOME -> HomeScreen(vm, launchPicker)
                Screen.EDITOR -> EditorScreen(vm)
                Screen.EXPORT -> ExportScreen(vm)
            }
        }
    }
    vm.error?.let {
        AlertDialog(
            onDismissRequest = { vm.error = null },
            confirmButton = { TextButton(onClick = { vm.error = null }) { Text("OK") } },
            title = { Text("Can't use this video") }, text = { Text(it) }
        )
    }
}

// ───────────────────────── HOME ─────────────────────────
@Composable
fun HomeScreen(vm: EditorViewModel, pick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("VIRAL", style = MaterialTheme.typography.displaySmall, color = NeonBlue)
        Text("GAMING ENHANCER", style = MaterialTheme.typography.titleLarge, color = NeonPurple)
        Text("Colour-grade your Free Fire MAX clips and export 9:16 Reels. " +
            "Everything runs on your phone. Nothing is uploaded.",
            style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { vm.pendingPreset = null; pick() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Import Gameplay")
        }
        Text("Preset shortcuts", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PresetManager.all) { p ->
                AssistChip(onClick = { vm.pendingPreset = p; pick() }, label = { Text(p.name) })
            }
        }
        Text("Tap a preset to pick a video and start with that look.",
            style = MaterialTheme.typography.bodySmall)
        Text("Rendering can take longer than the clip itself and may warm up your phone.",
            style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────── EDITOR ─────────────────────────
@Composable
fun EditorScreen(vm: EditorViewModel) {
    val info = vm.info ?: return
    val s = vm.settings
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text(info.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text("${fmtTime(info.durationMs)} • ${info.width}×${info.height} • " +
            (if (info.fps > 0) "${info.fps.toInt()} fps" else "fps unknown") +
            " • ${"%.1f".format(info.sizeBytes / 1_048_576f)} MB • ${info.mime}",
            style = MaterialTheme.typography.bodySmall)

        PreviewSection(info, s)

        Section("Trim")
        RangeSlider(
            value = s.trimStartMs.toFloat()..s.trimEndMs.toFloat(),
            onValueChange = { r ->
                vm.change(s.copy(trimStartMs = r.start.toLong(), trimEndMs = r.endInclusive.toLong()))
            },
            onValueChangeFinished = { vm.commit() },
            valueRange = 0f..info.durationMs.toFloat()
        )
        Text("${fmtTime(s.trimStartMs)} → ${fmtTime(s.trimEndMs)}  (${fmtTime(s.trimEndMs - s.trimStartMs)})",
            style = MaterialTheme.typography.bodySmall)
        Text("The preview plays the whole clip; the trim is applied on export.", style = MaterialTheme.typography.bodySmall)

        Section("Presets")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PresetManager.all) { p ->
                FilterChip(selected = vm.presetName == p.name, onClick = { vm.applyPreset(p) }, label = { Text(p.name) })
            }
        }
        PresetManager.all.firstOrNull { it.name == vm.presetName }?.let {
            Text(it.note, style = MaterialTheme.typography.bodySmall)
        }

        Section("Adjustments")
        Adj("Saturation", s.saturation, vm) { s.copy(saturation = it) }
        Adj("Contrast", s.contrast, vm) { s.copy(contrast = it) }
        Adj("Brightness", s.brightness, vm) { s.copy(brightness = it) }
        Adj("Temperature (cool ↔ warm)", s.temperature, vm) { s.copy(temperature = it) }
        Adj("Tint (green ↔ magenta)", s.tint, vm) { s.copy(tint = it) }

        Section("Output")
        Text("Aspect ratio", style = MaterialTheme.typography.bodySmall)
        ChipRow(OutputAspect.entries.toList(), { it == s.aspect }, { it.label }) { vm.applyStep(s.copy(aspect = it)) }
        Text("Frame rate (can only lower, never invent frames)", style = MaterialTheme.typography.bodySmall)
        ChipRow(listOf(0, 30, 60), { it == s.targetFps }, { if (it == 0) "Original" else "$it fps" }) { vm.applyStep(s.copy(targetFps = it)) }
        Text("Video bitrate", style = MaterialTheme.typography.bodySmall)
        ChipRow(listOf(8, 12, 20), { it == s.bitrateMbps }, { "$it Mbps" }) { vm.applyStep(s.copy(bitrateMbps = it)) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.undo() }, enabled = vm.canUndo) { Text("Undo") }
            OutlinedButton(onClick = { vm.reset() }) { Text("Reset") }
            Button(onClick = { vm.export = ExportUi.Idle; vm.screen = Screen.EXPORT }, modifier = Modifier.weight(1f)) { Text("Export…") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun Section(t: String) = Text(t, style = MaterialTheme.typography.titleMedium, color = NeonBlue)

@Composable
private fun Adj(label: String, value: Float, vm: EditorViewModel, make: (Float) -> EditorSettings) {
    Text("$label: ${"%.2f".format(value)}", style = MaterialTheme.typography.bodySmall)
    Slider(value = value, onValueChange = { vm.change(make(it)) },
        onValueChangeFinished = { vm.commit() }, valueRange = -1f..1f)
}

@Composable
private fun <T> ChipRow(items: List<T>, selected: (T) -> Boolean, label: (T) -> String, onPick: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { FilterChip(selected = selected(it), onClick = { onPick(it) }, label = { Text(label(it)) }) }
    }
}

@Composable
private fun PreviewSection(info: VideoInfo, s: EditorSettings) {
    val ctx = LocalContext.current
    val player = remember(info.uri) {
        ExoPlayer.Builder(ctx).build().apply { setMediaItem(MediaItem.fromUri(info.uri)); prepare() }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    var enhanced by remember { mutableStateOf(true) }
    var pos by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }

    // Same colour effects as the export. Aspect/FPS are export-only.
    LaunchedEffect(player, enhanced, s.saturation, s.contrast, s.brightness, s.temperature, s.tint) {
        player.setVideoEffects(if (enhanced) EffectChain.colorEffects(s) else emptyList())
    }
    LaunchedEffect(player) { while (true) { pos = player.currentPosition; playing = player.isPlaying; delay(200) } }

    AndroidView(
        factory = { PlayerView(it).apply { useController = false; this.player = player } },
        update = { it.player = player },
        modifier = Modifier.fillMaxWidth().height(300.dp)
    )
    Slider(value = pos.toFloat().coerceIn(0f, info.durationMs.toFloat()),
        onValueChange = { player.seekTo(it.toLong()); pos = it.toLong() },
        valueRange = 0f..info.durationMs.toFloat())
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { if (player.isPlaying) player.pause() else player.play() }) { Text(if (playing) "Pause" else "Play") }
        Text("${fmtTime(pos)} / ${fmtTime(info.durationMs)}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.weight(1f))
        Text(if (enhanced) "Enhanced" else "Original", style = MaterialTheme.typography.bodySmall)
        Switch(checked = enhanced, onCheckedChange = { enhanced = it })
    }
}

// ───────────────────────── EXPORT ─────────────────────────
@Composable
fun ExportScreen(vm: EditorViewModel) {
    val info = vm.info ?: return
    val s = vm.settings
    val ctx = LocalContext.current
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        if (uri != null) vm.startExport(uri)
    }
    val outRes = if (s.aspect == OutputAspect.ORIGINAL) "${info.width}×${info.height} (original)" else "${s.aspect.width}×${s.aspect.height}"
    val effFps = if (s.targetFps > 0 && info.fps > s.targetFps + 1) "${s.targetFps} fps (frames dropped)"
        else if (s.targetFps > 0) "unchanged (${s.targetFps} fps can't be created from ${info.fps.toInt()} fps)"
        else "original"

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Export", style = MaterialTheme.typography.headlineSmall, color = NeonBlue)
        Text("Resolution: $outRes\nFrame rate: $effFps\nCodec: H.264 (AVC) + original audio\n" +
            "Aspect: ${s.aspect.label}\nBitrate: ${s.bitrateMbps} Mbps\n" +
            "Length: ${fmtTime(s.trimEndMs - s.trimStartMs)}\nPreset: ${vm.presetName ?: "custom"}")
        Text("Your original file is never changed. Rendering may take longer than the clip and warm the phone; " +
            "keep the app open and the screen on.", style = MaterialTheme.typography.bodySmall)

        when (val e = vm.export) {
            ExportUi.Idle -> {
                Button(onClick = { saver.launch("VGE_${System.currentTimeMillis() / 1000}.mp4") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose location & start")
                }
                OutlinedButton(onClick = { vm.screen = Screen.EDITOR }, modifier = Modifier.fillMaxWidth()) { Text("Back to editor") }
            }
            is ExportUi.Running -> {
                LinearProgressIndicator(progress = { e.percent / 100f }, modifier = Modifier.fillMaxWidth())
                Text("Rendering… ${e.percent}%")
                OutlinedButton(onClick = { vm.cancelExport() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
            is ExportUi.Done -> {
                Text("Export finished and saved.", color = NeonBlue)
                Button(onClick = { ctx.startActivity(viewIntent(e.uri)) }, modifier = Modifier.fillMaxWidth()) { Text("Play exported video") }
                Button(onClick = { ctx.startActivity(Intent.createChooser(shareIntent(e.uri), "Share video")) },
                    modifier = Modifier.fillMaxWidth()) { Text("Share") }
                OutlinedButton(onClick = { vm.export = ExportUi.Idle; vm.screen = Screen.EDITOR }, modifier = Modifier.fillMaxWidth()) { Text("Back to editor") }
            }
            is ExportUi.Failed -> {
                Text(e.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = { vm.export = ExportUi.Idle }, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
        }
    }
}

private fun viewIntent(uri: Uri) = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, "video/mp4"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
}
private fun shareIntent(uri: Uri) = Intent(Intent.ACTION_SEND).apply {
    type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

fun fmtTime(ms: Long): String {
    val t = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(t / 60, t % 60)
}
