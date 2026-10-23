package com.example.physics

import kotlin.math.hypot

data class Particle(
  var x: Float,
  var y: Float,
  var oldX: Float = x,
  var oldY: Float = y,
  val mass: Float = 1.0f,
  val radius: Float = 6.0f,
  var isPinned: Boolean = false,
  var friction: Float = 0.985f
) {
  var accX: Float = 0f
  var accY: Float = 0f

  val vx: Float get() = x - oldX
  val vy: Float get() = y - oldY
  val speed: Float get() = hypot(vx, vy)

  fun scaleDisplacement(ratio: Float) {
    if (isPinned) return
    oldX = x - (x - oldX) * ratio
    oldY = y - (y - oldY) * ratio
  }

  fun update(dt: Float, gravity: Float, timeDilation: Float = 1.0f) {
    if (isPinned) return

    val effectiveFriction = 1.0f - (1.0f - friction) * timeDilation.coerceIn(0.15f, 1.0f)
    val velX = (x - oldX) * effectiveFriction
    val velY = (y - oldY) * effectiveFriction

    oldX = x
    oldY = y

    x += velX + accX * dt * dt
    y += velY + (gravity + accY) * dt * dt

    accX = 0f
    accY = 0f
  }

  fun applyForce(fx: Float, fy: Float) {
    if (isPinned) return
    accX += fx / mass
    accY += fy / mass
  }

  fun applyImpulse(ix: Float, iy: Float) {
    if (isPinned) return
    oldX -= ix / mass
    oldY -= iy / mass
  }

  fun setPosition(newX: Float, newY: Float) {
    x = newX
    y = newY
    oldX = newX
    oldY = newY
  }
}
