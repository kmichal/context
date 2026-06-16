package com.asylus.context.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.asylus.context.R
import com.asylus.context.ui.MainViewModel
import com.asylus.context.ui.components.DrawerContent
import com.asylus.context.ui.navigation.Screen
import com.asylus.context.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingHomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val amplitudeScale by viewModel.amplitudeScale.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) viewModel.toggleRecording()
            else Toast.makeText(context, "Permission Required", Toast.LENGTH_SHORT).show()
        }
    )

    // UI State: Large button in center if recording OR idle. 
    // Small button at top if transcribing OR transcript exists and not recording.
    val showSmallButton = isTranscribing || (transcript.isNotEmpty() && !isRecording)

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
                    onRecordingClick = { recording ->
                        scope.launch { drawerState.close() }
                        viewModel.selectRecording(recording)
                    },
                    onDeleteClick = { viewModel.deleteRecording(it) },
                    onCloseClick = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextLight)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextLight)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg)
                )
            },
            containerColor = DeepBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Brush.verticalGradient(listOf(SurfaceBg, DeepBg)))
            ) {
                // Transcript and Status Area
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = if (showSmallButton) 100.dp else 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (isTranscribing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = GlowRed,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "TRANSCRIBING...",
                                color = GlowRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    } else if (transcript.isNotEmpty()) {
                        Text(
                            text = "TRANSCRIPT",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }

                    if (transcript.isNotEmpty() || isTranscribing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = transcript,
                            color = TextLight,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 28.sp
                        )
                    }
                }

                // Recording Button Animation
                val buttonSize by animateDpAsState(
                    targetValue = if (showSmallButton) 64.dp else 120.dp,
                    label = "buttonSize",
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = if (showSmallButton) Alignment.TopEnd else Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(if (showSmallButton) 80.dp else 280.dp)
                        ) {
                            if (isRecording) {
                                // Pulsing ripples
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val rippleScale by infiniteTransition.animateFloat(
                                    initialValue = 1f, targetValue = 1.8f,
                                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
                                    label = "ripple"
                                )
                                val rippleAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.6f, targetValue = 0f,
                                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
                                    label = "alpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(buttonSize)
                                        .scale(rippleScale)
                                        .background(GlowRedDim.copy(alpha = rippleAlpha), CircleShape)
                                )

                                val animatedAmplitude by animateFloatAsState(
                                    targetValue = amplitudeScale,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "amp"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(buttonSize * 1.1f)
                                        .scale(animatedAmplitude)
                                        .background(GlowRedRipple, CircleShape)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(buttonSize)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(Color(0xFFFF5E62), GlowRed)))
                                    .clickable {
                                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) viewModel.toggleRecording()
                                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Record",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (showSmallButton) 24.dp else 48.dp)
                                )
                            }
                        }

                        if (!showSmallButton && !isRecording) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("TAP TO START", color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        } else if (isRecording) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(elapsedTime, color = GlowRed, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
