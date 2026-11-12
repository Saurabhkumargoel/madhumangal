package com.echo.recorder.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

object GeminiApiService {

    /**
     * Transcribes an audio file (WAV) directly using Google Gemini 1.5 Flash multimodal audio API.
     * Supports Hindi, Hinglish, and English with highest verbatim accuracy.
     */
    suspend fun transcribeAudioWithGemini(
        audioFile: File,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() <= 44) {
            return@withContext Result.failure(IllegalArgumentException("Audio file is empty or missing"))
        }

        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 20000
                readTimeout = 25000
                doOutput = true
            }

            val requestBody = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()

                // Audio part
                val audioPart = JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mime_type", "audio/wav")
                        put("data", base64Audio)
                    }
                    put("inline_data", inlineData)
                }
                parts.put(audioPart)

                // Instruction part
                val textPart = JSONObject().apply {
                    put("text", "Transcribe this audio recording verbatim. It contains speech in Hindi, Hinglish, or English. Return ONLY the transcribed words with correct punctuation, with no intro, outro, or quotes.")
                }
                parts.put(textPart)

                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonResponse = JSONObject(responseStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""
                    if (text.isNotBlank()) {
                        return@withContext Result.success(text)
                    }
                }
                Result.failure(Exception("Empty transcription returned by Gemini API"))
            } else {
                val errorStream = connection.errorStream
                val errorMsg = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                } else {
                    "HTTP $responseCode"
                }
                Result.failure(Exception("Gemini API Error ($responseCode): $errorMsg"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Extracts structured todos and notes from transcript using Gemini API.
     */
    suspend fun extractTasksWithGemini(
        transcript: String,
        apiKey: String
    ): Result<List<TinyAiProcessor.ExtractedTask>> = withContext(Dispatchers.IO) {
        if (transcript.isBlank() || apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Empty transcript or API key"))
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
            }

            val prompt = """
                Analyze this transcribed audio text (in Hindi, Hinglish, or English):
                "$transcript"
                
                Extract action items, todos, tasks, or notes.
                For each item, output one line formatted EXACTLY as:
                TASK||[Group]||[Priority]||[IsQuickWin]||[Title]||[Description]
                
                Where:
                [Group] is one of: Work, Shopping, Personal, Urgent, Ideas
                [Priority] is one of: 1 (LOW), 2 (MEDIUM), 3 (HIGH)
                [IsQuickWin] is: true or false
                [Title] is: short action title
                [Description] is: brief explanation
                
                Example:
                TASK||Personal||3||true||Call Mummy||Call mother today at 5pm
            """.trimIndent()

            val requestBody = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                }
                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonResponse = JSONObject(responseStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

                val tasks = mutableListOf<TinyAiProcessor.ExtractedTask>()
                text.lines().forEach { line ->
                    if (line.startsWith("TASK||")) {
                        val parts = line.split("||")
                        if (parts.size >= 6) {
                            val group = parts[1].trim()
                            val priority = parts[2].trim().toIntOrNull() ?: 2
                            val isQuickWin = parts[3].trim().toBoolean()
                            val title = parts[4].trim()
                            val description = parts[5].trim()
                            tasks.add(
                                TinyAiProcessor.ExtractedTask(
                                    title = title,
                                    description = description,
                                    group = group,
                                    priority = priority,
                                    isLowHangingFruit = isQuickWin
                                )
                            )
                        }
                    }
                }
                Result.success(tasks)
            } else {
                Result.failure(Exception("Gemini task extraction error ($responseCode)"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Interactive conversational AI response using Gemini 1.5 Flash / Pro.
     * Yields concise, dynamic assistant responses perfect for voice TTS and chat.
     */
    suspend fun chatWithGemini(
        query: String,
        apiKey: String,
        model: String = "gemini-1.5-flash",
        systemInstruction: String = "You are Madhu, a warm, intelligent, concise voice assistant in Madhumangal. Give natural, helpful answers in the user's language (Hindi, Hinglish, or English). Keep voice responses direct and easy to listen to.",
        contextInfo: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (query.isBlank() || apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Empty query or API key"))
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
            }

            val fullPrompt = if (!contextInfo.isNullOrBlank()) {
                "Context information:\n$contextInfo\n\nUser Question:\n$query"
            } else {
                query
            }

            val requestBody = JSONObject().apply {
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", fullPrompt) })
                }
                contentObj.put("parts", parts)
                contents.put(contentObj)
                put("contents", contents)

                if (systemInstruction.isNotBlank()) {
                    val systemInstructionObj = JSONObject().apply {
                        val sysParts = JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        }
                        put("parts", sysParts)
                    }
                    put("systemInstruction", systemInstructionObj)
                }
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonResponse = JSONObject(responseStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.trim() ?: ""
                if (text.isNotBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Empty response from Gemini"))
                }
            } else {
                val errorStream = connection.errorStream
                val errorMsg = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                } else {
                    "HTTP $responseCode"
                }
                Result.failure(Exception("Gemini Chat Error ($responseCode): $errorMsg"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
