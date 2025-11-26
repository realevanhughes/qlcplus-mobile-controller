package com.evanh.qlc_controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ControlViewModelFactory(
    private val repo: SettingsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ControlViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ControlViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
