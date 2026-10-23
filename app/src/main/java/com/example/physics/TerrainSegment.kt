package com.example.physics

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class TerrainSegment(
  val x1: Float,
  val y1: Float,
  val x2: Float,
  val y2: Float,
  val friction: Float = 0.88f,
  val restitution: Float = 0.22f,
  val isHazard: Boolean = false,
  val isBooster: Boolean = false
) {
  val length: Float = hypot(x2 - x1, y2 - y1)
  val dirX: Float = if (length > 0.0001f) (x2 - x1) / length else 1f
  val dirY: Float = if (length > 0.0001f) (y2 - y1) / length else 0f
  // Normal pointing outward (up/left depending on direction)
  val normX: Float = -dirY
  val normY: Float = dirX

  fun collideParticle(particle: Particle, timeDilation: Float = 1.0f): Float {
    if (length < 0.001f) return 0f

    // Vector from p1 to particle
    val px = particle.x - x1
    val py = particle.y - y1

    // Projection along segment
    val t = max(0f, min(length, px * dirX + py * dirY))
    val closestX = x1 + dirX * t
    val closestY = y1 + dirY * t

    val distVecX = particle.x - closestX
    val distVecY = particle.y - closestY
    val dist = hypot(distVecX, distVecY)

    if (dist < particle.radius && dist > 0.0001f) {
      val overlap = particle.radius - dist
      val nx = distVecX / dist
      val ny = distVecY / dist

      // Push particle out of surface
      particle.x += nx * overlap
      particle.y += ny * overlap

      // Velocity in normal and tangent direction
      val curVx = particle.vx
      val curVy = particle.vy
      val vNormal = curVx * nx + curVy * ny
      val vTangent = curVx * (-ny) + curVy * nx

      if (vNormal < 0f) {
        val impactForce = -vNormal

        val newVNormal = -vNormal * restitution
        // Scale sliding friction smoothly with timeDilation so slow motion preserves momentum
        val effectiveFriction = 1.0f - (1.0f - friction) * timeDilation.coerceIn(0.15f, 1.0f)
        val newVTangent = vTangent * effectiveFriction

        val finalVx = newVTangent * (-ny) + newVNormal * nx
        val finalVy = newVTangent * nx + newVNormal * ny

        particle.oldX = particle.x - finalVx
        particle.oldY = particle.y - finalVy

        return impactForce
      }
    }
    return 0f
  }
}
