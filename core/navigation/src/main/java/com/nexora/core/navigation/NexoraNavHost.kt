package com.nexora.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile
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
                onOpenEditor = { file ->
                    navController.navigate(NexoraDestination.Editor.createRoute(file))
                }
            )
        }

        composable(NexoraDestination.FileManager.route) {
            FileManagerScreen(onOpenFile = {
                navController.navigate(NexoraDestination.Editor.createRoute(it))
            })
        }

        composable(NexoraDestination.Tools.route) {
            ToolsScreen(onOpenEditor = { file ->
                navController.navigate(NexoraDestination.Editor.createRoute(file))
            })
        }

        composable(NexoraDestination.Templates.route) {
            TemplatesScreen(onOpenTemplate = { file ->
                navController.navigate(NexoraDestination.Editor.createRoute(file))
            })
        }

        composable(NexoraDestination.Profile.route) {
            ProfileScreen()
        }

        composable(
            route = NexoraDestination.Editor.route,
            arguments = listOf(
                navArgument(NexoraDestination.Editor.fileIdArg) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(NexoraDestination.Editor.titleArg) {
                    type = NavType.StringType
                    defaultValue = "Untitled Document.docx"
                },
                navArgument(NexoraDestination.Editor.typeArg) {
                    type = NavType.StringType
                    defaultValue = DocumentType.DOC.name
                },
                navArgument(NexoraDestination.Editor.pathArg) {
                    type = NavType.StringType
                    defaultValue = "nexora://workspace/new"
                }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments
                ?.getString(NexoraDestination.Editor.titleArg)
                ?.let(Uri::decode)
                ?.ifBlank { "Untitled Document.docx" }
                ?: "Untitled Document.docx"
            val fileId = backStackEntry.arguments
                ?.getString(NexoraDestination.Editor.fileIdArg)
                ?.let(Uri::decode)
                ?.ifBlank { title }
                ?: title
            val path = backStackEntry.arguments
                ?.getString(NexoraDestination.Editor.pathArg)
                ?.let(Uri::decode)
                ?.ifBlank { "nexora://workspace/new" }
                ?: "nexora://workspace/new"
            val type = backStackEntry.arguments
                ?.getString(NexoraDestination.Editor.typeArg)
                ?.let { runCatching { DocumentType.valueOf(it) }.getOrDefault(DocumentType.DOC) }
                ?: DocumentType.DOC

            EditorScreen(
                openedFile = WorkspaceFile(
                    id = fileId,
                    name = title,
                    path = path,
                    type = type
                ),
                onDone = {
                    navController.popBackStack()
                }
            )
        }
    }
}
