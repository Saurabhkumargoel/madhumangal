package com.echo.recorder.stt

import android.content.Context
import com.echo.recorder.ai.TinyAiProcessor
import com.echo.recorder.data.AudioSegment
import com.echo.recorder.data.AudioSegmentDao
import com.echo.recorder.data.NoteTaskDao
import com.echo.recorder.data.PreferencesRepository
import com.echo.recorder.data.SttCapability
import com.echo.recorder.data.Transcription
import com.echo.recorder.data.TranscriptionDao
import com.echo.recorder.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class TranscriptionManager(
    private val context: Context,
    private val audioSegmentDao: AudioSegmentDao,
    private val transcriptionDao: TranscriptionDao,
    private val noteTaskDao: NoteTaskDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val transcriptionQueue = Channel<Pair<Long, String?>>(Channel.UNLIMITED)
    private val prefsRepo = PreferencesRepository(context)
    private val sttEngine: SpeechToTextEngine = HindiHinglishSTTEngine(context, prefsRepo)
    private val tinyAiProcessor = TinyAiProcessor()

    init {
        scope.launch {
            for ((segmentId, liveTranscript) in transcriptionQueue) {
                processSegment(segmentId, liveTranscript)
            }
        }
    }

    fun enqueueTranscription(segmentId: Long, liveTranscript: String? = null) {
        scope.launch {
            transcriptionQueue.send(Pair(segmentId, liveTranscript))
        }
    }

    private suspend fun processSegment(segmentId: Long, liveTranscript: String?) {
        val segment = audioSegmentDao.getSegmentById(segmentId) ?: return

        // Update status to TRANSCRIBING
        val updatedSegment = segment.copy(status = AudioSegment.STATUS_TRANSCRIBING)
        audioSegmentDao.updateSegment(updatedSegment)

        val audioFile = File(segment.filePath)
        val prefs = prefsRepo.userPreferencesFlow.first()

        val textToUse = if (!liveTranscript.isNullOrBlank()) {
            liveTranscript
        } else if (audioFile.exists() && audioFile.length() > 44) {
            // Live speech was empty or disconnected; automatically decode the recorded audio chunk
            val targetModel = if (UserPreferences.getModelCapability(prefs.selectedSttModel) == SttCapability.LIVE_ONLY) {
                UserPreferences.STT_MODEL_VOSK_HINDI
            } else {
                prefs.selectedSttModel
            }
            com.echo.recorder.util.AppLogger.i("TRANSCRIPTION", "Live speech stream was blank; decoding ${audioFile.name} with offline model ($targetModel)...")
            val res = sttEngine.transcribeAudioFile(audioFile, targetModel)
            val decoded = res.getOrNull()
            if (!decoded.isNullOrBlank() && !decoded.startsWith("[Offline model not ready")) {
                decoded
            } else {
                "[Audio Recorded: ${audioFile.name}]"
            }
        } else {
            "[Ambient Audio: ${audioFile.name}]"
        }

        // Insert into transcriptions table
        val transcription = Transcription(
            segmentId = segment.id,
            text = textToUse,
            timestampMillis = segment.startTimeMillis,
            durationSeconds = segment.durationSeconds,
            confidence = 0.95f
        )
        val transcriptionId = transcriptionDao.insertTranscription(transcription)

        // Update live transcript display on screen ONLY if it's real speech (not ambient tag)
        if (!textToUse.startsWith("[Ambient") && !textToUse.startsWith("[Silence") && !textToUse.startsWith("[Audio Recorded")) {
            com.echo.recorder.audio.AudioRecordingState.liveTranscript.value = textToUse
        }

        // Process with Tiny AI to extract NoteTasks ONLY if real speech is present!
        val extractedTasks = if (!textToUse.startsWith("[Ambient") && !textToUse.startsWith("[Silence") && !textToUse.startsWith("[Audio Recorded")) {
            val hierarchical = tinyAiProcessor.processTranscriptionHierarchical(textToUse, transcriptionId)
            val saved = tinyAiProcessor.saveHierarchicalTasksToDatabase(noteTaskDao, hierarchical)
            com.echo.recorder.util.AppLogger.i("AI", "Extracted ${saved.size} background tasks into Dashboard:")
            for ((idx, task) in saved.withIndex()) {
                com.echo.recorder.util.AppLogger.i("AI", "  [Task #${idx + 1}] \"${task.title}\" | Group: ${task.categoryGroup} | Prio: ${task.priority}")
            }
            saved
        } else {
            emptyList()
        }

        // Save transcription and AI notes files to user configured folders (default: Download/App data/poc/yesboss)
        try {
            com.echo.recorder.util.AppStorageManager.saveTranscriptionToFile(
                context = context,
                audioFileName = audioFile.name,
                text = textToUse,
                durationSeconds = segment.durationSeconds,
                overlapSeconds = segment.overlapSeconds,
                engineName = prefs.selectedSttModel,
                tasks = extractedTasks,
                customTranscriptionDir = prefs.transcriptionFolderPath
            )
            com.echo.recorder.util.AppStorageManager.saveNotesToFile(
                context = context,
                audioFileName = audioFile.name,
                transcriptionText = textToUse,
                tasks = extractedTasks,
                customNotesDir = prefs.notesFolderPath
            )
        } catch (e: Exception) {
            com.echo.recorder.util.AppLogger.e("STORAGE", "Error saving transcript/note file", e)
        }

        // Mark segment COMPLETED
        audioSegmentDao.updateSegment(segment.copy(status = AudioSegment.STATUS_COMPLETED))

        // Check auto-delete setting
        if (prefs.autoDeleteAudio && audioFile.exists()) {
            try {
                audioFile.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
