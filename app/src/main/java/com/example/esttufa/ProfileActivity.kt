package com.example.esttufa

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.esttufa.adapter.SettingsAdapter
import com.example.esttufa.databinding.ActivityProfileBinding
import com.example.esttufa.model.UserProfile
import com.example.esttufa.repository.PlanRepository
import com.example.esttufa.viewmodel.ProfileUiState
import com.example.esttufa.viewmodel.ProfileViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSettings()
        setupActions()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarProfile.setNavigationOnClickListener { finish() }
    }

    private fun setupSettings() {
        binding.lvSettings.adapter = SettingsAdapter(this, viewModel.settingsItems)
        binding.lvSettings.setOnItemClickListener { _, _, position, _ ->
            when (viewModel.settingsItems[position].id) {
                "logout" -> showLogoutDialog()
                else -> showComingSoon()
            }
        }
    }

    private fun setupActions() {
        binding.btnViewPlans.setOnClickListener {
            startActivity(Intent(this, PlansActivity::class.java))
        }
        binding.btnRetryProfile.setOnClickListener { viewModel.loadProfile() }

        listOf(
            binding.btnEditName,
            binding.btnEditEmail,
            binding.btnEditPhone,
            binding.btnEditLocation
        ).forEach { button ->
            button.setOnClickListener { showComingSoon() }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                ProfileUiState.Loading -> showLoading()
                is ProfileUiState.Success -> showProfile(state.profile)
                is ProfileUiState.Error -> showError(state.message)
            }
        }
    }

    private fun showLoading() {
        binding.progressProfile.visibility = View.VISIBLE
        binding.profileContent.visibility = View.GONE
        binding.profileErrorState.visibility = View.GONE
    }

    private fun showProfile(profile: UserProfile) {
        binding.progressProfile.visibility = View.GONE
        binding.profileErrorState.visibility = View.GONE
        binding.profileContent.visibility = View.VISIBLE

        val plan = PlanRepository().getPlans().getOrDefault(emptyList())
            .firstOrNull { it.id == profile.currentPlanId }

        binding.tvProfileName.text = profile.displayName
        binding.tvNameValue.text = profile.displayName
        setOptionalValue(binding.tvEmailValue, profile.email)
        setOptionalValue(binding.tvPhoneValue, profile.phone)
        setOptionalValue(binding.tvLocationValue, profile.location)

        val planName = plan?.name ?: getString(R.string.profile_no_plan)
        binding.tvPlanBadge.text = planName
        binding.tvPlanBadge.contentDescription = getString(
            R.string.profile_plan_badge_description,
            planName
        )
        binding.tvCurrentPlan.text = planName
        binding.tvRenewalDate.visibility = if (profile.currentPlanId.isBlank()) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.tvRenewalDate.text = getString(
            R.string.profile_renewal_label,
            profile.renewalDate.orEmpty()
        )

        bindAvatar(profile)
    }

    private fun setOptionalValue(view: TextView, value: String?) {
        val hasValue = !value.isNullOrBlank()
        view.text = if (hasValue) value else getString(R.string.profile_not_informed)
        view.setTextColor(
            ContextCompat.getColor(
                this,
                if (hasValue) R.color.text_primary else R.color.text_secondary_light
            )
        )
    }

    private fun bindAvatar(profile: UserProfile) {
        val initials = profile.displayName
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "U" }

        binding.tvAvatarInitials.text = initials
        binding.tvAvatarInitials.contentDescription = getString(
            R.string.profile_initials_description,
            initials
        )
        binding.tvAvatarInitials.visibility = View.VISIBLE
        binding.ivProfileAvatar.visibility = View.GONE

        val photoUrl = profile.photoUrl ?: return
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(photoUrl).openStream().use(BitmapFactory::decodeStream)
                }.getOrNull()
            } ?: return@launch

            binding.ivProfileAvatar.setImageBitmap(bitmap)
            binding.ivProfileAvatar.visibility = View.VISIBLE
            binding.tvAvatarInitials.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.progressProfile.visibility = View.GONE
        binding.profileContent.visibility = View.GONE
        binding.profileErrorState.visibility = View.VISIBLE
        binding.tvProfileError.text = message
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_dialog_title)
            .setMessage(R.string.logout_dialog_message)
            .setNegativeButton(R.string.logout_dialog_cancel, null)
            .setPositiveButton(R.string.logout_dialog_confirm) { _, _ ->
                viewModel.logout()
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
            }
            .show()
    }

    private fun showComingSoon() {
        Toast.makeText(this, R.string.settings_coming_soon, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
