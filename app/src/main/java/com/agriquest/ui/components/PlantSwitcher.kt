package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.agriquest.domain.model.MAX_PLANTS_PER_ACCOUNT
import com.agriquest.domain.model.OwnedPlant
import com.agriquest.domain.model.PlantSpecies
import com.agriquest.ui.theme.AppAccentBackground
import com.agriquest.ui.theme.AppCardBackground
import com.agriquest.ui.theme.AppHighlightBackground
import com.agriquest.ui.theme.AppTextColor

/**
 * "A · Flora" / "B · Aubrey" chips in the top bar. Tapping the other plant switches to it;
 * "+ Add plant" appears while the account has fewer than [MAX_PLANTS_PER_ACCOUNT] plants.
 */
@Composable
fun PlantSwitcher(
    plants: List<OwnedPlant>,
    currentPlantId: String?,
    busy: Boolean,
    onSwitch: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        plants.take(MAX_PLANTS_PER_ACCOUNT).forEachIndexed { index, plant ->
            val selected = plant.plantId == currentPlantId
            Surface(
                onClick = { if (!selected && !busy) onSwitch(plant.plantId) },
                shape = RoundedCornerShape(50),
                color = if (selected) AppHighlightBackground else AppAccentBackground,
                contentColor = if (selected) AppTextColor else AppCardBackground,
                border = BorderStroke(1.dp, AppHighlightBackground)
            ) {
                Text("${'A' + index} · ${plant.name}", Modifier.widthIn(max = 120.dp).padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (plants.size < MAX_PLANTS_PER_ACCOUNT) {
            Surface(onClick = { if (!busy) onAdd() }, shape = RoundedCornerShape(50), color = AppAccentBackground,
                contentColor = AppHighlightBackground, border = BorderStroke(1.dp, AppHighlightBackground)) {
                Text("+ Add plant", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
        if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = AppHighlightBackground, strokeWidth = 2.dp)
    }
}

/** Pick Tomato or Eggplant and name the new plant. Its age is asked next, like the first plant. */
@Composable
fun AddPlantDialog(busy: Boolean, onAdd: (PlantSpecies, String) -> Unit, onDismiss: () -> Unit) {
    var species by rememberSaveable { mutableStateOf(PlantSpecies.TOMATO) }
    var name by rememberSaveable { mutableStateOf("") }
    val valid = name.trim().length in 1..40
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = AppCardBackground,
        titleContentColor = AppTextColor,
        title = { Text("Add a plant", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("It gets the same care and quests as your first plant.", style = MaterialTheme.typography.bodySmall,
                    color = AppTextColor)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlantSpecies.entries.forEach { option ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            // Preview the ripe plant of each kind.
                            CompositionLocalProvider(LocalPlantSpecies provides option) {
                                GrowingTomatoPlant(120, size = 72.dp)
                            }
                            if (option == species) {
                                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = PixelGreen,
                                    contentColor = AppCardBackground)) { Text(option.label) }
                            } else {
                                OutlinedButton(onClick = { species = option }, enabled = !busy) {
                                    Text(option.label, color = AppTextColor)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Plant name") }, singleLine = true,
                    enabled = !busy, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(species, name) }, enabled = valid && !busy,
                colors = ButtonDefaults.buttonColors(containerColor = PixelGreen, contentColor = AppCardBackground)) {
                Text(if (busy) "Adding..." else "Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !busy) { Text("Cancel", color = AppTextColor) }
        }
    )
}
