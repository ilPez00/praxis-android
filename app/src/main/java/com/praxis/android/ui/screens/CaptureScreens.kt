package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.praxis.android.auth.AuthManager
import com.praxis.android.data.api.SupabaseStorage
import com.praxis.android.data.model.CreateEntryRequest
import com.praxis.android.data.repository.PraxisRepository
import com.praxis.android.ui.screens.common.DataUi
import com.praxis.android.util.AudioRecorder
import com.praxis.android.util.LocationHelper
import kotlinx.coroutines.launch
import java.io.File

/**
 * Photo, audio and video capture in one screen — the mode comes from the nav
 * argument (`capture?mode=photo|audio|video`).
 *
 * Every capture becomes a notebook entry (with optional GPS tag). Online it
 * uploads straight to Supabase Storage; offline the file is queued in Room and
 * the sync worker lands it later — captures taken on a trail must survive
 * having no signal.
 */
@Composable
fun MediaCaptureScreen(
    repo: PraxisRepository,
    mode: String,
    autoStart: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var mimeType by remember { mutableStateOf("image/jpeg") }
    var status by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var attachLocation by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (!ok) {
            status = "Capture cancelled."
            pendingFile?.delete(); pendingFile = null
        } else {
            mimeType = "image/jpeg"
            pendingFile?.let { file ->
                capturedUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                status = null
            }
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        if (!ok) {
            status = "Recording cancelled."
            pendingFile?.delete(); pendingFile = null
        } else {
            mimeType = "video/mp4"
            pendingFile?.let { file ->
                capturedUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                status = null
            }
        }
    }

    fun newCaptureFile(ext: String): Pair<File, Uri> {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "praxis_${System.currentTimeMillis()}.$ext")
        pendingFile = file
        return file to FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun capturePhoto() {
        AudioRecorder.stop()
        val (_, uri) = newCaptureFile("jpg")
        runCatching { photoLauncher.launch(uri) }.onFailure { status = "No camera available on this device." }
    }

    fun captureVideo() {
        AudioRecorder.stop()
        val (_, uri) = newCaptureFile("mp4")
        runCatching { videoLauncher.launch(uri) }.onFailure { status = "No camera available on this device." }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            status = "Microphone permission is needed for voice notes."
        } else {
            val (file, _) = newCaptureFile("m4a")
            runCatching {
                AudioRecorder.start(file)
                isRecording = true
                status = "Recording… tap Stop when done."
            }.onFailure { status = "Recorder unavailable." }
        }
    }

    fun captureAudio() {
        micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    // Location is requested the moment the user opts into tagging — without
    // this runtime ask the manifest declaration alone never grants access on
    // API 23+, and getCurrentLocation would silently return null forever.
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) {
            attachLocation = true
            status = "Location will be tagged on save."
        } else {
            attachLocation = false
            status = "Location permission denied — capture will be saved without GPS."
        }
    }

    LaunchedEffect(mode, autoStart) {
        when {
            mode == "photo" && autoStart -> capturePhoto()
            mode == "video" && autoStart -> captureVideo()
            mode == "audio" && autoStart && !isRecording -> captureAudio()
        }
    }

    // Leaving the screen mid-recording must release the microphone.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { AudioRecorder.stop() }
    }

    DataUi.ScreenScaffold(
        title = when (mode) {
            "audio" -> "Voice note"
            "video" -> "Video log"
            else -> "Camera"
        },
        onBack = onBack
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { capturePhoto() }, modifier = Modifier.weight(1f)) { Text("Photo") }
            OutlinedButton(onClick = { captureAudio() }, modifier = Modifier.weight(1f)) { Text("Audio") }
            OutlinedButton(onClick = { captureVideo() }, modifier = Modifier.weight(1f)) { Text("Video") }
        }

        if (isRecording) {
            androidx.compose.material3.Button(
                onClick = {
                    AudioRecorder.stop()
                    isRecording = false
                    pendingFile?.let { file ->
                        mimeType = "audio/mp4"
                        capturedUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        status = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Stop recording") }
        }

        status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        capturedUri?.let { uri ->
            if (mimeType.startsWith("image/")) {
                AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(240.dp))
            } else {
                val kb = pendingFile?.length()?.div(1024) ?: 0L
                Text("${mimeType.substringAfter('/')} captured · ${kb} KB", style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tag with current location", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = attachLocation,
                    onCheckedChange = { want ->
                        if (want && !com.praxis.android.util.LocationHelper.hasPermission(context)) {
                            locationPermission.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        } else if (want && !com.praxis.android.util.LocationHelper.isAnyProviderEnabled(context)) {
                            attachLocation = false
                            status = "No location provider is enabled — turn on GPS in system settings."
                        } else {
                            attachLocation = want
                        }
                    }
                )
            }

            PraxisButton(
                onClick = {
                    val file = pendingFile ?: return@PraxisButton
                    scope.launch {
                        status = "Saving…"
                        val outcome = saveCapture(repo, context, file, mimeType, attachLocation)
                        status = outcome
                        if (!outcome.startsWith("Queued")) {
                            capturedUri = null
                            pendingFile = null
                            attachLocation = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecording
            ) { Text("Save to notebook") }
        }
    }
}

/**
 * Upload + attach one capture. Returns a human-readable outcome. An upload
 * that cannot complete right now queues the bytes instead of losing them.
 */
private suspend fun saveCapture(
    repo: PraxisRepository,
    context: android.content.Context,
    file: File,
    mimeType: String,
    withLocation: Boolean
): String {
    val location = if (withLocation) LocationHelper.getCurrentLocation(context) else null
    val storagePath = SupabaseStorage.storagePath(
        userId = AuthManager.getUserId(context) ?: "anon",
        fileName = file.name
    )

    try {
        val result = SupabaseStorage.upload(context, file, storagePath, mimeType)
        val attachment = mapOf<String, Any?>(
            "type" to "file",
            "url" to result.publicUrl,
            "name" to file.name,
            "mimeType" to mimeType
        )
        val kind = mimeType.substringBefore('/')
        val res = repo.createNotebookEntry(
            CreateEntryRequest(
                content = "[$kind] ${result.publicUrl}",
                entryType = kind,
                lat = location?.latitude,
                lng = location?.longitude,
                attachments = listOf(attachment)
            )
        )
        return if (res.isSuccess) "Saved." else res.exceptionOrNull()?.message ?: "Could not save."
    } catch (e: Exception) {
        repo.queueUpload(file.absolutePath, storagePath, mimeType)
        return "Queued — will upload next time you're online."
    }
}
