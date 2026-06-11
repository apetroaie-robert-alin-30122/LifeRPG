package com.example.test2.screens

import android.content.*
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.test2.services.JoggingService
import com.example.test2.services.WalkingService
import com.example.test2.quests.Quest
import com.example.test2.quests.QuestType
import com.example.test2.services.PushUpService
import com.example.test2.services.SitUpService
import com.example.test2.viewmodel.AuthViewModel
import com.example.test2.viewmodel.QuestViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun QuestBanner(
    quest: Quest,
    questViewModel: QuestViewModel,
    authViewModel: AuthViewModel,
    isForged: Boolean = false
) {
    val context = LocalContext.current
    var showStartDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    val allProgress by questViewModel.progress.collectAsState()
    val questProgress = allProgress[quest.id]
    val isActive = questProgress?.isActive == true
    val isComplete = questProgress?.isComplete == true
    val current = questProgress?.current ?: 0f
    val progress = (current / quest.target).coerceIn(0f, 1f)

    val isPhysical = quest.type in listOf(
        QuestType.WALKING, QuestType.JOGGING, QuestType.SITUPS, QuestType.PUSHUPS
    )

    val inputValues = remember { quest.inputFields.associateWith { mutableStateOf("") } }

    // Camera setup
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoCaptured by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoCaptured = true
            showCompleteDialog = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createPhotoUri(context, quest.id)
            photoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    DisposableEffect(quest.id) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    "WALK_PROGRESS" -> if (quest.type == QuestType.WALKING)
                        questViewModel.updateProgress(quest.id, intent.getFloatExtra("distance", 0f))
                    "WALK_COMPLETE" -> if (quest.type == QuestType.WALKING) {
                        questViewModel.completeQuest(quest.id)
                        authViewModel.completeQuestAndAwardXP(
                            quest = quest,
                            questViewModel = questViewModel
                        )
                    }
                    "JOG_PROGRESS" -> if (quest.type == QuestType.JOGGING)
                        questViewModel.updateProgress(quest.id, intent.getFloatExtra("distance", 0f))
                    "JOG_COMPLETE" -> if (quest.type == QuestType.JOGGING) {
                        questViewModel.completeQuest(quest.id)
                        authViewModel.completeQuestAndAwardXP(
                            quest = quest,
                            questViewModel = questViewModel
                        )
                    }
                    "SITUP_PROGRESS" -> if (quest.type == QuestType.SITUPS)
                        questViewModel.updateProgress(quest.id, intent.getIntExtra("reps", 0).toFloat())
                    "SITUP_COMPLETE" -> if (quest.type == QuestType.SITUPS) {
                        questViewModel.completeQuest(quest.id)
                        authViewModel.completeQuestAndAwardXP(
                            quest = quest,
                            questViewModel = questViewModel
                        )
                    }
                    "PUSHUP_PROGRESS" -> if (quest.type == QuestType.PUSHUPS)
                        questViewModel.updateProgress(quest.id, intent.getIntExtra("reps", 0).toFloat())
                    "PUSHUP_COMPLETE" -> if (quest.type == QuestType.PUSHUPS) {
                        questViewModel.completeQuest(quest.id)
                        authViewModel.completeQuestAndAwardXP(
                            quest = quest,
                            questViewModel = questViewModel
                        )
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("WALK_PROGRESS"); addAction("WALK_COMPLETE")
            addAction("JOG_PROGRESS"); addAction("JOG_COMPLETE")
            addAction("SITUP_PROGRESS"); addAction("SITUP_COMPLETE")
            addAction("PUSHUP_PROGRESS"); addAction("PUSHUP_COMPLETE")
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = if (quest.category == "storyline"){
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        } else if (isForged) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) else CardDefaults.cardColors(),
        border = if (quest.category == "storyline") {
            BorderStroke(2.5.dp, Color(0xFF9C27B0))
        } else if (isForged) BorderStroke(
            2.dp, MaterialTheme.colorScheme.secondary
        ) else null,
        onClick = {
            when {
                quest.category == "storyline" -> {
                    showCompleteDialog = true
                }
                isComplete && !isForged -> showStartDialog = true
                isComplete && isForged -> { /* forged quests cannot be restarted */ }
                isActive && isPhysical -> { }
                isActive && quest.type == QuestType.PHOTO -> {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
                isActive && !isPhysical -> showCompleteDialog = true
                else -> showStartDialog = true
            }
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isComplete) "✓ ${quest.title}" else quest.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        isComplete -> Text(
                            "Done",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        isActive -> Text(
                            "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(quest.description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            val displayCurrent = if (isComplete && (quest.type == QuestType.HONOR
                        || quest.type == QuestType.PHOTO)) quest.target else current
            Text(
                text = "${displayCurrent.toInt()} / ${quest.target.toInt()} ${quest.unit}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    // Start dialog
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text(if (isComplete) "Restart Quest?" else "Start Quest?") },
            text = { Text(quest.description) },
            confirmButton = {
                TextButton(onClick = {
                    showStartDialog = false
                    questViewModel.resetQuest(quest.id)
                    questViewModel.startQuest(quest.id)
                    when (quest.type) {
                        QuestType.WALKING, QuestType.JOGGING,
                        QuestType.SITUPS, QuestType.PUSHUPS ->
                            launchQuestService(context, quest.type)
                        QuestType.PHOTO ->
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        else -> {}
                    }
                }) { Text(if (isComplete) "Restart" else "Start") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Completion dialog — honor quests and photo confirmation
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("Complete Quest?") },
            text = {
                Column {
                    if (quest.type == QuestType.PHOTO && photoCaptured) {
                        Text("Photo taken! Mark this quest as complete?")
                    } else {
                        Text("Confirm that you've completed: ${quest.title}")
                    }
                    if (quest.requiresInput) {
                        Spacer(Modifier.height(12.dp))
                        quest.inputFields.forEach { field ->
                            val state = inputValues[field] ?: return@forEach
                            OutlinedTextField(
                                value = state.value,
                                onValueChange = { state.value = it },
                                label = { Text(field) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val userId = authViewModel.token.value?.toIntOrNull()
                    if (userId != null) {
                        if (quest.category == "storyline") {
                            authViewModel.completeQuestAndAwardXP(
                                quest = quest,
                                questViewModel = questViewModel
                            )
                            questViewModel.advanceStoryline(userId) {
                                questViewModel.fetchActiveStorylineQuest(userId)
                                showCompleteDialog = false
                            }
                        } else {
                            if (quest.requiresInput) {
                                questViewModel.saveInputs(
                                    quest.id,
                                    inputValues.mapValues { it.value.value }
                                )
                            }
                            if (quest.type == QuestType.PHOTO) {
                                questViewModel.saveInputs(
                                    quest.id,
                                    mapOf("photoUri" to (photoUri?.toString() ?: ""))
                                )
                            }
                            questViewModel.completeQuest(quest.id)
                            authViewModel.completeQuestAndAwardXP(
                                quest = quest,
                                questViewModel = questViewModel
                            )
                            showCompleteDialog = false
                            photoCaptured = false
                        }
                    } else {
                        // Biztonsági mentés, ha nincs userId
                        showCompleteDialog = false
                        photoCaptured = false
                    }
                }) { Text("Complete") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCompleteDialog = false
                    photoCaptured = false
                }) { Text("Cancel") }
            }
        )
    }
}

private fun createPhotoUri(context: Context, questId: String): Uri {
    val photoFile = java.io.File(
        context.cacheDir.resolve("photos").also { it.mkdirs() },
        "quest_${questId}_${System.currentTimeMillis()}.jpg"
    )
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}

private fun launchQuestService(context: Context, type: QuestType) {
    val serviceIntent = when (type) {
        QuestType.WALKING -> Intent(context, com.example.test2.services.WalkingService::class.java)
        QuestType.JOGGING -> Intent(context, com.example.test2.services.JoggingService::class.java)
        QuestType.SITUPS -> Intent(context, com.example.test2.services.SitUpService::class.java)
        QuestType.PUSHUPS -> Intent(context, com.example.test2.services.PushUpService::class.java)
        else -> return
    }
    ContextCompat.startForegroundService(context, serviceIntent)
}