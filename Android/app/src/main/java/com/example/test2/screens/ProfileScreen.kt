package com.example.test2.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.test2.quests.QuestRepository
import com.example.test2.viewmodel.AuthViewModel
import com.example.test2.viewmodel.QuestViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    questViewModel: QuestViewModel
) {
    val profile by authViewModel.profile.collectAsState()

    if (profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(profile!!.username, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Level ${profile!!.level}", style = MaterialTheme.typography.titleLarge)
                    Text("XP: ${profile!!.experience}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(24.dp))
                    Text("Quests", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }
            }

            items(QuestRepository.all) { quest ->
                QuestBanner(quest = quest, questViewModel = questViewModel)
            }
        }
    }
}