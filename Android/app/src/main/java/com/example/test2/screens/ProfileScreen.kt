package com.example.test2.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.example.test2.R


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
    var showForgeDialog by remember { mutableStateOf(false) }
    val forgedQuests by questViewModel.forgedQuests.collectAsState()

    var showStorylineDialog by remember { mutableStateOf(false) }
    val storylines by questViewModel.storylines.collectAsState()
    var showStorylineDetails by remember { mutableStateOf(false) }

    val activeStorylineQuest by questViewModel.activeStorylineQuest.collectAsState()

    var showingCompleted by remember { mutableStateOf(false) }

    var showAvatarDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        android.util.Log.d("ProfileScreen", "LaunchedEffect fired, token: $token")
        val userId = token?.toIntOrNull()
        if (userId != null) {
            questViewModel.fetchActiveStorylineQuest(userId)
        }
        if (questViewModel.activeQuests.value.isEmpty()) {
            questViewModel.fetchRandomQuests()
        }
        questViewModel.fetchStorylines()
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
                    Image(
                        painter = painterResource(
                            id = avatarToDrawable(profile!!.avatar)
                        ),
                        contentDescription = "Profile Avatar",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(136.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showAvatarDialog = true
                            }
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        profile!!.username,
                        style = MaterialTheme.typography.headlineLarge
                    )
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
                        Row {
                            TextButton(onClick = { showingCompleted = false }) {
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
                        if (!showingCompleted) {
                            Row {
                                TextButton(onClick = { showStorylineDialog = true }) {
                                    Text("Storylines")
                                }
                            }
                            TextButton(onClick = { showForgeDialog = true }) {
                                Text("⚒ Forge")
                            }
                        }
                    }

                    // Show forge dialog:
                    if (showForgeDialog) {
                        ForgeQuestDialog(
                            onDismiss = { showForgeDialog = false },
                            onForge = { title, questType, target, targetLabel ->
                                token?.toIntOrNull()?.let { userId ->
                                    questViewModel.forgeQuest(userId, title, questType, target, targetLabel)
                                }
                            }
                        )
                    }

                    if (showStorylineDialog) {
                        AlertDialog(
                            onDismissRequest = { showStorylineDialog = false },
                            title = {
                                Text("Select your next adventure!")
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (storylines.isEmpty()) {
                                        Text("No storyline available!")
                                    }
                                    storylines.forEach { storyline ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            onClick = {
                                                val userId = token?.toIntOrNull()
                                                if (userId != null) {
                                                    questViewModel.startStoryline(
                                                        userId,
                                                        storyline.id
                                                    ) {
                                                        showStorylineDialog = false
                                                        questViewModel.fetchActiveStorylineQuest(userId)
                                                    }
                                                }
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(storyline.title, style = MaterialTheme.typography.titleMedium)
                                                Spacer(Modifier.height(4.dp))
                                                Text(storyline.description, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showStorylineDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }

            if (showingCompleted) {
                items(completedQuests) { quest ->
                    val isForged = quest.category == "forged"
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = if (isForged) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) else CardDefaults.cardColors(),
                        border = if (isForged) BorderStroke(
                            2.dp, MaterialTheme.colorScheme.secondary
                        ) else null
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

                activeStorylineQuest?.let { storylineQuest ->
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(2.5.dp, Color(0xFF9C27B0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📖 ${storylineQuest.title}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "+${storylineQuest.xpReward} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(storylineQuest.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                items(forgedQuests) { quest ->
                    QuestBanner(
                        quest = quest,
                        questViewModel = questViewModel,
                        authViewModel = authViewModel,
                        isForged = true
                    )
                }
                items(activeQuests) { quest ->
                    QuestBanner(
                        quest = quest,
                        questViewModel = questViewModel,
                        authViewModel = authViewModel,
                        isForged = false
                    )
                }
            }
        }
        if (showAvatarDialog) {
            AlertDialog(
                onDismissRequest = { showAvatarDialog = false },
                title = {
                    Text("Choose Avatar")
                },
                text = {
                    Column {

                        AVAILABLE_AVATARS.chunked(3).forEach { rowAvatars ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {

                                rowAvatars.forEach { avatar ->

                                    Card(
                                        modifier = Modifier
                                            .width(90.dp)
                                            .padding(4.dp),
                                        onClick = {
                                            authViewModel.updateAvatar(avatar.id)
                                            showAvatarDialog = false
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {

                                            Image(
                                                painter = painterResource(avatar.drawable),
                                                contentDescription = avatar.id,
                                                modifier = Modifier.size(64.dp)
                                            )

                                            Spacer(Modifier.height(4.dp))

                                            Text(
                                                avatar.id.replaceFirstChar {
                                                    it.uppercase()
                                                },
                                                style = MaterialTheme.typography.labelSmall
                                            )

                                            if (avatar.id == profile!!.avatar) {
                                                Text(
                                                    "✓",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                },
                confirmButton = {}
            )
        }

    }
}
data class Avatar(
    val id: String,
    val drawable: Int
)

val AVAILABLE_AVATARS = listOf(
    Avatar("Weird guy", R.drawable.default_avatar),
    Avatar("Brad", R.drawable.brad),
    Avatar("Brian", R.drawable.brian),
    Avatar("Mothman", R.drawable.mothman),
    Avatar("Lilienne", R.drawable.netpicker)

)
fun avatarToDrawable(avatar: String): Int {
    return AVAILABLE_AVATARS
        .find { it.id == avatar }
        ?.drawable
        ?: R.drawable.default_avatar
}