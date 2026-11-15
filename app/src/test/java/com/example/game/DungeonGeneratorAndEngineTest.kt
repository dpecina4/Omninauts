package com.example.game

import com.example.data.model.BiomeType
import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.LeaderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DungeonGeneratorAndEngineTest {

  @Test
  fun proceduralDungeon_respectsBiomeRestrictionsAndGeneratesValidEntities() {
    val sector = SectorCatalog.generateProceduralSector(seed = 7777)
    val allElements = setOf(ElementType.PYRO, ElementType.HYDRO, ElementType.VOLT)
    val leaders =
      ElementType.entries.associateWith { el ->
        LeaderState(elementId = el.id, name = el.leaderName, unlocked = true)
      }
    val labs =
      ElementType.entries.associateWith { el ->
        ElementalLabState(elementId = el.id, unlocked = true)
      }

    val state =
      DungeonGenerator.generate(
        sector = sector,
        deployedElements = allElements,
        leaderStates = leaders,
        labStates = labs,
        initialSquadCounts = mapOf(ElementType.PYRO to 4, ElementType.HYDRO to 4, ElementType.VOLT to 4),
      )

    // Only biome-allowed elements should be deployed
    assertTrue(state.leaders.all { it.element in sector.biome.allowedElements })
    assertTrue(state.soldiers.all { it.element in sector.biome.allowedElements })
    assertTrue(state.rescuePods.isNotEmpty())
    assertTrue(state.enemies.any { it.kind.isBoss })
    assertTrue(state.obstacles.all { it.requiredElement in sector.biome.allowedElements })
  }

  @Test
  fun expeditionEngine_whistleAndCombatModeToggle_operateCorrectly() {
    val sector = SectorCatalog.tutorialSectors.first()
    val state =
      DungeonGenerator.generate(
        sector = sector,
        deployedElements = setOf(ElementType.PYRO),
        leaderStates = mapOf(ElementType.PYRO to LeaderState(elementId = "pyro", unlocked = true)),
        labStates = mapOf(ElementType.PYRO to ElementalLabState(elementId = "pyro", unlocked = true)),
        initialSquadCounts = mapOf(ElementType.PYRO to 4),
      )
    val engine = ExpeditionEngine(state, emptyMap())

    engine.setControlMode(ControlMode.LEADER_DIRECT)
    assertEquals(ControlMode.LEADER_DIRECT, engine.currentState().controlMode)

    engine.setJoystick(1f, 0f)
    repeat(10) { engine.step(0.016f) }
    assertTrue(engine.currentState().activeLeader!!.x > state.startX)

    engine.setControlMode(ControlMode.COMMANDER_SQUAD)
    engine.setWhistle(true)
    repeat(10) { engine.step(0.016f) }
    assertTrue(engine.currentState().whistleRadius > 1f)

    engine.setWhistle(false)
    assertFalse(engine.currentState().whistleActive)
  }

  @Test
  fun biomeRestrictions_excludeDisallowedElements() {
    assertFalse(ElementType.PYRO in BiomeType.DYNAMO_RUINS.allowedElements)
    assertFalse(ElementType.HYDRO in BiomeType.BASALT_RIFT.allowedElements)
    assertFalse(ElementType.VOLT in BiomeType.CRYSTAL_CISTERN.allowedElements)
  }
}
