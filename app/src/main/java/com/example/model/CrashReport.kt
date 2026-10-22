package com.example.model

data class CrashReport(
  val totalScore: Int,
  val cashEarned: Int,
  val brokenBones: List<BoneStatus>,
  val somersaultsCount: Int,
  val airTimeSeconds: Float,
  val maxSpeedKmh: Float,
  val vehicleDamageScore: Int,
  val stageName: String,
  val vehicleName: String,
  val isNewHighScore: Boolean = false
) {
  val totalFractures: Int get() = brokenBones.count { it.isBroken }
}
