package com.madhumangal.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhumangal.data.ChatMessage
import com.madhumangal.ui.theme.AccentPurple
import com.madhumangal.ui.theme.ActiveRed
import com.madhumangal.ui.theme.CardBorder
import com.madhumangal.ui.theme.DarkBackground
import com.madhumangal.ui.theme.PrimaryBlue
import com.madhumangal.ui.theme.SurfaceDark
import com.madhumangal.ui.theme.TextMuted
import com.madhumangal.ui.theme.TextPrimary
import com.madhumangal.ui.theme.TextSecondary
import com.madhumangal.ui.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAskBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    isHindi: Boolean = false
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()

    var inputQuery by remember { mutableStateOf("") }
    var isVoiceOutputEnabled by remember { mutableStateOf(true) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    val listState = rememberLazyListState()

    // Initialize TextToSpeech engine
    DisposableEffect(Unit) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Try Indian English or Hindi Locale
                val hiLocale = Locale("hi", "IN")
                val langAvailable = ttsInstance?.isLanguageAvailable(hiLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (langAvailable >= TextToSpeech.LANG_AVAILABLE) {
                    ttsInstance?.language = hiLocale
                } else {
                    ttsInstance?.language = Locale.getDefault()
                }
            }
        }
        ttsEngine = ttsInstance

        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    fun speakText(text: String) {
        if (!isVoiceOutputEnabled) return
        val clean = text.replace(Regex("[*•_#\\[\\]]"), " ")
        ttsEngine?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "echo_ai_voice")
    }

    // Speech-To-Text voice launcher (Default to voice query)
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputQuery = spoken
                viewModel.askAiAssistant(spoken) { response ->
                    speakText(response.text)
                }
                inputQuery = ""
            }
        }
    }

    fun launchVoiceRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindi) "hi-IN" else "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, if (isHindi) "hi-IN" else "en-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHindi) "इको एआई से पूछें (उदा. \"बाज़ार में क्या सामान लेना है?\")" else "Ask Echo AI (e.g. \"Market me kya lena hai?\")")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Speech recognition not available on device", Toast.LENGTH_SHORT).show()
        }
    }

    // Scroll to latest message on update
    LaunchedEffect(chatMessages.size, isThinking) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .imePadding()
        ) {
            // ── Top Header Row ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryBlue.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "इको एआई सहायक" else "Echo AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isHindi) "वॉइस क्वेरी व आउटपुट सक्रिय" else "Voice Query & Output Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Output (TTS) Toggle
                    IconButton(
                        onClick = {
                            isVoiceOutputEnabled = !isVoiceOutputEnabled
                            if (!isVoiceOutputEnabled) {
                                ttsEngine?.stop()
                            }
                            Toast.makeText(
                                context,
                                if (isVoiceOutputEnabled) (if (isHindi) "🔊 आवाज़ आउटपुट चालू" else "🔊 Voice output enabled")
                                else (if (isHindi) "🔇 आवाज़ आउटपुट बंद" else "🔇 Voice output muted"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (isVoiceOutputEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Voice Output",
                            tint = if (isVoiceOutputEnabled) PrimaryBlue else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Close Button
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ── Chat Messages Stream ────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(chatMessages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onSaveAsGroup = { groupName, items ->
                            viewModel.saveAiResponseAsGroup(groupName, items) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "इको एआई आपकी सूची में खोज रहा है..." else "Echo AI is searching your lists...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Input & Voice Bar ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Mic Button (Default)
                Surface(
                    shape = CircleShape,
                    color = PrimaryBlue,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    onClick = { launchVoiceRecognition() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Query",
                            tint = DarkBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Text Field
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = {
                        Text(
                            text = if (isHindi) "पूछें (उदा. बाज़ार से क्या सामान लाना है?)..." else "Ask (e.g. market me kya lena hai?)...",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button
                IconButton(
                    onClick = {
                        if (inputQuery.isNotBlank()) {
                            val q = inputQuery
                            inputQuery = ""
                            viewModel.askAiAssistant(q) { response ->
                                speakText(response.text)
                            }
                        }
                    },
                    enabled = inputQuery.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (inputQuery.isNotBlank()) AccentPurple else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputQuery.isNotBlank()) Color.White else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onSaveAsGroup: (String, List<String>) -> Unit
) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isUser) PrimaryBlue else CardBorder
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) DarkBackground else TextPrimary,
                        lineHeight = 20.sp
                    )

                    // If AI generated a structured list of items, provide "📌 Save as Pinned Group" button
                    if (!isUser && message.suggestedItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val groupName = message.suggestedGroup ?: "Market Shopping"
                                onSaveAsGroup(groupName, message.suggestedItems)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPurple
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📌 Pin as \"${message.suggestedGroup ?: "Group"}\" at Top",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isUser) "You" else "Echo AI",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
