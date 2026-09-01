package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerSettingsState(
    val defaultPlaybackSpeed: Float = 1.0f,
    val gesturesEnabled: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val resumeMode: String = "ASK", // ASK, ALWAYS, NEVER
    val autoPlayNext: Boolean = true,
    val keepScreenOn: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val rememberAspectRatio: Boolean = true,
    val subtitleFontSize: Int = 18,
    val subtitleStyle: String = "WHITE_SHADOW",
    val subtitleEncoding: String = "UTF-8",
    val hardwareAcceleration: Boolean = true,
    val audioBoost: Boolean = false,
    val decoderPreference: String = "HW_PREFERRED",
    val themeMode: String = "DARK", // DARK, LIGHT, SYSTEM
    val sortOption: String = "DATE_DESC",
    val isGridView: Boolean = true
)

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pakplay_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PlayerSettingsState> = _settings.asStateFlow()

    private fun loadSettings(): PlayerSettingsState {
        return PlayerSettingsState(
            defaultPlaybackSpeed = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f),
            gesturesEnabled = prefs.getBoolean(KEY_GESTURES, true),
            doubleTapSeekSeconds = prefs.getInt(KEY_SEEK_SECONDS, 10),
            resumeMode = prefs.getString(KEY_RESUME_MODE, "ASK") ?: "ASK",
            autoPlayNext = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true),
            keepScreenAwake = prefs.getBoolean(KEY_KEEP_AWAKE, true),
            rememberAspectRatio = prefs.getBoolean(KEY_REMEMBER_ASPECT, true),
            subtitleFontSize = prefs.getInt(KEY_SUB_FONT_SIZE, 18),
            subtitleStyle = prefs.getString(KEY_SUB_STYLE, "WHITE_SHADOW") ?: "WHITE_SHADOW",
            subtitleEncoding = prefs.getString(KEY_SUB_ENCODING, "UTF-8") ?: "UTF-8",
            hardwareAcceleration = prefs.getBoolean(KEY_HW_ACCEL, true),
            audioBoost = prefs.getBoolean(KEY_AUDIO_BOOST, false),
            decoderPreference = prefs.getString(KEY_DECODER_PREF, "HW_PREFERRED") ?: "HW_PREFERRED",
            themeMode = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK",
            sortOption = prefs.getString(KEY_SORT_OPTION, "DATE_DESC") ?: "DATE_DESC",
            isGridView = prefs.getBoolean(KEY_GRID_VIEW, true)
        )
    }

    fun updateDefaultPlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()
        _settings.value = _settings.value.copy(defaultPlaybackSpeed = speed)
    }

    fun updateGesturesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURES, enabled).apply()
        _settings.value = _settings.value.copy(gesturesEnabled = enabled)
    }

    fun updateDoubleTapSeekSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_SEEK_SECONDS, seconds).apply()
        _settings.value = _settings.value.copy(doubleTapSeekSeconds = seconds)
    }

    fun updateResumeMode(mode: String) {
        prefs.edit().putString(KEY_RESUME_MODE, mode).apply()
        _settings.value = _settings.value.copy(resumeMode = mode)
    }

    fun updateAutoPlayNext(autoPlay: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY_NEXT, autoPlay).apply()
        _settings.value = _settings.value.copy(autoPlayNext = autoPlay)
    }

    fun updateKeepScreenOn(keepOn: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_AWAKE, keepOn).apply()
        _settings.value = _settings.value.copy(keepScreenOn = keepOn, keepScreenAwake = keepOn)
    }

    fun updateKeepScreenAwake(keepAwake: Boolean) = updateKeepScreenOn(keepAwake)


    fun updateRememberAspectRatio(remember: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ASPECT, remember).apply()
        _settings.value = _settings.value.copy(rememberAspectRatio = remember)
    }

    fun updateSubtitleFontSize(size: Int) {
        prefs.edit().putInt(KEY_SUB_FONT_SIZE, size).apply()
        _settings.value = _settings.value.copy(subtitleFontSize = size)
    }

    fun updateSubtitleStyle(style: String) {
        prefs.edit().putString(KEY_SUB_STYLE, style).apply()
        _settings.value = _settings.value.copy(subtitleStyle = style)
    }

    fun updateSubtitleEncoding(encoding: String) {
        prefs.edit().putString(KEY_SUB_ENCODING, encoding).apply()
        _settings.value = _settings.value.copy(subtitleEncoding = encoding)
    }

    fun updateHardwareAcceleration(hw: Boolean) {
        prefs.edit().putBoolean(KEY_HW_ACCEL, hw).apply()
        _settings.value = _settings.value.copy(hardwareAcceleration = hw)
    }

    fun updateAudioBoost(boost: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_BOOST, boost).apply()
        _settings.value = _settings.value.copy(audioBoost = boost)
    }

    fun updateDecoderPreference(pref: String) {
        prefs.edit().putString(KEY_DECODER_PREF, pref).apply()
        _settings.value = _settings.value.copy(decoderPreference = pref)
    }

    fun updateThemeMode(theme: String) {
        prefs.edit().putString(KEY_THEME_MODE, theme).apply()
        _settings.value = _settings.value.copy(themeMode = theme)
    }

    fun updateSortOption(sort: String) {
        prefs.edit().putString(KEY_SORT_OPTION, sort).apply()
        _settings.value = _settings.value.copy(sortOption = sort)
    }

    fun updateGridView(isGrid: Boolean) {
        prefs.edit().putBoolean(KEY_GRID_VIEW, isGrid).apply()
        _settings.value = _settings.value.copy(isGridView = isGrid)
    }

    companion object {
        private const val KEY_PLAYBACK_SPEED = "pref_playback_speed"
        private const val KEY_GESTURES = "pref_gestures_enabled"
        private const val KEY_SEEK_SECONDS = "pref_seek_seconds"
        private const val KEY_RESUME_MODE = "pref_resume_mode"
        private const val KEY_AUTO_PLAY_NEXT = "pref_auto_play_next"
        private const val KEY_KEEP_AWAKE = "pref_keep_awake"
        private const val KEY_REMEMBER_ASPECT = "pref_remember_aspect"
        private const val KEY_SUB_FONT_SIZE = "pref_sub_font_size"
        private const val KEY_SUB_STYLE = "pref_sub_style"
        private const val KEY_SUB_ENCODING = "pref_sub_encoding"
        private const val KEY_HW_ACCEL = "pref_hw_accel"
        private const val KEY_AUDIO_BOOST = "pref_audio_boost"
        private const val KEY_DECODER_PREF = "pref_decoder"
        private const val KEY_THEME_MODE = "pref_theme"
        private const val KEY_SORT_OPTION = "pref_sort"
        private const val KEY_GRID_VIEW = "pref_grid"
    }
}
