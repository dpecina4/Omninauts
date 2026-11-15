package com.example.game

import com.example.data.model.BiomeType
import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.LeaderState
import com.example.data.model.ResourceType
import com.example.data.model.UpgradeCatalog
import com.example.data.model.WeatherCondition
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object SectorCatalog {
  val tutorialSectors: List<SectorSpec> =
    listOf(
      SectorSpec(
        id = "tut_01",
        name = "Sector 01: Ember Awakening",
        subtitle = "Tutorial • Pyro Core Mechanics",
        seed = 1011,
        biome = BiomeType.BASALT_RIFT,
        weather = WeatherCondition.CLEAR,
        tutorialNumber = 1,
        timeLimitSeconds = 240,
        hasBoss = false,
        hintBanner =
          "Tap Cryo Barriers or Eggs to assign Ember-Mites. Hold WHISTLE to rally your squad!",
      ),
      SectorSpec(
        id = "tut_02",
        name = "Sector 02: Flooded Cistern",
        subtitle = "Tutorial • Hydro Counterplay & Weather",
        seed = 2022,
        biome = BiomeType.CRYSTAL_CISTERN,
        weather = WeatherCondition.BRINE_RAIN,
        tutorialNumber = 2,
        timeLimitSeconds = 260,
        hasBoss = true,
        hintBanner =
          "Use Dew-Pods to douse Fire Jets & bridge Brine Trenches. Switch to Leader Mode vs the Beetle Boss!",
      ),
      SectorSpec(
        id = "tut_03",
        name = "Sector 03: Dead Circuit Vault",
        subtitle = "Tutorial • Volt Relays & Biome Limits",
        seed = 3033,
        biome = BiomeType.DYNAMO_RUINS,
        weather = WeatherCondition.ION_SURGE,
        tutorialNumber = 3,
        timeLimitSeconds = 280,
        hasBoss = true,
        hintBanner =
          "Pyro is restricted in oxygen-starved ruins. Assign Spark-Pips to Circuit Locks & stun the Overseer!",
      ),
    )

  private val sectorPrefixes =
    listOf("KEPLER", "NYX", "VULCAN", "CHARYBDIS", "HELIX", "AETHER", "DRIFT", "SOLARIS")
  private val sectorSuffixes =
    listOf("TRENCH", "NEXUS", "VAULT", "ABYSS", "HOLLOW", "SANCTUM", "SPIRE", "CALDERA")

  fun generateProceduralSector(seed: Int): SectorSpec {
    val rng = Random(seed)
    val biome = BiomeType.entries[rng.nextInt(BiomeType.entries.size)]
    val weather = WeatherCondition.entries[rng.nextInt(WeatherCondition.entries.size)]
    val prefix = sectorPrefixes[rng.nextInt(sectorPrefixes.size)]
    val suffix = sectorSuffixes[rng.nextInt(sectorSuffixes.size)]
    val sectorNum = (abs(seed) % 90) + 10
    return SectorSpec(
      id = "proc_${abs(seed)}",
      name = "$prefix-$sectorNum // $suffix",
      subtitle = "Procedural Dungeon • Seed #$seed",
      seed = seed,
      biome = biome,
      weather = weather,
      tutorialNumber = null,
      timeLimitSeconds = 270,
      hasBoss = true,
      hintBanner = "${biome.displayName} (${weather.displayName}). ${biome.restrictionNote}",
    )
  }
}

data class GeneratedWorld(
  val width: Int,
  val height: Int,
  val tiles: MutableList<TileType>,
  val obstacles: MutableList<ObstacleEntity>,
  val eggs: MutableList<FieldEgg>,
  val resourceNodes: MutableList<ResourcePickupNode>,
  val rescuePods: MutableList<RescuePod>,
  val enemies: MutableList<EnemyEntity>,
  val startX: Float,
  val startY: Float,
  val exitX: Float,
  val exitY: Float,
)

object DungeonGenerator {

  fun generate(
    sector: SectorSpec,
    deployedElements: Set<ElementType>,
    leaderStates: Map<ElementType, LeaderState>,
    labStates: Map<ElementType, ElementalLabState>,
    initialSquadCounts: Map<ElementType, Int>,
  ): ExpeditionUiState {
    val world =
      when (sector.tutorialNumber) {
        1 -> buildTutorialOne(sector)
        2 -> buildTutorialTwo(sector)
        3 -> buildTutorialThree(sector)
        else -> buildProceduralDungeon(sector, deployedElements)
      }

    val effectiveElements =
      deployedElements.filter { it in sector.biome.allowedElements }.ifEmpty {
        listOf(sector.biome.allowedElements.first())
      }

    val leaders =
      effectiveElements.mapIndexed { idx, element ->
        val state = leaderStates[element]
        val skills = UpgradeCatalog.skillsFor(element)
        val primary = skills.first().id
        val secondary =
          if ((state?.level ?: 1) >= 2 || state?.unlockedSkills?.contains(skills[1].id) == true) {
            skills[1].id
          } else null
        LeaderUnit(
          element = element,
          name = element.leaderName,
          level = state?.level ?: 1,
          x = world.startX + (idx - 0.5f) * 0.7f,
          y = world.startY,
          hp = (state?.maxHp ?: 190).toFloat(),
          maxHp = (state?.maxHp ?: 190).toFloat(),
          attackPower = (state?.attackPower ?: 25).toFloat(),
          primarySkillId = primary,
          secondarySkillId = secondary,
        )
      }

    val soldiers = mutableListOf<SoldierUnit>()
    var unitCounter = 0
    effectiveElements.forEach { el ->
      val count = (initialSquadCounts[el] ?: 4).coerceIn(2, 12)
      val unlocked = labStates[el]?.unlockedUpgrades ?: emptyList()
      repeat(count) { i ->
        unitCounter++
        val angle = (i.toFloat() / count) * (Math.PI * 2.0)
        soldiers.add(
          createSoldier(
            id = "unit_${el.id}_$unitCounter",
            element = el,
            x = world.startX + kotlin.math.cos(angle).toFloat() * 0.9f,
            y = world.startY + kotlin.math.sin(angle).toFloat() * 0.9f,
            unlockedUpgrades = unlocked,
          )
        )
      }
    }

    return ExpeditionUiState(
      sector = sector,
      gridWidth = world.width,
      gridHeight = world.height,
      tiles = world.tiles,
      obstacles = world.obstacles,
      leaders = leaders,
      activeLeaderElement = leaders.first().element,
      soldiers = soldiers,
      eggs = world.eggs,
      resourceNodes = world.resourceNodes,
      rescuePods = world.rescuePods,
      enemies = world.enemies,
      effects = emptyList(),
      timeRemaining = sector.timeLimitSeconds.toFloat(),
      statusBanner = sector.hintBanner,
      startX = world.startX,
      startY = world.startY,
      exitX = world.exitX,
      exitY = world.exitY,
    )
  }

  fun createSoldier(
    id: String,
    element: ElementType,
    x: Float,
    y: Float,
    unlockedUpgrades: List<String>,
  ): SoldierUnit {
    val hpMult = if (unlockedUpgrades.any { it.endsWith("_surv_1") }) 1.4f else 1f
    val atkMult =
      when {
        unlockedUpgrades.any { it.endsWith("_combat_2") } -> 1.6f
        unlockedUpgrades.any { it.endsWith("_combat_1") } -> 1.35f
        else -> 1f
      }
    val moveMult = if (unlockedUpgrades.any { it.endsWith("_util_2") }) 1.25f else 1f
    val clearMult = if (unlockedUpgrades.any { it.endsWith("_util_1") }) 1.45f else 1f
    val weatherResist = if (unlockedUpgrades.any { it.endsWith("_surv_2") }) 0.6f else 0f
    val bonusEffect = unlockedUpgrades.any { it.endsWith("_combat_2") }

    val baseHp =
      when (element) {
        ElementType.PYRO -> 60f
        ElementType.HYDRO -> 72f
        ElementType.VOLT -> 54f
      }
    val baseAtk =
      when (element) {
        ElementType.PYRO -> 11f
        ElementType.HYDRO -> 9f
        ElementType.VOLT -> 12f
      }
    val baseSpeed =
      when (element) {
        ElementType.PYRO -> 3.4f
        ElementType.HYDRO -> 3.2f
        ElementType.VOLT -> 3.8f
      }

    return SoldierUnit(
      id = id,
      element = element,
      x = x,
      y = y,
      hp = baseHp * hpMult,
      maxHp = baseHp * hpMult,
      attackPower = baseAtk * atkMult,
      moveSpeed = baseSpeed * moveMult,
      clearSpeedMult = clearMult,
      weatherResist = weatherResist,
      hasBonusCombatEffect = bonusEffect,
    )
  }

  private fun blankGrid(w: Int, h: Int): MutableList<TileType> =
    MutableList(w * h) { TileType.WALL }

  private fun carveRect(
    tiles: MutableList<TileType>,
    w: Int,
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int,
  ) {
    for (y in y0..y1) {
      for (x in x0..x1) {
        tiles[y * w + x] = TileType.FLOOR
      }
    }
  }

  private fun carveCorridor(
    tiles: MutableList<TileType>,
    w: Int,
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int,
  ) {
    var cx = x0
    var cy = y0
    while (cx != x1) {
      tiles[cy * w + cx] = TileType.FLOOR
      tiles[(cy + 1).coerceAtMost(tiles.size / w - 2) * w + cx] = TileType.FLOOR
      cx += if (x1 > cx) 1 else -1
    }
    while (cy != y1) {
      tiles[cy * w + cx] = TileType.FLOOR
      tiles[cy * w + (cx + 1).coerceAtMost(w - 2)] = TileType.FLOOR
      cy += if (y1 > cy) 1 else -1
    }
  }

  private fun placeObstacle(
    tiles: MutableList<TileType>,
    obstacles: MutableList<ObstacleEntity>,
    w: Int,
    tx: Int,
    ty: Int,
    type: TileType,
    hp: Float = 5.5f,
  ) {
    val req = type.requiredElement() ?: return
    tiles[ty * w + tx] = type
    obstacles.add(
      ObstacleEntity(
        id = "obs_${tx}_$ty",
        tileX = tx,
        tileY = ty,
        tileType = type,
        requiredElement = req,
        hp = hp,
        maxHp = hp,
      )
    )
  }

  private fun buildTutorialOne(sector: SectorSpec): GeneratedWorld {
    val w = 18
    val h = 14
    val tiles = blankGrid(w, h)
    carveRect(tiles, w, 1, 5, 5, 9) // Start chamber
    carveRect(tiles, w, 5, 6, 12, 8) // Mid corridor
    carveRect(tiles, w, 7, 2, 11, 5) // North egg cove
    carveRect(tiles, w, 12, 3, 16, 11) // East rescue & exit chamber

    tiles[7 * w + 2] = TileType.START_PAD
    tiles[7 * w + 15] = TileType.EXIT_BEACON

    val obstacles = mutableListOf<ObstacleEntity>()
    placeObstacle(tiles, obstacles, w, 8, 5, TileType.OBSTACLE_SPORE_WEB, hp = 4.5f)
    placeObstacle(tiles, obstacles, w, 11, 6, TileType.OBSTACLE_CRYO_BARRIER, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 11, 7, TileType.OBSTACLE_CRYO_BARRIER, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 11, 8, TileType.OBSTACLE_CRYO_BARRIER, hp = 5.0f)

    val eggs =
      mutableListOf(
        FieldEgg("egg_t1_1", ElementType.PYRO, 9.5f, 3.5f),
        FieldEgg("egg_t1_2", ElementType.PYRO, 13.5f, 9.5f),
      )

    val resources =
      mutableListOf(
        ResourcePickupNode("res_t1_1", ResourceType.SPARK_FLINT, 4.2f, 6.2f, 3),
        ResourcePickupNode("res_t1_2", ResourceType.FUEL_CELLS, 7.0f, 7.5f, 3),
        ResourcePickupNode("res_t1_3", ResourceType.MAGMA_MATCHES, 13.5f, 4.5f, 2),
      )

    val pods =
      mutableListOf(
        RescuePod("pod_t1_1", "Echo-01", 9.5f, 2.8f),
        RescuePod("pod_t1_2", "Beacon-02", 14.5f, 5.2f),
      )

    val enemies =
      mutableListOf(
        EnemyEntity(
          id = "enemy_t1_1",
          kind = EnemyKind.SPORE_CRAWLER,
          title = "Spore Crawler",
          x = 13.5f,
          y = 7.5f,
          hp = 75f,
          maxHp = 75f,
          attackDamage = 8f,
          radius = 0.55f,
        )
      )

    return GeneratedWorld(w, h, tiles, obstacles, eggs, resources, pods, enemies, 2.5f, 7.5f, 15.5f, 7.5f)
  }

  private fun buildTutorialTwo(sector: SectorSpec): GeneratedWorld {
    val w = 20
    val h = 16
    val tiles = blankGrid(w, h)
    carveRect(tiles, w, 1, 6, 5, 10)
    carveRect(tiles, w, 5, 7, 13, 9)
    carveRect(tiles, w, 7, 2, 12, 6)
    carveRect(tiles, w, 13, 3, 18, 13)

    tiles[8 * w + 2] = TileType.START_PAD
    tiles[8 * w + 17] = TileType.EXIT_BEACON

    val obstacles = mutableListOf<ObstacleEntity>()
    placeObstacle(tiles, obstacles, w, 7, 7, TileType.OBSTACLE_FIRE_JET, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 7, 8, TileType.OBSTACLE_FIRE_JET, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 7, 9, TileType.OBSTACLE_FIRE_JET, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 9, 6, TileType.OBSTACLE_BRINE_CHANNEL, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 12, 7, TileType.OBSTACLE_CRYO_BARRIER, hp = 5.5f)
    placeObstacle(tiles, obstacles, w, 12, 8, TileType.OBSTACLE_CRYO_BARRIER, hp = 5.5f)
    placeObstacle(tiles, obstacles, w, 12, 9, TileType.OBSTACLE_CRYO_BARRIER, hp = 5.5f)

    val eggs =
      mutableListOf(
        FieldEgg("egg_t2_1", ElementType.HYDRO, 6.2f, 7.2f),
        FieldEgg("egg_t2_2", ElementType.PYRO, 10.5f, 4.2f),
      )

    val resources =
      mutableListOf(
        ResourcePickupNode("res_t2_1", ResourceType.GLASS_JARS, 4.2f, 9.2f, 3),
        ResourcePickupNode("res_t2_2", ResourceType.DEW_VIALS, 8.8f, 3.2f, 3),
        ResourcePickupNode("res_t2_3", ResourceType.CRYO_CONDENSERS, 15.0f, 11.2f, 2),
        ResourcePickupNode("res_t2_4", ResourceType.FUEL_CELLS, 14.5f, 4.2f, 2),
      )

    val pods =
      mutableListOf(
        RescuePod("pod_t2_1", "Cistern-01", 10.8f, 2.8f),
        RescuePod("pod_t2_2", "Cistern-02", 16.2f, 5.2f),
      )

    val enemies =
      mutableListOf(
        EnemyEntity(
          id = "enemy_t2_1",
          kind = EnemyKind.CRYO_BEETLE,
          title = "Cryo Beetle",
          x = 10.0f,
          y = 8.0f,
          hp = 95f,
          maxHp = 95f,
          attackDamage = 10f,
          radius = 0.6f,
        ),
        EnemyEntity(
          id = "boss_t2",
          kind = EnemyKind.CAVERN_BOSS,
          title = "Magma-Shell Leviathan",
          x = 15.8f,
          y = 8.5f,
          hp = 310f,
          maxHp = 310f,
          attackDamage = 22f,
          radius = 1.05f,
        ),
      )

    return GeneratedWorld(w, h, tiles, obstacles, eggs, resources, pods, enemies, 2.5f, 8.5f, 17.5f, 8.5f)
  }

  private fun buildTutorialThree(sector: SectorSpec): GeneratedWorld {
    val w = 20
    val h = 16
    val tiles = blankGrid(w, h)
    carveRect(tiles, w, 1, 6, 5, 10)
    carveRect(tiles, w, 5, 7, 13, 9)
    carveRect(tiles, w, 8, 10, 12, 14)
    carveRect(tiles, w, 13, 3, 18, 13)

    tiles[8 * w + 2] = TileType.START_PAD
    tiles[8 * w + 17] = TileType.EXIT_BEACON

    val obstacles = mutableListOf<ObstacleEntity>()
    placeObstacle(tiles, obstacles, w, 7, 7, TileType.OBSTACLE_CIRCUIT_LOCK, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 7, 8, TileType.OBSTACLE_CIRCUIT_LOCK, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 7, 9, TileType.OBSTACLE_CIRCUIT_LOCK, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 10, 10, TileType.OBSTACLE_BRINE_CHANNEL, hp = 5.0f)
    placeObstacle(tiles, obstacles, w, 12, 7, TileType.OBSTACLE_SHIELD_PYLON, hp = 5.5f)
    placeObstacle(tiles, obstacles, w, 12, 8, TileType.OBSTACLE_SHIELD_PYLON, hp = 5.5f)
    placeObstacle(tiles, obstacles, w, 12, 9, TileType.OBSTACLE_SHIELD_PYLON, hp = 5.5f)

    val eggs =
      mutableListOf(
        FieldEgg("egg_t3_1", ElementType.VOLT, 4.2f, 6.8f),
        FieldEgg("egg_t3_2", ElementType.HYDRO, 9.5f, 12.2f),
      )

    val resources =
      mutableListOf(
        ResourcePickupNode("res_t3_1", ResourceType.CAVE_BATTERIES, 4.2f, 9.2f, 3),
        ResourcePickupNode("res_t3_2", ResourceType.CAPACITOR_CHIPS, 9.0f, 8.0f, 3),
        ResourcePickupNode("res_t3_3", ResourceType.COPPER_COILS, 15.0f, 5.0f, 2),
        ResourcePickupNode("res_t3_4", ResourceType.CRYO_CONDENSERS, 11.2f, 12.8f, 2),
      )

    val pods =
      mutableListOf(
        RescuePod("pod_t3_1", "Relay-01", 10.5f, 13.0f),
        RescuePod("pod_t3_2", "Relay-02", 16.0f, 11.2f),
      )

    val enemies =
      mutableListOf(
        EnemyEntity(
          id = "enemy_t3_1",
          kind = EnemyKind.ARC_SENTINEL,
          title = "Arc Sentinel",
          x = 10.5f,
          y = 8.2f,
          hp = 105f,
          maxHp = 105f,
          attackDamage = 12f,
          radius = 0.6f,
        ),
        EnemyEntity(
          id = "boss_t3",
          kind = EnemyKind.CAVERN_BOSS,
          title = "Dynamo Core Sentinel",
          x = 15.8f,
          y = 8.5f,
          hp = 350f,
          maxHp = 350f,
          attackDamage = 24f,
          radius = 1.1f,
        ),
      )

    return GeneratedWorld(w, h, tiles, obstacles, eggs, resources, pods, enemies, 2.5f, 8.5f, 17.5f, 8.5f)
  }

  private fun buildProceduralDungeon(
    sector: SectorSpec,
    deployedElements: Set<ElementType>,
  ): GeneratedWorld {
    val rng = Random(sector.seed)
    val w = 24
    val h = 18
    val tiles = blankGrid(w, h)

    // Carve 5–6 connected cavern rooms
    data class Room(val cx: Int, val cy: Int, val rx: Int, val ry: Int)
    val rooms = mutableListOf<Room>()
    val startRoom = Room(3, h / 2, 2, 2)
    rooms.add(startRoom)

    val targetRooms = 5 + rng.nextInt(2)
    var attempts = 0
    while (rooms.size < targetRooms && attempts < 40) {
      attempts++
      val rx = 2 + rng.nextInt(2)
      val ry = 2 + rng.nextInt(2)
      val cx = rx + 2 + rng.nextInt(w - rx * 2 - 4)
      val cy = ry + 1 + rng.nextInt(h - ry * 2 - 2)
      val candidate = Room(cx, cy, rx, ry)
      if (rooms.none { abs(it.cx - cx) < 4 && abs(it.cy - cy) < 4 }) {
        rooms.add(candidate)
      }
    }
    // Sort left-to-right so progression flows naturally toward the deep boss room
    val sortedRooms = rooms.sortedBy { it.cx }
    sortedRooms.forEach { r ->
      carveRect(
        tiles,
        w,
        max(1, r.cx - r.rx),
        max(1, r.cy - r.ry),
        min(w - 2, r.cx + r.rx),
        min(h - 2, r.cy + r.ry),
      )
    }

    val corridorMidpoints = mutableListOf<Pair<Int, Int>>()
    for (i in 0 until sortedRooms.lastIndex) {
      val a = sortedRooms[i]
      val b = sortedRooms[i + 1]
      carveCorridor(tiles, w, a.cx, a.cy, b.cx, b.cy)
      corridorMidpoints.add(((a.cx + b.cx) / 2) to b.cy)
    }

    val start = sortedRooms.first()
    val bossRoom = sortedRooms.last()
    tiles[start.cy * w + start.cx] = TileType.START_PAD
    tiles[bossRoom.cy * w + min(w - 2, bossRoom.cx + 1)] = TileType.EXIT_BEACON

    val allowed =
      (deployedElements intersect sector.biome.allowedElements).ifEmpty {
        sector.biome.allowedElements
      }.toList()

    val obstacles = mutableListOf<ObstacleEntity>()
    corridorMidpoints.take(3).forEachIndexed { idx, (mx, my) ->
      val el = allowed[idx % allowed.size]
      val obsType =
        when (el) {
          ElementType.PYRO ->
            if (rng.nextBoolean()) TileType.OBSTACLE_CRYO_BARRIER else TileType.OBSTACLE_SPORE_WEB
          ElementType.HYDRO ->
            if (rng.nextBoolean()) TileType.OBSTACLE_FIRE_JET else TileType.OBSTACLE_BRINE_CHANNEL
          ElementType.VOLT ->
            if (rng.nextBoolean()) TileType.OBSTACLE_CIRCUIT_LOCK else TileType.OBSTACLE_SHIELD_PYLON
        }
      if (tiles[my * w + mx] == TileType.FLOOR) {
        placeObstacle(tiles, obstacles, w, mx, my, obsType, hp = 6.0f)
      }
    }

    val eggs = mutableListOf<FieldEgg>()
    val resources = mutableListOf<ResourcePickupNode>()
    val pods = mutableListOf<RescuePod>()
    val enemies = mutableListOf<EnemyEntity>()

    val middleRooms = sortedRooms.drop(1).dropLast(1).ifEmpty { listOf(bossRoom) }
    middleRooms.forEachIndexed { idx, room ->
      val el = allowed[idx % allowed.size]
      eggs.add(
        FieldEgg(
          id = "proc_egg_$idx",
          element = el,
          x = room.cx.toFloat() + 0.5f,
          y = room.cy.toFloat() - 0.5f,
        )
      )
      val resChoices = ResourceType.entries.filter { it.element == el }
      val res = resChoices[rng.nextInt(resChoices.size)]
      resources.add(
        ResourcePickupNode(
          id = "proc_res_$idx",
          resourceType = res,
          x = room.cx.toFloat() - 0.6f,
          y = room.cy.toFloat() + 0.6f,
          amountLeft = 3 + rng.nextInt(2),
        )
      )
      if (idx < 3) {
        pods.add(
          RescuePod(
            id = "proc_pod_$idx",
            callsign = "Pod-${idx + 1}A",
            x = room.cx.toFloat() + 0.7f,
            y = room.cy.toFloat() + 0.6f,
          )
        )
      }
      val enemyKind =
        when (el) {
          ElementType.PYRO -> EnemyKind.SPORE_CRAWLER
          ElementType.HYDRO -> EnemyKind.CRYO_BEETLE
          ElementType.VOLT -> EnemyKind.ARC_SENTINEL
        }
      enemies.add(
        EnemyEntity(
          id = "proc_enemy_$idx",
          kind = enemyKind,
          title = enemyKind.title,
          x = room.cx.toFloat(),
          y = room.cy.toFloat(),
          hp = 95f + rng.nextInt(30),
          maxHp = 115f,
          attackDamage = 11f,
          radius = 0.58f,
        )
      )
    }

    if (pods.isEmpty()) {
      pods.add(RescuePod("proc_pod_boss", "Pod-Omega", bossRoom.cx - 0.8f, bossRoom.cy.toFloat()))
    }

    // Extra deep resource cache
    val bonusEl = allowed[rng.nextInt(allowed.size)]
    val bonusRes = ResourceType.entries.filter { it.element == bonusEl }.random(rng)
    resources.add(
      ResourcePickupNode(
        id = "proc_res_deep",
        resourceType = bonusRes,
        x = bossRoom.cx.toFloat() - 0.5f,
        y = bossRoom.cy.toFloat() + 0.8f,
        amountLeft = 4,
      )
    )

    enemies.add(
      EnemyEntity(
        id = "proc_boss",
        kind = EnemyKind.CAVERN_BOSS,
        title = "${sector.biome.displayName.substringBefore(" ")} Warden",
        x = bossRoom.cx.toFloat(),
        y = bossRoom.cy.toFloat(),
        hp = 400f,
        maxHp = 400f,
        attackDamage = 26f,
        radius = 1.15f,
      )
    )

    return GeneratedWorld(
      width = w,
      height = h,
      tiles = tiles,
      obstacles = obstacles,
      eggs = eggs,
      resourceNodes = resources,
      rescuePods = pods,
      enemies = enemies,
      startX = start.cx.toFloat() + 0.5f,
      startY = start.cy.toFloat() + 0.5f,
      exitX = min(w - 2, bossRoom.cx + 1).toFloat() + 0.5f,
      exitY = bossRoom.cy.toFloat() + 0.5f,
    )
  }
}
