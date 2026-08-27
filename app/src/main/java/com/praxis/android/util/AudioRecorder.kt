package com.praxis.android.util

import android.media.MediaRecorder
import java.io.File

/**
 * Thin wrapper over [MediaRecorder] for voice notes: AAC in an m4a container,
 * microphone as source. One recording at a time — [isRecording] guards the
 * capture screen against double-starts from widget deep links.
 */
object AudioRecorder {

    private var recorder: MediaRecorder? = null

    fun isRecording(): Boolean = recorder != null

    fun start(file: File) {
        val r = MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        recorder = r
    }

    /** Stop and release; safe to call even when nothing is recording. */
    fun stop() {
        recorder?.let { r ->
            runCatching {
                r.stop()
                r.release()
            }.onFailure {
                // A stop() on a recorder that never wrote a frame throws;
                // release anyway so the native resource does not leak.
                runCatching { r.release() }
            }
        }
        recorder = null
    }
}
