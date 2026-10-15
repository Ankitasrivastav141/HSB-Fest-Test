package com.zenith26.fest.ui

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.zenith26.fest.R
import com.zenith26.fest.data.api.ApiClient
import com.zenith26.fest.data.model.Event
import com.zenith26.fest.data.model.Registration
import com.zenith26.fest.databinding.ActivityMainBinding
import com.zenith26.fest.databinding.DialogServerConfigBinding
import com.zenith26.fest.ui.admin.AddEventDialog
import com.zenith26.fest.ui.admin.AdminEventAdapter
import com.zenith26.fest.ui.admin.CategoryStatAdapter
import com.zenith26.fest.ui.admin.RegistrationAdapter
import com.zenith26.fest.ui.portal.EventAdapter
import com.zenith26.fest.ui.portal.EventDetailDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Portal State
    private var currentCategory: String = "All"
    private var currentSearch: String = ""
    private var searchDebounceJob: Job? = null
    private lateinit var eventAdapter: EventAdapter

    // Admin State
    private lateinit var adminEventAdapter: AdminEventAdapter
    private lateinit var registrationAdapter: RegistrationAdapter
    private lateinit var categoryStatAdapter: CategoryStatAdapter
    private var allEventsCache = listOf<Event>()
    private var selectedRegistrationEventId: Int? = null

    // View Mode: 0 = Portal, 1 = Admin
    private var currentViewMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopNav()
        setupPortalUI()
        setupAdminUI()
        setupClubhouseRunners()

        // Initial load
        showPortalView()
    }

    // ========================================================
    // Top Navigation & Server Settings
    // ========================================================

    private fun setupTopNav() {
        binding.btnNavEvents.setOnClickListener {
            showPortalView()
        }

        binding.btnNavAdmin.setOnClickListener {
            showAdminView()
        }

        binding.btnServerConfig.setOnClickListener {
            showServerConfigDialog()
        }
    }

    private fun showPortalView() {
        currentViewMode = 0
        binding.portalBadge.text = "FEST PORTAL"
        binding.btnNavEvents.setBackgroundResource(R.drawable.bg_button_indigo)
        binding.btnNavEvents.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.btnNavAdmin.setBackgroundResource(R.drawable.bg_button_slate)
        binding.btnNavAdmin.setTextColor(ContextCompat.getColor(this, R.color.slate_300))

        binding.swipeRefreshPortal.visibility = View.VISIBLE
        binding.swipeRefreshAdmin.visibility = View.GONE

        fetchPortalEvents()
    }

    private fun showAdminView() {
        currentViewMode = 1
        binding.portalBadge.text = "COORDINATOR DASHBOARD"
        binding.btnNavEvents.setBackgroundResource(R.drawable.bg_button_slate)
        binding.btnNavEvents.setTextColor(ContextCompat.getColor(this, R.color.slate_300))
        binding.btnNavAdmin.setBackgroundResource(R.drawable.bg_button_indigo)
        binding.btnNavAdmin.setTextColor(ContextCompat.getColor(this, R.color.white))

        binding.swipeRefreshPortal.visibility = View.GONE
        binding.swipeRefreshAdmin.visibility = View.VISIBLE

        loadAdminDashboard()
    }

    private fun showServerConfigDialog() {
        val dialog = Dialog(this)
        val dialogBinding = DialogServerConfigBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.editServerUrl.setText(ApiClient.getBaseUrl())

        dialogBinding.btnCancelServerConfig.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnSaveServerConfig.setOnClickListener {
            val newUrl = dialogBinding.editServerUrl.text.toString().trim()
            if (newUrl.isNotEmpty()) {
                ApiClient.updateBaseUrl(this, newUrl)
                dialog.dismiss()
                Toast.makeText(this, "Server URL updated: $newUrl", Toast.LENGTH_SHORT).show()
                if (currentViewMode == 0) {
                    fetchPortalEvents()
                } else {
                    loadAdminDashboard()
                }
            }
        }
        dialog.show()
    }

    // ========================================================
    // Student Event Portal Logic
    // ========================================================

    private fun setupPortalUI() {
        eventAdapter = EventAdapter { event ->
            EventDetailDialog(this, event.id) {
                // On registration success, refresh portal and admin
                fetchPortalEvents()
            }.show()
        }

        binding.recyclerEvents.layoutManager = LinearLayoutManager(this)
        binding.recyclerEvents.adapter = eventAdapter

        binding.swipeRefreshPortal.setOnRefreshListener {
            fetchPortalEvents()
        }

        binding.portalRetryBtn.setOnClickListener {
            fetchPortalEvents()
        }

        binding.portalResetFiltersBtn.setOnClickListener {
            resetPortalFilters()
        }

        // Search Input
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s?.toString()?.trim() ?: ""
                binding.clearSearchBtn.visibility = if (currentSearch.isNotEmpty()) View.VISIBLE else View.GONE

                searchDebounceJob?.cancel()
                searchDebounceJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(300)
                    fetchPortalEvents()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearSearchBtn.setOnClickListener {
            binding.searchInput.text.clear()
            currentSearch = ""
            binding.clearSearchBtn.visibility = View.GONE
            fetchPortalEvents()
        }

        // Category Filter Buttons
        setupCategoryButton(binding.filterAll, "All")
        setupCategoryButton(binding.filterTechnical, "Technical")
        setupCategoryButton(binding.filterCultural, "Cultural")
        setupCategoryButton(binding.filterSports, "Sports")
        setupCategoryButton(binding.filterGaming, "Gaming")
        setupCategoryButton(binding.filterWorkshop, "Workshop")
    }

    private fun setupCategoryButton(button: TextView, category: String) {
        button.setOnClickListener {
            currentCategory = category
            updateCategoryFilterUI()
            fetchPortalEvents()
        }
    }

    private fun updateCategoryFilterUI() {
        val buttons = listOf(
            binding.filterAll to "All",
            binding.filterTechnical to "Technical",
            binding.filterCultural to "Cultural",
            binding.filterSports to "Sports",
            binding.filterGaming to "Gaming",
            binding.filterWorkshop to "Workshop"
        )

        buttons.forEach { (btn, cat) ->
            if (cat.equals(currentCategory, ignoreCase = true)) {
                btn.setBackgroundResource(R.drawable.bg_category_active)
                btn.setTextColor(ContextCompat.getColor(this, R.color.butter_200))
            } else {
                btn.setBackgroundResource(R.drawable.bg_category_inactive)
                btn.setTextColor(ContextCompat.getColor(this, R.color.coffee_800))
            }
        }
    }

    private fun resetPortalFilters() {
        currentCategory = "All"
        currentSearch = ""
        binding.searchInput.text.clear()
        binding.clearSearchBtn.visibility = View.GONE
        updateCategoryFilterUI()
        fetchPortalEvents()
    }

    private fun fetchPortalEvents() {
        binding.portalLoadingState.visibility = View.VISIBLE
        binding.portalErrorState.visibility = View.GONE
        binding.portalEmptyState.visibility = View.GONE

        val categoryParam = if (currentCategory.equals("All", ignoreCase = true)) null else currentCategory
        val searchParam = currentSearch.ifEmpty { null }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.getService().getEvents(
                    category = categoryParam,
                    search = searchParam
                )

                withContext(Dispatchers.Main) {
                    binding.portalLoadingState.visibility = View.GONE
                    binding.swipeRefreshPortal.isRefreshing = false

                    if (response.isSuccessful && response.body() != null) {
                        val events = response.body()!!
                        binding.eventCountDisplay.text = "Showing ${events.size} events"

                        if (events.isEmpty()) {
                            binding.portalEmptyState.visibility = View.VISIBLE
                            eventAdapter.submitList(emptyList())
                        } else {
                            binding.portalEmptyState.visibility = View.GONE
                            eventAdapter.submitList(events)
                        }
                    } else {
                        binding.portalErrorState.visibility = View.VISIBLE
                        binding.portalErrorMessage.text = ApiClient.parseErrorMessage(response)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.portalLoadingState.visibility = View.GONE
                    binding.swipeRefreshPortal.isRefreshing = false
                    binding.portalErrorState.visibility = View.VISIBLE
                    binding.portalErrorMessage.text =
                        "Could not connect to the backend server (${ApiClient.getBaseUrl()}). Please make sure FastAPI is running: ${e.localizedMessage}"
                }
            }
        }
    }

    // ========================================================
    // Admin Dashboard Logic
    // ========================================================

    private fun setupAdminUI() {
        adminEventAdapter = AdminEventAdapter()
        binding.recyclerAdminEvents.layoutManager = LinearLayoutManager(this)
        binding.recyclerAdminEvents.adapter = adminEventAdapter

        registrationAdapter = RegistrationAdapter { reg ->
            showCancelRegistrationConfirmDialog(reg)
        }
        binding.recyclerRegistrations.layoutManager = LinearLayoutManager(this)
        binding.recyclerRegistrations.adapter = registrationAdapter

        categoryStatAdapter = CategoryStatAdapter()
        binding.recyclerCategoryStats.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerCategoryStats.adapter = categoryStatAdapter

        binding.swipeRefreshAdmin.setOnRefreshListener {
            loadAdminDashboard()
        }

        binding.adminBtnRefresh.setOnClickListener {
            loadAdminDashboard()
        }

        binding.adminBtnRetry.setOnClickListener {
            loadAdminDashboard()
        }

        binding.adminBtnAddEvent.setOnClickListener {
            AddEventDialog(this) {
                loadAdminDashboard()
            }.show()
        }

        // Tab Navigation
        binding.adminTabEvents.setOnClickListener {
            binding.adminTabEvents.setBackgroundResource(R.drawable.bg_category_active)
            binding.adminTabEvents.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.adminTabRegistrations.setBackgroundResource(R.drawable.bg_category_inactive)
            binding.adminTabRegistrations.setTextColor(ContextCompat.getColor(this, R.color.slate_700))

            binding.adminSectionEvents.visibility = View.VISIBLE
            binding.adminSectionRegistrations.visibility = View.GONE
        }

        binding.adminTabRegistrations.setOnClickListener {
            binding.adminTabRegistrations.setBackgroundResource(R.drawable.bg_category_active)
            binding.adminTabRegistrations.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.adminTabEvents.setBackgroundResource(R.drawable.bg_category_inactive)
            binding.adminTabEvents.setTextColor(ContextCompat.getColor(this, R.color.slate_700))

            binding.adminSectionEvents.visibility = View.GONE
            binding.adminSectionRegistrations.visibility = View.VISIBLE
        }
    }

    private fun loadAdminDashboard() {
        binding.adminErrorBanner.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch stats, events, and registrations concurrently
                val statsDeferred = ApiClient.getService().getStats()
                val eventsDeferred = ApiClient.getService().getEvents()
                val registrationsDeferred = ApiClient.getService().getRegistrations(selectedRegistrationEventId)

                withContext(Dispatchers.Main) {
                    binding.swipeRefreshAdmin.isRefreshing = false

                    if (statsDeferred.isSuccessful && statsDeferred.body() != null) {
                        val stats = statsDeferred.body()!!
                        binding.statTotalEvents.text = stats.totalEvents.toString()
                        binding.statTotalRegistrations.text = stats.totalRegistrations.toString()
                        binding.statUtilizationPct.text = "${stats.overallUtilizationPct}%"
                        binding.statCapacityFraction.text = "of ${stats.totalCapacity} seats"
                        binding.statUtilizationBar.progress = stats.overallUtilizationPct.toInt().coerceIn(0, 100)
                        binding.statUpcomingEvents.text = stats.upcomingEventsCount.toString()

                        categoryStatAdapter.submitMap(stats.eventsByCategory)
                    }

                    if (eventsDeferred.isSuccessful && eventsDeferred.body() != null) {
                        allEventsCache = eventsDeferred.body()!!
                        binding.adminEventsCount.text = "Showing ${allEventsCache.size} events"
                        adminEventAdapter.submitList(allEventsCache)
                        populateAdminEventFilterDropdown(allEventsCache)
                    }

                    if (registrationsDeferred.isSuccessful && registrationsDeferred.body() != null) {
                        val regs = registrationsDeferred.body()!!
                        binding.adminRegistrationsCount.text = "Showing ${regs.size} registrations"
                        registrationAdapter.submitList(regs)
                    }

                    val currentTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                    binding.adminSyncStatus.text = "● Synced $currentTime"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefreshAdmin.isRefreshing = false
                    binding.adminErrorBanner.visibility = View.VISIBLE
                    binding.adminErrorText.text = "Failed to load dashboard: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun populateAdminEventFilterDropdown(events: List<Event>) {
        val options = mutableListOf("All Events")
        val idList = mutableListOf<Int?>(null)

        events.forEach { ev ->
            options.add("${ev.title} (${ev.category})")
            idList.add(ev.id)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        binding.spinnerRegistrationEvents.adapter = adapter

        // Set selected index
        val selectedIndex = idList.indexOf(selectedRegistrationEventId).coerceAtLeast(0)
        binding.spinnerRegistrationEvents.setSelection(selectedIndex)

        binding.spinnerRegistrationEvents.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newId = idList[position]
                if (newId != selectedRegistrationEventId) {
                    selectedRegistrationEventId = newId
                    fetchRegistrationsOnly()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchRegistrationsOnly() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = ApiClient.getService().getRegistrations(selectedRegistrationEventId)
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful && res.body() != null) {
                        val regs = res.body()!!
                        binding.adminRegistrationsCount.text = "Showing ${regs.size} registrations"
                        registrationAdapter.submitList(regs)
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun showCancelRegistrationConfirmDialog(reg: Registration) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Registration")
            .setMessage("Are you sure you want to cancel registration #${reg.id} for \"${reg.studentName}\" in \"${reg.eventTitle}\"?\nThis will immediately free up one seat.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelRegistration(reg.id)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelRegistration(regId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.getService().deleteRegistration(regId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Registration #$regId cancelled successfully", Toast.LENGTH_SHORT).show()
                        loadAdminDashboard()
                        if (currentViewMode == 0) {
                            fetchPortalEvents()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed: ${ApiClient.parseErrorMessage(response)}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private var runnerScrollJob: Job? = null

    private fun setupClubhouseRunners() {
        val runners = listOf(
            Triple(binding.runnerMickeyView, "🐭 Mickey Mouse", "Oh Boy! Welcome to Zenith '26! 🎈"),
            Triple(binding.runnerMinnieView, "🎀 Minnie Mouse", "Yoo-Hoo! Sweet fest treats! 🎀"),
            Triple(binding.runnerDonaldView, "🦆 Donald Duck", "Aw, Phooey! Let's Go! 🦆"),
            Triple(binding.runnerDaisyView, "💜 Daisy Duck", "Simply fabulous! 💜"),
            Triple(binding.runnerGoofyView, "🐕 Goofy", "Gawrsh! Hot dog, hot diggity dog! 🐕")
        )

        runners.forEach { (view, name, quote) ->
            view.setOnClickListener {
                view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).withEndAction {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                }.start()
                Toast.makeText(this, "$name: \"$quote\"", Toast.LENGTH_SHORT).show()
            }
        }

        // Auto-run animation across the scroll track
        runnerScrollJob?.cancel()
        runnerScrollJob = CoroutineScope(Dispatchers.Main).launch {
            var scrollX = 0
            var direction = 1
            while (true) {
                delay(30)
                val maxScroll = binding.clubhouseCharactersContainer.width - binding.clubhouseHorizontalScroll.width
                if (maxScroll > 0) {
                    scrollX += (2 * direction)
                    if (scrollX >= maxScroll) {
                        scrollX = maxScroll
                        direction = -1
                        delay(1200)
                    } else if (scrollX <= 0) {
                        scrollX = 0
                        direction = 1
                        delay(1200)
                    }
                    binding.clubhouseHorizontalScroll.scrollTo(scrollX, 0)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runnerScrollJob?.cancel()
        searchDebounceJob?.cancel()
    }
}

