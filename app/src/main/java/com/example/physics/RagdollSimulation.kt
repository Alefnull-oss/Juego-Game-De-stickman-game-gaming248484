package com.example.physics

import com.example.model.BoneType
import com.example.model.DismountPose
import com.example.model.ObstacleType
import com.example.model.PlacedObstacle
import com.example.model.SlopeType
import com.example.model.Stage
import com.example.model.Vehicle
import com.example.model.VisualParticle
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
    particles.clear()

    obstacles.forEach { it.isTriggered = false }

    if (vehicle.wheelCount > 0) {
      val vBody = VehicleBody(vehicle, spawnX, spawnY)
      vehicleBody = vBody
      val riderX = spawnX + vehicle.riderSeatOffsetX
      val riderY = spawnY + vehicle.riderSeatOffsetY
      ragdoll = RagdollBody(riderX, riderY, pose)
      vBody.attachRider(ragdoll)
    } else {
      vehicleBody = null
      ragdoll = RagdollBody(spawnX, spawnY, pose)
    }

    cameraX = ragdoll.centerOfMassX
    cameraY = ragdoll.centerOfMassY
  }

  fun getPushJointPosition(): Pair<Float, Float> {
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

    val effectiveDt = dtSeconds * timeDilation
    simulationTimeSeconds += effectiveDt
    val dtSub = (effectiveDt / subSteps) * 60f // Normalized to ~1.0 per frame

    for (step in 0 until subSteps) {
      // 1. Update ragdoll particles
      for (p in ragdoll.particles) {
        p.update(dtSub, gravity)
      }

      // 2. Update vehicle particles
      vehicleBody?.particles?.forEach { p ->
        p.update(dtSub, gravity)
      }

      // 3. Solve ragdoll skeletal constraints with bone fracture detection
      for (c in ragdoll.constraints) {
        c.solve(onBoneBroken = { boneType, speed ->
          ragdoll.breakBone(boneType, speed)
          onBoneBroken?.invoke(boneType, speed)
          onSevereImpact?.invoke(speed, boneType)
          createFractureParticles(c.p1.x, c.p1.y)
        })
      }

      // 4. Solve vehicle constraints and rider attachment
      vehicleBody?.let { vb ->
        for (c in vb.constraints) {
          c.solve()
        }
        for (rc in vb.riderConstraints) {
          rc.solve()
        }
        vb.checkEjection {
          onRiderEjected?.invoke()
          onSevereImpact?.invoke(16f, null)
          createSparks(vb.chassisFront.x, vb.chassisFront.y, count = 12)
        }
      }

      // 5. Collide ragdoll with terrain
      var touchingGround = false
      for (p in ragdoll.particles) {
        for (t in terrain) {
          val impact = t.collideParticle(p)
          if (impact > 0.1f) {
            touchingGround = true
            if (impact > 11f) {
              createImpactParticles(p.x, p.y, impact)
            }
            if (impact > 14f) {
              onSevereImpact?.invoke(impact, null)
            }
          }
        }
      }

      // 6. Collide vehicle with terrain
      vehicleBody?.let { vb ->
        for (p in vb.particles) {
          for (t in terrain) {
            val impact = t.collideParticle(p)
            if (impact > 12f) {
              createSparks(p.x, p.y, count = 8)
            }
            if (impact > 18f) {
              onSevereImpact?.invoke(impact, null)
            }
          }
        }
      }

      // 7. Check Obstacle Triggers
      checkObstacleCollisions()
    }

    // Acrobatics tracking
    ragdoll.updateAcrobatics()

    // Speed tracking (km/h estimate)
    val curSpeed = max(ragdoll.averageSpeed, vehicleBody?.speed ?: 0f) * 3.6f * 4f
    if (curSpeed > maxSpeed) {
      maxSpeed = curSpeed
    }

    // Air time accumulation
    val isTouchingAny = ragdoll.particles.any { p ->
      terrain.any { t ->
        hypot(p.x - t.x1, p.y - t.y1) < 25f || hypot(p.x - t.x2, p.y - t.y2) < 25f
      }
    }
    if (!isTouchingAny && curSpeed > 4f) {
      airTimeSeconds += dtSeconds
    }

    // Update visual particles
    updateParticles(dtSeconds)

    // Continuously follow stickman smoothly
    val targetX = ragdoll.centerOfMassX
    val targetY = ragdoll.centerOfMassY
    cameraX += (targetX - cameraX) * 0.18f
    cameraY += (targetY - cameraY) * 0.18f

    // Check completion (settled down or time threshold)
    if (simulationTimeSeconds > 2.0f && curSpeed < 0.6f && !isFinished) {
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
          }
          ObstacleType.TNT_BARREL -> {
            applyExplosion(obsX, obsY, obs.type.explosionForce, radius = 130f)
            createExplosionEffect(obsX, obsY, isBig = true)
          }
          ObstacleType.BRICK_WALL -> {
            // High resistance impact
            ragdoll.applyImpulse(-15f, -6f)
            vehicleBody?.applyImpulse(-25f, -8f)
            createBrickDebris(obsX, obsY)
          }
          ObstacleType.TURBO_PAD -> {
            // Forward rocket boost
            ragdoll.applyImpulse(28f, -4f)
            vehicleBody?.applyImpulse(38f, -6f)
            createTurboStreak(obsX, obsY)
          }
          ObstacleType.PINBALL_BUMPER -> {
            // High velocity elastic bounce
            val dirX = (checkX - obsX)
            val dirY = (checkY - obsY)
            val len = max(1f, hypot(dirX, dirY))
            ragdoll.applyImpulse((dirX / len) * 32f, (dirY / len) * 32f)
            vehicleBody?.applyImpulse((dirX / len) * 35f, (dirY / len) * 35f)
            createSparks(obsX, obsY, count = 20)
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

  fun getCameraTarget(): Pair<Float, Float> {
    return Pair(cameraX, cameraY)
  }
}
