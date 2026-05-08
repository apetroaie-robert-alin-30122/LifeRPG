package com.example.test2

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
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
            val token by viewModel.token.collectAsState()
            val questViewModel: QuestViewModel = viewModel()



            NavHost(navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(viewModel, onNavigateToRegister = { navController.navigate("register") })
                    LaunchedEffect(token) {
                        if (token != null) navController.navigate("profile") { popUpTo(0) }
                    }
                }
                composable("register") {
                    RegisterScreen(viewModel, onNavigateToLogin = { navController.popBackStack() })
                    LaunchedEffect(token) {
                        if (token != null) navController.navigate("profile") { popUpTo(0) }
                    }
                }

                composable("profile") {
                    ProfileScreen(authViewModel = viewModel, questViewModel = questViewModel)
                }
            }
        }
    }
}