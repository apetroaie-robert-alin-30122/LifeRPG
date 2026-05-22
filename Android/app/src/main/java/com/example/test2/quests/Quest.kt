package com.example.test2.quests

enum class QuestType {
    WALKING,
    JOGGING,
    SITUPS,
    PUSHUPS,
    HONOR,
    PHOTO,
    OTHER
}
data class Quest(
    val id: String,
    val backendId: Int = 0,
    val title: String,
    val description: String,
    val target: Float,
    val unit: String,
    val type: QuestType,
    val xpReward: Int,
    val category: String = "",
    val requiresInput: Boolean = false,
    val inputFields: List<String> = emptyList()
)