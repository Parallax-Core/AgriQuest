package com.agriquest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.agriquest.R
import com.agriquest.domain.model.Outfit
import com.agriquest.domain.model.OutfitSlot

/** Drawable for each outfit (replace the PNGs in res/drawable-nodpi with final art; keep the names). */
fun outfitDrawable(outfit: Outfit): Int = when (outfit.id) {
    "straw_hat" -> R.drawable.outfit_straw_hat
    "cap" -> R.drawable.outfit_cap
    "flower_crown" -> R.drawable.outfit_flower_crown
    "crown" -> R.drawable.outfit_crown
    "sunglasses" -> R.drawable.outfit_sunglasses
    "scarf" -> R.drawable.outfit_scarf
    else -> R.drawable.placeholder
}

@Composable
fun OutfitImage(outfit: Outfit, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(outfitDrawable(outfit)), outfit.name, modifier.size(size))
}

/** Worn outfits placed on a plant drawn at [plantSize]: hat on top, glasses just below, scarf on the stem. */
@Composable
fun BoxScope.OutfitLayer(outfits: List<Outfit>, dayCount: Int, plantSize: Dp) {
    val top = tomatoTopFraction(dayCount)
    val stem = tomatoStemFraction(dayCount)
    val itemSize = plantSize * 0.3f
    outfits.forEach { outfit ->
        val y = when (outfit.slot) {
            OutfitSlot.HEAD -> plantSize * top - itemSize * 0.8f
            OutfitSlot.FACE -> plantSize * top - itemSize * 0.35f
            OutfitSlot.NECK -> plantSize * (top + stem * 0.3f) - itemSize * 0.5f
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y = y)) { OutfitImage(outfit, itemSize) }
    }
}
