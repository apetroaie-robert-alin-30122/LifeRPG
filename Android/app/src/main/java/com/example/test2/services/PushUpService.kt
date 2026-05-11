package com.example.test2.services

class PushUpService : RepDetectionService() {
    override val targetReps = 20
    override val channelId = "pushup_quest_channel"
    override val questTitle = "Push-Up Quest"
    override val notificationId = 6
    override val progressAction = "PUSHUP_PROGRESS"
    override val completeAction = "PUSHUP_COMPLETE"
    override val motionThreshold = 4.5f
}