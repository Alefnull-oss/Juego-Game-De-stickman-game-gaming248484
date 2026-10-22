package com.example.model

enum class BoneType(
  val code: String,
  val clinicalName: String,
  val pointValue: Int,
  val breakThreshold: Float // Velocity / Force required to fracture
) {
  CRANIUM("CRAN", "Fractura de Cráneo", 2500, 16f),
  CERVICAL_SPINE("NECK", "Luxación Cervical (Cuello)", 2000, 13f),
  THORAX_RIBS("RIBS", "Fractura de Caja Torácica", 1400, 15f),
  LUMBAR_SPINE("LUMB", "Fractura Lumbar (Columna)", 1800, 14f),
  PELVIS("PELV", "Fractura Pélvica Severa", 1600, 16f),
  LEFT_HUMERUS("L_HUM", "Húmero Izquierdo", 800, 12f),
  RIGHT_HUMERUS("R_HUM", "Húmero Derecho", 800, 12f),
  LEFT_FOREARM("L_RAD", "Cúbito/Radio Izquierdo", 650, 11f),
  RIGHT_FOREARM("R_RAD", "Cúbito/Radio Derecho", 650, 11f),
  LEFT_FEMUR("L_FEM", "Fémur Izquierdo", 1200, 16f),
  RIGHT_FEMUR("R_FEM", "Fémur Derecho", 1200, 16f),
  LEFT_TIBIA("L_TIB", "Tibia/Peroné Izquierdo", 900, 13f),
  RIGHT_TIBIA("R_TIB", "Tibia/Peroné Derecho", 900, 13f)
}

data class BoneStatus(
  val type: BoneType,
  var isBroken: Boolean = false,
  var breakSpeed: Float = 0f,
  var breakCount: Int = 0
) {
  fun reset() {
    isBroken = false
    breakSpeed = 0f
    breakCount = 0
  }
}
