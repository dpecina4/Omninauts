package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BiomeType
import com.example.data.model.ElementType
import com.example.data.model.WeatherCondition
import com.example.game.EffectKind
import com.example.game.EnemyEntity
import com.example.game.ExpeditionUiState
import com.example.game.FieldEgg
import com.example.game.LeaderUnit
import com.example.game.ObstacleEntity
import com.example.game.RescuePod
import com.example.game.ResourcePickupNode
import com.example.game.SoldierState
import com.example.game.SoldierUnit
import com.example.game.TileType
import com.example.ui.theme.BioCyan
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.HydroAqua
import com.example.ui.theme.HydroGlow
import com.example.ui.theme.PyroAmber
import com.example.ui.theme.PyroGlow
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.VoltGold
import com.example.ui.theme.VoltNeon
import com.example.ui.theme.VoltaicViolet
import com.example.ui.theme.accentColor
import com.example.ui.theme.glowColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpeditionCanvas(
  state: ExpeditionUiState,
  onWorldTap: (Float, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val leader = state.activeLeader
  var camX by remember { mutableFloatStateOf(state.startX) }
  var camY by remember { mutableFloatStateOf(state.startY) }

  LaunchedEffect(leader?.x, leader?.y, state.frameTick) {
    if (leader != null) {
      camX += (leader.x - camX) * 0.16f
      camY += (leader.y - camY) * 0.16f
    }
  }

  val density = LocalDensity.current
  val minTilePx = with(density) { 36.dp.toPx() }
  val maxTilePx = with(density) { 62.dp.toPx() }
  val textSizePx = with(density) { 13.sp.toPx() }

  val textPaint = remember(textSizePx) {
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
      textSize = textSizePx
      isFakeBoldText = true
    }
  }

  Canvas(
    modifier =
      modifier.fillMaxSize().pointerInput(minTilePx, maxTilePx) {
        detectTapGestures { offset ->
          val tilePx = (minOf(size.width, size.height) / 9.2f).coerceIn(minTilePx, maxTilePx)
          val ySquash = 0.82f // Top-down angled Pikmin-style foreshortening
          val wx = camX + (offset.x - size.width / 2f) / tilePx
          val wy = camY + (offset.y - size.height / 2f) / (tilePx * ySquash)
          onWorldTap(wx, wy)
        }
      }
  ) {
    val tilePx = (minOf(size.width, size.height) / 9.2f).coerceIn(minTilePx, maxTilePx)
    val ySquash = 0.82f
    val tileH = tilePx * ySquash

    fun toScreenX(wx: Float): Float = size.width / 2f + (wx - camX) * tilePx
    fun toScreenY(wy: Float): Float = size.height / 2f + (wy - camY) * tileH

    val (floorBase, floorAccent, wallTop, wallSide) = biomeColors(state.sector.biome)

    // Deep obsidian cavern backdrop
    drawRect(color = Color(0xFF04070D))

    // Draw 2.5D angled tiles back-to-front by row (y)
    val startTx = (camX - size.width / (2f * tilePx) - 2f).toInt().coerceAtLeast(0)
    val endTx = (camX + size.width / (2f * tilePx) + 2f).toInt().coerceAtMost(state.gridWidth - 1)
    val startTy = (camY - size.height / (2f * tileH) - 2f).toInt().coerceAtLeast(0)
    val endTy = (camY + size.height / (2f * tileH) + 3f).toInt().coerceAtMost(state.gridHeight - 1)

    for (ty in startTy..endTy) {
      for (tx in startTx..endTx) {
        val tile = state.tileAt(tx, ty)
        val sx = toScreenX(tx.toFloat())
        val sy = toScreenY(ty.toFloat())

        if (tile == TileType.WALL) {
          val wallLift = tileH * 0.42f
          // Front extruded wall face
          drawRect(
            color = wallSide,
            topLeft = Offset(sx, sy + tileH - wallLift),
            size = Size(tilePx + 1f, wallLift + 1f),
          )
          // Top rock cap
          drawRect(
            color = wallTop,
            topLeft = Offset(sx, sy - wallLift),
            size = Size(tilePx + 1f, tileH + 1f),
          )
          // Occasional bioluminescent crystal fleck
          if ((tx * 31 + ty * 17) % 7 == 0) {
            drawCircle(
              color = floorAccent.copy(alpha = 0.35f),
              radius = tilePx * 0.1f,
              center = Offset(sx + tilePx * 0.5f, sy - wallLift + tileH * 0.45f),
            )
          }
        } else {
          // Walkable cavern floor
          drawRect(
            color = floorBase,
            topLeft = Offset(sx, sy),
            size = Size(tilePx + 1f, tileH + 1f),
          )
          drawRect(
            color = floorAccent.copy(alpha = 0.08f),
            topLeft = Offset(sx, sy),
            size = Size(tilePx, tileH),
            style = Stroke(width = 1f),
          )
          // Mycelium spore glow speckles
          if ((tx * 13 + ty * 29) % 5 == 0) {
            drawCircle(
              color = floorAccent.copy(alpha = 0.18f),
              radius = tilePx * 0.09f,
              center = Offset(sx + tilePx * 0.35f, sy + tileH * 0.6f),
            )
          }
          if (tile == TileType.START_PAD) {
            drawCircle(
              color = BioCyan.copy(alpha = 0.25f),
              radius = tilePx * 0.42f,
              center = Offset(sx + tilePx * 0.5f, sy + tileH * 0.5f),
            )
            drawCircle(
              color = BioCyan,
              radius = tilePx * 0.32f,
              center = Offset(sx + tilePx * 0.5f, sy + tileH * 0.5f),
              style = Stroke(width = 3f),
            )
          } else if (tile == TileType.EXIT_BEACON) {
            val portalColor = if (state.exitUnlocked) SuccessEmerald else Color(0xFF475B75)
            val pulse = 0.85f + 0.15f * sin(state.elapsedSeconds * 5f)
            drawCircle(
              color = portalColor.copy(alpha = 0.32f),
              radius = tilePx * 0.52f * pulse,
              center = Offset(sx + tilePx * 0.5f, sy + tileH * 0.5f),
            )
            drawCircle(
              color = portalColor,
              radius = tilePx * 0.36f,
              center = Offset(sx + tilePx * 0.5f, sy + tileH * 0.5f),
              style = Stroke(width = 4f),
            )
          }
        }
      }
    }

    // Draw Obstacles
    state.obstacles.forEach { obs ->
      drawObstacle(obs, toScreenX(obs.tileX + 0.5f), toScreenY(obs.tileY + 0.5f), tilePx, tileH, state.elapsedSeconds)
    }

    // Draw Resource Nodes
    state.resourceNodes.filter { it.amountLeft > 0 }.forEach { node ->
      drawResourceNode(node, toScreenX(node.x), toScreenY(node.y), tilePx, state.elapsedSeconds)
    }

    // Draw Field Eggs
    state.eggs.filter { !it.hatched }.forEach { egg ->
      drawFieldEgg(egg, toScreenX(egg.x), toScreenY(egg.y), tilePx, state.elapsedSeconds)
    }

    // Draw Rescue Pods
    state.rescuePods.forEach { pod ->
      drawRescuePod(pod, toScreenX(pod.x), toScreenY(pod.y), tilePx, state.elapsedSeconds)
    }

    // Draw Enemies & Bosses
    state.enemies.forEach { enemy ->
      drawEnemy(enemy, toScreenX(enemy.x), toScreenY(enemy.y), tilePx, state.elapsedSeconds)
    }

    // Draw Soldiers
    state.soldiers.forEach { soldier ->
      drawSoldier(soldier, toScreenX(soldier.x), toScreenY(soldier.y), tilePx, state.elapsedSeconds)
    }

    // Draw Leaders
    state.leaders.filter { it.hp > 0f }.forEach { l ->
      drawLeader(
        leader = l,
        isActive = l.element == state.activeLeaderElement,
        sx = toScreenX(l.x),
        sy = toScreenY(l.y),
        tilePx = tilePx,
      )
    }

    // Draw Whistle Sonar Ring
    if (state.whistleActive && leader != null) {
      val lx = toScreenX(leader.x)
      val ly = toScreenY(leader.y)
      val rPx = state.whistleRadius * tilePx
      drawCircle(
        color = BioCyan.copy(alpha = 0.16f),
        radius = rPx,
        center = Offset(lx, ly),
      )
      drawCircle(
        color = BioCyan,
        radius = rPx,
        center = Offset(lx, ly),
        style = Stroke(width = 4f),
      )
      drawCircle(
        color = BioCyan.copy(alpha = 0.5f),
        radius = rPx * 0.65f,
        center = Offset(lx, ly),
        style = Stroke(width = 2f),
      )
    }

    // Draw Visual Effects & Floating Combat/Loot Text
    state.effects.forEach { fx ->
      val ex = toScreenX(fx.x)
      val ey = toScreenY(fx.y)
      val progress = 1f - (fx.life / fx.maxLife).coerceIn(0f, 1f)
      val col = fx.element?.accentColor() ?: BioCyan
      when (fx.kind) {
        EffectKind.WAYPOINT_PING -> {
          drawCircle(
            color = col.copy(alpha = 1f - progress),
            radius = tilePx * (0.25f + progress * 0.45f),
            center = Offset(ex, ey),
            style = Stroke(width = 3f),
          )
        }
        EffectKind.ABILITY_BLAST, EffectKind.HATCH_BURST -> {
          drawCircle(
            color = col.copy(alpha = (1f - progress) * 0.42f),
            radius = fx.radius * tilePx * (0.4f + progress * 0.65f),
            center = Offset(ex, ey),
          )
          drawCircle(
            color = col.copy(alpha = 1f - progress),
            radius = fx.radius * tilePx * (0.4f + progress * 0.65f),
            center = Offset(ex, ey),
            style = Stroke(width = 4f),
          )
        }
        EffectKind.BOSS_SLAM -> {
          drawCircle(
            color = DangerCrimson.copy(alpha = (1f - progress) * 0.55f),
            radius = fx.radius * tilePx,
            center = Offset(ex, ey),
          )
        }
        EffectKind.FLOATING_TEXT -> {
          textPaint.color = col.copy(alpha = (1f - progress * 0.8f)).toArgb()
          drawContext.canvas.nativeCanvas.drawText(
            fx.label,
            ex,
            ey - progress * 36f,
            textPaint,
          )
        }
        EffectKind.WHISTLE_RING -> Unit
      }
    }

    // Weather Atmospheric Overlay
    drawWeatherOverlay(state.sector.weather, state.elapsedSeconds, leader?.let { toScreenX(it.x) to toScreenY(it.y) })
  }
}

private fun DrawScope.drawObstacle(
  obs: ObstacleEntity,
  cx: Float,
  cy: Float,
  tilePx: Float,
  tileH: Float,
  elapsed: Float,
) {
  val accent = obs.requiredElement.accentColor()
  val glow = obs.requiredElement.glowColor()
  val w = tilePx * 0.86f
  val h = tileH * 0.86f
  val left = cx - w / 2f
  val top = cy - h / 2f

  drawRoundRect(
    color = accent.copy(alpha = 0.25f),
    topLeft = Offset(left - 3f, top - 3f),
    size = Size(w + 6f, h + 6f),
    cornerRadius = CornerRadius(12f, 12f),
  )

  when (obs.tileType) {
    TileType.OBSTACLE_CRYO_BARRIER -> {
      val path =
        Path().apply {
          moveTo(cx, top)
          lineTo(cx + w * 0.45f, cy + h * 0.35f)
          lineTo(cx - w * 0.45f, cy + h * 0.35f)
          close()
        }
      drawPath(path, color = Color(0xFF90E0EF))
      drawPath(path, color = Color.White.copy(alpha = 0.65f), style = Stroke(2.5f))
    }
    TileType.OBSTACLE_SPORE_WEB -> {
      drawCircle(color = Color(0xFF4A2559), radius = w * 0.42f, center = Offset(cx, cy))
      drawCircle(color = PyroAmber.copy(alpha = 0.7f), radius = w * 0.38f, center = Offset(cx, cy), style = Stroke(3f))
    }
    TileType.OBSTACLE_FIRE_JET -> {
      val flicker = 0.85f + 0.2f * sin(elapsed * 12f)
      drawCircle(color = PyroAmber.copy(alpha = 0.85f), radius = w * 0.35f * flicker, center = Offset(cx, cy))
      drawCircle(color = VoltGold, radius = w * 0.18f * flicker, center = Offset(cx, cy))
    }
    TileType.OBSTACLE_BRINE_CHANNEL -> {
      drawRoundRect(
        color = HydroAqua.copy(alpha = 0.65f),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(8f, 8f),
      )
    }
    TileType.OBSTACLE_CIRCUIT_LOCK, TileType.OBSTACLE_SHIELD_PYLON -> {
      drawRoundRect(
        color = Color(0xFF202A3A),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(10f, 10f),
      )
      drawCircle(color = VoltGold, radius = w * 0.22f, center = Offset(cx, cy))
      drawCircle(color = glow, radius = w * 0.36f, center = Offset(cx, cy), style = Stroke(2.5f))
    }
    else -> Unit
  }

  // Mini obstacle HP bar
  val ratio = (obs.hp / obs.maxHp).coerceIn(0f, 1f)
  drawRect(
    color = Color.Black.copy(alpha = 0.65f),
    topLeft = Offset(cx - w * 0.35f, top - 8f),
    size = Size(w * 0.7f, 5f),
  )
  drawRect(
    color = accent,
    topLeft = Offset(cx - w * 0.35f, top - 8f),
    size = Size(w * 0.7f * ratio, 5f),
  )
}

private fun DrawScope.drawResourceNode(
  node: ResourcePickupNode,
  cx: Float,
  cy: Float,
  tilePx: Float,
  elapsed: Float,
) {
  val col = node.resourceType.element.accentColor()
  val r = tilePx * 0.24f
  val bob = sin(elapsed * 4f + node.x) * 3f
  drawCircle(color = col.copy(alpha = 0.22f), radius = r * 1.45f, center = Offset(cx, cy + bob))
  drawRoundRect(
    color = col,
    topLeft = Offset(cx - r * 0.8f, cy - r * 0.8f + bob),
    size = Size(r * 1.6f, r * 1.6f),
    cornerRadius = CornerRadius(5f, 5f),
  )
  drawCircle(color = Color.White.copy(alpha = 0.75f), radius = r * 0.32f, center = Offset(cx, cy + bob))
}

private fun DrawScope.drawFieldEgg(
  egg: FieldEgg,
  cx: Float,
  cy: Float,
  tilePx: Float,
  elapsed: Float,
) {
  val accent = egg.element.accentColor()
  val glow = egg.element.glowColor()
  val pulse = 1f + 0.08f * sin(elapsed * 6f)
  drawCircle(color = accent.copy(alpha = 0.24f), radius = tilePx * 0.42f * pulse, center = Offset(cx, cy))
  drawOval(
    color = Color(0xFFE7F2FF),
    topLeft = Offset(cx - tilePx * 0.2f, cy - tilePx * 0.26f),
    size = Size(tilePx * 0.4f, tilePx * 0.52f),
  )
  drawCircle(color = accent, radius = tilePx * 0.1f, center = Offset(cx - tilePx * 0.05f, cy - tilePx * 0.04f))
  drawCircle(color = glow, radius = tilePx * 0.07f, center = Offset(cx + tilePx * 0.07f, cy + tilePx * 0.08f))

  if (egg.progress > 0f) {
    val sweep = (egg.progress / egg.requiredProgress).coerceIn(0f, 1f) * 360f
    drawArc(
      color = accent,
      startAngle = -90f,
      sweepAngle = sweep,
      useCenter = false,
      topLeft = Offset(cx - tilePx * 0.36f, cy - tilePx * 0.36f),
      size = Size(tilePx * 0.72f, tilePx * 0.72f),
      style = Stroke(width = 4f),
    )
  }
}

private fun DrawScope.drawRescuePod(
  pod: RescuePod,
  cx: Float,
  cy: Float,
  tilePx: Float,
  elapsed: Float,
) {
  val col = if (pod.rescued) SuccessEmerald else BioCyan
  val ringScale = 1f + 0.25f * ((elapsed * 1.4f) % 1f)
  if (!pod.rescued) {
    drawCircle(
      color = DangerCrimson.copy(alpha = 0.45f * (2f - ringScale)),
      radius = tilePx * 0.48f * ringScale,
      center = Offset(cx, cy),
      style = Stroke(width = 3f),
    )
  }
  drawRoundRect(
    color = Color(0xFF1F3047),
    topLeft = Offset(cx - tilePx * 0.28f, cy - tilePx * 0.32f),
    size = Size(tilePx * 0.56f, tilePx * 0.64f),
    cornerRadius = CornerRadius(12f, 12f),
  )
  drawRoundRect(
    color = col,
    topLeft = Offset(cx - tilePx * 0.28f, cy - tilePx * 0.32f),
    size = Size(tilePx * 0.56f, tilePx * 0.64f),
    cornerRadius = CornerRadius(12f, 12f),
    style = Stroke(width = 3f),
  )
  drawCircle(color = col, radius = tilePx * 0.12f, center = Offset(cx, cy))
  if (!pod.rescued && pod.rescueProgress > 0f) {
    drawArc(
      color = SuccessEmerald,
      startAngle = -90f,
      sweepAngle = (pod.rescueProgress / 2f).coerceIn(0f, 1f) * 360f,
      useCenter = false,
      topLeft = Offset(cx - tilePx * 0.38f, cy - tilePx * 0.38f),
      size = Size(tilePx * 0.76f, tilePx * 0.76f),
      style = Stroke(width = 4f),
    )
  }
}

private fun DrawScope.drawEnemy(
  enemy: EnemyEntity,
  cx: Float,
  cy: Float,
  tilePx: Float,
  elapsed: Float,
) {
  val r = enemy.radius * tilePx
  // Boss telegraphed slam warning ring
  if (enemy.kind.isBoss && enemy.telegraphTimer > 0f) {
    val fillRatio = (enemy.telegraphTimer / enemy.telegraphMax).coerceIn(0f, 1f)
    val warnR = enemy.telegraphRadius * tilePx
    drawCircle(
      color = DangerCrimson.copy(alpha = 0.16f + 0.18f * fillRatio),
      radius = warnR * fillRatio,
      center = Offset(cx, cy),
    )
    drawCircle(
      color = DangerCrimson,
      radius = warnR,
      center = Offset(cx, cy),
      style = Stroke(width = 4f),
    )
  }

  // Drop shadow
  drawOval(
    color = Color.Black.copy(alpha = 0.45f),
    topLeft = Offset(cx - r * 0.9f, cy + r * 0.25f),
    size = Size(r * 1.8f, r * 0.8f),
  )

  val bodyColor =
    when {
      enemy.hitFlash > 0f -> Color.White
      enemy.stunTimer > 0f -> VoltGold
      enemy.kind.isBoss -> Color(0xFFC92A4E)
      else -> VoltaicViolet
    }
  val wobble = sin(elapsed * 7f + enemy.x) * 2.5f
  drawCircle(color = bodyColor, radius = r, center = Offset(cx, cy + wobble))
  drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = r, center = Offset(cx, cy + wobble), style = Stroke(3f))
  // Glowing eyes
  drawCircle(color = VoltGold, radius = r * 0.18f, center = Offset(cx - r * 0.3f, cy - r * 0.1f + wobble))
  drawCircle(color = VoltGold, radius = r * 0.18f, center = Offset(cx + r * 0.3f, cy - r * 0.1f + wobble))

  // Enemy HP bar
  val hpRatio = (enemy.hp / enemy.maxHp).coerceIn(0f, 1f)
  val barW = r * 2.0f
  drawRect(
    color = Color.Black.copy(alpha = 0.7f),
    topLeft = Offset(cx - barW / 2f, cy - r - 12f),
    size = Size(barW, 6f),
  )
  drawRect(
    color = DangerCrimson,
    topLeft = Offset(cx - barW / 2f, cy - r - 12f),
    size = Size(barW * hpRatio, 6f),
  )
}

private fun DrawScope.drawSoldier(
  soldier: SoldierUnit,
  cx: Float,
  cy: Float,
  tilePx: Float,
  elapsed: Float,
) {
  val r = tilePx * 0.18f
  val accent = soldier.element.accentColor()
  val glow = soldier.element.glowColor()
  val hop = if (soldier.state != SoldierState.IDLE) kotlin.math.abs(sin(elapsed * 11f + soldier.x)) * 3f else 0f

  // Shadow
  drawOval(
    color = Color.Black.copy(alpha = 0.45f),
    topLeft = Offset(cx - r, cy + r * 0.35f),
    size = Size(r * 2f, r * 0.9f),
  )
  // Bioluminescent leaf/stem tip (Pikmin-inspired silhouette)
  drawLine(
    color = glow,
    start = Offset(cx, cy - r - hop),
    end = Offset(cx, cy - r * 1.8f - hop),
    strokeWidth = 2.5f,
  )
  drawCircle(color = glow, radius = r * 0.35f, center = Offset(cx, cy - r * 1.9f - hop))

  // Creature body
  drawCircle(color = accent, radius = r, center = Offset(cx, cy - hop))
  drawCircle(color = Color.White, radius = r * 0.25f, center = Offset(cx + r * 0.25f, cy - r * 0.15f - hop))

  if (soldier.hp < soldier.maxHp) {
    val ratio = (soldier.hp / soldier.maxHp).coerceIn(0f, 1f)
    drawRect(
      color = SuccessEmerald,
      topLeft = Offset(cx - r, cy - r * 2.3f - hop),
      size = Size(r * 2f * ratio, 3f),
    )
  }
}

private fun DrawScope.drawLeader(
  leader: LeaderUnit,
  isActive: Boolean,
  sx: Float,
  sy: Float,
  tilePx: Float,
) {
  val r = tilePx * 0.32f
  val accent = leader.element.accentColor()
  val glow = leader.element.glowColor()

  if (isActive) {
    drawCircle(
      color = BioCyan.copy(alpha = 0.25f),
      radius = r * 1.65f,
      center = Offset(sx, sy),
    )
    drawCircle(
      color = BioCyan,
      radius = r * 1.45f,
      center = Offset(sx, sy),
      style = Stroke(width = 3f),
    )
  }

  // Drop shadow
  drawOval(
    color = Color.Black.copy(alpha = 0.55f),
    topLeft = Offset(sx - r, sy + r * 0.4f),
    size = Size(r * 2f, r * 0.9f),
  )

  // Leader body + helmet visor
  drawCircle(color = accent, radius = r, center = Offset(sx, sy))
  drawCircle(color = glow, radius = r, center = Offset(sx, sy), style = Stroke(width = 3f))

  val visorX = sx + cos(leader.facingAngle) * r * 0.35f
  val visorY = sy + sin(leader.facingAngle) * r * 0.35f
  drawCircle(color = Color(0xFF0A1422), radius = r * 0.42f, center = Offset(visorX, visorY))
  drawCircle(color = BioCyan, radius = r * 0.22f, center = Offset(visorX, visorY))

  // Leader HP bar
  val hpRatio = (leader.hp / leader.maxHp).coerceIn(0f, 1f)
  drawRect(
    color = Color.Black.copy(alpha = 0.7f),
    topLeft = Offset(sx - r, sy - r - 10f),
    size = Size(r * 2f, 5f),
  )
  drawRect(
    color = SuccessEmerald,
    topLeft = Offset(sx - r, sy - r - 10f),
    size = Size(r * 2f * hpRatio, 5f),
  )
}

private fun DrawScope.drawWeatherOverlay(
  weather: WeatherCondition,
  elapsed: Float,
  leaderScreenPos: Pair<Float, Float>?,
) {
  when (weather) {
    WeatherCondition.SPORE_MIST -> {
      val center = leaderScreenPos?.let { Offset(it.first, it.second) } ?: Offset(size.width / 2f, size.height / 2f)
      drawRect(
        brush =
          Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0xCC06101B)),
            center = center,
            radius = minOf(size.width, size.height) * 0.55f,
          )
      )
    }
    WeatherCondition.BRINE_RAIN -> {
      for (i in 0..22) {
        val px = ((i * 173f + elapsed * 140f) % size.width)
        val py = ((i * 251f + elapsed * 460f) % size.height)
        drawLine(
          color = HydroGlow.copy(alpha = 0.26f),
          start = Offset(px, py),
          end = Offset(px - 8f, py + 22f),
          strokeWidth = 2f,
        )
      }
    }
    WeatherCondition.ION_SURGE -> {
      if ((elapsed * 3f).toInt() % 4 == 0) {
        drawRect(color = VoltGold.copy(alpha = 0.05f))
      }
      for (i in 0..8) {
        val sx = ((i * 211f + elapsed * 90f) % size.width)
        val sy = ((i * 157f + elapsed * 60f) % size.height)
        drawCircle(color = VoltNeon.copy(alpha = 0.28f), radius = 3.5f, center = Offset(sx, sy))
      }
    }
    WeatherCondition.THERMAL_UPDRAFT -> {
      for (i in 0..16) {
        val ex = ((i * 193f + sin(elapsed + i) * 30f) % size.width + size.width) % size.width
        val ey = size.height - ((i * 137f + elapsed * 110f) % size.height)
        drawCircle(color = PyroGlow.copy(alpha = 0.34f), radius = 4f, center = Offset(ex, ey))
      }
    }
    WeatherCondition.CLEAR -> Unit
  }
}

private data class BiomePalette(
  val floorBase: Color,
  val floorAccent: Color,
  val wallTop: Color,
  val wallSide: Color,
)

private fun biomeColors(biome: BiomeType): BiomePalette =
  when (biome) {
    BiomeType.BASALT_RIFT ->
      BiomePalette(Color(0xFF1A1316), PyroAmber, Color(0xFF2B1C20), Color(0xFF140B0D))
    BiomeType.CRYSTAL_CISTERN ->
      BiomePalette(Color(0xFF0C1A26), HydroAqua, Color(0xFF162C3D), Color(0xFF08141E))
    BiomeType.DYNAMO_RUINS ->
      BiomePalette(Color(0xFF131922), VoltGold, Color(0xFF222E3F), Color(0xFF0C1119))
    BiomeType.FUNGAL_GROTTO ->
      BiomePalette(Color(0xFF0C1B1E), BioCyan, Color(0xFF172F36), Color(0xFF081316))
    BiomeType.STORM_CHASM ->
      BiomePalette(Color(0xFF151226), VoltaicViolet, Color(0xFF272145), Color(0xFF0E0B1C))
  }
