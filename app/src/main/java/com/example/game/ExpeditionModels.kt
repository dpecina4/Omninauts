package com.example.game

import com.example.data.model.BiomeType
import com.example.data.model.ElementType
import com.example.data.model.ResourceType
import com.example.data.model.WeatherCondition

enum class TileType(val walkableByDefault: Boolean) {
  WALL(false),
  FLOOR(true),
  START_PAD(true),
  EXIT_BEACON(true),
  OBSTACLE_CRYO_BARRIER(false),
  OBSTACLE_SPORE_WEB(false),
  OBSTACLE_FIRE_JET(false),
  OBSTACLE_BRINE_CHANNEL(false),
  OBSTACLE_CIRCUIT_LOCK(false),
  OBSTACLE_SHIELD_PYLON(false);

  fun requiredElement(): ElementType? =
    when (this) {
      OBSTACLE_CRYO_BARRIER, OBSTACLE_SPORE_WEB -> ElementType.PYRO
      OBSTACLE_FIRE_JET, OBSTACLE_BRINE_CHANNEL -> ElementType.HYDRO
      OBSTACLE_CIRCUIT_LOCK, OBSTACLE_SHIELD_PYLON -> ElementType.VOLT
      else -> null
    }

  fun obstacleLabel(): String =
    when (this) {
      OBSTACLE_CRYO_BARRIER -> "Cryo Barrier"
      OBSTACLE_SPORE_WEB -> "Spore Web"
      OBSTACLE_FIRE_JET -> "Plasma Fire Jet"
      OBSTACLE_BRINE_CHANNEL -> "Brine Trench"
      OBSTACLE_CIRCUIT_LOCK -> "Circuit Lock"
      OBSTACLE_SHIELD_PYLON -> "Shield Pylon"
      else -> ""
    }
}

data class ObstacleEntity(
  val id: String,
  val tileX: Int,
  val tileY: Int,
  val tileType: TileType,
  val requiredElement: ElementType,
  val hp: Float,
  val maxHp: Float,
)

enum class SoldierState {
  FOLLOWING,
  MOVING_TO_POINT,
  CLEARING_OBSTACLE,
  FIGHTING,
  HARVESTING,
  HATCHING,
  IDLE,
}

data class SoldierUnit(
  val id: String,
  val element: ElementType,
  val x: Float,
  val y: Float,
  val vx: Float = 0f,
  val vy: Float = 0f,
  val hp: Float,
  val maxHp: Float,
  val attackPower: Float,
  val moveSpeed: Float,
  val clearSpeedMult: Float,
  val weatherResist: Float,
  val hasBonusCombatEffect: Boolean,
  val state: SoldierState = SoldierState.FOLLOWING,
  val targetX: Float = x,
  val targetY: Float = y,
  val targetId: String? = null,
  val attackCooldown: Float = 0f,
)

data class LeaderUnit(
  val element: ElementType,
  val name: String,
  val level: Int,
  val x: Float,
  val y: Float,
  val vx: Float = 0f,
  val vy: Float = 0f,
  val hp: Float,
  val maxHp: Float,
  val attackPower: Float,
  val primarySkillId: String,
  val secondarySkillId: String?,
  val primaryCooldown: Float = 0f,
  val secondaryCooldown: Float = 0f,
  val attackCooldown: Float = 0f,
  val facingAngle: Float = 0f,
)

data class FieldEgg(
  val id: String,
  val element: ElementType,
  val x: Float,
  val y: Float,
  val progress: Float = 0f,
  val requiredProgress: Float = 4.5f,
  val hatched: Boolean = false,
)

data class ResourcePickupNode(
  val id: String,
  val resourceType: ResourceType,
  val x: Float,
  val y: Float,
  val amountLeft: Int,
  val harvestProgress: Float = 0f,
)

data class RescuePod(
  val id: String,
  val callsign: String,
  val x: Float,
  val y: Float,
  val rescueProgress: Float = 0f,
  val rescued: Boolean = false,
)

enum class EnemyKind(val title: String, val isBoss: Boolean) {
  SPORE_CRAWLER("Spore Crawler", false),
  CRYO_BEETLE("Cryo Beetle", false),
  ARC_SENTINEL("Arc Sentinel", false),
  CAVERN_BOSS("Abyssal Leviathan", true),
}

data class EnemyEntity(
  val id: String,
  val kind: EnemyKind,
  val title: String,
  val x: Float,
  val y: Float,
  val hp: Float,
  val maxHp: Float,
  val attackDamage: Float,
  val radius: Float,
  val attackCooldown: Float = 0f,
  val telegraphTimer: Float = 0f,
  val telegraphMax: Float = 1.6f,
  val telegraphRadius: Float = 2.1f,
  val stunTimer: Float = 0f,
  val hitFlash: Float = 0f,
)

data class VisualEffect(
  val id: String,
  val kind: EffectKind,
  val x: Float,
  val y: Float,
  val radius: Float,
  val element: ElementType? = null,
  val life: Float,
  val maxLife: Float,
  val label: String = "",
)

enum class EffectKind {
  WHISTLE_RING,
  ABILITY_BLAST,
  BOSS_SLAM,
  HATCH_BURST,
  FLOATING_TEXT,
  WAYPOINT_PING,
}

enum class ControlMode {
  COMMANDER_SQUAD,
  LEADER_DIRECT,
}

data class SectorSpec(
  val id: String,
  val name: String,
  val subtitle: String,
  val seed: Int,
  val biome: BiomeType,
  val weather: WeatherCondition,
  val tutorialNumber: Int?,
  val timeLimitSeconds: Int,
  val hasBoss: Boolean,
  val hintBanner: String,
)

data class ExpeditionOutcome(
  val sector: SectorSpec,
  val victory: Boolean,
  val rescuedCount: Int,
  val totalPods: Int,
  val bossDefeated: Boolean,
  val durationSeconds: Int,
  val collectedResources: Map<ResourceType, Int>,
  val collectedEggs: Map<ElementType, Int>,
  val deployedElements: Set<ElementType>,
  val xpEarned: Int,
  val rank: String,
)

data class ExpeditionUiState(
  val sector: SectorSpec,
  val gridWidth: Int,
  val gridHeight: Int,
  val tiles: List<TileType>,
  val obstacles: List<ObstacleEntity>,
  val leaders: List<LeaderUnit>,
  val activeLeaderElement: ElementType,
  val soldiers: List<SoldierUnit>,
  val eggs: List<FieldEgg>,
  val resourceNodes: List<ResourcePickupNode>,
  val rescuePods: List<RescuePod>,
  val enemies: List<EnemyEntity>,
  val effects: List<VisualEffect>,
  val controlMode: ControlMode = ControlMode.COMMANDER_SQUAD,
  val whistleActive: Boolean = false,
  val whistleRadius: Float = 0f,
  val timeRemaining: Float,
  val elapsedSeconds: Float = 0f,
  val collectedResources: Map<ResourceType, Int> = emptyMap(),
  val collectedEggs: Map<ElementType, Int> = emptyMap(),
  val statusBanner: String = "",
  val exitUnlocked: Boolean = false,
  val startX: Float,
  val startY: Float,
  val exitX: Float,
  val exitY: Float,
  val isPaused: Boolean = false,
  val outcome: ExpeditionOutcome? = null,
  val frameTick: Long = 0L,
) {
  val activeLeader: LeaderUnit?
    get() = leaders.firstOrNull { it.element == activeLeaderElement } ?: leaders.firstOrNull()

  fun tileAt(tx: Int, ty: Int): TileType {
    if (tx < 0 || ty < 0 || tx >= gridWidth || ty >= gridHeight) return TileType.WALL
    return tiles[ty * gridWidth + tx]
  }
}
