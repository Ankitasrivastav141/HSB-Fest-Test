package com.zenith26.fest.ui.portal

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zenith26.fest.R
import com.zenith26.fest.data.model.Event
import com.zenith26.fest.databinding.ItemEventCardBinding

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val events = mutableListOf<Event>()

    fun submitList(newList: List<Event>) {
        events.clear()
        events.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(
        private val binding: ItemEventCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            val context = itemView.context

            binding.cardEventIcon.text = event.icon ?: "⚡"
            binding.cardEventTitle.text = event.title
            binding.cardEventDescription.text = event.description ?: "Join this exciting fest event at Zenith '26."
            binding.cardEventDatetime.text = "📅 ${event.eventDate} · ${event.eventTime}"
            binding.cardEventVenue.text = "📍 ${event.venue}"

            // Category Styling
            binding.cardEventCategory.text = event.category
            applyCategoryStyle(binding.cardEventCategory, event.category)

            // Availability Badge
            if (event.isFull) {
                binding.cardEventSeats.text = "FULL"
                binding.cardEventSeats.setTextColor(ContextCompat.getColor(context, R.color.red_700))
                binding.cardEventSeats.setBackgroundResource(R.drawable.bg_badge_full)
                binding.cardBtnRegister.text = "View Event →"
                binding.cardBtnRegister.setBackgroundResource(R.drawable.bg_button_slate)
            } else {
                binding.cardEventSeats.text = "${event.availableSeats} / ${event.capacity} left"
                binding.cardEventSeats.setTextColor(ContextCompat.getColor(context, R.color.slate_600))
                binding.cardEventSeats.setBackgroundResource(R.drawable.bg_badge_seats)
                binding.cardBtnRegister.text = "View & Register →"
                binding.cardBtnRegister.setBackgroundResource(R.drawable.bg_button_indigo)
            }

            // Fee
            if (event.fee > 0) {
                binding.cardEventFee.text = "₹${event.fee}"
                binding.cardEventFee.setTextColor(ContextCompat.getColor(context, R.color.brand_600))
            } else {
                binding.cardEventFee.text = "Free"
                binding.cardEventFee.setTextColor(ContextCompat.getColor(context, R.color.emerald_600))
            }

            // Click Handlers
            binding.cardBtnRegister.setOnClickListener { onEventClick(event) }
            binding.root.setOnClickListener { onEventClick(event) }
        }

        private fun applyCategoryStyle(view: android.widget.TextView, category: String) {
            val (bgColor, textColor, strokeColor) = when (category.lowercase()) {
                "technical" -> Triple("#EFF6FF", "#1D4ED8", "#BFDBFE")
                "cultural" -> Triple("#FAF5FF", "#7E22CE", "#E9D5FF")
                "sports" -> Triple("#ECFDF5", "#047857", "#A7F3D0")
                "gaming" -> Triple("#FFF1F2", "#BE123C", "#FECDD3")
                "workshop" -> Triple("#FFFBEB", "#B45309", "#FDE68A")
                else -> Triple("#F1F5F9", "#334155", "#E2E8F0")
            }

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(Color.parseColor(bgColor))
                setStroke(2, Color.parseColor(strokeColor))
            }
            view.background = drawable
            view.setTextColor(Color.parseColor(textColor))
        }
    }
}
