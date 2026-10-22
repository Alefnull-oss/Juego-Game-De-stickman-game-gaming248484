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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.GameRepository
import com.example.model.ObstacleType
import com.example.model.Stage
import com.example.model.Vehicle
import com.example.ui.theme.BoostBlue
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
fun ShopDialog(
  viewModel: GameViewModel,
  onDismiss: () -> Unit
) {
  val repo = viewModel.repository
  val cash by repo.cash.collectAsState()
  val unlockedVehicles by repo.unlockedVehicles.collectAsState()
  val unlockedStages by repo.unlockedStages.collectAsState()
  val unlockedObstacles by repo.unlockedObstacles.collectAsState()
  val selectedVehicleId by repo.selectedVehicleId.collectAsState()
  val selectedStageId by repo.selectedStageId.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("VEHÍCULOS", "ESCENARIOS", "TRAMPAS")

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.5.dp, DismountBorder, RoundedCornerShape(16.dp))
        .testTag("shop_dialog"),
      colors = CardDefaults.cardColors(containerColor = DismountSurface)
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        // Header with Cash Balance
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "TIENDA DE DESMONTE",
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              color = DismountWhite
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = CashGold, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "$$cash",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CashGold,
                fontFamily = FontFamily.Monospace
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_shop_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = DismountWhite)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = DismountSurfaceElevated,
          contentColor = DismountWhite,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = CashGold
            )
          }
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Text(
                  text = title,
                  fontSize = 11.sp,
                  fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                  color = if (selectedTab == index) CashGold else Color.Gray
                )
              },
              modifier = Modifier.testTag("shop_tab_$index")
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        Box(modifier = Modifier.height(350.dp)) {
          when (selectedTab) {
            0 -> VehiclesTab(
              vehicles = Vehicle.DEFAULT_LIST,
              unlockedIds = unlockedVehicles,
              selectedId = selectedVehicleId,
              currentCash = cash,
              onSelect = { viewModel.buyAndSelectVehicle(it) }
            )
            1 -> StagesTab(
              stages = Stage.DEFAULT_LIST,
              unlockedIds = unlockedStages,
              selectedId = selectedStageId,
              currentCash = cash,
              repository = repo,
              onSelect = { viewModel.buyAndSelectStage(it) }
            )
            2 -> ObstaclesTab(
              obstacles = ObstacleType.DEFAULT_LIST,
              unlockedIds = unlockedObstacles,
              currentCash = cash,
              onBuy = { viewModel.buyObstacle(it) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun VehiclesTab(
  vehicles: List<Vehicle>,
  unlockedIds: Set<String>,
  selectedId: String,
  currentCash: Int,
  onSelect: (Vehicle) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(vehicles) { v ->
      val isUnlocked = unlockedIds.contains(v.id)
      val isSelected = v.id == selectedId
      val canAfford = currentCash >= v.price

      ShopItemCard(
        title = v.name,
        category = v.category,
        subtitle = v.description,
        price = v.price,
        isUnlocked = isUnlocked,
        isSelected = isSelected,
        canAfford = canAfford,
        icon = Icons.Default.DirectionsCar,
        stats = listOf(
          "Masa: ${v.mass.toInt()}kg",
          "Velocidad: x${v.speedMultiplier}",
          "Rebote: ${v.bounce}"
        ),
        onAction = { onSelect(v) }
      )
    }
  }
}

@Composable
private fun StagesTab(
  stages: List<Stage>,
  unlockedIds: Set<String>,
  selectedId: String,
  currentCash: Int,
  repository: GameRepository,
  onSelect: (Stage) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(stages) { s ->
      val isUnlocked = unlockedIds.contains(s.id)
      val isSelected = s.id == selectedId
      val canAfford = currentCash >= s.price
      val highScore = repository.getHighScore(s.id)

      ShopItemCard(
        title = s.name,
        category = "Dificultad: ${s.difficulty}",
        subtitle = s.description,
        price = s.price,
        isUnlocked = isUnlocked,
        isSelected = isSelected,
        canAfford = canAfford,
        icon = Icons.Default.Terrain,
        stats = listOf(
          "Ranuras: ${s.obstacleSlots}",
          "Récord: ${String.format("%,d", highScore)}"
        ),
        onAction = { onSelect(s) }
      )
    }
  }
}

@Composable
private fun ObstaclesTab(
  obstacles: List<ObstacleType>,
  unlockedIds: Set<String>,
  currentCash: Int,
  onBuy: (ObstacleType) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(obstacles) { obs ->
      val isUnlocked = unlockedIds.contains(obs.id)
      val canAfford = currentCash >= obs.price

      ShopItemCard(
        title = obs.displayName,
        category = "Trampa de Pista",
        subtitle = obs.subtitle,
        price = obs.price,
        isUnlocked = isUnlocked,
        isSelected = false,
        canAfford = canAfford,
        icon = Icons.Default.Warning,
        stats = listOf(
          "Bono: +${obs.bonusPoints} pts",
          "Fuerza: ${obs.explosionForce.toInt()}"
        ),
        onAction = { onBuy(obs) }
      )
    }
  }
}

@Composable
private fun ShopItemCard(
  title: String,
  category: String,
  subtitle: String,
  price: Int,
  isUnlocked: Boolean,
  isSelected: Boolean,
  canAfford: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  stats: List<String>,
  onAction: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) BoostBlue.copy(alpha = 0.15f) else DismountSurfaceElevated)
      .border(
        width = 1.dp,
        color = if (isSelected) BoostBlue else if (isUnlocked) DismountBorder else DismountBorder.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp)
      )
      .padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = if (isSelected) BoostBlue else DismountWhite, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = title,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = if (isSelected) BoostBlue else DismountWhite
        )
      }

      Text(
        text = category,
        fontSize = 10.sp,
        color = Color.Gray,
        fontWeight = FontWeight.Medium
      )

      Text(
        text = subtitle,
        fontSize = 10.sp,
        color = Color.LightGray,
        maxLines = 2,
        modifier = Modifier.padding(vertical = 2.dp)
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { stat ->
          Text(
            text = stat,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Button state
    if (isSelected) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(BoostBlue)
          .padding(horizontal = 10.dp, vertical = 8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("EQUIPADO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
      }
    } else if (isUnlocked) {
      Button(
        onClick = onAction,
        colors = ButtonDefaults.buttonColors(containerColor = DismountWhite),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(34.dp)
      ) {
        Text("EQUIPAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DismountBackground)
      }
    } else {
      Button(
        onClick = onAction,
        enabled = canAfford,
        colors = ButtonDefaults.buttonColors(
          containerColor = CashGold,
          disabledContainerColor = DismountBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(34.dp)
      ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = if (canAfford) Color.Black else Color.Gray, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "$$price",
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = if (canAfford) Color.Black else Color.Gray
        )
      }
    }
  }
}
