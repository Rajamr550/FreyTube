package com.freytube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freytube.app.data.local.SettingsStore
import com.freytube.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()

    val darkMode by settingsStore.darkMode.collectAsState(initial = true)
    val backgroundPlay by settingsStore.backgroundPlay.collectAsState(initial = true)
    val sponsorBlock by settingsStore.sponsorBlockEnabled.collectAsState(initial = true)
    val defaultQuality by settingsStore.defaultQuality.collectAsState(initial = "720p")
    val pipEnabled by settingsStore.pipEnabled.collectAsState(initial = true)
    val autoPlay by settingsStore.autoPlay.collectAsState(initial = true)
    val playbackSpeed by settingsStore.playbackSpeed.collectAsState(initial = 1.0f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text("Settings", fontWeight = FontWeight.Bold)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ═══════════════════════════════════════════
            // Playback Section
            // ═══════════════════════════════════════════
            item {
                SettingsSection(title = "Playback")
            }

            item {
                SettingsSwitch(
                    icon = Icons.Outlined.MusicNote,
                    title = "Background Play",
                    subtitle = "Continue playing audio when app is minimized",
                    checked = backgroundPlay,
                    onCheckedChange = {
                        scope.launch {
                            settingsStore.setSetting(SettingsStore.BACKGROUND_PLAY, it)
                        }
                    }
                )
            }

            item {
                SettingsSwitch(
                    icon = Icons.Outlined.PictureInPicture,
                    title = "Picture-in-Picture",
                    subtitle = "Show floating video player when leaving the app",
                    checked = pipEnabled,
                    onCheckedChange = {
                        scope.launch {
                            settingsStore.setSetting(SettingsStore.PIP_ENABLED, it)
                        }
                    }
                )
            }

            item {
                SettingsSwitch(
                    icon = Icons.Outlined.PlayCircle,
                    title = "Auto-play",
                    subtitle = "Automatically play next related video",
                    checked = autoPlay,
                    onCheckedChange = {
                        scope.launch {
                            settingsStore.setSetting(SettingsStore.AUTO_PLAY, it)
                        }
                    }
                )
            }

            item {
                var showQualityMenu by remember { mutableStateOf(false) }
                val qualities = listOf("360p", "480p", "720p", "1080p", "1440p", "2160p")

                SettingsItem(
                    icon = Icons.Outlined.HighQuality,
                    title = "Default Quality",
                    subtitle = defaultQuality,
                    onClick = { showQualityMenu = true }
                )

                DropdownMenu(
                    expanded = showQualityMenu,
                    onDismissRequest = { showQualityMenu = false }
                ) {
                    qualities.forEach { quality ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(quality)
                                    if (quality == defaultQuality) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                scope.launch {
                                    settingsStore.setSetting(SettingsStore.DEFAULT_QUALITY, quality)
                                }
                                showQualityMenu = false
                            }
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════
            // Ad Blocking Section
            // ═══════════════════════════════════════════
            item {
                SettingsSection(title = "Content & Privacy")
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Shield,
                    title = "Privacy Mode",
                    subtitle = "Content served via privacy-respecting APIs",
                    onClick = {}
                )
            }

            item {
                SettingsSwitch(
                    icon = Icons.Outlined.SkipNext,
                    title = "SponsorBlock",
                    subtitle = "Skip sponsored segments automatically",
                    checked = sponsorBlock,
                    onCheckedChange = {
                        scope.launch {
                            settingsStore.setSetting(SettingsStore.SPONSORBLOCK_ENABLED, it)
                        }
                    }
                )
            }

            // ═══════════════════════════════════════════
            // Appearance Section
            // ═══════════════════════════════════════════
            item {
                SettingsSection(title = "Appearance")
            }

            item {
                SettingsSwitch(
                    icon = Icons.Outlined.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme throughout the app",
                    checked = darkMode,
                    onCheckedChange = {
                        scope.launch {
                            settingsStore.setSetting(SettingsStore.DARK_MODE, it)
                        }
                    }
                )
            }

            // ═══════════════════════════════════════════
            // Downloads Section
            // ═══════════════════════════════════════════
            item {
                SettingsSection(title = "Downloads")
            }

            item {
                var showDownloadQuality by remember { mutableStateOf(false) }
                val qualities = listOf("360p", "480p", "720p", "1080p", "Best Available")

                SettingsItem(
                    icon = Icons.Outlined.Download,
                    title = "Download Quality",
                    subtitle = "Preferred quality for downloads",
                    onClick = { showDownloadQuality = true }
                )

                DropdownMenu(
                    expanded = showDownloadQuality,
                    onDismissRequest = { showDownloadQuality = false }
                ) {
                    qualities.forEach { quality ->
                        DropdownMenuItem(
                            text = { Text(quality) },
                            onClick = {
                                scope.launch {
                                    settingsStore.setSetting(SettingsStore.DOWNLOAD_QUALITY, quality)
                                }
                                showDownloadQuality = false
                            }
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════
            // About Section
            // ═══════════════════════════════════════════
            item {
                SettingsSection(title = "About")
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "FreyTube",
                    subtitle = "Version 1.0.0 • Open Source Video Client",
                    onClick = {}
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Code,
                    title = "Source Code",
                    subtitle = "github.com/AiCodeCraft/FreyTube",
                    onClick = {}
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Shield,
                    title = "Privacy",
                    subtitle = "No data collection • No tracking • No ads",
                    onClick = {}
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Primary.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 FreyTube",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            text = "Background Play • Downloads • Open Source",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Powered by Piped API",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = Primary.copy(alpha = 0.3f)
            )
        )
    }
}
