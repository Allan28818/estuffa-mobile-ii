package com.example.esttufa

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.esttufa.adapter.PlanAdapter
import com.example.esttufa.databinding.ActivityPlansBinding
import com.example.esttufa.model.Plan
import com.example.esttufa.viewmodel.PlansUiState
import com.example.esttufa.viewmodel.PlansViewModel
import com.example.esttufa.viewmodel.SubscriptionEvent
import com.google.android.material.snackbar.Snackbar

class PlansActivity : AppCompatActivity(), PlanAdapter.OnPlanActionListener {

    private lateinit var binding: ActivityPlansBinding
    private val viewModel: PlansViewModel by viewModels()
    private val planAdapter = PlanAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlansBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupList()
        setupActions()
        observeViewModel()
        viewModel.loadPlans()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarPlans)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarPlans.setNavigationOnClickListener { finish() }
    }

    private fun setupList() {
        binding.rvPlans.layoutManager = LinearLayoutManager(this)
        binding.rvPlans.adapter = planAdapter
    }

    private fun setupActions() {
        binding.btnRetryPlans.setOnClickListener { viewModel.loadPlans() }
        supportFragmentManager.setFragmentResultListener(
            ConfirmSubscriptionBottomSheet.REQUEST_CONFIRM_SUBSCRIPTION,
            this
        ) { _, result ->
            result.getString(ConfirmSubscriptionBottomSheet.RESULT_PLAN_ID)
                ?.let(viewModel::confirmSubscription)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                PlansUiState.Loading -> showLoading()
                is PlansUiState.Success -> showPlans(state)
                is PlansUiState.Error -> showError(state.message)
            }
        }

        viewModel.subscriptionEvent.observe(this) { event ->
            when (event) {
                is SubscriptionEvent.ConfirmSubscription -> {
                    ConfirmSubscriptionBottomSheet.newInstance(event.plan)
                        .show(supportFragmentManager, CONFIRM_SHEET_TAG)
                    viewModel.clearEvent()
                }
                is SubscriptionEvent.SubscriptionSuccess -> {
                    Snackbar.make(
                        binding.root,
                        getString(
                            R.string.subscription_success_message,
                            event.planName
                        ),
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.clearEvent()
                }
                is SubscriptionEvent.SubscriptionError -> {
                    Snackbar.make(
                        binding.root,
                        event.message,
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.clearEvent()
                }
                null -> Unit
            }
        }
    }

    private fun showLoading() {
        binding.plansLoadingState.visibility = View.VISIBLE
        binding.rvPlans.visibility = View.GONE
        binding.plansErrorState.visibility = View.GONE
        binding.plansEmptyState.visibility = View.GONE
    }

    private fun showPlans(state: PlansUiState.Success) {
        binding.plansLoadingState.visibility = View.GONE
        binding.plansErrorState.visibility = View.GONE

        if (state.plans.isEmpty()) {
            binding.rvPlans.visibility = View.GONE
            binding.plansEmptyState.visibility = View.VISIBLE
            return
        }

        binding.plansEmptyState.visibility = View.GONE
        binding.rvPlans.visibility = View.VISIBLE
        planAdapter.currentPlanId = state.currentPlanId
        planAdapter.submitPlans(state.plans)
    }

    private fun showError(message: String) {
        binding.plansLoadingState.visibility = View.GONE
        binding.rvPlans.visibility = View.GONE
        binding.plansEmptyState.visibility = View.GONE
        binding.plansErrorState.visibility = View.VISIBLE
        binding.tvPlansError.text = message
    }

    override fun onSubscribeClick(plan: Plan) {
        viewModel.requestSubscription(plan)
    }

    override fun onSeeMoreClick(planId: String) {
        viewModel.toggleCardExpansion(planId)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private companion object {
        const val CONFIRM_SHEET_TAG = "confirm_subscription_sheet"
    }
}
