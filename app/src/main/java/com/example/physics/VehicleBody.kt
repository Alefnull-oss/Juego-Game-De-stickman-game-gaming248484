package com.example.physics

import com.example.model.Vehicle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class VehicleBody(
  val vehicle: Vehicle,
  startX: Float,
  startY: Float
) {
  val particles = mutableListOf<Particle>()
  val constraints = mutableListOf<DistanceConstraint>()
  val riderConstraints = mutableListOf<DistanceConstraint>()

  lateinit var chassisFront: Particle
  lateinit var chassisRear: Particle
  lateinit var chassisTop: Particle
  lateinit var chassisSeat: Particle

  lateinit var wheelFront: Particle
  lateinit var wheelRear: Particle

  var isRiderEjected: Boolean = false
  var chassisDamagePoints: Int = 0

  init {
    assemble(startX, startY)
  }

  private fun assemble(originX: Float, originY: Float) {
    particles.clear()
    constraints.clear()
    riderConstraints.clear()
    isRiderEjected = false
    chassisDamagePoints = 0

    if (vehicle.wheelCount == 0) {
      // On Foot - no vehicle chassis
      return
    }

    val halfW = vehicle.chassisWidth * 0.5f
    val halfH = vehicle.chassisHeight * 0.5f
    val wheelR = vehicle.wheelRadius
    val wheelDistHalf = vehicle.wheelDistance * 0.5f

    chassisFront = Particle(originX + halfW, originY - halfH, mass = vehicle.mass * 0.3f, radius = 5f)
    chassisRear = Particle(originX - halfW, originY - halfH, mass = vehicle.mass * 0.3f, radius = 5f)
    chassisTop = Particle(originX, originY - vehicle.chassisHeight * 1.1f, mass = vehicle.mass * 0.2f, radius = 4f)
    chassisSeat = Particle(
      originX + vehicle.riderSeatOffsetX,
      originY + vehicle.riderSeatOffsetY,
      mass = vehicle.mass * 0.2f,
      radius = 4f
    )

    wheelFront = Particle(originX + wheelDistHalf, originY + wheelR, mass = vehicle.mass * 0.25f, radius = wheelR)
    wheelRear = Particle(originX - wheelDistHalf, originY + wheelR, mass = vehicle.mass * 0.25f, radius = wheelR)

    particles.addAll(listOf(chassisFront, chassisRear, chassisTop, chassisSeat, wheelFront, wheelRear))

    // Rigid Chassis Box
    addChassisConstraint(chassisFront, chassisRear, 0.98f)
    addChassisConstraint(chassisFront, chassisTop, 0.95f)
    addChassisConstraint(chassisRear, chassisTop, 0.95f)
    addChassisConstraint(chassisSeat, chassisFront, 0.95f)
    addChassisConstraint(chassisSeat, chassisRear, 0.95f)
    addChassisConstraint(chassisSeat, chassisTop, 0.95f)

    // Suspension Springs between wheels and chassis
    addSuspension(wheelFront, chassisFront, 0.75f)
    addSuspension(wheelFront, chassisSeat, 0.65f)
    addSuspension(wheelRear, chassisRear, 0.75f)
    addSuspension(wheelRear, chassisSeat, 0.65f)

    // Wheelbase strut
    addChassisConstraint(wheelFront, wheelRear, 0.85f)
  }

  fun attachRider(ragdoll: RagdollBody) {
    if (vehicle.wheelCount == 0) return

    riderConstraints.clear()
    isRiderEjected = false

    // Seat constraint to pelvis
    val seatToPelvis = DistanceConstraint(
      p1 = chassisSeat,
      p2 = ragdoll.pelvis,
      restLength = 8f,
      stiffness = 0.9f,
      breakable = true,
      breakThreshold = vehicle.ejectForceThreshold
    )
    riderConstraints.add(seatToPelvis)

    // Handlebar constraint to hands
    val handleToHand = DistanceConstraint(
      p1 = chassisFront,
      p2 = ragdoll.rHand,
      restLength = 12f,
      stiffness = 0.8f,
      breakable = true,
      breakThreshold = vehicle.ejectForceThreshold * 0.8f
    )
    riderConstraints.add(handleToHand)
  }

  fun checkEjection(onEjected: () -> Unit) {
    if (isRiderEjected || vehicle.wheelCount == 0) return

    val anyBroken = riderConstraints.any { it.isBroken }
    if (anyBroken) {
      isRiderEjected = true
      riderConstraints.forEach { it.isBroken = true }
      chassisDamagePoints += 1500
      onEjected()
    }
  }

  private fun addChassisConstraint(p1: Particle, p2: Particle, stiffness: Float) {
    constraints.add(
      DistanceConstraint(
        p1 = p1,
        p2 = p2,
        restLength = hypot(p2.x - p1.x, p2.y - p1.y),
        stiffness = stiffness,
        breakable = false
      )
    )
  }

  private fun addSuspension(wheel: Particle, chassisPart: Particle, stiffness: Float) {
    constraints.add(
      DistanceConstraint(
        p1 = wheel,
        p2 = chassisPart,
        restLength = hypot(chassisPart.x - wheel.x, chassisPart.y - wheel.y),
        stiffness = stiffness,
        breakable = false
      )
    )
  }

  val centerOfMassX: Float
    get() = if (particles.isEmpty()) 0f else particles.map { it.x }.average().toFloat()

  val centerOfMassY: Float
    get() = if (particles.isEmpty()) 0f else particles.map { it.y }.average().toFloat()

  val speed: Float
    get() = if (particles.isEmpty()) 0f else particles.map { it.speed }.average().toFloat()

  fun applyImpulse(ix: Float, iy: Float) {
    particles.forEach { it.applyImpulse(ix * vehicle.speedMultiplier, iy * vehicle.speedMultiplier) }
  }
}
