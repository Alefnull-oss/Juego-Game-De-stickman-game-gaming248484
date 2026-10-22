package com.example.physics

import com.example.model.BoneType
import kotlin.math.abs
import kotlin.math.hypot

data class DistanceConstraint(
  val p1: Particle,
  val p2: Particle,
  var restLength: Float = hypot(p2.x - p1.x, p2.y - p1.y),
  val stiffness: Float = 0.95f,
  val boneType: BoneType? = null,
  val breakable: Boolean = false,
  val breakThreshold: Float = 25f,
  var isBroken: Boolean = false
) {
  var lastDelta: Float = 0f

  fun solve(onBoneBroken: ((BoneType, Float) -> Unit)? = null) {
    if (isBroken) return

    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val currentDist = hypot(dx, dy)
    if (currentDist < 0.0001f) return

    val delta = currentDist - restLength
    lastDelta = delta

    // Check fracture stress
    if (breakable && abs(delta) > breakThreshold) {
      isBroken = true
      boneType?.let { bone ->
        val impactSpeed = (p1.speed + p2.speed) * 0.5f + abs(delta)
        onBoneBroken?.invoke(bone, impactSpeed)
      }
      return
    }

    val diff = (delta / currentDist) * 0.5f * stiffness

    val totalMass = p1.mass + p2.mass
    val m1Ratio = if (p1.isPinned) 0f else p2.mass / totalMass
    val m2Ratio = if (p2.isPinned) 0f else p1.mass / totalMass

    if (!p1.isPinned) {
      p1.x += dx * diff * m1Ratio
      p1.y += dy * diff * m1Ratio
    }
    if (!p2.isPinned) {
      p2.x -= dx * diff * m2Ratio
      p2.y -= dy * diff * m2Ratio
    }
  }
}
