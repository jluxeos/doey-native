package com.doey.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val state     by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    var input     by remember { mutableStateOf("") }
    val scope     = rememberCoroutineScope()
    val settings  = remember { vm.getSettings() }
    var theme     by remember { mutableStateOf("DeepSeaBlue") }

    LaunchedEffect(Unit) {
        theme = settings.getTheme()
        updateGlassTheme(theme)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Box(Modifier.fillMaxSize()) {
        GlassBackground(accentColor = TauAccent)

        Column(Modifier.fillMaxSize()) {
            // Top Bar (Empty space for system bars)
            Spacer(Modifier.height(32.dp))
            
            // Chat Area
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(state.messages) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        GlassCard(
                            modifier = Modifier.widthIn(max = 280.dp),
                            opacity = if (isUser) GlassOpacity else GlassOpacity * 0.5f,
                            blur = GlassBlur
                        ) {
                            Text(
                                text = msg.text,
                                color = TauText1,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Input Area
            Box(Modifier.padding(16.dp)) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    opacity = GlassOpacity,
                    blur = GlassBlur
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Escribe algo...", color = TauText3) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TauText1,
                                unfocusedTextColor = TauText1
                            )
                        )
                        IconButton(
                            onClick = {
                                if (input.isNotBlank()) {
                                    vm.sendMessage(input)
                                    input = ""
                                }
                            },
                            modifier = Modifier.clip(CircleShape).background(TauAccent)
                        ) {
                            Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp)) // Bottom navigation space
        }
    }
}
