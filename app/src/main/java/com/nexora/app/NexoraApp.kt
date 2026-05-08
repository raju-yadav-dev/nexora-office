package com.nexora.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.nexora.core.navigation.NexoraNavHost

@Composable
fun NexoraApp() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        NexoraNavHost(navController = navController)
    }
}
