package com.echo.recorder.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.zIndex
import com.echo.recorder.data.ChatMessage
import com.echo.recorder.data.NoteTask
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.ActiveRed
import com.echo.recorder.ui.theme.CardBorder
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import com.echo.recorder.ui.viewmodel.MainViewModel
import com.echo.recorder.util.AppStrings
import java.util.Locale

@Composable
fun AiTab(
    viewModel: MainViewModel,
    tasks: List<NoteTask>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val isHindi = userPreferences.appLanguage == "hi"

    var inputQuery by remember { mutableStateOf("") }
    var isVoiceOutputEnabled by remember { mutableStateOf(true) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var currentSpeechText by remember { mutableStateOf("") }
    var speechSentences by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSentenceIndex by remember { mutableIntStateOf(0) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsSpeed by remember { mutableFloatStateOf(userPreferences.ttsSpeed) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun speakSentenceAt(index: Int) {
        if (!isVoiceOutputEnabled || speechSentences.isEmpty()) return
        if (index >= speechSentences.size) {
            isSpeaking = false
            isPaused = false
            currentSentenceIndex = 0
            return
        }
        currentSentenceIndex = index
        ttsEngine?.setSpeechRate(ttsSpeed)
        val textToSpeak = speechSentences[index]
        ttsEngine?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, index.toString())
        isSpeaking = true
        isPaused = false
    }

    // Initialize TextToSpeech engine with utterance listener
    DisposableEffect(userPreferences.aiLanguage) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = if (userPreferences.aiLanguage == "hi") {
                    Locale("hi", "IN")
                } else {
                    Locale.ENGLISH
                }
                val langAvailable = ttsInstance?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (langAvailable >= TextToSpeech.LANG_AVAILABLE) {
                    ttsInstance?.language = locale
                } else {
                    ttsInstance?.language = Locale.getDefault()
                }
                ttsInstance?.setSpeechRate(ttsSpeed)
            }
        }
        ttsInstance.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                isPaused = false
            }
            override fun onDone(utteranceId: String?) {
                val nextIdx = (utteranceId?.toIntOrNull() ?: 0) + 1
                if (nextIdx < speechSentences.size && isSpeaking && !isPaused) {
                    currentSentenceIndex = nextIdx
                    speakSentenceAt(nextIdx)
                } else {
                    isSpeaking = false
                    isPaused = false
                    currentSentenceIndex = 0
                }
            }
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                isPaused = false
            }
        })
        ttsEngine = ttsInstance

        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    fun startSpeech(text: String) {
        if (!isVoiceOutputEnabled) return
        val clean = text.replace(Regex("[*•_#\\[\\]\\-]"), " ").trim()
        currentSpeechText = clean
        val sList = clean.split(Regex("(?<=[.?!।\n])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        speechSentences = if (sList.isNotEmpty()) sList else listOf(clean)
        currentSentenceIndex = 0
        speakSentenceAt(0)
    }

    fun pauseSpeech() {
        ttsEngine?.stop()
        isSpeaking = false
        isPaused = true
    }

    fun resumeSpeech() {
        if (speechSentences.isNotEmpty() && currentSentenceIndex < speechSentences.size) {
            speakSentenceAt(currentSentenceIndex)
        } else {
            val latestAiMsg = chatMessages.findLast { !it.isUser }
            if (latestAiMsg != null) {
                startSpeech(latestAiMsg.text)
            }
        }
    }

    fun stopSpeaking() {
        ttsEngine?.stop()
        isSpeaking = false
        isPaused = false
        currentSentenceIndex = 0
    }

    fun updateTtsSpeedLive(newSpeed: Float) {
        ttsSpeed = newSpeed
        viewModel.updateTtsSpeed(newSpeed)
        ttsEngine?.setSpeechRate(newSpeed)
        // Live speed update: if speaking, restart current sentence immediately with new speed
        if (isSpeaking && !isPaused && speechSentences.isNotEmpty() && currentSentenceIndex < speechSentences.size) {
            ttsEngine?.stop()
            speakSentenceAt(currentSentenceIndex)
        }
    }

    // Speech-To-Text voice launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.askAiAssistant(spoken) { response ->
                    startSpeech(response.text)
                }
            }
        }
    }

    fun launchVoiceRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (userPreferences.aiLanguage == "hi") "hi-IN" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, AppStrings.get("search_tasks_hint", isHindi))
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

    // Predefined smart suggestion questions based on active tasks list
    val suggestionQuestions = remember(tasks, isHindi) {
        val list = mutableListOf<String>()
        val hasShopping = tasks.any { it.categoryGroup.equals("Shopping", ignoreCase = true) || it.title.startsWith("Buy", ignoreCase = true) }
        val hasUrgent = tasks.any { it.priority == NoteTask.PRIORITY_HIGH || it.categoryGroup.equals("Urgent", ignoreCase = true) }
        val hasUpcoming = tasks.any { it.dueTimeText.isNotBlank() }

        if (isHindi) {
            if (hasShopping) list.add("बाजार में क्या क्या सामान खरीदना है?")
            if (hasUrgent) list.add("मेरे सबसे जरूरी कार्य क्या हैं?")
            if (hasUpcoming) list.add("आज के लिए कौन से काम तय हैं?")
            list.add("मेरे सभी लंबित कार्यों का सारांश दिखाओ")
            list.add("आसान और तुरंत होने वाले काम कौन से हैं?")
        } else {
            if (hasShopping) list.add("What groceries do I need to buy?")
            if (hasUrgent) list.add("Show my urgent high-priority tasks")
            if (hasUpcoming) list.add("What tasks are scheduled for today?")
            list.add("Summarize all my active tasks")
            list.add("What quick wins can I do right now?")
        }
        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding()
        ) {
            // ── Top Header with Media Player & Controls ─────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AccentPurple.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = AppStrings.get("tab_ai", isHindi),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (userPreferences.aiLanguage == "hi") "हिन्दी भाषा • ${String.format(Locale.US, "%.1fx", ttsSpeed)}" else "English • ${String.format(Locale.US, "%.1fx", ttsSpeed)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    // AI Response Language Selector in Chat Head
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("auto", "Auto", "Auto (Match)"),
                            Triple("hi", "हिन्दी", "Hindi"),
                            Triple("en", "English", "English")
                        ).forEach { (langCode, label, fullName) ->
                            val isSelected = userPreferences.aiLanguage == langCode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AccentPurple else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) AccentPurple else CardBorder),
                                modifier = Modifier.clickable {
                                    viewModel.updateAiLanguage(langCode)
                                    Toast.makeText(context, "AI Language: $fullName", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Predefined Suggested Questions ──────────────────────────────────
        Text(
            text = if (isHindi) "सुझाए गए प्रश्न (त्वरित चयन):" else "Suggested Queries (Quick Tap):",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestionQuestions.forEach { question ->
                FilterChip(
                    selected = false,
                    onClick = {
                        inputQuery = question
                        viewModel.askAiAssistant(question) { resp ->
                            startSpeech(resp.text)
                        }
                        inputQuery = ""
                    },
                    label = { Text(question, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceDark,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = CardBorder,
                        enabled = true,
                        selected = false
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Chat Messages Stream (Selectable Text & Copy Buttons) ───────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(chatMessages) { message ->
                AiMessageItem(
                    message = message,
                    isHindi = isHindi,
                    onCopy = { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Echo AI Response", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, if (isHindi) "कॉपी किया गया" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onSpeak = { text -> startSpeech(text) },
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
                            text = if (isHindi) "एआई आपकी सूची की जांच कर रहा है..." else "Echo AI is searching your lists...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Input & Voice Bar ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Mic Button
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

            Spacer(modifier = Modifier.width(8.dp))

            // Text Field
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = {
                    Text(
                        text = if (isHindi) "प्रश्न पूछें (उदा. क्या सामान लाना है?)..." else "Ask a question about your tasks...",
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
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                singleLine = true,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        val q = inputQuery.trim()
                        inputQuery = ""
                        viewModel.askAiAssistant(q) { response ->
                            startSpeech(response.text)
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

    // ── Floating Action Menu (Bottom Left) with Outside Touch Collapse ──
    var showSpeedPresets by remember { mutableStateOf(false) }

    if (showSettingsSheet) {
        // Full screen transparent dismiss scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(24f)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    showSettingsSheet = false
                    showSpeedPresets = false
                }
        )
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(bottom = 76.dp, start = 16.dp)
            .zIndex(25f)
    ) {
        AnimatedVisibility(
            visible = showSettingsSheet,
            enter = fadeIn(tween(150)) + expandVertically(tween(150)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed Presets Popup
                AnimatedVisibility(
                    visible = showSpeedPresets,
                    enter = fadeIn(tween(120)) + expandVertically(tween(120)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                val isSelected = Math.abs(ttsSpeed - spd) < 0.05f
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        updateTtsSpeedLive(spd)
                                        showSpeedPresets = false
                                    }
                                ) {
                                    Text(
                                        text = "${spd}x",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) DarkBackground else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Current Speed Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                    shadowElevation = 6.dp,
                    modifier = Modifier.clickable { showSpeedPresets = !showSpeedPresets }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.2fx", ttsSpeed)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }

                // 2. Play Button
                Surface(
                    shape = CircleShape,
                    color = PrimaryBlue,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable {
                            if (isPaused) {
                                resumeSpeech()
                            } else {
                                val latestAiMsg = chatMessages.findLast { !it.isUser }
                                if (latestAiMsg != null) {
                                    startSpeech(latestAiMsg.text)
                                } else {
                                    startSpeech("No AI responses to play.")
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = DarkBackground, modifier = Modifier.size(22.dp))
                    }
                }

                // 3. Pause Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF59E0B),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { pauseSpeech() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = DarkBackground, modifier = Modifier.size(20.dp))
                    }
                }

                // 4. Stop Button
                Surface(
                    shape = CircleShape,
                    color = ActiveRed,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { stopSpeaking() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Bottom-Left Floating Hamburger Trigger Button
        Surface(
            shape = CircleShape,
            color = if (showSettingsSheet) AccentPurple else SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (showSettingsSheet) AccentPurple else CardBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(46.dp)
                .clickable {
                    showSettingsSheet = !showSettingsSheet
                    if (!showSettingsSheet) showSpeedPresets = false
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (showSettingsSheet) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = if (showSettingsSheet) Color.White else PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
    }
}

@Composable
fun AiMessageItem(
    message: ChatMessage,
    isHindi: Boolean,
    onCopy: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onSaveAsGroup: (String, List<String>) -> Unit
) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) PrimaryBlue else SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isUser) PrimaryBlue else CardBorder
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Selectable Text Container
                    SelectionContainer {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) DarkBackground else TextPrimary,
                            lineHeight = 22.sp
                        )
                    }

                    // Action buttons row for Assistant messages: Copy, Speak, Pin as Group
                    if (!isUser) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onCopy(message.text) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Text",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onSpeak(message.text) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speak Text",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (message.suggestedItems.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val groupName = message.suggestedGroup ?: if (isHindi) "बाजार खरीदारी" else "Market Shopping"
                                        onSaveAsGroup(groupName, message.suggestedItems)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentPurple
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHindi) "पिन करें" else "Pin Group",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isUser) (if (isHindi) "आप" else "You") else (if (isHindi) "गूड इको एआई" else "Echo AI"),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
