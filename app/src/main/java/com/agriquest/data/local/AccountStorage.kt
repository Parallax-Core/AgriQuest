package com.agriquest.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal object AccountStorage {
    private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

    /** Plant A keeps the original account file; Plant B gets its own file so its data never mixes with A's. */
    fun fileName(userId: String, plantId: String? = null): String =
        "agriquest_account_${UUID.fromString(userId)}" + (plantId?.let { "_plant_${UUID.fromString(it)}" } ?: "")

    // A process must have only one DataStore instance for each physical file.
    fun get(context: Context, userId: String, plantId: String? = null): DataStore<Preferences> {
        val name = fileName(userId, plantId)
        return stores.computeIfAbsent(name) {
            PreferenceDataStoreFactory.create(produceFile = {
                context.applicationContext.preferencesDataStoreFile(name)
            })
        }
    }
}
