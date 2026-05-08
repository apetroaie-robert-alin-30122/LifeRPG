package com.example.test2.quests

enum class QuestType {
    WALKING,
    // Add future types here: RUNNING, EXERCISE, ETC.
}

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val target: Float,
    val unit: String,
    val type: QuestType
)