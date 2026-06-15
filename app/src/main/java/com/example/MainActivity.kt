package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AppNavigator
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.InventoryViewModel
import com.example.ui.viewmodels.AppScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Register shortcuts
    setupShortcuts()

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val vm: InventoryViewModel = viewModel()
          
          LaunchedEffect(intent) {
            val shortcutType = intent?.getStringExtra("shortcut_type")
            if (shortcutType == "scan") {
              if (vm.isLoggedIn()) {
                vm.navigateTo(AppScreen.Scan)
              }
            } else if (shortcutType == "dashboard") {
              if (vm.isLoggedIn()) {
                vm.navigateTo(AppScreen.Dashboard)
              }
            }
          }

          AppNavigator(viewModel = vm)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  private fun setupShortcuts() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
      try {
        val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
        if (shortcutManager != null) {
          val scanIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("shortcut_type", "scan")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          }
          val scanShortcut = android.content.pm.ShortcutInfo.Builder(this, "shortcut_scan")
            .setShortLabel("Scanner QR")
            .setLongLabel("Lancer le scanner QR Code")
            .setIcon(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_camera))
            .setIntent(scanIntent)
            .build()

          val dashIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("shortcut_type", "dashboard")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          }
          val dashShortcut = android.content.pm.ShortcutInfo.Builder(this, "shortcut_dashboard")
            .setShortLabel("Équipements")
            .setLongLabel("Consulter les fiches équipements")
            .setIcon(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_sort_by_size))
            .setIntent(dashIntent)
            .build()

          shortcutManager.dynamicShortcuts = listOf(scanShortcut, dashShortcut)
        }
      } catch (e: Exception) {
        Log.e("MainActivity", "Failed to setup shortcuts: ${e.message}")
      }
    }
  }
}

