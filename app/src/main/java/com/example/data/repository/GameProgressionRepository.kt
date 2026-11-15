package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CommanderProfile
import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.ExpeditionRecord
import com.example.data.model.LeaderState
import com.example.data.model.ResourceType
import com.example.data.model.UpgradeCatalog
import com.example.data.model.UpgradeNodeDefinition
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GameProgressionRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
  val offlineStorage: OfflineProgressionStorage? = null,
  val isOfflineMode: Boolean = false,
) {
  constructor(
    context: Context,
    offlineStorage: OfflineProgressionStorage? = null,
    isOfflineMode: Boolean = false,
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
    offlineStorage,
    isOfflineMode,
  )

  private fun requireUserId(): String =
    if (isOfflineMode) "offline_commander"
    else auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  private fun labDoc(uid: String, element: ElementType) =
    userDoc(uid).collection("labs").document(element.id)

  private fun leaderDoc(uid: String, element: ElementType) =
    userDoc(uid).collection("leaders").document(element.id)

  private fun expeditionsCol(uid: String) = userDoc(uid).collection("expeditions")

  fun observeProfile(userId: String): Flow<CommanderProfile?> = flow {
    if (isOfflineMode) {
      val flow = offlineStorage?.profileFlow
      if (flow != null) {
        emitAll(flow)
      } else {
        emit(OfflineProgressionStorage.createDefaultProfile("Offline Commander"))
      }
      return@flow
    }
    val docRef = userDoc(userId)
    emitAll(
      docRef
        .snapshots()
        .map { snap ->
          if (snap.exists()) {
            val prof = snap.toObject(CommanderProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            if (prof != null) {
              offlineStorage?.saveProfile(prof)
            }
            prof
          } else {
            null
          }
        }
        .catch { error ->
          logFlowError(error, OperationType.GET, docRef.path, userId)
          throw error
        }
    )
  }

  fun observeLabs(userId: String): Flow<List<ElementalLabState>> = flow {
    if (isOfflineMode) {
      val flow = offlineStorage?.labsFlow
      if (flow != null) {
        emitAll(flow)
      } else {
        emit(ElementType.entries.map { OfflineProgressionStorage.createDefaultLab(it) })
      }
      return@flow
    }
    val colRef = userDoc(userId).collection("labs")
    emitAll(
      colRef
        .snapshots()
        .map { snap ->
          val list = snap.documents.mapNotNull {
            it.toObject(ElementalLabState::class.java, ServerTimestampBehavior.ESTIMATE)
          }
          list.forEach { lab -> offlineStorage?.saveLab(lab) }
          list
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  fun observeLeaders(userId: String): Flow<List<LeaderState>> = flow {
    if (isOfflineMode) {
      val flow = offlineStorage?.leadersFlow
      if (flow != null) {
        emitAll(flow)
      } else {
        emit(ElementType.entries.map { OfflineProgressionStorage.createDefaultLeader(it) })
      }
      return@flow
    }
    val colRef = userDoc(userId).collection("leaders")
    emitAll(
      colRef
        .snapshots()
        .map { snap ->
          val list = snap.documents.mapNotNull {
            it.toObject(LeaderState::class.java, ServerTimestampBehavior.ESTIMATE)
          }
          list.forEach { l -> offlineStorage?.saveLeader(l) }
          list
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, colRef.path, userId)
          throw error
        }
    )
  }

  fun observeExpeditions(userId: String): Flow<List<ExpeditionRecord>> = flow {
    if (isOfflineMode) {
      val flow = offlineStorage?.expeditionsFlow
      if (flow != null) {
        emitAll(flow)
      } else {
        emit(emptyList())
      }
      return@flow
    }
    val query = expeditionsCol(userId).orderBy("createdAt", Query.Direction.DESCENDING).limit(25)
    emitAll(
      query
        .snapshots()
        .map { snap ->
          snap.documents.mapNotNull {
            it.toObject(ExpeditionRecord::class.java, ServerTimestampBehavior.ESTIMATE)
          }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, expeditionsCol(userId).path, userId)
          throw error
        }
    )
  }

  suspend fun ensureInitializedForUser(callsignHint: String? = null): Result<Unit> {
    if (isOfflineMode) {
      return Result.success(Unit)
    }
    val uid = requireUserId()
    val profileRef = userDoc(uid)
    return try {
      val existing = profileRef.get().await()
      if (!existing.exists()) {
        val cleanCallsign =
          callsignHint?.trim()?.takeIf { it.isNotEmpty() }?.take(40) ?: "Commander"
        val batch = db.batch()
        batch.set(
          profileRef,
          mapOf(
            "userId" to uid,
            "callsign" to cleanCallsign,
            "highestTutorialCleared" to 0,
            "totalRescues" to 0,
            "totalExpeditions" to 0,
            "bossesDefeated" to 0,
            "fuelCells" to 6,
            "sparkFlint" to 6,
            "magmaMatches" to 3,
            "glassJars" to 2,
            "dewVials" to 2,
            "cryoCondensers" to 0,
            "capacitorChips" to 2,
            "caveBatteries" to 2,
            "copperCoils" to 0,
            "pyroEggs" to 5,
            "hydroEggs" to 2,
            "voltEggs" to 2,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        ElementType.entries.forEach { element ->
          val starterUnlocked = element == ElementType.PYRO
          batch.set(
            labDoc(uid, element),
            mapOf(
              "userId" to uid,
              "elementId" to element.id,
              "unlocked" to starterUnlocked,
              "labTier" to 1,
              "unlockedUpgrades" to emptyList<String>(),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
          val firstSkill = UpgradeCatalog.skillsFor(element).first().id
          batch.set(
            leaderDoc(uid, element),
            mapOf(
              "userId" to uid,
              "elementId" to element.id,
              "name" to element.leaderName,
              "unlocked" to starterUnlocked,
              "level" to 1,
              "xp" to 0,
              "maxHp" to when (element) {
                ElementType.PYRO -> 190
                ElementType.HYDRO -> 210
                ElementType.VOLT -> 170
              },
              "attackPower" to when (element) {
                ElementType.PYRO -> 26
                ElementType.HYDRO -> 22
                ElementType.VOLT -> 28
              },
              "unlockedSkills" to listOf(firstSkill),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        batch.commit().await()
      }
      if (offlineStorage != null && offlineStorage.hasExistingSave()) {
        mergeOfflineProgressIntoCloud(callsignHint)
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, profileRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun purchaseLabUpgrade(
    element: ElementType,
    node: UpgradeNodeDefinition,
  ): Result<Unit> {
    if (isOfflineMode) {
      return offlineStorage?.purchaseUpgrade(element, node)
        ?: Result.failure(IllegalStateException("Offline storage unavailable"))
    }
    val uid = requireUserId()
    val profileRef = userDoc(uid)
    val labRef = labDoc(uid, element)
    return try {
      db.runTransaction { tx ->
          val profileSnap = tx.get(profileRef)
          val labSnap = tx.get(labRef)
          val profile =
            profileSnap.toObject(CommanderProfile::class.java)
              ?: throw IllegalStateException("Commander profile not initialized")
          val lab =
            labSnap.toObject(ElementalLabState::class.java)
              ?: throw IllegalStateException("Lab state not initialized")

          require(lab.unlocked) { "${element.labName} is locked." }
          require(!lab.unlockedUpgrades.contains(node.id)) { "Upgrade already unlocked." }
          node.prerequisiteId?.let { prereq ->
            require(lab.unlockedUpgrades.contains(prereq)) {
              "Requires previous upgrade node first."
            }
          }

          val profileUpdates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
          node.costs.forEach { (resource, cost) ->
            val current = profile.resourceAmount(resource)
            require(current >= cost) {
              "Insufficient ${resource.displayName} (need $cost, have $current)."
            }
            profileUpdates[resource.key] = current - cost
          }

          val nextUpgrades = (lab.unlockedUpgrades + node.id).distinct()
          val nextTier = (1 + nextUpgrades.size / 2).coerceIn(1, 5)

          tx.update(profileRef, profileUpdates)
          tx.update(
            labRef,
            mapOf(
              "unlockedUpgrades" to nextUpgrades,
              "labTier" to nextTier,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, labRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun synthesizeReserveEgg(element: ElementType): Result<Unit> {
    if (isOfflineMode) {
      return offlineStorage?.synthesizeEgg(element)
        ?: Result.failure(IllegalStateException("Offline storage unavailable"))
    }
    val uid = requireUserId()
    val profileRef = userDoc(uid)
    return try {
      db.runTransaction { tx ->
          val profile =
            tx.get(profileRef).toObject(CommanderProfile::class.java)
              ?: throw IllegalStateException("Profile missing")

          val costMap = eggSynthesisCost(element)
          val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
          costMap.forEach { (res, cost) ->
            val have = profile.resourceAmount(res)
            require(have >= cost) { "Need $cost ${res.displayName} to synthesize egg." }
            updates[res.key] = have - cost
          }
          val eggField =
            when (element) {
              ElementType.PYRO -> "pyroEggs"
              ElementType.HYDRO -> "hydroEggs"
              ElementType.VOLT -> "voltEggs"
            }
          updates[eggField] = profile.eggsFor(element) + 1
          tx.update(profileRef, updates)
        }
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, profileRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun levelUpLeaderWithXp(element: ElementType): Result<Unit> {
    if (isOfflineMode) {
      return offlineStorage?.trainLeader(element)
        ?: Result.failure(IllegalStateException("Offline storage unavailable"))
    }
    val uid = requireUserId()
    val leaderRef = leaderDoc(uid, element)
    return try {
      db.runTransaction { tx ->
          val leader =
            tx.get(leaderRef).toObject(LeaderState::class.java)
              ?: throw IllegalStateException("Leader missing")
          require(leader.unlocked) { "${leader.name} is still locked." }
          require(leader.level < 20) { "${leader.name} is at max level." }
          val needed = leader.xpForNextLevel
          require(leader.xp >= needed) { "Need $needed XP to train ${leader.name}." }

          val nextLevel = leader.level + 1
          val newlyUnlockedSkills =
            UpgradeCatalog.skillsFor(element)
              .filter { it.unlockLevel <= nextLevel }
              .map { it.id }
          val mergedSkills = (leader.unlockedSkills + newlyUnlockedSkills).distinct()

          tx.update(
            leaderRef,
            mapOf(
              "level" to nextLevel,
              "xp" to (leader.xp - needed),
              "maxHp" to (leader.maxHp + 25),
              "attackPower" to (leader.attackPower + 5),
              "unlockedSkills" to mergedSkills,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, leaderRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun recordExpeditionOutcome(
    sectorId: String,
    sectorName: String,
    seed: Int,
    biomeId: String,
    weatherId: String,
    rescuedCount: Int,
    totalPods: Int,
    bossDefeated: Boolean,
    victory: Boolean,
    rank: String,
    durationSeconds: Int,
    tutorialSectorNumber: Int?,
    collectedResources: Map<ResourceType, Int>,
    collectedEggs: Map<ElementType, Int>,
    deployedElements: Set<ElementType>,
    xpEarned: Int,
  ): Result<String> {
    if (isOfflineMode) {
      val res =
        offlineStorage?.recordOutcome(
          sectorId = sectorId,
          sectorName = sectorName,
          seed = seed,
          biomeId = biomeId,
          weatherId = weatherId,
          rescuedCount = rescuedCount,
          totalPods = totalPods,
          bossDefeated = bossDefeated,
          victory = victory,
          rank = rank,
          durationSeconds = durationSeconds,
          tutorialSectorNumber = tutorialSectorNumber,
          collectedResources = collectedResources,
          collectedEggs = collectedEggs,
          deployedElements = deployedElements,
          xpEarned = xpEarned,
        )
      return if (res?.isSuccess == true) Result.success("offline_exp")
      else Result.failure(res?.exceptionOrNull() ?: IllegalStateException("Offline record failed"))
    }
    val uid = requireUserId()
    val profileRef = userDoc(uid)
    val expId = "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val expRef = expeditionsCol(uid).document(expId)

    return try {
      db.runTransaction { tx ->
          val profileSnap = tx.get(profileRef)
          val profile =
            profileSnap.toObject(CommanderProfile::class.java)
              ?: throw IllegalStateException("Profile missing")

          val unlockTutorialTarget: Int? =
            if (victory && tutorialSectorNumber != null) {
              when (tutorialSectorNumber) {
                1 -> 1
                2 -> 2
                3 -> 3
                else -> null
              }
            } else {
              null
            }

          val elementToUnlock: ElementType? =
            when (unlockTutorialTarget) {
              1 -> ElementType.PYRO
              2 -> ElementType.HYDRO
              3 -> ElementType.VOLT
              else -> null
            }

          val unlockedLabSnap = elementToUnlock?.let { tx.get(labDoc(uid, it)) }
          val unlockedLeaderSnap = elementToUnlock?.let { tx.get(leaderDoc(uid, it)) }

          val leaderSnaps =
            deployedElements.associateWith { el -> tx.get(leaderDoc(uid, el)) }

          // Writes begin after all reads
          tx.set(
            expRef,
            mapOf(
              "userId" to uid,
              "sectorId" to sectorId.take(64),
              "sectorName" to sectorName.take(80),
              "seed" to seed,
              "biome" to biomeId,
              "weather" to weatherId,
              "rescuedCount" to rescuedCount.coerceIn(0, 20),
              "totalPods" to totalPods.coerceIn(1, 20),
              "bossDefeated" to bossDefeated,
              "victory" to victory,
              "rank" to rank,
              "durationSeconds" to durationSeconds.coerceIn(0, 7200),
              "createdAt" to FieldValue.serverTimestamp(),
            ),
          )

          val nextTutorial =
            if (unlockTutorialTarget != null) {
              maxOf(profile.highestTutorialCleared, unlockTutorialTarget).coerceIn(0, 3)
            } else {
              profile.highestTutorialCleared
            }

          val profileUpdates = mutableMapOf<String, Any>(
            "highestTutorialCleared" to nextTutorial,
            "totalRescues" to (profile.totalRescues + rescuedCount.coerceAtLeast(0)),
            "totalExpeditions" to (profile.totalExpeditions + 1),
            "bossesDefeated" to (profile.bossesDefeated + if (bossDefeated) 1 else 0),
            "updatedAt" to FieldValue.serverTimestamp(),
          )

          ResourceType.entries.forEach { res ->
            val gain = collectedResources[res] ?: 0
            if (gain > 0) {
              profileUpdates[res.key] = profile.resourceAmount(res) + gain
            }
          }

          val pyroGain = collectedEggs[ElementType.PYRO] ?: 0
          val hydroGain = collectedEggs[ElementType.HYDRO] ?: 0
          val voltGain = collectedEggs[ElementType.VOLT] ?: 0
          if (pyroGain > 0) profileUpdates["pyroEggs"] = profile.pyroEggs + pyroGain
          if (hydroGain > 0) profileUpdates["hydroEggs"] = profile.hydroEggs + hydroGain
          if (voltGain > 0) profileUpdates["voltEggs"] = profile.voltEggs + voltGain

          tx.update(profileRef, profileUpdates)

          if (elementToUnlock != null && unlockedLabSnap != null && unlockedLeaderSnap != null) {
            if (unlockedLabSnap.exists()) {
              tx.update(
                labDoc(uid, elementToUnlock),
                mapOf("unlocked" to true, "updatedAt" to FieldValue.serverTimestamp()),
              )
            }
            if (unlockedLeaderSnap.exists()) {
              tx.update(
                leaderDoc(uid, elementToUnlock),
                mapOf("unlocked" to true, "updatedAt" to FieldValue.serverTimestamp()),
              )
            }
          }

          leaderSnaps.forEach { (el, snap) ->
            val leader = snap.toObject(LeaderState::class.java)
            if (leader != null && (leader.unlocked || el == elementToUnlock)) {
              tx.update(
                leaderDoc(uid, el),
                mapOf(
                  "xp" to (leader.xp + xpEarned.coerceAtLeast(0)),
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            }
          }
        }
        .await()
      Result.success(expId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, expRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun mergeOfflineProgressIntoCloud(callsignHint: String? = null): Result<Unit> {
    val uid = requireUserId()
    val storage = offlineStorage ?: return Result.failure(IllegalStateException("No offline storage provided"))
    val local = storage.currentSnapshot()
    val profileRef = userDoc(uid)
    return try {
      val existingProfileSnap = profileRef.get().await()
      val cloudProfile =
        if (existingProfileSnap.exists()) {
          existingProfileSnap.toObject(CommanderProfile::class.java)
        } else null

      val cloudLabs = mutableListOf<ElementalLabState>()
      val cloudLeaders = mutableListOf<LeaderState>()
      for (el in ElementType.entries) {
        val lSnap = labDoc(uid, el).get().await()
        if (lSnap.exists()) {
          lSnap.toObject(ElementalLabState::class.java)?.let { cloudLabs.add(it) }
        }
        val ldSnap = leaderDoc(uid, el).get().await()
        if (ldSnap.exists()) {
          ldSnap.toObject(LeaderState::class.java)?.let { cloudLeaders.add(it) }
        }
      }

      val mergedSnapshot =
        storage.mergeWithCloudData(
          cloudProfile = cloudProfile,
          cloudLabs = cloudLabs,
          cloudLeaders = cloudLeaders,
          cloudExpeditions = emptyList(),
        )

      val mp = mergedSnapshot.profile
      val batch = db.batch()
      val callsign = callsignHint?.trim()?.takeIf { it.isNotEmpty() }?.take(40) ?: mp.callsign
      batch.set(
        profileRef,
        mapOf(
          "userId" to uid,
          "callsign" to callsign,
          "highestTutorialCleared" to mp.highestTutorialCleared,
          "totalRescues" to mp.totalRescues,
          "totalExpeditions" to mp.totalExpeditions,
          "bossesDefeated" to mp.bossesDefeated,
          "fuelCells" to mp.fuelCells,
          "sparkFlint" to mp.sparkFlint,
          "magmaMatches" to mp.magmaMatches,
          "glassJars" to mp.glassJars,
          "dewVials" to mp.dewVials,
          "cryoCondensers" to mp.cryoCondensers,
          "capacitorChips" to mp.capacitorChips,
          "caveBatteries" to mp.caveBatteries,
          "copperCoils" to mp.copperCoils,
          "pyroEggs" to mp.pyroEggs,
          "hydroEggs" to mp.hydroEggs,
          "voltEggs" to mp.voltEggs,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      mergedSnapshot.labs.values.forEach { lab ->
        batch.set(
          labDoc(uid, ElementType.fromId(lab.elementId)),
          mapOf(
            "userId" to uid,
            "elementId" to lab.elementId,
            "unlocked" to lab.unlocked,
            "labTier" to lab.labTier,
            "unlockedUpgrades" to lab.unlockedUpgrades,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      mergedSnapshot.leaders.values.forEach { l ->
        batch.set(
          leaderDoc(uid, ElementType.fromId(l.elementId)),
          mapOf(
            "userId" to uid,
            "elementId" to l.elementId,
            "name" to l.name,
            "unlocked" to l.unlocked,
            "level" to l.level,
            "xp" to l.xp,
            "maxHp" to l.maxHp,
            "attackPower" to l.attackPower,
            "unlockedSkills" to l.unlockedSkills,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      batch.commit().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("GameProgressionRepo", "Error merging offline progress to cloud", e)
      Result.failure(e)
    }
  }

  private fun logFlowError(
    error: Throwable,
    op: OperationType,
    path: String,
    expectedUid: String,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val toLog = firestoreError ?: (error as? Exception)
    if (toLog != null && auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(toLog, op, path, auth)
    }
  }

  companion object {
    fun eggSynthesisCost(element: ElementType): Map<ResourceType, Int> =
      when (element) {
        ElementType.PYRO -> mapOf(ResourceType.SPARK_FLINT to 2, ResourceType.FUEL_CELLS to 1)
        ElementType.HYDRO -> mapOf(ResourceType.GLASS_JARS to 2, ResourceType.DEW_VIALS to 1)
        ElementType.VOLT ->
          mapOf(ResourceType.CAVE_BATTERIES to 2, ResourceType.CAPACITOR_CHIPS to 1)
      }
  }
}
