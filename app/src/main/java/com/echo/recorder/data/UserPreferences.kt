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
        const val STT_MODEL_GOOGLE_SPEECH = "google_speech"
        const val STT_MODEL_WHISPER_TINY = "whisper_tiny"
        const val STT_MODEL_WHISPER_BASE = "whisper_base"
        const val STT_MODEL_INDIC_CONFORMER = "indic_conformer"

        // AI Note & Task Extractor Models
        const val AI_MODEL_LOCAL_TINY = "local_tiny"
        const val AI_MODEL_LOCAL_GEMMA = "local_gemma"
        const val AI_MODEL_LOCAL_LLAMA = "local_llama"
        const val AI_MODEL_CLOUD_GEMINI = "cloud_gemini"
        const val AI_MODEL_CLOUD_OPENAI = "cloud_openai"

        val AVAILABLE_STT_MODELS = listOf(
            SttModelOption(
                id = STT_MODEL_GOOGLE_SPEECH,
                name = "Google Speech Recognizer",
                tag = "REAL-TIME STREAMING",
                description = "Native Android speech service with real-time streaming for Hindi (hi-IN) & Hinglish (en-IN).",
                isRecommended = true,
                sizeText = "Built-in"
            ),
            SttModelOption(
                id = STT_MODEL_WHISPER_TINY,
                name = "Whisper Tiny (On-Device)",
                tag = "OFFLINE 39MB",
                description = "OpenAI Whisper Tiny multilingual model running 100% on-device. Private & fast for Hindi/English.",
                isRecommended = false,
                sizeText = "39 MB"
            ),
            SttModelOption(
                id = STT_MODEL_WHISPER_BASE,
                name = "Whisper Base (High Precision)",
                tag = "OFFLINE 74MB",
                description = "Enhanced multilingual transformer with higher accuracy for complex Hinglish vocabulary.",
                isRecommended = false,
                sizeText = "74 MB"
            ),
            SttModelOption(
                id = STT_MODEL_INDIC_CONFORMER,
                name = "AI4Bharat IndicConformer",
                tag = "HINDI OPTIMIZED",
                description = "Conformer-CTC neural model fine-tuned specifically on Indian accents and regional Hindi dialects.",
                isRecommended = false,
                sizeText = "55 MB"
            )
        )

        val AVAILABLE_AI_MODELS = listOf(
            AiModelOption(
                id = AI_MODEL_LOCAL_TINY,
                name = "On-Device Tiny AI (Built-in)",
                tag = "BUILT-IN • OFFLINE",
                description = "Lightweight on-device semantic parser. Categorizes into Work, Shopping, Personal & flags Low-Hanging Fruits.",
                downloadUrl = null,
                isApi = false,
                isRecommended = true
            ),
            AiModelOption(
                id = AI_MODEL_CLOUD_GEMINI,
                name = "Google Gemini 1.5 Flash (Cloud API)",
                tag = "RECOMMENDED • CLOUD AI",
                description = "Transcribes audio and extracts comprehensive summaries and actionable todos using your free Gemini API key.",
                downloadUrl = "https://aistudio.google.com/app/apikey",
                isApi = true,
                isRecommended = true
            ),
            AiModelOption(
                id = AI_MODEL_LOCAL_GEMMA,
                name = "Google Gemma 2B (On-Device LLM)",
                tag = "LOCAL LLM • 1.6 GB",
                description = "Google DeepMind open model for advanced on-device task intelligence. Download via official HuggingFace repository.",
                downloadUrl = "https://huggingface.co/google/gemma-2-2b-it",
                isApi = false
            ),
            AiModelOption(
                id = AI_MODEL_LOCAL_LLAMA,
                name = "Meta Llama 3.2 1B (On-Device LLM)",
                tag = "LOCAL LLM • 1.1 GB",
                description = "Meta high-efficiency on-device model for edge reasoning. Download via official Meta HuggingFace repository.",
                downloadUrl = "https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct",
                isApi = false
            ),
            AiModelOption(
                id = AI_MODEL_CLOUD_OPENAI,
                name = "OpenAI GPT-4o-mini (Cloud API)",
                tag = "CLOUD API • ADVANCED",
                description = "OpenAI API for fast and highly nuanced Hindi/English task extraction with an OpenAI API key.",
                downloadUrl = "https://platform.openai.com/api-keys",
                isApi = true
            )
        )
    }
}
