package com.example.viewmodel

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.model.BoneStatus
import com.example.model.BoneType
import com.example.model.CrashReport
import com.example.model.DismountPose
import com.example.model.ObstacleType
import com.example.model.PlacedObstacle
import com.example.model.Stage
import com.example.model.Vehicle
import com.example.physics.RagdollSimulation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

enum class GamePhase {
  PREPARING,
  CHARGING_POWER,
  SIMULATING,
  FINISHED
}

data class GameUiState(
  val phase: GamePhase = GamePhase.PREPARING,
  val power: Float = 0.5f,
  val launchAngleDegrees: Float = 20f,
  val stage: Stage = Stage.STAIRS,
  val vehicle: Vehicle = Vehicle.SHOPPING_CART,
  val pose: DismountPose = DismountPose.STANDING,
  val score: Int = 0,
  val bestScore: Int = 0,
  val brokenBonesCount: Int = 0,
  val currentSpeedKmh: Float = 0f,
  val maxSpeedKmh: Float = 0f,
  val airTimeSeconds: Float = 0f,
  val somersaultsCount: Int = 0,
  val recentBrokenBone: Pair<BoneType, Long>? = null,
  val isShopOpen: Boolean = false,
  val isXRayModalOpen: Boolean = false,
  val isObstaclePickerOpen: Boolean = false,
  val isPaused: Boolean = false,
  val isSlowMoActive: Boolean = false,
  val timeDilation: Float = 1.0f,
  val pushJointIndex: Int = 0, // 0: Espalda/Torso, 1: Cabeza, 2: Pelvis, 3: Piernas
  val renderTick: Long = 0L,
  val crashReport: CrashReport? = null,
  val availableStages: List<Stage> = Stage.DEFAULT_LIST,
  val availableVehicles: List<Vehicle> = Vehicle.DEFAULT_LIST,
  val placedObstacles: List<PlacedObstacle> = emptyList()
)

class GameViewModel(
  private val context: Context,
  val repository: GameRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(GameUiState())
  val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

  val simulation: RagdollSimulation = RagdollSimulation(
    stage = Stage.STAIRS,
    vehicle = Vehicle.SHOPPING_CART,
    pose = DismountPose.STANDING
  )

  private var loopJob: Job? = null
  private var powerChargingJob: Job? = null
  private var slowMoJob: Job? = null

  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  init {
    setupSimulationCallbacks()
    syncWithRepository()
    startSimulationLoop()
  }

  private fun setupSimulationCallbacks() {
    simulation.onBoneBroken = { boneType, speed ->
      triggerHaptic(60, 200)
      _uiState.update { state ->
        val bonePoints = boneType.pointValue + (speed * 40f).toInt()
        val newScore = state.score + bonePoints
        val brokenCount = simulation.ragdoll.boneStatusMap.values.count { it.isBroken }
        state.copy(
          score = newScore,
          brokenBonesCount = brokenCount,
          recentBrokenBone = Pair(boneType, System.currentTimeMillis())
        )
      }
    }

    simulation.onSevereImpact = { impactForce, boneType ->
      triggerAutoSlowMotion()
    }

    simulation.onObstacleTriggered = { obsType ->
      triggerHaptic(120, 255)
      triggerAutoSlowMotion()
      _uiState.update { state ->
        state.copy(score = state.score + obsType.bonusPoints)
      }
    }

    simulation.onRiderEjected = {
      triggerHaptic(90, 220)
      triggerAutoSlowMotion()
      _uiState.update { state ->
        state.copy(score = state.score + 1500)
      }
    }
  }

  fun triggerAutoSlowMotion() {
    if (_uiState.value.phase != GamePhase.SIMULATING) return
    slowMoJob?.cancel()
    slowMoJob = viewModelScope.launch {
      simulation.timeDilation = 0.25f
      _uiState.update { it.copy(isSlowMoActive = true, timeDilation = 0.25f) }
      delay(1500) // Hold dramatic slow motion for 1.5 seconds

      // Smooth restoration to 1.0x
      var current = 0.25f
      while (current < 1.0f) {
        current += 0.15f
        val clamped = current.coerceAtMost(1.0f)
        simulation.timeDilation = clamped
        _uiState.update { it.copy(timeDilation = clamped) }
        delay(30)
      }
      simulation.timeDilation = 1.0f
      _uiState.update { it.copy(isSlowMoActive = false, timeDilation = 1.0f) }
    }
  }

  private fun syncWithRepository() {
    viewModelScope.launch {
      repository.selectedVehicleId.collect { id ->
        val vehicle = Vehicle.DEFAULT_LIST.find { it.id == id } ?: Vehicle.SHOPPING_CART
        _uiState.update { it.copy(vehicle = vehicle) }
        simulation.setConfiguration(_uiState.value.stage, vehicle, _uiState.value.pose)
      }
    }

    viewModelScope.launch {
      repository.selectedStageId.collect { id ->
        val stage = Stage.DEFAULT_LIST.find { it.id == id } ?: Stage.STAIRS
        val best = repository.getHighScore(stage.id)
        _uiState.update { it.copy(stage = stage, bestScore = best) }
        simulation.setConfiguration(stage, _uiState.value.vehicle, _uiState.value.pose)
      }
    }

    viewModelScope.launch {
      repository.selectedPose.collect { pose ->
        _uiState.update { it.copy(pose = pose) }
        simulation.setConfiguration(_uiState.value.stage, _uiState.value.vehicle, pose)
      }
    }
  }

  private fun startSimulationLoop() {
    loopJob?.cancel()
    loopJob = viewModelScope.launch {
      var lastTime = System.nanoTime()
      while (isActive) {
        val now = System.nanoTime()
        val dt = (now - lastTime) / 1_000_000_000f
        lastTime = now

        if (_uiState.value.phase == GamePhase.SIMULATING && !_uiState.value.isPaused) {
          simulation.update(dt.coerceIn(0.005f, 0.033f))

          val curSpeed = (simulation.vehicleBody?.speed ?: simulation.ragdoll.averageSpeed) * 14.4f
          _uiState.update { state ->
            state.copy(
              currentSpeedKmh = curSpeed,
              maxSpeedKmh = simulation.maxSpeed,
              airTimeSeconds = simulation.airTimeSeconds,
              somersaultsCount = simulation.ragdoll.somersaultCount,
              renderTick = state.renderTick + 1
            )
          }

          if (simulation.isFinished) {
            onSimulationFinished()
          }
        } else {
          _uiState.update { it.copy(renderTick = it.renderTick + 1) }
        }

        delay(16) // ~60 FPS
      }
    }
  }

  fun togglePowerCharging() {
    if (_uiState.value.phase == GamePhase.PREPARING) {
      _uiState.update { it.copy(phase = GamePhase.CHARGING_POWER) }
      powerChargingJob?.cancel()
      powerChargingJob = viewModelScope.launch {
        var t = 0f
        while (isActive && _uiState.value.phase == GamePhase.CHARGING_POWER) {
          t += 0.065f
          val power = (sin(t) + 1f) * 0.5f
          _uiState.update { it.copy(power = power.coerceIn(0.08f, 1.0f)) }
          delay(20)
        }
      }
    } else if (_uiState.value.phase == GamePhase.CHARGING_POWER) {
      powerChargingJob?.cancel()
      launchDismount()
    }
  }

  fun nextPose() {
    val poses = DismountPose.entries
    val currentIndex = poses.indexOf(_uiState.value.pose)
    val nextPose = poses[(currentIndex + 1) % poses.size]
    setPose(nextPose)
  }

  fun prevPose() {
    val poses = DismountPose.entries
    val currentIndex = poses.indexOf(_uiState.value.pose)
    val prevIndex = if (currentIndex <= 0) poses.size - 1 else currentIndex - 1
    setPose(poses[prevIndex])
  }

  fun cyclePushJoint() {
    val next = (simulation.pushJointIndex + 1) % 4
    simulation.pushJointIndex = next
    _uiState.update { it.copy(pushJointIndex = next) }
    triggerHaptic(20, 100)
  }

  fun togglePause() {
    _uiState.update { it.copy(isPaused = !it.isPaused) }
  }

  fun setLaunchAngle(angle: Float) {
    _uiState.update { it.copy(launchAngleDegrees = angle) }
  }

  fun setPose(pose: DismountPose) {
    repository.selectPose(pose)
    _uiState.update { it.copy(pose = pose) }
    simulation.setConfiguration(_uiState.value.stage, _uiState.value.vehicle, pose)
  }

  private fun launchDismount() {
    val state = _uiState.value
    _uiState.update {
      it.copy(
        phase = GamePhase.SIMULATING,
        score = 0,
        brokenBonesCount = 0,
        currentSpeedKmh = 0f,
        maxSpeedKmh = 0f,
        airTimeSeconds = 0f,
        somersaultsCount = 0,
        recentBrokenBone = null,
        crashReport = null,
        isSlowMoActive = false,
        timeDilation = 1.0f
      )
    }
    simulation.timeDilation = 1.0f
    simulation.launch(state.power, state.launchAngleDegrees)
    triggerHaptic(100, 255)
  }

  fun stopSimulationManually() {
    if (_uiState.value.phase == GamePhase.SIMULATING) {
      simulation.isFinished = true
      onSimulationFinished()
    }
  }

  private fun onSimulationFinished() {
    val state = _uiState.value
    slowMoJob?.cancel()
    simulation.timeDilation = 1.0f

    val brokenBones = simulation.ragdoll.boneStatusMap.values.toList()
    val fractureBonus = brokenBones.count { it.isBroken } * 500
    val airBonus = (simulation.airTimeSeconds * 400).toInt()
    val somersaultBonus = simulation.ragdoll.somersaultCount * 1200
    val totalScore = state.score + fractureBonus + airBonus + somersaultBonus
    val cashEarned = max(50, totalScore / 25)

    repository.addCash(cashEarned)
    val isHighScore = repository.saveHighScore(state.stage.id, totalScore)
    val newBest = if (isHighScore) totalScore else state.bestScore

    val report = CrashReport(
      totalScore = totalScore,
      cashEarned = cashEarned,
      brokenBones = brokenBones,
      somersaultsCount = simulation.ragdoll.somersaultCount,
      airTimeSeconds = simulation.airTimeSeconds,
      maxSpeedKmh = simulation.maxSpeed,
      vehicleDamageScore = simulation.vehicleBody?.chassisDamagePoints ?: 0,
      stageName = state.stage.name,
      vehicleName = state.vehicle.name,
      isNewHighScore = isHighScore
    )

    _uiState.update {
      it.copy(
        phase = GamePhase.FINISHED,
        score = totalScore,
        bestScore = newBest,
        crashReport = report,
        isSlowMoActive = false,
        timeDilation = 1.0f
      )
    }
    triggerHaptic(180, 255)
  }

  fun resetRun() {
    slowMoJob?.cancel()
    powerChargingJob?.cancel()
    simulation.timeDilation = 1.0f
    simulation.reset()
    _uiState.update {
      it.copy(
        phase = GamePhase.PREPARING,
        score = 0,
        brokenBonesCount = 0,
        currentSpeedKmh = 0f,
        maxSpeedKmh = 0f,
        airTimeSeconds = 0f,
        somersaultsCount = 0,
        recentBrokenBone = null,
        crashReport = null,
        isPaused = false,
        isSlowMoActive = false,
        timeDilation = 1.0f
      )
    }
  }

  fun addObstacle(type: ObstacleType, distanceAlongTrack: Float) {
    val newObstacle = PlacedObstacle(
      id = "obs_${System.currentTimeMillis()}",
      type = type,
      distanceAlongTrack = distanceAlongTrack
    )
    simulation.obstacles.add(newObstacle)
    _uiState.update { it.copy(placedObstacles = simulation.obstacles.toList()) }
  }

  fun clearObstacles() {
    simulation.obstacles.clear()
    _uiState.update { it.copy(placedObstacles = emptyList()) }
  }

  fun openShop(open: Boolean) {
    _uiState.update { it.copy(isShopOpen = open) }
  }

  fun openXRayModal(open: Boolean) {
    _uiState.update { it.copy(isXRayModalOpen = open) }
  }

  fun openObstaclePicker(open: Boolean) {
    _uiState.update { it.copy(isObstaclePickerOpen = open) }
  }

  fun buyAndSelectVehicle(vehicle: Vehicle) {
    if (repository.unlockVehicle(vehicle.id, vehicle.price)) {
      _uiState.update { it.copy(vehicle = vehicle) }
      simulation.setConfiguration(_uiState.value.stage, vehicle, _uiState.value.pose)
    }
  }

  fun buyAndSelectStage(stage: Stage) {
    if (repository.unlockStage(stage.id, stage.price)) {
      val best = repository.getHighScore(stage.id)
      _uiState.update { it.copy(stage = stage, bestScore = best) }
      simulation.setConfiguration(stage, _uiState.value.vehicle, _uiState.value.pose)
    }
  }

  fun buyObstacle(type: ObstacleType): Boolean {
    return repository.unlockObstacle(type.id, type.price)
  }

  private fun triggerHaptic(durationMs: Long, amplitude: Int) {
    try {
      if (vibrator != null && vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(
            VibrationEffect.createOneShot(
              durationMs,
              amplitude.coerceIn(1, 255)
            )
          )
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(durationMs)
        }
      }
    } catch (_: Exception) {
      // Ignore vibration errors on emulators/unsupported devices
    }
  }

  override fun onCleared() {
    super.onCleared()
    loopJob?.cancel()
    powerChargingJob?.cancel()
    slowMoJob?.cancel()
  }
}
