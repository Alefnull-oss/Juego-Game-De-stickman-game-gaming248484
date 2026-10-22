package com.example.physics

import com.example.model.BoneStatus
import com.example.model.BoneType
import com.example.model.DismountPose
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sin

class RagdollBody(startX: Float, startY: Float, pose: DismountPose = DismountPose.STANDING) {

  val particles = mutableListOf<Particle>()
  val constraints = mutableListOf<DistanceConstraint>()
  val boneStatusMap = BoneType.entries.associateWith { BoneStatus(it) }.toMutableMap()

  // Key joints
  lateinit var head: Particle
  lateinit var neck: Particle
  lateinit var chest: Particle
  lateinit var pelvis: Particle

  lateinit var lShoulder: Particle
  lateinit var lElbow: Particle
  lateinit var lHand: Particle

  lateinit var rShoulder: Particle
  lateinit var rElbow: Particle
  lateinit var rHand: Particle

  lateinit var lHip: Particle
  lateinit var lKnee: Particle
  lateinit var lFoot: Particle

  lateinit var rHip: Particle
  lateinit var rKnee: Particle
  lateinit var rFoot: Particle

  private var previousTorsoAngle: Float = 0f
  var totalRotationDegrees: Float = 0f
  var somersaultCount: Int = 0

  init {
    assemble(startX, startY, pose)
  }

  fun assemble(originX: Float, originY: Float, pose: DismountPose) {
    particles.clear()
    constraints.clear()
    boneStatusMap.values.forEach { it.reset() }
    totalRotationDegrees = 0f
    somersaultCount = 0

    // Coordinates according to pose
    val (torsoDx, torsoDy) = when (pose) {
      DismountPose.STANDING -> Pair(0f, 26f)
      DismountPose.SUPERMAN -> Pair(26f, 0f)
      DismountPose.SITTING -> Pair(4f, 24f)
      DismountPose.CROUCH -> Pair(12f, 18f)
      DismountPose.HEADFIRST -> Pair(18f, 18f)
    }

    head = Particle(originX, originY - 32f, mass = 2.5f, radius = 9.5f)
    neck = Particle(originX, originY - 20f, mass = 1.0f, radius = 4f)
    chest = Particle(originX, originY - 8f, mass = 4.0f, radius = 6.5f)
    pelvis = Particle(originX - torsoDx * 0.2f, originY + torsoDy, mass = 4.5f, radius = 7f)

    // Arms
    val (armXOffset, armYOffset) = when (pose) {
      DismountPose.SUPERMAN, DismountPose.HEADFIRST -> Pair(18f, -4f)
      DismountPose.SITTING -> Pair(12f, 6f)
      DismountPose.CROUCH -> Pair(6f, 12f)
      DismountPose.STANDING -> Pair(0f, 18f)
    }

    lShoulder = Particle(originX - 4f, originY - 10f, mass = 0.8f, radius = 3.5f)
    lElbow = Particle(originX - 4f + armXOffset * 0.5f, originY - 10f + armYOffset * 0.5f, mass = 0.8f, radius = 3.5f)
    lHand = Particle(originX - 4f + armXOffset, originY - 10f + armYOffset, mass = 0.6f, radius = 3f)

    rShoulder = Particle(originX + 4f, originY - 10f, mass = 0.8f, radius = 3.5f)
    rElbow = Particle(originX + 4f + armXOffset * 0.5f, originY - 10f + armYOffset * 0.5f, mass = 0.8f, radius = 3.5f)
    rHand = Particle(originX + 4f + armXOffset, originY - 10f + armYOffset, mass = 0.6f, radius = 3f)

    // Legs
    val (legXOffset, legYOffset) = when (pose) {
      DismountPose.SITTING -> Pair(22f, 8f)
      DismountPose.CROUCH -> Pair(8f, 14f)
      else -> Pair(0f, 28f)
    }

    lHip = Particle(originX - 5f, pelvis.y, mass = 1.0f, radius = 4f)
    lKnee = Particle(originX - 5f + legXOffset * 0.5f, pelvis.y + legYOffset * 0.5f, mass = 1.2f, radius = 4f)
    lFoot = Particle(originX - 5f + legXOffset, pelvis.y + legYOffset, mass = 0.9f, radius = 3.8f)

    rHip = Particle(originX + 5f, pelvis.y, mass = 1.0f, radius = 4f)
    rKnee = Particle(originX + 5f + legXOffset * 0.5f, pelvis.y + legYOffset * 0.5f, mass = 1.2f, radius = 4f)
    rFoot = Particle(originX + 5f + legXOffset, pelvis.y + legYOffset, mass = 0.9f, radius = 3.8f)

    particles.addAll(
      listOf(
        head, neck, chest, pelvis,
        lShoulder, lElbow, lHand,
        rShoulder, rElbow, rHand,
        lHip, lKnee, lFoot,
        rHip, rKnee, rFoot
      )
    )

    // Skeletal anatomical constraints with breakage thresholds
    addConstraint(head, neck, BoneType.CRANIUM, breakable = true, breakThreshold = BoneType.CRANIUM.breakThreshold)
    addConstraint(neck, chest, BoneType.CERVICAL_SPINE, breakable = true, breakThreshold = BoneType.CERVICAL_SPINE.breakThreshold)
    addConstraint(chest, pelvis, BoneType.LUMBAR_SPINE, breakable = true, breakThreshold = BoneType.LUMBAR_SPINE.breakThreshold)

    // Torso structure & ribs
    addConstraint(neck, pelvis, BoneType.THORAX_RIBS, breakable = true, breakThreshold = BoneType.THORAX_RIBS.breakThreshold, stiffness = 0.85f)
    addConstraint(lHip, rHip, BoneType.PELVIS, breakable = true, breakThreshold = BoneType.PELVIS.breakThreshold)

    // Left Arm
    addConstraint(chest, lShoulder, null, breakable = false)
    addConstraint(lShoulder, lElbow, BoneType.LEFT_HUMERUS, breakable = true, breakThreshold = BoneType.LEFT_HUMERUS.breakThreshold)
    addConstraint(lElbow, lHand, BoneType.LEFT_FOREARM, breakable = true, breakThreshold = BoneType.LEFT_FOREARM.breakThreshold)

    // Right Arm
    addConstraint(chest, rShoulder, null, breakable = false)
    addConstraint(rShoulder, rElbow, BoneType.RIGHT_HUMERUS, breakable = true, breakThreshold = BoneType.RIGHT_HUMERUS.breakThreshold)
    addConstraint(rElbow, rHand, BoneType.RIGHT_FOREARM, breakable = true, breakThreshold = BoneType.RIGHT_FOREARM.breakThreshold)

    // Left Leg
    addConstraint(pelvis, lHip, null, breakable = false)
    addConstraint(lHip, lKnee, BoneType.LEFT_FEMUR, breakable = true, breakThreshold = BoneType.LEFT_FEMUR.breakThreshold)
    addConstraint(lKnee, lFoot, BoneType.LEFT_TIBIA, breakable = true, breakThreshold = BoneType.LEFT_TIBIA.breakThreshold)

    // Right Leg
    addConstraint(pelvis, rHip, null, breakable = false)
    addConstraint(rHip, rKnee, BoneType.RIGHT_FEMUR, breakable = true, breakThreshold = BoneType.RIGHT_FEMUR.breakThreshold)
    addConstraint(rKnee, rFoot, BoneType.RIGHT_TIBIA, breakable = true, breakThreshold = BoneType.RIGHT_TIBIA.breakThreshold)

    // Joint stabilizing cross-constraints (soft springs to keep limbs from inverting unrealistically when unbroken)
    addSoftSpring(head, chest, 0.45f)
    addSoftSpring(chest, lHand, 0.25f)
    addSoftSpring(chest, rHand, 0.25f)
    addSoftSpring(pelvis, lFoot, 0.3f)
    addSoftSpring(pelvis, rFoot, 0.3f)

    previousTorsoAngle = atan2(pelvis.y - head.y, pelvis.x - head.x)
  }

  private fun addConstraint(
    p1: Particle,
    p2: Particle,
    boneType: BoneType?,
    breakable: Boolean,
    breakThreshold: Float = 25f,
    stiffness: Float = 0.95f
  ) {
    constraints.add(
      DistanceConstraint(
        p1 = p1,
        p2 = p2,
        restLength = hypot(p2.x - p1.x, p2.y - p1.y),
        stiffness = stiffness,
        boneType = boneType,
        breakable = breakable,
        breakThreshold = breakThreshold
      )
    )
  }

  private fun addSoftSpring(p1: Particle, p2: Particle, stiffness: Float) {
    constraints.add(
      DistanceConstraint(
        p1 = p1,
        p2 = p2,
        restLength = hypot(p2.x - p1.x, p2.y - p1.y),
        stiffness = stiffness,
        boneType = null,
        breakable = false
      )
    )
  }

  fun updateAcrobatics() {
    val currentTorsoAngle = atan2(pelvis.y - head.y, pelvis.x - head.x)
    var diff = currentTorsoAngle - previousTorsoAngle
    while (diff < -Math.PI) diff += (2 * Math.PI).toFloat()
    while (diff > Math.PI) diff -= (2 * Math.PI).toFloat()

    val deg = Math.toDegrees(diff.toDouble()).toFloat()
    totalRotationDegrees += deg
    somersaultCount = (kotlin.math.abs(totalRotationDegrees) / 360f).toInt()
    previousTorsoAngle = currentTorsoAngle
  }

  val centerOfMassX: Float
    get() = (chest.x * 2 + pelvis.x * 2 + head.x) / 5f

  val centerOfMassY: Float
    get() = (chest.y * 2 + pelvis.y * 2 + head.y) / 5f

  val averageSpeed: Float
    get() = (chest.speed + pelvis.speed + head.speed) / 3f

  fun applyImpulse(ix: Float, iy: Float) {
    particles.forEach { it.applyImpulse(ix, iy) }
  }

  fun breakBone(boneType: BoneType, speed: Float) {
    val status = boneStatusMap[boneType] ?: return
    if (!status.isBroken) {
      status.isBroken = true
      status.breakSpeed = speed
      status.breakCount = 1
    } else {
      status.breakCount++
    }
  }
}
