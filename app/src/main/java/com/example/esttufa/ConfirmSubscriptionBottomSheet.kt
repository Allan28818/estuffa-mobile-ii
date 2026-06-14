package com.example.esttufa

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.esttufa.databinding.BottomSheetConfirmSubscriptionBinding
import com.example.esttufa.model.Plan
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ConfirmSubscriptionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetConfirmSubscriptionBinding? = null
    private val binding get() = checkNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetConfirmSubscriptionBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val planId = requireArguments().getString(ARG_PLAN_ID).orEmpty()
        val planName = requireArguments().getString(ARG_PLAN_NAME).orEmpty()
        val price = requireArguments().getString(ARG_PLAN_PRICE).orEmpty()
        val benefits = requireArguments().getStringArrayList(ARG_PLAN_BENEFITS).orEmpty()

        binding.tvSheetPlanName.text = planName
        binding.tvSheetPrice.text = price
        bindBenefits(benefits.take(3))

        binding.btnConfirmSubscription.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_CONFIRM_SUBSCRIPTION,
                Bundle().apply { putString(RESULT_PLAN_ID, planId) }
            )
            dismiss()
        }
        binding.btnCancelSubscription.setOnClickListener { dismiss() }
    }

    private fun bindBenefits(benefits: List<String>) {
        binding.llSheetBenefits.removeAllViews()
        benefits.forEach { benefit ->
            binding.llSheetBenefits.addView(
                TextView(requireContext()).apply {
                    text = getString(R.string.subscription_benefit_check, benefit)
                    setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary)
                    )
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setPadding(0, 0, 0, dpToPx(8))
                }
            )
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_CONFIRM_SUBSCRIPTION = "confirm_subscription"
        const val RESULT_PLAN_ID = "plan_id"

        private const val ARG_PLAN_ID = "arg_plan_id"
        private const val ARG_PLAN_NAME = "arg_plan_name"
        private const val ARG_PLAN_PRICE = "arg_plan_price"
        private const val ARG_PLAN_BENEFITS = "arg_plan_benefits"

        fun newInstance(plan: Plan) = ConfirmSubscriptionBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_PLAN_ID, plan.id)
                putString(ARG_PLAN_NAME, plan.name)
                putString(ARG_PLAN_PRICE, plan.priceRange)
                putStringArrayList(ARG_PLAN_BENEFITS, ArrayList(plan.benefits))
            }
        }
    }
}
