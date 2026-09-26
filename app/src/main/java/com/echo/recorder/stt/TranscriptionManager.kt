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
    private val transcriptionQueue = Channel<Long>(Channel.UNLIMITED)
    private val sttEngine: SpeechToTextEngine = HindiHinglishSTTEngine(context)
    private val tinyAiProcessor = TinyAiProcessor()
    private val prefsRepo = PreferencesRepository(context)

    init {
        scope.launch {
            for (segmentId in transcriptionQueue) {
                processSegment(segmentId)
            }
        }
    }

    fun enqueueTranscription(segmentId: Long) {
        scope.launch {
            transcriptionQueue.send(segmentId)
        }
    }

    private suspend fun processSegment(segmentId: Long) {
        val segment = audioSegmentDao.getSegmentById(segmentId) ?: return

        // Update status to TRANSCRIBING
        val updatedSegment = segment.copy(status = AudioSegment.STATUS_TRANSCRIBING)
        audioSegmentDao.updateSegment(updatedSegment)

        val audioFile = File(segment.filePath)
        val result = sttEngine.transcribeAudioFile(audioFile)

        result.fold(
            onSuccess = { transcribedText ->
                // Insert into transcriptions table
                val transcription = Transcription(
                    segmentId = segment.id,
                    text = transcribedText,
                    timestampMillis = segment.startTimeMillis,
                    durationSeconds = segment.durationSeconds,
                    confidence = 0.95f
                )
                val transcriptionId = transcriptionDao.insertTranscription(transcription)

                // Process with Tiny AI to extract NoteTasks
                val extractedTasks = tinyAiProcessor.processTranscription(transcribedText, transcriptionId)
                for (task in extractedTasks) {
                    noteTaskDao.insertTask(task)
                }

                // Mark segment COMPLETED
                audioSegmentDao.updateSegment(segment.copy(status = AudioSegment.STATUS_COMPLETED))

                // Check auto-delete setting
                val prefs = prefsRepo.userPreferencesFlow.first()
                if (prefs.autoDeleteAudio && audioFile.exists()) {
                    try {
                        audioFile.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onFailure = { error ->
                audioSegmentDao.updateSegment(segment.copy(status = AudioSegment.STATUS_FAILED))
            }
        )
    }
}
