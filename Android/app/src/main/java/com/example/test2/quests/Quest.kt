package com.example.test2.quests

enum class QuestType {
    WALKING,
    JOGGING,
    SITUPS,
    PUSHUPS,
    HONOR,
    PHOTO
}
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val target: Float,
    val unit: String,
    val type: QuestType,
    val requiresInput: Boolean = false,
    val inputFields: List<String> = emptyList()
)