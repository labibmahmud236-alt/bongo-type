package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.BongoSettings
import com.example.model.DictationLanguage
import com.example.model.GlobalShortcut
import com.example.model.SpeechEngineType

@Composable
fun SettingsDialog(
    settings: BongoSettings,
    isBubbleServiceActive: Boolean = false,
    hasOverlayPermission: Boolean = false,
    onRefreshPermissions: () -> Unit = {},
    onSaveSettings: (BongoSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var currentSettings by remember { mutableStateOf(settings) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161E2E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bongo Type Settings",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Global Shortcut
                Text(
                    text = "Global Shortcut",
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    GlobalShortcut.entries.forEach { shortcut ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { currentSettings = currentSettings.copy(shortcut = shortcut) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentSettings.shortcut == shortcut,
                                onClick = { currentSettings = currentSettings.copy(shortcut = shortcut) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = shortcut.label,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Speech Recognition Engine
                Text(
                    text = "Speech Recognition Engine",
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpeechEngineType.entries.forEach { engine ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentSettings.speechEngine == engine) Color(0xFF1E293B) else Color.Transparent,
                            border = if (currentSettings.speechEngine == engine) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { currentSettings = currentSettings.copy(speechEngine = engine) }
                                .padding(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                RadioButton(
                                    selected = currentSettings.speechEngine == engine,
                                    onClick = { currentSettings = currentSettings.copy(speechEngine = engine) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = engine.label,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = engine.description,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Universal Voice Typing Behaviors
                Text(
                    text = "Behavior & Windows Integration",
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                SettingsToggleRow(
                    title = "Continuous Dictation",
                    subtitle = "Never stops after one sentence until toggled",
                    checked = currentSettings.continuousDictation,
                    onCheckedChange = { currentSettings = currentSettings.copy(continuousDictation = it) }
                )

                SettingsToggleRow(
                    title = "Auto Text Injection",
                    subtitle = "Automatically insert text into active app without copying",
                    checked = currentSettings.autoTextInjection,
                    onCheckedChange = { currentSettings = currentSettings.copy(autoTextInjection = it) }
                )

                SettingsToggleRow(
                    title = "Always On Top",
                    subtitle = "Keep floating voice pill above all windows",
                    checked = currentSettings.alwaysOnTop,
                    onCheckedChange = { currentSettings = currentSettings.copy(alwaysOnTop = it) }
                )

                SettingsToggleRow(
                    title = "Startup With Windows",
                    subtitle = "Launch Bongo Type in system tray automatically",
                    checked = currentSettings.startWithWindows,
                    onCheckedChange = { currentSettings = currentSettings.copy(startWithWindows = it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 4: Universal Keyboard Floating Bubble (Permissions & Setup)
                Text(
                    text = "Universal Keyboard Bubble (IMO, WhatsApp, etc.)",
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                UniversalKeyboardBubbleCard(
                    isServiceActive = isBubbleServiceActive,
                    hasOverlayPermission = hasOverlayPermission,
                    onRefresh = onRefreshPermissions
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = {
                            onSaveSettings(currentSettings)
                            onDismiss()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Apply Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0284C7),
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
