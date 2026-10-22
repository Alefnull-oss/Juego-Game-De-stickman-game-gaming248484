package com.example

import com.example.model.BoneType
import com.example.model.DismountPose
import com.example.model.Stage
import com.example.model.Vehicle
import com.example.physics.RagdollBody
import com.example.physics.RagdollSimulation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testRagdollInitialization() {
    val ragdoll = RagdollBody(100f, 100f, DismountPose.STANDING)
    assertEquals(16, ragdoll.particles.size)
    assertTrue(ragdoll.constraints.isNotEmpty())
    assertEquals(13, ragdoll.boneStatusMap.size)
    assertFalse(ragdoll.boneStatusMap.values.any { it.isBroken })
  }

  @Test
  fun testBoneBreakRegistration() {
    val ragdoll = RagdollBody(100f, 100f, DismountPose.STANDING)
    ragdoll.breakBone(BoneType.CRANIUM, 35f)

    val skullStatus = ragdoll.boneStatusMap[BoneType.CRANIUM]
    assertNotNull(skullStatus)
    assertTrue(skullStatus!!.isBroken)
    assertEquals(1, skullStatus.breakCount)
    assertEquals(35f, skullStatus.breakSpeed)
  }

  @Test
  fun testSimulationLaunch() {
    val sim = RagdollSimulation(Stage.STAIRS, Vehicle.SHOPPING_CART, DismountPose.STANDING)
    assertFalse(sim.isSimulating)

    sim.launch(0.8f, 25f)
    assertTrue(sim.isSimulating)

    // Advance simulation a few steps
    sim.update(0.016f)
    sim.update(0.016f)
    assertTrue(sim.simulationTimeSeconds > 0f)
  }

  @Test
  fun testCameraFollowsStickman() {
    val sim = RagdollSimulation(Stage.STAIRS, Vehicle.ON_FOOT, DismountPose.STANDING)
    val initialCamX = sim.cameraX

    sim.launch(1.0f, 20f)
    for (i in 0 until 40) {
      sim.update(0.016f)
    }

    val (camX, camY) = sim.getCameraTarget()
    // Verify camera actively advances with the stickman
    assertTrue(camX > initialCamX + 20f)
    // Verify camera coordinates match simulation camera target
    assertEquals(sim.cameraX, camX, 0.001f)
    assertEquals(sim.cameraY, camY, 0.001f)
  }
}
