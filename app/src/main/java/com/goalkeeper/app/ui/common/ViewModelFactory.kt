package com.goalkeeper.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.goalkeeper.app.GoalKeeperApp
import com.goalkeeper.app.di.AppContainer

/**
 * Builds a ViewModel factory with access to the [AppContainer].
 *
 * ```
 * val vm: TodayViewModel = viewModel(factory = gkViewModelFactory { c -> TodayViewModel(c.goalRepository, c.clock) })
 * // with navigation arguments:
 * gkViewModelFactory { c -> GoalDetailViewModel(createSavedStateHandle(), c.goalRepository) }
 * ```
 */
inline fun <reified VM : ViewModel> gkViewModelFactory(
    crossinline create: CreationExtras.(AppContainer) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GoalKeeperApp
        create(app.container)
    }
}
