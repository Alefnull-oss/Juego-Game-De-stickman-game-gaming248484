package com.example.model

data class Stage(
  val id: String,
  val name: String,
  val price: Int,
  val description: String,
  val slopeType: SlopeType,
  val startHeight: Float,
  val length: Float,
  val difficulty: String,
  val obstacleSlots: Int
) {
  companion object {
    val STAIRS = Stage(
      id = "stairs",
      name = "Escalera Mortal",
      price = 0,
      description = "Escalones pronunciados de concreto. Caídas continuas y fracturas múltiples.",
      slopeType = SlopeType.STAIRCASE,
      startHeight = 450f,
      length = 1800f,
      difficulty = "Media",
      obstacleSlots = 3
    )

    val MEGA_RAMP = Stage(
      id = "mega_ramp",
      name = "La Mega Rampa",
      price = 1500,
      description = "Descenso supersónico que culmina en un salto al vacío hacia una pared final.",
      slopeType = SlopeType.RAMP_AND_GAP,
      startHeight = 650f,
      length = 2400f,
      difficulty = "Alta",
      obstacleSlots = 4
    )

    val THE_CHASM = Stage(
      id = "the_chasm",
      name = "El Desfiladero Rocoso",
      price = 4000,
      description = "Pendientes dentadas con salientes afilados e impactos en ángulo.",
      slopeType = SlopeType.JAGGED_CHASM,
      startHeight = 550f,
      length = 2200f,
      difficulty = "Extrema",
      obstacleSlots = 4
    )

    val PINBALL_LOOP = Stage(
      id = "pinball_loop",
      name = "El Bucle Pinball",
      price = 8500,
      description = "Curvatura invertida de 360 grados con zonas de choque cinético.",
      slopeType = SlopeType.LOOP_DE_LOOP,
      startHeight = 700f,
      length = 2600f,
      difficulty = "Pesadilla",
      obstacleSlots = 5
    )

    val DEFAULT_LIST = listOf(
      STAIRS,
      MEGA_RAMP,
      THE_CHASM,
      PINBALL_LOOP
    )
  }
}

enum class SlopeType {
  STAIRCASE,
  RAMP_AND_GAP,
  JAGGED_CHASM,
  LOOP_DE_LOOP
}
