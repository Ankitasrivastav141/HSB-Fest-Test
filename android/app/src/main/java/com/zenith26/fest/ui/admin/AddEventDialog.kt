package com.zenith26.fest.ui.admin

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import com.zenith26.fest.data.api.ApiClient
import com.zenith26.fest.data.model.CreateEventRequest
import com.zenith26.fest.databinding.DialogAddEventBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEventDialog(
    context: Context,
    private val onEventCreated: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogAddEventBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupCategorySpinner()
        setupListeners()
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf("Technical", "Cultural", "Sports", "Gaming", "Workshop")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.newEventCategorySpinner.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCancelAddEvent.setOnClickListener { dismiss() }

        binding.btnSubmitAddEvent.setOnClickListener {
            val title = binding.newEventTitle.text.toString().trim()
            val category = binding.newEventCategorySpinner.selectedItem.toString()
            val icon = binding.newEventIcon.text.toString().trim().ifEmpty { "⚡" }
            val venue = binding.newEventVenue.text.toString().trim()
            val date = binding.newEventDate.text.toString().trim()
            val time = binding.newEventTime.text.toString().trim()
            val capacityStr = binding.newEventCapacity.text.toString().trim()
            val feeStr = binding.newEventFee.text.toString().trim()
            val desc = binding.newEventDesc.text.toString().trim().ifEmpty { null }

            val capacity = capacityStr.toIntOrNull() ?: 0
            val fee = feeStr.toIntOrNull() ?: 0

            if (title.isEmpty() || venue.isEmpty() || date.isEmpty() || time.isEmpty()) {
                binding.addEventErrorAlert.visibility = View.VISIBLE
                binding.addEventErrorAlert.text = "Title, category, venue, date, and time are required."
                return@setOnClickListener
            }

            if (capacity <= 0) {
                binding.addEventErrorAlert.visibility = View.VISIBLE
                binding.addEventErrorAlert.text = "Capacity must be greater than 0."
                return@setOnClickListener
            }

            val request = CreateEventRequest(
                title = title,
                category = category,
                description = desc,
                eventDate = date,
                eventTime = time,
                venue = venue,
                capacity = capacity,
                fee = fee,
                icon = icon
            )

            binding.btnSubmitAddEvent.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val res = ApiClient.getService().createEvent(request)
                    withContext(Dispatchers.Main) {
                        binding.btnSubmitAddEvent.isEnabled = true
                        if (res.isSuccessful) {
                            dismiss()
                            onEventCreated()
                        } else {
                            binding.addEventErrorAlert.visibility = View.VISIBLE
                            binding.addEventErrorAlert.text = ApiClient.parseErrorMessage(res)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.btnSubmitAddEvent.isEnabled = true
                        binding.addEventErrorAlert.visibility = View.VISIBLE
                        binding.addEventErrorAlert.text = "Error: ${e.localizedMessage}"
                    }
                }
            }
        }
    }
}
