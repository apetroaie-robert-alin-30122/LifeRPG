package com.example.test2.screens

import android.content.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.test2.WalkingService
import com.example.test2.quests.Quest
import com.example.test2.quests.QuestType
import com.example.test2.viewmodel.QuestViewModel

@Composable
fun QuestBanner(
    quest: Quest,
    questViewModel: QuestViewModel
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val allProgress by questViewModel.progress.collectAsState()
    val questProgress = allProgress[quest.id]
    val isActive = questProgress?.isActive == true
    val isComplete = questProgress?.isComplete == true
    val current = questProgress?.current ?: 0f
    val progress = (current / quest.target).coerceIn(0f, 1f)

    // Broadcasts for this quest type
    DisposableEffect(quest.id) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    "WALK_PROGRESS" -> {
                        val distance = intent.getFloatExtra("distance", 0f)
                        questViewModel.updateProgress(quest.id, distance)
                    }
                    "WALK_COMPLETE" -> questViewModel.completeQuest(quest.id)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("WALK_PROGRESS")
            addAction("WALK_COMPLETE")
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = { if (!isActive && !isComplete) showDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isComplete) "✓ ${quest.title}" else quest.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (isActive) {
                    Text("Active", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(quest.description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${current.toInt()} / ${quest.target.toInt()} ${quest.unit}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Start Quest?") },
            text = { Text(quest.description) },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    questViewModel.startQuest(quest.id)
                    launchQuestService(context, quest.type)
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun launchQuestService(context: Context, type: QuestType) {
    val serviceIntent = when (type) {
        QuestType.WALKING -> Intent(context, WalkingService::class.java)
        // Future types: QuestType.RUNNING -> Intent(context, RunningService::class.java)
    }
    ContextCompat.startForegroundService(context, serviceIntent)
}