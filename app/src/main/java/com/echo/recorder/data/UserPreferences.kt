package com.echo.recorder.data

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
    val downloadUrl: String? = null,
    val isApi: Boolean = false,
    val isRecommended: Boolean = false
)

data class UserPreferences(
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
    val downloadedModels: Set<String> = emptySet()
) {
    companion object {
        const val QUALITY_TINIEST = 8000    // 8 kHz (lowest file size, speech optimized)
        const val QUALITY_SPEECH = 16000   // 16 kHz (standard STT)
        const val QUALITY_STANDARD = 22050 // 22.05 kHz
        const val QUALITY_HIGH = 44100     // 44.1 kHz (CD quality)

        // STT Models
        const val STT_MODEL_GOOGLE_SPEECH = "google_speech"     // Live mic only
        const val STT_MODEL_VOSK_HINDI = "vosk_hindi"          // Real offline model ✅ (Files)
        const val STT_MODEL_VOSK_EN_IN = "vosk_en_in"          // Vosk Indian English ✅ (Files)
        const val STT_MODEL_CLOUD_GEMINI = "cloud_gemini"      // Gemini 1.5 Flash (Both Live & Files)
        const val STT_MODEL_WHISPER_TINY = "whisper_tiny"     // OpenAI Whisper Tiny (Files)
        const val STT_MODEL_WHISPER_BASE = "whisper_base"     // OpenAI Whisper Base (Files)
        const val STT_MODEL_INDIC_CONFORMER = "indic_conformer" // AI4Bharat IndicConformer (Files)

        // AI Note & Task Extractor Models
        const val AI_MODEL_LOCAL_TINY = "local_tiny"
        const val AI_MODEL_LOCAL_GEMMA = "local_gemma"
        const val AI_MODEL_LOCAL_LLAMA = "local_llama"
        const val AI_MODEL_CLOUD_GEMINI = "cloud_gemini"
        const val AI_MODEL_CLOUD_OPENAI = "cloud_openai"

        val AVAILABLE_STT_MODELS = listOf(
            SttModelOption(
                id = STT_MODEL_GOOGLE_SPEECH,
                name = "Google Speech Recognizer (Built-in)",
                tag = "RECOMMENDED • LIVE STREAMING ONLY",
                description = "Native Android live speech recognition. Ultra-responsive real-time mic stream decoding in Hindi, Hinglish, & English. Zero download needed.",
                capability = SttCapability.LIVE_ONLY,
                isRecommended = true,
                sizeText = "Built-in"
            ),
            SttModelOption(
                id = STT_MODEL_VOSK_HINDI,
                name = "Vosk Hindi (Offline – Works on Files)",
                tag = "RECOMMENDED • TRULY OFFLINE • 45 MB",
                description = "Alpha Cephei Vosk small Hindi model. 100% on-device neural STT — transcribes saved WAV recordings without any internet or API key. Supports Hindi & Hinglish.",
                capability = SttCapability.FILE_ONLY,
                isRecommended = true,
                sizeText = "45 MB",
                downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip"
            ),
            SttModelOption(
                id = STT_MODEL_CLOUD_GEMINI,
                name = "Google Gemini 1.5 Flash (Cloud API)",
                tag = "CLOUD API • LIVE & FILES • HIGHEST ACCURACY",
                description = "DeepMind Gemini 1.5 Flash audio multimodal API. Transcribes both live audio segments and recorded files with state-of-the-art Hindi/Hinglish accuracy. Requires free API key.",
                capability = SttCapability.BOTH,
                isRecommended = false,
                sizeText = "Cloud API",
                downloadUrl = "https://aistudio.google.com/app/apikey"
            ),
            SttModelOption(
                id = STT_MODEL_VOSK_EN_IN,
                name = "Vosk Indian English (Offline – Works on Files)",
                tag = "OFFLINE • INDIAN ACCENT • 31 MB",
                description = "Alpha Cephei Vosk Indian English acoustic model (en-in). Optimized for Indian English terminology and accents offline.",
                capability = SttCapability.FILE_ONLY,
                isRecommended = false,
                sizeText = "31 MB",
                downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip"
            ),
            SttModelOption(
                id = STT_MODEL_WHISPER_TINY,
                name = "OpenAI Whisper Tiny (Multi-lingual)",
                tag = "LOCAL MODEL • 39 MB",
                description = "OpenAI Whisper Tiny quantized encoder-decoder model (39M parameters). Supports 99 languages including Hindi and English for audio files.",
                capability = SttCapability.FILE_ONLY,
                isRecommended = false,
                sizeText = "39 MB",
                downloadUrl = "https://huggingface.co/openai/whisper-tiny"
            ),
            SttModelOption(
                id = STT_MODEL_WHISPER_BASE,
                name = "OpenAI Whisper Base (High Precision)",
                tag = "LOCAL MODEL • 142 MB",
                description = "OpenAI Whisper Base model (74M parameters). Higher accuracy on complex domain terminology, noise resilience, and conversational Hindi/Hinglish.",
                capability = SttCapability.FILE_ONLY,
                isRecommended = false,
                sizeText = "142 MB",
                downloadUrl = "https://huggingface.co/openai/whisper-base"
            ),
            SttModelOption(
                id = STT_MODEL_INDIC_CONFORMER,
                name = "AI4Bharat IndicConformer (Hindi SOTA)",
                tag = "INDIC SPECIALIZED • 85 MB",
                description = "State-of-the-art acoustic conformer model developed by AI4Bharat (IIT Madras). Fine-tuned on thousands of hours of Indian speech datasets.",
                capability = SttCapability.FILE_ONLY,
                isRecommended = false,
                sizeText = "85 MB",
                downloadUrl = "https://github.com/AI4Bharat/indicwav2vec"
            )
        )

        val AVAILABLE_AI_MODELS = listOf(
            AiModelOption(
                id = AI_MODEL_LOCAL_GEMMA,
                name = "Google Gemma 2B (On-Device LLM)",
                tag = "RECOMMENDED • LOCAL LLM • 1.6 GB",
                description = "Google DeepMind open weights for advanced on-device task intelligence and reasoning. Zero cloud API needed.",
                downloadUrl = "https://huggingface.co/google/gemma-2-2b-it",
                isApi = false,
                isRecommended = true
            ),
            AiModelOption(
                id = AI_MODEL_LOCAL_TINY,
                name = "On-Device Tiny AI (Built-in)",
                tag = "RECOMMENDED • OFFLINE 0MB",
                description = "Lightweight on-device semantic parser. Categorizes into Work, Shopping, Personal & flags Low-Hanging Fruits.",
                downloadUrl = null,
                isApi = false,
                isRecommended = true
            ),
            AiModelOption(
                id = AI_MODEL_LOCAL_LLAMA,
                name = "Meta Llama 3.2 1B (On-Device LLM)",
                tag = "LOCAL LLM • 1.1 GB",
                description = "Meta high-efficiency on-device model for edge reasoning. 100% local on-device inference.",
                downloadUrl = "https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct",
                isApi = false,
                isRecommended = false
            ),
            AiModelOption(
                id = AI_MODEL_CLOUD_GEMINI,
                name = "Google Gemini 1.5 Flash (Cloud API)",
                tag = "CLOUD API • OPTIONAL",
                description = "Optional cloud transcription and summarization if an external Gemini API key is provided.",
                downloadUrl = "https://aistudio.google.com/app/apikey",
                isApi = true,
                isRecommended = false
            ),
            AiModelOption(
                id = AI_MODEL_CLOUD_OPENAI,
                name = "OpenAI GPT-4o-mini (Cloud API)",
                tag = "CLOUD API • OPTIONAL",
                description = "Optional cloud API for task extraction with an OpenAI API key.",
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
                STT_MODEL_VOSK_EN_IN -> com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN)
                STT_MODEL_CLOUD_GEMINI -> cloudApiKey.isNotBlank()
                STT_MODEL_WHISPER_TINY, STT_MODEL_WHISPER_BASE, STT_MODEL_INDIC_CONFORMER -> {
                    downloadedModels.contains(modelId) ||
                    java.io.File(com.echo.recorder.util.AppStorageManager.getModelsDirectory(context), "${modelId}.bin").exists() ||
                    com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME)
                }
                else -> false
            }
        }

        fun isAiModelRequirementFulfilled(
            context: android.content.Context,
            modelId: String,
            cloudApiKey: String,
            downloadedModels: Set<String>
        ): Boolean {
            return when (modelId) {
                AI_MODEL_LOCAL_TINY -> true // Always ready on-device zero-MB built-in parser
                AI_MODEL_LOCAL_GEMMA -> {
                    downloadedModels.contains(modelId) ||
                    java.io.File(com.echo.recorder.util.AppStorageManager.getModelsDirectory(context), "gemma-2-2b-it.gguf").exists() ||
                    java.io.File(com.echo.recorder.util.AppStorageManager.getModelsDirectory(context), "${modelId}.bin").exists()
                }
                AI_MODEL_LOCAL_LLAMA -> {
                    downloadedModels.contains(modelId) ||
                    java.io.File(com.echo.recorder.util.AppStorageManager.getModelsDirectory(context), "llama-3.2-1b.gguf").exists() ||
                    java.io.File(com.echo.recorder.util.AppStorageManager.getModelsDirectory(context), "${modelId}.bin").exists()
                }
                AI_MODEL_CLOUD_GEMINI, AI_MODEL_CLOUD_OPENAI -> cloudApiKey.isNotBlank()
                else -> false
            }
        }
    }
}

