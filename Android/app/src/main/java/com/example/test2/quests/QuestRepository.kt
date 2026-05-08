package com.example.test2.quests

object QuestRepository {
    val all = listOf(
        Quest(
            id = "walk_250",
            title = "Walk 250 meters",
            description = "Take a short walk and cover 250 meters.",
            target = 250f,
            unit = "m",
            type = QuestType.WALKING
        )
        // Add future quests here
    )
}