package com.example.model

enum class DismountPose(
  val id: String,
  val displayName: String,
  val subtitle: String,
  val initialTorsoAngle: Float, // in degrees
  val armPose: String,
  val legPose: String
) {
  STANDING(
    id = "standing",
    displayName = "De Pie",
    subtitle = "Exposición frontal total",
    initialTorsoAngle = -90f,
    armPose = "DOWN",
    legPose = "STRAIGHT"
  ),
  SUPERMAN(
    id = "superman",
    displayName = "Superman",
    subtitle = "Brazos adelante, máximo planeo",
    initialTorsoAngle = 0f,
    armPose = "FORWARD",
    legPose = "STRAIGHT"
  ),
  SITTING(
    id = "sitting",
    displayName = "Sentado",
    subtitle = "Centro de gravedad bajo para rodar",
    initialTorsoAngle = -80f,
    armPose = "FORWARD",
    legPose = "BENT_90"
  ),
  CROUCH(
    id = "crouch",
    displayName = "Agachado",
    subtitle = "Efecto resorte compacto",
    initialTorsoAngle = -45f,
    armPose = "TUCKED",
    legPose = "COMPACT"
  ),
  HEADFIRST(
    id = "headfirst",
    displayName = "Zambullida",
    subtitle = "Impacto craneal directo",
    initialTorsoAngle = 45f,
    armPose = "FORWARD",
    legPose = "STRAIGHT"
  )
}
