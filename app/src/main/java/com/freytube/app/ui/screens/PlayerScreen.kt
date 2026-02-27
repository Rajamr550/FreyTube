package com.freytube.app.ui.screens

import android.text.Html
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.freytube.app.data.model.PipedStream
import com.freytube.app.ui.components.VideoCard
import com.freytube.app.ui.theme.*
import com.freytube.app.viewmodel.PlayerViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoId: String,
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    var showDescription by remember { mutableStateOf(false) }

    LaunchedEffect(videoId) {
        playerViewModel.initializePlayer()
        playerViewModel.loadVideo(videoId)
    }

    DisposableEffect(Unit) {
        onDispose {
            // Don't release player on dispose to allow background play
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Video Player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                playerViewModel.player?.let { exoPlayer ->
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                player = exoPlayer
                                useController = true
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Back button overlay
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(4.dp)
                )
            }
        }

        // Action buttons row
        uiState.videoStream?.let { stream ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Background Play
                ActionButton(
                    icon = if (uiState.isBackgroundPlaying) Icons.Filled.MusicNote else Icons.Outlined.MusicNote,
                    label = "Background",
                    isActive = uiState.isBackgroundPlaying,
                    onClick = {
                        if (uiState.isBackgroundPlaying) {
                            playerViewModel.stopBackgroundPlay()
                        } else {
                            playerViewModel.startBackgroundPlay()
                        }
                    }
                )

                // Download
                ActionButton(
                    icon = Icons.Outlined.Download,
                    label = "Download",
                    onClick = { playerViewModel.showDownloadDialog() }
                )

                // Quality
                ActionButton(
                    icon = Icons.Outlined.HighQuality,
                    label = uiState.selectedQuality,
                    onClick = { playerViewModel.showQualityDialog() }
                )

                // Speed
                ActionButton(
                    icon = Icons.Outlined.Speed,
                    label = "${uiState.playbackSpeed}x",
                    onClick = { playerViewModel.showSpeedDialog() }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Video info + related videos
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            uiState.videoStream?.let { stream ->
                // Title
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = stream.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stream.formattedViews,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stream.uploadDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Likes
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.ThumbUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stream.formattedLikes,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (stream.dislikes >= 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.ThumbDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${stream.dislikes}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Channel info
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val channelId = stream.uploaderUrl.removePrefix("/channel/")
                                onChannelClick(channelId)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = stream.uploaderAvatar,
                            contentDescription = stream.uploader,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stream.uploader,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (stream.uploaderVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Verified,
                                        contentDescription = "Verified",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = stream.formattedSubscribers,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Description
                item {
                    Column(
                        modifier = Modifier
                            .clickable { showDescription = !showDescription }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                if (showDescription) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        AnimatedVisibility(visible = showDescription) {
                            Text(
                                text = stream.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Comments section
                if (uiState.comments.isNotEmpty()) {
                    item {
                        Text(
                            text = "Comments (${uiState.comments.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                        )
                    }

                    items(uiState.comments.take(5)) { comment ->
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            AsyncImage(
                                model = comment.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.author,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = comment.commentedTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (comment.pinned) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Filled.PushPin,
                                            contentDescription = "Pinned",
                                            modifier = Modifier.size(12.dp),
                                            tint = Primary
                                        )
                                    }
                                }
                                Text(
                                    text = comment.commentText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.ThumbUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${comment.likeCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (comment.hearted) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = "Hearted",
                                            modifier = Modifier.size(14.dp),
                                            tint = Primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Related Videos
                if (uiState.relatedVideos.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Text(
                            text = "Related Videos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(
                        items = uiState.relatedVideos,
                        key = { it.url }
                    ) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onVideoClick(video.videoId) },
                            onChannelClick = {
                                val channelId = video.uploaderUrl.removePrefix("/channel/")
                                onChannelClick(channelId)
                            },
                            compact = true
                        )
                    }
                }
            }

            // Error state
            if (uiState.error != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.error ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { playerViewModel.loadVideo(videoId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }

    // Quality Selection Dialog
    if (uiState.showQualityDialog) {
        QualityDialog(
            qualities = uiState.availableQualities,
            selectedQuality = uiState.selectedQuality,
            onQualitySelected = { playerViewModel.selectQuality(it) },
            onDismiss = { playerViewModel.dismissQualityDialog() }
        )
    }

    // Speed Selection Dialog
    if (uiState.showSpeedDialog) {
        SpeedDialog(
            currentSpeed = uiState.playbackSpeed,
            onSpeedSelected = { playerViewModel.setPlaybackSpeed(it) },
            onDismiss = { playerViewModel.dismissSpeedDialog() }
        )
    }

    // Download Dialog
    if (uiState.showDownloadDialog) {
        DownloadDialog(
            videoStream = uiState.videoStream,
            onDownload = { stream, isAudio -> playerViewModel.downloadVideo(stream, isAudio) },
            onDismiss = { playerViewModel.dismissDownloadDialog() }
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = if (isActive) Primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QualityDialog(
    qualities: List<String>,
    selectedQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Video Quality", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn {
                items(qualities) { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(quality) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = quality == selectedQuality,
                            onClick = { onQualitySelected(quality) },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = quality,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (quality.contains("2160") || quality.contains("4320")) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Primary
                            ) {
                                Text(
                                    text = if (quality.contains("4320")) "8K" else "4K",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Primary)
            }
        }
    )
}

@Composable
private fun SpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Playback Speed", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn {
                items(speeds) { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = speed == currentSpeed,
                            onClick = { onSpeedSelected(speed) },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (speed == 1.0f) FontWeight.Bold else FontWeight.Normal
                        )
                        if (speed == 1.0f) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Normal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Primary)
            }
        }
    )
}

@Composable
private fun DownloadDialog(
    videoStream: com.freytube.app.data.model.VideoStream?,
    onDownload: (PipedStream, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val stream = videoStream ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Download", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn {
                // Audio streams
                item {
                    Text(
                        text = "🎵 Audio Only",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(stream.audioStreams.take(3)) { audioStream ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDownload(audioStream, true) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${audioStream.quality ?: "Audio"} - ${audioStream.format}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = audioStream.formattedSize,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Video streams
                item {
                    Text(
                        text = "🎬 Video",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(stream.sortedVideoStreams) { vidStream ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDownload(vidStream, false) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.VideoFile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${vidStream.qualityLabel} - ${vidStream.format}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = vidStream.formattedSize,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (vidStream.isHighRes) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (vidStream.is4K) Primary else Secondary
                            ) {
                                Text(
                                    text = if (vidStream.is8K) "8K" else if (vidStream.is4K) "4K" else "HD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Primary)
            }
        }
    )
}
