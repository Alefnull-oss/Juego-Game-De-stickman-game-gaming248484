package com.example.physics

import com.example.model.Vehicle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class VehicleDebris(
  val particle: Particle,
  var rotationAngle: Float = 0f,
  var angularVelocity: Float = 0f,
  val size: Float = 10f,
  val colorHex: Long = 0xFF4A4D52L,
  val isWheel: Boolean = false,
  val wheelRadius: Float = 8f
)

class VehicleBody(
  val vehicle: Vehicle,
  startX: Float,
  startY: Float
) {
  val particles = mutableListOf<Particle>()
  val constraints = mutableListOf<DistanceConstraint>()
  val riderConstraints = mutableListOf<DistanceConstraint>()

  // Key Chassis Particles
  lateinit var chassisFront: Particle
  lateinit var chassisRear: Particle
  lateinit var chassisTop: Particle
  lateinit var chassisSeat: Particle
  lateinit var chassisFloor: Particle
  lateinit var steeringCol: Particle

  // Wheels
  lateinit var wheelFront: Particle
  lateinit var wheelRear: Particle

  // Destruction state
  var isFrontWheelDetached: Boolean = false
  var isRearWheelDetached: Boolean = false
  var isRoofCrushed: Boolean = false
  var isFrontCrushed: Boolean = false
  var isRearCrushed: Boolean = false

  // Dynamic wheel spin angles
  var frontWheelAngle: Float = 0f
  var rearWheelAngle: Float = 0f

  // Detached flying debris and wheels
  val detachedDebris = mutableListOf<VehicleDebris>()

  // Rider tracking
  var isRiderEjected: Boolean = false
  var chassisDamagePoints: Int = 0

  // Track breakable constraints
  private var frontSuspension1: DistanceConstraint? = null
  private var frontSuspension2: DistanceConstraint? = null
  private var rearSuspension1: DistanceConstraint? = null
  private var rearSuspension2: DistanceConstraint? = null
  private var roofConstraint1: DistanceConstraint? = null
  private var roofConstraint2: DistanceConstraint? = null
  private var frontBumperConstraint: DistanceConstraint? = null
  private var rearBumperConstraint: DistanceConstraint? = null

  init {
    assemble(startX, startY)
  }

  private fun assemble(originX: Float, originY: Float) {
    particles.clear()
    constraints.clear()
    riderConstraints.clear()
    detachedDebris.clear()
    isFrontWheelDetached = false
    isRearWheelDetached = false
    isRoofCrushed = false
    isFrontCrushed = false
    isRearCrushed = false
    frontWheelAngle = 0f
    rearWheelAngle = 0f
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

    // 1. Chassis particles
    chassisFront = Particle(originX + halfW, originY - halfH * 0.7f, mass = vehicle.mass * 0.18f, radius = 6f)
    chassisRear = Particle(originX - halfW, originY - halfH * 0.7f, mass = vehicle.mass * 0.18f, radius = 6f)
    chassisFloor = Particle(originX, originY + 2f, mass = vehicle.mass * 0.22f, radius = 7f)
    chassisTop = Particle(originX - 4f, originY - vehicle.chassisHeight * 1.05f, mass = vehicle.mass * 0.12f, radius = 5f)
    chassisSeat = Particle(
      originX + vehicle.riderSeatOffsetX,
      originY + vehicle.riderSeatOffsetY,
      mass = vehicle.mass * 0.15f,
      radius = 5.5f
    )
    steeringCol = Particle(
      originX + vehicle.riderSeatOffsetX + 12f,
      originY + vehicle.riderSeatOffsetY - 6f,
      mass = vehicle.mass * 0.08f,
      radius = 4f
    )

    // 2. Wheels
    wheelFront = Particle(originX + wheelDistHalf, originY + wheelR, mass = vehicle.mass * 0.16f, radius = wheelR)
    wheelRear = Particle(originX - wheelDistHalf, originY + wheelR, mass = vehicle.mass * 0.16f, radius = wheelR)

    particles.addAll(
      listOf(
        chassisFront,
        chassisRear,
        chassisFloor,
        chassisTop,
        chassisSeat,
        steeringCol,
        wheelFront,
        wheelRear
      )
    )

    // 3. Core rigid & semi-rigid chassis truss
    addChassisConstraint(chassisFloor, chassisFront, stiffness = 0.96f)
    addChassisConstraint(chassisFloor, chassisRear, stiffness = 0.96f)
    addChassisConstraint(chassisFront, chassisRear, stiffness = 0.94f)
    addChassisConstraint(chassisSeat, chassisFloor, stiffness = 0.96f)
    addChassisConstraint(chassisSeat, chassisRear, stiffness = 0.94f)
    addChassisConstraint(steeringCol, chassisFloor, stiffness = 0.92f)
    addChassisConstraint(steeringCol, chassisSeat, stiffness = 0.92f)

    // 4. Breakable front & rear crumple zones
    val bumperThreshold = vehicle.wheelBreakThreshold * 1.35f
    frontBumperConstraint = addBreakableConstraint(chassisFront, steeringCol, stiffness = 0.92f, threshold = bumperThreshold)
    rearBumperConstraint = addBreakableConstraint(chassisRear, chassisTop, stiffness = 0.92f, threshold = bumperThreshold)

    // 5. Breakable roof / roll cage (collapses on violent rollover)
    val roofThreshold = vehicle.roofBreakThreshold
    roofConstraint1 = addBreakableConstraint(chassisTop, chassisFront, stiffness = 0.90f, threshold = roofThreshold)
    roofConstraint2 = addBreakableConstraint(chassisTop, chassisSeat, stiffness = 0.90f, threshold = roofThreshold)

    // 6. Breakable suspension struts for wheels
    val suspThreshold = vehicle.wheelBreakThreshold
    frontSuspension1 = addBreakableConstraint(wheelFront, chassisFront, stiffness = 0.82f, threshold = suspThreshold)
    frontSuspension2 = addBreakableConstraint(wheelFront, chassisFloor, stiffness = 0.78f, threshold = suspThreshold)

    rearSuspension1 = addBreakableConstraint(wheelRear, chassisRear, stiffness = 0.82f, threshold = suspThreshold)
    rearSuspension2 = addBreakableConstraint(wheelRear, chassisFloor, stiffness = 0.78f, threshold = suspThreshold)

    // Axle stabilizer between wheels (breaks if either wheel detaches)
    addBreakableConstraint(wheelFront, wheelRear, stiffness = 0.75f, threshold = suspThreshold * 1.2f)
  }

  fun attachRider(ragdoll: RagdollBody) {
    if (vehicle.wheelCount == 0) return

    riderConstraints.clear()
    isRiderEjected = false

    // 1. Seat belt / pelvis anchor
    val seatToPelvis = DistanceConstraint(
      p1 = chassisSeat,
      p2 = ragdoll.pelvis,
      restLength = 6f,
      stiffness = 0.92f,
      breakable = true,
      breakThreshold = vehicle.ejectForceThreshold
    )
    riderConstraints.add(seatToPelvis)

    // 2. Torso back support
    val seatToChest = DistanceConstraint(
      p1 = chassisSeat,
      p2 = ragdoll.chest,
      restLength = 14f,
      stiffness = 0.80f,
      breakable = true,
      breakThreshold = vehicle.ejectForceThreshold * 1.1f
    )
    riderConstraints.add(seatToChest)

    // 3. Hands grasping steering wheel / handlebars
    val wheelToRHand = DistanceConstraint(
      p1 = steeringCol,
      p2 = ragdoll.rHand,
      restLength = 5f,
      stiffness = 0.85f,
      breakable = true,
      breakThreshold = vehicle.ejectForceThreshold * 0.75f
    )
    val wheelToLHand = DistanceConstraint(
      p1 = steeringCol,
      p2 = ragdoll.lHand,
      restLength = 5f,
      stiffness = 0.85f,
      breakable = true,
      breakThreshold = vehicle.ejectForceThreshold * 0.75f
    )
    riderConstraints.add(wheelToRHand)
    riderConstraints.add(wheelToLHand)

    // 4. Feet placed at floorboard / pegs
    val floorToRFoot = DistanceConstraint(
      p1 = chassisFloor,
      p2 = ragdoll.rFoot,
      restLength = 12f,
      stiffness = 0.65f,
      breakable = true,
      breakThreshold = vehicle.ejectForceThreshold * 0.9f
    )
    val floorToLFoot = DistanceConstraint(
      p1 = chassisFloor,
      p2 = ragdoll.lFoot,
      restLength = 12f,
      stiffness = 0.65f,
      breakable = true,
      breakThreshold = vehicle.ejectForceThreshold * 0.9f
    )
    riderConstraints.add(floorToRFoot)
    riderConstraints.add(floorToLFoot)
  }

  fun solveConstraints(
    onPartBroken: ((partName: String, x: Float, y: Float) -> Unit)? = null
  ) {
    for (c in constraints) {
      if (c.isBroken) continue
      c.solve()

      // Plastic deformation / crumple zone effect:
      // If a non-broken chassis strut takes high compressive force, slightly permanently shorten it
      if (c.stiffness > 0.88f && c.lastDelta < -6f) {
        c.restLength = max(c.restLength * 0.85f, c.restLength + c.lastDelta * 0.05f)
      }
    }

    // Check front wheel detachment
    if (!isFrontWheelDetached && (frontSuspension1?.isBroken == true || frontSuspension2?.isBroken == true)) {
      isFrontWheelDetached = true
      chassisDamagePoints += 2500
      detachedDebris.add(
        VehicleDebris(
          particle = wheelFront,
          rotationAngle = frontWheelAngle,
          angularVelocity = wheelFront.vx * 0.15f,
          size = vehicle.wheelRadius * 2f,
          colorHex = vehicle.secondaryColorHex,
          isWheel = true,
          wheelRadius = vehicle.wheelRadius
        )
      )
      onPartBroken?.invoke("front_wheel", wheelFront.x, wheelFront.y)
    }

    // Check rear wheel detachment
    if (!isRearWheelDetached && (rearSuspension1?.isBroken == true || rearSuspension2?.isBroken == true)) {
      isRearWheelDetached = true
      chassisDamagePoints += 2500
      detachedDebris.add(
        VehicleDebris(
          particle = wheelRear,
          rotationAngle = rearWheelAngle,
          angularVelocity = wheelRear.vx * 0.15f,
          size = vehicle.wheelRadius * 2f,
          colorHex = vehicle.secondaryColorHex,
          isWheel = true,
          wheelRadius = vehicle.wheelRadius
        )
      )
      onPartBroken?.invoke("rear_wheel", wheelRear.x, wheelRear.y)
    }

    // Check roof / roll cage collapse
    if (!isRoofCrushed && (roofConstraint1?.isBroken == true || roofConstraint2?.isBroken == true)) {
      isRoofCrushed = true
      chassisDamagePoints += 3200
      // Spawn flying scrap piece
      val scrapParticle = Particle(
        chassisTop.x,
        chassisTop.y,
        oldX = chassisTop.x - chassisTop.vx * 1.2f,
        oldY = chassisTop.y - chassisTop.vy * 1.2f - 4f,
        mass = vehicle.mass * 0.08f,
        radius = 5f
      )
      particles.add(scrapParticle)
      detachedDebris.add(
        VehicleDebris(
          particle = scrapParticle,
          rotationAngle = 0f,
          angularVelocity = 0.25f,
          size = 14f,
          colorHex = vehicle.primaryColorHex,
          isWheel = false
        )
      )
      onPartBroken?.invoke("roof_crushed", chassisTop.x, chassisTop.y)
    }

    // Check front bumper snap
    if (!isFrontCrushed && frontBumperConstraint?.isBroken == true) {
      isFrontCrushed = true
      chassisDamagePoints += 1800
      onPartBroken?.invoke("bumper_snapped", chassisFront.x, chassisFront.y)
    }
  }

  fun updateWheelRotation() {
    if (!isFrontWheelDetached) {
      frontWheelAngle += wheelFront.vx / vehicle.wheelRadius.coerceAtLeast(4f)
    }
    if (!isRearWheelDetached) {
      rearWheelAngle += wheelRear.vx / vehicle.wheelRadius.coerceAtLeast(4f)
    }

    // Update detached debris rotation
    detachedDebris.forEach { deb ->
      deb.rotationAngle += deb.angularVelocity
      deb.angularVelocity *= 0.99f
    }
  }

  fun checkEjection(onEjected: () -> Unit) {
    if (isRiderEjected || vehicle.wheelCount == 0) return

    val anyBroken = riderConstraints.any { it.isBroken }
    if (anyBroken) {
      isRiderEjected = true
      riderConstraints.forEach { it.isBroken = true }
      chassisDamagePoints += 2000
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

  private fun addBreakableConstraint(
    p1: Particle,
    p2: Particle,
    stiffness: Float,
    threshold: Float
  ): DistanceConstraint {
    val constraint = DistanceConstraint(
      p1 = p1,
      p2 = p2,
      restLength = hypot(p2.x - p1.x, p2.y - p1.y),
      stiffness = stiffness,
      breakable = true,
      breakThreshold = threshold
    )
    constraints.add(constraint)
    return constraint
  }

  val centerOfMassX: Float
    get() = if (particles.isEmpty()) 0f else (chassisFloor.x * 2 + chassisFront.x + chassisRear.x) / 4f

  val centerOfMassY: Float
    get() = if (particles.isEmpty()) 0f else (chassisFloor.y * 2 + chassisFront.y + chassisRear.y) / 4f

  val speed: Float
    get() = if (particles.isEmpty()) 0f else particles.map { it.speed }.average().toFloat()

  fun applyImpulse(ix: Float, iy: Float) {
    particles.forEach { it.applyImpulse(ix * vehicle.speedMultiplier, iy * vehicle.speedMultiplier) }
  }
}
