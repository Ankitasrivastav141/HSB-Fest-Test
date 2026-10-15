package com.zenith26.fest.data.api

import com.zenith26.fest.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("api/events")
    suspend fun getEvents(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): Response<List<Event>>

    @GET("api/events/{event_id}")
    suspend fun getEventDetail(
        @Path("event_id") eventId: Int
    ): Response<Event>

    @POST("api/events/{event_id}/register")
    suspend fun registerForEvent(
        @Path("event_id") eventId: Int,
        @Body request: RegistrationRequest
    ): Response<RegistrationResponse>

    @GET("api/registrations")
    suspend fun getRegistrations(
        @Query("event_id") eventId: Int? = null
    ): Response<List<Registration>>

    @GET("api/stats")
    suspend fun getStats(): Response<Stats>

    @POST("api/events")
    suspend fun createEvent(
        @Body request: CreateEventRequest
    ): Response<Event>

    @DELETE("api/registrations/{registration_id}")
    suspend fun deleteRegistration(
        @Path("registration_id") registrationId: Int
    ): Response<Map<String, Any>>
}
