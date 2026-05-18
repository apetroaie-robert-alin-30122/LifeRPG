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
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val usernameError by viewModel.usernameError.collectAsState()
    val navigateToProfile by viewModel.navigateToProfile.collectAsState()
    LaunchedEffect(emailError) {
        android.util.Log.d("LoginScreen", "emailError changed: $emailError")
    }
    LaunchedEffect(passwordError) {
        android.util.Log.d("LoginScreen", "passwordError changed: $passwordError")
    }

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
            onValueChange = { username = it; viewModel.clearUsernameError() },
            label = { Text("Username") },
            isError = usernameError != null,
            modifier = Modifier.fillMaxWidth()
        )
        usernameError?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearEmailError() },
            label = { Text("Email") },
            isError = emailError != null,
            modifier = Modifier.fillMaxWidth()
        )
        emailError?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearPasswordError() },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null,
            modifier = Modifier.fillMaxWidth()
        )
        passwordError?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.register(email, password, username) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
        TextButton(onClick = {
            viewModel.clearErrors()
            onNavigateToLogin()
        }) {
            Text("Already have an account? Login")
        }
    }
}