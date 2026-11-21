package com.agriquest.domain.model

/** Where an outfit sits on the tomato. One outfit per slot can be worn at a time. */
enum class OutfitSlot(val label: String) { HEAD("Head"), FACE("Face"), NECK("Neck") }

/**
 * An outfit for the tomato, bought with points. [imageName] is the drawable file name
 * (res/drawable-nodpi/<imageName>.png); the team replaces the placeholder art with final art.
 */
data class Outfit(val id: String, val name: String, val price: Int, val slot: OutfitSlot, val imageName: String) {
    companion object {
        val CATALOG = listOf(
            Outfit("straw_hat", "Straw Hat", 60, OutfitSlot.HEAD, "outfit_straw_hat"),
            Outfit("cap", "Garden Cap", 40, OutfitSlot.HEAD, "outfit_cap"),
            Outfit("flower_crown", "Flower Crown", 90, OutfitSlot.HEAD, "outfit_flower_crown"),
            Outfit("crown", "Harvest Crown", 200, OutfitSlot.HEAD, "outfit_crown"),
            Outfit("sunglasses", "Sunglasses", 50, OutfitSlot.FACE, "outfit_sunglasses"),
            Outfit("scarf", "Cozy Scarf", 70, OutfitSlot.NECK, "outfit_scarf"),
        )

        fun byId(id: String): Outfit? = CATALOG.firstOrNull { it.id == id }
    }
}

/**
 * The player's wardrobe. Points come from all XP ever earned; spending points never lowers XP or level.
 *
 * @param spentPoints points already spent on outfits
 * @param equipped outfit id worn in each slot
 */
data class Wardrobe(
    val owned: Set<String> = emptySet(),
    val equipped: Map<OutfitSlot, String> = emptyMap(),
    val spentPoints: Int = 0
) {
    fun points(totalXp: Int): Int = (totalXp - spentPoints).coerceAtLeast(0)
    val equippedOutfits: List<Outfit> get() = equipped.values.mapNotNull(Outfit::byId)
}
