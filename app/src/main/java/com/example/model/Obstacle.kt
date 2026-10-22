package com.example.model

enum class ObstacleType(
  val id: String,
  val displayName: String,
  val subtitle: String,
  val price: Int,
  val explosionForce: Float,
  val bonusPoints: Int,
  val isDestructible: Boolean
) {
  LANDMINE(
    id = "mine",
    displayName = "Mina Terrestre",
    subtitle = "Onda expansiva vertical extrema",
    price = 600,
    explosionForce = 48f,
    bonusPoints = 2500,
    isDestructible = false
  ),
  TNT_BARREL(
    id = "tnt",
    displayName = "Barril de Dinamita TNT",
    subtitle = "Explosión masiva omnidireccional",
    price = 1200,
    explosionForce = 62f,
    bonusPoints = 4000,
    isDestructible = false
  ),
  BRICK_WALL(
    id = "brick_wall",
    displayName = "Muro de Ladrillos",
    subtitle = "Colisión violenta destructible",
    price = 1800,
    explosionForce = 15f,
    bonusPoints = 3000,
    isDestructible = true
  ),
  TURBO_PAD(
    id = "turbo_pad",
    displayName = "Acelerador Turbo",
    subtitle = "Impulso cinético hacia adelante",
    price = 900,
    explosionForce = -35f, // Negative indicates forward horizontal vector
    bonusPoints = 1500,
    isDestructible = false
  ),
  PINBALL_BUMPER(
    id = "pinball_bumper",
    displayName = "Bumper Elástico",
    subtitle = "Rebote de alta energía cinética",
    price = 1400,
    explosionForce = 38f,
    bonusPoints = 2000,
    isDestructible = false
  );

  companion object {
    val DEFAULT_LIST = entries
  }
}

data class PlacedObstacle(
  val id: String,
  val type: ObstacleType,
  var distanceAlongTrack: Float, // distance along stage
  var isTriggered: Boolean = false,
  var triggerTimestamp: Long = 0L
)

data class VisualParticle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var life: Float, // 1.0 down to 0.0
  val maxLife: Float,
  val colorHex: Long,
  val size: Float
)
