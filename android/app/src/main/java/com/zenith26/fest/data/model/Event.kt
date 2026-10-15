package com.zenith26.fest.data.model

import com.google.gson.annotations.SerializedName

data class Event(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("event_time") val eventTime: String,
    @SerializedName("venue") val venue: String,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("fee") val fee: Int = 0,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("available_seats") val availableSeats: Int
) {
    val booked: Int
        get() = (capacity - availableSeats).coerceAtLeast(0)

    val utilizationPct: Double
        get() = if (capacity > 0) {
            Math.round((booked.toDouble() / capacity.toDouble() * 100.0) * 10.0) / 10.0
        } else 0.0

    val isFull: Boolean
        get() = availableSeats <= 0

    val isHighDemand: Boolean
        get() = utilizationPct >= 90.0
}
