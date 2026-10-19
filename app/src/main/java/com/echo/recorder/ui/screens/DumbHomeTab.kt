package com.echo.recorder.ui.screens

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.ActiveRed
import com.echo.recorder.ui.theme.CardBorder
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.SurfaceVariantDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import com.echo.recorder.ui.viewmodel.MainViewModel
import java.util.Locale

/**
 * Minimalist Smart/Dumb Mode Homepage:
 * - A single master toggle for continuous background listening & transcribing.
 * - Voice Assistant ("Good Bot" / "गुड बॉट") listening in real-time.
 * - When activated, AI answers and speaks back via TextToSpeech.
 * - Recording NEVER stops while listening or speaking.
 * - Interruption detection: bot immediately silences when user speaks or taps to interrupt.
 * - Fully reactive to active themes (AMOLED, Neon, Pastel, Custom).
 */
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
    var lastAiResponse by remember { mutableStateOf<String?>(null) }
    var isAiThinking by remember { mutableStateOf(false) }
    var isBotSpeaking by remember { mutableStateOf(false) }
    var ttsStartTime by remember { mutableLongStateOf(0L) }
    var detectedQueryText by remember { mutableStateOf("") }

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
            isBotSpeaking = true
            ttsStartTime = System.currentTimeMillis()
            ttsEngine?.setSpeechRate(userPreferences.ttsSpeed)
            ttsEngine?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "good_bot_speech_${System.currentTimeMillis()}")
        }
    }

    fun silenceBot() {
        ttsEngine?.stop()
        isBotSpeaking = false
    }

    // Interruption Detector:
    // When bot is speaking, if the user starts speaking (speech detected or high amplitude/dB after grace window),
    // immediately silence the bot without touching the recorder!
    LaunchedEffect(isSpeechDetected, currentAmplitude, currentDb, isBotSpeaking) {
        if (isBotSpeaking) {
            val now = System.currentTimeMillis()
            // 700ms grace window after TTS begins to prevent initial speaker feedback trip
            if (now - ttsStartTime > 700L) {
                if (isSpeechDetected || currentAmplitude > 0.22f || currentDb > -28) {
                    silenceBot()
                }
            }
        }
    }

    // Keyword Matcher for "good bot" or "गुड बॉट"
    val botKeywordRegex = remember {
        Regex("(?i)\\b(good\\s*bot|gud\\s*bot|गुड\\s*बॉट)\\b")
    }

    LaunchedEffect(liveTranscript, isRecording) {
        if (!isRecording || isAiThinking) return@LaunchedEffect

        val trimmed = liveTranscript.trim()
        if (trimmed.isNotBlank() && trimmed != lastProcessedTranscript) {
            val match = botKeywordRegex.find(trimmed)
            if (match != null) {
                val queryAfterKeyword = trimmed.substring(match.range.last + 1).trim()
                lastProcessedTranscript = trimmed

                if (queryAfterKeyword.length >= 3) {
                    // Extract query & query AI assistant
                    detectedQueryText = queryAfterKeyword
                    isAiThinking = true
                    viewModel.askAiAssistant(queryAfterKeyword) { response ->
                        isAiThinking = false
                        lastAiResponse = response.text
                        speakBot(response.text)
                    }
                } else if (queryAfterKeyword.isBlank()) {
                    // Keyword acknowledged
                    val ack = if (userPreferences.aiLanguage == "hi") {
                        "हाँ, मैं सुन रहा हूँ! आप क्या पूछना चाहते हैं?"
                    } else {
                        "Yes, I'm listening! How can I help you?"
                    }
                    lastAiResponse = ack
                    speakBot(ack)
                }
            }
        }
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRecording) 1000 else 2400, easing = FastOutSlowInEasing),
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
            isRecording -> if (isSpeechDetected) PrimaryBlue else PrimaryBlue.copy(alpha = 0.85f)
            else -> SurfaceVariantDark
        },
        animationSpec = tween(400),
        label = "orbColor"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Smart Assistant",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "स्मार्ट वॉइस मोड" else "SMART VOICE MODE",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Elapsed Timer Badge
            if (isRecording) {
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

        Spacer(modifier = Modifier.height(28.dp))

        // CENTER GLOWING VOICE ORB
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleRecording
                )
        ) {
            // Outermost Atmospheric Ring
            if (isRecording) {
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
                    .scale(if (isRecording) dynamicOrbScale * 0.95f else 1f)
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
                        width = if (isRecording) 2.dp else 1.dp,
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
                            colors = if (isRecording) {
                                listOf(activeOrbColor, activeOrbColor.copy(alpha = 0.75f))
                            } else {
                                listOf(SurfaceDark, SurfaceVariantDark)
                            }
                        )
                    )
                    .shadow(elevation = if (isRecording) 16.dp else 4.dp, shape = CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isRecording) Color.White.copy(alpha = 0.8f) else TextMuted.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = when {
                        !isRecording -> Icons.Default.MicOff
                        isBotSpeaking -> Icons.Default.VolumeUp
                        isAiThinking -> Icons.Default.AutoAwesome
                        else -> Icons.Default.Mic
                    },
                    contentDescription = if (isRecording) "Stop Listening" else "Start Listening",
                    tint = if (isRecording) Color.White else TextSecondary,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // STATUS LABEL
        val statusText = when {
            !isRecording -> if (isHindi) "शुरू करने के लिए टैप करें" else "Tap Orb to Start Listening"
            isBotSpeaking -> if (isHindi) "बॉट बोल रहा है • बोलने पर चुप होगा" else "Good Bot is Speaking • Speak to Interrupt"
            isAiThinking -> if (isHindi) "एआई सोच रहा है..." else "Good Bot is thinking..."
            isSpeechDetected -> if (isHindi) "आवाज़ सुनाई दे रही है..." else "Hearing Speech..."
            else -> if (isHindi) "\"गुड बॉट\" कहें और सवाल पूछें" else "Listening... Say \"Good Bot\" anytime"
        }

        Text(
            text = statusText,
            color = if (isBotSpeaking) AccentPurple else if (isRecording) PrimaryBlue else TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        // Waveform / Audio Level Bar when Recording
        if (isRecording) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = null,
                    tint = if (isSpeechDetected) PrimaryBlue else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(fraction = (currentAmplitude * 2.5f).coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PrimaryBlue, AccentPurple)
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${currentDb} dB",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

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
            Spacer(modifier = Modifier.height(14.dp))
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
                            Text(
                                text = if (isHindi) "गुड बॉट उत्तर" else "GOOD BOT RESPONSE",
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
                    text = if (isHindi) "💡 संकेत: \"गुड बॉट\" कहें (जैसे: \"गुड बॉट, आज क्या काम है?\")"
                    else "💡 Trigger: Say \"Good bot, what are my tasks today?\"",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isHindi) "🎙️ रिकॉर्डिंग कभी बंद नहीं होगी, बोलते समय भी सुनता रहेगा।"
                    else "🎙️ Continuous listening: Recording never stops, even while bot speaks.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
