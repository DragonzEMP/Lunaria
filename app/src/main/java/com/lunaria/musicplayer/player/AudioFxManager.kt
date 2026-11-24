package com.lunaria.musicplayer.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.ln
import kotlin.math.roundToInt

data class EqBand(
    val index: Int,
    val centerFreqHz: Int,
    val displayFrequency: String,
    val levelMb: Int,
    val levelDb: Float
)

/**
 * High-performance audio DSP engine hooking into Android native audiofx.
 * Provides an 8-band Equalizer interface with logarithmic hardware DSP mapping,
 * BassBoost, Virtualizer 3D surround, and Preamp LoudnessEnhancer.
 */
object AudioFxManager {

    private const val TAG = "AudioFxManager"

    // Strict 8-band UI target frequencies and concise labels
    val TARGET_FREQUENCIES_HZ = listOf(60, 230, 910, 3000, 8000, 12000, 14000, 16000)
    val TARGET_FREQUENCY_LABELS = listOf("60Hz", "230Hz", "910Hz", "3kHz", "8kHz", "12kHz", "14kHz", "16kHz")

    private data class HwBand(val index: Short, val centerFreqHz: Int)
    private var hardwareBands: List<HwBand> = emptyList()

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0

    // Master power state
    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // Session flag to determine if user manually toggled EQ ON while in Online Mode
    var userManuallyEnabledEqInOnline: Boolean = false

    // Strict 8-band reactive state
    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    val bands: StateFlow<List<EqBand>> = _bands.asStateFlow()

    var minLevelMb: Int = -1500
        private set
    var maxLevelMb: Int = 1500
        private set

    // Bass Boost (0..1000)
    private val _bassStrength = MutableStateFlow(350)
    val bassStrength: StateFlow<Int> = _bassStrength.asStateFlow()

    // Virtualizer Surround (0..1000)
    private val _virtualizerStrength = MutableStateFlow(400)
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    // Preamp Loudness Gain (+400mB to +800mB baseline volume boost)
    const val BASELINE_PREAMP_GAIN_MB = 600
    private val _preampGainMb = MutableStateFlow(BASELINE_PREAMP_GAIN_MB)
    val preampGainMb: StateFlow<Int> = _preampGainMb.asStateFlow()

    // Active Preset
    private val _currentPreset = MutableStateFlow("Electronic")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    val availablePresets = listOf(
        "Flat",
        "Rock",
        "Pop",
        "Classical",
        "Electronic",
        "Bass Boost",
        "Vocal Boost",
        "Custom"
    )

    init {
        // Initialize default 8-band configuration
        initializeDefaultBands()
    }

    /**
     * Attaches audiofx DSP to the given audioSessionId from ExoPlayer.
     */
    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        currentSessionId = audioSessionId
        release()

        try {
            // 1. Equalizer setup
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
                val range = bandLevelRange
                if (range.size >= 2) {
                    minLevelMb = range[0].toInt()
                    maxLevelMb = range[1].toInt()
                }
            }
            cacheHardwareBands()

            // 2. Bass Boost setup
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength(_bassStrength.value.toShort())
                }
            }

            // 3. Virtualizer setup
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength(_virtualizerStrength.value.toShort())
                }
            }

            // 4. Loudness Enhancer (Preamp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = _isEnabled.value
                    setTargetGain(_preampGainMb.value)
                }
            }

            // Apply active preset & push 8-band values to hardware DSP
            applyPreset(_currentPreset.value)
            Log.d(TAG, "AudioFx attached to audioSessionId: $audioSessionId (HW bands: ${hardwareBands.size})")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing hardware audiofx: ${e.message}", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            loudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling AudioFx state: ${e.message}")
        }
    }

    /**
     * Updates one of the 8 UI bands and interpolates gain across native hardware bands.
     */
    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        val levelMb = (levelDb * 100f).roundToInt().coerceIn(minLevelMb, maxLevelMb)
        val updatedBands = _bands.value.map { band ->
            if (band.index == bandIndex) {
                band.copy(levelMb = levelMb, levelDb = levelDb)
            } else band
        }
        _bands.value = updatedBands
        _currentPreset.value = "Custom"
        pushUiBandsToHardware()
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassStrength.value = clamped
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating BassBoost: ${e.message}")
        }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _virtualizerStrength.value = clamped
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating Virtualizer: ${e.message}")
        }
    }

    fun setPreampGain(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1000)
        _preampGainMb.value = clamped
        try {
            loudnessEnhancer?.setTargetGain(clamped)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating LoudnessEnhancer: ${e.message}")
        }
    }

    /**
     * Sets 8-band levels from standard presets and pushes to hardware DSP.
     */
    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        // Strict 8-band preset profiles: 60Hz, 230Hz, 910Hz, 3kHz, 8kHz, 12kHz, 14kHz, 16kHz
        val dbValues = when (presetName) {
            "Flat" -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            "Rock" -> listOf(5.0f, 3.5f, -1.0f, -0.5f, 2.0f, 3.5f, 4.5f, 4.0f)
            "Pop" -> listOf(2.0f, 3.5f, 4.0f, 3.0f, 2.0f, 1.5f, 2.5f, 2.0f)
            "Classical" -> listOf(4.0f, 3.0f, 1.0f, -1.5f, 1.0f, 2.5f, 3.5f, 4.0f)
            "Electronic" -> listOf(5.5f, 4.5f, 1.5f, 0.0f, 1.5f, 3.0f, 4.5f, 5.0f)
            "Bass Boost" -> listOf(7.0f, 5.5f, 3.5f, 1.5f, 0.0f, -0.5f, -1.0f, -1.5f)
            "Vocal Boost" -> listOf(-2.5f, -0.5f, 2.5f, 5.0f, 4.0f, 2.5f, 1.0f, 0.0f)
            else -> return
        }

        val updated = TARGET_FREQUENCIES_HZ.mapIndexed { i, hz ->
            val db = if (i < dbValues.size) dbValues[i] else 0f
            val mb = (db * 100f).roundToInt().coerceIn(minLevelMb, maxLevelMb)
            val label = TARGET_FREQUENCY_LABELS.getOrElse(i) { "${hz}Hz" }
            EqBand(
                index = i,
                centerFreqHz = hz,
                displayFrequency = label,
                levelMb = mb,
                levelDb = db
            )
        }
        _bands.value = updated
        pushUiBandsToHardware()
    }

    /**
     * Caches native hardware equalizer band frequencies from the Android DSP.
     */
    private fun cacheHardwareBands() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val list = mutableListOf<HwBand>()
            for (i in 0 until numBands) {
                // getCenterFreq returns milliHertz
                val centerHz = (eq.getCenterFreq(i.toShort()) / 1000).coerceAtLeast(20)
                list.add(HwBand(i.toShort(), centerHz))
            }
            hardwareBands = list
            Log.d(TAG, "Hardware DSP bands: ${list.map { "${it.index}: ${it.centerFreqHz}Hz" }}")
        } catch (e: Exception) {
            Log.w(TAG, "Error caching hardware bands: ${e.message}")
        }
    }

    /**
     * Mathematically interpolates the gain for any arbitrary hardware frequency
     * based on the logarithmic distribution of the 8 UI sliders.
     */
    private fun interpolateGainForFrequency(freqHz: Int, uiBands: List<EqBand>): Int {
        if (uiBands.isEmpty()) return 0
        if (uiBands.size == 1) return uiBands[0].levelMb

        val f = freqHz.coerceAtLeast(20).toDouble()
        val firstFreq = uiBands.first().centerFreqHz.toDouble()
        val lastFreq = uiBands.last().centerFreqHz.toDouble()

        if (f <= firstFreq) return uiBands.first().levelMb
        if (f >= lastFreq) return uiBands.last().levelMb

        for (i in 0 until uiBands.size - 1) {
            val f0 = uiBands[i].centerFreqHz.toDouble()
            val f1 = uiBands[i + 1].centerFreqHz.toDouble()
            if (f in f0..f1) {
                val logF = ln(f)
                val logF0 = ln(f0)
                val logF1 = ln(f1)
                val t = ((logF - logF0) / (logF1 - logF0)).coerceIn(0.0, 1.0)
                val g0 = uiBands[i].levelMb.toDouble()
                val g1 = uiBands[i + 1].levelMb.toDouble()
                return ((1.0 - t) * g0 + t * g1).roundToInt().coerceIn(minLevelMb, maxLevelMb)
            }
        }
        return uiBands.last().levelMb
    }

    /**
     * Pushes current 8-band UI settings to the native hardware Equalizer.
     */
    private fun pushUiBandsToHardware() {
        val eq = equalizer ?: return
        val currentUiBands = _bands.value
        if (currentUiBands.isEmpty() || hardwareBands.isEmpty()) return

        for (hw in hardwareBands) {
            val targetMb = interpolateGainForFrequency(hw.centerFreqHz, currentUiBands)
            try {
                eq.setBandLevel(hw.index, targetMb.toShort())
            } catch (e: Exception) {
                Log.w(TAG, "Failed setting hardware band ${hw.index} level: ${e.message}")
            }
        }
    }

    private fun initializeDefaultBands() {
        applyPreset("Electronic")
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audiofx: ${e.message}")
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        hardwareBands = emptyList()
        currentSessionId = 0
    }
}

