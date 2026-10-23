package com.example.model

data class Shockwave(
  var x: Float,
  var y: Float,
  var radius: Float = 6f,
  val maxRadius: Float = 64f,
  var life: Float = 1.0f,
  val colorHex: Long = 0xFFFFFFFFL
)

data class KineticBurstLine(
  var startX: Float,
  var startY: Float,
  var endX: Float,
  var endY: Float,
  var life: Float = 1.0f,
  val maxLife: Float = 1.0f,
  val strokeWidth: Float = 2.5f
)

data class SpeedStreak(
  var x: Float,
  var y: Float,
  var length: Float,
  var speed: Float,
  var life: Float = 1.0f
)
