package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CommanderProfile
import com.example.data.model.ElementType
import com.example.data.model.ElementalLabState
import com.example.data.model.ExpeditionRecord
import com.example.data.model.LeaderState
import com.example.data.model.ResourceType
import com.example.data.model.UpgradeBranch
import com.example.data.model.UpgradeCatalog
import com.example.data.model.UpgradeNodeDefinition
import com.example.data.repository.GameProgressionRepository
import com.example.game.SectorCatalog
import com.example.game.SectorSpec
import com.example.ui.theme.BioCyan
import com.example.ui.theme.CavernAlloy
import com.example.ui.theme.CavernBasalt
import com.example.ui.theme.CavernBorder
import com.example.ui.theme.CavernObsidian
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltaicViolet
import com.example.ui.theme.accentColor
import com.example.ui.theme.surfaceTint
import com.example.ui.viewmodel.BaseProgressionViewModel
import com.example.ui.viewmodel.BaseUiState

enum class BaseTab(val label: String) {
  SECTORS("Sectors"),
  LABS("Base Labs"),
  ARCHIVES("Archives"),
}

@Composable
fun BaseHubScreen(
  viewModel: BaseProgressionViewModel,
  isOffline: Boolean = false,
  onLaunchExpedition: (SectorSpec, Set<ElementType>, Map<ElementType, Int>) -> Unit,
  onSignOut: () -> Unit,
  onLinkGoogleCloud: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val proceduralSector by viewModel.proceduralSector.collectAsStateWithLifecycle()
  val banner by viewModel.actionBanner.collectAsStateWithLifecycle()

  BaseHubScreenContent(
    uiState = uiState,
    proceduralSector = proceduralSector,
    banner = banner,
    isOffline = isOffline,
    onClearBanner = { viewModel.clearBanner() },
    onRerollSeed = { viewModel.rerollProceduralSector() },
    onPurchaseUpgrade = { el, node -> viewModel.purchaseUpgrade(el, node) },
    onSynthesizeEgg = { el -> viewModel.synthesizeEgg(el) },
    onTrainLeader = { el -> viewModel.trainLeader(el) },
    onLaunchExpedition = onLaunchExpedition,
    onSignOut = onSignOut,
    onLinkGoogleCloud = onLinkGoogleCloud,
  )
}

@Composable
internal fun BaseHubScreenContent(
  uiState: BaseUiState,
  proceduralSector: SectorSpec,
  banner: String?,
  isOffline: Boolean = false,
  onClearBanner: () -> Unit,
  onRerollSeed: () -> Unit,
  onPurchaseUpgrade: (ElementType, UpgradeNodeDefinition) -> Unit,
  onSynthesizeEgg: (ElementType) -> Unit,
  onTrainLeader: (ElementType) -> Unit,
  onLaunchExpedition: (SectorSpec, Set<ElementType>, Map<ElementType, Int>) -> Unit,
  onSignOut: () -> Unit,
  onLinkGoogleCloud: () -> Unit = {},
) {
  var selectedTab by remember { mutableStateOf(BaseTab.SECTORS) }
  var selectedLabElement by remember { mutableStateOf(ElementType.PYRO) }
  var pendingSquadSector by remember { mutableStateOf<SectorSpec?>(null) }
  var pendingRequirementsSector by remember { mutableStateOf<SectorSpec?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(banner) {
    if (banner != null) {
      snackbarHostState.showSnackbar(banner)
      onClearBanner()
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(CavernObsidian)) {
    val useNavigationRail = maxWidth >= 600.dp
    val useTwoPaneLayout = maxWidth >= 660.dp && maxHeight >= 460.dp
    val isNarrowCover = maxWidth < 380.dp

    Scaffold(
      containerColor = CavernObsidian,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar(
            containerColor = CavernBasalt,
            modifier = Modifier.navigationBarsPadding(),
          ) {
            BaseTab.entries.forEach { tab ->
              NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { selectedTab = tab },
                icon = {
                  Icon(
                    imageVector =
                      when (tab) {
                        BaseTab.SECTORS -> Icons.Filled.Explore
                        BaseTab.LABS -> Icons.Filled.Biotech
                        BaseTab.ARCHIVES -> Icons.Filled.History
                      },
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavigationRail) {
          NavigationRail(
            containerColor = CavernBasalt,
            modifier = Modifier.fillMaxHeight().padding(top = 12.dp),
          ) {
            BaseTab.entries.forEach { tab ->
              NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { selectedTab = tab },
                icon = {
                  Icon(
                    imageVector =
                      when (tab) {
                        BaseTab.SECTORS -> Icons.Filled.Explore
                        BaseTab.LABS -> Icons.Filled.Biotech
                        BaseTab.ARCHIVES -> Icons.Filled.History
                      },
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
              )
            }
          }
        }

        when (uiState) {
          is BaseUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = BioCyan)
                Spacer(Modifier.height(12.dp))
                Text("Loading Omninauts Base...", color = TextSecondary)
              }
            }
          }
          is BaseUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
              Text(text = uiState.message, color = DangerCrimson)
            }
          }
          is BaseUiState.Ready -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
              when (selectedTab) {
                BaseTab.SECTORS ->
                  SectorsTabContent(
                    profile = uiState.profile,
                    proceduralSector = proceduralSector,
                    useTwoPane = useTwoPaneLayout,
                    isNarrowCover = isNarrowCover,
                    isOffline = isOffline,
                    onRerollSeed = onRerollSeed,
                    onSelectSector = { sector -> pendingSquadSector = sector },
                    onShowRequirements = { sector -> pendingRequirementsSector = sector },
                    onLinkGoogleCloud = onLinkGoogleCloud,
                  )
                BaseTab.LABS ->
                  LabsTabContent(
                    profile = uiState.profile,
                    labs = uiState.labs,
                    leaders = uiState.leaders,
                    selectedElement = selectedLabElement,
                    useTwoPane = useTwoPaneLayout,
                    onSelectElement = { selectedLabElement = it },
                    onPurchaseUpgrade = onPurchaseUpgrade,
                    onSynthesizeEgg = onSynthesizeEgg,
                    onTrainLeader = onTrainLeader,
                  )
                BaseTab.ARCHIVES ->
                  ArchivesTabContent(
                    profile = uiState.profile,
                    expeditions = uiState.recentExpeditions,
                    useTwoPane = useTwoPaneLayout,
                    onSignOut = onSignOut,
                  )
              }
            }

            pendingSquadSector?.let { sector ->
              SquadDeploymentModal(
                sector = sector,
                profile = uiState.profile,
                leaders = uiState.leaders,
                onDismiss = { pendingSquadSector = null },
                onConfirmLaunch = { elements, counts ->
                  pendingSquadSector = null
                  onLaunchExpedition(sector, elements, counts)
                },
              )
            }

            pendingRequirementsSector?.let { sector ->
              val tutNum = sector.tutorialNumber ?: 1
              val unlocked = sector.id.startsWith("proc_") || (tutNum <= uiState.profile.highestTutorialCleared + 1)
              val lockHint = if (sector.id.startsWith("proc_")) "Complete Tutorial 01 to Unlock Scanner" else "Clear Tutorial 0${tutNum - 1} to unlock"
              SectorRequirementsDialog(
                sector = sector,
                isUnlocked = unlocked,
                lockHint = lockHint,
                onDismiss = { pendingRequirementsSector = null },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectorsTabContent(
  profile: CommanderProfile,
  proceduralSector: SectorSpec,
  useTwoPane: Boolean,
  isNarrowCover: Boolean,
  isOffline: Boolean = false,
  onRerollSeed: () -> Unit,
  onSelectSector: (SectorSpec) -> Unit,
  onShowRequirements: (SectorSpec) -> Unit,
  onLinkGoogleCloud: () -> Unit = {},
) {
  if (useTwoPane) {
    Row(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      LazyColumn(
        modifier = Modifier.weight(1.1f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          Text(
            text = "TUTORIAL EXPEDITIONS",
            style = MaterialTheme.typography.labelMedium,
            color = BioCyan,
          )
        }
        items(SectorCatalog.tutorialSectors) { sector ->
          val tutNum = sector.tutorialNumber ?: 1
          val unlocked = tutNum <= profile.highestTutorialCleared + 1
          val cleared = tutNum <= profile.highestTutorialCleared
          SectorCard(
            sector = sector,
            isUnlocked = unlocked,
            isCleared = cleared,
            lockHint = "Clear Tutorial 0${tutNum - 1} to unlock",
            onLaunch = { onSelectSector(sector) },
            onShowRequirements = { onShowRequirements(sector) },
          )
        }
      }

      LazyColumn(
        modifier = Modifier.weight(0.9f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item {
          CommanderHeaderCard(
            profile = profile,
            isNarrowCover = false,
            isOffline = isOffline,
            onLinkGoogleCloud = onLinkGoogleCloud,
          )
        }
        item {
          Text(
            text = "PROCEDURAL ANOMALY SCANNER",
            style = MaterialTheme.typography.labelMedium,
            color = VoltaicViolet,
          )
        }
        item {
          ProceduralScannerCard(
            profile = profile,
            proceduralSector = proceduralSector,
            onRerollSeed = onRerollSeed,
            onSelectSector = onSelectSector,
            onShowRequirements = onShowRequirements,
          )
        }
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(if (isNarrowCover) 12.dp else 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        CommanderHeaderCard(
          profile = profile,
          isNarrowCover = isNarrowCover,
          isOffline = isOffline,
          onLinkGoogleCloud = onLinkGoogleCloud,
        )
      }

      item {
        Text(
          text = "TUTORIAL EXPEDITIONS",
          style = MaterialTheme.typography.labelMedium,
          color = BioCyan,
        )
      }

      items(SectorCatalog.tutorialSectors) { sector ->
        val tutNum = sector.tutorialNumber ?: 1
        val unlocked = tutNum <= profile.highestTutorialCleared + 1
        val cleared = tutNum <= profile.highestTutorialCleared
        SectorCard(
          sector = sector,
          isUnlocked = unlocked,
          isCleared = cleared,
          lockHint = "Clear Tutorial 0${tutNum - 1} to unlock",
          onLaunch = { onSelectSector(sector) },
          onShowRequirements = { onShowRequirements(sector) },
        )
      }

      item {
        Spacer(Modifier.height(4.dp))
        Text(
          text = "PROCEDURAL ANOMALY SCANNER",
          style = MaterialTheme.typography.labelMedium,
          color = VoltaicViolet,
        )
      }

      item {
        ProceduralScannerCard(
          profile = profile,
          proceduralSector = proceduralSector,
          onRerollSeed = onRerollSeed,
          onSelectSector = onSelectSector,
          onShowRequirements = onShowRequirements,
        )
      }
    }
  }
}

@Composable
private fun ProceduralScannerCard(
  profile: CommanderProfile,
  proceduralSector: SectorSpec,
  onRerollSeed: () -> Unit,
  onSelectSector: (SectorSpec) -> Unit,
  onShowRequirements: (SectorSpec) -> Unit,
) {
  val proceduralUnlocked = profile.highestTutorialCleared >= 1
  Surface(
    color = CavernAlloy,
    shape = RoundedCornerShape(20.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(1.dp, VoltaicViolet.copy(alpha = 0.65f), RoundedCornerShape(20.dp)),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = proceduralSector.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${proceduralSector.biome.displayName} • ${proceduralSector.weather.displayName}",
            style = MaterialTheme.typography.bodyMedium,
            color = BioCyan,
          )
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
          onClick = onRerollSeed,
          modifier = Modifier.height(48.dp).testTag("reroll_seed_button"),
        ) {
          Icon(Icons.Filled.Casino, contentDescription = "Scan new seed", tint = VoltaicViolet)
          Spacer(Modifier.width(6.dp))
          Text("Scan")
        }
      }

      Text(
        text = proceduralSector.biome.restrictionNote,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = { if (proceduralUnlocked) onSelectSector(proceduralSector) else onShowRequirements(proceduralSector) },
          modifier = Modifier.weight(1f).height(50.dp).testTag("launch_procedural_button"),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (proceduralUnlocked) VoltaicViolet else CavernBasalt,
              contentColor = if (proceduralUnlocked) CavernObsidian else TextSecondary,
            ),
        ) {
          Icon(Icons.Filled.PlayArrow, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(
            text =
              if (proceduralUnlocked) "Deploy Squad (Seed #${proceduralSector.seed})"
              else "Locked (View Requirements)",
            fontWeight = FontWeight.Bold,
          )
        }

        OutlinedButton(
          onClick = { onShowRequirements(proceduralSector) },
          modifier = Modifier.size(50.dp).testTag("requirements_info_${proceduralSector.id}"),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(0.dp),
        ) {
          Icon(Icons.Filled.Info, contentDescription = "View requirements info", tint = VoltaicViolet)
        }
      }
    }
  }
}

@Composable
private fun CommanderHeaderCard(
  profile: CommanderProfile,
  isNarrowCover: Boolean,
  isOffline: Boolean = false,
  onLinkGoogleCloud: () -> Unit = {},
) {
  Surface(
    color = CavernBasalt,
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth().border(1.dp, CavernBorder, RoundedCornerShape(20.dp)),
  ) {
    Column(modifier = Modifier.padding(if (isNarrowCover) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "OMNINAUTS COMMAND BASE",
              style = MaterialTheme.typography.labelSmall,
              color = BioCyan,
            )
            if (isOffline) {
              Surface(color = BioCyan.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                Text(
                  text = "OFFLINE",
                  style = MaterialTheme.typography.labelSmall,
                  color = BioCyan,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = profile.callsign,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
          )
          Text(
            text = "Rescues: ${profile.totalRescues}  •  Bosses: ${profile.bossesDefeated}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
        if (isOffline) {
          OutlinedButton(
            onClick = onLinkGoogleCloud,
            modifier = Modifier.testTag("link_cloud_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Filled.CloudSync, contentDescription = "Sync to Google Cloud", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Sync Cloud", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ElementType.entries.forEach { el ->
          val accent = el.accentColor()
          Surface(
            color = accent.copy(alpha = 0.16f),
            shape = RoundedCornerShape(10.dp),
            modifier =
              Modifier.weight(1f).border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
          ) {
            Text(
              text =
                if (isNarrowCover) "${el.displayName}: ${profile.eggsFor(el)}"
                else "${el.displayName}: ${profile.eggsFor(el)} Eggs",
              style = MaterialTheme.typography.labelSmall,
              color = accent,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = if (isNarrowCover) 6.dp else 8.dp, vertical = 6.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SectorCard(
  sector: SectorSpec,
  isUnlocked: Boolean,
  isCleared: Boolean,
  lockHint: String,
  onLaunch: () -> Unit,
  onShowRequirements: () -> Unit,
) {
  Surface(
    color = CavernAlloy,
    shape = RoundedCornerShape(18.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          if (isCleared) SuccessEmerald.copy(alpha = 0.55f) else CavernBorder,
          RoundedCornerShape(18.dp),
        ),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = sector.name, style = MaterialTheme.typography.titleLarge)
          Text(text = sector.subtitle, style = MaterialTheme.typography.bodyMedium, color = BioCyan)
        }
        if (isCleared) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Sector cleared",
            tint = SuccessEmerald,
          )
        } else if (!isUnlocked) {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "Sector locked",
            tint = TextSecondary,
          )
        }
      }

      Text(
        text = "${sector.biome.displayName} • ${sector.weather.displayName}",
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
      )
      Text(
        text = sector.biome.restrictionNote,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = { if (isUnlocked) onLaunch() else onShowRequirements() },
          modifier = Modifier.weight(1f).height(48.dp).testTag("launch_sector_${sector.id}"),
          colors = ButtonDefaults.buttonColors(containerColor = if (isUnlocked) BioCyan else CavernBasalt, contentColor = if (isUnlocked) CavernObsidian else TextSecondary),
        ) {
          Text(
            text = if (isUnlocked) "Prepare Squad" else "Locked (View Requirements)",
            fontWeight = FontWeight.Bold,
          )
        }

        OutlinedButton(
          onClick = onShowRequirements,
          modifier = Modifier.size(48.dp).testTag("requirements_info_${sector.id}"),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(0.dp),
        ) {
          Icon(Icons.Filled.Info, contentDescription = "View requirements info", tint = BioCyan)
        }
      }
    }
  }
}

@Composable
private fun LabsTabContent(
  profile: CommanderProfile,
  labs: Map<ElementType, ElementalLabState>,
  leaders: Map<ElementType, LeaderState>,
  selectedElement: ElementType,
  useTwoPane: Boolean,
  onSelectElement: (ElementType) -> Unit,
  onPurchaseUpgrade: (ElementType, UpgradeNodeDefinition) -> Unit,
  onSynthesizeEgg: (ElementType) -> Unit,
  onTrainLeader: (ElementType) -> Unit,
) {
  val lab = labs[selectedElement]
  val leader = leaders[selectedElement]
  val accent = selectedElement.accentColor()

  if (useTwoPane) {
    Row(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      LazyColumn(
        modifier = Modifier.weight(0.95f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          LabSelectorRow(labs = labs, selectedElement = selectedElement, onSelectElement = onSelectElement)
        }
        item {
          LabHeaderCard(selectedElement = selectedElement, lab = lab, profile = profile)
        }
        if (lab?.unlocked == true && leader != null) {
          item {
            LeaderMasteryCard(
              selectedElement = selectedElement,
              leader = leader,
              accent = accent,
              onTrainLeader = onTrainLeader,
            )
          }
          item {
            EggIncubatorCard(
              selectedElement = selectedElement,
              profile = profile,
              accent = accent,
              onSynthesizeEgg = onSynthesizeEgg,
            )
          }
        }
      }

      LazyColumn(
        modifier = Modifier.weight(1.05f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (lab?.unlocked != true) {
          item { LockedLabNotice(selectedElement = selectedElement) }
        } else {
          UpgradeBranch.entries.forEach { branch ->
            val nodes = UpgradeCatalog.upgradesFor(selectedElement).filter { it.branch == branch }
            item {
              Text(
                text = "${branch.title.uppercase()} UPGRADE BRANCH",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
              )
            }
            items(nodes) { node ->
              UpgradeNodeCard(
                node = node,
                lab = lab,
                profile = profile,
                accent = accent,
                onPurchaseUpgrade = { onPurchaseUpgrade(selectedElement, node) },
              )
            }
          }
        }
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        LabSelectorRow(labs = labs, selectedElement = selectedElement, onSelectElement = onSelectElement)
      }
      item {
        LabHeaderCard(selectedElement = selectedElement, lab = lab, profile = profile)
      }
      if (lab?.unlocked != true) {
        item { LockedLabNotice(selectedElement = selectedElement) }
      } else {
        if (leader != null) {
          item {
            LeaderMasteryCard(
              selectedElement = selectedElement,
              leader = leader,
              accent = accent,
              onTrainLeader = onTrainLeader,
            )
          }
        }
        item {
          EggIncubatorCard(
            selectedElement = selectedElement,
            profile = profile,
            accent = accent,
            onSynthesizeEgg = onSynthesizeEgg,
          )
        }
        UpgradeBranch.entries.forEach { branch ->
          val nodes = UpgradeCatalog.upgradesFor(selectedElement).filter { it.branch == branch }
          item {
            Text(
              text = "${branch.title.uppercase()} UPGRADE BRANCH",
              style = MaterialTheme.typography.labelMedium,
              color = accent,
            )
          }
          items(nodes) { node ->
            UpgradeNodeCard(
              node = node,
              lab = lab,
              profile = profile,
              accent = accent,
              onPurchaseUpgrade = { onPurchaseUpgrade(selectedElement, node) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LabSelectorRow(
  labs: Map<ElementType, ElementalLabState>,
  selectedElement: ElementType,
  onSelectElement: (ElementType) -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    ElementType.entries.forEach { el ->
      val elAccent = el.accentColor()
      val selected = el == selectedElement
      val unlocked = labs[el]?.unlocked == true
      Surface(
        color = if (selected) elAccent.copy(alpha = 0.22f) else CavernBasalt,
        shape = RoundedCornerShape(14.dp),
        modifier =
          Modifier.weight(1f)
            .height(50.dp)
            .border(
              if (selected) 2.dp else 1.dp,
              if (selected) elAccent else CavernBorder,
              RoundedCornerShape(14.dp),
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelectElement(el) }
            .testTag("select_lab_${el.id}"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.padding(horizontal = 6.dp),
        ) {
          Icon(
            imageVector =
              when (el) {
                ElementType.PYRO -> Icons.Filled.LocalFireDepartment
                ElementType.HYDRO -> Icons.Filled.WaterDrop
                ElementType.VOLT -> Icons.Filled.Bolt
              },
            contentDescription = el.labName,
            tint = if (unlocked) elAccent else TextSecondary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(5.dp))
          Text(
            text = el.displayName,
            style = MaterialTheme.typography.labelLarge,
            color = if (unlocked) elAccent else TextSecondary,
          )
        }
      }
    }
  }
}

@Composable
private fun LabHeaderCard(
  selectedElement: ElementType,
  lab: ElementalLabState?,
  profile: CommanderProfile,
) {
  val accent = selectedElement.accentColor()
  val bannerRes =
    when (selectedElement) {
      ElementType.PYRO -> R.drawable.img_lab_pyro
      ElementType.HYDRO -> R.drawable.img_lab_hydro
      ElementType.VOLT -> R.drawable.img_lab_volt
    }
  Surface(
    color = selectedElement.surfaceTint(),
    shape = RoundedCornerShape(20.dp),
    modifier =
      Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        Image(
          painter = painterResource(id = bannerRes),
          contentDescription = selectedElement.labName,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(0f to Color.Transparent, 1f to CavernBasalt.copy(alpha = 0.92f))
              )
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
          Text(
            text = selectedElement.labName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
          )
          Text(
            text =
              if (lab?.unlocked == true) "Tier ${lab.labTier} Facility • ${selectedElement.unitName} Division"
              else "Facility Locked",
            style = MaterialTheme.typography.titleLarge,
          )
        }
      }

      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          selectedElement.specialtySummary,
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
        val themedResources = ResourceType.entries.filter { it.element == selectedElement }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          themedResources.forEach { res ->
            Surface(
              color = CavernObsidian.copy(alpha = 0.7f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(res.shortLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                  text = "${profile.resourceAmount(res)}",
                  style = MaterialTheme.typography.titleMedium,
                  color = accent,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LockedLabNotice(selectedElement: ElementType) {
  val requiredTutorial =
    when (selectedElement) {
      ElementType.PYRO -> 1
      ElementType.HYDRO -> 2
      ElementType.VOLT -> 3
    }
  Surface(
    color = CavernAlloy,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth().border(1.dp, CavernBorder, RoundedCornerShape(18.dp)),
  ) {
    Row(
      modifier = Modifier.padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary)
      Text(
        text = "Complete Tutorial Sector 0$requiredTutorial to activate ${selectedElement.labName} and unlock ${selectedElement.leaderName}.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
      )
    }
  }
}

@Composable
private fun LeaderMasteryCard(
  selectedElement: ElementType,
  leader: LeaderState,
  accent: Color,
  onTrainLeader: (ElementType) -> Unit,
) {
  Surface(
    color = CavernAlloy,
    shape = RoundedCornerShape(18.dp),
    modifier =
      Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("PERMANENT LEADER", style = MaterialTheme.typography.labelSmall, color = accent)
          Text("${leader.name} • Lv.${leader.level}", style = MaterialTheme.typography.titleLarge)
          Text(
            "HP ${leader.maxHp}  •  ATK ${leader.attackPower}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
        Button(
          onClick = { onTrainLeader(selectedElement) },
          enabled = leader.xp >= leader.xpForNextLevel && leader.level < 20,
          colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = CavernObsidian),
          modifier = Modifier.height(48.dp).testTag("train_leader_${selectedElement.id}"),
        ) {
          Icon(Icons.Filled.Upgrade, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text("Train")
        }
      }
      LinearProgressIndicator(
        progress = { (leader.xp.toFloat() / leader.xpForNextLevel.toFloat()).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = accent,
        trackColor = CavernObsidian,
      )
      Text(
        text = "XP ${leader.xp} / ${leader.xpForNextLevel} • Skills: ${leader.unlockedSkills.joinToString()}",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
    }
  }
}

@Composable
private fun EggIncubatorCard(
  selectedElement: ElementType,
  profile: CommanderProfile,
  accent: Color,
  onSynthesizeEgg: (ElementType) -> Unit,
) {
  val eggCost = GameProgressionRepository.eggSynthesisCost(selectedElement)
  val costText = eggCost.entries.joinToString(" + ") { "${it.value} ${it.key.shortLabel}" }
  val canSynth = eggCost.all { (res, amt) -> profile.resourceAmount(res) >= amt }
  Surface(
    color = CavernAlloy,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth().border(1.dp, CavernBorder, RoundedCornerShape(18.dp)),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${selectedElement.displayName} Egg Incubator (${profile.eggsFor(selectedElement)} stored)",
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = "Synthesize +1 deployable egg ($costText)",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }
      Spacer(Modifier.width(8.dp))
      OutlinedButton(
        onClick = { onSynthesizeEgg(selectedElement) },
        enabled = canSynth,
        modifier = Modifier.height(48.dp).testTag("synth_egg_${selectedElement.id}"),
      ) {
        Icon(Icons.Filled.Science, contentDescription = null, tint = accent)
        Spacer(Modifier.width(6.dp))
        Text("Hatch +1", color = accent)
      }
    }
  }
}

@Composable
private fun UpgradeNodeCard(
  node: UpgradeNodeDefinition,
  lab: ElementalLabState,
  profile: CommanderProfile,
  accent: Color,
  onPurchaseUpgrade: () -> Unit,
) {
  val isUnlocked = lab.unlockedUpgrades.contains(node.id)
  val prereqMet = node.prerequisiteId == null || lab.unlockedUpgrades.contains(node.prerequisiteId)
  val canAfford = node.costs.all { (res, cost) -> profile.resourceAmount(res) >= cost }
  val costSummary = node.costs.entries.joinToString(" • ") { "${it.value} ${it.key.shortLabel}" }

  Surface(
    color = if (isUnlocked) accent.copy(alpha = 0.14f) else CavernAlloy,
    shape = RoundedCornerShape(16.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(1.dp, if (isUnlocked) accent else CavernBorder, RoundedCornerShape(16.dp)),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("T${node.tier}: ${node.title}", style = MaterialTheme.typography.titleMedium)
          Surface(color = accent.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
            Text(
              text = node.statBadge,
              style = MaterialTheme.typography.labelSmall,
              color = accent,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
        Text(node.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        if (!isUnlocked) {
          Text("Cost: $costSummary", style = MaterialTheme.typography.labelSmall, color = accent)
        }
      }
      Spacer(Modifier.width(10.dp))
      if (isUnlocked) {
        Icon(Icons.Filled.CheckCircle, contentDescription = "Unlocked", tint = SuccessEmerald)
      } else {
        Button(
          onClick = onPurchaseUpgrade,
          enabled = prereqMet && canAfford,
          colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = CavernObsidian),
          modifier = Modifier.height(48.dp).testTag("upgrade_${node.id}"),
        ) {
          Text(if (prereqMet) "Research" else "Locked", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun ArchivesTabContent(
  profile: CommanderProfile,
  expeditions: List<ExpeditionRecord>,
  useTwoPane: Boolean,
  onSignOut: () -> Unit,
) {
  if (useTwoPane) {
    Row(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      LazyColumn(
        modifier = Modifier.weight(0.95f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item { ArchivesHeaderRow(profile = profile, onSignOut = onSignOut) }
        item { ResourceStockpileCard(profile = profile) }
      }
      LazyColumn(
        modifier = Modifier.weight(1.05f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Text("RECENT EXPEDITION LOGS", style = MaterialTheme.typography.labelMedium, color = BioCyan)
        }
        if (expeditions.isEmpty()) {
          item { EmptyExpeditionLogCard() }
        } else {
          items(expeditions) { rec -> ExpeditionHistoryCard(rec) }
        }
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item { ArchivesHeaderRow(profile = profile, onSignOut = onSignOut) }
      item { ResourceStockpileCard(profile = profile) }
      item {
        Text("RECENT EXPEDITION LOGS", style = MaterialTheme.typography.labelMedium, color = BioCyan)
      }
      if (expeditions.isEmpty()) {
        item { EmptyExpeditionLogCard() }
      } else {
        items(expeditions) { rec -> ExpeditionHistoryCard(rec) }
      }
    }
  }
}

@Composable
private fun ArchivesHeaderRow(profile: CommanderProfile, onSignOut: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text("COMMANDER ARCHIVES", style = MaterialTheme.typography.labelMedium, color = BioCyan)
      Text("${profile.callsign} • ${profile.totalExpeditions} Runs", style = MaterialTheme.typography.headlineSmall)
    }
    OutlinedButton(
      onClick = onSignOut,
      modifier = Modifier.height(48.dp).testTag("sign_out_button"),
    ) {
      Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
      Spacer(Modifier.width(6.dp))
      Text("Sign Out")
    }
  }
}

@Composable
private fun ResourceStockpileCard(profile: CommanderProfile) {
  Surface(
    color = CavernAlloy,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth().border(1.dp, CavernBorder, RoundedCornerShape(18.dp)),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("ELEMENTAL SUPPLY STOCKPILE", style = MaterialTheme.typography.labelSmall, color = BioCyan)
      ResourceType.entries.chunked(3).forEach { rowItems ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          rowItems.forEach { res ->
            val accent = res.element.accentColor()
            Surface(
              color = CavernBasalt,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(res.shortLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("${profile.resourceAmount(res)}", style = MaterialTheme.typography.titleMedium, color = accent)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyExpeditionLogCard() {
  Surface(
    color = CavernAlloy,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = "No expeditions recorded yet. Launch Tutorial Sector 01 to begin!",
      style = MaterialTheme.typography.bodyLarge,
      color = TextSecondary,
      modifier = Modifier.padding(20.dp),
    )
  }
}

@Composable
private fun ExpeditionHistoryCard(rec: ExpeditionRecord) {
  Surface(
    color = CavernAlloy,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth().border(1.dp, CavernBorder, RoundedCornerShape(14.dp)),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(rec.sectorName, style = MaterialTheme.typography.titleMedium)
        Text(
          text = "Seed #${rec.seed} • ${rec.biome} • Pods ${rec.rescuedCount}/${rec.totalPods}",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }
      Surface(
        color = (if (rec.victory) SuccessEmerald else DangerCrimson).copy(alpha = 0.2f),
        shape = CircleShape,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = rec.rank,
            style = MaterialTheme.typography.titleMedium,
            color = if (rec.victory) SuccessEmerald else DangerCrimson,
          )
        }
      }
    }
  }
}

@Composable
private fun SquadDeploymentModal(
  sector: SectorSpec,
  profile: CommanderProfile,
  leaders: Map<ElementType, LeaderState>,
  onDismiss: () -> Unit,
  onConfirmLaunch: (Set<ElementType>, Map<ElementType, Int>) -> Unit,
) {
  val counts = remember(sector.id) {
    mutableStateMapOf(
      ElementType.PYRO to 4,
      ElementType.HYDRO to 4,
      ElementType.VOLT to 4,
    )
  }
  val selected = remember(sector.id) {
    mutableStateMapOf<ElementType, Boolean>().apply {
      ElementType.entries.forEach { el ->
        val tutAllowed =
          when (sector.tutorialNumber) {
            1 -> el == ElementType.PYRO
            2 -> el == ElementType.PYRO || el == ElementType.HYDRO
            3 -> el == ElementType.HYDRO || el == ElementType.VOLT
            else -> (leaders[el]?.unlocked == true) || (el in sector.biome.allowedElements)
          }
        this[el] = el in sector.biome.allowedElements && tutAllowed
      }
      if (values.none { it }) {
        this[sector.biome.allowedElements.first()] = true
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = CavernAlloy,
      shape = RoundedCornerShape(24.dp),
      modifier =
        Modifier.widthIn(max = 480.dp)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 24.dp)
          .border(1.dp, BioCyan.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("SQUAD DEPLOYMENT", style = MaterialTheme.typography.labelMedium, color = BioCyan)
        Text(sector.name, style = MaterialTheme.typography.headlineSmall)
        Text(sector.biome.restrictionNote, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

        ElementType.entries.forEach { el ->
          val allowedBiome = el in sector.biome.allowedElements
          val unlocked =
            (leaders[el]?.unlocked == true) ||
              (sector.tutorialNumber == 2 && el == ElementType.HYDRO) ||
              (sector.tutorialNumber == 3 && el == ElementType.VOLT) ||
              (sector.tutorialNumber == 1 && el == ElementType.PYRO)
          val accent = el.accentColor()
          val isChosen = selected[el] == true
          val maxCap = (4 + profile.eggsFor(el)).coerceAtMost(10)

          Surface(
            color = if (isChosen) accent.copy(alpha = 0.16f) else CavernBasalt,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .border(1.dp, if (isChosen) accent else CavernBorder, RoundedCornerShape(14.dp))
                .clickable(enabled = allowedBiome && unlocked) { selected[el] = !isChosen },
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${el.leaderName} + ${el.unitName}s",
                  style = MaterialTheme.typography.titleMedium,
                  color = if (allowedBiome && unlocked) accent else TextSecondary,
                )
                Text(
                  text =
                    when {
                      !allowedBiome -> "Restricted by Biome"
                      !unlocked -> "Unlock in Tutorial"
                      else -> "Tap to ${if (isChosen) "Exclude" else "Deploy"}"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary,
                )
              }
              if (isChosen && allowedBiome && unlocked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { counts[el] = ((counts[el] ?: 4) - 1).coerceAtLeast(2) },
                    modifier = Modifier.size(48.dp),
                  ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease ${el.unitName} count")
                  }
                  Text("${counts[el] ?: 4}", style = MaterialTheme.typography.titleMedium, color = accent)
                  IconButton(
                    onClick = { counts[el] = ((counts[el] ?: 4) + 1).coerceAtMost(maxCap) },
                    modifier = Modifier.size(48.dp),
                  ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase ${el.unitName} count")
                  }
                }
              }
            }
          }
        }

        val activeElements = selected.filterValues { it }.keys
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(50.dp)) {
            Text("Cancel")
          }
          Button(
            onClick = { onConfirmLaunch(activeElements, counts.toMap()) },
            enabled = activeElements.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = BioCyan, contentColor = CavernObsidian),
            modifier = Modifier.weight(1f).height(50.dp).testTag("confirm_launch_button"),
          ) {
            Text("Launch", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun SectorRequirementsDialog(
  sector: SectorSpec,
  isUnlocked: Boolean,
  lockHint: String,
  onDismiss: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(CavernObsidian.copy(alpha = 0.82f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(16.dp)
        .clickable(onClick = onDismiss),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      color = CavernAlloy,
      shape = RoundedCornerShape(24.dp),
      modifier =
        Modifier.widthIn(max = 480.dp)
          .fillMaxWidth()
          .border(1.dp, BioCyan.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
          .clickable(onClick = {}),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("EXPEDITION REQUIREMENTS", style = MaterialTheme.typography.labelMedium, color = BioCyan)
          Icon(Icons.Filled.Info, contentDescription = null, tint = BioCyan)
        }
        Text(sector.name, style = MaterialTheme.typography.headlineSmall)
        Text(sector.subtitle, style = MaterialTheme.typography.bodyMedium, color = BioCyan)

        Surface(
          color = CavernBasalt,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().border(1.dp, CavernBorder, RoundedCornerShape(12.dp)),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Status: ${if (isUnlocked) "Unlocked & Ready" else "Locked"}", fontWeight = FontWeight.Bold, color = if (isUnlocked) SuccessEmerald else DangerCrimson)
            if (!isUnlocked) {
              Text("Missing Prerequisite: $lockHint", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            } else {
              Text("Prerequisites met. Ready for squad deployment.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
          }
        }

        Surface(
          color = CavernBasalt,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().border(1.dp, CavernBorder, RoundedCornerShape(12.dp)),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Environment & Biome", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Biome: ${sector.biome.displayName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("Weather: ${sector.weather.displayName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("Allowed Elements: ${sector.biome.allowedElements.joinToString { it.unitName + "s" }}", style = MaterialTheme.typography.bodyMedium, color = BioCyan)
            Text(sector.biome.restrictionNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
          }
        }

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = BioCyan, contentColor = CavernObsidian),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("close_requirements_dialog"),
        ) {
          Text("Got It", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
