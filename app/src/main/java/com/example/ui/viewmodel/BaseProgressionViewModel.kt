package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CommanderProfile
import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.ExpeditionRecord
import com.example.data.model.LeaderState
import com.example.data.model.UpgradeNodeDefinition
import com.example.data.repository.GameProgressionRepository
import com.example.game.ExpeditionOutcome
import com.example.game.SectorCatalog
import com.example.game.SectorSpec
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BaseUiState {
  data object Loading : BaseUiState

  data class Ready(
    val profile: CommanderProfile,
    val labs: Map<ElementType, ElementalLabState>,
    val leaders: Map<ElementType, LeaderState>,
    val recentExpeditions: List<ExpeditionRecord>,
  ) : BaseUiState

  data class Error(val message: String) : BaseUiState
}

class BaseProgressionViewModel(
  private val repository: GameProgressionRepository,
  private val currentUserId: String,
  callsignHint: String?,
) : ViewModel() {

  private val _actionBanner = MutableStateFlow<String?>(null)
  val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

  private val _proceduralSeed = MutableStateFlow(4096)
  val proceduralSeed: StateFlow<Int> = _proceduralSeed.asStateFlow()

  private val _proceduralSector = MutableStateFlow(SectorCatalog.generateProceduralSector(4096))
  val proceduralSector: StateFlow<SectorSpec> = _proceduralSector.asStateFlow()

  init {
    viewModelScope.launch {
      val result = repository.ensureInitializedForUser(callsignHint)
      if (result.isFailure) {
        _actionBanner.value =
          result.exceptionOrNull()?.localizedMessage ?: "Failed to initialize commander profile."
      }
    }
  }

  val uiState: StateFlow<BaseUiState> =
    combine(
        repository.observeProfile(currentUserId),
        repository.observeLabs(currentUserId),
        repository.observeLeaders(currentUserId),
        repository.observeExpeditions(currentUserId),
      ) { profile, labs, leaders, expeditions ->
        if (profile == null) {
          BaseUiState.Loading
        } else {
          BaseUiState.Ready(
            profile = profile,
            labs = labs.associateBy { ElementType.fromId(it.elementId) },
            leaders = leaders.associateBy { ElementType.fromId(it.elementId) },
            recentExpeditions = expeditions,
          )
        }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing base progression", rootError)
        emit(BaseUiState.Error(rootError.localizedMessage ?: "Failed to sync cloud progression."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = BaseUiState.Loading,
      )

  fun clearBanner() {
    _actionBanner.value = null
  }

  fun rerollProceduralSector() {
    val nextSeed = ((_proceduralSeed.value * 1103515245 + 12345) and 0x7FFFFFFF) % 90000 + 1000
    _proceduralSeed.value = nextSeed
    _proceduralSector.value = SectorCatalog.generateProceduralSector(nextSeed)
  }

  fun setCustomSeed(seedInput: Int) {
    _proceduralSeed.value = seedInput
    _proceduralSector.value = SectorCatalog.generateProceduralSector(seedInput)
  }

  fun purchaseUpgrade(element: ElementType, node: UpgradeNodeDefinition) {
    viewModelScope.launch {
      val result = repository.purchaseLabUpgrade(element, node)
      _actionBanner.value =
        if (result.isSuccess) {
          "Unlocked ${node.title} in ${element.labName}!"
        } else {
          result.exceptionOrNull()?.localizedMessage ?: "Could not unlock upgrade."
        }
    }
  }

  fun synthesizeEgg(element: ElementType) {
    viewModelScope.launch {
      val result = repository.synthesizeReserveEgg(element)
      _actionBanner.value =
        if (result.isSuccess) {
          "Synthesized +1 ${element.displayName} Reserve Egg!"
        } else {
          result.exceptionOrNull()?.localizedMessage ?: "Insufficient materials for egg synthesis."
        }
    }
  }

  fun trainLeader(element: ElementType) {
    viewModelScope.launch {
      val result = repository.levelUpLeaderWithXp(element)
      _actionBanner.value =
        if (result.isSuccess) {
          "${element.leaderName} leveled up!"
        } else {
          result.exceptionOrNull()?.localizedMessage ?: "Not enough XP to level up."
        }
    }
  }

  fun commitExpeditionOutcome(outcome: ExpeditionOutcome, onComplete: () -> Unit) {
    viewModelScope.launch {
      val result =
        repository.recordExpeditionOutcome(
          sectorId = outcome.sector.id,
          sectorName = outcome.sector.name,
          seed = outcome.sector.seed,
          biomeId = outcome.sector.biome.id,
          weatherId = outcome.sector.weather.id,
          rescuedCount = outcome.rescuedCount,
          totalPods = outcome.totalPods,
          bossDefeated = outcome.bossDefeated,
          victory = outcome.victory,
          rank = outcome.rank,
          durationSeconds = outcome.durationSeconds,
          tutorialSectorNumber = outcome.sector.tutorialNumber,
          collectedResources = outcome.collectedResources,
          collectedEggs = outcome.collectedEggs,
          deployedElements = outcome.deployedElements,
          xpEarned = outcome.xpEarned,
        )
      _actionBanner.value =
        if (result.isSuccess) {
          if (outcome.victory && outcome.sector.tutorialNumber != null) {
            "Sector cleared! Unlocked ${ElementType.entries[outcome.sector.tutorialNumber - 1].labName}!"
          } else {
            "Expedition log synced to cloud."
          }
        } else {
          result.exceptionOrNull()?.localizedMessage ?: "Failed to sync expedition outcome."
        }
      onComplete()
    }
  }

  private companion object {
    const val TAG = "BaseProgressionVM"
  }
}
