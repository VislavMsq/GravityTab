// ui/vm/SettingsViewModel.kt
package com.FDGEntertain.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FDGEntertain.data.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {

    val soundOn = settings.settings
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun toggleSound(on: Boolean) {
        viewModelScope.launch { settings.updateSound(on) }
    }
}
