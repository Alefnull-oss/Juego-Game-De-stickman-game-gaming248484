package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ChalkButtonDark
import com.example.ui.theme.DismountBorder
import com.example.ui.theme.DismountSurface
import com.example.ui.theme.DismountWhite
import com.example.ui.theme.FractureRed
import com.example.viewmodel.GameViewModel

@Composable
fun DismountMainScreen(viewModel: GameViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val cash by viewModel.repository.cash.collectAsState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("dismount_main_screen")
  ) {
    // 1. Full-screen Physics Simulation Canvas in Landscape
    DismountCanvas(
      simulation = viewModel.simulation,
      phase = uiState.phase,
      isSlowMoActive = uiState.isSlowMoActive,
      renderTick = uiState.renderTick,
      onStickmanClicked = { viewModel.cyclePushJoint() },
      modifier = Modifier.fillMaxSize()
    )

    // 2. Dynamic Bone Broken Alert Banner
    uiState.recentBrokenBone?.let { (bone, timestamp) ->
      val isRecent = System.currentTimeMillis() - timestamp < 1800
      AnimatedVisibility(
        visible = isRecent,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 16.dp)
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FractureRed.copy(alpha = 0.94f))
            .border(1.dp, Color.White, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "¡${bone.clinicalName.uppercase()}! +${bone.pointValue}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }

    // 3. Landscape Controls matching reference aesthetic
    LandscapeControlOverlay(
      uiState = uiState,
      cash = cash,
      onTogglePower = { viewModel.togglePowerCharging() },
      onReset = { viewModel.resetRun() },
      onStopManually = { viewModel.stopSimulationManually() },
      onPrevPose = { viewModel.prevPose() },
      onNextPose = { viewModel.nextPose() },
      onCyclePushJoint = { viewModel.cyclePushJoint() },
      onTogglePause = { viewModel.togglePause() },
      onOpenShop = { viewModel.openShop(true) },
      onOpenObstacles = { viewModel.openObstaclePicker(true) },
      onOpenXRay = { viewModel.openXRayModal(true) }
    )

    // 4. Pause Dialog
    if (uiState.isPaused) {
      PauseDialog(
        onResume = { viewModel.togglePause() },
        onRestart = {
          viewModel.togglePause()
          viewModel.resetRun()
        },
        onOpenShop = {
          viewModel.togglePause()
          viewModel.openShop(true)
        }
      )
    }

    // 5. Modals (X-Ray, Shop, Obstacles, Crash Report)
    if (uiState.isXRayModalOpen) {
      XRayModalDialog(
        boneStatuses = viewModel.simulation.ragdoll.boneStatusMap,
        onDismiss = { viewModel.openXRayModal(false) }
      )
    }

    if (uiState.isShopOpen) {
      ShopDialog(
        viewModel = viewModel,
        onDismiss = { viewModel.openShop(false) }
      )
    }

    if (uiState.isObstaclePickerOpen) {
      ObstaclePlacementDialog(
        viewModel = viewModel,
        onDismiss = { viewModel.openObstaclePicker(false) }
      )
    }

    uiState.crashReport?.let { report ->
      CrashReportDialog(
        report = report,
        onRetry = { viewModel.resetRun() },
        onOpenShop = {
          viewModel.resetRun()
          viewModel.openShop(true)
        },
        onInspectXRay = { viewModel.openXRayModal(true) }
      )
    }
  }
}

@Composable
private fun PauseDialog(
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onOpenShop: () -> Unit
) {
  Dialog(onDismissRequest = onResume) {
    Card(
      modifier = Modifier
        .width(280.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, DismountBorder, RoundedCornerShape(16.dp))
        .testTag("pause_dialog"),
      colors = CardDefaults.cardColors(containerColor = DismountSurface)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "JUEGO EN PAUSA",
          fontSize = 15.sp,
          fontWeight = FontWeight.Black,
          color = DismountWhite,
          fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
          onClick = onResume,
          modifier = Modifier.fillMaxWidth().height(42.dp),
          colors = ButtonDefaults.buttonColors(containerColor = DismountWhite),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ChalkButtonDark)
          Spacer(modifier = Modifier.width(6.dp))
          Text("CONTINUAR", color = ChalkButtonDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Button(
          onClick = onRestart,
          modifier = Modifier.fillMaxWidth().height(42.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ChalkButtonDark),
          border = androidx.compose.foundation.BorderStroke(1.dp, DismountBorder),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, tint = DismountWhite)
          Spacer(modifier = Modifier.width(6.dp))
          Text("REINICIAR", color = DismountWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Button(
          onClick = onOpenShop,
          modifier = Modifier.fillMaxWidth().height(42.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ChalkButtonDark),
          border = androidx.compose.foundation.BorderStroke(1.dp, DismountBorder),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = DismountWhite)
          Spacer(modifier = Modifier.width(6.dp))
          Text("TIENDA", color = DismountWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}
