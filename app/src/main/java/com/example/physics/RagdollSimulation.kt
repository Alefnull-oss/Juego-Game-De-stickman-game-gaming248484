package com.example.physics

import com.example.model.BoneType
import com.example.model.DismountPose
import com.example.model.KineticBurstLine
import com.example.model.ObstacleType
import com.example.model.PlacedObstacle
import com.example.model.Shockwave
import com.example.model.SlopeType
import com.example.model.SpeedStreak
import com.example.model.Stage
import com.example.model.Vehicle
import com.example.model.VisualParticle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class RagdollSimulation(
  var stage: Stage,
  var vehicle: Vehicle,
  var pose: DismountPose
) {
  val terrain = mutableListOf<TerrainSegment>()
  val obstacles = mutableListOf<PlacedObstacle>()
  val particles = mutableListOf<VisualParticle>()
  val shockwaves = mutableListOf<Shockwave>()
  val kineticBursts = mutableListOf<KineticBurstLine>()
  val speedStreaks = mutableListOf<SpeedStreak>()

  var onImpactAudio: ((String, Float) -> Unit)? = null

  lateinit var ragdoll: RagdollBody
  var vehicleBody: VehicleBody? = null

  var gravity: Float = 0.52f
  val subSteps: Int = 4

  var isSimulating: Boolean = false
  var isFinished: Boolean = false
  var simulationTimeSeconds: Float = 0f
  var airTimeSeconds: Float = 0f
  var maxSpeed: Float = 0f

  var timeDilation: Float = 1.0f
  private var lastTimeDilation: Float = 1.0f
  val isSlowMoActive: Boolean get() = timeDilation < 0.95f
  var pushJointIndex: Int = 0 // 0: Torso/Back, 1: Head, 2: Pelvis, 3: Knees/Feet

  var cameraX: Float = 120f
  var cameraY: Float = 150f

  var onBoneBroken: ((BoneType, Float) -> Unit)? = null
  var onSevereImpact: ((Float, BoneType?) -> Unit)? = null
  var onObstacleTriggered: ((ObstacleType) -> Unit)? = null
  var onRiderEjected: (() -> Unit)? = null

  // Spawn position
  var spawnX: Float = 120f
  var spawnY: Float = 150f

  init {
    buildTerrain()
    reset()
  }

  fun setConfiguration(newStage: Stage, newVehicle: Vehicle, newPose: DismountPose) {
    stage = newStage
    vehicle = newVehicle
    pose = newPose
    buildTerrain()
    reset()
  }

  fun buildTerrain() {
    terrain.clear()

    when (stage.slopeType) {
      SlopeType.STAIRCASE -> {
        // Flat start platform
        terrain.add(TerrainSegment(0f, 180f, 260f, 180f))

        // Steep staircase steps
        var curX = 260f
        var curY = 180f
        val stepW = 42f
        val stepH = 28f
        val numSteps = 30

        for (i in 0 until numSteps) {
          // Horizontal tread
          terrain.add(TerrainSegment(curX, curY, curX + stepW, curY))
          // Vertical riser
          terrain.add(TerrainSegment(curX + stepW, curY, curX + stepW, curY + stepH))
          curX += stepW
          curY += stepH
        }

        // Runout flat ground
        terrain.add(TerrainSegment(curX, curY, curX + 2200f, curY, friction = 0.92f))
        // Back wall
        terrain.add(TerrainSegment(curX + 2200f, curY, curX + 2200f, curY - 400f))
      }

      SlopeType.RAMP_AND_GAP -> {
        // Starting high flat
        terrain.add(TerrainSegment(0f, 120f, 200f, 120f))
        // Downward accelerator slope
        terrain.add(TerrainSegment(200f, 120f, 750f, 480f, friction = 0.99f))
        // Kick up ramp
        terrain.add(TerrainSegment(750f, 480f, 980f, 420f))

        // Giant gap into landing downhill
        terrain.add(TerrainSegment(1280f, 520f, 1750f, 620f))
        // Flat ground
        terrain.add(TerrainSegment(1750f, 620f, 3200f, 620f))
        // End impact crash wall
        terrain.add(TerrainSegment(3200f, 620f, 3200f, 200f))
      }

      SlopeType.JAGGED_CHASM -> {
        // High drop off
        terrain.add(TerrainSegment(0f, 140f, 220f, 140f))
        terrain.add(TerrainSegment(220f, 140f, 450f, 320f))
        // Jagged bump 1
        terrain.add(TerrainSegment(450f, 320f, 520f, 260f))
        terrain.add(TerrainSegment(520f, 260f, 700f, 420f))
        // Jagged bump 2
        terrain.add(TerrainSegment(700f, 420f, 780f, 360f))
        terrain.add(TerrainSegment(780f, 360f, 1100f, 560f))
        // Boulder pit
        terrain.add(TerrainSegment(1100f, 560f, 1250f, 500f))
        terrain.add(TerrainSegment(1250f, 500f, 1500f, 600f))
        terrain.add(TerrainSegment(1500f, 600f, 3000f, 600f))
        terrain.add(TerrainSegment(3000f, 600f, 3000f, 150f))
      }

      SlopeType.LOOP_DE_LOOP -> {
        // Steep launch chute
        terrain.add(TerrainSegment(0f, 100f, 180f, 100f))
        terrain.add(TerrainSegment(180f, 100f, 680f, 520f, friction = 0.99f))
        terrain.add(TerrainSegment(680f, 520f, 850f, 520f))

        // 360 degree loop arc approximation
        val loopCenterX = 1040f
        val loopCenterY = 360f
        val loopRadius = 160f
        val segments = 24
        var prevX = 850f
        var prevY = 520f

        for (i in 1..segments) {
          // Angle from 270 (bottom) clockwise through 360
          val angleDeg = 90.0 - (i.toDouble() / segments) * 360.0
          val rad = Math.toRadians(angleDeg)
          val nextX = loopCenterX + (cos(rad) * loopRadius).toFloat()
          val nextY = loopCenterY + (sin(rad) * loopRadius).toFloat()
          terrain.add(TerrainSegment(prevX, prevY, nextX, nextY, friction = 0.99f))
          prevX = nextX
          prevY = nextY
        }

        // Loop exit ramp to flat
        terrain.add(TerrainSegment(prevX, prevY, 1280f, 520f))
        terrain.add(TerrainSegment(1280f, 520f, 3200f, 520f))
        terrain.add(TerrainSegment(3200f, 520f, 3200f, 100f))
      }
    }

    spawnX = 140f
    spawnY = terrain.firstOrNull()?.y1?.minus(30f) ?: 120f
  }

  fun reset() {
    isSimulating = false
    isFinished = false
    simulationTimeSeconds = 0f
    airTimeSeconds = 0f
    maxSpeed = 0f
    timeDilation = 1.0f
    lastTimeDilation = 1.0f
    particles.clear()
    shockwaves.clear()
    kineticBursts.clear()
    speedStreaks.clear()

    obstacles.forEach { it.isTriggered = false }

    if (vehicle.wheelCount > 0) {
      val vBody = VehicleBody(vehicle, spawnX, spawnY)
      vehicleBody = vBody
      val riderX = spawnX + vehicle.riderSeatOffsetX
      val riderY = spawnY + vehicle.riderSeatOffsetY
      val effectivePose = if (pose == DismountPose.STANDING) DismountPose.SITTING else pose
      ragdoll = RagdollBody(riderX, riderY, effectivePose)
      vBody.attachRider(ragdoll)
    } else {
      vehicleBody = null
      ragdoll = RagdollBody(spawnX, spawnY, pose)
    }

    cameraX = ragdoll.centerOfMassX
    cameraY = ragdoll.centerOfMassY
  }

  fun getPushJointPosition(): Pair<Float, Float> {
    vehicleBody?.let { vb ->
      return Pair(vb.chassisRear.x - 14f, vb.chassisRear.y)
    }
    return when (pushJointIndex) {
      1 -> Pair(ragdoll.head.x - 8f, ragdoll.head.y) // Head
      2 -> Pair(ragdoll.pelvis.x - 8f, ragdoll.pelvis.y) // Pelvis
      3 -> Pair(ragdoll.lKnee.x - 8f, ragdoll.lKnee.y) // Knees / Legs
      else -> Pair(ragdoll.chest.x - 10f, ragdoll.chest.y) // Back / Torso
    }
  }

  fun launch(powerPercent: Float, angleDegrees: Float) {
    if (isSimulating) return
    isSimulating = true
    isFinished = false

    val power = (powerPercent.coerceIn(0.05f, 1.0f)) * 26f
    val rad = Math.toRadians(angleDegrees.toDouble())
    val impulseX = (cos(rad) * power).toFloat()
    val impulseY = (sin(rad) * power).toFloat()

    if (vehicleBody != null && vehicle.wheelCount > 0) {
      vehicleBody?.applyImpulse(impulseX, impulseY)
      ragdoll.applyImpulse(impulseX * 0.4f, impulseY * 0.4f)
    } else {
      // Apply primary impulse to selected push joint
      when (pushJointIndex) {
        1 -> {
          ragdoll.head.applyImpulse(impulseX * 1.5f, impulseY * 1.5f)
          ragdoll.chest.applyImpulse(impulseX * 0.7f, impulseY * 0.7f)
        }
        2 -> {
          ragdoll.pelvis.applyImpulse(impulseX * 1.5f, impulseY * 1.5f)
          ragdoll.chest.applyImpulse(impulseX * 0.8f, impulseY * 0.8f)
        }
        3 -> {
          ragdoll.lKnee.applyImpulse(impulseX * 1.4f, impulseY * 1.4f)
          ragdoll.rKnee.applyImpulse(impulseX * 1.4f, impulseY * 1.4f)
          ragdoll.pelvis.applyImpulse(impulseX * 0.6f, impulseY * 0.6f)
        }
        else -> {
          // Back / Chest
          ragdoll.chest.applyImpulse(impulseX * 1.6f, impulseY * 1.6f)
          ragdoll.pelvis.applyImpulse(impulseX * 0.8f, impulseY * 0.8f)
        }
      }
    }
  }

  fun update(dtSeconds: Float) {
    if (!isSimulating) return

    val currentDilation = timeDilation.coerceIn(0.15f, 1.0f)
    if (abs(currentDilation - lastTimeDilation) > 0.001f) {
      val ratio = currentDilation / lastTimeDilation
      ragdoll.particles.forEach { it.scaleDisplacement(ratio) }
      vehicleBody?.particles?.forEach { it.scaleDisplacement(ratio) }
      lastTimeDilation = currentDilation
    }

    val effectiveDt = dtSeconds * currentDilation
    simulationTimeSeconds += effectiveDt
    val dtSub = (effectiveDt / subSteps) * 60f // Normalized to ~1.0 per frame

    for (step in 0 until subSteps) {
      // 1. Update ragdoll particles
      for (p in ragdoll.particles) {
        p.update(dtSub, gravity, currentDilation)
      }

      // 2. Update vehicle particles
      vehicleBody?.particles?.forEach { p ->
        p.update(dtSub, gravity, currentDilation)
      }

      // 3. Solve ragdoll skeletal constraints with bone fracture detection
      for (c in ragdoll.constraints) {
        c.solve(onBoneBroken = { boneType, speed ->
          ragdoll.breakBone(boneType, speed)
          onBoneBroken?.invoke(boneType, speed)
          val trueImpact = speed / currentDilation
          onSevereImpact?.invoke(trueImpact, boneType)
          onImpactAudio?.invoke("bone_crunch", 1.0f)
          createFractureParticles(c.p1.x, c.p1.y)
          addShockwave(c.p1.x, c.p1.y, maxRadius = 75f, colorHex = 0xFFFF3B30L)
          addKineticBurst(c.p1.x, c.p1.y, count = 10, length = 32f)
        })
      }

      // 4. Solve vehicle constraints and rider attachment
      vehicleBody?.let { vb ->
        vb.solveConstraints { partName, px, py ->
          onImpactAudio?.invoke("metallic_clang", 1.0f)
          createSparks(px, py, count = 14)
          addShockwave(px, py, maxRadius = 70f, colorHex = 0xFFFFD60AL)
          addKineticBurst(px, py, count = 8, length = 28f)
          onSevereImpact?.invoke(14f, null)
        }
        for (rc in vb.riderConstraints) {
          rc.solve()
        }
        vb.checkEjection {
          onRiderEjected?.invoke()
          onSevereImpact?.invoke(16f, null)
          onImpactAudio?.invoke("metallic_clang", 1.0f)
          createSparks(vb.chassisFront.x, vb.chassisFront.y, count = 12)
          addShockwave(vb.chassisFront.x, vb.chassisFront.y, maxRadius = 65f, colorHex = 0xFFFF9500L)
        }
      }

      // 5. Collide ragdoll with terrain
      var touchingGround = false
      for (p in ragdoll.particles) {
        for (t in terrain) {
          val impact = t.collideParticle(p, currentDilation)
          if (impact > 0.1f) {
            touchingGround = true
            val trueImpact = impact / currentDilation
            if (trueImpact > 11f) {
              createImpactParticles(p.x, p.y, trueImpact)
              onImpactAudio?.invoke("thud", (trueImpact / 20f).coerceIn(0.4f, 1f))
              addShockwave(p.x, p.y, maxRadius = (trueImpact * 3.5f).coerceIn(24f, 65f), colorHex = 0xFFFFFFFFL)
              addKineticBurst(p.x, p.y, count = 6, length = 18f)
            }
            if (trueImpact > 14f) {
              onSevereImpact?.invoke(trueImpact, null)
            }
          }
        }
      }

      // 6. Collide vehicle with terrain
      vehicleBody?.let { vb ->
        for (p in vb.particles) {
          for (t in terrain) {
            val impact = t.collideParticle(p, currentDilation)
            val trueImpact = impact / currentDilation
            if (trueImpact > 12f) {
              createSparks(p.x, p.y, count = 8)
              onImpactAudio?.invoke("metallic_clang", (trueImpact / 20f).coerceIn(0.3f, 1f))
              addShockwave(p.x, p.y, maxRadius = 45f, colorHex = 0xFFCCCCCCL)
            }
            if (trueImpact > 18f) {
              onSevereImpact?.invoke(trueImpact, null)
            }
          }
        }
      }

      // 7. Check Vehicle-Ragdoll Collisions, Run-overs and Crush Damage
      checkVehicleRagdollCollisions(currentDilation)

      // 8. Check Obstacle Triggers
      checkObstacleCollisions()
    }

    // Update dynamic wheel rotation angles
    vehicleBody?.updateWheelRotation()

    // Acrobatics tracking
    ragdoll.updateAcrobatics()

    // Speed tracking (km/h estimate) - normalized by currentDilation for true physical speed
    val rawSpeed = max(ragdoll.averageSpeed, vehicleBody?.speed ?: 0f) * 3.6f * 4f
    val physicalSpeed = if (currentDilation > 0.05f) rawSpeed / currentDilation else rawSpeed
    if (physicalSpeed > maxSpeed) {
      maxSpeed = physicalSpeed
    }

    // Air time accumulation
    val isTouchingAny = ragdoll.particles.any { p ->
      terrain.any { t ->
        hypot(p.x - t.x1, p.y - t.y1) < 25f || hypot(p.x - t.x2, p.y - t.y2) < 25f
      }
    }
    if (!isTouchingAny && physicalSpeed > 4f) {
      airTimeSeconds += effectiveDt
    }

    // Speed streaks when moving fast
    if (physicalSpeed > 32f && Random.nextFloat() < 0.35f && speedStreaks.size < 18) {
      speedStreaks.add(
        SpeedStreak(
          x = ragdoll.centerOfMassX - Random.nextFloat() * 50f,
          y = ragdoll.centerOfMassY + (Random.nextFloat() - 0.5f) * 36f,
          length = (physicalSpeed * 1.6f).coerceIn(28f, 95f),
          speed = physicalSpeed * 0.4f
        )
      )
    }

    // Update all visual particles, shockwaves and burst lines
    updateVisualEffects(effectiveDt)

    // Continuously follow stickman smoothly
    val targetX = ragdoll.centerOfMassX
    val targetY = ragdoll.centerOfMassY
    cameraX += (targetX - cameraX) * 0.18f
    cameraY += (targetY - cameraY) * 0.18f

    // Check completion (settled down or time threshold)
    // Never finish while slow motion is active, and test true physical speed
    val isSlowMo = currentDilation < 0.90f
    if (simulationTimeSeconds > 2.0f && physicalSpeed < 0.6f && !isFinished && !isSlowMo) {
      isFinished = true
    }
  }

  private fun checkObstacleCollisions() {
    val checkX = ragdoll.centerOfMassX
    val checkY = ragdoll.centerOfMassY

    for (obs in obstacles) {
      if (obs.isTriggered) continue

      val obsX = obs.distanceAlongTrack
      val obsTerrain = findTerrainAtX(obsX) ?: continue
      val obsY = obsTerrain.y1

      val dist = hypot(checkX - obsX, checkY - obsY)
      val vehicleDist = vehicleBody?.let { hypot(it.centerOfMassX - obsX, it.centerOfMassY - obsY) } ?: 999f

      if (dist < 36f || vehicleDist < 42f) {
        obs.isTriggered = true
        obs.triggerTimestamp = System.currentTimeMillis()
        onObstacleTriggered?.invoke(obs.type)

        when (obs.type) {
          ObstacleType.LANDMINE -> {
            applyExplosion(obsX, obsY, obs.type.explosionForce, radius = 90f)
            createExplosionEffect(obsX, obsY, isBig = false)
            onImpactAudio?.invoke("explosion", 0.9f)
            addShockwave(obsX, obsY, maxRadius = 120f, colorHex = 0xFFFF9500L)
            addKineticBurst(obsX, obsY, count = 14, length = 45f)
          }
          ObstacleType.TNT_BARREL -> {
            applyExplosion(obsX, obsY, obs.type.explosionForce, radius = 130f)
            createExplosionEffect(obsX, obsY, isBig = true)
            onImpactAudio?.invoke("explosion", 1.0f)
            addShockwave(obsX, obsY, maxRadius = 160f, colorHex = 0xFFFF453AL)
            addKineticBurst(obsX, obsY, count = 20, length = 55f)
          }
          ObstacleType.BRICK_WALL -> {
            ragdoll.applyImpulse(-15f, -6f)
            vehicleBody?.applyImpulse(-25f, -8f)
            createBrickDebris(obsX, obsY)
            onImpactAudio?.invoke("thud", 1.0f)
            addShockwave(obsX, obsY, maxRadius = 80f, colorHex = 0xFFD2691EL)
          }
          ObstacleType.TURBO_PAD -> {
            ragdoll.applyImpulse(28f, -4f)
            vehicleBody?.applyImpulse(38f, -6f)
            createTurboStreak(obsX, obsY)
            onImpactAudio?.invoke("whoosh", 1.0f)
            addShockwave(obsX, obsY, maxRadius = 70f, colorHex = 0xFF0A84FFL)
          }
          ObstacleType.PINBALL_BUMPER -> {
            val dirX = (checkX - obsX)
            val dirY = (checkY - obsY)
            val len = max(1f, hypot(dirX, dirY))
            ragdoll.applyImpulse((dirX / len) * 32f, (dirY / len) * 32f)
            vehicleBody?.applyImpulse((dirX / len) * 35f, (dirY / len) * 35f)
            createSparks(obsX, obsY, count = 20)
            onImpactAudio?.invoke("whoosh", 1.0f)
            addShockwave(obsX, obsY, maxRadius = 85f, colorHex = 0xFFFFD60AL)
            addKineticBurst(obsX, obsY, count = 12, length = 35f)
          }
        }
      }
    }
  }

  private fun findTerrainAtX(x: Float): TerrainSegment? {
    return terrain.firstOrNull { t ->
      val minX = min(t.x1, t.x2)
      val maxX = max(t.x1, t.x2)
      x in minX..maxX
    } ?: terrain.minByOrNull { kotlin.math.abs(it.x1 - x) }
  }

  private fun applyExplosion(cx: Float, cy: Float, force: Float, radius: Float) {
    for (p in ragdoll.particles) {
      val dx = p.x - cx
      val dy = p.y - cy
      val dist = max(8f, hypot(dx, dy))
      if (dist < radius) {
        val factor = (1f - dist / radius) * force
        p.applyImpulse((dx / dist) * factor, (dy / dist) * factor - factor * 0.4f)
      }
    }
    vehicleBody?.particles?.forEach { p ->
      val dx = p.x - cx
      val dy = p.y - cy
      val dist = max(8f, hypot(dx, dy))
      if (dist < radius) {
        val factor = (1f - dist / radius) * force
        p.applyImpulse((dx / dist) * factor, (dy / dist) * factor - factor * 0.3f)
      }
    }
  }

  private fun createExplosionEffect(x: Float, y: Float, isBig: Boolean) {
    val count = if (isBig) 32 else 20
    for (i in 0 until count) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val speed = Random.nextFloat() * (if (isBig) 14f else 9f) + 2f
      val life = Random.nextFloat() * 0.6f + 0.4f
      val color = if (i % 3 == 0) 0xFFFF9F0AL else if (i % 3 == 1) 0xFFFF3B30L else 0xFFFFD60AL
      particles.add(
        VisualParticle(
          x = x,
          y = y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed - 3f,
          life = life,
          maxLife = life,
          colorHex = color,
          size = Random.nextFloat() * 8f + 4f
        )
      )
    }
  }

  private fun createFractureParticles(x: Float, y: Float) {
    for (i in 0 until 14) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val speed = Random.nextFloat() * 7f + 2f
      particles.add(
        VisualParticle(
          x = x,
          y = y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed - 2f,
          life = 0.5f,
          maxLife = 0.5f,
          colorHex = 0xFFFF3B30L, // Neon Red fracture spark
          size = 4f
        )
      )
    }
  }

  private fun createImpactParticles(x: Float, y: Float, impact: Float) {
    val count = min(10, (impact * 0.6f).toInt())
    for (i in 0 until count) {
      val angle = Random.nextFloat() * Math.PI.toFloat() + Math.PI.toFloat()
      val speed = Random.nextFloat() * 4f + 1f
      particles.add(
        VisualParticle(
          x = x,
          y = y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          life = 0.4f,
          maxLife = 0.4f,
          colorHex = 0xFF8B949EL,
          size = 3f
        )
      )
    }
  }

  private fun createSparks(x: Float, y: Float, count: Int) {
    for (i in 0 until count) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val speed = Random.nextFloat() * 9f + 2f
      particles.add(
        VisualParticle(
          x = x,
          y = y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed - 2f,
          life = 0.35f,
          maxLife = 0.35f,
          colorHex = 0xFFFFD60AL,
          size = 3.5f
        )
      )
    }
  }

  private fun createBrickDebris(x: Float, y: Float) {
    for (i in 0 until 18) {
      val angle = Random.nextFloat() * Math.PI.toFloat() + Math.PI.toFloat()
      val speed = Random.nextFloat() * 11f + 3f
      particles.add(
        VisualParticle(
          x = x,
          y = y - Random.nextFloat() * 40f,
          vx = cos(angle) * speed + 3f,
          vy = sin(angle) * speed,
          life = 0.8f,
          maxLife = 0.8f,
          colorHex = 0xFFD2691EL, // Brick brown
          size = Random.nextFloat() * 7f + 5f
        )
      )
    }
  }

  private fun createTurboStreak(x: Float, y: Float) {
    for (i in 0 until 16) {
      val speed = Random.nextFloat() * 12f + 6f
      particles.add(
        VisualParticle(
          x = x,
          y = y + (Random.nextFloat() - 0.5f) * 20f,
          vx = speed,
          vy = (Random.nextFloat() - 0.5f) * 3f,
          life = 0.4f,
          maxLife = 0.4f,
          colorHex = 0xFF0A84FFL, // Electric Blue
          size = 5f
        )
      )
    }
  }

  fun addShockwave(x: Float, y: Float, maxRadius: Float = 60f, colorHex: Long = 0xFFFFFFFFL) {
    if (shockwaves.size < 25) {
      shockwaves.add(Shockwave(x = x, y = y, radius = 6f, maxRadius = maxRadius, colorHex = colorHex))
    }
  }

  fun addKineticBurst(x: Float, y: Float, count: Int = 8, length: Float = 24f) {
    if (kineticBursts.size < 40) {
      for (i in 0 until count) {
        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        val dist = Random.nextFloat() * 8f + 6f
        val lineLen = Random.nextFloat() * length * 0.5f + length * 0.5f
        kineticBursts.add(
          KineticBurstLine(
            startX = x + (cos(angle) * dist),
            startY = y + (sin(angle) * dist),
            endX = x + (cos(angle) * (dist + lineLen)),
            endY = y + (sin(angle) * (dist + lineLen)),
            life = 0.35f,
            maxLife = 0.35f
          )
        )
      }
    }
  }

  private fun updateVisualEffects(dtSeconds: Float) {
    // 1. Update particles
    updateParticles(dtSeconds)

    // 2. Update shockwaves
    val waveIter = shockwaves.iterator()
    while (waveIter.hasNext()) {
      val w = waveIter.next()
      w.life -= dtSeconds * 2.2f
      w.radius += (w.maxRadius - w.radius) * 0.22f
      if (w.life <= 0f) {
        waveIter.remove()
      }
    }

    // 3. Update kinetic burst lines
    val burstIter = kineticBursts.iterator()
    while (burstIter.hasNext()) {
      val b = burstIter.next()
      b.life -= dtSeconds * 3.0f
      if (b.life <= 0f) {
        burstIter.remove()
      }
    }

    // 4. Update speed streaks
    val streakIter = speedStreaks.iterator()
    while (streakIter.hasNext()) {
      val s = streakIter.next()
      s.life -= dtSeconds * 3.0f
      s.x -= s.speed * dtSeconds * 32f
      if (s.life <= 0f) {
        streakIter.remove()
      }
    }
  }

  private fun updateParticles(dtSeconds: Float) {
    val iter = particles.iterator()
    while (iter.hasNext()) {
      val p = iter.next()
      p.life -= dtSeconds
      if (p.life <= 0f) {
        iter.remove()
      } else {
        p.x += p.vx
        p.y += p.vy
        p.vy += gravity * 0.4f
      }
    }
  }

  private fun checkVehicleRagdollCollisions(currentDilation: Float) {
    val vb = vehicleBody ?: return
    if (vehicle.wheelCount == 0) return

    val vehicleParts = vb.particles
    val isEjected = vb.isRiderEjected

    // 1. If rider is still in vehicle: check roof rollover crush on head/neck
    if (!isEjected) {
      val isRolledOver = vb.chassisTop.y > vb.chassisSeat.y - 4f
      val headNearGround = terrain.any { t ->
        val px = ragdoll.head.x - t.x1
        val py = ragdoll.head.y - t.y1
        val proj = max(0f, min(t.length, px * t.dirX + py * t.dirY))
        val cx = t.x1 + t.dirX * proj
        val cy = t.y1 + t.dirY * proj
        hypot(ragdoll.head.x - cx, ragdoll.head.y - cy) < (ragdoll.head.radius + 10f)
      }

      if (isRolledOver && headNearGround) {
        val rolloverImpact = (vb.speed / currentDilation) * 1.8f + 14f
        if (rolloverImpact > 11f) {
          ragdoll.breakBone(BoneType.CRANIUM, rolloverImpact)
          ragdoll.breakBone(BoneType.CERVICAL_SPINE, rolloverImpact)
          onBoneBroken?.invoke(BoneType.CRANIUM, rolloverImpact)
          onBoneBroken?.invoke(BoneType.CERVICAL_SPINE, rolloverImpact)
          onSevereImpact?.invoke(rolloverImpact, BoneType.CRANIUM)
          onImpactAudio?.invoke("bone_crunch", 1.0f)
          createFractureParticles(ragdoll.head.x, ragdoll.head.y)
          addShockwave(ragdoll.head.x, ragdoll.head.y, maxRadius = 75f, colorHex = 0xFFFF3B30L)
          addKineticBurst(ragdoll.head.x, ragdoll.head.y, count = 12, length = 35f)
        }
      }
    }

    // 2. Active collision between all vehicle particles and ragdoll particles
    for (vp in vehicleParts) {
      for (rp in ragdoll.particles) {
        // If rider is not ejected, don't self-collide anchor joints
        if (!isEjected) {
          if (vp === vb.chassisSeat && (rp === ragdoll.pelvis || rp === ragdoll.chest)) continue
          if (vp === vb.steeringCol && (rp === ragdoll.lHand || rp === ragdoll.rHand)) continue
          if (vp === vb.chassisFloor && (rp === ragdoll.lFoot || rp === ragdoll.rFoot)) continue
        }

        val dx = rp.x - vp.x
        val dy = rp.y - vp.y
        val dist = hypot(dx, dy)
        val minDist = vp.radius + rp.radius + 2.5f

        if (dist < minDist && dist > 0.001f) {
          val nx = dx / dist
          val ny = dy / dist
          val overlap = minDist - dist

          val totalMass = vp.mass + rp.mass
          val rRatio = vp.mass / totalMass
          val vRatio = rp.mass / totalMass

          // Positional push
          rp.x += nx * overlap * rRatio
          rp.y += ny * overlap * rRatio
          vp.x -= nx * overlap * vRatio
          vp.y -= ny * overlap * vRatio

          // Relative velocity calculation
          val relVx = rp.vx - vp.vx
          val relVy = rp.vy - vp.vy
          val approachSpeed = -(relVx * nx + relVy * ny)

          if (approachSpeed > 0f) {
            val restitution = 0.35f
            val impulse = approachSpeed * (1f + restitution) * (vp.mass * rp.mass / totalMass)
            rp.oldX -= (nx * impulse / rp.mass)
            rp.oldY -= (ny * impulse / rp.mass)
            vp.oldX += (nx * impulse / vp.mass)
            vp.oldY += (ny * impulse / vp.mass)
          }

          var impactStrength = (max(0f, approachSpeed) / currentDilation) + (vp.speed / currentDilation) * 0.45f

          // Ground crush check (sandwich between heavy vehicle chassis/wheel and ground)
          val isNearGround = terrain.any { t ->
            val px = rp.x - t.x1
            val py = rp.y - t.y1
            val proj = max(0f, min(t.length, px * t.dirX + py * t.dirY))
            val cx = t.x1 + t.dirX * proj
            val cy = t.y1 + t.dirY * proj
            hypot(rp.x - cx, rp.y - cy) < (rp.radius + 10f)
          }

          if (isNearGround && ny > 0.25f) {
            impactStrength += (vp.mass * 0.22f) + 8f
          }

          if (impactStrength > 7.5f) {
            val bone = getBoneForParticle(rp)
            bone?.let { b ->
              ragdoll.breakBone(b, impactStrength)
              onBoneBroken?.invoke(b, impactStrength)
            }
            onSevereImpact?.invoke(impactStrength, bone)
            onImpactAudio?.invoke("bone_crunch", 1.0f)
            onImpactAudio?.invoke("metallic_clang", 0.85f)
            createImpactParticles(rp.x, rp.y, impactStrength)
            createSparks(vp.x, vp.y, count = 8)
            addShockwave(rp.x, rp.y, maxRadius = (impactStrength * 3.2f).coerceIn(30f, 85f), colorHex = 0xFFFF453AL)
            addKineticBurst(rp.x, rp.y, count = 8, length = 25f)
          }
        }
      }
    }
  }

  private fun getBoneForParticle(p: Particle): BoneType? {
    return when {
      p === ragdoll.head -> BoneType.CRANIUM
      p === ragdoll.neck -> BoneType.CERVICAL_SPINE
      p === ragdoll.chest -> BoneType.THORAX_RIBS
      p === ragdoll.pelvis -> BoneType.PELVIS
      p === ragdoll.lKnee || p === ragdoll.lHip -> BoneType.LEFT_FEMUR
      p === ragdoll.lFoot -> BoneType.LEFT_TIBIA
      p === ragdoll.rKnee || p === ragdoll.rHip -> BoneType.RIGHT_FEMUR
      p === ragdoll.rFoot -> BoneType.RIGHT_TIBIA
      p === ragdoll.lShoulder || p === ragdoll.lElbow -> BoneType.LEFT_HUMERUS
      p === ragdoll.lHand -> BoneType.LEFT_FOREARM
      p === ragdoll.rShoulder || p === ragdoll.rElbow -> BoneType.RIGHT_HUMERUS
      p === ragdoll.rHand -> BoneType.RIGHT_FOREARM
      else -> null
    }
  }

  fun getCameraTarget(): Pair<Float, Float> {
    return Pair(cameraX, cameraY)
  }
}
