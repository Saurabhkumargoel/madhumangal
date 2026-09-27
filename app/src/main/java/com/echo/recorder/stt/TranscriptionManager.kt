package com.echo.recorder.stt

import android.content.Context
import com.echo.recorder.ai.TinyAiProcessor
import com.echo.recorder.data.AudioSegment
import com.echo.recorder.data.AudioSegmentDao
import com.echo.recorder.data.NoteTaskDao
import com.echo.recorder.data.PreferencesRepository
import com.echo.recorder.data.Transcription
import com.echo.recorder.data.TranscriptionDao
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
        val textToUse = if (!liveTranscript.isNullOrBlank()) {
            liveTranscript
        } else {
            sttEngine.transcribeAudioFile(audioFile).getOrDefault("[Audio Recorded: ${audioFile.name}]")
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

        // Update live transcript display on screen
        com.echo.recorder.audio.AudioRecordingState.liveTranscript.value = textToUse

        // Process with Tiny AI to extract NoteTasks
        val extractedTasks = tinyAiProcessor.processTranscription(textToUse, transcriptionId)
        for (task in extractedTasks) {
            noteTaskDao.insertTask(task)
        }

        val prefs = prefsRepo.userPreferencesFlow.first()

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
