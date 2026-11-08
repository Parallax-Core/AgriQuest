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

    fun fileName(userId: String): String = "agriquest_account_${UUID.fromString(userId)}"

    // A process must have only one DataStore instance for each physical file.
    fun get(context: Context, userId: String): DataStore<Preferences> {
        val name = fileName(userId)
        return stores.computeIfAbsent(name) {
            PreferenceDataStoreFactory.create(produceFile = {
                context.applicationContext.preferencesDataStoreFile(name)
            })
        }
    }
}
