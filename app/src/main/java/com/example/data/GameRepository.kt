package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DismountPose
import com.example.model.ObstacleType
import com.example.model.PlacedObstacle
import com.example.model.Stage
import com.example.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("stickman_dismount_prefs", Context.MODE_PRIVATE)

  private val _cash = MutableStateFlow(prefs.getInt(KEY_CASH, 500))
  val cash: StateFlow<Int> = _cash.asStateFlow()

  private val _unlockedVehicles = MutableStateFlow(
    prefs.getStringSet(KEY_UNLOCKED_VEHICLES, setOf("on_foot", "shopping_cart")) ?: setOf("on_foot", "shopping_cart")
  )
  val unlockedVehicles: StateFlow<Set<String>> = _unlockedVehicles.asStateFlow()

  private val _unlockedStages = MutableStateFlow(
    prefs.getStringSet(KEY_UNLOCKED_STAGES, setOf("stairs")) ?: setOf("stairs")
  )
  val unlockedStages: StateFlow<Set<String>> = _unlockedStages.asStateFlow()

  private val _unlockedObstacles = MutableStateFlow(
    prefs.getStringSet(KEY_UNLOCKED_OBSTACLES, setOf("mine")) ?: setOf("mine")
  )
  val unlockedObstacles: StateFlow<Set<String>> = _unlockedObstacles.asStateFlow()

  private val _selectedVehicleId = MutableStateFlow(prefs.getString(KEY_SELECTED_VEHICLE, "shopping_cart") ?: "shopping_cart")
  val selectedVehicleId: StateFlow<String> = _selectedVehicleId.asStateFlow()

  private val _selectedStageId = MutableStateFlow(prefs.getString(KEY_SELECTED_STAGE, "stairs") ?: "stairs")
  val selectedStageId: StateFlow<String> = _selectedStageId.asStateFlow()

  private val _selectedPose = MutableStateFlow(DismountPose.STANDING)
  val selectedPose: StateFlow<DismountPose> = _selectedPose.asStateFlow()

  fun addCash(amount: Int) {
    val newTotal = _cash.value + amount
    _cash.value = newTotal
    prefs.edit().putInt(KEY_CASH, newTotal).apply()
  }

  fun spendCash(amount: Int): Boolean {
    if (_cash.value >= amount) {
      val newTotal = _cash.value - amount
      _cash.value = newTotal
      prefs.edit().putInt(KEY_CASH, newTotal).apply()
      return true
    }
    return false
  }

  fun unlockVehicle(vehicleId: String, cost: Int): Boolean {
    if (_unlockedVehicles.value.contains(vehicleId)) return true
    if (spendCash(cost)) {
      val updated = _unlockedVehicles.value + vehicleId
      _unlockedVehicles.value = updated
      prefs.edit().putStringSet(KEY_UNLOCKED_VEHICLES, updated).apply()
      selectVehicle(vehicleId)
      return true
    }
    return false
  }

  fun unlockStage(stageId: String, cost: Int): Boolean {
    if (_unlockedStages.value.contains(stageId)) return true
    if (spendCash(cost)) {
      val updated = _unlockedStages.value + stageId
      _unlockedStages.value = updated
      prefs.edit().putStringSet(KEY_UNLOCKED_STAGES, updated).apply()
      selectStage(stageId)
      return true
    }
    return false
  }

  fun unlockObstacle(obstacleId: String, cost: Int): Boolean {
    if (_unlockedObstacles.value.contains(obstacleId)) return true
    if (spendCash(cost)) {
      val updated = _unlockedObstacles.value + obstacleId
      _unlockedObstacles.value = updated
      prefs.edit().putStringSet(KEY_UNLOCKED_OBSTACLES, updated).apply()
      return true
    }
    return false
  }

  fun selectVehicle(vehicleId: String) {
    _selectedVehicleId.value = vehicleId
    prefs.edit().putString(KEY_SELECTED_VEHICLE, vehicleId).apply()
  }

  fun selectStage(stageId: String) {
    _selectedStageId.value = stageId
    prefs.edit().putString(KEY_SELECTED_STAGE, stageId).apply()
  }

  fun selectPose(pose: DismountPose) {
    _selectedPose.value = pose
  }

  fun getHighScore(stageId: String): Int {
    return prefs.getInt(KEY_HIGH_SCORE_PREFIX + stageId, 0)
  }

  fun saveHighScore(stageId: String, score: Int): Boolean {
    val current = getHighScore(stageId)
    if (score > current) {
      prefs.edit().putInt(KEY_HIGH_SCORE_PREFIX + stageId, score).apply()
      return true
    }
    return false
  }

  companion object {
    private const val KEY_CASH = "key_dismount_cash"
    private const val KEY_UNLOCKED_VEHICLES = "key_unlocked_vehicles"
    private const val KEY_UNLOCKED_STAGES = "key_unlocked_stages"
    private const val KEY_UNLOCKED_OBSTACLES = "key_unlocked_obstacles"
    private const val KEY_SELECTED_VEHICLE = "key_selected_vehicle"
    private const val KEY_SELECTED_STAGE = "key_selected_stage"
    private const val KEY_HIGH_SCORE_PREFIX = "key_high_score_"
  }
}
