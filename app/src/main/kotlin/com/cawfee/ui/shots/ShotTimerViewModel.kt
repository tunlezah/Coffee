package com.cawfee.ui.shots

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject

data class ShotTimerState(
    val elapsedMs: Long = 0,
    val isRunning: Boolean = false,
    val preInfusionMs: Long? = null,
)

/** Espresso shot stopwatch. Ported from ShotTimerViewModel.swift. */
@HiltViewModel
class ShotTimerViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ShotTimerState())
    val state: StateFlow<ShotTimerState> = _state.asStateFlow()

    private var ticker: Job? = null
    private var startUptime: Long = 0
    private var accumulated: Long = 0

    fun startOrStop() {
        if (_state.value.isRunning) stop() else start()
    }

    private fun start() {
        // Monotonic clock: wall-clock (currentTimeMillis) jumps on NTP/manual changes.
        startUptime = SystemClock.elapsedRealtime()
        _state.update { it.copy(isRunning = true) }
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(50)
                // Guard on isRunning inside the atomic update: a tick racing stop()
                // must not overwrite the final elapsed value with a stale one.
                _state.update {
                    if (!it.isRunning) it
                    else it.copy(elapsedMs = accumulated + (SystemClock.elapsedRealtime() - startUptime))
                }
            }
        }
    }

    private fun stop() {
        ticker?.cancel()
        ticker = null
        accumulated += SystemClock.elapsedRealtime() - startUptime
        _state.update { it.copy(isRunning = false, elapsedMs = accumulated) }
    }

    fun markPreInfusion() {
        _state.update { it.copy(preInfusionMs = it.elapsedMs) }
    }

    fun reset() {
        ticker?.cancel()
        ticker = null
        accumulated = 0
        _state.value = ShotTimerState()
    }

    companion object {
        /** "27.4" style formatting — locale-pinned so comma-decimal locales don't vary it. */
        fun format(ms: Long): String = String.format(Locale.US, "%.1f", ms / 1000.0)
    }
}
