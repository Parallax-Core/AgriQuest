package com.agriquest.ui.awards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agriquest.domain.model.Outfit
import com.agriquest.domain.model.Wardrobe
import com.agriquest.ui.components.OutfitImage
import com.agriquest.ui.components.PixelButton
import com.agriquest.ui.components.PixelChip
import com.agriquest.ui.components.PixelGreen
import com.agriquest.ui.components.PixelHeading
import com.agriquest.ui.components.PixelPanel
import com.agriquest.ui.components.PixelTan
import com.agriquest.ui.theme.AppCardBorder
import com.agriquest.ui.theme.AppTextColor

/** Outfit shop: buy outfits with points (all XP earned minus points spent), then wear or remove them. */
@Composable
fun OutfitShopPanel(wardrobe: Wardrobe, totalXp: Int, onBuy: (String) -> Unit, onToggle: (String) -> Unit) {
    val points = wardrobe.points(totalXp)
    PixelPanel {
        PixelHeading("${com.agriquest.ui.components.LocalPlantSpecies.current.label} Outfits", trailing = { PixelChip("$points points") })
        Text("Earn points with XP from quests and quizzes. Spending points doesn't lower your level.",
            style = MaterialTheme.typography.bodySmall, color = AppTextColor)
        Outfit.CATALOG.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { outfit ->
                    OutfitCard(outfit, owned = outfit.id in wardrobe.owned, worn = wardrobe.equipped[outfit.slot] == outfit.id,
                        affordable = points >= outfit.price, onBuy = { onBuy(outfit.id) }, onToggle = { onToggle(outfit.id) },
                        modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Column(Modifier.weight(1f)) {}
            }
        }
    }
}

@Composable
private fun OutfitCard(outfit: Outfit, owned: Boolean, worn: Boolean, affordable: Boolean,
    onBuy: () -> Unit, onToggle: () -> Unit, modifier: Modifier) {
    Surface(modifier, color = if (worn) Color(0xFFE3EBB0) else PixelTan, shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (worn) PixelGreen else AppCardBorder)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutfitImage(outfit, 56.dp)
            Text(outfit.name, style = MaterialTheme.typography.labelLarge, color = AppTextColor)
            Text(outfit.slot.label, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
            when {
                owned -> PixelButton(if (worn) "Remove" else "Wear", onToggle, Modifier.fillMaxWidth(),
                    color = if (worn) Color(0xFF8B5237) else PixelGreen)
                else -> PixelButton("Buy · ${outfit.price}", onBuy, Modifier.fillMaxWidth(), enabled = affordable)
            }
        }
    }
}
