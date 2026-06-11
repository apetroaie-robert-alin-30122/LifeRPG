package com.example.test2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Operation
import com.example.test2.CompleteQuestMutation
import com.example.test2.LoginMutation
import com.example.test2.RegisterMutation
import com.example.test2.MeQuery
import com.example.test2.UpdateAvatarMutation
import com.example.test2.quests.Quest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.test2.services.ApolloClientInstance

data class UserProfile(
    val username: String,
    val level: Int,
    val experience: Int,
    val xpForNextLevel: Int,
    val avatar: String
)

class AuthViewModel : ViewModel() {

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError: StateFlow<String?> = _usernameError

    private val _navigateToProfile = MutableStateFlow(false)
    val navigateToProfile: StateFlow<Boolean> = _navigateToProfile

    private val _leveledUp = MutableStateFlow(false)
    val leveledUp: StateFlow<Boolean> = _leveledUp

    private val _isServerReachable = MutableStateFlow(true)
    val isServerReachable: StateFlow<Boolean> = _isServerReachable

    fun clearEmailError() { _emailError.value = null }
    fun clearPasswordError() { _passwordError.value = null }
    fun clearUsernameError() { _usernameError.value = null }

    fun clearErrors() {
        clearEmailError()
        clearPasswordError()
        clearUsernameError()
    }

    fun resetNavigation() {
        _navigateToProfile.value = false
    }

    fun clearLevelUp() {
        _leveledUp.value = false
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val response = ApolloClientInstance.client
                .mutation(LoginMutation(email = email, password = password))
                .execute()
            if (response.exception != null) {
                _isServerReachable.value = false
                return@launch
            }
            _isServerReachable.value = true
            val result = response.data?.login ?: return@launch
            when {
                result.success -> {
                    _token.value = result.token
                    fetchProfile()
                    _navigateToProfile.value = true
                }
                result.message.contains("No account") ->
                    _emailError.value = "User doesn't exist."
                result.message.contains("Incorrect password") ->
                    _passwordError.value = "Wrong password."
            }
        }
    }

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            val response = ApolloClientInstance.client
                .mutation(RegisterMutation(email = email, password = password, username = username))
                .execute()
            if (response.exception != null) {
                _isServerReachable.value = false
                return@launch
            }
            _isServerReachable.value = true
            val result = response.data?.register ?: return@launch
            when {
                result.success -> {
                    _token.value = result.token
                    fetchProfile()
                    _navigateToProfile.value = true
                }

                result.message.contains("email", ignoreCase = true) ->
                    _emailError.value = result.message

                result.message.contains("username", ignoreCase = true) ->
                    _usernameError.value = result.message

                result.message.contains("password", ignoreCase = true) ->
                    _passwordError.value = result.message
            }
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            val currentToken = _token.value ?: return@launch
            val response = ApolloClientInstance.client
                .query(MeQuery(token = currentToken))
                .execute()
            if (response.exception != null) {
                _isServerReachable.value = false
                return@launch
            }
            _isServerReachable.value = true
            val me = response.data?.me ?: return@launch
            _profile.value = UserProfile(
                username = me.username,
                level = me.level,
                experience = me.experience,
                xpForNextLevel = me.xpForNextLevel,
                avatar = me.avatar
            )
        }
    }

    fun completeQuestAndAwardXP(quest: Quest, questViewModel: QuestViewModel) {
        viewModelScope.launch {
            val currentToken = _token.value ?: return@launch
            val userIdInt = currentToken.toIntOrNull() ?: return@launch
            val response = ApolloClientInstance.client
                .mutation(
                    CompleteQuestMutation(
                        userId = userIdInt,
                        questId = quest.id,
                        title = quest.title,
                        description = quest.description,
                        xpReward = quest.xpReward,
                        category = "fitness",
                        difficulty = "Easy",
                        questType = quest.type.name.lowercase(),
                        target = quest.target.toInt()
                    )
                )
                .execute()
            if (response.exception != null) {
                _isServerReachable.value = false
                return@launch
            }
            _isServerReachable.value = true
            val result = response.data?.completeQuest ?: return@launch
            _profile.value = UserProfile(
                username = result.username,
                level = result.level,
                experience = result.experience,
                xpForNextLevel = result.xpForNextLevel,
                avatar = _profile.value?.avatar ?: "default_avatar"
            )
            if (result.leveledUp) _leveledUp.value = true
            if (quest.category == "forged") {
                questViewModel.removeForgedQuest(quest.id)
            } else {
                questViewModel.fetchReplacementQuest(quest.id)
            }
        }
    }
    fun updateAvatar(avatar: String) {
        viewModelScope.launch {

            val userId = _token.value?.toIntOrNull()
                ?: return@launch

            ApolloClientInstance.client
                .mutation(
                    UpdateAvatarMutation(
                        userId = userId,
                        avatar = avatar
                    )
                )
                .execute()

            fetchProfile()
        }
    }

}