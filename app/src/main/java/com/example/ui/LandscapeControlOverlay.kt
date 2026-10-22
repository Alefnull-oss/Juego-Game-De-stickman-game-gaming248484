package com.example.ui

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CashGold
import com.example.ui.theme.ChalkButtonBorder
import com.example.ui.theme.ChalkButtonDark
import com.example.ui.theme.ChalkButtonText
import com.example.ui.theme.FractureRed
import com.example.ui.theme.HazardOrange
import com.example.ui.theme.PushArrowOrange
import com.example.viewmodel.GamePhase
import com.example.viewmodel.GameUiState

@Composable
fun LandscapeControlOverlay(
  uiState: GameUiState,
  cash: Int,
  onTogglePower: () -> Unit,
  onReset: () -> Unit,
  onStopManually: () -> Unit,
  onPrevPose: () -> Unit,
  onNextPose: () -> Unit,
  onCyclePushJoint: () -> Unit,
  onTogglePause: () -> Unit,
  onOpenShop: () -> Unit,
  onOpenObstacles: () -> Unit,
  onOpenXRay: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(14.dp)
  ) {
    // ------------------------------------------------------------------
    // TOP-LEFT: Golden Coin Balance + Pause Button
    // ------------------------------------------------------------------
    Row(
      modifier = Modifier.align(Alignment.TopStart),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Golden Currency Counter
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(ChalkButtonDark)
          .border(1.dp, ChalkButtonBorder, RoundedCornerShape(10.dp))
          .clickable(onClick = onOpenShop)
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("currency_badge"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.MonetizationOn,
          contentDescription = null,
          tint = CashGold,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "$cash",
          color = ChalkButtonText,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      // Pause Button ||
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(ChalkButtonDark)
          .border(1.dp, ChalkButtonBorder, RoundedCornerShape(8.dp))
          .clickable(onClick = onTogglePause)
          .testTag("pause_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
          contentDescription = "Pausa",
          tint = ChalkButtonText,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    // ------------------------------------------------------------------
    // TOP-RIGHT: SCORE: 0 & BEST: 3919 (matching reference image)
    // ------------------------------------------------------------------
    Column(
      modifier = Modifier.align(Alignment.TopEnd),
      horizontalAlignment = Alignment.End
    ) {
      Text(
        text = "SCORE: ${uiState.score}",
        color = ChalkButtonDark,
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = "BEST: ${uiState.bestScore}",
        color = Color(0xFF636970),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
      if (uiState.brokenBonesCount > 0) {
        Text(
          text = "${uiState.brokenBonesCount} FRACTURAS",
          color = FractureRed,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // ------------------------------------------------------------------
    // BOTTOM-LEFT: Action Pills [PREV POSE] [NEXT POSE] [PUSH JOINT] [VEHICLES] [TRAMPAS] [RAYOS X]
    // ------------------------------------------------------------------
    Row(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (uiState.phase == GamePhase.PREPARING) {
        // [PREV POSE]
        ChalkPillButton(
          title = "PREV\nPOSE",
          testTag = "prev_pose_button",
          onClick = onPrevPose
        )

        // [NEXT POSE]
        ChalkPillButton(
          title = "NEXT\nPOSE",
          testTag = "next_pose_button",
          onClick = onNextPose
        )

        // [PUSH JOINT]
        val jointLabel = when (uiState.pushJointIndex) {
          1 -> "CABEZA"
          2 -> "PELVIS"
          3 -> "PIERNAS"
          else -> "ESPALDA"
        }
        ChalkPillButton(
          title = "EMPUJE:\n$jointLabel",
          accentColor = PushArrowOrange,
          testTag = "push_joint_button",
          onClick = onCyclePushJoint
        )
      }

      // [VEHICLES / CHARACTER]
      ChalkPillButton(
        title = "VEHICLES\n(${uiState.vehicle.name.take(7)})",
        testTag = "vehicles_button",
        onClick = onOpenShop
      )

      // [TRAMPAS]
      ChalkPillButton(
        title = "TRAMPAS\n(${uiState.placedObstacles.size})",
        accentColor = HazardOrange,
        testTag = "traps_button",
        onClick = onOpenObstacles
      )

      // [RAYOS X]
      ChalkPillButton(
        title = "RAYOS X\n(${uiState.brokenBonesCount}/13)",
        accentColor = if (uiState.brokenBonesCount > 0) FractureRed else null,
        testTag = "xray_button",
        onClick = onOpenXRay
      )
    }

    // ------------------------------------------------------------------
    // BOTTOM-RIGHT: Big circular START / DISMOUNT Button
    // ------------------------------------------------------------------
    Column(
      modifier = Modifier.align(Alignment.BottomEnd),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Subtitle hint matching screenshot ("Tap and hold" / "Pulse para lanzar")
      val hintText = when (uiState.phase) {
        GamePhase.PREPARING -> "Pulse para cargar"
        GamePhase.CHARGING_POWER -> "¡PULSA PARA DESMONTAR!"
        GamePhase.SIMULATING -> "${uiState.currentSpeedKmh.toInt()} km/h"
        GamePhase.FINISHED -> "Fin del desmonte"
      }

      Text(
        text = hintText,
        color = if (uiState.phase == GamePhase.CHARGING_POWER) FractureRed else Color(0xFF4A4E54),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 6.dp)
      )

      // Big Circular Button with Circular Power Meter Ring
      val infiniteTransition = rememberInfiniteTransition(label = "pulse_start")
      val startScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (uiState.phase == GamePhase.CHARGING_POWER) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
          animation = tween(350),
          repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
      )

      Box(
        modifier = Modifier
          .size(92.dp)
          .scale(startScale)
          .testTag("big_start_button"),
        contentAlignment = Alignment.Center
      ) {
        // Power Ring Indicator
        if (uiState.phase == GamePhase.CHARGING_POWER) {
          CircularProgressIndicator(
            progress = { uiState.power },
            modifier = Modifier.fillMaxSize(),
            color = if (uiState.power > 0.75f) FractureRed else PushArrowOrange,
            strokeWidth = 6.dp,
            trackColor = Color(0x33000000),
            strokeCap = StrokeCap.Round
          )
        }

        // Inner solid circular button
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
              when (uiState.phase) {
                GamePhase.PREPARING -> ChalkButtonDark
                GamePhase.CHARGING_POWER -> FractureRed
                GamePhase.SIMULATING -> Color(0xD9333639)
                GamePhase.FINISHED -> ChalkButtonDark
              }
            )
            .border(
              width = 2.dp,
              color = if (uiState.phase == GamePhase.CHARGING_POWER) Color.White else ChalkButtonBorder,
              shape = CircleShape
            )
            .clickable {
              when (uiState.phase) {
                GamePhase.PREPARING, GamePhase.CHARGING_POWER -> onTogglePower()
                GamePhase.SIMULATING -> onStopManually()
                GamePhase.FINISHED -> onReset()
              }
            },
          contentAlignment = Alignment.Center
        ) {
          val label = when (uiState.phase) {
            GamePhase.PREPARING -> "START"
            GamePhase.CHARGING_POWER -> "${(uiState.power * 100).toInt()}%"
            GamePhase.SIMULATING -> "STOP"
            GamePhase.FINISHED -> "RESET"
          }

          Text(
            text = label,
            color = Color.White,
            fontSize = if (uiState.phase == GamePhase.CHARGING_POWER) 16.sp else 15.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
private fun ChalkPillButton(
  title: String,
  testTag: String,
  accentColor: Color? = null,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .background(ChalkButtonDark)
      .border(
        width = 1.dp,
        color = accentColor?.copy(alpha = 0.7f) ?: ChalkButtonBorder,
        shape = RoundedCornerShape(14.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      color = accentColor ?: ChalkButtonText,
      fontSize = 10.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      textAlign = TextAlign.Center,
      lineHeight = 12.sp
    )
  }
}
