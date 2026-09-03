package com.aatmn2000.aibuilder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aatmn2000.aibuilder.AppContainer

/**
 * Small factory helper so screens can build ViewModels that need the
 * [AppContainer] without pulling in a DI framework.
 */
fun containerFactory(
    container: AppContainer,
    create: (AppContainer) -> ViewModel
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            create(container) as T
    }
}
