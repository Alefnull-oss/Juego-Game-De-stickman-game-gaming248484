package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoneType
import com.example.model.ObstacleType
import com.example.model.PlacedObstacle
import com.example.model.VisualParticle
import com.example.physics.DistanceConstraint
import com.example.physics.Particle
import com.example.physics.RagdollBody
import com.example.physics.RagdollSimulation
import com.example.physics.TerrainSegment
import com.example.physics.VehicleBody
import com.example.ui.theme.BoostBlue
import com.example.ui.theme.CashGold
import com.example.ui.theme.DismountWhite
import com.example.ui.theme.FractureRed
import com.example.ui.theme.HazardOrange
import com.example.ui.theme.PlatformDarkEdge
import com.example.ui.theme.PlatformGrey
import com.example.ui.theme.PushArrowOrange
import com.example.ui.theme.StickmanSilhouette
import com.example.ui.theme.StudioGreyBottom
import com.example.ui.theme.StudioGreyCenter
import com.example.ui.theme.StudioGreyTop
import com.example.ui.theme.StudioVignetteEdge
import com.example.viewmodel.GamePhase
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun DismountCanvas(
  simulation: RagdollSimulation,
  phase: GamePhase,
  isSlowMoActive: Boolean,
  renderTick: Long,
  onStickmanClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Smooth animated zoom when slow-mo is active
  val targetZoom = if (isSlowMoActive) 1.25f else 0.95f
  val zoom by animateFloatAsState(
    targetValue = targetZoom,
    animationSpec = tween(durationMillis = 400),
    label = "camera_zoom"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "pulse_arrow")
  val arrowPulse by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(500),
      repeatMode = RepeatMode.Reverse
    ),
    label = "arrow_pulse"
  )

  Box(modifier = modifier.fillMaxSize()) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .clickable(enabled = phase == GamePhase.PREPARING, onClick = onStickmanClicked)
        .testTag("dismount_canvas")
    ) {
      @Suppress("UNUSED_VARIABLE")
      val tick = renderTick

      val canvasWidth = size.width
      val canvasHeight = size.height

      // Camera centering strictly tracking the stickman
      val (targetX, targetY) = simulation.getCameraTarget()
      val offsetX = canvasWidth * 0.40f - targetX * zoom
      val offsetY = canvasHeight * 0.50f - targetY * zoom

      fun toScreenX(worldX: Float) = worldX * zoom + offsetX
      fun toScreenY(worldY: Float) = worldY * zoom + offsetY

      // 1. Draw Studio Background with Floating Cubes & Vignette
      drawStudioBackground(canvasWidth, canvasHeight, offsetX, offsetY)

      // 2. Draw Solid Grey Platforms and Terrain
      drawSolidTerrain(simulation.terrain, ::toScreenX, ::toScreenY, zoom, canvasHeight)

      // 3. Draw Placed Obstacles
      drawObstacles(simulation.obstacles, simulation.terrain, ::toScreenX, ::toScreenY, zoom)

      // 4. Draw Vehicle (if equipped)
      simulation.vehicleBody?.let { vb ->
        drawVehicle(vb, ::toScreenX, ::toScreenY, zoom)
      }

      // 5. Draw Stickman with Dark Silhouette & Internal X-Ray Glow
      drawStickmanWithXRay(simulation.ragdoll, ::toScreenX, ::toScreenY, zoom)

      // 6. Draw Directional Orange Push Arrow (during preparation)
      if (phase == GamePhase.PREPARING || phase == GamePhase.CHARGING_POWER) {
        val (pushX, pushY) = simulation.getPushJointPosition()
        drawPushArrow(
          originX = toScreenX(pushX),
          originY = toScreenY(pushY),
          angleDegrees = 20f,
          scale = arrowPulse,
          zoom = zoom
        )
      }

      // 7. Draw Visual Particles (fracture sparks, smoke, explosions)
      drawParticles(simulation.particles, ::toScreenX, ::toScreenY, zoom)

      // 8. Slow-Mo Vignette Tint
      if (isSlowMoActive) {
        drawRect(
          color = FractureRed.copy(alpha = 0.08f),
          size = size
        )
      }
    }

    // Slow-Mo HUD Indicator Overlay
    if (isSlowMoActive) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 18.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xD91E2022))
          .padding(horizontal = 14.dp, vertical = 6.dp)
          .testTag("slowmo_indicator")
      ) {
        Text(
          text = "● CÁMARA LENTA AUTOMÁTICA (0.25x)",
          color = FractureRed,
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )
      }
    }
  }
}

private fun DrawScope.drawStudioBackground(
  width: Float,
  height: Float,
  camOffsetX: Float,
  camOffsetY: Float
) {
  // 1. Studio vertical light gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        StudioGreyTop,
        StudioGreyCenter,
        StudioGreyBottom
      )
    ),
    size = Size(width, height)
  )

  // 2. Floating ambient geometric cubes (as seen in the reference screenshot)
  val cubeParallax = 0.2f
  val baseCubes = listOf(
    Pair(Offset(120f, 90f), 75f),
    Pair(Offset(220f, 180f), 110f),
    Pair(Offset(450f, 70f), 90f),
    Pair(Offset(680f, 150f), 125f),
    Pair(Offset(880f, 60f), 80f),
    Pair(Offset(960f, 220f), 105f)
  )

  for ((pos, s) in baseCubes) {
    val x = (pos.x + camOffsetX * cubeParallax) % (width + 200f) - 100f
    val y = pos.y
    drawRect(
      color = Color.White.copy(alpha = 0.18f),
      topLeft = Offset(x, y),
      size = Size(s, s)
    )
  }

  // 3. Ambient studio photographic vignette (darkening corners)
  drawRect(
    brush = Brush.radialGradient(
      colors = listOf(
        Color.Transparent,
        Color.Transparent,
        StudioVignetteEdge
      ),
      center = Offset(width * 0.5f, height * 0.5f),
      radius = max(width, height) * 0.75f
    ),
    size = Size(width, height)
  )
}

private fun DrawScope.drawSolidTerrain(
  segments: List<TerrainSegment>,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float,
  screenHeight: Float
) {
  if (segments.isEmpty()) return

  // Build filled polygonal ground path
  val groundPath = Path().apply {
    val first = segments.first()
    moveTo(toX(first.x1), toY(first.y1))

    for (seg in segments) {
      lineTo(toX(seg.x2), toY(seg.y2))
    }

    val last = segments.last()
    // Drop down to bottom of screen
    lineTo(toX(last.x2), screenHeight + 200f)
    lineTo(toX(first.x1), screenHeight + 200f)
    close()
  }

  // Solid platform fill
  drawPath(path = groundPath, color = PlatformGrey)

  // Draw crisp top border edges
  for (seg in segments) {
    drawLine(
      color = PlatformDarkEdge,
      start = Offset(toX(seg.x1), toY(seg.y1)),
      end = Offset(toX(seg.x2), toY(seg.y2)),
      strokeWidth = 3f * zoom,
      cap = StrokeCap.Square
    )
  }
}

private fun DrawScope.drawStickmanWithXRay(
  ragdoll: RagdollBody,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  // 1. Draw Solid Dark Silhouette Limbs
  for (c in ragdoll.constraints) {
    val sx1 = toX(c.p1.x)
    val sy1 = toY(c.p1.y)
    val sx2 = toX(c.p2.x)
    val sy2 = toY(c.p2.y)

    val isBoneBroken = c.boneType != null && (ragdoll.boneStatusMap[c.boneType]?.isBroken == true)

    if (isBoneBroken) {
      // UNIQUE TOUCH: The fractured bone lights up from inside the silhouette in radiant neon red!
      val midX = (sx1 + sx2) * 0.5f + (if (c.p1.speed.toInt() % 2 == 0) 4f else -4f) * zoom
      val midY = (sy1 + sy2) * 0.5f + (if (c.p2.speed.toInt() % 2 == 0) -4f else 4f) * zoom

      // Outer glow
      drawLine(
        color = FractureRed.copy(alpha = 0.4f),
        start = Offset(sx1, sy1),
        end = Offset(sx2, sy2),
        strokeWidth = 14f * zoom,
        cap = StrokeCap.Round
      )
      // Intense fracture crack
      drawLine(
        color = FractureRed,
        start = Offset(sx1, sy1),
        end = Offset(midX, midY),
        strokeWidth = 6f * zoom,
        cap = StrokeCap.Round
      )
      drawLine(
        color = Color.White,
        start = Offset(midX, midY),
        end = Offset(sx2, sy2),
        strokeWidth = 4f * zoom,
        cap = StrokeCap.Round
      )
    } else {
      // Solid dark stickman silhouette limb
      val limbWidth = if (c.boneType == BoneType.LUMBAR_SPINE || c.boneType == BoneType.THORAX_RIBS) 9f else 7f
      drawLine(
        color = StickmanSilhouette,
        start = Offset(sx1, sy1),
        end = Offset(sx2, sy2),
        strokeWidth = limbWidth * zoom,
        cap = StrokeCap.Round
      )
    }
  }

  // 2. Draw Stickman Head
  val headX = toX(ragdoll.head.x)
  val headY = toY(ragdoll.head.y)
  val headR = ragdoll.head.radius * zoom
  val isHeadBroken = ragdoll.boneStatusMap[BoneType.CRANIUM]?.isBroken == true

  if (isHeadBroken) {
    // Glowing red fractured skull
    drawCircle(
      color = FractureRed.copy(alpha = 0.35f),
      radius = headR * 1.3f,
      center = Offset(headX, headY)
    )
    drawCircle(
      color = FractureRed,
      radius = headR,
      center = Offset(headX, headY)
    )
    drawLine(
      color = Color.White,
      start = Offset(headX - headR * 0.6f, headY),
      end = Offset(headX + headR * 0.6f, headY),
      strokeWidth = 2.5f * zoom
    )
  } else {
    // Pure dark head silhouette matching the reference image
    drawCircle(
      color = StickmanSilhouette,
      radius = headR,
      center = Offset(headX, headY)
    )
  }

  // 3. Draw Silhouette Joints
  val joints = listOf(
    ragdoll.neck, ragdoll.chest, ragdoll.pelvis,
    ragdoll.lShoulder, ragdoll.rShoulder,
    ragdoll.lElbow, ragdoll.rElbow,
    ragdoll.lHand, ragdoll.rHand,
    ragdoll.lHip, ragdoll.rHip,
    ragdoll.lKnee, ragdoll.rKnee,
    ragdoll.lFoot, ragdoll.rFoot
  )

  for (j in joints) {
    drawCircle(
      color = StickmanSilhouette,
      radius = (j.radius * 0.95f) * zoom,
      center = Offset(toX(j.x), toY(j.y))
    )
  }
}

private fun DrawScope.drawPushArrow(
  originX: Float,
  originY: Float,
  angleDegrees: Float,
  scale: Float,
  zoom: Float
) {
  val arrowLength = 48f * zoom * scale
  val rad = Math.toRadians(angleDegrees.toDouble())
  val targetX = originX + (cos(rad) * arrowLength).toFloat()
  val targetY = originY + (sin(rad) * arrowLength).toFloat()

  // Arrow shaft
  drawLine(
    color = PushArrowOrange,
    start = Offset(originX - (cos(rad) * 12f * zoom).toFloat(), originY - (sin(rad) * 12f * zoom).toFloat()),
    end = Offset(targetX, targetY),
    strokeWidth = 4.5f * zoom,
    cap = StrokeCap.Round
  )

  // Arrow head chevrons
  val headSize = 14f * zoom * scale
  val angle1 = rad + Math.toRadians(150.0)
  val angle2 = rad - Math.toRadians(150.0)

  val head1X = targetX + (cos(angle1) * headSize).toFloat()
  val head1Y = targetY + (sin(angle1) * headSize).toFloat()
  val head2X = targetX + (cos(angle2) * headSize).toFloat()
  val head2Y = targetY + (sin(angle2) * headSize).toFloat()

  val arrowPath = Path().apply {
    moveTo(targetX, targetY)
    lineTo(head1X, head1Y)
    lineTo(head2X, head2Y)
    close()
  }

  drawPath(path = arrowPath, color = PushArrowOrange)
}

private fun DrawScope.drawVehicle(
  vb: VehicleBody,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  if (vb.particles.isEmpty()) return

  // Chassis Struts
  for (c in vb.constraints) {
    drawLine(
      color = Color(0xFF2C2F33),
      start = Offset(toX(c.p1.x), toY(c.p1.y)),
      end = Offset(toX(c.p2.x), toY(c.p2.y)),
      strokeWidth = 4f * zoom,
      cap = StrokeCap.Round
    )
  }

  // Wheels
  if (vb.vehicle.wheelCount > 0 && vb.particles.size >= 6) {
    drawWheel(vb.wheelFront, toX, toY, zoom)
    drawWheel(vb.wheelRear, toX, toY, zoom)
  }
}

private fun DrawScope.drawWheel(
  wheel: Particle,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  val cx = toX(wheel.x)
  val cy = toY(wheel.y)
  val r = wheel.radius * zoom

  // Tire
  drawCircle(
    color = Color(0xFF232528),
    radius = r,
    center = Offset(cx, cy),
    style = Stroke(width = 4f * zoom)
  )

  // Spoke
  val spokeAngle = wheel.x * 0.08f
  drawLine(
    color = Color.White,
    start = Offset(cx - cos(spokeAngle) * r * 0.8f, cy - sin(spokeAngle) * r * 0.8f),
    end = Offset(cx + cos(spokeAngle) * r * 0.8f, cy + sin(spokeAngle) * r * 0.8f),
    strokeWidth = 2.5f * zoom
  )
}

private fun DrawScope.drawObstacles(
  obstacles: List<PlacedObstacle>,
  terrain: List<TerrainSegment>,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  for (obs in obstacles) {
    val worldX = obs.distanceAlongTrack
    val seg = terrain.firstOrNull { t ->
      val minX = min(t.x1, t.x2)
      val maxX = max(t.x1, t.x2)
      worldX in minX..maxX
    } ?: continue

    val ratio = if (seg.x2 != seg.x1) (worldX - seg.x1) / (seg.x2 - seg.x1) else 0f
    val worldY = seg.y1 + ratio * (seg.y2 - seg.y1)

    val sx = toX(worldX)
    val sy = toY(worldY)

    if (obs.isTriggered) {
      drawCircle(
        color = Color(0x66000000),
        radius = 16f * zoom,
        center = Offset(sx, sy)
      )
      continue
    }

    when (obs.type) {
      ObstacleType.LANDMINE -> {
        drawRect(
          color = Color(0xFF2C2F33),
          topLeft = Offset(sx - 14f * zoom, sy - 6f * zoom),
          size = Size(28f * zoom, 6f * zoom)
        )
        drawCircle(
          color = FractureRed,
          radius = 4f * zoom,
          center = Offset(sx, sy - 6f * zoom)
        )
      }

      ObstacleType.TNT_BARREL -> {
        val bw = 24f * zoom
        val bh = 34f * zoom
        drawRect(
          color = HazardOrange,
          topLeft = Offset(sx - bw / 2, sy - bh),
          size = Size(bw, bh)
        )
        drawRect(
          color = Color.Black,
          topLeft = Offset(sx - bw / 2, sy - bh * 0.6f),
          size = Size(bw, bh * 0.2f)
        )
      }

      ObstacleType.BRICK_WALL -> {
        val ww = 24f * zoom
        val wh = 54f * zoom
        drawRect(
          color = Color(0xFF8B0000),
          topLeft = Offset(sx - ww / 2, sy - wh),
          size = Size(ww, wh)
        )
      }

      ObstacleType.TURBO_PAD -> {
        drawRect(
          color = BoostBlue,
          topLeft = Offset(sx - 18f * zoom, sy - 4f * zoom),
          size = Size(36f * zoom, 4f * zoom)
        )
      }

      ObstacleType.PINBALL_BUMPER -> {
        drawCircle(
          color = PushArrowOrange,
          radius = 16f * zoom,
          center = Offset(sx, sy - 18f * zoom)
        )
        drawCircle(
          color = Color.White,
          radius = 7f * zoom,
          center = Offset(sx, sy - 18f * zoom)
        )
      }
    }
  }
}

private fun DrawScope.drawParticles(
  particles: List<VisualParticle>,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  for (p in particles) {
    val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
    val color = Color(p.colorHex).copy(alpha = alpha)
    drawCircle(
      color = color,
      radius = p.size * zoom * (0.5f + 0.5f * alpha),
      center = Offset(toX(p.x), toY(p.y))
    )
  }
}
