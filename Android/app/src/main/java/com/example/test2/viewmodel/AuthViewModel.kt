package com.example.test2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test2.LoginMutation
import com.example.test2.RegisterMutation
import com.example.test2.MeQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.test2.ApolloClientInstance

data class UserProfile(val username: String, val level: Int, val experience: Int)

class AuthViewModel : ViewModel() {

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            val response = ApolloClientInstance.client
                .mutation(RegisterMutation(email = email, password = password, username = username))
                .execute()
            val result = response.data?.register
            if (result?.success == true) {
                _token.value = result.token
            } else {
                _error.value = result?.message
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val response = ApolloClientInstance.client
                .mutation(LoginMutation(email = email, password = password))
                .execute()
            val result = response.data?.login
            if (result?.success == true) {
                _token.value = result.token
            } else {
                _error.value = result?.message
            }
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            val currentToken = _token.value ?: return@launch
            val response = ApolloClientInstance.client
                .query(MeQuery(token = currentToken))
                .execute()
            val me = response.data?.me
            if (me != null) {
                _profile.value = UserProfile(
                    username = me.username,
                    level = me.level,
                    experience = me.experience
                )
            }
        }
    }
}