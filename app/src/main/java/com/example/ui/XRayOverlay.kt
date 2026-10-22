package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.BoneStatus
import com.example.model.BoneType
import com.example.ui.theme.BoneIntactGreen
import com.example.ui.theme.CashGold
import com.example.ui.theme.DismountBorder
import com.example.ui.theme.DismountSurface
import com.example.ui.theme.DismountSurfaceElevated
import com.example.ui.theme.DismountWhite
import com.example.ui.theme.FractureRed
import com.example.ui.theme.XRayBacking

@Composable
fun XRayMiniHud(
  boneStatuses: Map<BoneType, BoneStatus>,
  onExpandClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val brokenCount = boneStatuses.values.count { it.isBroken }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(XRayBacking)
      .border(1.5.dp, if (brokenCount > 0) FractureRed else DismountBorder, RoundedCornerShape(12.dp))
      .clickable(onClick = onExpandClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag("xray_mini_hud")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Mini Skeleton Icon Canvas
      XRaySkeletonCanvas(
        boneStatuses = boneStatuses,
        modifier = Modifier.size(width = 30.dp, height = 48.dp)
      )

      Column {
        Text(
          text = "RAYOS X",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = if (brokenCount > 0) FractureRed else BoneIntactGreen,
          letterSpacing = 1.sp
        )
        Text(
          text = "$brokenCount / ${boneStatuses.size}",
          fontSize = 13.sp,
          fontWeight = FontWeight.ExtraBold,
          color = DismountWhite,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = if (brokenCount == 0) "INTACTO" else "FRACTURAS",
          fontSize = 9.sp,
          color = if (brokenCount > 0) FractureRed else BoneIntactGreen
        )
      }
    }
  }
}

@Composable
fun XRaySkeletonCanvas(
  boneStatuses: Map<BoneType, BoneStatus>,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "fracture_pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(400),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    fun boneColor(type: BoneType): Color {
      val isBroken = boneStatuses[type]?.isBroken == true
      return if (isBroken) FractureRed.copy(alpha = pulseAlpha) else BoneIntactGreen.copy(alpha = 0.85f)
    }

    val cx = w * 0.5f

    // Head
    val headRadius = w * 0.16f
    drawCircle(
      color = boneColor(BoneType.CRANIUM),
      radius = headRadius,
      center = Offset(cx, h * 0.12f)
    )

    // Cervical Spine (Neck)
    drawLine(
      color = boneColor(BoneType.CERVICAL_SPINE),
      start = Offset(cx, h * 0.19f),
      end = Offset(cx, h * 0.28f),
      strokeWidth = w * 0.08f,
      cap = StrokeCap.Round
    )

    // Ribs / Thorax
    drawLine(
      color = boneColor(BoneType.THORAX_RIBS),
      start = Offset(cx - w * 0.22f, h * 0.33f),
      end = Offset(cx + w * 0.22f, h * 0.33f),
      strokeWidth = w * 0.06f,
      cap = StrokeCap.Round
    )

    // Lumbar Spine
    drawLine(
      color = boneColor(BoneType.LUMBAR_SPINE),
      start = Offset(cx, h * 0.28f),
      end = Offset(cx, h * 0.48f),
      strokeWidth = w * 0.09f,
      cap = StrokeCap.Round
    )

    // Pelvis
    drawLine(
      color = boneColor(BoneType.PELVIS),
      start = Offset(cx - w * 0.2f, h * 0.50f),
      end = Offset(cx + w * 0.2f, h * 0.50f),
      strokeWidth = w * 0.08f,
      cap = StrokeCap.Round
    )

    // Left Arm
    drawLine(
      color = boneColor(BoneType.LEFT_HUMERUS),
      start = Offset(cx - w * 0.18f, h * 0.28f),
      end = Offset(cx - w * 0.38f, h * 0.42f),
      strokeWidth = w * 0.06f,
      cap = StrokeCap.Round
    )
    drawLine(
      color = boneColor(BoneType.LEFT_FOREARM),
      start = Offset(cx - w * 0.38f, h * 0.42f),
      end = Offset(cx - w * 0.44f, h * 0.60f),
      strokeWidth = w * 0.05f,
      cap = StrokeCap.Round
    )

    // Right Arm
    drawLine(
      color = boneColor(BoneType.RIGHT_HUMERUS),
      start = Offset(cx + w * 0.18f, h * 0.28f),
      end = Offset(cx + w * 0.38f, h * 0.42f),
      strokeWidth = w * 0.06f,
      cap = StrokeCap.Round
    )
    drawLine(
      color = boneColor(BoneType.RIGHT_FOREARM),
      start = Offset(cx + w * 0.38f, h * 0.42f),
      end = Offset(cx + w * 0.44f, h * 0.60f),
      strokeWidth = w * 0.05f,
      cap = StrokeCap.Round
    )

    // Left Leg
    drawLine(
      color = boneColor(BoneType.LEFT_FEMUR),
      start = Offset(cx - w * 0.15f, h * 0.52f),
      end = Offset(cx - w * 0.22f, h * 0.72f),
      strokeWidth = w * 0.07f,
      cap = StrokeCap.Round
    )
    drawLine(
      color = boneColor(BoneType.LEFT_TIBIA),
      start = Offset(cx - w * 0.22f, h * 0.72f),
      end = Offset(cx - w * 0.25f, h * 0.94f),
      strokeWidth = w * 0.06f,
      cap = StrokeCap.Round
    )

    // Right Leg
    drawLine(
      color = boneColor(BoneType.RIGHT_FEMUR),
      start = Offset(cx + w * 0.15f, h * 0.52f),
      end = Offset(cx + w * 0.22f, h * 0.72f),
      strokeWidth = w * 0.07f,
      cap = StrokeCap.Round
    )
    drawLine(
      color = boneColor(BoneType.RIGHT_TIBIA),
      start = Offset(cx + w * 0.22f, h * 0.72f),
      end = Offset(cx + w * 0.25f, h * 0.94f),
      strokeWidth = w * 0.06f,
      cap = StrokeCap.Round
    )
  }
}

@Composable
fun XRayModalDialog(
  boneStatuses: Map<BoneType, BoneStatus>,
  onDismiss: () -> Unit
) {
  val brokenList = boneStatuses.values.filter { it.isBroken }
  val intactList = boneStatuses.values.filter { !it.isBroken }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.5.dp, DismountBorder, RoundedCornerShape(16.dp))
        .testTag("xray_modal_dialog"),
      colors = CardDefaults.cardColors(containerColor = DismountSurface)
    ) {
      Column(
        modifier = Modifier.padding(18.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = FractureRed,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "INFORME DE RAYOS X",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = DismountWhite
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_xray_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = DismountWhite)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Skeleton Visualizer & Overview
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(XRayBacking)
            .border(1.dp, DismountBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          XRaySkeletonCanvas(
            boneStatuses = boneStatuses,
            modifier = Modifier.size(width = 90.dp, height = 150.dp)
          )

          Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "ESTADO ESQUELÉTICO",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.LightGray
            )

            Text(
              text = "${brokenList.size} DE ${boneStatuses.size} ROTOS",
              fontSize = 17.sp,
              fontWeight = FontWeight.ExtraBold,
              color = if (brokenList.isNotEmpty()) FractureRed else BoneIntactGreen,
              fontFamily = FontFamily.Monospace
            )

            val traumaPercentage = (brokenList.size.toFloat() / boneStatuses.size * 100f).toInt()
            Text(
              text = "Traumatismo: $traumaPercentage%",
              fontSize = 13.sp,
              color = DismountWhite
            )

            val pointsEarned = brokenList.sumOf { it.type.pointValue }
            Text(
              text = "+$pointsEarned pts",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = CashGold,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "REGISTRO CLÍNICO DE LESIONES",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.Gray,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (brokenList.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Sin fracturas detectadas todavía. ¡Lanza al Stickman!",
                  color = Color.Gray,
                  fontSize = 13.sp
                )
              }
            }
          }

          items(brokenList) { bone ->
            BoneItemRow(bone = bone, isBroken = true)
          }

          items(intactList) { bone ->
            BoneItemRow(bone = bone, isBroken = false)
          }
        }
      }
    }
  }
}

@Composable
private fun BoneItemRow(bone: BoneStatus, isBroken: Boolean) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(if (isBroken) FractureRed.copy(alpha = 0.12f) else DismountSurfaceElevated)
      .border(
        width = 1.dp,
        color = if (isBroken) FractureRed.copy(alpha = 0.4f) else DismountBorder.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
      )
      .padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      Text(
        text = bone.type.clinicalName,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = if (isBroken) DismountWhite else Color.Gray
      )
      Text(
        text = if (isBroken) "FRACTURA SEVERA" else "ESTRUCTURA INTACTA",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (isBroken) FractureRed else BoneIntactGreen
      )
    }

    Text(
      text = if (isBroken) "+${bone.type.pointValue}" else "0",
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = if (isBroken) CashGold else Color.DarkGray,
      fontFamily = FontFamily.Monospace
    )
  }
}
