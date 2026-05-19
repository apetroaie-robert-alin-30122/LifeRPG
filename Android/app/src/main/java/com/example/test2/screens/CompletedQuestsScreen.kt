package com.example.test2.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.test2.viewmodel.QuestViewModel

@Composable
fun CompletedQuestsScreen(questViewModel: QuestViewModel, userId: Int) {
    val completedQuests by questViewModel.completedQuests.collectAsState()

    LaunchedEffect(Unit) {
        questViewModel.fetchCompletedQuests(userId)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                "Completed Quests",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
        if (completedQuests.isEmpty()) {
            item {
                Text(
                    "No completed quests yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            items(completedQuests) { quest ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("✓ ${quest.title}", style = MaterialTheme.typography.titleMedium)
                            Text("+${quest.xpReward} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(quest.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}