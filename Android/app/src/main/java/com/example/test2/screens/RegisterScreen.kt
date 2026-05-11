package com.example.test2.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.test2.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(viewModel: AuthViewModel, onNavigateToLogin: () -> Unit, onNavigateToProfile: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val navigateToProfile by viewModel.navigateToProfile.collectAsState()

    LaunchedEffect(navigateToProfile) {
        if (navigateToProfile) {
            viewModel.resetNavigation()
            onNavigateToProfile()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; viewModel.clearError() },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearError() },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.register(email, password, username) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
        TextButton(onClick = {
            viewModel.clearError()
            onNavigateToLogin()
        }) {
            Text("Already have an account? Login")
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}