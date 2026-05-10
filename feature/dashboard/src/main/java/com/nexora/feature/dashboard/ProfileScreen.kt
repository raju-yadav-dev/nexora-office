package com.nexora.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.core.designsystem.component.NexoraCard
import com.nexora.core.designsystem.component.NexoraGradientBackground
import com.nexora.core.designsystem.component.NexoraIconBadge
import com.nexora.core.designsystem.theme.NexoraError
import com.nexora.core.designsystem.theme.NexoraPrimary
import com.nexora.core.designsystem.theme.NexoraPrimaryVariant
import com.nexora.core.designsystem.theme.NexoraSecondary

@Composable
fun ProfileScreen() {
    var darkModePreview by remember { mutableStateOf(true) }
    var selectedMenu by remember { mutableStateOf("Cloud Storage") }
    val menuItems = listOf(
        ProfileMenuItem("Cloud Storage", "Pro sync active", "C", NexoraSecondary),
        ProfileMenuItem("My Templates", "18 saved", "T", NexoraPrimary),
        ProfileMenuItem("My Documents", "128 files", "D", Color(0xFF2563EB)),
        ProfileMenuItem("Recycle Bin", "3 items", "R", NexoraError),
        ProfileMenuItem("Settings", "Workspace preferences", "S", MaterialTheme.colorScheme.onSurfaceVariant),
        ProfileMenuItem("Help & Feedback", "Support center", "?", NexoraPrimaryVariant),
        ProfileMenuItem("About Nexora Office", "Version 1.0.0", "i", MaterialTheme.colorScheme.onSurfaceVariant)
    )

    NexoraGradientBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileHeader()
            }

            item {
                ProAccessCard()
            }

            item {
                StorageCard()
            }

            item {
                NexoraCard(contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NexoraIconBadge(label = "UI", color = NexoraPrimary, size = 34.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dark theme",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Workspace preview",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = darkModePreview, onCheckedChange = { darkModePreview = it })
                    }
                }
            }

            items(menuItems.size) { index ->
                val item = menuItems[index]
                ProfileMenuRow(
                    item = item,
                    selected = selectedMenu == item.title,
                    onClick = { selectedMenu = item.title }
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(NexoraSecondary, NexoraPrimary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Nexora User",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(NexoraPrimary.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("Pro", color = NexoraPrimary, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                text = "nexora.user@email.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProAccessCard() {
    NexoraCard(
        color = NexoraPrimary.copy(alpha = 0.2f),
        contentPadding = 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexoraIconBadge(label = "Pro", color = NexoraPrimaryVariant, size = 44.dp)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pro access active",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Every user gets all workspace features for now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("On", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun StorageCard() {
    NexoraCard(contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Cloud Storage",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "2.45 GB / Pro",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { 0.245f },
            color = NexoraSecondary,
            trackColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    }
}

@Composable
private fun ProfileMenuRow(
    item: ProfileMenuItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    NexoraCard(
        contentPadding = 12.dp,
        color = if (selected) {
            item.color.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexoraIconBadge(label = item.badge, color = item.color, size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (selected) "Open" else ">",
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private data class ProfileMenuItem(
    val title: String,
    val detail: String,
    val badge: String,
    val color: Color
)
