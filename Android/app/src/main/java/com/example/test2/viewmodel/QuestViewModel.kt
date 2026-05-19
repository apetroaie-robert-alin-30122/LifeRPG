package com.example.test2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test2.GetCompletedQuestsQuery
import com.example.test2.quests.Quest
import com.example.test2.quests.QuestType
import com.example.test2.services.ApolloClientInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.test2.GetRandomQuestsQuery
import com.example.test2.GetReplacementQuestQuery
import com.apollographql.apollo.api.Optional


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

    private val _activeQuests = MutableStateFlow<List<Quest>>(emptyList())
    val activeQuests: StateFlow<List<Quest>> = _activeQuests

    private val _completedQuests = MutableStateFlow<List<Quest>>(emptyList())
    val completedQuests: StateFlow<List<Quest>> = _completedQuests

    fun fetchRandomQuests() {
        viewModelScope.launch {
            android.util.Log.d("QuestViewModel", "fetchRandomQuests called")
            val response = ApolloClientInstance.client
                .query(GetRandomQuestsQuery(count = Optional.present(7)))
                .execute()
            android.util.Log.d(
                "QuestViewModel",
                "Response: ${response.data}, Errors: ${response.errors}, Exception: ${response.exception}"
            )
            val quests = response.data?.getRandomQuests ?: return@launch
            android.util.Log.d("QuestViewModel", "Quests received: ${quests.size}")
            _activeQuests.value = quests.map { it.toQuest() }
        }
    }

    fun fetchReplacementQuest(completedQuestId: String) {
        viewModelScope.launch {
            val remaining = _activeQuests.value.filter { it.id != completedQuestId }
            val excludeTypes = remaining.map { it.type.name.lowercase() }
            val response = ApolloClientInstance.client
                .query(GetReplacementQuestQuery(excludeTypes = excludeTypes))
                .execute()
            val replacement = response.data?.getReplacementQuest?.toQuest()
            _activeQuests.value = if (replacement != null) remaining + replacement else remaining
        }
    }

    fun fetchCompletedQuests(userId: Int) {
        viewModelScope.launch {
            val response = ApolloClientInstance.client
                .query(GetCompletedQuestsQuery(userId = userId))
                .execute()
            val quests = response.data?.getCompletedQuests ?: return@launch
            _completedQuests.value = quests.map { it.toQuest() }
        }
    }

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

    fun resetQuest(questId: String) {
        _progress.value = _progress.value - questId
    }

    fun saveInputs(questId: String, inputs: Map<String, String>) {
        _questInputs.value = _questInputs.value + (questId to inputs)
    }

    private fun GetRandomQuestsQuery.GetRandomQuest.toQuest(): Quest {
        val questType = this.questType.toQuestType()
        return Quest(
            id = this.id,
            title = this.title,
            description = this.description,
            target = this.target.toFloat(),
            unit = questType.toUnit(),
            type = questType,
            xpReward = this.xpReward,
            requiresInput = questType == QuestType.HONOR,
            inputFields = if (questType == QuestType.HONOR) listOf(
                "Book Title",
                "Author"
            ) else emptyList()
        )
    }

    private fun GetReplacementQuestQuery.GetReplacementQuest.toQuest(): Quest {
        val questType = this.questType.toQuestType()
        return Quest(
            id = this.id,
            title = this.title,
            description = this.description,
            target = this.target.toFloat(),
            unit = questType.toUnit(),
            type = questType,
            xpReward = this.xpReward,
            requiresInput = questType == QuestType.HONOR,
            inputFields = if (questType == QuestType.HONOR) listOf(
                "Book Title",
                "Author"
            ) else emptyList()
        )
    }

    private fun GetCompletedQuestsQuery.GetCompletedQuest.toQuest(): Quest {
        val questType = this.questType.toQuestType()
        return Quest(
            id = this.id,
            title = this.title,
            description = this.description,
            target = this.target.toFloat(),
            unit = questType.toUnit(),
            type = questType,
            xpReward = this.xpReward
        )
    }

    private fun String.toQuestType(): QuestType = when (this) {
        "walking" -> QuestType.WALKING
        "jogging" -> QuestType.JOGGING
        "situps" -> QuestType.SITUPS
        "pushups" -> QuestType.PUSHUPS
        "reading" -> QuestType.HONOR
        "photo" -> QuestType.PHOTO
        else -> QuestType.HONOR
    }

    private fun QuestType.toUnit(): String = when (this) {
        QuestType.WALKING, QuestType.JOGGING -> "m"
        QuestType.SITUPS, QuestType.PUSHUPS -> "reps"
        QuestType.HONOR -> "book"
        QuestType.PHOTO -> "photo"
    }
}