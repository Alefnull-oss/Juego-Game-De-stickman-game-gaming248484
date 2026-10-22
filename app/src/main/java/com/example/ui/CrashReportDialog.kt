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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.CrashReport
import com.example.ui.theme.BoostBlue
import com.example.ui.theme.CashGold
import com.example.ui.theme.DismountBackground
import com.example.ui.theme.DismountBorder
import com.example.ui.theme.DismountSurface
import com.example.ui.theme.DismountSurfaceElevated
import com.example.ui.theme.DismountWhite
import com.example.ui.theme.FractureRed
import com.example.ui.theme.HazardOrange

@Composable
fun CrashReportDialog(
  report: CrashReport,
  onRetry: () -> Unit,
  onOpenShop: () -> Unit,
  onInspectXRay: () -> Unit
) {
  val rating = when {
    report.totalFractures >= 10 -> "¡DESTRUCCIÓN TOTAL DEL ESQUELETO!"
    report.totalFractures >= 6 -> "¡TRAUMATISMO SEVERO MÚLTIPLE!"
    report.totalFractures >= 3 -> "¡IMPACTO MODERADO CON FRACTURAS!"
    report.totalFractures >= 1 -> "LESIONES LEVES REGISTRADAS"
    else -> "DESMONTE SIN FRACTURAS GRAVES"
  }

  Dialog(onDismissRequest = onRetry) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.5.dp, DismountBorder, RoundedCornerShape(16.dp))
        .testTag("crash_report_dialog"),
      colors = CardDefaults.cardColors(containerColor = DismountSurface)
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // High score badge
        if (report.isNewHighScore) {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(CashGold.copy(alpha = 0.2f))
              .border(1.dp, CashGold, RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CashGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("¡NUEVO RÉCORD DE PISTA!", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CashGold)
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        // Title
        Text(
          text = "INFORME DE CHOQUE",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.Gray,
          letterSpacing = 1.sp
        )
        Text(
          text = rating,
          fontSize = 14.sp,
          fontWeight = FontWeight.ExtraBold,
          color = if (report.totalFractures >= 6) FractureRed else HazardOrange,
          modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Total Score and Cash Earned Card
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DismountBackground)
            .border(1.dp, DismountBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "PUNTUACIÓN TOTAL", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(
              text = String.format("%,d", report.totalScore),
              fontSize = 20.sp,
              fontWeight = FontWeight.Black,
              color = DismountWhite,
              fontFamily = FontFamily.Monospace
            )
          }

          Box(
            modifier = Modifier
              .width(1.dp)
              .height(34.dp)
              .background(DismountBorder)
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "DINERO GANADO", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = CashGold, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "+$${report.cashEarned}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = CashGold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics Breakdown Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          MetricChip(
            label = "FRACTURAS",
            value = "${report.totalFractures} / 13",
            color = if (report.totalFractures > 0) FractureRed else Color.LightGray,
            modifier = Modifier.weight(1f)
          )
          MetricChip(
            label = "VOLTERETAS",
            value = "x${report.somersaultsCount}",
            color = BoostBlue,
            modifier = Modifier.weight(1f)
          )
          MetricChip(
            label = "TIEMPO AIRE",
            value = "${String.format("%.1f", report.airTimeSeconds)}s",
            color = HazardOrange,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Broken Bones List Preview
        val brokenList = report.brokenBones.filter { it.isBroken }
        if (brokenList.isNotEmpty()) {
          Text(
            text = "HUESOS FRACTURADOS:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.Start)
          )
          Spacer(modifier = Modifier.height(4.dp))
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(100.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            items(brokenList) { b ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(FractureRed.copy(alpha = 0.12f))
                  .border(0.5.dp, FractureRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(b.type.clinicalName, fontSize = 11.sp, color = DismountWhite)
                Text("+${b.type.pointValue}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CashGold, fontFamily = FontFamily.Monospace)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons
        Button(
          onClick = onRetry,
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("report_retry_button"),
          colors = ButtonDefaults.buttonColors(containerColor = DismountWhite),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, tint = DismountBackground)
          Spacer(modifier = Modifier.width(6.dp))
          Text("VOLVER A INTENTAR", fontWeight = FontWeight.ExtraBold, color = DismountBackground)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onInspectXRay,
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("report_xray_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DismountWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, DismountBorder),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = FractureRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("VER RAYOS X", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onOpenShop,
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("report_shop_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CashGold),
            border = androidx.compose.foundation.BorderStroke(1.dp, CashGold.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = CashGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("TIENDA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun MetricChip(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(DismountSurfaceElevated)
      .border(1.dp, DismountBorder, RoundedCornerShape(8.dp))
      .padding(vertical = 6.dp, horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
      Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color, fontFamily = FontFamily.Monospace)
    }
  }
}
