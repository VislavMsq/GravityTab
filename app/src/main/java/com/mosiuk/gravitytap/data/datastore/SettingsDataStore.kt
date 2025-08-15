package com.mosiuk.gravitytap.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mosiuk.gravitytap.domain.model.Difficulty
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsDataStore
    @Inject
    constructor(
        @ApplicationContext
        context: Context,
    ) {
        private val ds = context.settingsDataStore

        private val keyDiff = stringPreferencesKey("difficulty")
        private val keySound = booleanPreferencesKey("sound")

        val settings: Flow<Pair<Difficulty, Boolean>> =
            ds.data.map { p: Preferences ->
                val diffStr = p[keyDiff] ?: "NORMAL"
                val diff = runCatching { Difficulty.valueOf(diffStr) }.getOrDefault(Difficulty.NORMAL)
                val sound = p[keySound] ?: true
                diff to sound
            }

        suspend fun updateDifficulty(d: Difficulty) {
            ds.edit { it[keyDiff] = d.name }
        }

        suspend fun updateSound(on: Boolean) {
            ds.edit { it[keySound] = on }
        }
    }
