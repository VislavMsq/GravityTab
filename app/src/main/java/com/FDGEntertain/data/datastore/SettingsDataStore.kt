package com.FDGEntertain.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.FDGEntertain.domain.model.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class SettingsDataStore @Inject constructor(
    private val ds: DataStore<Preferences>
) {
    private val KEY_DIFF = stringPreferencesKey("difficulty")
    private val KEY_SOUND = booleanPreferencesKey("sound")

    val settings: Flow<Pair<Difficulty, Boolean>> =
        ds.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { p ->
                val diff = runCatching { Difficulty.valueOf(p[KEY_DIFF] ?: "NORMAL") }
                    .getOrDefault(Difficulty.NORMAL)
                val sound = p[KEY_SOUND] ?: true
                diff to sound
            }

    suspend fun updateDifficulty(d: Difficulty) {
        ds.edit { it[KEY_DIFF] = d.name }
    }

    suspend fun updateSound(on: Boolean) {
        ds.edit { it[KEY_SOUND] = on }
    }
}
