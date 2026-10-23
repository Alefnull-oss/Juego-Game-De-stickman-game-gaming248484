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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoneType
import com.example.model.KineticBurstLine
import com.example.model.ObstacleType
import com.example.model.PlacedObstacle
import com.example.model.Shockwave
import com.example.model.SpeedStreak
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
  shakeOffsetX: Float = 0f,
  shakeOffsetY: Float = 0f,
  shakeAngle: Float = 0f,
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

      // Camera centering strictly tracking the stickman + dynamic trauma shake
      val (targetX, targetY) = simulation.getCameraTarget()
      val offsetX = canvasWidth * 0.40f - targetX * zoom + shakeOffsetX
      val offsetY = canvasHeight * 0.50f - targetY * zoom + shakeOffsetY

      fun toScreenX(worldX: Float) = worldX * zoom + offsetX
      fun toScreenY(worldY: Float) = worldY * zoom + offsetY

      rotate(degrees = shakeAngle, pivot = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f)) {
        // 1. Draw Studio Background with Floating Cubes & Vignette
        drawStudioBackground(canvasWidth, canvasHeight, offsetX, offsetY)

        // 2. Draw Solid Grey Platforms and Terrain
        drawSolidTerrain(simulation.terrain, ::toScreenX, ::toScreenY, zoom, canvasHeight)

        // 3. Draw Placed Obstacles
        drawObstacles(simulation.obstacles, simulation.terrain, ::toScreenX, ::toScreenY, zoom)

        // 4a. Draw Vehicle Rear & Interior Layer (Rear wheels, interior seat, background frame)
        simulation.vehicleBody?.let { vb ->
          drawVehicleRearLayer(vb, ::toScreenX, ::toScreenY, zoom)
        }

        // 5. Draw Stickman with Dark Silhouette & Internal X-Ray Glow (Sitting inside cockpit)
        drawStickmanWithXRay(simulation.ragdoll, ::toScreenX, ::toScreenY, zoom)

        // 4b. Draw Vehicle Front/Exterior Layer (Cockpit sides, steering wheel, front panels, outer wheels)
        simulation.vehicleBody?.let { vb ->
          drawVehicleFrontLayer(vb, ::toScreenX, ::toScreenY, zoom)
        }

        // 4c. Draw Detached Vehicle Parts & Flying Scrap Debris
        simulation.vehicleBody?.let { vb ->
          drawVehicleDebris(vb, ::toScreenX, ::toScreenY, zoom)
        }

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

        // 7b. Draw Speed Streaks
        drawSpeedStreaks(simulation.speedStreaks, ::toScreenX, ::toScreenY, zoom)

        // 7c. Draw Shockwaves
        drawShockwaves(simulation.shockwaves, ::toScreenX, ::toScreenY, zoom)

        // 7d. Draw Kinetic Burst Lines
        drawKineticBursts(simulation.kineticBursts, ::toScreenX, ::toScreenY, zoom)

        // 8. Slow-Mo Vignette Tint
        if (isSlowMoActive) {
          drawRect(
            color = FractureRed.copy(alpha = 0.08f),
            size = size
          )
        }
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

private fun DrawScope.drawVehicleRearLayer(
  vb: VehicleBody,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  if (vb.particles.isEmpty() || vb.vehicle.wheelCount == 0) return

  val seatX = toX(vb.chassisSeat.x)
  val seatY = toY(vb.chassisSeat.y)
  val rearX = toX(vb.chassisRear.x)
  val rearY = toY(vb.chassisRear.y)
  val floorX = toX(vb.chassisFloor.x)
  val floorY = toY(vb.chassisFloor.y)

  // 1. Interior Cockpit Shadow / Backdrop
  val cockpitBackdrop = Path().apply {
    moveTo(rearX, rearY)
    lineTo(seatX, seatY)
    lineTo(floorX, floorY)
    close()
  }
  drawPath(path = cockpitBackdrop, color = Color(0x99181A1D))

  // 2. Interior Seat Backrest
  drawLine(
    color = Color(0xFF1E2024),
    start = Offset(seatX - 4f * zoom, seatY - 14f * zoom),
    end = Offset(seatX, seatY + 4f * zoom),
    strokeWidth = 6f * zoom,
    cap = StrokeCap.Round
  )

  // 3. Interior Structural Frame Struts (Behind driver)
  drawLine(
    color = Color(0xFF2C2F33),
    start = Offset(rearX, rearY),
    end = Offset(floorX, floorY),
    strokeWidth = 4f * zoom,
    cap = StrokeCap.Round
  )
  drawLine(
    color = Color(0xFF3A3E45),
    start = Offset(rearX, rearY),
    end = Offset(seatX, seatY),
    strokeWidth = 3.5f * zoom,
    cap = StrokeCap.Round
  )

  // 4. Rear Wheel (Inner side / back layer)
  if (!vb.isRearWheelDetached) {
    drawWheelDetailed(
      cx = toX(vb.wheelRear.x),
      cy = toY(vb.wheelRear.y),
      radius = vb.vehicle.wheelRadius * zoom,
      rotationAngle = vb.rearWheelAngle,
      rimColor = Color(vb.vehicle.secondaryColorHex),
      zoom = zoom,
      isInnerWheel = true
    )
  }
}

private fun DrawScope.drawVehicleFrontLayer(
  vb: VehicleBody,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  if (vb.particles.isEmpty() || vb.vehicle.wheelCount == 0) return

  val frontX = toX(vb.chassisFront.x)
  val frontY = toY(vb.chassisFront.y)
  val steerX = toX(vb.steeringCol.x)
  val steerY = toY(vb.steeringCol.y)
  val floorX = toX(vb.chassisFloor.x)
  val floorY = toY(vb.chassisFloor.y)
  val seatX = toX(vb.chassisSeat.x)
  val seatY = toY(vb.chassisSeat.y)
  val topX = toX(vb.chassisTop.x)
  val topY = toY(vb.chassisTop.y)
  val rearX = toX(vb.chassisRear.x)
  val rearY = toY(vb.chassisRear.y)

  val primaryCol = Color(vb.vehicle.primaryColorHex)
  val secondaryCol = Color(vb.vehicle.secondaryColorHex)

  when (vb.vehicle.id) {
    "shopping_cart" -> {
      // Wire mesh basket sides
      val basketPath = Path().apply {
        moveTo(frontX, frontY - 10f * zoom)
        lineTo(frontX, frontY + 8f * zoom)
        lineTo(rearX, rearY + 6f * zoom)
        lineTo(steerX, steerY - 6f * zoom)
        close()
      }
      drawPath(path = basketPath, color = primaryCol.copy(alpha = 0.25f))

      // Wire grid lines
      val gridSteps = 5
      for (i in 1..gridSteps) {
        val t = i.toFloat() / (gridSteps + 1)
        val x1 = frontX * (1 - t) + rearX * t
        val y1 = (frontY - 10f * zoom) * (1 - t) + (steerY - 6f * zoom) * t
        val x2 = frontX * (1 - t) + rearX * t
        val y2 = (frontY + 8f * zoom) * (1 - t) + (rearY + 6f * zoom) * t
        drawLine(
          color = primaryCol,
          start = Offset(x1, y1),
          end = Offset(x2, y2),
          strokeWidth = 1.8f * zoom
        )
      }

      // Upper rim bar
      drawLine(
        color = Color.White.copy(alpha = 0.8f),
        start = Offset(frontX, frontY - 10f * zoom),
        end = Offset(steerX, steerY - 6f * zoom),
        strokeWidth = 3f * zoom,
        cap = StrokeCap.Round
      )

      // Red Plastic Handlebar (In front of stickman's hands)
      drawLine(
        color = secondaryCol,
        start = Offset(steerX - 4f * zoom, steerY - 8f * zoom),
        end = Offset(steerX + 6f * zoom, steerY - 4f * zoom),
        strokeWidth = 5.5f * zoom,
        cap = StrokeCap.Round
      )
    }

    "muscle_car" -> {
      // Car Body Shell (Sleek muscle car front hood and side sill)
      val bodyPath = Path().apply {
        moveTo(frontX + 6f * zoom, frontY + 4f * zoom)
        lineTo(frontX - 8f * zoom, frontY - 8f * zoom)
        lineTo(steerX, steerY - 4f * zoom)
        if (!vb.isRoofCrushed) {
          lineTo(topX, topY)
          lineTo(rearX, rearY - 8f * zoom)
        } else {
          // Crushed crumpled roof line
          lineTo(seatX, seatY - 6f * zoom)
          lineTo(rearX, rearY + 2f * zoom)
        }
        lineTo(rearX - 4f * zoom, rearY + 6f * zoom)
        lineTo(floorX, floorY + 4f * zoom)
        close()
      }
      drawPath(path = bodyPath, color = primaryCol)

      // White Racing Stripes
      drawLine(
        color = secondaryCol,
        start = Offset(frontX - 6f * zoom, frontY - 6f * zoom),
        end = Offset(steerX, steerY - 3f * zoom),
        strokeWidth = 3f * zoom
      )

      // Windshield A-pillar
      if (!vb.isRoofCrushed) {
        drawLine(
          color = Color(0x66FFFFFF),
          start = Offset(steerX, steerY - 4f * zoom),
          end = Offset(topX, topY),
          strokeWidth = 2.5f * zoom
        )
      }

      // Steering wheel in front of rider
      drawCircle(
        color = Color(0xFF1E2024),
        radius = 5.5f * zoom,
        center = Offset(steerX, steerY),
        style = Stroke(width = 2.5f * zoom)
      )

      // Front Headlight Glow
      drawCircle(
        color = CashGold,
        radius = 4f * zoom,
        center = Offset(frontX + 4f * zoom, frontY - 2f * zoom)
      )
    }

    "stunt_bike" -> {
      // Telescopic Front Fork down to front wheel
      if (!vb.isFrontWheelDetached) {
        drawLine(
          color = Color(0xFF8B949E),
          start = Offset(steerX, steerY),
          end = Offset(toX(vb.wheelFront.x), toY(vb.wheelFront.y)),
          strokeWidth = 3.5f * zoom,
          cap = StrokeCap.Round
        )
      }

      // Fuel tank
      val tankPath = Path().apply {
        moveTo(steerX, steerY)
        lineTo(seatX, seatY - 4f * zoom)
        lineTo(floorX, floorY)
        close()
      }
      drawPath(path = tankPath, color = primaryCol)

      // Handlebars and grips
      drawLine(
        color = secondaryCol,
        start = Offset(steerX - 3f * zoom, steerY - 6f * zoom),
        end = Offset(steerX + 5f * zoom, steerY - 2f * zoom),
        strokeWidth = 4.5f * zoom,
        cap = StrokeCap.Round
      )
    }

    "scooter" -> {
      // Scooter Deck
      drawLine(
        color = primaryCol,
        start = Offset(rearX, rearY + 2f * zoom),
        end = Offset(floorX + 10f * zoom, floorY + 2f * zoom),
        strokeWidth = 5f * zoom,
        cap = StrokeCap.Round
      )

      // Steering stem tube
      drawLine(
        color = primaryCol,
        start = Offset(floorX + 8f * zoom, floorY + 2f * zoom),
        end = Offset(steerX, steerY),
        strokeWidth = 4f * zoom,
        cap = StrokeCap.Round
      )

      // Cyan accent line
      drawLine(
        color = secondaryCol,
        start = Offset(steerX, steerY),
        end = Offset(steerX + 4f * zoom, steerY - 2f * zoom),
        strokeWidth = 4f * zoom,
        cap = StrokeCap.Round
      )
    }

    else -> {
      // Default / Rocket Sled
      val sledPath = Path().apply {
        moveTo(frontX + 8f * zoom, frontY)
        lineTo(steerX, steerY - 4f * zoom)
        lineTo(rearX, rearY - 4f * zoom)
        lineTo(floorX, floorY + 4f * zoom)
        close()
      }
      drawPath(path = sledPath, color = primaryCol)

      // Booster flame/thruster rim
      drawCircle(
        color = secondaryCol,
        radius = 5f * zoom,
        center = Offset(rearX - 4f * zoom, rearY)
      )

      // Control joystick
      drawLine(
        color = Color.White,
        start = Offset(steerX, steerY + 4f * zoom),
        end = Offset(steerX, steerY - 6f * zoom),
        strokeWidth = 3.5f * zoom,
        cap = StrokeCap.Round
      )
    }
  }

  // Outer Front Wheel
  if (!vb.isFrontWheelDetached) {
    drawWheelDetailed(
      cx = toX(vb.wheelFront.x),
      cy = toY(vb.wheelFront.y),
      radius = vb.vehicle.wheelRadius * zoom,
      rotationAngle = vb.frontWheelAngle,
      rimColor = secondaryCol,
      zoom = zoom,
      isInnerWheel = false
    )
  }

  // If front is crushed, draw crumpled impact marks
  if (vb.isFrontCrushed) {
    drawLine(
      color = FractureRed,
      start = Offset(frontX - 6f * zoom, frontY - 6f * zoom),
      end = Offset(frontX + 4f * zoom, frontY + 6f * zoom),
      strokeWidth = 2.5f * zoom
    )
  }
}

private fun DrawScope.drawVehicleDebris(
  vb: VehicleBody,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  if (vb.detachedDebris.isEmpty()) return

  for (deb in vb.detachedDebris) {
    val cx = toX(deb.particle.x)
    val cy = toY(deb.particle.y)

    if (deb.isWheel) {
      drawWheelDetailed(
        cx = cx,
        cy = cy,
        radius = deb.wheelRadius * zoom,
        rotationAngle = deb.rotationAngle,
        rimColor = Color(deb.colorHex),
        zoom = zoom,
        isInnerWheel = false
      )
    } else {
      // Rotating scrap metal shard
      val s = deb.size * zoom
      rotate(degrees = Math.toDegrees(deb.rotationAngle.toDouble()).toFloat(), pivot = Offset(cx, cy)) {
        val shardPath = Path().apply {
          moveTo(cx - s * 0.6f, cy - s * 0.3f)
          lineTo(cx + s * 0.7f, cy - s * 0.5f)
          lineTo(cx + s * 0.4f, cy + s * 0.6f)
          lineTo(cx - s * 0.5f, cy + s * 0.4f)
          close()
        }
        drawPath(path = shardPath, color = Color(deb.colorHex))
        drawPath(path = shardPath, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = 1.5f * zoom))
      }
    }
  }
}

private fun DrawScope.drawWheelDetailed(
  cx: Float,
  cy: Float,
  radius: Float,
  rotationAngle: Float,
  rimColor: Color,
  zoom: Float,
  isInnerWheel: Boolean
) {
  val tireColor = if (isInnerWheel) Color(0xFF181A1D) else Color(0xFF232528)
  val spokeColor = if (isInnerWheel) Color(0xFF6B7280) else Color(0xFFE5E7EB)

  // Outer Tire Rubber
  drawCircle(
    color = tireColor,
    radius = radius,
    center = Offset(cx, cy)
  )

  // Inner Rim
  drawCircle(
    color = rimColor.copy(alpha = if (isInnerWheel) 0.5f else 0.85f),
    radius = radius * 0.65f,
    center = Offset(cx, cy),
    style = Stroke(width = 2.5f * zoom)
  )

  // Rotating spokes (4 cross spokes)
  for (i in 0 until 4) {
    val angle = rotationAngle + (i * Math.PI.toFloat() / 2f)
    val xOffset = cos(angle) * radius * 0.82f
    val yOffset = sin(angle) * radius * 0.82f
    drawLine(
      color = spokeColor,
      start = Offset(cx, cy),
      end = Offset(cx + xOffset, cy + yOffset),
      strokeWidth = 2f * zoom,
      cap = StrokeCap.Round
    )
  }

  // Wheel center hub
  drawCircle(
    color = if (isInnerWheel) Color(0xFF374151) else Color.White,
    radius = 2.5f * zoom,
    center = Offset(cx, cy)
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

private fun DrawScope.drawShockwaves(
  shockwaves: List<Shockwave>,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  for (sw in shockwaves) {
    val alpha = sw.life.coerceIn(0f, 1f)
    if (alpha <= 0.01f) continue
    val center = Offset(toX(sw.x), toY(sw.y))
    val r = sw.radius * zoom
    // Sharp technical shockwave ring
    drawCircle(
      color = Color(sw.colorHex).copy(alpha = alpha * 0.85f),
      radius = r,
      center = center,
      style = Stroke(width = max(1.5f, 3.5f * alpha * zoom))
    )
    if (r > 12f * zoom) {
      drawCircle(
        color = Color(sw.colorHex).copy(alpha = alpha * 0.4f),
        radius = r * 0.65f,
        center = center,
        style = Stroke(width = max(1f, 1.8f * alpha * zoom))
      )
    }
  }
}

private fun DrawScope.drawKineticBursts(
  bursts: List<KineticBurstLine>,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  for (burst in bursts) {
    val progress = (burst.life / burst.maxLife).coerceIn(0f, 1f)
    if (progress <= 0.01f) continue
    val p1 = Offset(toX(burst.startX), toY(burst.startY))
    val p2 = Offset(toX(burst.endX), toY(burst.endY))
    drawLine(
      color = Color.White.copy(alpha = progress),
      start = p1,
      end = p2,
      strokeWidth = burst.strokeWidth * zoom * progress,
      cap = StrokeCap.Round
    )
  }
}

private fun DrawScope.drawSpeedStreaks(
  streaks: List<SpeedStreak>,
  toX: (Float) -> Float,
  toY: (Float) -> Float,
  zoom: Float
) {
  for (s in streaks) {
    val alpha = s.life.coerceIn(0f, 1f) * 0.65f
    if (alpha <= 0.01f) continue
    val sx = toX(s.x)
    val sy = toY(s.y)
    val len = s.length * zoom
    drawLine(
      color = Color.White.copy(alpha = alpha),
      start = Offset(sx - len, sy),
      end = Offset(sx, sy),
      strokeWidth = 2f * zoom,
      cap = StrokeCap.Round
    )
  }
}
