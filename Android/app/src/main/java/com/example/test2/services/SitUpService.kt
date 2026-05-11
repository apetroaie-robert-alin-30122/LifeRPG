package com.example.test2.services

class SitUpService : RepDetectionService() {
    override val targetReps = 20
    override val channelId = "situp_quest_channel"
    override val questTitle = "Sit-Up Quest"
    override val notificationId = 5
    override val progressAction = "SITUP_PROGRESS"
    override val completeAction = "SITUP_COMPLETE"
    override val motionThreshold = 5.5f
}