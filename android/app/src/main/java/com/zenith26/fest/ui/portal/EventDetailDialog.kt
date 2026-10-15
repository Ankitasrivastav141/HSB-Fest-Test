package com.zenith26.fest.ui.portal

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import com.zenith26.fest.R
import com.zenith26.fest.data.api.ApiClient
import com.zenith26.fest.data.model.Event
import com.zenith26.fest.data.model.RegistrationRequest
import com.zenith26.fest.databinding.DialogEventDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventDetailDialog(
    context: Context,
    private val eventId: Int,
    private val onRegistrationSuccess: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogEventDetailBinding
    private var currentEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupListeners()
        loadEventDetails()
    }

    private fun setupListeners() {
        binding.dialogCloseBtn.setOnClickListener { dismiss() }
        binding.btnCancelRegistration.setOnClickListener { dismiss() }

        binding.btnSubmitRegistration.setOnClickListener {
            submitRegistration()
        }
    }

    private fun loadEventDetails() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.getService().getEventDetail(eventId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        currentEvent = response.body()
                        populateUI(currentEvent!!)
                    } else {
                        binding.dialogFormErrorAlert.visibility = View.VISIBLE
                        binding.dialogFormErrorText.text = ApiClient.parseErrorMessage(response)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.dialogFormErrorAlert.visibility = View.VISIBLE
                    binding.dialogFormErrorText.text = "Connection error: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun populateUI(event: Event) {
        binding.dialogEventIcon.text = event.icon ?: "⚡"
        binding.dialogEventTitle.text = event.title
        binding.dialogEventCategory.text = event.category
        binding.dialogEventFee.text = if (event.fee > 0) "Fee: ₹${event.fee}" else "Free Entry"
        binding.dialogEventDatetime.text = "${event.eventDate} · ${event.eventTime}"
        binding.dialogEventVenue.text = event.venue
        binding.dialogEventDescription.text = event.description ?: "No specific description provided."

        if (event.isFull) {
            binding.dialogEventSeats.text = "FULL (0 / ${event.capacity} available)"
            binding.dialogEventSeats.setTextColor(ContextCompat.getColor(context, R.color.red_600))
            binding.btnSubmitRegistration.isEnabled = false
            binding.btnSubmitRegistration.alpha = 0.5f
            binding.btnSubmitRegistration.text = "Event Full"
        } else {
            binding.dialogEventSeats.text = "${event.availableSeats} / ${event.capacity} seats remaining"
            binding.dialogEventSeats.setTextColor(ContextCompat.getColor(context, R.color.emerald_600))
            binding.btnSubmitRegistration.isEnabled = true
            binding.btnSubmitRegistration.alpha = 1.0f
            binding.btnSubmitRegistration.text = "Confirm Registration"
        }
    }

    private fun submitRegistration() {
        // Clear previous alerts
        binding.dialogFormErrorAlert.visibility = View.GONE
        binding.dialogFormSuccessAlert.visibility = View.GONE
        binding.errorRegName.visibility = View.GONE
        binding.errorRegRoll.visibility = View.GONE
        binding.errorRegEmail.visibility = View.GONE

        val name = binding.inputRegName.text.toString().trim()
        val roll = binding.inputRegRoll.text.toString().trim()
        val email = binding.inputRegEmail.text.toString().trim()
        val phone = binding.inputRegPhone.text.toString().trim().ifEmpty { null }

        var isValid = true
        if (name.isEmpty()) {
            binding.errorRegName.visibility = View.VISIBLE
            isValid = false
        }
        if (roll.isEmpty()) {
            binding.errorRegRoll.visibility = View.VISIBLE
            isValid = false
        }
        if (email.isEmpty() || !email.contains("@")) {
            binding.errorRegEmail.visibility = View.VISIBLE
            isValid = false
        }

        if (!isValid) return

        // Set loading state
        binding.btnSubmitRegistration.isEnabled = false
        binding.btnSubmitRegistration.text = "Registering..."
        binding.progressSubmittingReg.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = RegistrationRequest(
                    studentName = name,
                    rollNumber = roll,
                    email = email,
                    phone = phone
                )
                val response = ApiClient.getService().registerForEvent(eventId, request)

                withContext(Dispatchers.Main) {
                    binding.progressSubmittingReg.visibility = View.GONE
                    binding.btnSubmitRegistration.isEnabled = true
                    binding.btnSubmitRegistration.text = "Confirm Registration"

                    if (response.code() == 201) {
                        // Success: Show confirmation message
                        binding.dialogFormSuccessAlert.visibility = View.VISIBLE
                        val eventTitle = currentEvent?.title ?: "this event"
                        binding.dialogFormSuccessText.text =
                            "Congratulations, $name! You are officially registered for \"$eventTitle\". A confirmation has been recorded."

                        // Clear inputs
                        binding.inputRegName.text.clear()
                        binding.inputRegRoll.text.clear()
                        binding.inputRegEmail.text.clear()
                        binding.inputRegPhone.text.clear()

                        // Update seats count in dialog
                        currentEvent?.let { ev ->
                            val updatedSeats = (ev.availableSeats - 1).coerceAtLeast(0)
                            binding.dialogEventSeats.text = "$updatedSeats / ${ev.capacity} seats remaining"
                        }

                        onRegistrationSuccess()

                        // Auto dismiss after 2.2 seconds
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(2200)
                            if (isShowing) {
                                dismiss()
                            }
                        }
                    } else {
                        // Backend error message
                        val errorMsg = ApiClient.parseErrorMessage(response)
                        binding.dialogFormErrorAlert.visibility = View.VISIBLE
                        binding.dialogFormErrorText.text = errorMsg
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressSubmittingReg.visibility = View.GONE
                    binding.btnSubmitRegistration.isEnabled = true
                    binding.btnSubmitRegistration.text = "Confirm Registration"
                    binding.dialogFormErrorAlert.visibility = View.VISIBLE
                    binding.dialogFormErrorText.text = "Network error: unable to reach the registration server."
                }
            }
        }
    }
}
