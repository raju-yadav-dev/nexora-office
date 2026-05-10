package com.nexora.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nexora.feature.dashboard.DashboardScreen
import com.nexora.feature.dashboard.ProfileScreen
import com.nexora.feature.dashboard.TemplatesScreen
import com.nexora.feature.dashboard.ToolsScreen
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
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NexoraDestination.Dashboard.route,
        modifier = modifier
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

        composable(NexoraDestination.Tools.route) {
            ToolsScreen(onOpenEditor = {
                navController.navigate(NexoraDestination.Editor.route)
            })
        }

        composable(NexoraDestination.Templates.route) {
            TemplatesScreen(onOpenTemplate = {
                navController.navigate(NexoraDestination.Editor.route)
            })
        }

        composable(NexoraDestination.Profile.route) {
            ProfileScreen()
        }

        composable(NexoraDestination.Editor.route) {
            EditorScreen()
        }
    }
}
