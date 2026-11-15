package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.LeaderState
import com.example.game.ControlMode
import com.example.game.DungeonGenerator
import com.example.game.ExpeditionEngine
import com.example.game.ExpeditionUiState
import com.example.game.SectorSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExpeditionViewModel(
  sector: SectorSpec,
  deployedElements: Set<ElementType>,
  leaderStates: Map<ElementType, LeaderState>,
  labStates: Map<ElementType, ElementalLabState>,
  initialSquadCounts: Map<ElementType, Int>,
  autoStartLoop: Boolean = true,
) : ViewModel() {

  private val engine =
    ExpeditionEngine(
      initialState =
        DungeonGenerator.generate(
          sector = sector,
          deployedElements = deployedElements,
          leaderStates = leaderStates,
          labStates = labStates,
          initialSquadCounts = initialSquadCounts,
        ),
      labStates = labStates,
    )

  private val _uiState = MutableStateFlow(engine.currentState())
  val uiState: StateFlow<ExpeditionUiState> = _uiState.asStateFlow()

  private var loopJob: Job? = null

  init {
    if (autoStartLoop) {
      startSimulationLoop()
    }
  }

  private fun startSimulationLoop() {
    loopJob?.cancel()
    loopJob =
      viewModelScope.launch {
        var lastNanos = System.nanoTime()
        while (isActive) {
          delay(16L)
          val now = System.nanoTime()
          val dt = ((now - lastNanos) / 1_000_000_000.0).toFloat()
          lastNanos = now
          engine.step(dt)
          _uiState.value = engine.currentState()
        }
      }
  }

  fun onWorldTap(wx: Float, wy: Float) {
    engine.onWorldTap(wx, wy)
    _uiState.value = engine.currentState()
  }

  fun setWhistle(active: Boolean) {
    engine.setWhistle(active)
    _uiState.value = engine.currentState()
  }

  fun setControlMode(mode: ControlMode) {
    engine.setControlMode(mode)
    _uiState.value = engine.currentState()
  }

  fun setJoystick(dx: Float, dy: Float) {
    engine.setJoystick(dx, dy)
  }

  fun switchLeader(element: ElementType) {
    engine.switchActiveLeader(element)
    _uiState.value = engine.currentState()
  }

  fun triggerStrike() {
    engine.triggerLeaderPrimaryStrike()
    _uiState.value = engine.currentState()
  }

  fun triggerSkill(slot: Int) {
    engine.triggerLeaderSkill(slot)
    _uiState.value = engine.currentState()
  }

  fun togglePause() {
    engine.setPaused(!_uiState.value.isPaused)
    _uiState.value = engine.currentState()
  }

  override fun onCleared() {
    super.onCleared()
    loopJob?.cancel()
  }
}
