package com.echo.recorder.data

enum class SettingsMode(val label: String) {
    SMART("Smart Mode"),
    ADVANCED("Advanced Mode")
}

enum class SttCapability(val label: String) {
    LIVE_ONLY("Live Mic Only"),
    FILE_ONLY("Recorded Audio Files"),
    BOTH("Live & Files")
}

sealed class ModelValidationResult {
    object Valid : ModelValidationResult()
    data class IncompatibleMode(val message: String, val suggestedModelId: String) : ModelValidationResult()
    data class MissingDownload(val message: String, val modelId: String) : ModelValidationResult()
    data class MissingApiKey(val message: String) : ModelValidationResult()
    data class ServiceUnavailable(val message: String) : ModelValidationResult()
}

data class SttModelOption(
    val id: String,
    val name: String,
    val tag: String,
    val description: String,
    val capability: SttCapability = SttCapability.FILE_ONLY,
    val isRecommended: Boolean = false,
    val sizeText: String = "Built-in",
    val downloadUrl: String? = null
)

data class AiModelOption(
    val id: String,
    val name: String,
    val tag: String,
    val description: String,
    val sizeText: String = "Built-in",
    val downloadUrl: String? = null,
    val isApi: Boolean = false,
    val isRecommended: Boolean = false
)

data class UserPreferences(
    val settingsMode: SettingsMode = SettingsMode.SMART,
    val bufferLengthSeconds: Int = 15, // Default to smaller 15s chunks
    val overlapSeconds: Int = 3,       // Default 3s overlap
    val sampleRate: Int = 8000,        // 8000 Hz = Tiniest Quality (Default from Good-Echo)
    val liveTranscribe: Boolean = true,// Live transcribe enabled by default
    val skipSilence: Boolean = true,   // Voice Activity Detection / skip silence
    val autoDeleteAudio: Boolean = false,
    val selectedLanguage: String = "hi-IN", // Hindi / Hinglish
    val selectedSttModel: String = STT_MODEL_GOOGLE_SPEECH,
    val selectedAiModel: String = AI_MODEL_LOCAL_TINY,
    val cloudApiKey: String = "",
    val cloudModelName: String = "gemini-1.5-flash",
    val audioFolderPath: String = "",
    val transcriptionFolderPath: String = "",
    val notesFolderPath: String = "",
    val logsFolderPath: String = "",
    val baseFolderPath: String = "",
    val lastExportSourcePath: String = "",
    val activeTheme: String = "MIDNIGHT_BLUE",
    val appLanguage: String = "en", // "en" for English, "hi" for Hindi
    val isWebSearchEnabled: Boolean = true,
    val customSystemPrompt: String = "",
    val aiLanguage: String = "auto", // "auto", "hi", "en"
    val fontScale: Float = 1.0f,
    val fontMode: String = "DEFAULT", // "DEFAULT", "THIN", "BOLD"
    val customThemeJson: String = "",
    val customAiModels: String = "",
    val ttsSpeed: Float = 1.0f,
    val pinnedCategories: Set<String> = setOf("Urgent", "Shopping"),
    val customCategories: Set<String> = emptySet(),
    val downloadedModels: Set<String> = emptySet(),
    val logFileExtension: String = "log", // "log", "txt", "md"
    val huggingFaceToken: String = "",
    val recorderLayoutMode: String = "BOTH_TABS", // "BOTH_TABS", "DEDICATED_TAB_ONLY", "BOTTOM_MEDIA_ONLY"
    val defaultAbortSelection: String = "ALL_FILES", // "ALL_FILES", "LAST_FILE_ONLY"
    val showLogsTab: Boolean = false, // Hidden by default for basic users
    val showAiTab: Boolean = true // Visible by default for basic users
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """You are Echo AI, an intelligent personal assistant and task extraction expert.
Objectives:
1. Analyze spoken audio transcripts, notes, and user messages in Hindi, Hinglish, and English.
2. Group tasks into existing categories or dynamically create logical new category groups when relevant.
3. Extract nested attributes (varieties like green/yellow/red, sizes, quantities, specifications) as hierarchical child tasks.
4. Extract temporal deadlines (e.g., today, tomorrow, this evening, specific times).
5. For entities, produce clean, singular-language names without confusing bilingual duplicates (e.g., output 'टमाटर' for Hindi or 'Tomato' for English; never 'टमाटर(tamatar(tomato))').
6. Avoid emojis in task titles unless explicitly requested. Provide concise, direct, and actionable answers."""

        const val QUALITY_TINIEST = 8000    // 8 kHz (lowest file size, speech optimized)
        const val QUALITY_SPEECH = 16000   // 16 kHz (standard STT)
        const val QUALITY_STANDARD = 22050 // 22.05 kHz
        const val QUALITY_HIGH = 44100     // 44.1 kHz (CD quality)

        // STT Models
        const val STT_MODEL_GOOGLE_SPEECH = "google_speech"     // Live mic only (offline pack supported)
        const val STT_MODEL_VOSK_HINDI = "vosk_hindi"          // Real offline model ✅ (Files & Fallback)
        const val STT_MODEL_VOSK_HINDI_LARGE = "vosk_hindi_large" // Real offline 1.5GB full acoustic model
        const val STT_MODEL_SHERPA_ONNX_HINDI = "sherpa_onnx_hindi" // SOTA Zipformer transducer Hindi
        const val STT_MODEL_VOSK_EN_IN = "vosk_en_in"          // Vosk Indian English ✅ (Files)
        const val STT_MODEL_WHISPER_BASE = "whisper_base"      // Whisper Base Multilingual
        const val STT_MODEL_WHISPER_LARGE = "cloud_whisper_large" // Whisper Large-v3
        const val STT_MODEL_CLOUD_GEMINI = "cloud_gemini"      // Gemini 1.5 Flash (Both Live & Files)
        const val STT_MODEL_WHISPER_TINY = "whisper_tiny"     // Legacy ID reference
        const val STT_MODEL_INDIC_CONFORMER = "indic_conformer" // Legacy ID reference

        // AI Note & Task Extractor Models
        const val AI_MODEL_LOCAL_TINY = "local_tiny"
        const val AI_MODEL_LOCAL_GEMMA_3_1B = "local_gemma3_1b"
        const val AI_MODEL_LOCAL_GEMMA_3_4B = "local_gemma3_4b"
        const val AI_MODEL_LOCAL_GEMMA_3_12B = "local_gemma3_12b"
        const val AI_MODEL_LOCAL_GEMMA = "local_gemma"
        const val AI_MODEL_LOCAL_LLAMA = "local_llama"
        const val AI_MODEL_CLOUD_GEMINI = "cloud_gemini"
        const val AI_MODEL_CLOUD_OPENAI = "cloud_openai"

        // Genuine, functional STT models on Android
        val AVAILABLE_STT_MODELS = listOf(
            SttModelOption(
                id = STT_MODEL_GOOGLE_SPEECH,
                name = "Google Speech Recognizer",
                tag = "LIVE STREAMING ONLY",
                description = "Native Android live speech recognition. Ultra-responsive real-time mic stream decoding in Hindi, Hinglish, & English. Can run offline if offline speech pack is installed in Android Speech Services.",
                capability = SttCapability.LIVE_ONLY,
                isRecommended = true,
                sizeText = "Built-in"
            ),
            SttModelOption(
                id = STT_MODEL_VOSK_HINDI,
                name = "Vosk Hindi Small (Offline)",
                tag = "TRULY OFFLINE • 45 MB",
                description = "Alpha Cephei Vosk small Hindi model. 100% on-device neural STT — transcribes saved WAV recordings without any internet or API key. Fast and lightweight.",
                capability = SttCapability.FILE_ONLY,
                isRecommended = true,
                sizeText = "45 MB",
                downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip"
            ),
            SttModelOption(
                id = STT_MODEL_VOSK_HINDI_LARGE,
                name = "Vosk Hindi Full / Large (Offline)",
                tag = "HIGH ACCURACY • 1.5 GB",
                description = "Full-scale Alpha Cephei Hindi acoustic model (vosk-model-hi-0.22). Massive vocabulary, vastly superior recognition for rapid, formal, or accented Hindi.",
                capability = SttCapability.FILE_ONLY,
                isRecommended = true,
                sizeText = "1.5 GB",
                downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-hi-0.22.zip"
            ),
            SttModelOption(
                id = STT_MODEL_SHERPA_ONNX_HINDI,
                name = "Sherpa-ONNX Hindi Zipformer",
                tag = "MANUAL ZIP IMPORT • 65 MB",
                description = "Next-gen Kaldi Zipformer transducer model via Sherpa-ONNX. Requires manual model ZIP/tar import via 'Import ZIP' button below.",
                capability = SttCapability.BOTH,
                isRecommended = false,
                sizeText = "65 MB",
                downloadUrl = null
            ),
            SttModelOption(
                id = STT_MODEL_WHISPER_BASE,
                name = "OpenAI Whisper Base (Multilingual)",
                tag = "TRANSFORMER • 145 MB",
                description = "OpenAI Whisper multilingual model for on-device/edge execution. Robust contextual understanding of mixed Hindi-English (Hinglish) audio.",
                capability = SttCapability.FILE_ONLY,
                isRecommended = false,
                sizeText = "145 MB",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
            ),
            SttModelOption(
                id = STT_MODEL_VOSK_EN_IN,
                name = "Vosk Indian English (Offline)",
                tag = "INDIAN ACCENT • 31 MB",
                description = "Alpha Cephei Vosk Indian English acoustic model (en-in). Optimized for Indian English terminology and accents offline.",
                capability = SttCapability.FILE_ONLY,
                isRecommended = false,
                sizeText = "31 MB",
                downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip"
            ),
            SttModelOption(
                id = STT_MODEL_CLOUD_GEMINI,
                name = "Google Gemini 1.5 Flash",
                tag = "CLOUD API • HIGHEST ACCURACY",
                description = "DeepMind Gemini 1.5 Flash audio multimodal API. Transcribes both live audio segments and recorded files with state-of-the-art Hindi/Hinglish accuracy. Requires free API key.",
                capability = SttCapability.BOTH,
                isRecommended = false,
                sizeText = "Cloud API",
                downloadUrl = "https://aistudio.google.com/app/apikey"
            ),
            SttModelOption(
                id = STT_MODEL_WHISPER_LARGE,
                name = "OpenAI Whisper Large-v3",
                tag = "NEAR-HUMAN ACCURACY • API/SERVER",
                description = "OpenAI flagship Whisper Large-v3. Highest benchmark accuracy for noisy background audio and conversational Hindi/Hinglish.",
                capability = SttCapability.BOTH,
                isRecommended = false,
                sizeText = "Cloud / Server API",
                downloadUrl = "https://platform.openai.com/docs/guides/speech-to-text"
            )
        )

        // Genuine AI models for Task & Note Extraction
        val AVAILABLE_AI_MODELS = listOf(
            AiModelOption(
                id = AI_MODEL_LOCAL_TINY,
                name = "On-Device Tiny AI",
                tag = "OFFLINE 0MB • INSTANT",
                description = "Lightweight on-device semantic parser. Categorizes into Work, Shopping, Personal & flags Low-Hanging Fruits without any network or model download.",
                sizeText = "0 MB",
                downloadUrl = null,
                isApi = false,
                isRecommended = true
            ),
            AiModelOption(
                id = AI_MODEL_LOCAL_LLAMA,
                name = "Meta Llama 3.2 (1B/3B)",
                tag = "⭐ 100% UNGATED • 807 MB",
                description = "Meta Llama 3.2 compact edge weights. Free public download without Hugging Face login or token. Fast on-device task extraction & reasoning.",
                sizeText = "807 MB",
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                isApi = false,
                isRecommended = true
            ),
            AiModelOption(
                id = "local_qwen",
                name = "Alibaba Qwen 2.5 (1.5B)",
                tag = "⭐ 100% UNGATED • 1.1 GB",
                description = "Alibaba Qwen 2.5 multilingual model with exceptional Hindi & Hinglish support. Free public download without any login or token.",
                sizeText = "1.1 GB",
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
                isApi = false,
                isRecommended = true
            ),
            AiModelOption(
                id = "local_phi3",
                name = "Microsoft Phi-3.5 Mini",
                tag = "100% UNGATED • 2.2 GB",
                description = "Microsoft high-efficiency edge model with exceptional reasoning and logic capabilities. Free public download.",
                sizeText = "2.2 GB",
                downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
                isApi = false,
                isRecommended = false
            ),
            AiModelOption(
                id = AI_MODEL_LOCAL_GEMMA_3_1B,
                name = "Google Gemma 3 1B Instruct",
                tag = "GATED • HF TOKEN REQUIRED • 1.1 GB",
                description = "Google DeepMind Gemma 3 ultra-compact edge model. Requires entering your free Hugging Face token in Settings (due to Google license acceptance).",
                sizeText = "1.1 GB",
                downloadUrl = "https://huggingface.co/bartowski/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf",
                isApi = false,
                isRecommended = false
            ),
            AiModelOption(
                id = AI_MODEL_LOCAL_GEMMA_3_4B,
                name = "Google Gemma 3 4B Instruct",
                tag = "GATED • HF TOKEN REQUIRED • 3.2 GB",
                description = "Google DeepMind Gemma 3 edge model. Requires entering your free Hugging Face token in Settings (due to Google license acceptance).",
                sizeText = "3.2 GB",
                downloadUrl = "https://huggingface.co/bartowski/gemma-3-4b-it-GGUF/resolve/main/gemma-3-4b-it-Q4_K_M.gguf",
                isApi = false,
                isRecommended = false
            ),
            AiModelOption(
                id = AI_MODEL_LOCAL_GEMMA_3_12B,
                name = "Google Gemma 3 12B Instruct",
                tag = "GATED • 8.5 GB • FRONTIER",
                description = "Flagship Google DeepMind Gemma 3 open model for local Ollama host or high-capacity workstations.",
                sizeText = "8.5 GB",
                downloadUrl = "https://huggingface.co/bartowski/gemma-3-12b-it-GGUF/resolve/main/gemma-3-12b-it-Q4_K_M.gguf",
                isApi = false,
                isRecommended = false
            ),
            AiModelOption(
                id = AI_MODEL_LOCAL_GEMMA,
                name = "Google Gemma 2 2B Instruct",
                tag = "GATED • HF TOKEN REQUIRED • 1.6 GB",
                description = "Google DeepMind open weights. Requires entering your free Hugging Face token in Settings or importing downloaded GGUF file.",
                sizeText = "1.6 GB",
                downloadUrl = "https://huggingface.co/google/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it.Q4_K_M.gguf",
                isApi = false,
                isRecommended = false
            ),
            AiModelOption(
                id = AI_MODEL_CLOUD_GEMINI,
                name = "Google Gemini 1.5 Flash",
                tag = "CLOUD API • HIGH ACCURACY",
                description = "Optional cloud transcription and summarization if an external Gemini API key is provided.",
                sizeText = "Cloud API",
                downloadUrl = "https://aistudio.google.com/app/apikey",
                isApi = true,
                isRecommended = false
            ),
            AiModelOption(
                id = AI_MODEL_CLOUD_OPENAI,
                name = "OpenAI GPT-4o-mini",
                tag = "CLOUD API • HIGH ACCURACY",
                description = "Optional cloud API for task extraction with an OpenAI API key.",
                sizeText = "Cloud API",
                downloadUrl = "https://platform.openai.com/api-keys",
                isApi = true,
                isRecommended = false
            )
        )

        fun validateModelSelection(
            prefs: UserPreferences,
            isRecognitionAvailable: Boolean,
            isModelDownloaded: (String) -> Boolean
        ): ModelValidationResult {
            val modelOption = AVAILABLE_STT_MODELS.find { it.id == prefs.selectedSttModel }
                ?: return ModelValidationResult.IncompatibleMode("Selected model not found.", STT_MODEL_GOOGLE_SPEECH)

            if (prefs.liveTranscribe) {
                // Mode is LIVE
                if (modelOption.capability == SttCapability.FILE_ONLY) {
                    return ModelValidationResult.IncompatibleMode(
                        message = "\"${modelOption.name}\" only works on recorded files, not live streaming. For live mode, please use Google Speech Recognizer or Gemini Flash.",
                        suggestedModelId = STT_MODEL_GOOGLE_SPEECH
                    )
                }
                if (modelOption.id == STT_MODEL_GOOGLE_SPEECH && !isRecognitionAvailable) {
                    return ModelValidationResult.ServiceUnavailable("Google Speech Recognition service is not available on this device.")
                }
                if (modelOption.id == STT_MODEL_CLOUD_GEMINI && prefs.cloudApiKey.isBlank()) {
                    return ModelValidationResult.MissingApiKey("Gemini 1.5 Flash requires a free Gemini API key in Settings.")
                }
            } else {
                // Mode is FILE
                if (modelOption.capability == SttCapability.LIVE_ONLY) {
                    return ModelValidationResult.IncompatibleMode(
                        message = "\"${modelOption.name}\" requires live microphone input and cannot transcribe recorded files. Please use Vosk Hindi (offline) or Gemini Flash for file mode.",
                        suggestedModelId = STT_MODEL_VOSK_HINDI
                    )
                }
                if (modelOption.id in listOf(STT_MODEL_VOSK_HINDI, STT_MODEL_VOSK_EN_IN) && !isModelDownloaded(modelOption.id)) {
                    return ModelValidationResult.MissingDownload(
                        message = "\"${modelOption.name}\" is not downloaded yet. Please tap 'Download Model' in Settings.",
                        modelId = modelOption.id
                    )
                }
                if (modelOption.id == STT_MODEL_CLOUD_GEMINI && prefs.cloudApiKey.isBlank()) {
                    return ModelValidationResult.MissingApiKey("Gemini 1.5 Flash requires a Gemini API key in Settings.")
                }
            }
            return ModelValidationResult.Valid
        }

        fun getModelCapability(modelId: String): SttCapability {
            return AVAILABLE_STT_MODELS.find { it.id == modelId }?.capability ?: SttCapability.FILE_ONLY
        }

        fun isSttModelRequirementFulfilled(
            context: android.content.Context,
            modelId: String,
            cloudApiKey: String,
            downloadedModels: Set<String>
        ): Boolean {
            return when (modelId) {
                STT_MODEL_GOOGLE_SPEECH -> android.speech.SpeechRecognizer.isRecognitionAvailable(context)
                STT_MODEL_VOSK_HINDI -> com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME)
                STT_MODEL_VOSK_HINDI_LARGE -> downloadedModels.contains(modelId) || com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_HINDI_LARGE)
                STT_MODEL_SHERPA_ONNX_HINDI -> downloadedModels.contains(modelId) || java.io.File(com.echo.recorder.util.AppStorageManager.getModelsDirectory(context), "sherpa-onnx-hindi").exists()
                STT_MODEL_VOSK_EN_IN -> com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN)
                STT_MODEL_WHISPER_BASE -> {
                    val dir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(context)
                    val files = dir.listFiles() ?: emptyArray()
                    val hasValidBase = files.any { f ->
                        (f.name.equals("whisper-base.bin", true) || f.name.equals("whisper_base.bin", true) || f.name.equals("ggml-base.bin", true)) && f.length() > 50 * 1024 * 1024L
                    }
                    hasValidBase || (downloadedModels.contains(modelId) && hasValidBase)
                }
                STT_MODEL_WHISPER_TINY -> {
                    val dir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(context)
                    val files = dir.listFiles() ?: emptyArray()
                    val hasValidTiny = files.any { f ->
                        (f.name.equals("whisper-tiny.bin", true) || f.name.equals("whisper_tiny.bin", true) || f.name.equals("ggml-tiny.bin", true)) && f.length() > 25 * 1024 * 1024L
                    }
                    hasValidTiny || (downloadedModels.contains(modelId) && hasValidTiny)
                }
                STT_MODEL_CLOUD_GEMINI, STT_MODEL_WHISPER_LARGE -> cloudApiKey.isNotBlank()
                else -> false
            }
        }

        fun isAiModelRequirementFulfilled(
            context: android.content.Context,
            modelId: String,
            cloudApiKey: String,
            downloadedModels: Set<String>
        ): Boolean {
            if (modelId == AI_MODEL_LOCAL_TINY) return true
            if (modelId in listOf(AI_MODEL_CLOUD_GEMINI, AI_MODEL_CLOUD_OPENAI)) return cloudApiKey.isNotBlank()
            if (downloadedModels.contains(modelId)) return true

            val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(context)
            val files = modelsDir.listFiles() ?: emptyArray()

            return when (modelId) {
                AI_MODEL_LOCAL_GEMMA_3_1B -> {
                    files.any { it.isFile && it.name.endsWith(".gguf", true) && it.name.contains("1b", true) && it.name.contains("gemma", true) } ||
                    files.any { it.name.equals("${modelId}.gguf", true) || it.name.contains("gemma-3-1b", true) }
                }
                AI_MODEL_LOCAL_GEMMA_3_4B -> {
                    files.any { it.isFile && it.name.endsWith(".gguf", true) && it.name.contains("4b", true) && it.name.contains("gemma", true) } ||
                    files.any { it.name.equals("${modelId}.gguf", true) || it.name.contains("gemma-3-4b", true) }
                }
                AI_MODEL_LOCAL_GEMMA_3_12B -> {
                    files.any { it.isFile && it.name.endsWith(".gguf", true) && it.name.contains("12b", true) && it.name.contains("gemma", true) } ||
                    files.any { it.name.equals("${modelId}.gguf", true) || it.name.contains("gemma-3-12b", true) }
                }
                AI_MODEL_LOCAL_GEMMA -> {
                    files.any { it.isFile && it.name.endsWith(".gguf", true) && it.name.contains("gemma", true) && !it.name.contains("gemma-3", true) && !it.name.contains("gemma3", true) } ||
                    files.any { it.name.equals("${modelId}.gguf", true) }
                }
                AI_MODEL_LOCAL_LLAMA -> {
                    files.any { it.isFile && it.name.endsWith(".gguf", true) && it.name.contains("llama", true) } ||
                    files.any { it.name.equals("${modelId}.gguf", true) }
                }
                "local_qwen" -> {
                    files.any { it.isFile && it.name.endsWith(".gguf", true) && it.name.contains("qwen", true) } ||
                    files.any { it.name.equals("local_qwen.gguf", true) }
                }
                "local_phi3" -> {
                    files.any { it.isFile && it.name.endsWith(".gguf", true) && it.name.contains("phi", true) } ||
                    files.any { it.name.equals("local_phi3.gguf", true) }
                }
                else -> {
                    files.any { it.name.equals("${modelId}.gguf", true) } || files.any { it.isFile && it.name.endsWith(".gguf", true) }
                }
            }
        }
    }
}

