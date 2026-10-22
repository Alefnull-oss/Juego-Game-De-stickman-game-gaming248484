package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.GameRepository
import com.example.ui.DismountMainScreen
import com.example.ui.theme.DismountBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

  private lateinit var viewModel: GameViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val repository = GameRepository(applicationContext)
    val factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(applicationContext, repository) as T
      }
    }
    viewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = DismountBackground
        ) {
          DismountMainScreen(viewModel = viewModel)
        }
      }
    }
  }
}
