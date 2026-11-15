package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.CommanderProfile
import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.ExpeditionRecord
import com.example.data.model.LeaderState
import com.example.data.model.ResourceType
import com.example.data.model.UpgradeCatalog
import com.example.data.model.UpgradeNodeDefinition
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

data class OfflineSnapshot(
  val profile: CommanderProfile,
  val labs: Map<ElementType, ElementalLabState>,
  val leaders: Map<ElementType, LeaderState>,
  val expeditions: List<ExpeditionRecord>,
)

class OfflineProgressionStorage(private val context: Context) {

  private val saveFile: File
    get() = File(context.filesDir, "omninauts_offline_progression.json")

  private val mutex = Mutex()
  private val scope = CoroutineScope(Dispatchers.IO)

  private val _profileFlow = MutableStateFlow<CommanderProfile?>(null)
  val profileFlow: StateFlow<CommanderProfile?> = _profileFlow.asStateFlow()

  private val _labsFlow = MutableStateFlow<List<ElementalLabState>>(emptyList())
  val labsFlow: StateFlow<List<ElementalLabState>> = _labsFlow.asStateFlow()

  private val _leadersFlow = MutableStateFlow<List<LeaderState>>(emptyList())
  val leadersFlow: StateFlow<List<LeaderState>> = _leadersFlow.asStateFlow()

  private val _expeditionsFlow = MutableStateFlow<List<ExpeditionRecord>>(emptyList())
  val expeditionsFlow: StateFlow<List<ExpeditionRecord>> = _expeditionsFlow.asStateFlow()

  init {
    loadOrInitialize()
  }

  fun hasExistingSave(): Boolean {
    return saveFile.exists() && saveFile.length() > 10L
  }

  private fun loadOrInitialize() {
    try {
      if (saveFile.exists()) {
        val jsonStr = saveFile.readText()
        val snapshot = parseSnapshot(jsonStr)
        if (snapshot != null) {
          emitSnapshot(snapshot)
          return
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to load offline save, initializing fresh", e)
    }

    val fresh = createDefaultSnapshot("Offline Commander")
    emitSnapshot(fresh)
    scope.launch { persistSnapshot(fresh) }
  }

  fun currentSnapshot(): OfflineSnapshot {
    val p = _profileFlow.value ?: createDefaultProfile("Offline Commander")
    val lb = _labsFlow.value.associateBy { ElementType.fromId(it.elementId) }
    val ld = _leadersFlow.value.associateBy { ElementType.fromId(it.elementId) }
    return OfflineSnapshot(
      profile = p,
      labs = lb,
      leaders = ld,
      expeditions = _expeditionsFlow.value,
    )
  }

  private fun emitSnapshot(snapshot: OfflineSnapshot) {
    _profileFlow.value = snapshot.profile
    _labsFlow.value = snapshot.labs.values.toList()
    _leadersFlow.value = snapshot.leaders.values.toList()
    _expeditionsFlow.value = snapshot.expeditions
  }

  suspend fun saveProfile(profile: CommanderProfile) {
    mutex.withLock {
      val snap = currentSnapshot().copy(profile = profile)
      emitSnapshot(snap)
      persistSnapshot(snap)
    }
  }

  suspend fun saveLab(lab: ElementalLabState) {
    mutex.withLock {
      val nextLabs = currentSnapshot().labs.toMutableMap()
      nextLabs[ElementType.fromId(lab.elementId)] = lab
      val snap = currentSnapshot().copy(labs = nextLabs)
      emitSnapshot(snap)
      persistSnapshot(snap)
    }
  }

  suspend fun saveLeader(leader: LeaderState) {
    mutex.withLock {
      val nextLeaders = currentSnapshot().leaders.toMutableMap()
      nextLeaders[ElementType.fromId(leader.elementId)] = leader
      val snap = currentSnapshot().copy(leaders = nextLeaders)
      emitSnapshot(snap)
      persistSnapshot(snap)
    }
  }

  suspend fun recordExpedition(record: ExpeditionRecord) {
    mutex.withLock {
      val nextExp = (listOf(record) + currentSnapshot().expeditions).take(30)
      val snap = currentSnapshot().copy(expeditions = nextExp)
      emitSnapshot(snap)
      persistSnapshot(snap)
    }
  }

  suspend fun purchaseUpgrade(element: ElementType, node: UpgradeNodeDefinition): Result<Unit> {
    return mutex.withLock {
      val snapshot = currentSnapshot()
      val profile = snapshot.profile
      val lab = snapshot.labs[element] ?: return@withLock Result.failure(IllegalStateException("${element.labName} not found"))

      if (!lab.unlocked) {
        return@withLock Result.failure(IllegalStateException("${element.labName} is locked."))
      }
      if (lab.unlockedUpgrades.contains(node.id)) {
        return@withLock Result.failure(IllegalStateException("Upgrade already unlocked."))
      }
      node.prerequisiteId?.let { prereq ->
        if (!lab.unlockedUpgrades.contains(prereq)) {
          return@withLock Result.failure(IllegalStateException("Requires previous upgrade node first."))
        }
      }

      val profileMap = profileToResourceMap(profile).toMutableMap()
      for ((res, cost) in node.costs) {
        val curr = profileMap[res] ?: 0
        if (curr < cost) {
          return@withLock Result.failure(IllegalStateException("Insufficient ${res.displayName} (need $cost, have $curr)."))
        }
        profileMap[res] = curr - cost
      }

      val nextProfile = applyResourceMapToProfile(profile, profileMap)
      val nextUpgrades = (lab.unlockedUpgrades + node.id).distinct()
      val nextTier = (1 + nextUpgrades.size / 2).coerceIn(1, 5)
      val nextLab = lab.copy(unlockedUpgrades = nextUpgrades, labTier = nextTier)

      val nextLabs = snapshot.labs.toMutableMap().apply { put(element, nextLab) }
      val nextSnap = snapshot.copy(profile = nextProfile, labs = nextLabs)
      emitSnapshot(nextSnap)
      persistSnapshot(nextSnap)
      Result.success(Unit)
    }
  }

  suspend fun synthesizeEgg(element: ElementType): Result<Unit> {
    return mutex.withLock {
      val snapshot = currentSnapshot()
      val profile = snapshot.profile
      val cost = GameProgressionRepository.eggSynthesisCost(element)
      val profileMap = profileToResourceMap(profile).toMutableMap()

      for ((res, amt) in cost) {
        val curr = profileMap[res] ?: 0
        if (curr < amt) {
          return@withLock Result.failure(IllegalStateException("Insufficient ${res.displayName} for ${element.displayName} egg synthesis."))
        }
        profileMap[res] = curr - amt
      }

      val updated = applyResourceMapToProfile(profile, profileMap)
      val nextProfile =
        when (element) {
          ElementType.PYRO -> updated.copy(pyroEggs = (updated.pyroEggs + 1).coerceAtMost(10))
          ElementType.HYDRO -> updated.copy(hydroEggs = (updated.hydroEggs + 1).coerceAtMost(10))
          ElementType.VOLT -> updated.copy(voltEggs = (updated.voltEggs + 1).coerceAtMost(10))
        }

      val nextSnap = snapshot.copy(profile = nextProfile)
      emitSnapshot(nextSnap)
      persistSnapshot(nextSnap)
      Result.success(Unit)
    }
  }

  suspend fun trainLeader(element: ElementType): Result<Unit> {
    return mutex.withLock {
      val snapshot = currentSnapshot()
      val leader = snapshot.leaders[element] ?: return@withLock Result.failure(IllegalStateException("Leader not found"))
      if (!leader.unlocked) return@withLock Result.failure(IllegalStateException("Leader is locked"))
      val needed = leader.xpForNextLevel
      if (leader.xp < needed) {
        return@withLock Result.failure(IllegalStateException("Requires $needed XP to level up (current: ${leader.xp})"))
      }
      if (leader.level >= 20) {
        return@withLock Result.failure(IllegalStateException("${leader.name} has reached maximum rank."))
      }

      val nextLevel = leader.level + 1
      val remainingXp = leader.xp - needed
      val skills = leader.unlockedSkills.toMutableList()
      val unlockable = UpgradeCatalog.skillsFor(element).firstOrNull { it.unlockLevel <= nextLevel && !skills.contains(it.id) }
      if (unlockable != null) {
        skills.add(unlockable.id)
      }

      val nextLeader = leader.copy(
        level = nextLevel,
        xp = remainingXp,
        maxHp = leader.maxHp + 24,
        attackPower = leader.attackPower + 4,
        unlockedSkills = skills,
      )

      val nextLeaders = snapshot.leaders.toMutableMap().apply { put(element, nextLeader) }
      val nextSnap = snapshot.copy(leaders = nextLeaders)
      emitSnapshot(nextSnap)
      persistSnapshot(nextSnap)
      Result.success(Unit)
    }
  }

  suspend fun recordOutcome(
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
  ): Result<Unit> {
    return mutex.withLock {
      val snapshot = currentSnapshot()
      val p = snapshot.profile

      val profileMap = profileToResourceMap(p).toMutableMap()
      collectedResources.forEach { (res, amt) ->
        profileMap[res] = (profileMap[res] ?: 0) + amt
      }
      var updatedProfile = applyResourceMapToProfile(p, profileMap)

      collectedEggs.forEach { (el, amt) ->
        updatedProfile = when (el) {
          ElementType.PYRO -> updatedProfile.copy(pyroEggs = (updatedProfile.pyroEggs + amt).coerceAtMost(10))
          ElementType.HYDRO -> updatedProfile.copy(hydroEggs = (updatedProfile.hydroEggs + amt).coerceAtMost(10))
          ElementType.VOLT -> updatedProfile.copy(voltEggs = (updatedProfile.voltEggs + amt).coerceAtMost(10))
        }
      }

      val nextTut = if (victory && tutorialSectorNumber != null && tutorialSectorNumber > p.highestTutorialCleared) {
        tutorialSectorNumber
      } else {
        p.highestTutorialCleared
      }

      updatedProfile = updatedProfile.copy(
        highestTutorialCleared = nextTut,
        totalRescues = p.totalRescues + rescuedCount,
        totalExpeditions = p.totalExpeditions + 1,
        bossesDefeated = p.bossesDefeated + (if (bossDefeated) 1 else 0),
      )

      // Unlock labs & leaders if tutorial cleared
      val nextLabs = snapshot.labs.toMutableMap()
      val nextLeaders = snapshot.leaders.toMutableMap()

      if (victory && tutorialSectorNumber != null) {
        when (tutorialSectorNumber) {
          1 -> {
            nextLabs[ElementType.PYRO] = (nextLabs[ElementType.PYRO] ?: createDefaultLab(ElementType.PYRO)).copy(unlocked = true)
            nextLeaders[ElementType.PYRO] = (nextLeaders[ElementType.PYRO] ?: createDefaultLeader(ElementType.PYRO)).copy(unlocked = true)
            // Preview/unlock Hydro
            nextLabs[ElementType.HYDRO] = (nextLabs[ElementType.HYDRO] ?: createDefaultLab(ElementType.HYDRO)).copy(unlocked = true)
            nextLeaders[ElementType.HYDRO] = (nextLeaders[ElementType.HYDRO] ?: createDefaultLeader(ElementType.HYDRO)).copy(unlocked = true)
          }
          2 -> {
            nextLabs[ElementType.HYDRO] = (nextLabs[ElementType.HYDRO] ?: createDefaultLab(ElementType.HYDRO)).copy(unlocked = true)
            nextLeaders[ElementType.HYDRO] = (nextLeaders[ElementType.HYDRO] ?: createDefaultLeader(ElementType.HYDRO)).copy(unlocked = true)
            // Preview/unlock Volt
            nextLabs[ElementType.VOLT] = (nextLabs[ElementType.VOLT] ?: createDefaultLab(ElementType.VOLT)).copy(unlocked = true)
            nextLeaders[ElementType.VOLT] = (nextLeaders[ElementType.VOLT] ?: createDefaultLeader(ElementType.VOLT)).copy(unlocked = true)
          }
          3 -> {
            nextLabs[ElementType.VOLT] = (nextLabs[ElementType.VOLT] ?: createDefaultLab(ElementType.VOLT)).copy(unlocked = true)
            nextLeaders[ElementType.VOLT] = (nextLeaders[ElementType.VOLT] ?: createDefaultLeader(ElementType.VOLT)).copy(unlocked = true)
          }
        }
      }

      // Add XP to deployed leaders
      deployedElements.forEach { el ->
        val l = nextLeaders[el]
        if (l != null && l.unlocked) {
          nextLeaders[el] = l.copy(xp = l.xp + xpEarned)
        }
      }

      val record = ExpeditionRecord(
        id = UUID.randomUUID().toString(),
        userId = p.userId,
        sectorId = sectorId,
        sectorName = sectorName,
        seed = seed,
        biome = biomeId,
        weather = weatherId,
        rescuedCount = rescuedCount,
        totalPods = totalPods,
        bossDefeated = bossDefeated,
        victory = victory,
        rank = rank,
        durationSeconds = durationSeconds,
      )

      val nextExpeditions = (listOf(record) + snapshot.expeditions).take(30)
      val nextSnap = OfflineSnapshot(
        profile = updatedProfile,
        labs = nextLabs,
        leaders = nextLeaders,
        expeditions = nextExpeditions,
      )
      emitSnapshot(nextSnap)
      persistSnapshot(nextSnap)
      Result.success(Unit)
    }
  }

  suspend fun mergeWithCloudData(
    cloudProfile: CommanderProfile?,
    cloudLabs: List<ElementalLabState>,
    cloudLeaders: List<LeaderState>,
    cloudExpeditions: List<ExpeditionRecord>,
  ): OfflineSnapshot {
    return mutex.withLock {
      val local = currentSnapshot()
      val merged = mergeSnapshots(local, cloudProfile, cloudLabs, cloudLeaders, cloudExpeditions)
      emitSnapshot(merged)
      persistSnapshot(merged)
      merged
    }
  }

  private fun mergeSnapshots(
    local: OfflineSnapshot,
    cloudProfile: CommanderProfile?,
    cloudLabs: List<ElementalLabState>,
    cloudLeaders: List<LeaderState>,
    cloudExpeditions: List<ExpeditionRecord>,
  ): OfflineSnapshot {
    if (cloudProfile == null) return local

    val lp = local.profile
    val cp = cloudProfile

    val mergedProfile = CommanderProfile(
      id = cp.id.ifEmpty { lp.id },
      userId = cp.userId.ifEmpty { lp.userId },
      callsign = if (cp.callsign != "Commander" && cp.callsign.isNotBlank()) cp.callsign else lp.callsign,
      highestTutorialCleared = maxOf(lp.highestTutorialCleared, cp.highestTutorialCleared),
      totalRescues = maxOf(lp.totalRescues, cp.totalRescues),
      totalExpeditions = maxOf(lp.totalExpeditions, cp.totalExpeditions),
      bossesDefeated = maxOf(lp.bossesDefeated, cp.bossesDefeated),
      fuelCells = maxOf(lp.fuelCells, cp.fuelCells),
      sparkFlint = maxOf(lp.sparkFlint, cp.sparkFlint),
      magmaMatches = maxOf(lp.magmaMatches, cp.magmaMatches),
      glassJars = maxOf(lp.glassJars, cp.glassJars),
      dewVials = maxOf(lp.dewVials, cp.dewVials),
      cryoCondensers = maxOf(lp.cryoCondensers, cp.cryoCondensers),
      capacitorChips = maxOf(lp.capacitorChips, cp.capacitorChips),
      caveBatteries = maxOf(lp.caveBatteries, cp.caveBatteries),
      copperCoils = maxOf(lp.copperCoils, cp.copperCoils),
      pyroEggs = maxOf(lp.pyroEggs, cp.pyroEggs),
      hydroEggs = maxOf(lp.hydroEggs, cp.hydroEggs),
      voltEggs = maxOf(lp.voltEggs, cp.voltEggs),
    )

    val cloudLabMap = cloudLabs.associateBy { ElementType.fromId(it.elementId) }
    val mergedLabs = ElementType.entries.associateWith { el ->
      val ll = local.labs[el] ?: createDefaultLab(el)
      val cl = cloudLabMap[el]
      if (cl == null) {
        ll
      } else {
        val upgrades = (ll.unlockedUpgrades + cl.unlockedUpgrades).distinct()
        val tier = maxOf(ll.labTier, cl.labTier, (1 + upgrades.size / 2).coerceIn(1, 5))
        ll.copy(
          unlocked = ll.unlocked || cl.unlocked,
          labTier = tier,
          unlockedUpgrades = upgrades,
        )
      }
    }

    val cloudLeaderMap = cloudLeaders.associateBy { ElementType.fromId(it.elementId) }
    val mergedLeaders = ElementType.entries.associateWith { el ->
      val lLead = local.leaders[el] ?: createDefaultLeader(el)
      val cLead = cloudLeaderMap[el]
      if (cLead == null) {
        lLead
      } else {
        val level = maxOf(lLead.level, cLead.level)
        val xp = if (lLead.level > cLead.level) lLead.xp else maxOf(lLead.xp, cLead.xp)
        val skills = (lLead.unlockedSkills + cLead.unlockedSkills).distinct()
        lLead.copy(
          unlocked = lLead.unlocked || cLead.unlocked,
          level = level,
          xp = xp,
          maxHp = maxOf(lLead.maxHp, cLead.maxHp),
          attackPower = maxOf(lLead.attackPower, cLead.attackPower),
          unlockedSkills = skills,
        )
      }
    }

    val seenIds = mutableSetOf<String>()
    val mergedExpeditions = (local.expeditions + cloudExpeditions)
      .filter { if (it.id.isNotEmpty()) seenIds.add(it.id) else true }
      .take(30)

    return OfflineSnapshot(
      profile = mergedProfile,
      labs = mergedLabs,
      leaders = mergedLeaders,
      expeditions = mergedExpeditions,
    )
  }

  private fun persistSnapshot(snapshot: OfflineSnapshot) {
    try {
      val root = JSONObject()
      val pObj = JSONObject().apply {
        put("id", snapshot.profile.id)
        put("userId", snapshot.profile.userId)
        put("callsign", snapshot.profile.callsign)
        put("highestTutorialCleared", snapshot.profile.highestTutorialCleared)
        put("totalRescues", snapshot.profile.totalRescues)
        put("totalExpeditions", snapshot.profile.totalExpeditions)
        put("bossesDefeated", snapshot.profile.bossesDefeated)
        put("fuelCells", snapshot.profile.fuelCells)
        put("sparkFlint", snapshot.profile.sparkFlint)
        put("magmaMatches", snapshot.profile.magmaMatches)
        put("glassJars", snapshot.profile.glassJars)
        put("dewVials", snapshot.profile.dewVials)
        put("cryoCondensers", snapshot.profile.cryoCondensers)
        put("capacitorChips", snapshot.profile.capacitorChips)
        put("caveBatteries", snapshot.profile.caveBatteries)
        put("copperCoils", snapshot.profile.copperCoils)
        put("pyroEggs", snapshot.profile.pyroEggs)
        put("hydroEggs", snapshot.profile.hydroEggs)
        put("voltEggs", snapshot.profile.voltEggs)
      }
      root.put("profile", pObj)

      val labsArr = JSONArray()
      snapshot.labs.values.forEach { lab ->
        labsArr.put(
          JSONObject().apply {
            put("id", lab.id)
            put("elementId", lab.elementId)
            put("unlocked", lab.unlocked)
            put("labTier", lab.labTier)
            put("unlockedUpgrades", JSONArray(lab.unlockedUpgrades))
          }
        )
      }
      root.put("labs", labsArr)

      val leadersArr = JSONArray()
      snapshot.leaders.values.forEach { l ->
        leadersArr.put(
          JSONObject().apply {
            put("id", l.id)
            put("elementId", l.elementId)
            put("name", l.name)
            put("unlocked", l.unlocked)
            put("level", l.level)
            put("xp", l.xp)
            put("maxHp", l.maxHp)
            put("attackPower", l.attackPower)
            put("unlockedSkills", JSONArray(l.unlockedSkills))
          }
        )
      }
      root.put("leaders", leadersArr)

      val expArr = JSONArray()
      snapshot.expeditions.forEach { e ->
        expArr.put(
          JSONObject().apply {
            put("id", e.id)
            put("sectorId", e.sectorId)
            put("sectorName", e.sectorName)
            put("seed", e.seed)
            put("biome", e.biome)
            put("weather", e.weather)
            put("rescuedCount", e.rescuedCount)
            put("totalPods", e.totalPods)
            put("bossDefeated", e.bossDefeated)
            put("victory", e.victory)
            put("rank", e.rank)
            put("durationSeconds", e.durationSeconds)
          }
        )
      }
      root.put("expeditions", expArr)

      val temp = File(context.filesDir, "omninauts_offline_save.tmp")
      temp.writeText(root.toString())
      if (!temp.renameTo(saveFile)) {
        saveFile.writeText(root.toString())
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error persisting offline progression snapshot", e)
    }
  }

  private fun parseSnapshot(jsonStr: String): OfflineSnapshot? {
    return try {
      val root = JSONObject(jsonStr)
      val pObj = root.getJSONObject("profile")
      val profile = CommanderProfile(
        id = pObj.optString("id", "offline_local"),
        userId = pObj.optString("userId", "offline_user"),
        callsign = pObj.optString("callsign", "Commander"),
        highestTutorialCleared = pObj.optInt("highestTutorialCleared", 0),
        totalRescues = pObj.optInt("totalRescues", 0),
        totalExpeditions = pObj.optInt("totalExpeditions", 0),
        bossesDefeated = pObj.optInt("bossesDefeated", 0),
        fuelCells = pObj.optInt("fuelCells", 6),
        sparkFlint = pObj.optInt("sparkFlint", 6),
        magmaMatches = pObj.optInt("magmaMatches", 3),
        glassJars = pObj.optInt("glassJars", 2),
        dewVials = pObj.optInt("dewVials", 2),
        cryoCondensers = pObj.optInt("cryoCondensers", 0),
        capacitorChips = pObj.optInt("capacitorChips", 2),
        caveBatteries = pObj.optInt("caveBatteries", 2),
        copperCoils = pObj.optInt("copperCoils", 0),
        pyroEggs = pObj.optInt("pyroEggs", 4),
        hydroEggs = pObj.optInt("hydroEggs", 2),
        voltEggs = pObj.optInt("voltEggs", 2),
      )

      val labsMap = mutableMapOf<ElementType, ElementalLabState>()
      val labsArr = root.optJSONArray("labs") ?: JSONArray()
      for (i in 0 until labsArr.length()) {
        val o = labsArr.getJSONObject(i)
        val el = ElementType.fromId(o.optString("elementId", "pyro"))
        val upArr = o.optJSONArray("unlockedUpgrades") ?: JSONArray()
        val upList = mutableListOf<String>()
        for (j in 0 until upArr.length()) upList.add(upArr.getString(j))
        labsMap[el] = ElementalLabState(
          id = o.optString("id", el.id),
          elementId = el.id,
          unlocked = o.optBoolean("unlocked", el == ElementType.PYRO),
          labTier = o.optInt("labTier", 1),
          unlockedUpgrades = upList,
        )
      }
      ElementType.entries.forEach { el ->
        if (!labsMap.containsKey(el)) labsMap[el] = createDefaultLab(el)
      }

      val leadersMap = mutableMapOf<ElementType, LeaderState>()
      val leadersArr = root.optJSONArray("leaders") ?: JSONArray()
      for (i in 0 until leadersArr.length()) {
        val o = leadersArr.getJSONObject(i)
        val el = ElementType.fromId(o.optString("elementId", "pyro"))
        val skArr = o.optJSONArray("unlockedSkills") ?: JSONArray()
        val skList = mutableListOf<String>()
        for (j in 0 until skArr.length()) skList.add(skArr.getString(j))
        leadersMap[el] = LeaderState(
          id = o.optString("id", el.id),
          elementId = el.id,
          name = o.optString("name", el.leaderName),
          unlocked = o.optBoolean("unlocked", el == ElementType.PYRO),
          level = o.optInt("level", 1),
          xp = o.optInt("xp", 0),
          maxHp = o.optInt("maxHp", 190),
          attackPower = o.optInt("attackPower", 26),
          unlockedSkills = skList,
        )
      }
      ElementType.entries.forEach { el ->
        if (!leadersMap.containsKey(el)) leadersMap[el] = createDefaultLeader(el)
      }

      val expList = mutableListOf<ExpeditionRecord>()
      val expArr = root.optJSONArray("expeditions") ?: JSONArray()
      for (i in 0 until expArr.length()) {
        val o = expArr.getJSONObject(i)
        expList.add(
          ExpeditionRecord(
            id = o.optString("id", UUID.randomUUID().toString()),
            sectorId = o.optString("sectorId", ""),
            sectorName = o.optString("sectorName", "Subterranean Expedition"),
            seed = o.optInt("seed", 0),
            biome = o.optString("biome", "FUNGAL_GROTTO"),
            weather = o.optString("weather", "CLEAR"),
            rescuedCount = o.optInt("rescuedCount", 0),
            totalPods = o.optInt("totalPods", 1),
            bossDefeated = o.optBoolean("bossDefeated", false),
            victory = o.optBoolean("victory", false),
            rank = o.optString("rank", "B"),
            durationSeconds = o.optInt("durationSeconds", 0),
          )
        )
      }

      OfflineSnapshot(
        profile = profile,
        labs = labsMap,
        leaders = leadersMap,
        expeditions = expList,
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing offline JSON snapshot", e)
      null
    }
  }

  companion object {
    private const val TAG = "OfflineProgression"

    fun createDefaultProfile(callsign: String): CommanderProfile {
      return CommanderProfile(
        id = "offline_profile",
        userId = "offline_user",
        callsign = callsign.ifBlank { "Offline Commander" },
        highestTutorialCleared = 0,
        totalRescues = 0,
        totalExpeditions = 0,
        bossesDefeated = 0,
        fuelCells = 6,
        sparkFlint = 6,
        magmaMatches = 3,
        glassJars = 3,
        dewVials = 3,
        cryoCondensers = 1,
        capacitorChips = 3,
        caveBatteries = 3,
        copperCoils = 1,
        pyroEggs = 4,
        hydroEggs = 2,
        voltEggs = 2,
      )
    }

    fun createDefaultLab(element: ElementType): ElementalLabState {
      return ElementalLabState(
        id = element.id,
        userId = "offline_user",
        elementId = element.id,
        unlocked = element == ElementType.PYRO,
        labTier = 1,
        unlockedUpgrades = emptyList(),
      )
    }

    fun createDefaultLeader(element: ElementType): LeaderState {
      val firstSkill = UpgradeCatalog.skillsFor(element).first().id
      return LeaderState(
        id = element.id,
        userId = "offline_user",
        elementId = element.id,
        name = element.leaderName,
        unlocked = element == ElementType.PYRO,
        level = 1,
        xp = 0,
        maxHp = when (element) {
          ElementType.PYRO -> 190
          ElementType.HYDRO -> 210
          ElementType.VOLT -> 170
        },
        attackPower = when (element) {
          ElementType.PYRO -> 26
          ElementType.HYDRO -> 22
          ElementType.VOLT -> 28
        },
        unlockedSkills = listOf(firstSkill),
      )
    }

    fun createDefaultSnapshot(callsign: String): OfflineSnapshot {
      val p = createDefaultProfile(callsign)
      val labs = ElementType.entries.associateWith { createDefaultLab(it) }
      val leaders = ElementType.entries.associateWith { createDefaultLeader(it) }
      return OfflineSnapshot(
        profile = p,
        labs = labs,
        leaders = leaders,
        expeditions = emptyList(),
      )
    }

    private fun profileToResourceMap(p: CommanderProfile): Map<ResourceType, Int> {
      return mapOf(
        ResourceType.FUEL_CELLS to p.fuelCells,
        ResourceType.SPARK_FLINT to p.sparkFlint,
        ResourceType.MAGMA_MATCHES to p.magmaMatches,
        ResourceType.GLASS_JARS to p.glassJars,
        ResourceType.DEW_VIALS to p.dewVials,
        ResourceType.CRYO_CONDENSERS to p.cryoCondensers,
        ResourceType.CAPACITOR_CHIPS to p.capacitorChips,
        ResourceType.CAVE_BATTERIES to p.caveBatteries,
        ResourceType.COPPER_COILS to p.copperCoils,
      )
    }

    private fun applyResourceMapToProfile(
      p: CommanderProfile,
      m: Map<ResourceType, Int>,
    ): CommanderProfile {
      return p.copy(
        fuelCells = m[ResourceType.FUEL_CELLS] ?: p.fuelCells,
        sparkFlint = m[ResourceType.SPARK_FLINT] ?: p.sparkFlint,
        magmaMatches = m[ResourceType.MAGMA_MATCHES] ?: p.magmaMatches,
        glassJars = m[ResourceType.GLASS_JARS] ?: p.glassJars,
        dewVials = m[ResourceType.DEW_VIALS] ?: p.dewVials,
        cryoCondensers = m[ResourceType.CRYO_CONDENSERS] ?: p.cryoCondensers,
        capacitorChips = m[ResourceType.CAPACITOR_CHIPS] ?: p.capacitorChips,
        caveBatteries = m[ResourceType.CAVE_BATTERIES] ?: p.caveBatteries,
        copperCoils = m[ResourceType.COPPER_COILS] ?: p.copperCoils,
      )
    }
  }
}
