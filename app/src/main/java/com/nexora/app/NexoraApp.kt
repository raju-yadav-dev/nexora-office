package com.nexora.app

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexora.core.designsystem.component.NexoraLogoMark
import com.nexora.core.model.WorkspaceFile
import com.nexora.core.navigation.NexoraDestination
import com.nexora.core.navigation.NexoraNavHost
import kotlin.math.abs

@Composable
fun NexoraApp(
    externalOpenFile: WorkspaceFile? = null,
    externalOpenError: String? = null,
    onExternalOpenConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NexoraDestination.Dashboard.route
    val showBottomBar = topLevelItems.any { it.route == currentRoute }
    var dragTotal by remember(currentRoute) { mutableStateOf(0f) }
    val swipeTabsModifier = if (showBottomBar) {
        Modifier.pointerInput(currentRoute) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount ->
                    dragTotal += dragAmount
                },
                onDragEnd = {
                    val currentIndex = topLevelItems.indexOfFirst { it.route == currentRoute }
                    if (currentIndex >= 0 && abs(dragTotal) > 96f) {
                        val nextIndex = if (dragTotal < 0f) currentIndex + 1 else currentIndex - 1
                        topLevelItems.getOrNull(nextIndex)?.let { item ->
                            navController.navigateTopLevel(item.route)
                        }
                    }
                    dragTotal = 0f
                },
                onDragCancel = {
                    dragTotal = 0f
                }
            )
        }
    } else {
        Modifier
    }

    LaunchedEffect(externalOpenFile?.id, externalOpenError) {
        externalOpenFile?.let { file ->
            navController.navigate(NexoraDestination.Editor.createRoute(file)) {
                launchSingleTop = true
            }
            onExternalOpenConsumed()
        }
        if (externalOpenError != null) {
            navController.navigate(NexoraDestination.FileManager.route) {
                launchSingleTop = true
            }
            onExternalOpenConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NexoraBottomBar(
                    navController = navController,
                    items = topLevelItems,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        NexoraNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .then(swipeTabsModifier)
        )
    }
}

@Composable
private fun NexoraBottomBar(
    navController: NavHostController,
    items: List<TopLevelItem>,
    currentRoute: String
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 10.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigateTopLevel(item.route)
                },
                icon = {
                    NexoraNavGlyph(label = item.glyph, selected = selected)
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun NexoraNavGlyph(
    label: String,
    selected: Boolean
) {
    if (label == "Logo") {
        NexoraLogoMark(size = 24.dp)
        return
    }

    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private val topLevelItems = listOf(
    TopLevelItem("Home", "Logo", NexoraDestination.Dashboard.route),
    TopLevelItem("Files", "F", NexoraDestination.FileManager.route),
    TopLevelItem("Tools", "T", NexoraDestination.Tools.route),
    TopLevelItem("Templates", "TP", NexoraDestination.Templates.route),
    TopLevelItem("Me", "M", NexoraDestination.Profile.route)
)

private data class TopLevelItem(
    val label: String,
    val glyph: String,
    val route: String
)

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
