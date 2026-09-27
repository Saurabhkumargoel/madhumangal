package com.echo.recorder.data

data class SttModelOption(
    val id: String,
    val name: String,
    val tag: String,
    val description: String,
    val isRecommended: Boolean = false,
    val sizeText: String = "Built-in"
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
        const val STT_MODEL_GOOGLE_SPEECH = "google_speech"   // Live mic only (no file transcription)
        const val STT_MODEL_WHISPER_TINY = "whisper_tiny"     // Legacy stub – no native engine
        const val STT_MODEL_WHISPER_BASE = "whisper_base"     // Legacy stub – no native engine
        const val STT_MODEL_INDIC_CONFORMER = "indic_conformer" // Legacy stub – no native engine
        const val STT_MODEL_VOSK_HINDI = "vosk_hindi"          // Real offline model ✅

        // AI Note & Task Extractor Models
        const val AI_MODEL_LOCAL_TINY = "local_tiny"
        const val AI_MODEL_LOCAL_GEMMA = "local_gemma"
        const val AI_MODEL_LOCAL_LLAMA = "local_llama"
        const val AI_MODEL_CLOUD_GEMINI = "cloud_gemini"
        const val AI_MODEL_CLOUD_OPENAI = "cloud_openai"

        val AVAILABLE_STT_MODELS = listOf(
            SttModelOption(
                id = STT_MODEL_VOSK_HINDI,
                name = "Vosk Hindi (Offline – Works on Files)",
                tag = "RECOMMENDED • TRULY OFFLINE • 45 MB",
                description = "Alpha Cephei Vosk small Hindi model. 100% on-device neural STT — transcribes saved WAV recordings without any internet or API key. Supports Hindi & Hinglish.",
                isRecommended = true,
                sizeText = "45 MB"
            ),
            SttModelOption(
                id = STT_MODEL_GOOGLE_SPEECH,
                name = "Google Speech (Live Mic Only)",
                tag = "LIVE STREAMING ONLY",
                description = "Native Android speech service. Works for real-time live microphone only — cannot transcribe saved audio files. No download needed.",
                isRecommended = false,
                sizeText = "Built-in"
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
    }
}
