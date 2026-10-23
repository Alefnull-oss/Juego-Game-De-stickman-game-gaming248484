package com.example.model

data class Vehicle(
  val id: String,
  val name: String,
  val category: String,
  val price: Int,
  val mass: Float,
  val speedMultiplier: Float,
  val bounce: Float,
  val wheelCount: Int,
  val wheelRadius: Float,
  val wheelDistance: Float, // Distance between front and rear axle
  val chassisWidth: Float,
  val chassisHeight: Float,
  val riderSeatOffsetX: Float,
  val riderSeatOffsetY: Float,
  val description: String,
  val ejectForceThreshold: Float, // Force needed to throw the stickman off the seat
  val wheelBreakThreshold: Float = 14f,
  val roofBreakThreshold: Float = 16f,
  val primaryColorHex: Long = 0xFF4A4D52L,
  val secondaryColorHex: Long = 0xFFFF9500L
) {
  companion object {
    val ON_FOOT = Vehicle(
      id = "on_foot",
      name = "A Pie (Sin Vehículo)",
      category = "Básico",
      price = 0,
      mass = 1.0f,
      speedMultiplier = 1.0f,
      bounce = 0.2f,
      wheelCount = 0,
      wheelRadius = 0f,
      wheelDistance = 0f,
      chassisWidth = 0f,
      chassisHeight = 0f,
      riderSeatOffsetX = 0f,
      riderSeatOffsetY = 0f,
      description = "Desmonte puro a cuerpo descubierto. Sensibilidad máxima de impacto.",
      ejectForceThreshold = 0f,
      wheelBreakThreshold = 0f,
      roofBreakThreshold = 0f,
      primaryColorHex = 0x00000000L,
      secondaryColorHex = 0x00000000L
    )

    val SHOPPING_CART = Vehicle(
      id = "shopping_cart",
      name = "Carro de Compras",
      category = "Clásico",
      price = 0,
      mass = 18f,
      speedMultiplier = 1.15f,
      bounce = 0.45f,
      wheelCount = 2,
      wheelRadius = 9f,
      wheelDistance = 38f,
      chassisWidth = 46f,
      chassisHeight = 28f,
      riderSeatOffsetX = 4f,
      riderSeatOffsetY = -10f,
      description = "Cesta de alambre inestable. El piloto sale despedido a la menor irregularidad.",
      ejectForceThreshold = 9f,
      wheelBreakThreshold = 8.5f,
      roofBreakThreshold = 9f,
      primaryColorHex = 0xFFADB5BDL, // Zinc wire grid
      secondaryColorHex = 0xFFFF3B30L // Red plastic handle
    )

    val ELECTRIC_SCOOTER = Vehicle(
      id = "scooter",
      name = "Patinete Eléctrico",
      category = "Urbano",
      price = 1200,
      mass = 22f,
      speedMultiplier = 1.35f,
      bounce = 0.35f,
      wheelCount = 2,
      wheelRadius = 8f,
      wheelDistance = 42f,
      chassisWidth = 48f,
      chassisHeight = 12f,
      riderSeatOffsetX = 0f,
      riderSeatOffsetY = -24f,
      description = "Rápido y con centro de gravedad alto. Provoca caídas frontales catastróficas.",
      ejectForceThreshold = 10f,
      wheelBreakThreshold = 10.5f,
      roofBreakThreshold = 11f,
      primaryColorHex = 0xFF212529L, // Matte black
      secondaryColorHex = 0xFF0A84FFL // Cyan electric accent
    )

    val STUNT_BIKE = Vehicle(
      id = "stunt_bike",
      name = "Moto Stunt X",
      category = "Deporte",
      price = 3500,
      mass = 45f,
      speedMultiplier = 1.6f,
      bounce = 0.4f,
      wheelCount = 2,
      wheelRadius = 14f,
      wheelDistance = 58f,
      chassisWidth = 64f,
      chassisHeight = 24f,
      riderSeatOffsetX = -5f,
      riderSeatOffsetY = -20f,
      description = "Gran inercia y velocidad de rampa. Eyección a alta velocidad garantizada.",
      ejectForceThreshold = 14f,
      wheelBreakThreshold = 15f,
      roofBreakThreshold = 14f,
      primaryColorHex = 0xFFFFD60AL, // Racing yellow
      secondaryColorHex = 0xFF1C1D21L // Dark carbon
    )

    val MUSCLE_CAR = Vehicle(
      id = "muscle_car",
      name = "Muscle Car V8",
      category = "Potencia",
      price = 7000,
      mass = 95f,
      speedMultiplier = 1.75f,
      bounce = 0.3f,
      wheelCount = 2,
      wheelRadius = 16f,
      wheelDistance = 84f,
      chassisWidth = 100f,
      chassisHeight = 32f,
      riderSeatOffsetX = -8f,
      riderSeatOffsetY = -18f,
      description = "Pesado como un yunque. Aplasta obstáculos antes de destrozar al conductor.",
      ejectForceThreshold = 22f,
      wheelBreakThreshold = 20f,
      roofBreakThreshold = 17f,
      primaryColorHex = 0xFFFF3B30L, // Muscle crimson
      secondaryColorHex = 0xFFF8F9FAL // White racing stripes
    )

    val ROCKET_SLED = Vehicle(
      id = "rocket_sled",
      name = "Trineo Cohete Turbo",
      category = "Extremo",
      price = 15000,
      mass = 75f,
      speedMultiplier = 2.2f,
      bounce = 0.5f,
      wheelCount = 2,
      wheelRadius = 11f,
      wheelDistance = 70f,
      chassisWidth = 85f,
      chassisHeight = 20f,
      riderSeatOffsetX = -12f,
      riderSeatOffsetY = -14f,
      description = "Aceleración sobrehumana con propulsión supersónica.",
      ejectForceThreshold = 18f,
      wheelBreakThreshold = 18f,
      roofBreakThreshold = 22f,
      primaryColorHex = 0xFFFF9500L, // Rocket booster orange
      secondaryColorHex = 0xFF30D158L // Turbo neon green
    )

    val DEFAULT_LIST = listOf(
      ON_FOOT,
      SHOPPING_CART,
      ELECTRIC_SCOOTER,
      STUNT_BIKE,
      MUSCLE_CAR,
      ROCKET_SLED
    )
  }
}
