package com.freytube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.freytube.app.data.local.DownloadEntity
import com.freytube.app.data.local.DownloadStatus
import com.freytube.app.ui.theme.*
import com.freytube.app.viewmodel.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onVideoClick: (String) -> Unit,
    downloadViewModel: DownloadViewModel = viewModel()
) {
    val downloads by downloadViewModel.downloads.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        "Downloads",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${downloads.size} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            actions = {
                Icon(
                    Icons.Filled.FolderOpen,
                    contentDescription = "Downloads folder",
                    tint = Primary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        )

        if (downloads.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Download videos to watch offline\nTap the download button on any video",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = downloads,
                    key = { it.videoId }
                ) { download ->
                    DownloadItem(
                        download = download,
                        onPlayClick = { onVideoClick(download.videoId) },
                        onDeleteClick = { downloadViewModel.deleteDownload(download) },
                        onRetryClick = { downloadViewModel.retryDownload(download) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    download: DownloadEntity,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(enabled = download.status == DownloadStatus.COMPLETED) { onPlayClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = download.thumbnail,
                    contentDescription = download.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Status overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (download.status) {
                                DownloadStatus.COMPLETED -> Color.Transparent
                                DownloadStatus.DOWNLOADING -> Color.Black.copy(alpha = 0.3f)
                                else -> Color.Black.copy(alpha = 0.5f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (download.status) {
                        DownloadStatus.COMPLETED -> {
                            Icon(
                                Icons.Filled.PlayCircleFilled,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        DownloadStatus.DOWNLOADING -> {
                            CircularProgressIndicator(
                                progress = { download.downloadProgress / 100f },
                                modifier = Modifier.size(32.dp),
                                color = Primary,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                        DownloadStatus.FAILED -> {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = "Failed",
                                tint = Error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        else -> {}
                    }
                }

                // Quality badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = download.quality,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = download.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = download.uploader,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        LinearProgressIndicator(
                            progress = { download.downloadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Primary,
                        )
                        Text(
                            text = "${download.downloadProgress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }
                    DownloadStatus.COMPLETED -> {
                        val size = when {
                            download.fileSize >= 1_073_741_824 -> String.format("%.1f GB", download.fileSize / 1_073_741_824.0)
                            download.fileSize >= 1_048_576 -> String.format("%.1f MB", download.fileSize / 1_048_576.0)
                            else -> String.format("%.1f KB", download.fileSize / 1_024.0)
                        }
                        Text(
                            text = "✓ $size",
                            style = MaterialTheme.typography.labelSmall,
                            color = Success
                        )
                    }
                    DownloadStatus.FAILED -> {
                        Text(
                            text = "Download failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Error
                        )
                    }
                    else -> {}
                }
            }

            // Actions
            Column {
                if (download.status == DownloadStatus.FAILED) {
                    IconButton(onClick = onRetryClick) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Retry",
                            tint = Primary
                        )
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Download?") },
            text = { Text("This will delete the downloaded file from your device.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
