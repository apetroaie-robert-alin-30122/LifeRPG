package com.example.test2.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.shape.RoundedCornerShape

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    questViewModel: QuestViewModel
) {
    val profile by authViewModel.profile.collectAsState()
    val leveledUp by authViewModel.leveledUp.collectAsState()
    val activeQuests by questViewModel.activeQuests.collectAsState()
    val token by authViewModel.token.collectAsState()
    val completedQuests by questViewModel.completedQuests.collectAsState()
    var showingCompleted by remember { mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("ProfileScreen", "LaunchedEffect fired, token: $token")
        if (questViewModel.activeQuests.value.isEmpty()) {
            questViewModel.fetchRandomQuests()
        }
    }



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
                    Spacer(Modifier.height(8.dp))

                    val xpForNextLevel = profile!!.xpForNextLevel
                    val xpIntoLevel = run {
                        var accumulated = 0
                        var xpRequired = 100
                        for (i in 1 until profile!!.level) {
                            accumulated += xpRequired
                            xpRequired = (xpRequired * 1.2).toInt()
                        }
                        profile!!.experience - accumulated
                    }
                    val progressFraction = (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "$xpIntoLevel / $xpForNextLevel XP",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }

                    if (leveledUp) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎉 Level Up! You're now level ${profile!!.level}!",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            showingCompleted = false
                        }) {
                            Text(
                                "Available",
                                color = if (!showingCompleted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        TextButton(onClick = {
                            showingCompleted = true
                            token?.toIntOrNull()?.let { userId ->
                                questViewModel.fetchCompletedQuests(userId)
                            }
                        }) {
                            Text(
                                "Completed",
                                color = if (showingCompleted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (showingCompleted) {
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✓ ${quest.title}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "+${quest.xpReward} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(quest.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(activeQuests) { quest ->
                    QuestBanner(quest = quest, questViewModel = questViewModel, authViewModel = authViewModel)
                }
            }
        }
    }
}