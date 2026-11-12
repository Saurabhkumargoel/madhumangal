package com.madhumangal.ui.screens

import android.content.Context
import android.os.BatteryManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhumangal.data.ActionPosition
import com.madhumangal.data.ActionSize
import com.madhumangal.data.BotActionType
import com.madhumangal.data.BotCustomAction
import com.madhumangal.data.UserPreferences
import com.madhumangal.ui.theme.AccentPurple
import com.madhumangal.ui.theme.ActiveRed
import com.madhumangal.ui.theme.CardBorder
import com.madhumangal.ui.theme.DarkBackground
import com.madhumangal.ui.theme.PrimaryBlue
import com.madhumangal.ui.theme.SurfaceDark
import com.madhumangal.ui.theme.SurfaceVariantDark
import com.madhumangal.ui.theme.TextMuted
import com.madhumangal.ui.theme.TextPrimary
import com.madhumangal.ui.theme.TextSecondary
import com.madhumangal.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Minimalist Smart Voice Assistant & Madhu Tab:
 * - A single master toggle for continuous background listening & transcribing.
 * - Real-time conversational Voice Assistant ("Madhu" / "मधु" / "मधु" / "गॉड बॉट").
 * - Active 15-second follow-up window after wake word so subsequent speech is naturally answered.
 * - Instant on-device voice answers for Time, Date, and Battery via TTS.
 * - Recording NEVER stops while listening or speaking.
 * - Interruption detection: bot immediately silences when user speaks or taps to interrupt.
 * - Developer Mode Custom Action Studio (Pencil FAB) with customizable placement & size.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DumbHomeTab(
    viewModel: MainViewModel,
    isRecording: Boolean,
    currentAmplitude: Float,
    currentDb: Int,
    elapsedSeconds: Long,
    liveTranscript: String,
    isSpeechDetected: Boolean,
    userPreferences: UserPreferences,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHindi = userPreferences.appLanguage == "hi"

    // Assistant State
    var lastProcessedTranscript by remember { mutableStateOf("") }
    var lastProcessedTime by remember { mutableLongStateOf(0L) }
    var lastAiResponse by remember { mutableStateOf<String?>(null) }
    var isAiThinking by remember { mutableStateOf(false) }
    var isBotSpeaking by remember { mutableStateOf(false) }
    var isExplicitlySilenced by remember { mutableStateOf(false) }
    var ttsStartTime by remember { mutableLongStateOf(0L) }
    var detectedQueryText by remember { mutableStateOf("") }

    // Conversational Follow-Up State (Multi-Turn Dynamic Window)
    var isWaitingForFollowUpQuery by remember { mutableStateOf(false) }
    var activeWindowSecondsRemaining by remember { mutableIntStateOf(0) }

    // Developer Studio & Layout Inspector Dialog States
    var showStudioDialog by remember { mutableStateOf(false) }
    var showDevLayoutInspector by remember { mutableStateOf(false) }
    val customActions = remember(userPreferences.botCustomActions) {
        BotCustomAction.parseList(userPreferences.botCustomActions)
    }

    // TextToSpeech Engine
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    // Initialize TTS
    DisposableEffect(userPreferences.aiLanguage, userPreferences.ttsSpeed) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = if (userPreferences.aiLanguage == "hi") Locale("hi", "IN") else Locale.ENGLISH
                val available = ttsInstance?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (available >= TextToSpeech.LANG_AVAILABLE) {
                    ttsInstance?.language = locale
                } else {
                    ttsInstance?.language = Locale.getDefault()
                }
                ttsInstance?.setSpeechRate(userPreferences.ttsSpeed)
            }
        }

        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isBotSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isBotSpeaking = false
                // Multi-Turn: Keep listening for follow-up questions after answering unless user explicitly silenced
                if (!isExplicitlySilenced) {
                    isWaitingForFollowUpQuery = true
                    activeWindowSecondsRemaining = 15
                }
            }

            override fun onError(utteranceId: String?) {
                isBotSpeaking = false
            }
        })

        ttsEngine = ttsInstance

        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // Speech playback helper
    fun speakBot(text: String) {
        val clean = text.replace(Regex("[*•_#\\[\\]\\-]"), " ").trim()
        if (clean.isNotBlank()) {
            isExplicitlySilenced = false
            isBotSpeaking = true
            ttsStartTime = System.currentTimeMillis()
            ttsEngine?.setSpeechRate(userPreferences.ttsSpeed)
            ttsEngine?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "madhu_speech_${System.currentTimeMillis()}")
        }
    }

    fun silenceBot(stopListening: Boolean = false) {
        ttsEngine?.stop()
        isBotSpeaking = false
        if (stopListening) {
            isExplicitlySilenced = true
            isWaitingForFollowUpQuery = false
        } else {
            // User voice interruption: keep follow-up window open for user question!
            isWaitingForFollowUpQuery = true
            activeWindowSecondsRemaining = 15
        }
    }

    // Interruption Detector with configurable noise gate and speaker feedback protection
    LaunchedEffect(isSpeechDetected, currentAmplitude, currentDb, isBotSpeaking) {
        if (isBotSpeaking) {
            val now = System.currentTimeMillis()
            // 1200ms grace period to prevent speaker acoustic echo from self-interrupting
            if (now - ttsStartTime > 1200L) {
                val noiseGate = userPreferences.botInterruptionNoiseGateDb
                if (currentDb > noiseGate && currentAmplitude > 0.30f) {
                    silenceBot(stopListening = false)
                }
            }
        }
    }

    // Fast Instant Voice Answers
    fun speakCurrentTime() {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        val timeStr = sdf.format(Date())
        val answer = if (isHindi) "अभी समय $timeStr हुआ है।" else "The current time is $timeStr."
        lastAiResponse = answer
        speakBot(answer)
        isWaitingForFollowUpQuery = true
        activeWindowSecondsRemaining = 15
    }

    fun speakCurrentDate() {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", if (isHindi) Locale("hi", "IN") else Locale.US)
        val dateStr = sdf.format(Date())
        val answer = if (isHindi) "आज $dateStr है।" else "Today is $dateStr."
        lastAiResponse = answer
        speakBot(answer)
        isWaitingForFollowUpQuery = true
        activeWindowSecondsRemaining = 15
    }

    fun speakCurrentTimeAndDate() {
        val timeSdf = SimpleDateFormat("hh:mm a", Locale.US)
        val timeStr = timeSdf.format(Date())
        val dateSdf = SimpleDateFormat("EEEE, d MMMM yyyy", if (isHindi) Locale("hi", "IN") else Locale.US)
        val dateStr = dateSdf.format(Date())
        val answer = if (isHindi) "अभी समय $timeStr हुआ है और आज $dateStr है।" else "The current time is $timeStr and today is $dateStr."
        lastAiResponse = answer
        speakBot(answer)
        isWaitingForFollowUpQuery = true
        activeWindowSecondsRemaining = 15
    }

    fun speakBatteryLevel() {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val answer = if (level >= 0) {
            if (isHindi) "डिवाइस की बैटरी $level प्रतिशत है।" else "The device battery is at $level percent."
        } else {
            if (isHindi) "बैटरी स्तर उपलब्ध नहीं है।" else "Battery level is unavailable."
        }
        lastAiResponse = answer
        speakBot(answer)
        isWaitingForFollowUpQuery = true
        activeWindowSecondsRemaining = 12
    }

    // Handle user queries (fast local checks first, then AI assistant)
    fun handleQueryText(query: String) {
        val qLower = query.lowercase(Locale.ROOT).trim()

        // Clear active live speech buffer so the consumed query does not linger or retrigger
        com.madhumangal.audio.AudioRecordingState.liveTranscript.value = ""
        lastProcessedTranscript = query
        lastProcessedTime = System.currentTimeMillis()

        // Check if user is asking to silence/dismiss the bot
        val isDismissQuery = qLower in listOf("bye", "stop", "quiet", "बाय", "अलविदा", "बस", "चुप", "शांत", "थैंक यू", "धन्यवाद", "थैंक्स") ||
                qLower.startsWith("अलविदा") || qLower.startsWith("bye") || qLower.startsWith("बस करो")

        if (isDismissQuery) {
            val farewell = if (isHindi) "धन्यवाद! जब भी जरूरत हो, बुला लीजिएगा।" else "You're welcome! Just call me if you need anything."
            lastAiResponse = farewell
            isWaitingForFollowUpQuery = false
            speakBot(farewell)
            return
        }

        if (com.madhumangal.util.ReminderHelper.isReminderQuery(query)) {
            val reminder = com.madhumangal.util.ReminderHelper.parseReminder(query)
            viewModel.createTask(
                title = reminder.title,
                categoryGroup = "Reminders",
                priority = com.madhumangal.data.NoteTask.PRIORITY_HIGH,
                isLowHangingFruit = false,
                dueText = reminder.formattedTime,
                parentId = null,
                autoCompleteOnSubtasksDone = false
            )
            com.madhumangal.util.ReminderHelper.launchCalendarReminder(
                context = context,
                reminder = reminder,
                targetCalendarApp = userPreferences.selectedCalendarApp
            )
            val answer = if (isHindi) {
                "मैंने ${reminder.formattedTime} पर '${reminder.title}' के लिए कैलेंडर रिमाइंडर खोल दिया है।"
            } else {
                "I've opened the calendar reminder for '${reminder.title}' at ${reminder.formattedTime}."
            }
            lastAiResponse = answer
            speakBot(answer)
            isWaitingForFollowUpQuery = true
            activeWindowSecondsRemaining = 15
            return
        }

        val hasTime = qLower.contains("time") || qLower.contains("टाइम") || qLower.contains("कितने बजे") || qLower.contains("के बजे") || (qLower.contains("समय") && !qLower.contains("मौसम")) || qLower.contains("वक्त")
        val hasDate = qLower.contains("date") || qLower.contains("तारीख") || qLower.contains("तिथि") || qLower.contains("दिनांक") || qLower.contains("डेट") || qLower.contains("दैट") || qLower.contains("दिन") || qLower.contains("देन") || qLower.contains("आज क्या") || qLower.contains("आज के") || qLower.contains("वार") || qLower.contains("today") || qLower.contains("day")
        val isBatteryQuery = qLower.contains("battery") || qLower.contains("बैटरी") || qLower.contains("चार्ज")

        when {
            hasTime && hasDate -> speakCurrentTimeAndDate()
            hasTime -> speakCurrentTime()
            hasDate -> speakCurrentDate()
            isBatteryQuery -> speakBatteryLevel()
            else -> {
                detectedQueryText = query
                isAiThinking = true
                viewModel.askAiAssistant(query) { response ->
                    isAiThinking = false
                    lastAiResponse = response.text
                    speakBot(response.text)
                    isWaitingForFollowUpQuery = true
                    activeWindowSecondsRemaining = 15
                }
            }
        }
    }

    fun executeCustomAction(action: BotCustomAction) {
        when (action.actionType) {
            BotActionType.SPEAK_TIME -> speakCurrentTime()
            BotActionType.SPEAK_DATE -> speakCurrentDate()
            BotActionType.SPEAK_BATTERY -> speakBatteryLevel()
            BotActionType.WAKE_BOT -> {
                isWaitingForFollowUpQuery = true
                activeWindowSecondsRemaining = 15
                val ack = if (isHindi) "हाँ, मैं सुन रहा हूँ! क्या पूछना चाहते हैं?" else "Yes, I'm listening! What would you like to know?"
                lastAiResponse = ack
                speakBot(ack)
            }
            BotActionType.TOGGLE_RECORD -> onToggleRecording()
            BotActionType.ASK_AI_PROMPT -> {
                if (action.promptText.isNotBlank()) {
                    handleQueryText(action.promptText)
                }
            }
            BotActionType.CLEAR_TRANSCRIPT -> {
                com.madhumangal.audio.AudioRecordingState.liveTranscript.value = ""
            }
        }
    }

    // Keyword Matcher dynamically supporting custom bot name, acoustic alias list from voice training, and Hindi phonetic variations
    val botKeywordRegex = remember(userPreferences.customBotName, userPreferences.customWakeAliases) {
        val baseVariants = mutableListOf(
            "madhu\\s*bot", "madhu", "मधु\\s*बॉट", "मधु", "माधु", "मदु", "मधू", "माधो", "माधव",
            "मधुमंगल", "हे\\s*मधु", "hey\\s*madhu", "hello\\s*madhu", "madhumangal",
            "good\\s*bot", "gud\\s*bot", "god\\s*bot", "got\\s*bot", "good\\s*boy",
            "गुड\\s*बॉट", "गॉड\\s*बॉट", "गुड\\s*बहुत", "गॉड\\s*बहुत", "गुड\\s*भौत", "गुड\\s*बात",
            "गोद\\s*बहुत", "गत\\s*बहुत", "गुड़\\s*बहुत", "गुड\\s*बोट", "गुड\\s*वाट", "गुड\\s*वाच",
            "गुड\\s*बाप", "गुड\\s*बाय", "गुडबॉट", "गॉडबॉट", "गुडबहुत", "गुड\\s*बाद", "गत\\s*बाद",
            "गोल्ड\\s*बाद", "गोल्ड\\s*बॉन्ड", "गोल्ड\\s*बहुत", "घोर\\s*बहुत", "गुरु\\s*बहुत",
            "घर\\s*बहुत", "गौर\\s*बहुत", "गुट\\s*बहुत", "गुड\\s*मौत", "घटना\\s*मौत", "गीत\\s*की\\s*मौत",
            "गुड\\s*पाठ", "गोड\\s*गोड\\s*बहुत", "गुड\\s*गुड\\s*मौत", "गुरु\\s*भूत", "गोद\\s*बात",
            "बोर्ड\\s*बाद", "गत\\s*बॉट", "गत\\s*आठ", "दुखद\\s*मौत", "गॉड\\s*गॉड", "गर\\s*दौर",
            "बहुत\\s*बाद", "गॉड\\s*बाद", "गॉड\\s*बात", "गार्ड\\s*बात", "गत\\s*बात", "गत\\s*डॉट",
            "रोड\\s*बाद", "घर\\s*बैठे", "घोर\\s*बाद", "घोर\\s*बॉट", "दैट\\s*दे"
        )
        val customName = userPreferences.customBotName.trim()
        if (customName.isNotBlank()) {
            baseVariants.add(Regex.escape(customName))
            baseVariants.add("${Regex.escape(customName)}\\s*bot")
            baseVariants.add("hey\\s*${Regex.escape(customName)}")
            baseVariants.add("hello\\s*${Regex.escape(customName)}")
            baseVariants.add("हे\\s*${Regex.escape(customName)}")
        }
        userPreferences.customWakeAliases.forEach { alias ->
            if (alias.isNotBlank()) {
                baseVariants.add(Regex.escape(alias.trim()))
            }
        }
        val pattern = "(?i)(" + baseVariants.distinct().joinToString("|") + ")"
        Regex(pattern)
    }

    // Active Conversational Window Countdown (15s default)
    // Only counts down when bot is NOT speaking so the user has the full window AFTER bot speech completes
    LaunchedEffect(isWaitingForFollowUpQuery, isBotSpeaking) {
        if (isWaitingForFollowUpQuery && !isBotSpeaking) {
            if (activeWindowSecondsRemaining <= 0) {
                activeWindowSecondsRemaining = 15
            }
            while (activeWindowSecondsRemaining > 0 && isWaitingForFollowUpQuery && !isBotSpeaking) {
                delay(1000)
                activeWindowSecondsRemaining--
            }
            if (activeWindowSecondsRemaining <= 0 && !isBotSpeaking) {
                isWaitingForFollowUpQuery = false
            }
        }
    }

    // Reactive Live Speech Listener & State Machine
    LaunchedEffect(liveTranscript, isRecording) {
        if (!isRecording || isAiThinking) return@LaunchedEffect

        val trimmed = liveTranscript.trim()
        val now = System.currentTimeMillis()
        val isDifferentUtterance = trimmed != lastProcessedTranscript || (now - lastProcessedTime > 2500L)

        if (trimmed.isNotBlank() && isDifferentUtterance) {
            val match = botKeywordRegex.find(trimmed)
            if (match != null) {
                if (isBotSpeaking) {
                    silenceBot(stopListening = false)
                }
                val queryAfterKeyword = trimmed.substring(match.range.last + 1).trim()
                lastProcessedTranscript = trimmed
                lastProcessedTime = now

                if (queryAfterKeyword.length >= 2) {
                    handleQueryText(queryAfterKeyword)
                } else {
                    isWaitingForFollowUpQuery = true
                    activeWindowSecondsRemaining = 15
                    val ack = if (isHindi) "हाँ, मैं सुन रहा हूँ! क्या पूछना चाहते हैं?" else "Yes, I'm listening! What would you like to know?"
                    lastAiResponse = ack
                    speakBot(ack)
                }
            } else {
                val qLower = trimmed.lowercase(Locale.ROOT)
                val hasTime = qLower.contains("time") || qLower.contains("टाइम") || qLower.contains("कितने बजे") || qLower.contains("के बजे") || (qLower.contains("समय") && !qLower.contains("मौसम")) || qLower.contains("वक्त")
                val hasDate = qLower.contains("date") || qLower.contains("तारीख") || qLower.contains("तिथि") || qLower.contains("दिनांक") || qLower.contains("डेट") || qLower.contains("दैट") || qLower.contains("दिन") || qLower.contains("देन") || qLower.contains("आज क्या") || qLower.contains("आज के") || qLower.contains("वार") || qLower.contains("today") || qLower.contains("day")
                val isReminder = com.madhumangal.util.ReminderHelper.isReminderQuery(trimmed)
                val isBattery = qLower.contains("battery") || qLower.contains("बैटरी") || qLower.contains("चार्ज")
                val isDirectUtility = hasTime || hasDate || isReminder || isBattery

                if ((isDirectUtility || isWaitingForFollowUpQuery) && trimmed.length >= 2) {
                    val isNoiseArtifact = trimmed in listOf("और", "बाद", "बात", "दो", "घोर", "था", "थी", "है", "or", "and")
                    if (!isNoiseArtifact) {
                        if (isBotSpeaking) {
                            silenceBot(stopListening = false)
                        }
                        lastProcessedTranscript = trimmed
                        lastProcessedTime = now
                        handleQueryText(trimmed)
                    }
                }
            }
        }
    }

    // Visual Animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording || isWaitingForFollowUpQuery) 1.15f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isWaitingForFollowUpQuery) 600 else if (isRecording) 1000 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val amplitudeBoost = if (isRecording) (currentAmplitude * 0.45f).coerceIn(0f, 0.4f) else 0f
    val dynamicOrbScale by animateFloatAsState(
        targetValue = pulseScale + amplitudeBoost,
        animationSpec = tween(150),
        label = "orbScale"
    )

    val activeOrbColor by animateColorAsState(
        targetValue = when {
            isBotSpeaking -> AccentPurple
            isAiThinking -> PrimaryBlue
            isWaitingForFollowUpQuery -> Color(0xFF10B981) // Emerald Green during active follow-up
            isRecording -> if (isSpeechDetected) PrimaryBlue else PrimaryBlue.copy(alpha = 0.85f)
            else -> SurfaceVariantDark
        },
        animationSpec = tween(400),
        label = "orbColor"
    )

    val scrollState = rememberScrollState()
    var showHelpDialog by remember { mutableStateOf(false) }

    // Categorized Custom Actions by Placement
    val headerActions = remember(customActions) { customActions.filter { it.position == ActionPosition.HEADER } }
    val quickActions = remember(customActions) { customActions.filter { it.position == ActionPosition.QUICK_ACTIONS } }
    val bottomActions = remember(customActions) { customActions.filter { it.position == ActionPosition.BOTTOM } }
    val floatingActions = remember(customActions) { customActions.filter { it.position == ActionPosition.FLOATING } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when (userPreferences.homeTheme) {
                        com.madhumangal.data.UserPreferences.HOME_THEME_MIDNIGHT -> Brush.verticalGradient(listOf(Color(0xFF0A1224), Color(0xFF040814)))
                        com.madhumangal.data.UserPreferences.HOME_THEME_EMERALD -> Brush.verticalGradient(listOf(Color(0xFF071B14), Color(0xFF030D08)))
                        com.madhumangal.data.UserPreferences.HOME_THEME_CARBON -> Brush.verticalGradient(listOf(Color(0xFF14151A), Color(0xFF090A0C)))
                        else -> Brush.verticalGradient(listOf(Color(0xFF0F141C), Color(0xFF07090E)))
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TOP HEADER
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val botDisplayName = userPreferences.customBotName.ifBlank { "Madhu" }
                        Text(
                            text = if (isHindi) "$botDisplayName (मधु)" else botDisplayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Madhu Mangal Assistant",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help & Prompts",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modeLabel = when (userPreferences.settingsMode) {
                        com.madhumangal.data.SettingsMode.SMART -> if (isHindi) "⚡ स्मार्ट मोड" else "⚡ SMART MODE"
                        com.madhumangal.data.SettingsMode.ADVANCED -> if (isHindi) "⚙️ एडवांस्ड मोड" else "⚙️ ADVANCED MODE"
                        com.madhumangal.data.SettingsMode.DEVELOPER -> if (isHindi) "🛠️ डेवलपर मोड" else "🛠️ DEV MODE"
                    }
                    val modeColor = when (userPreferences.settingsMode) {
                        com.madhumangal.data.SettingsMode.SMART -> PrimaryBlue
                        com.madhumangal.data.SettingsMode.ADVANCED -> AccentPurple
                        com.madhumangal.data.SettingsMode.DEVELOPER -> Color(0xFF10B981)
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF141A24),
                        border = androidx.compose.foundation.BorderStroke(1.dp, modeColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Assistant Mode",
                                tint = modeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = modeLabel,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Elapsed Timer or Active Listening Window Badge
                        if (isWaitingForFollowUpQuery) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "सुन रहा है: ${activeWindowSecondsRemaining}s" else "Listening: ${activeWindowSecondsRemaining}s",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (isRecording) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ActiveRed.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ActiveRed.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ActiveRed)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val hrs = elapsedSeconds / 3600
                                    val mins = (elapsedSeconds % 3600) / 60
                                    val secs = elapsedSeconds % 60
                                    Text(
                                        text = if (hrs > 0) String.format("%02d:%02d:%02d", hrs, mins, secs) else String.format("%02d:%02d", mins, secs),
                                        color = ActiveRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = SurfaceDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Text(
                                    text = if (isHindi) "निष्क्रिय" else "IDLE",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Header Custom Action Chips
                if (headerActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        headerActions.forEach { action ->
                            CustomActionButton(action = action, onClick = { executeCustomAction(action) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CENTER GLOWING VOICE ORB (2.5x Scale default, dev-mode position & scale adjustable)
            val baseOrbScale = userPreferences.orbScaleMultiplier.coerceIn(0.5f, 5.0f)
            val orbOffsetX = userPreferences.orbOffsetX
            val orbOffsetY = userPreferences.orbOffsetY
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(orbOffsetX.dp, orbOffsetY.dp)
                    .graphicsLayer {
                        scaleX = baseOrbScale / 1.6f
                        scaleY = baseOrbScale / 1.6f
                        transformOrigin = TransformOrigin.Center
                    }
                    .size(240.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleRecording
                    )
            ) {
                // Outermost Atmospheric Ring
                if (isRecording || isWaitingForFollowUpQuery) {
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .scale(dynamicOrbScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        activeOrbColor.copy(alpha = 0.25f),
                                        activeOrbColor.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Middle Glowing Halo Ring
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .scale(if (isRecording || isWaitingForFollowUpQuery) dynamicOrbScale * 0.95f else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeOrbColor.copy(alpha = 0.5f),
                                activeOrbColor.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        width = if (isRecording || isWaitingForFollowUpQuery) 2.dp else 1.dp,
                        color = activeOrbColor.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                )

                // Core Interactive Orb Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = if (isRecording || isWaitingForFollowUpQuery) {
                                    listOf(activeOrbColor, activeOrbColor.copy(alpha = 0.75f))
                                } else {
                                    listOf(SurfaceDark, SurfaceVariantDark)
                                }
                            )
                        )
                        .shadow(elevation = if (isRecording) 16.dp else 4.dp, shape = CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (isRecording || isWaitingForFollowUpQuery) Color.White.copy(alpha = 0.8f) else TextMuted.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = when {
                            !isRecording -> Icons.Default.MicOff
                            isBotSpeaking -> Icons.Default.VolumeUp
                            isAiThinking -> Icons.Default.AutoAwesome
                            isWaitingForFollowUpQuery -> Icons.Default.Hearing
                            else -> Icons.Default.Mic
                        },
                        contentDescription = if (isRecording) "Stop Listening" else "Start Listening",
                        tint = if (isRecording || isWaitingForFollowUpQuery) Color.White else TextSecondary,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // STATUS LABEL
            val statusText = when {
                !isRecording -> if (isHindi) "शुरू करने के लिए टैप करें" else "Tap Orb to Start Listening"
                isBotSpeaking -> if (isHindi) "मधु बोल रही है • बोलने पर चुप होगा" else "Madhu is Speaking • Speak to Interrupt"
                isAiThinking -> if (isHindi) "मधु सोच रही है..." else "Madhu is thinking..."
                isWaitingForFollowUpQuery -> if (isHindi) "मैं सुन रहा हूँ! सवाल पूछें (${activeWindowSecondsRemaining}s)" else "Listening for question... (${activeWindowSecondsRemaining}s)"
                isSpeechDetected -> if (isHindi) "आवाज़ सुनाई दे रही है..." else "Hearing Speech..."
                else -> if (isHindi) "\"मधु\" कहें और सवाल पूछें" else "Listening... Say \"Madhu\" anytime"
            }

            Text(
                text = statusText,
                color = if (isBotSpeaking) AccentPurple else if (isWaitingForFollowUpQuery) Color(0xFF10B981) else if (isRecording) PrimaryBlue else TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // DYNAMIC ANIMATED SOUNDWAVE VISUALIZER & MIC STATS
            if (isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedSoundwaveBars(
                    isRecording = isRecording,
                    amplitude = currentAmplitude,
                    isSpeechDetected = isSpeechDetected,
                    accentColor = activeOrbColor
                )
                Text(
                    text = "${currentDb} dB • " + if (isSpeechDetected) "Speech Active" else "Quiet",
                    color = if (isSpeechDetected) Color(0xFF10B981) else TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }



            // Quick Actions Placement Grid
            if (quickActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickActions.forEach { action ->
                        CustomActionButton(action = action, onClick = { executeCustomAction(action) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // MESSAGE CARDS WRAPPER (Transformable in Dev Mode with center point scale and offsets)
            val homeCardScale = userPreferences.homeCardScale.coerceIn(0.5f, 3.0f)
            val homeCardUnit = userPreferences.homeCardUnit
            val homeCardOffsetX = userPreferences.homeCardOffsetX
            val homeCardOffsetY = userPreferences.homeCardOffsetY
            val effectiveScale = when (homeCardUnit) {
                "PERCENT" -> homeCardScale
                "PX" -> homeCardScale
                "VW" -> homeCardScale
                "VH" -> homeCardScale
                else -> homeCardScale
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = effectiveScale
                        scaleY = effectiveScale
                        translationX = homeCardOffsetX
                        translationY = homeCardOffsetY
                        transformOrigin = TransformOrigin.Center
                    },
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // LIVE USER TRANSCRIPT CARD
                if (isRecording && liveTranscript.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.9f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isHindi) "लाइव आवाज़ ट्रांसक्रिप्ट" else "LIVE SPEECH",
                                    color = PrimaryBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                if (isSpeechDetected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryBlue)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = liveTranscript,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // BOT RESPONSE / INTERRUPT CARD
                AnimatedVisibility(
                    visible = lastAiResponse != null || isBotSpeaking,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = if (isBotSpeaking) AccentPurple else CardBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val botDisplayName = userPreferences.customBotName.ifBlank { "Madhu" }
                                    Text(
                                        text = if (isHindi) "$botDisplayName उत्तर" else "${botDisplayName.uppercase(Locale.ROOT)} RESPONSE",
                                        color = AccentPurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                // Silence / Interrupt Button
                                if (isBotSpeaking) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ActiveRed.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ActiveRed.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable { silenceBot() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeOff,
                                                contentDescription = "Silence",
                                                tint = ActiveRed,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isHindi) "रोकें" else "Hush / Silence",
                                                color = ActiveRed,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = lastAiResponse ?: "",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // Bottom Custom Action Placement Area
            if (bottomActions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    bottomActions.forEach { action ->
                        CustomActionButton(action = action, onClick = { executeCustomAction(action) })
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // QUICK HINT FOOTER
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHindi) "💡 संकेत: \"मधु\" कहें (जैसे: \"मधु, अभी क्या टाइम हुआ है?\")"
                        else "💡 Trigger: Say \"Madhu, what time is it?\" or ask follow-ups naturally",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "🎙️ रिकॉर्डिंग कभी बंद नहीं होगी, बोलते समय भी सुनता रहेगा।"
                        else "🎙️ Continuous listening: 15s conversational window active on wake word.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // FLOATING ACTION BUTTON & CUSTOM BUTTONS (DEVELOPER MODE ONLY)
        if (userPreferences.isDeveloperMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Standalone Floating Custom Action Buttons
                floatingActions.forEach { action ->
                    CustomActionButton(action = action, onClick = { executeCustomAction(action) })
                }

                // Dev Mode Layout Inspector FAB
                FloatingActionButton(
                    onClick = { showDevLayoutInspector = true },
                    shape = CircleShape,
                    containerColor = AccentPurple,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Dev Layout Inspector",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Pencil Circular FAB (Madhu Studio for Developer Mode)
                FloatingActionButton(
                    onClick = { showStudioDialog = true },
                    shape = CircleShape,
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Madhu UI",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Developer Layout Inspector Dialog
        if (showDevLayoutInspector) {
            DevLayoutInspectorDialog(
                userPreferences = userPreferences,
                onUpdateOrbScale = { viewModel.updateOrbScale(it) },
                onUpdateOrbOffset = { x, y -> viewModel.updateOrbOffset(x, y) },
                onUpdateHomeCardLayout = { s, u, x, y -> viewModel.updateHomeCardLayout(s, u, x, y) },
                onDismiss = { showDevLayoutInspector = false },
                isHindi = isHindi
            )
        }

        // Studio Dialog
        if (showStudioDialog) {
            MadhuStudioDialog(
                initialActions = customActions,
                onSaveActions = { updated ->
                    viewModel.updateBotCustomActions(BotCustomAction.serializeList(updated))
                },
                onDismiss = { showStudioDialog = false }
            )
        }

        // Help & User Instructions Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "मधु गाइड व निर्देश" else "Madhu Guide & Instructions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isHindi) "🎙️ बातचीत कैसे करें:" else "🎙️ How to Talk with Madhu:",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isHindi)
                                "• केंद्र में स्थित वॉइस बटन को दबाएं या सीधे \"मधु\" कहें।\n• बॉट जब बोल रहा हो, आप बीच में बात करके उसे रोक (Interrupt) सकते हैं।\n• एक्टिव विंडो में अगला सवाल बिना \"मधु\" कहे सीधे पूछें।"
                            else
                                "• Tap the central voice orb or say \"Madhu\" anytime.\n• You can interrupt the bot while it is speaking simply by speaking.\n• During the active window, ask follow-up questions directly without repeating \"Madhu\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "💡 उदाहरण सवाल (टैप करके पूछें):" else "💡 Example Prompts (Tap to ask):",
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple,
                            fontSize = 13.sp
                        )

                        val promptSuggestions = if (isHindi) listOf(
                            "🔔 मुझे याद दिलाना कल 10 बजे मीटिंग है" to "मुझे याद दिलाना कल 10 बजे मीटिंग है",
                            "⏰ समय क्या है?" to "समय क्या है?",
                            "📅 आज क्या तारीख है?" to "आज क्या तारीख है?",
                            "🔋 बैटरी कितनी है?" to "बैटरी कितनी है?"
                        ) else listOf(
                            "🔔 Remind me tomorrow 10am meeting" to "Remind me tomorrow at 10 AM for meeting",
                            "⏰ What's the time?" to "What is the time?",
                            "📅 What's the date?" to "What is the date?",
                            "🔋 Battery level?" to "Battery level?"
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            promptSuggestions.forEach { (label, prompt) ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF141923),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263346)),
                                    modifier = Modifier.clickable {
                                        showHelpDialog = false
                                        handleQueryText(prompt)
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text(if (isHindi) "समझ गया" else "Got It", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}

@Composable
private fun CustomActionButton(
    action: BotCustomAction,
    onClick: () -> Unit
) {
    val actionColor = try {
        Color(android.graphics.Color.parseColor(action.colorHex))
    } catch (_: Exception) { PrimaryBlue }

    when (action.size) {
        ActionSize.SMALL -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = actionColor.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, actionColor.copy(alpha = 0.5f)),
                modifier = Modifier.clickable { onClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getCustomIcon(action.actionType),
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = action.label,
                        color = actionColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        ActionSize.MEDIUM -> {
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionColor.copy(alpha = 0.22f),
                    contentColor = actionColor
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, actionColor.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = getCustomIcon(action.actionType),
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = action.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        ActionSize.LARGE -> {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = actionColor.copy(alpha = 0.18f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, actionColor.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(actionColor.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCustomIcon(action.actionType),
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = action.label,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = action.actionType.description,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun getCustomIcon(type: BotActionType): ImageVector {
    return when (type) {
        BotActionType.SPEAK_TIME -> Icons.Default.Schedule
        BotActionType.SPEAK_DATE -> Icons.Default.Today
        BotActionType.SPEAK_BATTERY -> Icons.Default.BatteryStd
        BotActionType.WAKE_BOT -> Icons.Default.Mic
        BotActionType.TOGGLE_RECORD -> Icons.Default.RadioButtonChecked
        BotActionType.ASK_AI_PROMPT -> Icons.Default.Psychology
        BotActionType.CLEAR_TRANSCRIPT -> Icons.Default.Clear
    }
}

@Composable
fun AnimatedSoundwaveBars(
    isRecording: Boolean,
    amplitude: Float,
    isSpeechDetected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 18
        for (i in 0 until barCount) {
            val offset = i.toFloat() / barCount * 6.283f
            val baseSin = kotlin.math.abs(kotlin.math.sin(wavePhase + offset))
            val dynamicHeight = if (isRecording) {
                val ampFactor = (amplitude * 3.5f).coerceIn(0.18f, 1f)
                (6.dp + (28.dp * baseSin * ampFactor))
            } else {
                4.dp
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(3.5.dp)
                    .height(dynamicHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isRecording) {
                            if (isSpeechDetected) accentColor else accentColor.copy(alpha = 0.55f)
                        } else {
                            Color(0xFF263345)
                        }
                    )
            )
        }
    }
}

@Composable
fun DevLayoutInspectorDialog(
    userPreferences: UserPreferences,
    onUpdateOrbScale: (Float) -> Unit,
    onUpdateOrbOffset: (Float, Float) -> Unit,
    onUpdateHomeCardLayout: (Float, String, Float, Float) -> Unit,
    onDismiss: () -> Unit,
    isHindi: Boolean
) {
    var orbScale by remember { mutableStateOf(userPreferences.orbScaleMultiplier) }
    var orbX by remember { mutableStateOf(userPreferences.orbOffsetX) }
    var orbY by remember { mutableStateOf(userPreferences.orbOffsetY) }

    var cardScale by remember { mutableStateOf(userPreferences.homeCardScale) }
    var cardUnit by remember { mutableStateOf(userPreferences.homeCardUnit) }
    var cardX by remember { mutableStateOf(userPreferences.homeCardOffsetX) }
    var cardY by remember { mutableStateOf(userPreferences.homeCardOffsetY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = AccentPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "🛠️ डेवलपर लेआउट व एनिमेशन" else "🛠️ Dev Layout & Animation Inspector",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Voice Orb Controls
                Text("🔮 Voice Orb Layout (Anchor: Center)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)

                Text("Scale: ${"%.2f".format(orbScale)}x (Default 2.5x)", fontSize = 11.sp, color = TextSecondary)
                Slider(
                    value = orbScale,
                    onValueChange = {
                        orbScale = it
                        onUpdateOrbScale(it)
                    },
                    valueRange = 0.5f..4.0f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offset X: ${orbX.toInt()} dp", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = orbX,
                            onValueChange = {
                                orbX = it
                                onUpdateOrbOffset(it, orbY)
                            },
                            valueRange = -150f..150f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offset Y: ${orbY.toInt()} dp", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = orbY,
                            onValueChange = {
                                orbY = it
                                onUpdateOrbOffset(orbX, it)
                            },
                            valueRange = -150f..150f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
                        )
                    }
                }

                // Section 2: Home Message Cards Layout
                Text("💬 Home Cards & Animations (Anchor: Center)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentPurple)

                // Units Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Unit:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    listOf("PERCENT" to "%", "PX" to "px", "VW" to "vw", "VH" to "vh").forEach { (unitKey, unitLabel) ->
                        val isSelected = cardUnit == unitKey
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) AccentPurple else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentPurple else CardBorder),
                            modifier = Modifier.clickable {
                                cardUnit = unitKey
                                onUpdateHomeCardLayout(cardScale, unitKey, cardX, cardY)
                            }
                        ) {
                            Text(
                                text = unitLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text("Card Scale: ${"%.2f".format(cardScale)} ($cardUnit)", fontSize = 11.sp, color = TextSecondary)
                Slider(
                    value = cardScale,
                    onValueChange = {
                        cardScale = it
                        onUpdateHomeCardLayout(it, cardUnit, cardX, cardY)
                    },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = AccentPurple, activeTrackColor = AccentPurple)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cards Offset X: ${cardX.toInt()} dp", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = cardX,
                            onValueChange = {
                                cardX = it
                                onUpdateHomeCardLayout(cardScale, cardUnit, it, cardY)
                            },
                            valueRange = -150f..150f,
                            colors = SliderDefaults.colors(thumbColor = AccentPurple, activeTrackColor = AccentPurple)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cards Offset Y: ${cardY.toInt()} dp", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = cardY,
                            onValueChange = {
                                cardY = it
                                onUpdateHomeCardLayout(cardScale, cardUnit, cardX, it)
                            },
                            valueRange = -150f..150f,
                            colors = SliderDefaults.colors(thumbColor = AccentPurple, activeTrackColor = AccentPurple)
                        )
                    }
                }

                // Reset Button
                OutlinedButton(
                    onClick = {
                        orbScale = 2.5f
                        orbX = 0f
                        orbY = 0f
                        cardScale = 1.0f
                        cardUnit = "PERCENT"
                        cardX = 0f
                        cardY = 0f
                        onUpdateOrbScale(2.5f)
                        onUpdateOrbOffset(0f, 0f)
                        onUpdateHomeCardLayout(1.0f, "PERCENT", 0f, 0f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isHindi) "डिफ़ॉल्ट पर रीसेट करें" else "Reset to Defaults (2.5x Orb)", color = TextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isHindi) "पूर्ण" else "Done", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SurfaceDark
    )
}
