package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.CommanderProfile
import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.LeaderState
import com.example.game.SectorCatalog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BaseHubScreenContent
import com.example.ui.screens.ExpeditionScreen
import com.example.ui.theme.CavernautsTheme
import com.example.ui.viewmodel.BaseUiState
import com.example.ui.viewmodel.ExpeditionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdaptiveFoldableLayoutTest {

  @get:Rule val composeRule = createComposeRule()

  private data class FormFactor(val name: String, val width: Dp, val height: Dp)

  private val targetFormFactors =
    listOf(
      FormFactor("Fold6_Cover_Portrait", 344.dp, 820.dp),
      FormFactor("Fold6_Cover_Landscape", 820.dp, 344.dp),
      FormFactor("Fold6_Inner_Portrait", 674.dp, 794.dp),
      FormFactor("Fold6_Inner_Landscape", 794.dp, 674.dp),
      FormFactor("Large_Tablet_Portrait", 800.dp, 1200.dp),
      FormFactor("Large_Tablet_Landscape", 1200.dp, 800.dp),
    )

  @Test
  fun authScreen_displaysSignInButtonAcrossAllFold6AndTabletTransitions() {
    var activeFormFactor by mutableStateOf(targetFormFactors.first())

    composeRule.setContent {
      CavernautsTheme {
        Box(modifier = Modifier.size(activeFormFactor.width, activeFormFactor.height)) {
          AuthScreen(onAuthSuccess = {})
        }
      }
    }

    targetFormFactors.forEach { ff ->
      activeFormFactor = ff
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("google_sign_in_button").assertExists().assertIsDisplayed()
      composeRule.onNodeWithTag("play_offline_button").assertExists().assertIsDisplayed()
    }
  }

  @Test
  fun prepareSquad_opensDeploymentDialog_andLaunchConfirmsSelectedSquad() {
    val readyState = sampleBaseReadyState()
    val procedural = SectorCatalog.generateProceduralSector(seed = 4242)
    var launchedSector: com.example.game.SectorSpec? = null
    var launchedElements: Set<ElementType> = emptySet()
    var launchedCounts: Map<ElementType, Int> = emptyMap()

    composeRule.setContent {
      CavernautsTheme {
        Box(modifier = Modifier.size(390.dp, 844.dp)) {
          BaseHubScreenContent(
            uiState = readyState,
            proceduralSector = procedural,
            banner = null,
            onClearBanner = {},
            onRerollSeed = {},
            onPurchaseUpgrade = { _, _ -> },
            onSynthesizeEgg = {},
            onTrainLeader = {},
            onLaunchExpedition = { sector, elements, counts ->
              launchedSector = sector
              launchedElements = elements
              launchedCounts = counts
            },
            onSignOut = {},
          )
        }
      }
    }

    composeRule.onNodeWithTag("launch_sector_tut_01").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("SQUAD DEPLOYMENT").assertIsDisplayed()
    composeRule.onNodeWithTag("confirm_launch_button").assertIsDisplayed().performClick()
    composeRule.waitForIdle()

    assertNotNull(launchedSector)
    assertEquals("tut_01", launchedSector?.id)
    assertEquals(setOf(ElementType.PYRO), launchedElements)
    assertEquals(4, launchedCounts[ElementType.PYRO] ?: -1)
  }

  @Test
  fun baseHubScreen_adaptsNavAndTwoPaneAcrossFold6AndTabletTransitions() {
    val readyState = sampleBaseReadyState()
    val procedural = SectorCatalog.generateProceduralSector(seed = 4242)
    var activeFormFactor by mutableStateOf(targetFormFactors.first())

    composeRule.setContent {
      CavernautsTheme {
        Box(modifier = Modifier.size(activeFormFactor.width, activeFormFactor.height)) {
          BaseHubScreenContent(
            uiState = readyState,
            proceduralSector = procedural,
            banner = null,
            onClearBanner = {},
            onRerollSeed = {},
            onPurchaseUpgrade = { _, _ -> },
            onSynthesizeEgg = {},
            onTrainLeader = {},
            onLaunchExpedition = { _, _, _ -> },
            onSignOut = {},
          )
        }
      }
    }

    targetFormFactors.forEach { ff ->
      activeFormFactor = ff
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("nav_tab_sectors").assertExists().assertIsDisplayed().performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("nav_tab_labs").assertExists().assertIsDisplayed().performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("nav_tab_archives").assertExists().assertIsDisplayed().performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("sign_out_button").assertExists().assertIsDisplayed()
    }
  }

  @Test
  fun expeditionScreen_rendersHudAndControlsAcrossAllFold6AndTabletTransitions() {
    val sector = SectorCatalog.generateProceduralSector(seed = 9001)
    val allowed = sector.biome.allowedElements
    val leaders =
      ElementType.entries.associateWith { el ->
        LeaderState(elementId = el.id, name = el.leaderName, unlocked = true)
      }
    val labs =
      ElementType.entries.associateWith { el ->
        ElementalLabState(elementId = el.id, unlocked = true)
      }
    val vm =
      ExpeditionViewModel(
        sector = sector,
        deployedElements = allowed,
        leaderStates = leaders,
        labStates = labs,
        initialSquadCounts = allowed.associateWith { 4 },
        autoStartLoop = false,
      )

    var activeFormFactor by mutableStateOf(targetFormFactors.first())

    composeRule.setContent {
      CavernautsTheme {
        Box(modifier = Modifier.size(activeFormFactor.width, activeFormFactor.height)) {
          ExpeditionScreen(
            viewModel = vm,
            onAbortExpedition = {},
            onCommitAndExit = {},
          )
        }
      }
    }

    targetFormFactors.forEach { ff ->
      activeFormFactor = ff
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("pause_expedition_button").assertExists().assertIsDisplayed()
      composeRule.onNodeWithTag("toggle_control_mode_button").assertExists().assertIsDisplayed()
      composeRule.onNodeWithTag("whistle_rally_button").assertExists().assertIsDisplayed()

      composeRule.onNodeWithTag("toggle_control_mode_button").performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("leader_virtual_joystick").assertExists().assertIsDisplayed()
      composeRule.onNodeWithTag("leader_strike_button").assertExists().assertIsDisplayed()

      // Toggle back to squad mode for next pass
      composeRule.onNodeWithTag("toggle_control_mode_button").performClick()
      composeRule.waitForIdle()
    }
  }

  private fun sampleBaseReadyState(): BaseUiState.Ready {
    val profile =
      CommanderProfile(
        id = "test_user",
        userId = "test_user",
        callsign = "Pilot-Z6",
        highestTutorialCleared = 3,
      )
    val labs =
      ElementType.entries.associateWith { el ->
        ElementalLabState(elementId = el.id, userId = "test_user", unlocked = true, labTier = 2)
      }
    val leaders =
      ElementType.entries.associateWith { el ->
        LeaderState(
          elementId = el.id,
          userId = "test_user",
          name = el.leaderName,
          unlocked = true,
          level = 3,
        )
      }
    return BaseUiState.Ready(
      profile = profile,
      labs = labs,
      leaders = leaders,
      recentExpeditions = emptyList(),
    )
  }
}
