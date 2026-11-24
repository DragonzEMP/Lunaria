package com.lunaria.musicplayer.player

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Manages the Android Visualizer API, capturing Fast Fourier Transform (FFT) frequency bands
 * and outputting normalized float magnitudes at ~60fps for reactive UI visualization.
 */
object VisualizerManager {

    private const val TAG = "VisualizerManager"
    const val NUM_BARS = 36

    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0
    private var isPlayingActive: Boolean = false
    private var hasRecordPermission: Boolean = false
    private var activeSamplingRateHz: Int = 44100

    private val smoothedMagnitudes = FloatArray(NUM_BARS) { 0.05f }
    private val _magnitudes = MutableStateFlow<List<Float>>(List(NUM_BARS) { 0.05f })
    val magnitudes: StateFlow<List<Float>> = _magnitudes.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var decayJob: Job? = null

    init {
        startDecayLoop()
    }

    /**
     * Attaches visualizer to the audioSessionId with permission check.
     */
    fun attachToAudioSession(audioSessionId: Int, hasPermission: Boolean) {
        hasRecordPermission = hasPermission
        if (!hasPermission || audioSessionId <= 0) {
            release()
            return
        }

        if (audioSessionId == currentSessionId && visualizer != null) return
        currentSessionId = audioSessionId

        release()

        try {
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val captureSize = captureSizeRange[1].coerceAtMost(512)

            visualizer = Visualizer(audioSessionId).apply {
                enabled = false
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            // Waveform unused, using FFT for frequency bars
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (samplingRate > 0) {
                                activeSamplingRateHz = if (samplingRate > 100000) samplingRate / 1000 else samplingRate
                            }
                            if (fft != null && isPlayingActive) {
                                processFftData(fft)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    false,
                    true
                )
                enabled = isPlayingActive
            }
            Log.d(TAG, "Visualizer attached to session $audioSessionId (size $captureSize, sampleRate $activeSamplingRateHz)")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Visualizer: ${e.message}", e)
        }
    }

    /**
     * Toggles visualizer capture state based on playback state to preserve battery.
     */
    fun setPlaybackActive(isPlaying: Boolean) {
        isPlayingActive = isPlaying
        try {
            visualizer?.enabled = isPlaying && hasRecordPermission
        } catch (e: Exception) {
            Log.w(TAG, "Error toggling visualizer enabled: ${e.message}")
        }
    }

    fun updatePermission(hasPermission: Boolean) {
        hasRecordPermission = hasPermission
        if (currentSessionId > 0 && hasPermission && visualizer == null) {
            attachToAudioSession(currentSessionId, true)
        } else if (!hasPermission) {
            release()
        }
    }

    /**
     * Processes raw FFT bytes into NUM_BARS musically-spaced frequency bands
     * using logarithmic frequency binning from ~35Hz to ~14kHz, pink-noise / Fletcher-Munson
     * compensation curve, and non-linear dynamic range compression.
     */
    private fun processFftData(fft: ByteArray) {
        val n = fft.size
        if (n < 4) return

        val totalBins = (n / 2) - 1
        if (totalBins <= 1) return

        val binWidthHz = activeSamplingRateHz.toDouble() / n.toDouble()
        val minFreq = 35.0
        val maxFreq = 14000.0.coerceAtMost((activeSamplingRateHz / 2.0) - 200.0)

        val newMagnitudes = FloatArray(NUM_BARS)

        for (i in 0 until NUM_BARS) {
            val t0 = i.toDouble() / NUM_BARS
            val t1 = (i + 1).toDouble() / NUM_BARS

            // Logarithmic frequency bounds across the audible spectrum (35Hz - 14kHz)
            val fStart = minFreq * (maxFreq / minFreq).pow(t0)
            val fEnd = minFreq * (maxFreq / minFreq).pow(t1)

            val kStart = (fStart / binWidthHz).toInt().coerceIn(1, totalBins)
            val kEnd = ceil(fEnd / binWidthHz).toInt().coerceIn(kStart + 1, totalBins + 1)

            var peakMagnitude = 0.0
            for (k in kStart until kEnd) {
                val real = fft[2 * k].toDouble()
                val imag = fft[2 * k + 1].toDouble()
                val mag = sqrt(real * real + imag * imag)
                if (mag > peakMagnitude) {
                    peakMagnitude = mag
                }
            }

            // 1. Non-linear dynamic compression: amplifies quiet treble details without clipping bass
            val normMag = (peakMagnitude / 92.0).pow(0.74).toFloat().coerceIn(0f, 1.2f)

            // 2. Pink-noise / Fletcher-Munson equal loudness compensation:
            // High/mid frequencies naturally have less sound power; progressively boost them so waves pulse across the full screen
            val progress = i.toFloat() / (NUM_BARS - 1).toFloat()
            val compensation = 1.0f + 3.6f * progress.toDouble().pow(1.35).toFloat()

            val weighted = (normMag * compensation).coerceIn(0.04f, 1.0f)

            // 3. Fast attack for punchy transient beats, smooth decay fall-off
            val current = smoothedMagnitudes[i]
            smoothedMagnitudes[i] = if (weighted >= current) {
                current * 0.12f + weighted * 0.88f // Rapid beat attack
            } else {
                current * 0.76f + weighted * 0.24f // Smooth musical decay
            }
            newMagnitudes[i] = smoothedMagnitudes[i]
        }

        _magnitudes.value = newMagnitudes.toList()
    }

    /**
     * Gravity decay loop to smoothly return bars to floor when audio fades or pauses.
     */
    private fun startDecayLoop() {
        decayJob?.cancel()
        decayJob = scope.launch {
            while (isActive) {
                if (!isPlayingActive) {
                    var allSettled = true
                    for (i in 0 until NUM_BARS) {
                        if (smoothedMagnitudes[i] > 0.05f) {
                            smoothedMagnitudes[i] = (smoothedMagnitudes[i] * 0.85f).coerceAtLeast(0.04f)
                            allSettled = false
                        }
                    }
                    if (!allSettled) {
                        _magnitudes.value = smoothedMagnitudes.toList()
                    }
                }
                delay(33) // ~30 fps decay tick
            }
        }
    }

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing visualizer: ${e.message}")
        }
        visualizer = null
        currentSessionId = 0
    }
}

