package com.example.test2

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val viewModel: AuthViewModel = viewModel()
            val token by viewModel.token.collectAsState()

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
                    ProfileScreen(viewModel)
                }
            }
        }
    }
}