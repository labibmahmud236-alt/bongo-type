package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DictationLanguage
import com.example.model.MicState
import kotlin.math.roundToInt

@Composable
fun FloatingVoicePill(
    micState: MicState,
    selectedLanguage: DictationLanguage,
    audioRms: Float,
    livePartialText: String,
    onMicClick: () -> Unit,
    onLanguageSelected: (DictationLanguage) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrayMenu: () -> Unit,
    modifier: Modifier = Modifier,
    isDraggable: Boolean = true
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isLanguageMenuOpen by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val dragModifier = if (isDraggable) {
        Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(dragModifier)
            .testTag("floating_voice_pill_container")
    ) {
        // Live dictation preview bubble if speaking
        AnimatedVisibility(
            visible = livePartialText.isNotBlank(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .width(280.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF38BDF8), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = livePartialText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                }
            }
        }

        // Main Windows 11 Acrylic Voice Typing Pill
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xEB161C24),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF475569).copy(alpha = 0.6f),
                        Color(0xFF1E293B).copy(alpha = 0.3f)
                    )
                )
            ),
            shadowElevation = 16.dp,
            modifier = Modifier.testTag("floating_voice_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // Microphone Button with Animated Glow & Ripple
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("mic_button_wrapper")
                ) {
                    // Pulsing Glow behind Mic
                    if (micState == MicState.LISTENING) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .scale(pulseScale)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF00E5FF).copy(alpha = glowAlpha),
                                            Color(0xFF0284C7).copy(alpha = glowAlpha * 0.4f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                    }

                    val buttonBg = when (micState) {
                        MicState.LISTENING -> Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
                        MicState.PROCESSING -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                        MicState.MUTED -> Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
                        MicState.IDLE -> Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(buttonBg)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable { onMicClick() }
                            .testTag("mic_button")
                    ) {
                        when (micState) {
                            MicState.PROCESSING -> {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Processing Speech",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            MicState.MUTED -> {
                                Icon(
                                    imageVector = Icons.Default.MicOff,
                                    contentDescription = "Muted",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // Audio Waveform Visualizer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    WaveformVisualizer(
                        isListening = micState == MicState.LISTENING,
                        audioRms = audioRms,
                        barCount = 7,
                        width = 72.dp,
                        height = 30.dp
                    )
                }

                // Language Selector Button & Dropdown
                Box(modifier = Modifier.wrapContentSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF263238).copy(alpha = 0.7f))
                            .border(1.dp, Color(0xFF455A64).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { isLanguageMenuOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("language_selector_button")
                    ) {
                        Text(
                            text = selectedLanguage.flag,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = selectedLanguage.nativeName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Language",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isLanguageMenuOpen,
                        onDismissRequest = { isLanguageMenuOpen = false },
                        modifier = Modifier
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    ) {
                        DictationLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = lang.flag, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = lang.nativeName,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = lang.displayName,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onLanguageSelected(lang)
                                    isLanguageMenuOpen = false
                                },
                                modifier = Modifier.testTag("lang_option_${lang.name}")
                            )
                        }
                    }
                }

                // Quick Settings & Tray Options Icon
                IconButton(
                    onClick = { onOpenSettings() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("pill_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Voice Typing Settings",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onOpenTrayMenu() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("pill_tray_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
