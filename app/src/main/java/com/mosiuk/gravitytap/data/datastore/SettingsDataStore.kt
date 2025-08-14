package com.mosiuk.gravitytap.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mosiuk.gravitytap.domain.model.Difficulty
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsDataStore @Inject constructor(
    private val ds: DataStore<Preferences>
) {
    private val KEY_DIFF = stringPreferencesKey("difficulty")
    private val KEY_SOUND = booleanPreferencesKey("sound")

    val settings = ds.data.map { p ->
        val d = Difficulty.from(p[KEY_DIFF] ?: Difficulty.NORMAL.name)
        val sound = p[KEY_SOUND] ?: true
        d to sound
    }

    suspend fun updateDifficulty(d: Difficulty){
        ds.edit { it[KEY_DIFF] = d.name }
    }

    suspend fun updateSound(sound: Boolean){
        ds.edit { it[KEY_SOUND] = sound }
    }
}