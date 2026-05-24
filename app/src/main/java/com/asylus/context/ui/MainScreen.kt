package com.asylus.context.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.asylus.context.data.model.Recording
import com.asylus.context.ui.theme.CardBg
import com.asylus.context.ui.theme.CardBorder
import com.asylus.context.ui.theme.DeepBg
import com.asylus.context.ui.theme.GlowRed
import com.asylus.context.ui.theme.GlowRedDim
import com.asylus.context.ui.theme.GlowRedRipple
import com.asylus.context.ui.theme.SurfaceBg
import com.asylus.context.ui.theme.TextLight
import com.asylus.context.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isRecording by viewModel.isRecording.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val amplitudeScale by viewModel.amplitudeScale.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()

    // Request Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.toggleRecording()
            } else {
                Toast.makeText(context, "Microphone permission is required to record audio", Toast.LENGTH_LONG).show()
            }
        }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceBg,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.width(320.dp)
            ) {
                DrawerContent(
                    recordings = recordings,
                    currentlyPlaying = currentlyPlaying,
                    onPlayClick = { viewModel.playRecording(it) },
                    onDeleteClick = { viewModel.deleteRecording(it) },
                    onCloseClick = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Context",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Drawer",
                                tint = TextLight
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceBg
                    )
                )
            },
            containerColor = DeepBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(SurfaceBg, DeepBg)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Recording visualizer / Pulsating record button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(280.dp)
                    ) {
                        if (isRecording) {
                            // Semi-transparent background wave ripples
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse_ripple")
                            val rippleScale1 by infiniteTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 1.8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "ripple1"
                            )
                            val rippleAlpha1 by infiniteTransition.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 0.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "alpha1"
                            )

                            val rippleScale2 by infiniteTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 1.8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart,
                                    initialStartOffset = StartOffset(600)
                                ),
                                label = "ripple2"
                            )
                            val rippleAlpha2 by infiniteTransition.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 0.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart,
                                    initialStartOffset = StartOffset(600)
                                ),
                                label = "alpha2"
                            )

                            // Render ripple circles
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(rippleScale1)
                                    .background(GlowRedDim.copy(alpha = rippleAlpha1), shape = CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(rippleScale2)
                                    .background(GlowRedDim.copy(alpha = rippleAlpha2), shape = CircleShape)
                            )

                            // Live amplitude reactive circle
                            val animatedAmplitude by animateFloatAsState(
                                targetValue = amplitudeScale,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "reactiveScale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .scale(animatedAmplitude)
                                    .background(GlowRedRipple, shape = CircleShape)
                            )
                        }

                        // Main Red Recording Button
                        val buttonScale by animateFloatAsState(
                            targetValue = if (isRecording) 0.9f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "buttonPress"
                        )
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(buttonScale)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFF5E62), GlowRed)
                                    )
                                )
                                .clickable {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        viewModel.toggleRecording()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Chronometer below button
                    if (isRecording) {
                        Text(
                            text = elapsedTime,
                            color = GlowRed,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "RECORDING IN PROGRESS",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Provide a small hint helper label when not recording
                        Text(
                            text = "Tap to Record",
                            color = TextMuted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    recordings: List<Recording>,
    currentlyPlaying: Recording?,
    onPlayClick: (Recording) -> Unit,
    onDeleteClick: (Recording) -> Unit,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Context",
                    color = TextLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "LHS Navigation",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Drawer",
                    tint = TextMuted
                )
            }
        }

        HorizontalDivider(color = CardBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Recordings",
            color = TextLight,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recordings found.\nTap the microphone button to start recording.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recordings, key = { it.file.absolutePath }) { recording ->
                    val isPlaying = currentlyPlaying == recording
                    RecordingItem(
                        recording = recording,
                        isPlaying = isPlaying,
                        onPlayClick = { onPlayClick(recording) },
                        onDeleteClick = { onDeleteClick(recording) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingItem(
    recording: Recording,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardBg = if (isPlaying) GlowRedDim else CardBg
    val cardBorderColor = if (isPlaying) GlowRed else CardBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp))
            .clickable { onPlayClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isPlaying) GlowRed else CardBorder, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop Playback" else "Play Recording",
                tint = if (isPlaying) Color.White else TextLight,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info container
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = recording.displayName,
                color = TextLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = recording.durationFormatted,
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        // Delete Button
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Recording",
                tint = TextMuted
            )
        }
    }
}
