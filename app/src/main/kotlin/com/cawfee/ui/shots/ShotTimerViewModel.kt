package com.cawfee.ui.shots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
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
        startUptime = System.currentTimeMillis()
        _state.update { it.copy(isRunning = true) }
        ticker = viewModelScope.launch {
            while (true) {
                delay(50)
                val now = System.currentTimeMillis()
                _state.update { it.copy(elapsedMs = accumulated + (now - startUptime)) }
            }
        }
    }

    private fun stop() {
        ticker?.cancel()
        accumulated += System.currentTimeMillis() - startUptime
        _state.update { it.copy(isRunning = false, elapsedMs = accumulated) }
    }

    fun markPreInfusion() {
        _state.update { it.copy(preInfusionMs = it.elapsedMs) }
    }

    fun reset() {
        ticker?.cancel()
        accumulated = 0
        _state.value = ShotTimerState()
    }

    companion object {
        /** "27.4" style formatting. */
        fun format(ms: Long): String = "%.1f".format(ms / 1000.0)
    }
}
