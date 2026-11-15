package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CommanderProfile
import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.LeaderState
import com.example.data.model.ResourceType
import com.example.data.model.UpgradeCatalog
import com.example.data.repository.OfflineProgressionStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineProgressionStorageTest {

  private lateinit var context: Context
  private lateinit var storage: OfflineProgressionStorage

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.filesDir.resolve("omninauts_offline_progression.json").delete()
    storage = OfflineProgressionStorage(context)
  }

  @Test
  fun storage_initializesWithPyronautDefaultsAndPersists() = runBlocking {
    val snapshot = storage.currentSnapshot()
    assertEquals("Offline Commander", snapshot.profile.callsign)
    assertEquals(0, snapshot.profile.highestTutorialCleared)
    assertTrue(snapshot.profile.pyroEggs >= 4)

    val pyroLab = snapshot.labs[ElementType.PYRO]
    assertTrue(pyroLab != null && pyroLab.unlocked)

    val pyroLeader = snapshot.leaders[ElementType.PYRO]
    assertTrue(pyroLeader != null && pyroLeader.unlocked)
    assertEquals("Ignis Prime", pyroLeader?.name)
  }

  @Test
  fun storage_recordsOutcome_updatesProfileAndUnlocks() = runBlocking {
    val res =
      storage.recordOutcome(
        sectorId = "tut_01",
        sectorName = "Sector 01",
        seed = 101,
        biomeId = "BASALT_RIFT",
        weatherId = "CLEAR",
        rescuedCount = 1,
        totalPods = 1,
        bossDefeated = true,
        victory = true,
        rank = "S",
        durationSeconds = 45,
        tutorialSectorNumber = 1,
        collectedResources = mapOf(ResourceType.FUEL_CELLS to 5, ResourceType.SPARK_FLINT to 4),
        collectedEggs = mapOf(ElementType.PYRO to 1),
        deployedElements = setOf(ElementType.PYRO),
        xpEarned = 150,
      )

    assertTrue(res.isSuccess)
    val snap = storage.currentSnapshot()
    assertEquals(1, snap.profile.highestTutorialCleared)
    assertEquals(1, snap.profile.totalRescues)
    assertEquals(1, snap.profile.totalExpeditions)
    assertEquals(1, snap.profile.bossesDefeated)
    assertTrue(snap.profile.fuelCells >= 11)

    // Hydro should now be unlocked from clearing Tutorial 01
    val hydroLab = snap.labs[ElementType.HYDRO]
    assertTrue(hydroLab != null && hydroLab.unlocked)
    val hydroLeader = snap.leaders[ElementType.HYDRO]
    assertTrue(hydroLeader != null && hydroLeader.unlocked)
  }

  @Test
  fun storage_purchasesUpgradeAndTrainsLeader() = runBlocking {
    val node = UpgradeCatalog.upgradesFor(ElementType.PYRO).first()
    val res = storage.purchaseUpgrade(ElementType.PYRO, node)
    assertTrue(res.isSuccess)

    val snapAfterUpgrade = storage.currentSnapshot()
    assertTrue(snapAfterUpgrade.labs[ElementType.PYRO]?.unlockedUpgrades?.contains(node.id) == true)

    // Give Pyro leader XP and level up
    val leader = snapAfterUpgrade.leaders[ElementType.PYRO]!!
    storage.saveLeader(leader.copy(xp = leader.xpForNextLevel + 50))
    val trainRes = storage.trainLeader(ElementType.PYRO)
    assertTrue(trainRes.isSuccess)

    val snapAfterTrain = storage.currentSnapshot()
    assertEquals(2, snapAfterTrain.leaders[ElementType.PYRO]?.level)
  }

  @Test
  fun storage_smartMergesWithCloudData_takingHighestProgress() = runBlocking {
    val cloudProfile =
      CommanderProfile(
        id = "cloud_123",
        userId = "cloud_user",
        callsign = "Cloud Ace",
        highestTutorialCleared = 2,
        totalRescues = 5,
        totalExpeditions = 8,
        bossesDefeated = 3,
        fuelCells = 20,
        sparkFlint = 15,
        pyroEggs = 7,
      )
    val cloudLabs =
      listOf(
        ElementalLabState(
          id = "pyro",
          elementId = "pyro",
          unlocked = true,
          labTier = 2,
          unlockedUpgrades = listOf("pyro_combat_1", "pyro_util_1"),
        )
      )
    val cloudLeaders =
      listOf(
        LeaderState(
          id = "pyro",
          elementId = "pyro",
          name = "Ignis Prime",
          unlocked = true,
          level = 3,
          xp = 40,
        )
      )

    val merged =
      storage.mergeWithCloudData(
        cloudProfile = cloudProfile,
        cloudLabs = cloudLabs,
        cloudLeaders = cloudLeaders,
        cloudExpeditions = emptyList(),
      )

    assertEquals(2, merged.profile.highestTutorialCleared)
    assertEquals(20, merged.profile.fuelCells)
    assertEquals(7, merged.profile.pyroEggs)
    assertEquals(3, merged.leaders[ElementType.PYRO]?.level)
    assertTrue(merged.labs[ElementType.PYRO]?.unlockedUpgrades?.contains("pyro_combat_1") == true)
  }
}
