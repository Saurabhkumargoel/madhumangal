package com.echo.recorder.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class BotActionType(val title: String, val description: String) {
    SPEAK_TIME("Current Time", "Announces the current time via voice"),
    SPEAK_DATE("Current Date", "Announces today's day and date via voice"),
    SPEAK_BATTERY("Battery Status", "Announces device battery level"),
    WAKE_BOT("Activate GoodBot", "Triggers GoodBot active 15s listening window"),
    TOGGLE_RECORD("Toggle Recording", "Starts or stops continuous recording"),
    ASK_AI_PROMPT("Custom AI Prompt", "Sends a preset prompt to AI assistant"),
    CLEAR_TRANSCRIPT("Clear Transcript", "Clears the active live transcript view")
}

enum class ActionPosition(val title: String) {
    HEADER("Header (Top Bar)"),
    QUICK_ACTIONS("Quick Actions Grid"),
    BOTTOM("Bottom Action Area"),
    FLOATING("Floating (Screen Edge)")
}

enum class ActionSize(val title: String) {
    SMALL("Small (Chip)"),
    MEDIUM("Medium (Standard Button)"),
    LARGE("Large (Prominent Card)")
}

data class BotCustomAction(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val actionType: BotActionType,
    val promptText: String = "",
    val position: ActionPosition = ActionPosition.QUICK_ACTIONS,
    val size: ActionSize = ActionSize.MEDIUM,
    val colorHex: String = "#3B82F6",
    val iconName: String = "Sparkles"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("label", label)
            put("actionType", actionType.name)
            put("promptText", promptText)
            put("position", position.name)
            put("size", size.name)
            put("colorHex", colorHex)
            put("iconName", iconName)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): BotCustomAction {
            return BotCustomAction(
                id = obj.optString("id", UUID.randomUUID().toString()),
                label = obj.optString("label", "Custom Action"),
                actionType = runCatching { BotActionType.valueOf(obj.optString("actionType")) }.getOrDefault(BotActionType.SPEAK_TIME),
                promptText = obj.optString("promptText", ""),
                position = runCatching { ActionPosition.valueOf(obj.optString("position")) }.getOrDefault(ActionPosition.QUICK_ACTIONS),
                size = runCatching { ActionSize.valueOf(obj.optString("size")) }.getOrDefault(ActionSize.MEDIUM),
                colorHex = obj.optString("colorHex", "#3B82F6"),
                iconName = obj.optString("iconName", "Sparkles")
            )
        }

        fun parseList(jsonStr: String): List<BotCustomAction> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<BotCustomAction>()
                for (i in 0 until arr.length()) {
                    list.add(fromJson(arr.getJSONObject(i)))
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun serializeList(list: List<BotCustomAction>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }
    }
}
