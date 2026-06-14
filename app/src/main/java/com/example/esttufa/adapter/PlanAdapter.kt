package com.example.esttufa.adapter

import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.example.esttufa.R
import com.example.esttufa.databinding.ItemPlanCardBinding
import com.example.esttufa.model.Plan

class PlanAdapter(
    private val listener: OnPlanActionListener
) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {

    private var plans: List<Plan> = emptyList()
    private val expandedPlanIds = mutableSetOf<String>()

    var currentPlanId: String = ""
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun submitPlans(items: List<Plan>) {
        plans = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemPlanCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(plans[position])
    }

    override fun getItemCount(): Int = plans.size

    inner class PlanViewHolder(
        private val binding: ItemPlanCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: Plan) = with(binding) {
            val context = root.context
            val isExpanded = plan.id in expandedPlanIds
            val isCurrent = plan.id == currentPlanId

            tvPlanName.text = context.getString(
                R.string.plan_name_with_emoji,
                plan.emoji,
                plan.name
            )
            tvPlanPrice.text = plan.priceRange

            tvPopularBadge.visibility = if (plan.isRecommended) {
                View.VISIBLE
            } else {
                View.GONE
            }
            tvPopularBadge.contentDescription = context.getString(
                R.string.plans_recommended_description,
                plan.name
            )
            cardPlan.strokeWidth = if (plan.isRecommended) dpToPx(2) else 0
            cardPlan.strokeColor = ContextCompat.getColor(context, R.color.tertiary)

            val visibleBenefits = if (isExpanded) {
                plan.benefits
            } else {
                plan.benefits.take(plan.maxVisibleBenefits)
            }
            bindBenefits(visibleBenefits)

            val hasHiddenBenefits = plan.benefits.size > plan.maxVisibleBenefits
            tvSeeMore.visibility = if (hasHiddenBenefits) View.VISIBLE else View.GONE
            tvSeeMore.setText(
                if (isExpanded) R.string.plans_see_less else R.string.plans_see_more
            )
            tvSeeMore.contentDescription = context.getString(
                if (isExpanded) {
                    R.string.plans_collapse_description
                } else {
                    R.string.plans_expand_description
                },
                plan.name
            )
            tvSeeMore.setOnClickListener {
                TransitionManager.beginDelayedTransition(cardPlan)
                if (isExpanded) {
                    expandedPlanIds.remove(plan.id)
                } else {
                    expandedPlanIds.add(plan.id)
                }
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position)
                }
                listener.onSeeMoreClick(plan.id)
            }

            btnPlanAction.isEnabled = !isCurrent
            btnPlanAction.setText(
                if (isCurrent) R.string.plans_btn_current else R.string.plans_btn_subscribe
            )
            btnPlanAction.contentDescription = if (isCurrent) {
                context.getString(R.string.plans_current_description)
            } else {
                context.getString(R.string.plans_subscribe_description, plan.name)
            }
            btnPlanAction.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (isCurrent) R.color.disabled_button else R.color.tertiary
                )
            )
            btnPlanAction.setOnClickListener {
                if (!isCurrent) listener.onSubscribeClick(plan)
            }
        }

        private fun bindBenefits(benefits: List<String>) {
            binding.llBenefits.removeAllViews()
            benefits.forEach { benefit ->
                val textView = TextView(binding.root.context).apply {
                    text = binding.root.context.getString(
                        R.string.plan_benefit_bullet,
                        benefit
                    )
                    setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary)
                    )
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setPadding(0, 0, 0, dpToPx(8))
                }
                binding.llBenefits.addView(textView)
            }
        }

        private fun dpToPx(dp: Int): Int = (dp * binding.root.resources.displayMetrics.density)
            .toInt()
    }

    interface OnPlanActionListener {
        fun onSubscribeClick(plan: Plan)
        fun onSeeMoreClick(planId: String)
    }
}
