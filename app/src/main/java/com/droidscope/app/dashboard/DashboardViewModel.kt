package com.droidscope.app.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidscope.app.data.UserPreferences
import com.droidscope.app.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun moveCard(cardId: String, direction: Int) {
        val prefs = preferences.value
        val list = prefs.dashboardCardOrder.toMutableList()
        val index = list.indexOf(cardId)
        val target = index + direction
        if (index >= 0 && target in list.indices) {
            val item = list.removeAt(index)
            list.add(target, item)
            viewModelScope.launch {
                preferencesRepository.saveDashboardLayout(list, prefs.hiddenDashboardCards)
            }
        }
    }

    fun toggleVisibility(cardId: String) {
        val prefs = preferences.value
        val hidden = prefs.hiddenDashboardCards.toMutableSet()
        if (cardId in hidden) hidden.remove(cardId) else hidden.add(cardId)
        viewModelScope.launch {
            preferencesRepository.saveDashboardLayout(prefs.dashboardCardOrder, hidden)
        }
    }

    fun resetLayout() {
        viewModelScope.launch {
            preferencesRepository.resetDashboardLayout()
        }
    }
}
