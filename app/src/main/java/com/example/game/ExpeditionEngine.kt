package com.example.game

import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.ResourceType
import com.example.data.model.UpgradeCatalog
import com.example.data.model.WeatherCondition
import java.util.ArrayDeque
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class ExpeditionEngine(
  initialState: ExpeditionUiState,
  private val labStates: Map<ElementType, ElementalLabState>,
) {
  private var state: ExpeditionUiState = initialState
  private var leaderWaypoint: Pair<Float, Float>? = null
  private var joystickX: Float = 0f
  private var joystickY: Float = 0f
  private var weatherPulseTimer: Float = 0f
  private var effectCounter: Int = 0
  private var spawnedUnitCounter: Int = 100

  fun currentState(): ExpeditionUiState = state

  fun setControlMode(mode: ControlMode) {
    if (state.outcome != null) return
    leaderWaypoint = null
    joystickX = 0f
    joystickY = 0f
    state = state.copy(controlMode = mode)
  }

  fun setPaused(paused: Boolean) {
    state = state.copy(isPaused = paused)
  }

  fun switchActiveLeader(element: ElementType) {
    val target = state.leaders.firstOrNull { it.element == element && it.hp > 0f } ?: return
    leaderWaypoint = null
    state =
      state.copy(
        activeLeaderElement = target.element,
        statusBanner = "Active Leader: ${target.name} (${target.element.displayName})",
      )
  }

  fun setJoystick(dx: Float, dy: Float) {
    joystickX = dx.coerceIn(-1f, 1f)
    joystickY = dy.coerceIn(-1f, 1f)
  }

  fun setWhistle(active: Boolean) {
    state =
      state.copy(
        whistleActive = active,
        whistleRadius = if (active) max(1.2f, state.whistleRadius) else 0f,
      )
  }

  fun onWorldTap(wx: Float, wy: Float) {
    if (state.isPaused || state.outcome != null) return
    val activeLeader = state.activeLeader ?: return

    // 1. Check Obstacle tap
    val tappedObstacle =
      state.obstacles.minByOrNull { hypot(it.tileX + 0.5f - wx, it.tileY + 0.5f - wy) }
    if (tappedObstacle != null &&
      hypot(tappedObstacle.tileX + 0.5f - wx, tappedObstacle.tileY + 0.5f - wy) < 1.25f
    ) {
      assignSoldiersToObstacle(tappedObstacle)
      return
    }

    // 2. Check FieldEgg tap
    val tappedEgg =
      state.eggs.filter { !it.hatched }.minByOrNull { hypot(it.x - wx, it.y - wy) }
    if (tappedEgg != null && hypot(tappedEgg.x - wx, tappedEgg.y - wy) < 1.25f) {
      assignSoldiersToEgg(tappedEgg)
      return
    }

    // 3. Check ResourceNode tap
    val tappedRes =
      state.resourceNodes.filter { it.amountLeft > 0 }.minByOrNull { hypot(it.x - wx, it.y - wy) }
    if (tappedRes != null && hypot(tappedRes.x - wx, tappedRes.y - wy) < 1.25f) {
      assignSoldiersToResource(tappedRes)
      return
    }

    // 4. Check Enemy tap
    val tappedEnemy =
      state.enemies.filter { it.hp > 0f }.minByOrNull { hypot(it.x - wx, it.y - wy) }
    if (tappedEnemy != null &&
      hypot(tappedEnemy.x - wx, tappedEnemy.y - wy) < (tappedEnemy.radius + 0.9f)
    ) {
      assignSoldiersToEnemy(tappedEnemy)
      return
    }

    // 5. Move active leader toward tapped floor
    val tx = wx.toInt().coerceIn(0, state.gridWidth - 1)
    val ty = wy.toInt().coerceIn(0, state.gridHeight - 1)
    if (isWalkable(tx, ty)) {
      leaderWaypoint = wx to wy
      val newEffects =
        state.effects +
          VisualEffect(
            id = nextEffectId(),
            kind = EffectKind.WAYPOINT_PING,
            x = wx,
            y = wy,
            radius = 0.6f,
            element = activeLeader.element,
            life = 0.55f,
            maxLife = 0.55f,
          )
      state = state.copy(effects = newEffects)
    } else {
      leaderWaypoint = activeLeader.x to activeLeader.y
    }
  }

  private fun assignSoldiersToObstacle(obs: ObstacleEntity) {
    val eligible =
      state.soldiers.filter {
        it.element == obs.requiredElement &&
          (it.state == SoldierState.FOLLOWING || it.state == SoldierState.IDLE)
      }
    if (eligible.isEmpty()) {
      state =
        state.copy(
          statusBanner =
            "Requires ${obs.requiredElement.unitName} (${obs.requiredElement.displayName}) to clear ${obs.tileType.obstacleLabel()}!"
        )
      return
    }
    val chosen =
      eligible
        .sortedBy { hypot(it.x - (obs.tileX + 0.5f), it.y - (obs.tileY + 0.5f)) }
        .take(3)
        .map { it.id }
        .toSet()

    val updated =
      state.soldiers.map { s ->
        if (s.id in chosen) {
          s.copy(
            state = SoldierState.CLEARING_OBSTACLE,
            targetX = obs.tileX + 0.5f,
            targetY = obs.tileY + 0.5f,
            targetId = obs.id,
          )
        } else s
      }
    state =
      state.copy(
        soldiers = updated,
        statusBanner = "${chosen.size} ${obs.requiredElement.unitName}(s) clearing ${obs.tileType.obstacleLabel()}.",
      )
  }

  private fun assignSoldiersToEgg(egg: FieldEgg) {
    val eligible =
      state.soldiers.filter {
        it.element == egg.element &&
          (it.state == SoldierState.FOLLOWING || it.state == SoldierState.IDLE)
      }
    val fallback =
      if (eligible.isEmpty()) {
        state.soldiers.filter { it.state == SoldierState.FOLLOWING || it.state == SoldierState.IDLE }
      } else eligible
    if (fallback.isEmpty()) {
      state = state.copy(statusBanner = "Recall units with WHISTLE before hatching ${egg.element.displayName} Egg!")
      return
    }
    val chosen = fallback.sortedBy { hypot(it.x - egg.x, it.y - egg.y) }.take(2).map { it.id }.toSet()
    val updated =
      state.soldiers.map { s ->
        if (s.id in chosen) {
          s.copy(state = SoldierState.HATCHING, targetX = egg.x, targetY = egg.y, targetId = egg.id)
        } else s
      }
    state =
      state.copy(
        soldiers = updated,
        statusBanner = "Incubating ${egg.element.displayName} Egg...",
      )
  }

  private fun assignSoldiersToResource(node: ResourcePickupNode) {
    val eligible =
      state.soldiers.filter {
        it.state == SoldierState.FOLLOWING || it.state == SoldierState.IDLE
      }
    if (eligible.isEmpty()) {
      state = state.copy(statusBanner = "Hold WHISTLE to rally idle units for harvesting.")
      return
    }
    val chosen = eligible.sortedBy { hypot(it.x - node.x, it.y - node.y) }.take(2).map { it.id }.toSet()
    val updated =
      state.soldiers.map { s ->
        if (s.id in chosen) {
          s.copy(
            state = SoldierState.HARVESTING,
            targetX = node.x,
            targetY = node.y,
            targetId = node.id,
          )
        } else s
      }
    state =
      state.copy(
        soldiers = updated,
        statusBanner = "Harvesting ${node.resourceType.displayName}...",
      )
  }

  private fun assignSoldiersToEnemy(enemy: EnemyEntity) {
    val eligible =
      state.soldiers.filter {
        it.state == SoldierState.FOLLOWING || it.state == SoldierState.IDLE
      }
    if (eligible.isEmpty()) return
    val chosen =
      eligible.sortedBy { hypot(it.x - enemy.x, it.y - enemy.y) }.take(4).map { it.id }.toSet()
    val updated =
      state.soldiers.map { s ->
        if (s.id in chosen) {
          s.copy(
            state = SoldierState.FIGHTING,
            targetX = enemy.x,
            targetY = enemy.y,
            targetId = enemy.id,
          )
        } else s
      }
    state =
      state.copy(
        soldiers = updated,
        statusBanner = "Squad engaging ${enemy.title}!",
      )
  }

  fun triggerLeaderPrimaryStrike() {
    if (state.isPaused || state.outcome != null) return
    val leader = state.activeLeader ?: return
    if (leader.attackCooldown > 0f) return

    val strikeX = leader.x + cos(leader.facingAngle) * 0.9f
    val strikeY = leader.y + sin(leader.facingAngle) * 0.9f
    val effects = state.effects.toMutableList()
    effects.add(
      VisualEffect(
        id = nextEffectId(),
        kind = EffectKind.ABILITY_BLAST,
        x = strikeX,
        y = strikeY,
        radius = 1.45f,
        element = leader.element,
        life = 0.25f,
        maxLife = 0.25f,
      )
    )

    val newEnemies =
      state.enemies.map { e ->
        if (e.hp > 0f && hypot(e.x - strikeX, e.y - strikeY) <= (e.radius + 1.45f)) {
          val dmg = leader.attackPower
          effects.add(
            VisualEffect(
              id = nextEffectId(),
              kind = EffectKind.FLOATING_TEXT,
              x = e.x,
              y = e.y - 0.5f,
              radius = 0f,
              element = leader.element,
              life = 0.65f,
              maxLife = 0.65f,
              label = "-${dmg.toInt()}",
            )
          )
          e.copy(hp = (e.hp - dmg).coerceAtLeast(0f), hitFlash = 0.2f)
        } else e
      }

    // Leaders can also chip matching elemental obstacles directly
    val tiles = state.tiles.toMutableList()
    val remainingObstacles = mutableListOf<ObstacleEntity>()
    state.obstacles.forEach { obs ->
      if (obs.requiredElement == leader.element &&
        hypot(obs.tileX + 0.5f - strikeX, obs.tileY + 0.5f - strikeY) <= 1.6f
      ) {
        val nextHp = obs.hp - 2.2f
        if (nextHp <= 0f) {
          tiles[obs.tileY * state.gridWidth + obs.tileX] = TileType.FLOOR
        } else {
          remainingObstacles.add(obs.copy(hp = nextHp))
        }
      } else {
        remainingObstacles.add(obs)
      }
    }

    val updatedLeaders =
      state.leaders.map {
        if (it.element == leader.element) it.copy(attackCooldown = 0.42f) else it
      }

    state =
      state.copy(
        leaders = updatedLeaders,
        enemies = newEnemies.filter { it.hp > 0f },
        obstacles = remainingObstacles,
        tiles = tiles,
        effects = effects,
      )
  }

  fun triggerLeaderSkill(slot: Int) {
    if (state.isPaused || state.outcome != null) return
    val leader = state.activeLeader ?: return
    val skillId = if (slot == 1) leader.primarySkillId else leader.secondarySkillId ?: return
    val cooldown = if (slot == 1) leader.primaryCooldown else leader.secondaryCooldown
    if (cooldown > 0f) return

    val skillDef = UpgradeCatalog.leaderSkills.firstOrNull { it.id == skillId } ?: return
    val effects = state.effects.toMutableList()
    effects.add(
      VisualEffect(
        id = nextEffectId(),
        kind = EffectKind.ABILITY_BLAST,
        x = leader.x,
        y = leader.y,
        radius = skillDef.radiusTiles,
        element = leader.element,
        life = 0.45f,
        maxLife = 0.45f,
      )
    )

    // Damage & optional stun on enemies
    val stunBonus = if (leader.element == ElementType.VOLT) 2.2f else 0f
    val newEnemies =
      state.enemies.map { e ->
        if (e.hp > 0f && hypot(e.x - leader.x, e.y - leader.y) <= (skillDef.radiusTiles + e.radius)) {
          val dmg = skillDef.damage.toFloat() + leader.level * 5f
          effects.add(
            VisualEffect(
              id = nextEffectId(),
              kind = EffectKind.FLOATING_TEXT,
              x = e.x,
              y = e.y - 0.6f,
              radius = 0f,
              element = leader.element,
              life = 0.8f,
              maxLife = 0.8f,
              label = "-${dmg.toInt()}",
            )
          )
          e.copy(
            hp = (e.hp - dmg).coerceAtLeast(0f),
            stunTimer = max(e.stunTimer, stunBonus),
            hitFlash = 0.3f,
          )
        } else e
      }.filter { it.hp > 0f }

    // Hydro Tidal Barrier heals nearby units!
    val healedSoldiers =
      if (leader.element == ElementType.HYDRO) {
        state.soldiers.map { s ->
          if (hypot(s.x - leader.x, s.y - leader.y) <= skillDef.radiusTiles) {
            s.copy(hp = (s.hp + 28f).coerceAtMost(s.maxHp))
          } else s
        }
      } else state.soldiers

    // Matching obstacle clear blast
    val tiles = state.tiles.toMutableList()
    val remainingObstacles = mutableListOf<ObstacleEntity>()
    state.obstacles.forEach { obs ->
      if (obs.requiredElement == leader.element &&
        hypot(obs.tileX + 0.5f - leader.x, obs.tileY + 0.5f - leader.y) <= skillDef.radiusTiles
      ) {
        val nextHp = obs.hp - 4.5f
        if (nextHp <= 0f) {
          tiles[obs.tileY * state.gridWidth + obs.tileX] = TileType.FLOOR
        } else {
          remainingObstacles.add(obs.copy(hp = nextHp))
        }
      } else {
        remainingObstacles.add(obs)
      }
    }

    val updatedLeaders =
      state.leaders.map { l ->
        if (l.element == leader.element) {
          val healSelf = if (leader.element == ElementType.HYDRO) 35f else 0f
          l.copy(
            hp = (l.hp + healSelf).coerceAtMost(l.maxHp),
            primaryCooldown = if (slot == 1) skillDef.cooldownSeconds else l.primaryCooldown,
            secondaryCooldown = if (slot == 2) skillDef.cooldownSeconds else l.secondaryCooldown,
          )
        } else l
      }

    state =
      state.copy(
        leaders = updatedLeaders,
        soldiers = healedSoldiers,
        enemies = newEnemies,
        obstacles = remainingObstacles,
        tiles = tiles,
        effects = effects,
        statusBanner = "${leader.name} activated ${skillDef.name}!",
      )
  }

  fun step(dtRaw: Float) {
    if (state.isPaused || state.outcome != null) return
    val dt = dtRaw.coerceIn(0.001f, 0.05f)

    val tiles = state.tiles.toMutableList()
    val obstacles = state.obstacles.toMutableList()
    var enemies = state.enemies.toMutableList()
    val eggs = state.eggs.toMutableList()
    val resources = state.resourceNodes.toMutableList()
    val pods = state.rescuePods.toMutableList()
    val effects =
      state.effects
        .map { it.copy(life = it.life - dt) }
        .filter { it.life > 0f }
        .toMutableList()

    val collectedRes = state.collectedResources.toMutableMap()
    val collectedEggs = state.collectedEggs.toMutableMap()
    var banner = state.statusBanner

    // 1. Update Whistle Sonar Ring
    var whistleRadius = state.whistleRadius
    if (state.whistleActive) {
      whistleRadius = (whistleRadius + dt * 7.5f).coerceAtMost(4.8f)
    }

    // 2. Move Leaders
    val activeEl = state.activeLeaderElement
    val updatedLeaders =
      state.leaders.map { leader ->
        if (leader.hp <= 0f) return@map leader
        val nextCd1 = (leader.primaryCooldown - dt).coerceAtLeast(0f)
        val nextCd2 = (leader.secondaryCooldown - dt).coerceAtLeast(0f)
        val nextAtkCd = (leader.attackCooldown - dt).coerceAtLeast(0f)

        var vx = 0f
        var vy = 0f
        val speed = 4.3f
        if (leader.element == activeEl) {
          if (state.controlMode == ControlMode.LEADER_DIRECT) {
            vx = joystickX * speed
            vy = joystickY * speed
          } else {
            val wp = leaderWaypoint
            if (wp != null) {
              val dist = hypot(wp.first - leader.x, wp.second - leader.y)
              if (dist < 0.25f) {
                leaderWaypoint = null
              } else {
                val (sx, sy) = steerToward(leader.x, leader.y, wp.first, wp.second, tiles)
                vx = sx * speed
                vy = sy * speed
              }
            }
          }
        } else {
          // Inactive leaders gently follow the active leader
          val active = state.activeLeader
          if (active != null) {
            val dist = hypot(active.x - leader.x, active.y - leader.y)
            if (dist > 1.5f) {
              val (sx, sy) = steerToward(leader.x, leader.y, active.x, active.y, tiles)
              vx = sx * 3.4f
              vy = sy * 3.4f
            }
          }
        }

        val (nx, ny) = tryMove(leader.x, leader.y, vx * dt, vy * dt, 0.34f, tiles)
        val nextAngle =
          if (hypot(vx, vy) > 0.1f) atan2(vy, vx) else leader.facingAngle
        leader.copy(
          x = nx,
          y = ny,
          vx = vx,
          vy = vy,
          primaryCooldown = nextCd1,
          secondaryCooldown = nextCd2,
          attackCooldown = nextAtkCd,
          facingAngle = nextAngle,
        )
      }.toMutableList()

    val activeLeader =
      updatedLeaders.firstOrNull { it.element == activeEl && it.hp > 0f }
        ?: updatedLeaders.firstOrNull { it.hp > 0f }

    // 3. Weather periodic effects
    weatherPulseTimer += dt
    val applyWeatherTick = weatherPulseTimer >= 3.2f
    if (applyWeatherTick) weatherPulseTimer = 0f

    // 4. Update Soldiers (Pikmin/Lemmings autonomous task execution + boid separation)
    val newlyHatched = mutableListOf<SoldierUnit>()
    val updatedSoldiers = mutableListOf<SoldierUnit>()

    for (soldier in state.soldiers) {
      if (soldier.hp <= 0f) continue
      var s = soldier.copy(attackCooldown = (soldier.attackCooldown - dt).coerceAtLeast(0f))

      // Whistle recall pulls units back to leader
      if (state.whistleActive && activeLeader != null &&
        hypot(s.x - activeLeader.x, s.y - activeLeader.y) <= whistleRadius
      ) {
        s =
          s.copy(
            state = SoldierState.FOLLOWING,
            targetId = null,
            hp = (s.hp + 6f * dt).coerceAtMost(s.maxHp),
          )
      }

      // Weather interactions
      if (applyWeatherTick) {
        when (state.sector.weather) {
          WeatherCondition.BRINE_RAIN -> {
            if (s.element != ElementType.HYDRO) {
              val dmg = 3f * (1f - s.weatherResist)
              s = s.copy(hp = (s.hp - dmg).coerceAtLeast(1f))
            }
          }
          WeatherCondition.ION_SURGE -> {
            if (s.element != ElementType.VOLT) {
              val dmg = 3f * (1f - s.weatherResist)
              s = s.copy(hp = (s.hp - dmg).coerceAtLeast(1f))
            }
          }
          else -> Unit
        }
      }

      val weatherSpeedMult =
        if (state.sector.weather == WeatherCondition.ION_SURGE && s.element == ElementType.VOLT) {
          1.25f
        } else 1f

      val weatherClearMult =
        if (state.sector.weather == WeatherCondition.THERMAL_UPDRAFT && s.element == ElementType.PYRO) {
          1.3f
        } else 1f

      var moveTargetX = s.x
      var moveTargetY = s.y
      var shouldMove = false

      when (s.state) {
        SoldierState.FOLLOWING -> {
          if (activeLeader != null) {
            val d = hypot(activeLeader.x - s.x, activeLeader.y - s.y)
            if (d > 1.25f) {
              moveTargetX = activeLeader.x
              moveTargetY = activeLeader.y
              shouldMove = true
            } else {
              // Auto-assist nearby matching tasks when in range!
              val nearbyObs =
                obstacles.firstOrNull {
                  it.requiredElement == s.element &&
                    hypot(it.tileX + 0.5f - s.x, it.tileY + 0.5f - s.y) < 1.6f
                }
              if (nearbyObs != null) {
                s =
                  s.copy(
                    state = SoldierState.CLEARING_OBSTACLE,
                    targetX = nearbyObs.tileX + 0.5f,
                    targetY = nearbyObs.tileY + 0.5f,
                    targetId = nearbyObs.id,
                  )
              }
            }
          }
        }
        SoldierState.CLEARING_OBSTACLE -> {
          val obsIdx = obstacles.indexOfFirst { it.id == s.targetId }
          if (obsIdx == -1) {
            s = s.copy(state = SoldierState.FOLLOWING, targetId = null)
          } else {
            val obs = obstacles[obsIdx]
            val ox = obs.tileX + 0.5f
            val oy = obs.tileY + 0.5f
            val dist = hypot(ox - s.x, oy - s.y)
            if (dist > 1.15f) {
              moveTargetX = ox
              moveTargetY = oy
              shouldMove = true
            } else {
              val nextHp = obs.hp - dt * s.clearSpeedMult * weatherClearMult
              if (nextHp <= 0f) {
                tiles[obs.tileY * state.gridWidth + obs.tileX] = TileType.FLOOR
                obstacles.removeAt(obsIdx)
                effects.add(
                  VisualEffect(
                    id = nextEffectId(),
                    kind = EffectKind.ABILITY_BLAST,
                    x = ox,
                    y = oy,
                    radius = 1.2f,
                    element = obs.requiredElement,
                    life = 0.4f,
                    maxLife = 0.4f,
                  )
                )
                banner = "${obs.tileType.obstacleLabel()} cleared!"
                s = s.copy(state = SoldierState.FOLLOWING, targetId = null)
              } else {
                obstacles[obsIdx] = obs.copy(hp = nextHp)
              }
            }
          }
        }
        SoldierState.HATCHING -> {
          val eggIdx = eggs.indexOfFirst { it.id == s.targetId && !it.hatched }
          if (eggIdx == -1) {
            s = s.copy(state = SoldierState.FOLLOWING, targetId = null)
          } else {
            val egg = eggs[eggIdx]
            val dist = hypot(egg.x - s.x, egg.y - s.y)
            if (dist > 1.0f) {
              moveTargetX = egg.x
              moveTargetY = egg.y
              shouldMove = true
            } else {
              val boost = if (s.element == egg.element) 1.5f else 0.7f
              val nextProg = egg.progress + dt * boost
              if (nextProg >= egg.requiredProgress) {
                eggs[eggIdx] = egg.copy(progress = egg.requiredProgress, hatched = true)
                collectedEggs[egg.element] = (collectedEggs[egg.element] ?: 0) + 1
                val unlocked = labStates[egg.element]?.unlockedUpgrades ?: emptyList()
                repeat(2) { idx ->
                  spawnedUnitCounter++
                  newlyHatched.add(
                    DungeonGenerator.createSoldier(
                      id = "hatch_${egg.element.id}_$spawnedUnitCounter",
                      element = egg.element,
                      x = egg.x + (idx - 0.5f) * 0.4f,
                      y = egg.y + 0.3f,
                      unlockedUpgrades = unlocked,
                    )
                  )
                }
                effects.add(
                  VisualEffect(
                    id = nextEffectId(),
                    kind = EffectKind.HATCH_BURST,
                    x = egg.x,
                    y = egg.y,
                    radius = 1.3f,
                    element = egg.element,
                    life = 0.6f,
                    maxLife = 0.6f,
                    label = "+2 ${egg.element.unitName}s!",
                  )
                )
                banner = "Hatched +2 ${egg.element.unitName}s from field egg!"
                s = s.copy(state = SoldierState.FOLLOWING, targetId = null)
              } else {
                eggs[eggIdx] = egg.copy(progress = nextProg)
              }
            }
          }
        }
        SoldierState.HARVESTING -> {
          val resIdx = resources.indexOfFirst { it.id == s.targetId && it.amountLeft > 0 }
          if (resIdx == -1) {
            s = s.copy(state = SoldierState.FOLLOWING, targetId = null)
          } else {
            val node = resources[resIdx]
            val dist = hypot(node.x - s.x, node.y - s.y)
            if (dist > 1.0f) {
              moveTargetX = node.x
              moveTargetY = node.y
              shouldMove = true
            } else {
              val nextProg = node.harvestProgress + dt * s.clearSpeedMult
              if (nextProg >= 1.5f) {
                val left = node.amountLeft - 1
                resources[resIdx] = node.copy(amountLeft = left, harvestProgress = 0f)
                collectedRes[node.resourceType] = (collectedRes[node.resourceType] ?: 0) + 1
                effects.add(
                  VisualEffect(
                    id = nextEffectId(),
                    kind = EffectKind.FLOATING_TEXT,
                    x = node.x,
                    y = node.y - 0.4f,
                    radius = 0f,
                    element = node.resourceType.element,
                    life = 0.85f,
                    maxLife = 0.85f,
                    label = "+1 ${node.resourceType.shortLabel}",
                  )
                )
                if (left <= 0) {
                  s = s.copy(state = SoldierState.FOLLOWING, targetId = null)
                }
              } else {
                resources[resIdx] = node.copy(harvestProgress = nextProg)
              }
            }
          }
        }
        SoldierState.FIGHTING -> {
          val enemyIdx = enemies.indexOfFirst { it.id == s.targetId && it.hp > 0f }
          if (enemyIdx == -1) {
            s = s.copy(state = SoldierState.FOLLOWING, targetId = null)
          } else {
            val enemy = enemies[enemyIdx]
            val dist = hypot(enemy.x - s.x, enemy.y - s.y)
            if (dist > enemy.radius + 0.55f) {
              moveTargetX = enemy.x
              moveTargetY = enemy.y
              shouldMove = true
            } else if (s.attackCooldown <= 0f) {
              val dmg = s.attackPower
              val stun = if (s.element == ElementType.VOLT && s.hasBonusCombatEffect) 0.5f else 0f
              enemies[enemyIdx] =
                enemy.copy(
                  hp = (enemy.hp - dmg).coerceAtLeast(0f),
                  stunTimer = max(enemy.stunTimer, stun),
                  hitFlash = 0.15f,
                )
              s = s.copy(attackCooldown = 0.65f)
            }
          }
        }
        SoldierState.MOVING_TO_POINT, SoldierState.IDLE -> {
          val dist = hypot(s.targetX - s.x, s.targetY - s.y)
          if (dist > 0.35f) {
            moveTargetX = s.targetX
            moveTargetY = s.targetY
            shouldMove = true
          }
        }
      }

      if (shouldMove) {
        val (sx, sy) = steerToward(s.x, s.y, moveTargetX, moveTargetY, tiles)
        val spd = s.moveSpeed * weatherSpeedMult
        val (nx, ny) = tryMove(s.x, s.y, sx * spd * dt, sy * spd * dt, 0.22f, tiles)
        s = s.copy(x = nx, y = ny, vx = sx * spd, vy = sy * spd)
      }

      updatedSoldiers.add(s)
    }

    updatedSoldiers.addAll(newlyHatched)

    // Remove dead enemies
    enemies = enemies.filter { it.hp > 0f }.toMutableList()

    // 5. Update Enemies & Boss Telegraph Attacks
    for (i in enemies.indices) {
      var e = enemies[i].copy(
        hitFlash = (enemies[i].hitFlash - dt).coerceAtLeast(0f),
        attackCooldown = (enemies[i].attackCooldown - dt).coerceAtLeast(0f),
        stunTimer = (enemies[i].stunTimer - dt).coerceAtLeast(0f),
      )
      if (e.stunTimer > 0f) {
        enemies[i] = e
        continue
      }

      // Find nearest target (leader or soldier)
      val nearestLeader =
        updatedLeaders.filter { it.hp > 0f }.minByOrNull { hypot(it.x - e.x, it.y - e.y) }
      val nearestSoldier =
        updatedSoldiers.filter { it.hp > 0f }.minByOrNull { hypot(it.x - e.x, it.y - e.y) }

      val leaderDist = nearestLeader?.let { hypot(it.x - e.x, it.y - e.y) } ?: 999f
      val soldierDist = nearestSoldier?.let { hypot(it.x - e.x, it.y - e.y) } ?: 999f
      val targetDist = min(leaderDist, soldierDist)

      if (e.kind.isBoss && e.telegraphTimer > 0f) {
        val nextT = e.telegraphTimer + dt
        if (nextT >= e.telegraphMax) {
          // Boss AoE Slam detonates!
          effects.add(
            VisualEffect(
              id = nextEffectId(),
              kind = EffectKind.BOSS_SLAM,
              x = e.x,
              y = e.y,
              radius = e.telegraphRadius,
              element = ElementType.PYRO,
              life = 0.45f,
              maxLife = 0.45f,
            )
          )
          for (li in updatedLeaders.indices) {
            val l = updatedLeaders[li]
            if (l.hp > 0f && hypot(l.x - e.x, l.y - e.y) <= e.telegraphRadius) {
              updatedLeaders[li] = l.copy(hp = (l.hp - e.attackDamage).coerceAtLeast(0f))
            }
          }
          for (si in updatedSoldiers.indices) {
            val sol = updatedSoldiers[si]
            if (hypot(sol.x - e.x, sol.y - e.y) <= e.telegraphRadius) {
              updatedSoldiers[si] =
                sol.copy(hp = (sol.hp - e.attackDamage * 0.65f).coerceAtLeast(0f))
            }
          }
          e = e.copy(telegraphTimer = 0f, attackCooldown = 2.4f)
        } else {
          e = e.copy(telegraphTimer = nextT)
        }
      } else if (targetDist <= 5.2f) {
        val tx = if (leaderDist <= soldierDist) nearestLeader!!.x else nearestSoldier!!.x
        val ty = if (leaderDist <= soldierDist) nearestLeader!!.y else nearestSoldier!!.y
        if (targetDist > e.radius + 0.55f) {
          val (sx, sy) = steerToward(e.x, e.y, tx, ty, tiles)
          val spd = if (e.kind.isBoss) 1.6f else 2.0f
          val (nx, ny) = tryMove(e.x, e.y, sx * spd * dt, sy * spd * dt, e.radius * 0.6f, tiles)
          e = e.copy(x = nx, y = ny)
        } else if (e.attackCooldown <= 0f) {
          if (e.kind.isBoss) {
            e = e.copy(telegraphTimer = 0.01f)
          } else {
            if (leaderDist <= soldierDist && nearestLeader != null) {
              val li = updatedLeaders.indexOfFirst { it.element == nearestLeader.element }
              if (li >= 0) {
                updatedLeaders[li] =
                  updatedLeaders[li].copy(
                    hp = (updatedLeaders[li].hp - e.attackDamage).coerceAtLeast(0f)
                  )
              }
            } else if (nearestSoldier != null) {
              val si = updatedSoldiers.indexOfFirst { it.id == nearestSoldier.id }
              if (si >= 0) {
                updatedSoldiers[si] =
                  updatedSoldiers[si].copy(
                    hp = (updatedSoldiers[si].hp - e.attackDamage).coerceAtLeast(0f)
                  )
              }
            }
            e = e.copy(attackCooldown = 1.15f)
          }
        }
      }
      enemies[i] = e
    }

    // 6. Auto-Rescue Pods when Leader or Soldier is within 1.45f tiles
    for (pi in pods.indices) {
      val pod = pods[pi]
      if (pod.rescued) continue
      val unitNearby =
        updatedLeaders.any { it.hp > 0f && hypot(it.x - pod.x, it.y - pod.y) <= 1.45f } ||
          updatedSoldiers.any { it.hp > 0f && hypot(it.x - pod.x, it.y - pod.y) <= 1.35f }
      if (unitNearby) {
        val nextProg = pod.rescueProgress + dt * 1.4f
        if (nextProg >= 2.0f) {
          pods[pi] = pod.copy(rescueProgress = 2.0f, rescued = true)
          effects.add(
            VisualEffect(
              id = nextEffectId(),
              kind = EffectKind.HATCH_BURST,
              x = pod.x,
              y = pod.y,
              radius = 1.4f,
              element = ElementType.HYDRO,
              life = 0.7f,
              maxLife = 0.7f,
              label = "${pod.callsign} Rescued!",
            )
          )
          banner = "Rescued ${pod.callsign}!"
        } else {
          pods[pi] = pod.copy(rescueProgress = nextProg)
        }
      }
    }

    // 7. Auto-collect nearby resources when active leader walks over them
    if (activeLeader != null) {
      for (ri in resources.indices) {
        val node = resources[ri]
        if (node.amountLeft > 0 && hypot(node.x - activeLeader.x, node.y - activeLeader.y) <= 1.05f) {
          resources[ri] = node.copy(amountLeft = 0)
          collectedRes[node.resourceType] =
            (collectedRes[node.resourceType] ?: 0) + node.amountLeft
          effects.add(
            VisualEffect(
              id = nextEffectId(),
              kind = EffectKind.FLOATING_TEXT,
              x = node.x,
              y = node.y - 0.5f,
              radius = 0f,
              element = node.resourceType.element,
              life = 0.9f,
              maxLife = 0.9f,
              label = "+${node.amountLeft} ${node.resourceType.shortLabel}",
            )
          )
        }
      }
    }

    // 8. Check Exit & Win/Lose Conditions
    val allPodsRescued = pods.all { it.rescued }
    val bossCleared = !state.sector.hasBoss || enemies.none { it.kind.isBoss }
    val exitUnlocked = allPodsRescued && bossCleared

    val nextTimeRemaining = (state.timeRemaining - dt).coerceAtLeast(0f)
    val nextElapsed = state.elapsedSeconds + dt
    val allLeadersDown = updatedLeaders.all { it.hp <= 0f }
    val reachedExit =
      exitUnlocked &&
        activeLeader != null &&
        hypot(activeLeader.x - state.exitX, activeLeader.y - state.exitY) <= 1.25f

    var outcome: ExpeditionOutcome? = null
    if (reachedExit || nextTimeRemaining <= 0f || allLeadersDown) {
      val victory = reachedExit
      val rescuedCount = pods.count { it.rescued }
      val ratio = rescuedCount.toFloat() / max(1, pods.size).toFloat()
      val rank =
        when {
          !victory -> "F"
          ratio >= 1f && nextTimeRemaining >= state.sector.timeLimitSeconds * 0.45f -> "S"
          ratio >= 1f -> "A"
          ratio >= 0.5f -> "B"
          else -> "C"
        }
      val xpEarned =
        (rescuedCount * 45) +
          (if (bossCleared && state.sector.hasBoss) 90 else 0) +
          (if (victory) 60 else 20)

      outcome =
        ExpeditionOutcome(
          sector = state.sector,
          victory = victory,
          rescuedCount = rescuedCount,
          totalPods = pods.size,
          bossDefeated = bossCleared && state.sector.hasBoss,
          durationSeconds = nextElapsed.toInt(),
          collectedResources = collectedRes.toMap(),
          collectedEggs = collectedEggs.toMap(),
          deployedElements = state.leaders.map { it.element }.toSet(),
          xpEarned = xpEarned,
          rank = rank,
        )
    }

    val nextActiveEl =
      if (updatedLeaders.any { it.element == activeEl && it.hp > 0f }) activeEl
      else updatedLeaders.firstOrNull { it.hp > 0f }?.element ?: activeEl

    state =
      state.copy(
        tiles = tiles,
        obstacles = obstacles,
        leaders = updatedLeaders,
        activeLeaderElement = nextActiveEl,
        soldiers = updatedSoldiers.filter { it.hp > 0f },
        eggs = eggs,
        resourceNodes = resources,
        rescuePods = pods,
        enemies = enemies,
        effects = effects,
        whistleRadius = whistleRadius,
        timeRemaining = nextTimeRemaining,
        elapsedSeconds = nextElapsed,
        collectedResources = collectedRes,
        collectedEggs = collectedEggs,
        statusBanner = banner,
        exitUnlocked = exitUnlocked,
        outcome = outcome,
        frameTick = state.frameTick + 1L,
      )
  }

  private fun isWalkable(tx: Int, ty: Int): Boolean {
    if (tx < 0 || ty < 0 || tx >= state.gridWidth || ty >= state.gridHeight) return false
    return state.tiles[ty * state.gridWidth + tx].walkableByDefault
  }

  private fun isPointClear(x: Float, y: Float, radius: Float, tiles: List<TileType>): Boolean {
    val corners =
      listOf(
        (x - radius) to (y - radius),
        (x + radius) to (y - radius),
        (x - radius) to (y + radius),
        (x + radius) to (y + radius),
      )
    return corners.all { (cx, cy) ->
      val tx = cx.toInt()
      val ty = cy.toInt()
      tx in 0 until state.gridWidth &&
        ty in 0 until state.gridHeight &&
        tiles[ty * state.gridWidth + tx].walkableByDefault
    }
  }

  private fun tryMove(
    x: Float,
    y: Float,
    dx: Float,
    dy: Float,
    radius: Float,
    tiles: List<TileType>,
  ): Pair<Float, Float> {
    var nx = x
    var ny = y
    if (isPointClear(x + dx, y, radius, tiles)) nx += dx
    if (isPointClear(nx, y + dy, radius, tiles)) ny += dy
    return nx to ny
  }

  private fun steerToward(
    fromX: Float,
    fromY: Float,
    toX: Float,
    toY: Float,
    tiles: List<TileType>,
  ): Pair<Float, Float> {
    val directDist = hypot(toX - fromX, toY - fromY)
    if (directDist < 0.05f) return 0f to 0f

    val startTx = fromX.toInt().coerceIn(0, state.gridWidth - 1)
    val startTy = fromY.toInt().coerceIn(0, state.gridHeight - 1)
    val goalTx = toX.toInt().coerceIn(0, state.gridWidth - 1)
    val goalTy = toY.toInt().coerceIn(0, state.gridHeight - 1)

    if (startTx == goalTx && startTy == goalTy) {
      return ((toX - fromX) / directDist) to ((toY - fromY) / directDist)
    }

    // Fast BFS on small grid to navigate smoothly around cavern corners
    val w = state.gridWidth
    val h = state.gridHeight
    val parent = IntArray(w * h) { -1 }
    val visited = BooleanArray(w * h)
    val queue = ArrayDeque<Int>()
    val startIdx = startTy * w + startTx
    val goalIdx = goalTy * w + goalTx
    visited[startIdx] = true
    queue.add(startIdx)

    val dirs = intArrayOf(1, 0, -1, 0, 0, 1, 0, -1)
    var found = false
    while (queue.isNotEmpty()) {
      val curr = queue.removeFirst()
      if (curr == goalIdx) {
        found = true
        break
      }
      val cx = curr % w
      val cy = curr / w
      for (d in 0 until 4) {
        val nx = cx + dirs[d * 2]
        val ny = cy + dirs[d * 2 + 1]
        if (nx in 0 until w && ny in 0 until h) {
          val nIdx = ny * w + nx
          if (!visited[nIdx] && (tiles[nIdx].walkableByDefault || nIdx == goalIdx)) {
            visited[nIdx] = true
            parent[nIdx] = curr
            queue.add(nIdx)
          }
        }
      }
    }

    if (!found) {
      return ((toX - fromX) / directDist) to ((toY - fromY) / directDist)
    }

    var stepIdx = goalIdx
    while (parent[stepIdx] != -1 && parent[stepIdx] != startIdx) {
      stepIdx = parent[stepIdx]
    }
    val targetX = (stepIdx % w) + 0.5f
    val targetY = (stepIdx / w) + 0.5f
    val d = hypot(targetX - fromX, targetY - fromY).coerceAtLeast(0.01f)
    return ((targetX - fromX) / d) to ((targetY - fromY) / d)
  }

  private fun nextEffectId(): String {
    effectCounter++
    return "fx_$effectCounter"
  }
}
