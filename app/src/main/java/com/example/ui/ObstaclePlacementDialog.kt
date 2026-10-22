package com.example.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.ObstacleType
import com.example.model.PlacedObstacle
import com.example.ui.theme.CashGold
import com.example.ui.theme.DismountBackground
import com.example.ui.theme.DismountBorder
import com.example.ui.theme.DismountSurface
import com.example.ui.theme.DismountSurfaceElevated
import com.example.ui.theme.DismountWhite
import com.example.ui.theme.FractureRed
import com.example.ui.theme.HazardOrange
import com.example.viewmodel.GameViewModel

@Composable
fun ObstaclePlacementDialog(
  viewModel: GameViewModel,
  onDismiss: () -> Unit
) {
  val repo = viewModel.repository
  val unlockedObstacles by repo.unlockedObstacles.collectAsState()
  val uiState by viewModel.uiState.collectAsState()

  var selectedType by remember {
    mutableStateOf(
      ObstacleType.DEFAULT_LIST.firstOrNull { unlockedObstacles.contains(it.id) } ?: ObstacleType.LANDMINE
    )
  }
  var selectedDistance by remember { mutableFloatStateOf(450f) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.5.dp, DismountBorder, RoundedCornerShape(16.dp))
        .testTag("obstacle_placement_dialog"),
      colors = CardDefaults.cardColors(containerColor = DismountSurface)
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = HazardOrange, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "COLOCAR OBSTÁCULOS",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = DismountWhite
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_obstacles_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = DismountWhite)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "SELECCIONA TRAMPA PARA LA PISTA",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = Color.Gray,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Obstacles grid / list
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(ObstacleType.DEFAULT_LIST) { obs ->
            val isUnlocked = unlockedObstacles.contains(obs.id)
            val isSelected = selectedType == obs

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) HazardOrange.copy(alpha = 0.2f) else DismountSurfaceElevated)
                .border(1.dp, if (isSelected) HazardOrange else DismountBorder, RoundedCornerShape(8.dp))
                .clickable(enabled = isUnlocked) {
                  selectedType = obs
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = obs.displayName,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (!isUnlocked) Color.Gray else if (isSelected) HazardOrange else DismountWhite
                )
                Text(
                  text = if (isUnlocked) obs.subtitle else "Bloqueado en la Tienda ($${obs.price})",
                  fontSize = 9.sp,
                  color = Color.Gray
                )
              }

              if (isUnlocked) {
                Text(
                  text = "+${obs.bonusPoints} pts",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = CashGold,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Position on track selector
        Text(
          text = "DISTANCIA EN LA PISTA: ${selectedDistance.toInt()}m",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(300f, 450f, 650f, 900f).forEach { dist ->
            val isSel = selectedDistance == dist
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSel) DismountWhite else DismountSurfaceElevated)
                .clickable { selectedDistance = dist }
                .padding(vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${dist.toInt()}m",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSel) DismountBackground else DismountWhite,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Add Obstacle Button
        val canAdd = unlockedObstacles.contains(selectedType.id) &&
          uiState.placedObstacles.size < uiState.stage.obstacleSlots

        Button(
          onClick = {
            viewModel.addObstacle(selectedType, selectedDistance)
          },
          enabled = canAdd,
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("add_obstacle_confirm_button"),
          colors = ButtonDefaults.buttonColors(containerColor = HazardOrange),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (uiState.placedObstacles.size >= uiState.stage.obstacleSlots) "LÍMITE ALCANZADO" else "AÑADIR A LA PISTA",
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            fontSize = 11.sp
          )
        }

        // Active Obstacles List & Clear Button
        if (uiState.placedObstacles.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "COLOCADOS (${uiState.placedObstacles.size}/${uiState.stage.obstacleSlots}):",
              fontSize = 10.sp,
              color = Color.Gray,
              fontWeight = FontWeight.Bold
            )

            Text(
              text = "LIMPIAR TODOS",
              fontSize = 10.sp,
              color = FractureRed,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .clickable { viewModel.clearObstacles() }
                .padding(4.dp)
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            uiState.placedObstacles.forEach { obs ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(DismountSurfaceElevated)
                  .border(0.5.dp, DismountBorder, RoundedCornerShape(6.dp))
                  .padding(horizontal = 6.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "${obs.type.displayName} (${obs.distanceAlongTrack.toInt()}m)",
                  fontSize = 9.sp,
                  color = DismountWhite
                )
              }
            }
          }
        }
      }
    }
  }
}
