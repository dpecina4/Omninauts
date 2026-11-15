package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BiomeType
import com.example.data.model.ElementType
import com.example.data.model.ResourceType
import com.example.data.model.UpgradeCatalog
import com.example.data.model.WeatherCondition
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameProgressionRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun initializeAndObserveProfileLabsLeaders_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser("alice_init_${System.currentTimeMillis()}@test.com")
    val repo = GameProgressionRepository(firestore, auth)

    val initResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureInitializedForUser("Commander A") }
    assertTrue("Init failed: ${initResult.exceptionOrNull()}", initResult.isSuccess)

    val profile = withTimeout(FLOW_TIMEOUT_MS) { repo.observeProfile(uid).first { it != null } }
    assertNotNull(profile)
    assertEquals(uid, profile?.userId)

    val labs = withTimeout(FLOW_TIMEOUT_MS) { repo.observeLabs(uid).first { it.size == 3 } }
    assertTrue(labs.first { it.elementId == ElementType.PYRO.id }.unlocked)

    val leaders = withTimeout(FLOW_TIMEOUT_MS) { repo.observeLeaders(uid).first { it.size == 3 } }
    assertEquals(3, leaders.size)
  }

  @Test
  fun purchaseUpgradeAndRecordExpedition_validTransactions_persistToFirestore() = runBlocking {
    val uid = signInTestUser("alice_upgrade_${System.currentTimeMillis()}@test.com")
    val repo = GameProgressionRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureInitializedForUser("Commander A").getOrThrow() }

    val pyroNode = UpgradeCatalog.upgradesFor(ElementType.PYRO).first { it.id == "pyro_combat_1" }
    val upgradeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.purchaseLabUpgrade(ElementType.PYRO, pyroNode)
    }
    assertTrue("Upgrade failed: ${upgradeResult.exceptionOrNull()}", upgradeResult.isSuccess)

    val expResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.recordExpeditionOutcome(
        sectorId = "tut_02",
        sectorName = "Flooded Cistern",
        seed = 202,
        biomeId = BiomeType.CRYSTAL_CISTERN.id,
        weatherId = WeatherCondition.BRINE_RAIN.id,
        rescuedCount = 2,
        totalPods = 2,
        bossDefeated = true,
        victory = true,
        rank = "S",
        durationSeconds = 110,
        tutorialSectorNumber = 2,
        collectedResources = mapOf(ResourceType.GLASS_JARS to 4, ResourceType.DEW_VIALS to 3),
        collectedEggs = mapOf(ElementType.HYDRO to 2),
        deployedElements = setOf(ElementType.PYRO, ElementType.HYDRO),
        xpEarned = 140,
      )
    }
    assertTrue("Record expedition failed: ${expResult.exceptionOrNull()}", expResult.isSuccess)

    val hydroLab = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeLabs(uid).first { list -> list.any { it.elementId == "hydro" && it.unlocked } }
    }
    assertTrue(hydroLab.any { it.elementId == "hydro" && it.unlocked })
  }

  @Test
  fun crossUserAccess_readingAnotherCommanderProfile_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_cross_${System.currentTimeMillis()}@test.com")
    val aliceRepo = GameProgressionRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.ensureInitializedForUser("Alice").getOrThrow() }

    signInTestUser("bob_cross_${System.currentTimeMillis()}@test.com")
    val bobRepo = GameProgressionRepository(firestore, auth)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeLabs(aliceUid).first() }
    } catch (e: Throwable) {
      caught = e
    }
    val fsErr = caught?.unwrapFirestoreException()
    assertNotNull("Expected FirebaseFirestoreException, got $caught", fsErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
  }

  @Test
  fun observeProfile_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = GameProgressionRepository(firestore, auth)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeProfile("some_user_123").first() }
    } catch (e: Throwable) {
      caught = e
    }
    val fsErr = caught?.unwrapFirestoreException()
    assertNotNull("Expected FirebaseFirestoreException, got $caught", fsErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
