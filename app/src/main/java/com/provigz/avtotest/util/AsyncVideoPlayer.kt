package com.provigz.avtotest.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

@Composable
fun AsyncVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    autoPlay: Boolean = false,
    allowPlay: Boolean = true,
    onPlay: () -> Unit = {},
    onFinish: () -> Unit = {}
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(videoUrl) {
        val mediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = autoPlay
    }

    var isPlaying by remember { mutableStateOf(autoPlay) }
    var wasPlayedOnce by remember { mutableStateOf(autoPlay) }
    var playerSize by remember { mutableStateOf(IntSize.Zero) }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
                if (isPlaying) {
                    wasPlayedOnce = true
                    onPlay()
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onFinish()
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier.onSizeChanged {
            playerSize = it
        }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { playerSize.height.toDp() })
        )

        if (!isPlaying) {
            if (!thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Изображение",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(
                            with(LocalDensity.current) { playerSize.width.toDp() },
                            with(LocalDensity.current) { playerSize.height.toDp() }
                        )
                )
            }
            if (allowPlay) {
                IconButton(
                    onClick = {
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = true
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (wasPlayedOnce) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = "Пусни",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}