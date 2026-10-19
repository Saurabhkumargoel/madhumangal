package com.echo.recorder.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.echo.recorder.data.SettingsMode
import com.echo.recorder.data.UserPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SettingsExportHelper {

    fun exportToJson(prefs: UserPreferences): String {
        val root = JSONObject().apply {
            put("app", "GoodEcho")
            put("exportTimestamp", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
            put("settingsMode", prefs.settingsMode.name)
            put("bufferLengthSeconds", prefs.bufferLengthSeconds)
            put("overlapSeconds", prefs.overlapSeconds)
            put("sampleRate", prefs.sampleRate)
            put("liveTranscribe", prefs.liveTranscribe)
            put("skipSilence", prefs.skipSilence)
            put("autoDeleteAudio", prefs.autoDeleteAudio)
            put("selectedLanguage", prefs.selectedLanguage)
            put("selectedSttModel", prefs.selectedSttModel)
            put("selectedAiModel", prefs.selectedAiModel)
            put("cloudApiKey", prefs.cloudApiKey)
            put("cloudModelName", prefs.cloudModelName)
            put("audioFolderPath", prefs.audioFolderPath)
            put("transcriptionFolderPath", prefs.transcriptionFolderPath)
            put("notesFolderPath", prefs.notesFolderPath)
            put("logsFolderPath", prefs.logsFolderPath)
            put("baseFolderPath", prefs.baseFolderPath)
            put("activeTheme", prefs.activeTheme)
            put("appLanguage", prefs.appLanguage)
            put("isWebSearchEnabled", prefs.isWebSearchEnabled)
            put("customSystemPrompt", prefs.customSystemPrompt)
            put("aiLanguage", prefs.aiLanguage)
            put("fontScale", prefs.fontScale.toDouble())
            put("fontMode", prefs.fontMode)
            put("customThemeJson", prefs.customThemeJson)
            put("customAiModels", prefs.customAiModels)
            put("ttsSpeed", prefs.ttsSpeed.toDouble())
            put("pinnedCategories", JSONArray(prefs.pinnedCategories))
            put("customCategories", JSONArray(prefs.customCategories))
            put("downloadedModels", JSONArray(prefs.downloadedModels))
            put("logFileExtension", prefs.logFileExtension)
            put("huggingFaceToken", prefs.huggingFaceToken)
            put("recorderLayoutMode", prefs.recorderLayoutMode)
            put("defaultAbortSelection", prefs.defaultAbortSelection)
            put("showLogsTab", prefs.showLogsTab)
            put("showAiTab", prefs.showAiTab)
            put("homeTab", prefs.homeTab)
            put("showDashboardTab", prefs.showDashboardTab)
            put("showRecorderTab", prefs.showRecorderTab)
            put("showMediaTab", prefs.showMediaTab)
            put("showAssistantTab", prefs.showAssistantTab)
            put("tabOrder", prefs.tabOrder)
            put("botCustomActions", prefs.botCustomActions)
            put("botInterruptionNoiseGateDb", prefs.botInterruptionNoiseGateDb)
            put("logGroupingGapMinutes", prefs.logGroupingGapMinutes)
            put("logFilesPerPage", prefs.logFilesPerPage)
        }
        return root.toString(2)
    }

    fun importFromJson(jsonString: String): UserPreferences? {
        return try {
            val root = JSONObject(jsonString)

            fun optStringSet(key: String, default: Set<String>): Set<String> {
                val arr = root.optJSONArray(key) ?: return default
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                return set
            }

            val modeStr = root.optString("settingsMode", SettingsMode.SMART.name)
            val mode = runCatching { SettingsMode.valueOf(modeStr) }.getOrDefault(SettingsMode.SMART)

            UserPreferences(
                settingsMode = mode,
                bufferLengthSeconds = root.optInt("bufferLengthSeconds", 15),
                overlapSeconds = root.optInt("overlapSeconds", 3),
                sampleRate = root.optInt("sampleRate", UserPreferences.QUALITY_TINIEST),
                liveTranscribe = root.optBoolean("liveTranscribe", true),
                skipSilence = root.optBoolean("skipSilence", true),
                autoDeleteAudio = root.optBoolean("autoDeleteAudio", false),
                selectedLanguage = root.optString("selectedLanguage", "hi-IN"),
                selectedSttModel = root.optString("selectedSttModel", UserPreferences.STT_MODEL_VOSK_HINDI),
                selectedAiModel = root.optString("selectedAiModel", UserPreferences.AI_MODEL_LOCAL_TINY),
                cloudApiKey = root.optString("cloudApiKey", ""),
                cloudModelName = root.optString("cloudModelName", "gemini-1.5-flash"),
                audioFolderPath = root.optString("audioFolderPath", ""),
                transcriptionFolderPath = root.optString("transcriptionFolderPath", ""),
                notesFolderPath = root.optString("notesFolderPath", ""),
                logsFolderPath = root.optString("logsFolderPath", ""),
                baseFolderPath = root.optString("baseFolderPath", ""),
                activeTheme = root.optString("activeTheme", "MIDNIGHT_BLUE"),
                appLanguage = root.optString("appLanguage", "en"),
                isWebSearchEnabled = root.optBoolean("isWebSearchEnabled", true),
                customSystemPrompt = root.optString("customSystemPrompt", ""),
                aiLanguage = root.optString("aiLanguage", "auto"),
                fontScale = root.optDouble("fontScale", 1.0).toFloat(),
                fontMode = root.optString("fontMode", "DEFAULT"),
                customThemeJson = root.optString("customThemeJson", ""),
                customAiModels = root.optString("customAiModels", ""),
                ttsSpeed = root.optDouble("ttsSpeed", 1.0).toFloat(),
                pinnedCategories = optStringSet("pinnedCategories", setOf("Urgent", "Shopping")),
                customCategories = optStringSet("customCategories", emptySet()),
                downloadedModels = optStringSet("downloadedModels", emptySet()),
                logFileExtension = root.optString("logFileExtension", "txt"),
                huggingFaceToken = root.optString("huggingFaceToken", ""),
                recorderLayoutMode = root.optString("recorderLayoutMode", "BOTH_TABS"),
                defaultAbortSelection = root.optString("defaultAbortSelection", "ALL_FILES"),
                showLogsTab = root.optBoolean("showLogsTab", false),
                showAiTab = root.optBoolean("showAiTab", true),
                homeTab = root.optString("homeTab", "DUMB_ASSISTANT"),
                showDashboardTab = root.optBoolean("showDashboardTab", true),
                showRecorderTab = root.optBoolean("showRecorderTab", true),
                showMediaTab = root.optBoolean("showMediaTab", true),
                showAssistantTab = root.optBoolean("showAssistantTab", true),
                tabOrder = root.optString("tabOrder", "DUMB_ASSISTANT,DASHBOARD,RECORDER,AI,MEDIA,LOGS,SETTINGS"),
                botCustomActions = root.optString("botCustomActions", "[]"),
                botInterruptionNoiseGateDb = root.optInt("botInterruptionNoiseGateDb", -20),
                logGroupingGapMinutes = root.optInt("logGroupingGapMinutes", 5),
                logFilesPerPage = root.optInt("logFilesPerPage", 10)
            )
        } catch (e: Exception) {
            AppLogger.e("SETTINGS_IMPORT", "Failed to parse settings JSON: ${e.message}", e)
            null
        }
    }

    fun shareSettingsJson(context: Context, jsonString: String) {
        try {
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val exportFile = File(exportDir, "good_echo_settings_$dateStamp.json")
            exportFile.writeText(jsonString)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "GoodEcho Beta Settings Export ($dateStamp)")
                putExtra(Intent.EXTRA_TEXT, jsonString)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export GoodEcho Settings JSON"))
            AppLogger.i("SETTINGS_EXPORT", "Settings exported to ${exportFile.absolutePath} and shared.")
        } catch (e: Exception) {
            AppLogger.e("SETTINGS_EXPORT", "Error sharing settings: ${e.message}", e)
        }
    }
}
