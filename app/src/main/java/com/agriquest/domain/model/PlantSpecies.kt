package com.agriquest.domain.model

/** The kinds of plant a player can grow. Both follow the same stages and care. Stored in plants.cultivar. */
enum class PlantSpecies(val key: String, val label: String) {
    TOMATO("tomato", "Tomato"),
    EGGPLANT("eggplant", "Eggplant");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: TOMATO
    }
}

/** Most plants one account can own; the server enforces the same limit. */
const val MAX_PLANTS_PER_ACCOUNT = 2
