package com.example.test2.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.test2.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(viewModel: AuthViewModel) {
    val profile by viewModel.profile.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (profile == null) {
            CircularProgressIndicator()
        } else {
            Text(profile!!.username, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text("Level ${profile!!.level}", style = MaterialTheme.typography.titleLarge)
            Text("XP: ${profile!!.experience}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}