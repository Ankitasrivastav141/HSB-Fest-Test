package com.zenith26.fest.data.model

import com.google.gson.annotations.SerializedName

data class CreateEventRequest(
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("event_time") val eventTime: String,
    @SerializedName("venue") val venue: String,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("fee") val fee: Int = 0,
    @SerializedName("icon") val icon: String? = "⚡"
)
