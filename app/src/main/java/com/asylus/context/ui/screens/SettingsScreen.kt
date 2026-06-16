package com.asylus.context.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asylus.context.R
import com.asylus.context.ui.theme.DeepBg
import com.asylus.context.ui.theme.SurfaceBg
import com.asylus.context.ui.theme.TextLight
import com.asylus.context.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: com.asylus.context.ui.MainViewModel, onBackClick: () -> Unit) {
    val engines = listOf("xAI (Grok)", "Android System (Live)")
    var selectedEngine by remember { mutableStateOf(viewModel.getSelectedEngine()) }
    var xAiApiKey by remember { mutableStateOf(viewModel.getXaiApiKey()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextLight, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg)
            )
        },
        containerColor = DeepBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Brush.verticalGradient(listOf(SurfaceBg, DeepBg)))
                .padding(24.dp)
        ) {
            Text(
                text = "TRANSCRIPTION ENGINE",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Column(Modifier.selectableGroup()) {
                engines.forEach { text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (text == selectedEngine),
                                onClick = { 
                                    selectedEngine = text
                                    viewModel.setSelectedEngine(text)
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == selectedEngine),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF5E62))
                        )
                        Text(
                            text = text,
                            color = TextLight,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (selectedEngine == "xAI (Grok)") {
                Text(
                    text = "xAI CONFIGURATION",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = xAiApiKey,
                    onValueChange = { 
                        xAiApiKey = it
                        viewModel.setXaiApiKey(it)
                    },
                    label = { Text("xAI API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = TextLight,
                        focusedTextColor = TextLight,
                        unfocusedBorderColor = TextMuted,
                        focusedBorderColor = Color(0xFFFF5E62)
                    )
                )
            }
        }
    }
}
