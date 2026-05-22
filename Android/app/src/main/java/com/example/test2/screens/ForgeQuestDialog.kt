package com.example.test2.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ForgeQuestDialog(
    onDismiss: () -> Unit,
    onForge: (title: String, questType: String, target: Int, targetLabel: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("walking") }
    var target by remember { mutableStateOf("") }
    var targetLabel by remember { mutableStateOf("") }

    val questTypes = listOf("walking", "jogging", "situps", "pushups", "reading", "photo", "other")

    val targetRange = when (selectedType) {
        "walking" -> 250..2000 step 250
        "jogging" -> 500..2000 step 250
        "situps", "pushups" -> 3..30 step 1
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forge a Quest") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Quest Type", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                questTypes.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    target = ""
                                    targetLabel = ""
                                },
                                label = { Text(type.replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                when (selectedType) {
                    "reading" -> {
                        Text(
                            "You'll be asked for the book title and author when completing.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    "photo" -> {
                        OutlinedTextField(
                            value = targetLabel,
                            onValueChange = { targetLabel = it },
                            label = { Text("What to photograph") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "other" -> {
                        Text(
                            "You'll confirm completion yourself when done.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    else -> {
                        targetRange?.let { range ->
                            val currentValue = target.toIntOrNull() ?: range.first
                            Text(
                                "${when (selectedType) {
                                    "walking", "jogging" -> "Distance"
                                    else -> "Reps"
                                }}: $currentValue",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = currentValue.toFloat(),
                                onValueChange = { target = it.toInt().toString() },
                                valueRange = range.first.toFloat()..range.last.toFloat(),
                                steps = ((range.last - range.first) / range.step) - 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    val resolvedTarget = when (selectedType) {
                        "reading", "other" -> 1
                        "photo" -> 1
                        else -> target.toIntOrNull() ?: 0
                    }
                    onForge(title, selectedType, resolvedTarget, targetLabel)
                    onDismiss()
                }
            }) { Text("Forge") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}