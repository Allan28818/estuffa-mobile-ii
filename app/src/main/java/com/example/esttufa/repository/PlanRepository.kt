package com.example.esttufa.repository

import com.example.esttufa.model.Plan

class PlanRepository {

    fun getPlans(): Result<List<Plan>> = runCatching { plans }

    fun updateCurrentPlan(planId: String): Result<Plan> = runCatching {
        val plan = plans.firstOrNull { it.id == planId }
            ?: error("Plano não encontrado.")
        currentPlanId = plan.id
        plan
    }

    companion object {
        private var currentPlanId = "muda"

        fun getUserCurrentPlanId(): String = currentPlanId

        private val plans = listOf(
            Plan(
                id = "semente",
                name = "Plano Semente",
                priceRange = "R$ 199–300/mês",
                benefits = listOf(
                    "Irrigação automatizada básica (temperatura + tempo)",
                    "Até 2 estufas"
                ),
                emoji = "🌱"
            ),
            Plan(
                id = "muda",
                name = "Plano Muda",
                priceRange = "R$ 399,90–599,90/mês",
                benefits = listOf(
                    "Tudo do Semente + visão computacional das plantas",
                    "Detecção de pragas e saúde das folhas",
                    "Até 4 estufas"
                ),
                isRecommended = true,
                emoji = "🌿"
            ),
            Plan(
                id = "colheita",
                name = "Plano Colheita",
                priceRange = "Consultar valor",
                benefits = listOf(
                    "Tudo do Muda + previsão de crescimento e pragas",
                    "Controle de ventiladores, holofotes e lonas",
                    "Controle de luminosidade e umidade do ar"
                ),
                emoji = "🌾"
            ),
            Plan(
                id = "safra",
                name = "Plano Safra",
                priceRange = "R$ 1.599,90–2.599,90/mês",
                benefits = listOf(
                    "Tudo do Colheita + visitas quinzenais de técnico agrônomo",
                    "Controle automático de adubação, aquecimento e pesticidas",
                    "Previsão de lucro e detecção de sazonalidade"
                ),
                emoji = "🏆"
            ),
            Plan(
                id = "colheitadeira",
                name = "Plano Colheitadeira",
                priceRange = "A partir de R$ 15.000/mês",
                benefits = listOf(
                    "Plano empresarial completo",
                    "Visitas imediatas e manutenção semanal",
                    "Até 6 estufas + expansão mediante adicional",
                    "Testagem genética e sugestão de cruzamento de hortaliças",
                    "1 consultoria gratuita por mês"
                ),
                emoji = "🚜"
            )
        )
    }
}
