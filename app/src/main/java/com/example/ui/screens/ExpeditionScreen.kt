package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ElementType
import com.example.data.model.UpgradeCatalog
import com.example.game.ControlMode
import com.example.game.ExpeditionOutcome
import com.example.game.ExpeditionUiState
import com.example.ui.theme.BioCyan
import com.example.ui.theme.CavernAlloy
import com.example.ui.theme.CavernBasalt
import com.example.ui.theme.CavernBorder
import com.example.ui.theme.CavernObsidian
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltGold
import com.example.ui.theme.accentColor
import com.example.ui.viewmodel.ExpeditionViewModel
import kotlin.math.hypot

@Composable
fun ExpeditionScreen(
  viewModel: ExpeditionViewModel,
  onAbortExpedition: () -> Unit,
  onCommitAndExit: (ExpeditionOutcome) -> Unit,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val haptics = LocalHapticFeedback.current
  var isSavingOutcome by remember { mutableStateOf(false) }

  BackHandler {
    if (state.outcome == null) {
      viewModel.togglePause()
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(CavernObsidian)) {
    val isNarrowWidth = maxWidth < 400.dp
    val isShortLandscape = maxHeight < 480.dp

    ExpeditionCanvas(
      state = state,
      onWorldTap = { wx, wy -> viewModel.onWorldTap(wx, wy) },
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(if (isShortLandscape) 8.dp else 12.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      TopExpeditionHud(
        state = state,
        isShortLandscape = isShortLandscape,
        isNarrowWidth = isNarrowWidth,
        onPauseClick = { viewModel.togglePause() },
        onSwitchLeader = { el ->
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.switchLeader(el)
        },
      )

      BottomControlDeck(
        state = state,
        isShortLandscape = isShortLandscape,
        isNarrowWidth = isNarrowWidth,
        onToggleMode = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          val next =
            if (state.controlMode == ControlMode.COMMANDER_SQUAD) ControlMode.LEADER_DIRECT
            else ControlMode.COMMANDER_SQUAD
          viewModel.setControlMode(next)
        },
        onWhistleChange = { active ->
          if (active) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.setWhistle(active)
        },
        onJoystick = { dx, dy -> viewModel.setJoystick(dx, dy) },
        onStrike = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.triggerStrike()
        },
        onSkill = { slot ->
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.triggerSkill(slot)
        },
      )
    }

    if (state.isPaused && state.outcome == null) {
      PauseOverlay(
        sectorName = state.sector.name,
        onResume = { viewModel.togglePause() },
        onAbort = onAbortExpedition,
      )
    }

    state.outcome?.let { outcome ->
      ExpeditionDebriefOverlay(
        outcome = outcome,
        isSaving = isSavingOutcome,
        onConfirm = {
          isSavingOutcome = true
          onCommitAndExit(outcome)
        },
      )
    }
  }
}

@Composable
private fun TopExpeditionHud(
  state: ExpeditionUiState,
  isShortLandscape: Boolean,
  isNarrowWidth: Boolean,
  onPauseClick: () -> Unit,
  onSwitchLeader: (ElementType) -> Unit,
) {
  val rescued = state.rescuePods.count { it.rescued }
  val totalPods = state.rescuePods.size
  val mins = (state.timeRemaining.toInt() / 60).coerceAtLeast(0)
  val secs = (state.timeRemaining.toInt() % 60).coerceAtLeast(0)
  val timerColor = if (state.timeRemaining < 45f) DangerCrimson else BioCyan

  Column(verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 4.dp else 8.dp)) {
    Surface(
      color = CavernBasalt.copy(alpha = 0.88f),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().border(1.dp, CavernBorder, RoundedCornerShape(14.dp)),
    ) {
      Row(
        modifier =
          Modifier.padding(
            horizontal = if (isNarrowWidth) 8.dp else 12.dp,
            vertical = if (isShortLandscape) 4.dp else 8.dp,
          ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = state.sector.name,
            style = if (isNarrowWidth) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
          )
          val objText =
            when {
              state.exitUnlocked -> "EXIT UNLOCKED • Reach Beacon!"
              rescued < totalPods -> "Pods $rescued/$totalPods • ${state.sector.weather.displayName}"
              else -> "Defeat Cavern Boss to unlock Exit!"
            }
          Text(
            text = objText,
            style = MaterialTheme.typography.labelSmall,
            color = if (state.exitUnlocked) SuccessEmerald else TextSecondary,
            maxLines = 1,
          )
        }

        if (isShortLandscape && state.leaders.size > 1) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 6.dp),
          ) {
            state.leaders.forEach { leader ->
              LeaderSwitchPill(
                element = leader.element,
                level = leader.level,
                unitCount = state.soldiers.count { it.element == leader.element },
                alive = leader.hp > 0f,
                isActive = leader.element == state.activeLeaderElement,
                compact = true,
                onClick = { onSwitchLeader(leader.element) },
              )
            }
          }
        }

        Surface(
          color = timerColor.copy(alpha = 0.14f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.padding(horizontal = 4.dp),
        ) {
          Text(
            text = "%02d:%02d".format(mins, secs),
            style = MaterialTheme.typography.titleMedium,
            color = timerColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          )
        }

        IconButton(
          onClick = onPauseClick,
          modifier = Modifier.size(48.dp).testTag("pause_expedition_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Pause,
            contentDescription = "Pause expedition",
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    if (!isShortLandscape) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        state.leaders.forEach { leader ->
          LeaderSwitchPill(
            element = leader.element,
            level = leader.level,
            unitCount = state.soldiers.count { it.element == leader.element },
            alive = leader.hp > 0f,
            isActive = leader.element == state.activeLeaderElement,
            compact = isNarrowWidth,
            onClick = { onSwitchLeader(leader.element) },
          )
        }
      }
    }

    if (state.statusBanner.isNotBlank() && !isShortLandscape) {
      Surface(
        color = CavernAlloy.copy(alpha = 0.86f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.border(1.dp, BioCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
      ) {
        Text(
          text = state.statusBanner,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
    }
  }
}

@Composable
private fun LeaderSwitchPill(
  element: ElementType,
  level: Int,
  unitCount: Int,
  alive: Boolean,
  isActive: Boolean,
  compact: Boolean,
  onClick: () -> Unit,
) {
  val accent = element.accentColor()
  Surface(
    color = if (isActive) accent.copy(alpha = 0.24f) else CavernAlloy.copy(alpha = 0.84f),
    shape = RoundedCornerShape(12.dp),
    modifier =
      Modifier.height(48.dp)
        .border(
          width = if (isActive) 2.dp else 1.dp,
          color = if (isActive) accent else CavernBorder,
          shape = RoundedCornerShape(12.dp),
        )
        .clip(RoundedCornerShape(12.dp))
        .clickable(enabled = alive) { onClick() }
        .testTag("leader_switch_${element.id}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Text(
        text = if (compact) "${element.displayName.take(1)} L$level" else "${element.displayName} L$level",
        style = MaterialTheme.typography.labelMedium,
        color = if (alive) accent else DangerCrimson,
      )
      Text(
        text = "×$unitCount",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun BottomControlDeck(
  state: ExpeditionUiState,
  isShortLandscape: Boolean,
  isNarrowWidth: Boolean,
  onToggleMode: () -> Unit,
  onWhistleChange: (Boolean) -> Unit,
  onJoystick: (Float, Float) -> Unit,
  onStrike: () -> Unit,
  onSkill: (Int) -> Unit,
) {
  val activeLeader = state.activeLeader
  val joystickSize =
    when {
      isShortLandscape -> 100.dp
      isNarrowWidth -> 108.dp
      else -> 132.dp
    }
  val whistleSize = if (isShortLandscape || isNarrowWidth) 72.dp else 84.dp
  val abilitySize = if (isShortLandscape || isNarrowWidth) 52.dp else 66.dp

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom,
  ) {
    if (state.controlMode == ControlMode.COMMANDER_SQUAD) {
      Surface(
        color = CavernBasalt.copy(alpha = 0.88f),
        shape = RoundedCornerShape(14.dp),
        modifier =
          Modifier.widthIn(max = if (isNarrowWidth) 168.dp else 240.dp)
            .border(1.dp, CavernBorder, RoundedCornerShape(14.dp)),
      ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
          Text(
            text = "SQUAD COMMAND",
            style = MaterialTheme.typography.labelSmall,
            color = BioCyan,
          )
          if (!isShortLandscape) {
            Text(
              text = "Tap map, eggs, or hazards",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Text(
            text = "Units: ${state.soldiers.size}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
      }
    } else {
      VirtualJoystick(
        sizeDp = joystickSize,
        accentColor = activeLeader?.element?.accentColor() ?: BioCyan,
        onMove = onJoystick,
      )
    }

    // In short landscape, keep toggle pill alongside Whistle/Abilities so center canvas stays tall
    if (isShortLandscape) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        ModeTogglePill(
          mode = state.controlMode,
          compact = true,
          onToggleMode = onToggleMode,
        )
        ActionControlsCluster(
          state = state,
          whistleSize = whistleSize,
          abilitySize = abilitySize,
          compactSpacing = 6.dp,
          onWhistleChange = onWhistleChange,
          onStrike = onStrike,
          onSkill = onSkill,
        )
      }
    } else {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ModeTogglePill(
          mode = state.controlMode,
          compact = isNarrowWidth,
          onToggleMode = onToggleMode,
        )
        ActionControlsCluster(
          state = state,
          whistleSize = whistleSize,
          abilitySize = abilitySize,
          compactSpacing = if (isNarrowWidth) 6.dp else 10.dp,
          onWhistleChange = onWhistleChange,
          onStrike = onStrike,
          onSkill = onSkill,
        )
      }
    }
  }
}

@Composable
private fun ModeTogglePill(
  mode: ControlMode,
  compact: Boolean,
  onToggleMode: () -> Unit,
) {
  Surface(
    color = CavernAlloy.copy(alpha = 0.92f),
    shape = RoundedCornerShape(14.dp),
    modifier =
      Modifier.height(48.dp)
        .border(1.dp, BioCyan.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
        .clip(RoundedCornerShape(14.dp))
        .clickable { onToggleMode() }
        .testTag("toggle_control_mode_button"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = if (compact) 10.dp else 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector =
          if (mode == ControlMode.COMMANDER_SQUAD) Icons.Filled.Gamepad else Icons.Filled.Groups,
        contentDescription = "Switch control mode",
        tint = BioCyan,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text =
          if (mode == ControlMode.COMMANDER_SQUAD) {
            if (compact) "Leader" else "Leader Stick"
          } else {
            if (compact) "Squad" else "Squad Mode"
          },
        style = MaterialTheme.typography.labelLarge,
        color = BioCyan,
      )
    }
  }
}

@Composable
private fun ActionControlsCluster(
  state: ExpeditionUiState,
  whistleSize: Dp,
  abilitySize: Dp,
  compactSpacing: Dp,
  onWhistleChange: (Boolean) -> Unit,
  onStrike: () -> Unit,
  onSkill: (Int) -> Unit,
) {
  val activeLeader = state.activeLeader
  if (state.controlMode == ControlMode.COMMANDER_SQUAD) {
    Box(
      modifier =
        Modifier.size(whistleSize)
          .clip(CircleShape)
          .background(if (state.whistleActive) BioCyan else BioCyan.copy(alpha = 0.22f))
          .border(2.dp, BioCyan, CircleShape)
          .pointerInput(Unit) {
            detectTapGestures(
              onPress = {
                onWhistleChange(true)
                tryAwaitRelease()
                onWhistleChange(false)
              }
            )
          }
          .testTag("whistle_rally_button"),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
          imageVector = Icons.Filled.Sensors,
          contentDescription = "Hold to whistle rally",
          tint = if (state.whistleActive) CavernObsidian else BioCyan,
          modifier = Modifier.size(24.dp),
        )
        Text(
          text = "WHISTLE",
          style = MaterialTheme.typography.labelSmall,
          color = if (state.whistleActive) CavernObsidian else BioCyan,
        )
      }
    }
  } else if (activeLeader != null) {
    val accent = activeLeader.element.accentColor()
    val skill1 = UpgradeCatalog.leaderSkills.firstOrNull { it.id == activeLeader.primarySkillId }
    val skill2 =
      activeLeader.secondarySkillId?.let { id ->
        UpgradeCatalog.leaderSkills.firstOrNull { it.id == id }
      }

    Row(
      horizontalArrangement = Arrangement.spacedBy(compactSpacing),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (skill2 != null) {
        AbilityCircleButton(
          sizeDp = abilitySize,
          label = skill2.name.substringBefore(" "),
          cooldown = activeLeader.secondaryCooldown,
          accent = VoltGold,
          icon = Icons.Filled.FlashOn,
          tag = "leader_skill_2_button",
          onClick = { onSkill(2) },
        )
      }
      if (skill1 != null) {
        AbilityCircleButton(
          sizeDp = abilitySize,
          label = skill1.name.substringBefore(" "),
          cooldown = activeLeader.primaryCooldown,
          accent = accent,
          icon = Icons.Filled.Bolt,
          tag = "leader_skill_1_button",
          onClick = { onSkill(1) },
        )
      }
      AbilityCircleButton(
        sizeDp = abilitySize,
        label = "HIT",
        cooldown = activeLeader.attackCooldown,
        accent = BioCyan,
        icon = Icons.Filled.SportsMartialArts,
        tag = "leader_strike_button",
        onClick = onStrike,
      )
    }
  }
}

@Composable
private fun AbilityCircleButton(
  sizeDp: Dp,
  label: String,
  cooldown: Float,
  accent: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tag: String,
  onClick: () -> Unit,
) {
  val ready = cooldown <= 0.05f
  Box(
    modifier =
      Modifier.size(sizeDp)
        .clip(CircleShape)
        .background(if (ready) accent.copy(alpha = 0.26f) else CavernBasalt.copy(alpha = 0.85f))
        .border(2.dp, if (ready) accent else CavernBorder, CircleShape)
        .clickable(enabled = ready) { onClick() }
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      if (ready) {
        Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(18.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
      } else {
        Text(
          text = "%.1fs".format(cooldown),
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
    }
  }
}

@Composable
private fun VirtualJoystick(
  sizeDp: Dp,
  accentColor: Color,
  onMove: (Float, Float) -> Unit,
) {
  var knobOffset by remember { mutableStateOf(Offset.Zero) }
  val density = LocalDensity.current
  val maxDistPx = with(density) { (sizeDp * 0.34f).toPx() }

  Box(
    modifier =
      Modifier.size(sizeDp)
        .clip(CircleShape)
        .background(CavernBasalt.copy(alpha = 0.72f))
        .border(2.dp, accentColor.copy(alpha = 0.65f), CircleShape)
        .pointerInput(maxDistPx) {
          detectDragGestures(
            onDragEnd = {
              knobOffset = Offset.Zero
              onMove(0f, 0f)
            },
            onDragCancel = {
              knobOffset = Offset.Zero
              onMove(0f, 0f)
            },
            onDrag = { change, dragAmount ->
              change.consume()
              val next = knobOffset + dragAmount
              val d = hypot(next.x, next.y)
              val clamped = if (d > maxDistPx) Offset(next.x / d * maxDistPx, next.y / d * maxDistPx) else next
              knobOffset = clamped
              onMove(clamped.x / maxDistPx, clamped.y / maxDistPx)
            },
          )
        }
        .testTag("leader_virtual_joystick"),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      drawCircle(
        color = accentColor.copy(alpha = 0.25f),
        radius = size.minDimension * 0.22f,
        center = center + knobOffset,
      )
      drawCircle(
        color = accentColor,
        radius = size.minDimension * 0.16f,
        center = center + knobOffset,
      )
    }
  }
}

@Composable
private fun PauseOverlay(sectorName: String, onResume: () -> Unit, onAbort: () -> Unit) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(CavernObsidian.copy(alpha = 0.8f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      color = CavernAlloy,
      shape = RoundedCornerShape(22.dp),
      modifier =
        Modifier.widthIn(max = 380.dp)
          .fillMaxWidth()
          .border(1.dp, CavernBorder, RoundedCornerShape(22.dp)),
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text("EXPEDITION PAUSED", style = MaterialTheme.typography.headlineSmall, color = BioCyan)
        Text(sectorName, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        Button(
          onClick = onResume,
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("resume_expedition_button"),
          colors = ButtonDefaults.buttonColors(containerColor = BioCyan, contentColor = CavernObsidian),
        ) {
          Icon(Icons.Filled.PlayArrow, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Resume Mission", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
          onClick = onAbort,
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("abort_expedition_button"),
        ) {
          Text("Return to Base Hub", color = DangerCrimson)
        }
      }
    }
  }
}

@Composable
private fun ExpeditionDebriefOverlay(
  outcome: ExpeditionOutcome,
  isSaving: Boolean,
  onConfirm: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(CavernObsidian.copy(alpha = 0.86f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      color = CavernAlloy,
      shape = RoundedCornerShape(24.dp),
      modifier =
        Modifier.widthIn(max = 480.dp)
          .fillMaxWidth()
          .border(
            1.5.dp,
            if (outcome.victory) SuccessEmerald else DangerCrimson,
            RoundedCornerShape(24.dp),
          ),
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (outcome.victory) "EXTRACTION COMPLETE" else "SQUAD RECALLED",
              style = MaterialTheme.typography.labelMedium,
              color = if (outcome.victory) SuccessEmerald else DangerCrimson,
            )
            Text(
              text = outcome.sector.name,
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Surface(
            color = BioCyan.copy(alpha = 0.2f),
            shape = CircleShape,
            modifier = Modifier.size(52.dp).border(2.dp, BioCyan, CircleShape),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = outcome.rank,
                style = MaterialTheme.typography.headlineMedium,
                color = BioCyan,
              )
            }
          }
        }

        Text(
          text =
            "Pods Rescued: ${outcome.rescuedCount}/${outcome.totalPods}  •  Leader XP: +${outcome.xpEarned}  •  Duration: ${outcome.durationSeconds}s",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )

        if (outcome.collectedResources.isNotEmpty() || outcome.collectedEggs.isNotEmpty()) {
          Surface(
            color = CavernBasalt,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("SALVAGED SUPPLIES", style = MaterialTheme.typography.labelSmall, color = BioCyan)
              outcome.collectedResources.forEach { (res, amt) ->
                Text("• ${res.displayName}: +$amt", style = MaterialTheme.typography.bodyMedium)
              }
              outcome.collectedEggs.forEach { (el, amt) ->
                Text("• ${el.displayName} Field Eggs: +$amt", style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }

        Button(
          onClick = onConfirm,
          enabled = !isSaving,
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sync_debrief_button"),
          colors = ButtonDefaults.buttonColors(containerColor = BioCyan, contentColor = CavernObsidian),
        ) {
          Icon(Icons.Filled.CheckCircle, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (isSaving) "Syncing to Cloud..." else "Sync & Return to Base", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
