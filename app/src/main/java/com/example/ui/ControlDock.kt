package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DismountPose
import com.example.ui.theme.BoneIntactGreen
import com.example.ui.theme.BoostBlue
import com.example.ui.theme.CashGold
import com.example.ui.theme.DismountBackground
import com.example.ui.theme.DismountBorder
import com.example.ui.theme.DismountSurface
import com.example.ui.theme.DismountSurfaceElevated
import com.example.ui.theme.DismountWhite
import com.example.ui.theme.FractureRed
import com.example.ui.theme.HazardOrange
import com.example.viewmodel.GamePhase
import com.example.viewmodel.GameUiState

@Composable
fun ControlDock(
  uiState: GameUiState,
  onTogglePower: () -> Unit,
  onReset: () -> Unit,
  onStopManually: () -> Unit,
  onSelectPose: (DismountPose) -> Unit,
  onSelectAngle: (Float) -> Unit,
  onOpenShop: () -> Unit,
  onOpenObstacles: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showPoseSelector by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("control_dock"),
    color = DismountSurface.copy(alpha = 0.95f),
    tonalElevation = 8.dp,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, DismountBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      // 1. Live Telemetry Strip
      TelemetryStrip(uiState = uiState)

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Pose Selector Row (if expanded)
      AnimatedVisibility(visible = showPoseSelector && uiState.phase == GamePhase.PREPARING) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
          Text(
            text = "SELECCIONA POSTURA DE DESMONTE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            DismountPose.entries.forEach { pose ->
              val isSelected = uiState.pose == pose
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) BoostBlue.copy(alpha = 0.25f) else DismountSurfaceElevated)
                  .border(1.dp, if (isSelected) BoostBlue else DismountBorder, RoundedCornerShape(8.dp))
                  .clickable {
                    onSelectPose(pose)
                    showPoseSelector = false
                  }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("pose_option_${pose.id}")
              ) {
                Column {
                  Text(
                    text = pose.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) BoostBlue else DismountWhite
                  )
                  Text(
                    text = pose.subtitle,
                    fontSize = 9.sp,
                    color = Color.Gray
                  )
                }
              }
            }
          }
        }
      }

      // 3. Main Launch Controls Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Quick Pose & Angle Trigger
        if (uiState.phase == GamePhase.PREPARING) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Pose button
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DismountSurfaceElevated)
                .border(1.dp, DismountBorder, RoundedCornerShape(8.dp))
                .clickable { showPoseSelector = !showPoseSelector }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("toggle_pose_button")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Accessibility, contentDescription = null, tint = DismountWhite, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = uiState.pose.displayName,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = DismountWhite
                )
              }
            }

            // Angle Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf(10f, 25f, 40f).forEach { angle ->
                val isSel = uiState.launchAngleDegrees == angle
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSel) HazardOrange.copy(alpha = 0.3f) else DismountSurfaceElevated)
                    .border(1.dp, if (isSel) HazardOrange else DismountBorder, RoundedCornerShape(6.dp))
                    .clickable { onSelectAngle(angle) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("angle_${angle.toInt()}_button")
                ) {
                  Text(
                    text = "${angle.toInt()}°",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) HazardOrange else Color.Gray,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
            }
          }
        }

        // Center / Dynamic Power Meter
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = when (uiState.phase) {
                GamePhase.PREPARING -> "POTENCIA LISTA"
                GamePhase.CHARGING_POWER -> "¡PULSA PARA DESMONTAR!"
                GamePhase.SIMULATING -> "SIMULANDO FÍSICAS"
                GamePhase.FINISHED -> "DESMONTE FINALIZADO"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (uiState.phase == GamePhase.CHARGING_POWER) FractureRed else Color.LightGray
            )

            Text(
              text = "${(uiState.power * 100).toInt()}%",
              fontSize = 13.sp,
              fontWeight = FontWeight.ExtraBold,
              color = if (uiState.power > 0.8f) FractureRed else if (uiState.power > 0.5f) HazardOrange else BoneIntactGreen,
              fontFamily = FontFamily.Monospace
            )
          }

          // Custom animated power gauge
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(18.dp)
              .clip(RoundedCornerShape(9.dp))
              .background(DismountBackground)
              .border(1.dp, DismountBorder, RoundedCornerShape(9.dp))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(uiState.power.coerceIn(0.01f, 1f))
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(
                      BoneIntactGreen,
                      HazardOrange,
                      FractureRed
                    )
                  )
                )
            )
          }
        }

        // Main Action Button
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_launch")
        val scale by infiniteTransition.animateFloat(
          initialValue = 1.0f,
          targetValue = 1.05f,
          animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
          ),
          label = "scale"
        )

        when (uiState.phase) {
          GamePhase.PREPARING -> {
            Button(
              onClick = onTogglePower,
              modifier = Modifier
                .height(48.dp)
                .scale(scale)
                .testTag("start_power_button"),
              colors = ButtonDefaults.buttonColors(containerColor = DismountWhite),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Bolt, contentDescription = null, tint = DismountBackground)
              Spacer(modifier = Modifier.width(4.dp))
              Text("CARGAR", fontWeight = FontWeight.ExtraBold, color = DismountBackground, fontSize = 12.sp)
            }
          }

          GamePhase.CHARGING_POWER -> {
            Button(
              onClick = onTogglePower,
              modifier = Modifier
                .height(48.dp)
                .scale(scale)
                .testTag("launch_dismount_button"),
              colors = ButtonDefaults.buttonColors(containerColor = FractureRed),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text("¡DESMONTAR!", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
            }
          }

          GamePhase.SIMULATING -> {
            Button(
              onClick = onStopManually,
              modifier = Modifier
                .height(48.dp)
                .testTag("stop_simulation_button"),
              colors = ButtonDefaults.buttonColors(containerColor = DismountSurfaceElevated),
              border = androidx.compose.foundation.BorderStroke(1.dp, DismountBorder),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Stop, contentDescription = null, tint = FractureRed)
              Spacer(modifier = Modifier.width(4.dp))
              Text("PARAR", fontWeight = FontWeight.Bold, color = DismountWhite, fontSize = 11.sp)
            }
          }

          GamePhase.FINISHED -> {
            Button(
              onClick = onReset,
              modifier = Modifier
                .height(48.dp)
                .testTag("reset_button"),
              colors = ButtonDefaults.buttonColors(containerColor = BoostBlue),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text("OTRO", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            }
          }
        }
      }

      // 4. Quick Toolbar
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Obstacles button
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(DismountSurfaceElevated)
              .border(1.dp, DismountBorder, RoundedCornerShape(8.dp))
              .clickable(onClick = onOpenObstacles)
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("open_obstacles_button")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AddAlert, contentDescription = null, tint = HazardOrange, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("TRAMPAS (${uiState.placedObstacles.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DismountWhite)
            }
          }

          // Shop button
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(DismountSurfaceElevated)
              .border(1.dp, DismountBorder, RoundedCornerShape(8.dp))
              .clickable(onClick = onOpenShop)
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("open_shop_button")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = CashGold, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("TIENDA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DismountWhite)
            }
          }
        }

        IconButton(
          onClick = onReset,
          modifier = Modifier.testTag("quick_reset_icon_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = DismountWhite)
        }
      }
    }
  }
}

@Composable
private fun TelemetryStrip(uiState: GameUiState) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(DismountBackground)
      .border(1.dp, DismountBorder, RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Score
    Column {
      Text(text = "PUNTOS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
      Text(
        text = String.format("%,d", uiState.score),
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        color = CashGold,
        fontFamily = FontFamily.Monospace
      )
    }

    // Speedometer
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = "VELOCIDAD", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
      Text(
        text = "${uiState.currentSpeedKmh.toInt()} km/h",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = DismountWhite,
        fontFamily = FontFamily.Monospace
      )
    }

    // Acrobatics / Somersaults
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = "VOLTERETAS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
      Text(
        text = "x${uiState.somersaultsCount}",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = BoostBlue,
        fontFamily = FontFamily.Monospace
      )
    }

    // Broken bones counter
    Column(horizontalAlignment = Alignment.End) {
      Text(text = "FRACTURAS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
      Text(
        text = "${uiState.brokenBonesCount} / 13",
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (uiState.brokenBonesCount > 0) FractureRed else BoneIntactGreen,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}
