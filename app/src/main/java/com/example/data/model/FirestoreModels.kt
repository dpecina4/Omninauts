package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CommanderProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val callsign: String = "Commander",
  val highestTutorialCleared: Int = 0,
  val totalRescues: Int = 0,
  val totalExpeditions: Int = 0,
  val bossesDefeated: Int = 0,
  val fuelCells: Int = 4,
  val sparkFlint: Int = 4,
  val magmaMatches: Int = 2,
  val glassJars: Int = 2,
  val dewVials: Int = 2,
  val cryoCondensers: Int = 0,
  val capacitorChips: Int = 2,
  val caveBatteries: Int = 2,
  val copperCoils: Int = 0,
  val pyroEggs: Int = 4,
  val hydroEggs: Int = 2,
  val voltEggs: Int = 2,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resourceAmount(type: ResourceType): Int =
    when (type) {
      ResourceType.FUEL_CELLS -> fuelCells
      ResourceType.SPARK_FLINT -> sparkFlint
      ResourceType.MAGMA_MATCHES -> magmaMatches
      ResourceType.GLASS_JARS -> glassJars
      ResourceType.DEW_VIALS -> dewVials
      ResourceType.CRYO_CONDENSERS -> cryoCondensers
      ResourceType.CAPACITOR_CHIPS -> capacitorChips
      ResourceType.CAVE_BATTERIES -> caveBatteries
      ResourceType.COPPER_COILS -> copperCoils
    }

  fun eggsFor(element: ElementType): Int =
    when (element) {
      ElementType.PYRO -> pyroEggs
      ElementType.HYDRO -> hydroEggs
      ElementType.VOLT -> voltEggs
    }
}

@IgnoreExtraProperties
data class ElementalLabState(
  @DocumentId val id: String = "",
  val userId: String = "",
  val elementId: String = ElementType.PYRO.id,
  val unlocked: Boolean = false,
  val labTier: Int = 1,
  val unlockedUpgrades: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LeaderState(
  @DocumentId val id: String = "",
  val userId: String = "",
  val elementId: String = ElementType.PYRO.id,
  val name: String = ElementType.PYRO.leaderName,
  val unlocked: Boolean = false,
  val level: Int = 1,
  val xp: Int = 0,
  val maxHp: Int = 180,
  val attackPower: Int = 24,
  val unlockedSkills: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val xpForNextLevel: Int
    get() = level * 120
}

@IgnoreExtraProperties
data class ExpeditionRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val sectorId: String = "",
  val sectorName: String = "",
  val seed: Int = 0,
  val biome: String = BiomeType.FUNGAL_GROTTO.id,
  val weather: String = WeatherCondition.CLEAR.id,
  val rescuedCount: Int = 0,
  val totalPods: Int = 1,
  val bossDefeated: Boolean = false,
  val victory: Boolean = false,
  val rank: String = "B",
  val durationSeconds: Int = 0,
  @ServerTimestamp val createdAt: Timestamp? = null,
)
