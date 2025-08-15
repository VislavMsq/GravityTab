package com.mosiuk.gravitytap.ui.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosiuk.gravitytap.core.util.DispatchersProvider
import com.mosiuk.gravitytap.domain.model.Difficulty
import com.mosiuk.gravitytap.domain.model.ScoreEntry
import com.mosiuk.gravitytap.domain.usecase.GetHighScoresUseCase
import com.mosiuk.gravitytap.domain.usecase.SaveHighScoreUseCase
import com.mosiuk.gravitytap.ui.result.ResultUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel
    @Inject
    constructor(
        private val handle: SavedStateHandle,
        private val save: SaveHighScoreUseCase,
        getTop: GetHighScoresUseCase,
        private val dispatchers: DispatchersProvider,
    ) : ViewModel() {
        private companion object {
            const val KEY_SAVED = "result_saved"
        }

        private val score: Int = handle.get<Int>("score") ?: 0
        private val difficulty: Difficulty =
            run {
                val raw =
                    handle
                        .get<String>("difficulty")
                        ?.trim()
                        ?.removePrefix("{")
                        ?.removeSuffix("}")
                        ?.uppercase()

                Difficulty.entries.firstOrNull { it.name == raw } ?: Difficulty.NORMAL
            }
        private val maxCombo: Int = handle.get<Int>("maxCombo") ?: 0

        val ui: ResultUi = ResultUi(score, difficulty, maxCombo)

        val top: StateFlow<List<ScoreEntry>> =
            getTop().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            if (handle.get<Boolean>(KEY_SAVED) != true) {
                viewModelScope.launch(dispatchers.io) {
                    save(score, difficulty, maxCombo)
                    handle.set(KEY_SAVED, true)
                }
            }
        }
    }
