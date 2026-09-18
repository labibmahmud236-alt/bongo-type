package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveAppType
import com.example.model.DictationLanguage

@Composable
fun ActiveAppWorkbench(
    activeApp: ActiveAppType,
    currentText: String,
    onTextChanged: (String) -> Unit,
    onSelectApp: (ActiveAppType) -> Unit,
    onSampleDictation: (String) -> Unit,
    onCopyAll: () -> Unit,
    onClear: () -> Unit,
    selectedLanguage: DictationLanguage,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2430)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_app_workbench")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Windows Titlebar styling
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Window control dots (minimize, maximize, close)
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF5F56), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF27C93F), CircleShape))

                    Spacer(modifier = Modifier.width(14.dp))

                    val appIcon = when (activeApp) {
                        ActiveAppType.IMO -> Icons.Default.Chat
                        ActiveAppType.WHATSAPP -> Icons.Default.Chat
                        ActiveAppType.MESSENGER -> Icons.Default.Forum
                        ActiveAppType.TELEGRAM -> Icons.Default.Send
                        ActiveAppType.CHROME -> Icons.Default.Language
                        ActiveAppType.NOTEPAD -> Icons.Default.EditNote
                        ActiveAppType.VS_CODE -> Icons.Default.Code
                    }

                    Icon(
                        imageVector = appIcon,
                        contentDescription = activeApp.appName,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeApp.windowTitle,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Focused Target",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Window Selection Tabs (WhatsApp, IMO, Messenger, Telegram, Chrome, Notepad, VS Code)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF18202C))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(ActiveAppType.entries) { app ->
                    val isSelected = app == activeApp
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF2E384D) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectApp(app) }
                            .testTag("app_tab_${app.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = app.appName,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Editor / Active Typing Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(Color(0xFF0D1117))
                    .padding(14.dp)
            ) {
                if (currentText.isEmpty()) {
                    Text(
                        text = "Cursor active in ${activeApp.appName}. Click the mic or tap sample prompts below to test universal voice typing...",
                        color = Color(0xFF4B5563),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                BasicTextField(
                    value = currentText,
                    onValueChange = onTextChanged,
                    textStyle = TextStyle(
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontFamily = if (activeApp == ActiveAppType.VS_CODE) FontFamily.Monospace else FontFamily.Default
                    ),
                    cursorBrush = SolidColor(Color(0xFF38BDF8)),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("active_app_textfield")
                )
            }

            // Sample Utterance Chips for Bengali, Banglish, and Arabic quick testing
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161E2E))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Quick Voice Test Prompts (${selectedLanguage.displayName}):",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row {
                        IconButton(
                            onClick = onCopyAll,
                            modifier = Modifier.size(28.dp).testTag("workbench_copy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Text",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(28.dp).testTag("workbench_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Editor",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val samplePrompts = when (selectedLanguage) {
                    DictationLanguage.BANGLA -> listOf(
                        "আমি এখন কথা বলছি।",
                        "কেমন আছো বন্ধু?",
                        "আমি একটু পরে আসছি।",
                        "আজকের আবহাওয়া খুব সুন্দর।"
                    )
                    DictationLanguage.BANGLISH -> listOf(
                        "Ami tomar sathe kotha bolchi",
                        "Kemon acho bondhu?",
                        "Ami ekhon kaj korchi",
                        "Dhonnobad tomar message er jonno"
                    )
                    DictationLanguage.ARABIC -> listOf(
                        "السلام عليكم ورحمة الله وبركاته",
                        "كيف حالك يا صديقي؟",
                        "أهلاً وسهلاً بكم في بونغو تايب",
                        "شكراً جزيلاً لك"
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(samplePrompts) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF233044),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSampleDictation(prompt) }
                                .testTag("prompt_chip_$prompt")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = prompt,
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
