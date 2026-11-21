package com.agriquest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/** Asks how old the real plant is, so the game starts at the matching growth stage. */
@Composable
fun PlantAgeDialog(initialDays: Int, maxDays: Int, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(initialDays.coerceAtLeast(0).toString()) }
    val days = text.trim().toIntOrNull()
    val valid = days != null && days in 0..maxDays
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("How old is your plant?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Count the days since you sowed the seed. Enter 0 if you are sowing today.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(text, { text = it.filter(Char::isDigit).take(3) },
                    label = { Text("Days old") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valid, modifier = Modifier.fillMaxWidth())
                if (valid) {
                    GrowingTomatoPlant(requireNotNull(days), Modifier.align(Alignment.CenterHorizontally), size = 110.dp)
                    Text(TomatoLook.forDay(days).description, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Text("Enter a number from 0 to $maxDays.", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { days?.let(onConfirm) }, enabled = valid) { Text("Start") }
        }
    )
}
