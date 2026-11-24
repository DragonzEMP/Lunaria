package com.lunaria.musicplayer.ui.equalizer

import androidx.lifecycle.ViewModel
import com.lunaria.musicplayer.player.AudioFxManager
import com.lunaria.musicplayer.player.EqBand
import com.lunaria.musicplayer.player.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * ViewModel acting as reactive bridge between EqualizerScreen and AudioFxManager.
 * Features an automatic anti-clipping safeguard that dynamically attenuates the preamp/volume
 * whenever any EQ band is boosted above 0 dB.
 */
class EqualizerViewModel : ViewModel() {

    val isEnabled: StateFlow<Boolean> = AudioFxManager.isEnabled
    val bands: StateFlow<List<EqBand>> = AudioFxManager.bands
    val bassStrength: StateFlow<Int> = AudioFxManager.bassStrength
    val virtualizerStrength: StateFlow<Int> = AudioFxManager.virtualizerStrength
    val preampGainMb: StateFlow<Int> = AudioFxManager.preampGainMb
    val currentPreset: StateFlow<String> = AudioFxManager.currentPreset

    val availablePresets: List<String> = AudioFxManager.availablePresets
    val minLevelMb: Int get() = AudioFxManager.minLevelMb
    val maxLevelMb: Int get() = AudioFxManager.maxLevelMb

    private var baselinePreampGainMb: Int = AudioFxManager.BASELINE_PREAMP_GAIN_MB

    // Headroom attenuation applied in dB for anti-clipping
    private val _autoAttenuationDb = MutableStateFlow(0f)
    val autoAttenuationDb: StateFlow<Float> = _autoAttenuationDb.asStateFlow()

    init {
        // Enforce anti-clipping safeguard for active preset
        applyAntiClippingSafeguard()
    }

    /**
     * Dynamic Anti-Clipping (Auto-Preamp) Safeguard:
     * Calculates the maximum boosted value across all active EQ bands (> 0 dB).
     * Automatically reduces LoudnessEnhancer (or ExoPlayer master volume) by that exact
     * maximum boosted value, creating artificial headroom to eliminate digital clipping/distortion.
     */
    fun applyAntiClippingSafeguard() {
        if (!isEnabled.value) {
            PlaybackManager.setMasterVolume(1.0f)
            AudioFxManager.setPreampGain(baselinePreampGainMb)
            _autoAttenuationDb.value = 0f
            return
        }

        val currentBands = AudioFxManager.bands.value
        val maxBoostDb = currentBands.maxOfOrNull { it.levelDb }?.coerceAtLeast(0f) ?: 0f
        _autoAttenuationDb.value = maxBoostDb

        val maxBoostMb = (maxBoostDb * 100f).roundToInt()

        // 1. Automatically reduce LoudnessEnhancer preamp by the maximum boosted value
        val effectivePreampMb = (baselinePreampGainMb - maxBoostMb).coerceAtLeast(0)
        AudioFxManager.setPreampGain(effectivePreampMb)

        // 2. If the boost exceeds the baseline preamp boost range (e.g. > +6dB),
        // apply the remaining attenuation to ExoPlayer master volume
        val excessBoostDb = (maxBoostDb - (baselinePreampGainMb / 100f)).coerceAtLeast(0f)
        val masterVolume = if (excessBoostDb > 0f) {
            10.0.pow((-excessBoostDb / 20.0)).toFloat().coerceIn(0.05f, 1.0f)
        } else {
            1.0f
        }
        PlaybackManager.setMasterVolume(masterVolume)
    }

    fun togglePower(enabled: Boolean) {
        if (enabled) {
            AudioFxManager.userManuallyEnabledEqInOnline = true
        }
        AudioFxManager.setEnabled(enabled)
        if (enabled) {
            applyAntiClippingSafeguard()
        } else {
            PlaybackManager.setMasterVolume(1.0f)
            AudioFxManager.setPreampGain(baselinePreampGainMb)
            _autoAttenuationDb.value = 0f
        }
    }

    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        AudioFxManager.setBandLevel(bandIndex, levelDb)
        applyAntiClippingSafeguard()
    }

    fun setBassStrength(strength: Int) {
        AudioFxManager.setBassBoost(strength)
    }

    fun setVirtualizerStrength(strength: Int) {
        AudioFxManager.setVirtualizer(strength)
    }

    fun setPreampGain(gainMb: Int) {
        baselinePreampGainMb = gainMb.coerceIn(0, 1000)
        applyAntiClippingSafeguard()
    }

    fun selectPreset(preset: String) {
        AudioFxManager.applyPreset(preset)
        applyAntiClippingSafeguard()
    }
}

