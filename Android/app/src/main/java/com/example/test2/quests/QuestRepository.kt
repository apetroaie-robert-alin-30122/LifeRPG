package com.example.test2.quests

object QuestRepository {
    val all = listOf(
        Quest(
            id = "walk_250",
            title = "Walk 250 meters",
            description = "Take a short walk and cover 250 meters.",
            target = 250f,
            unit = "m",
            type = QuestType.WALKING,
            xpReward = 50
        ),
        Quest(
            id = "jog_500",
            title = "Jog 500 meters",
            description = "Pick up the pace and jog 500 meters.",
            target = 500f,
            unit = "m",
            type = QuestType.JOGGING,
            xpReward = 75
        ),
        Quest(
            id = "situps_20",
            title = "Do 20 sit-ups",
            description = "Complete 20 sit-ups.",
            target = 20f,
            unit = "reps",
            type = QuestType.SITUPS,
            xpReward = 60
        ),
        Quest(
            id = "pushups_20",
            title = "Do 20 push-ups",
            description = "Complete 20 push-ups.",
            target = 20f,
            unit = "reps",
            type = QuestType.PUSHUPS,
            xpReward = 60
        ),
        Quest(
            id = "read_book",
            title = "Read a book",
            description = "Pick up a book and read it.",
            target = 1f,
            unit = "book",
            type = QuestType.HONOR,
            xpReward = 100,
            requiresInput = true,
            inputFields = listOf("Book Title", "Author")
        ),
        Quest(
            id = "find_tree",
            title = "Find a tree",
            description = "Go outside and take a picture of a tree.",
            target = 1f,
            unit = "photo",
            type = QuestType.PHOTO,
            xpReward = 50
        )
    )
}