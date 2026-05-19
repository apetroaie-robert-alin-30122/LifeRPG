package com.example.test2

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import com.example.test2.screens.LoginScreen
import com.example.test2.screens.RegisterScreen
import com.example.test2.screens.ProfileScreen
import com.example.test2.viewmodel.AuthViewModel
import com.example.test2.viewmodel.QuestViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.test2.screens.CompletedQuestsScreen

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ComposableDestinationInComposeScope")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACTIVITY_RECOGNITION,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ), 100
            )
        }
        setContent {
            val navController = rememberNavController()
            val viewModel: AuthViewModel = viewModel()
            val questViewModel: QuestViewModel = viewModel()
            val isServerReachable by viewModel.isServerReachable.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(
                            viewModel = viewModel,
                            onNavigateToRegister = { navController.navigate("register") },
                            onNavigateToProfile = { navController.navigate("profile") { popUpTo(0) } }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            viewModel = viewModel,
                            onNavigateToLogin = { navController.popBackStack() },
                            onNavigateToProfile = { navController.navigate("profile") { popUpTo(0) } }
                        )
                    }
                    composable("profile") {
                        ProfileScreen(
                            authViewModel = viewModel,
                            questViewModel = questViewModel
                        )
                    }
                    composable("completed") {
                        val token by viewModel.token.collectAsState()
                        val userId = token?.toIntOrNull() ?: return@composable
                        CompletedQuestsScreen(questViewModel = questViewModel, userId = userId)
                    }
                }

                if (!isServerReachable) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Text("Cannot reach server. Please check your connection.")
                    }
                }
            }
        }
    }
}