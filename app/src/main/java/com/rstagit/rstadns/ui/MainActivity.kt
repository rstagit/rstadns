package com.rstagit.rstadns.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rstagit.rstadns.ui.screens.DnsScreen
import com.rstagit.rstadns.ui.theme.RstaDnsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RstaDnsTheme {
                val vm: DnsViewModel = viewModel(
                    factory = DnsViewModel.Factory(applicationContext)
                )
                DnsScreen(viewModel = vm)
            }
        }
    }
}
