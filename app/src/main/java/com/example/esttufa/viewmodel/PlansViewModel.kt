package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esttufa.model.Plan
import com.example.esttufa.repository.PlanRepository
import kotlinx.coroutines.launch

sealed class PlansUiState {
    data object Loading : PlansUiState()
    data class Success(
        val plans: List<Plan>,
        val currentPlanId: String
    ) : PlansUiState()

    data class Error(val message: String) : PlansUiState()
}

sealed class SubscriptionEvent {
    data class ConfirmSubscription(val plan: Plan) : SubscriptionEvent()
    data class SubscriptionSuccess(val planName: String) : SubscriptionEvent()
    data class SubscriptionError(val message: String) : SubscriptionEvent()
}

class PlansViewModel(
    private val repository: PlanRepository = PlanRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<PlansUiState>()
    val uiState: LiveData<PlansUiState> = _uiState

    private val _subscriptionEvent = MutableLiveData<SubscriptionEvent?>()
    val subscriptionEvent: LiveData<SubscriptionEvent?> = _subscriptionEvent

    private val expandedCards = mutableMapOf<String, Boolean>()

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.value = PlansUiState.Loading
            repository.getPlans()
                .onSuccess { plans ->
                    _uiState.value = PlansUiState.Success(
                        plans = plans,
                        currentPlanId = PlanRepository.getUserCurrentPlanId()
                    )
                }
                .onFailure {
                    _uiState.value = PlansUiState.Error(
                        it.message ?: "Erro ao carregar planos. Tente novamente."
                    )
                }
        }
    }

    fun requestSubscription(plan: Plan) {
        if (plan.id != PlanRepository.getUserCurrentPlanId()) {
            _subscriptionEvent.value = SubscriptionEvent.ConfirmSubscription(plan)
        }
    }

    fun confirmSubscription(planId: String) {
        viewModelScope.launch {
            repository.updateCurrentPlan(planId)
                .onSuccess { plan ->
                    val plans = repository.getPlans().getOrDefault(emptyList())
                    _uiState.value = PlansUiState.Success(plans, plan.id)
                    _subscriptionEvent.value =
                        SubscriptionEvent.SubscriptionSuccess(plan.name)
                }
                .onFailure {
                    _subscriptionEvent.value = SubscriptionEvent.SubscriptionError(
                        it.message ?: "Não foi possível concluir a assinatura."
                    )
                }
        }
    }

    fun toggleCardExpansion(planId: String): Boolean {
        val isExpanded = !(expandedCards[planId] ?: false)
        expandedCards[planId] = isExpanded
        return isExpanded
    }

    fun isCardExpanded(planId: String): Boolean = expandedCards[planId] ?: false

    fun clearEvent() {
        _subscriptionEvent.value = null
    }
}
