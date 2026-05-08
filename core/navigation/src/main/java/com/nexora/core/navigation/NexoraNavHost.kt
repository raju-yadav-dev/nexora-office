package com.nexora.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nexora.feature.dashboard.DashboardScreen
import com.nexora.feature.editor.EditorScreen
import com.nexora.feature.filemanager.FileManagerScreen

/**
 * Central app navigation graph.
 *
 * Feature modules expose screens, while routing remains centralized
 * to preserve clear cross-feature navigation policies.
 */
@Composable
fun NexoraNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = NexoraDestination.Dashboard.route
    ) {
        composable(NexoraDestination.Dashboard.route) {
            DashboardScreen(
                onOpenFileManager = {
                    navController.navigate(NexoraDestination.FileManager.route)
                },
                onOpenEditor = {
                    navController.navigate(NexoraDestination.Editor.route)
                }
            )
        }

        composable(NexoraDestination.FileManager.route) {
            FileManagerScreen(onOpenFile = {
                navController.navigate(NexoraDestination.Editor.route)
            })
        }

        composable(NexoraDestination.Editor.route) {
            EditorScreen()
        }
    }
}
