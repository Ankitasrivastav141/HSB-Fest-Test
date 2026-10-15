package com.zenith26.fest.ui.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zenith26.fest.R
import com.zenith26.fest.data.model.Event
import com.zenith26.fest.databinding.ItemAdminEventRowBinding

class AdminEventAdapter : RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder>() {

    private val events = mutableListOf<Event>()

    fun submitList(newList: List<Event>) {
        events.clear()
        // Sort by utilization percentage descending by default (matches web app step 8)
        events.addAll(newList.sortedByDescending { it.utilizationPct })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminEventViewHolder {
        val binding = ItemAdminEventRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AdminEventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminEventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class AdminEventViewHolder(
        private val binding: ItemAdminEventRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            val context = itemView.context
            binding.adminEventIcon.text = event.icon ?: "⚡"
            binding.adminEventTitle.text = event.title
            binding.adminEventCategory.text = event.category
            binding.adminEventDatetime.text = "📅 ${event.eventDate} · ${event.eventTime}"
            binding.adminEventVenue.text = "📍 ${event.venue}"

            binding.adminEventBookedSeats.text = "${event.booked} / ${event.capacity} booked"
            binding.adminEventUtilization.text = "${event.utilizationPct}%"

            val progress = event.utilizationPct.toInt().coerceIn(0, 100)
            binding.adminEventProgress.progress = progress

            // Warning styling for events exceeding 90% capacity
            if (event.isHighDemand) {
                binding.adminBadgeHighDemand.visibility = View.VISIBLE
                binding.adminEventRowContainer.setBackgroundResource(R.drawable.bg_admin_high_demand)
                binding.adminEventUtilization.setTextColor(ContextCompat.getColor(context, R.color.amber_700))
                binding.adminEventProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
            } else {
                binding.adminBadgeHighDemand.visibility = View.GONE
                binding.adminEventRowContainer.setBackgroundResource(R.drawable.bg_card)
                binding.adminEventUtilization.setTextColor(ContextCompat.getColor(context, R.color.slate_900))
                binding.adminEventProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#4F46E5"))
            }
        }
    }
}
