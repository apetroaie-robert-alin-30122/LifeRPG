package com.example.test2.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class QuestProgress(
    val questId: String,
    val current: Float = 0f,
    val isComplete: Boolean = false,
    val isActive: Boolean = false
)

class QuestViewModel : ViewModel() {
    private val _progress = MutableStateFlow<Map<String, QuestProgress>>(emptyMap())
    val progress: StateFlow<Map<String, QuestProgress>> = _progress

    private val _questInputs = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val questInputs: StateFlow<Map<String, Map<String, String>>> = _questInputs

    fun startQuest(questId: String) {
        _progress.value = _progress.value + (questId to QuestProgress(questId, isActive = true))
    }

    fun updateProgress(questId: String, current: Float) {
        val existing = _progress.value[questId] ?: return
        _progress.value = _progress.value + (questId to existing.copy(current = current))
    }

    fun incrementRep(questId: String, target: Float) {
        val existing = _progress.value[questId] ?: return
        val newCount = (existing.current + 1f).coerceAtMost(target)
        val updated = existing.copy(
            current = newCount,
            isComplete = newCount >= target,
            isActive = newCount < target
        )
        _progress.value = _progress.value + (questId to updated)
    }

    fun completeQuest(questId: String) {
        val existing = _progress.value[questId] ?: return
        _progress.value = _progress.value + (questId to existing.copy(isComplete = true, isActive = false))
    }

    fun saveInputs(questId: String, inputs: Map<String, String>) {
        _questInputs.value = _questInputs.value + (questId to inputs)
    }
    fun resetQuest(questId: String) {
        _progress.value = _progress.value - questId
    }
}