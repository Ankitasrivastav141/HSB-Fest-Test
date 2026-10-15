// frontend/js/app.js
// Client-side controller for Zenith '26 Student Event Portal

document.addEventListener("DOMContentLoaded", () => {
  // State variables
  let currentCategory = "All";
  let currentSearch = "";
  let searchDebounceTimer = null;
  let activeEventData = null;

  // DOM Elements
  const eventsGrid = document.getElementById("events-grid");
  const loadingState = document.getElementById("loading-state");
  const errorState = document.getElementById("error-state");
  const errorMessageText = document.getElementById("error-message-text");
  const emptyState = document.getElementById("empty-state");
  const eventCountDisplay = document.getElementById("event-count-display");
  const searchInput = document.getElementById("search-input");
  const clearSearchBtn = document.getElementById("clear-search-btn");
  const retryBtn = document.getElementById("retry-btn");
  const resetFiltersBtn = document.getElementById("reset-filters-btn");
  const categoryButtons = document.querySelectorAll(".category-btn");

  // Modal DOM Elements
  const detailModal = document.getElementById("detail-modal");
  const closeModalBtn = document.getElementById("close-modal-btn");
  const cancelModalBtn = document.getElementById("cancel-modal-btn");
  const modalEventIcon = document.getElementById("modal-event-icon");
  const modalEventTitle = document.getElementById("modal-event-title");
  const modalEventCategory = document.getElementById("modal-event-category");
  const modalEventFee = document.getElementById("modal-event-fee");
  const modalEventDateTime = document.getElementById("modal-event-datetime");
  const modalEventVenue = document.getElementById("modal-event-venue");
  const modalEventSeats = document.getElementById("modal-event-seats");
  const modalEventDescription = document.getElementById("modal-event-description");

  // Form DOM Elements
  const registrationForm = document.getElementById("registration-form");
  const regEventId = document.getElementById("reg-event-id");
  const regName = document.getElementById("reg-name");
  const regRoll = document.getElementById("reg-roll");
  const regEmail = document.getElementById("reg-email");
  const regPhone = document.getElementById("reg-phone");
  const errorName = document.getElementById("error-name");
  const errorRoll = document.getElementById("error-roll");
  const errorEmail = document.getElementById("error-email");
  const formErrorAlert = document.getElementById("form-error-alert");
  const formErrorText = document.getElementById("form-error-text");
  const formSuccessAlert = document.getElementById("form-success-alert");
  const formSuccessText = document.getElementById("form-success-text");
  const submitRegBtn = document.getElementById("submit-reg-btn");
  const submitBtnSpinner = document.getElementById("submit-btn-spinner");
  const submitBtnText = document.getElementById("submit-btn-text");

  // Category Color Helper
  // Category Color Helper (Ice Cream & Coffee Theme)
  const categoryStyles = {
    Technical: { badge: "bg-amber-100/90 text-amber-950 border-amber-300", label: "☕ Dark Roast Tech" },
    Cultural: { badge: "bg-pink-100/90 text-pink-950 border-pink-300", label: "🍨 Vanilla Culture" },
    Sports: { badge: "bg-emerald-100/90 text-emerald-950 border-emerald-300", label: "⚡ Espresso Sports" },
    Gaming: { badge: "bg-purple-100/90 text-purple-950 border-purple-300", label: "🍫 Choco Gaming" },
    Workshop: { badge: "bg-yellow-100/90 text-amber-950 border-amber-400", label: "🧇 Waffle Workshop" }
  };

  /**
   * Fetch events from GET /api/events with category and search query parameters.
   */
  async function fetchEvents() {
    // Show loading state
    eventsGrid.innerHTML = "";
    loadingState.classList.remove("hidden");
    errorState.classList.add("hidden");
    emptyState.classList.add("hidden");

    try {
      const params = new URLSearchParams();
      if (currentCategory && currentCategory !== "All") {
        params.append("category", currentCategory);
      }
      if (currentSearch && currentSearch.trim() !== "") {
        params.append("search", currentSearch.trim());
      }

      const url = `/api/events${params.toString() ? "?" + params.toString() : ""}`;
      const response = await fetch(url);

      if (!response.ok) {
        throw new Error(`Server returned status ${response.status}: ${response.statusText}`);
      }

      const events = await response.json();
      loadingState.classList.add("hidden");
      renderEvents(events);
    } catch (err) {
      console.error("Error fetching events:", err);
      loadingState.classList.add("hidden");
      errorState.classList.remove("hidden");
      errorMessageText.textContent = `Unable to connect to /api/events: ${err.message}. Please verify the backend server is running.`;
    }
  }

  /**
   * Render list of events inside #events-grid
   */
  function renderEvents(events) {
    eventCountDisplay.textContent = events.length;

    if (!events || events.length === 0) {
      emptyState.classList.remove("hidden");
      return;
    }

    eventsGrid.innerHTML = "";

    events.forEach(event => {
      const card = document.createElement("div");
      card.className = "bg-white rounded-3xl border-2 border-amber-200/80 creamy-card flex flex-col overflow-hidden animate-fade-in group";

      const catStyle = categoryStyles[event.category] || { badge: "bg-amber-50 text-coffee-800 border-amber-200", label: event.category };
      const isFull = event.available_seats <= 0;
      const feeLabel = event.fee > 0 ? `🍫 ₹${event.fee} Treat` : "✨ Free Scoop";

      card.innerHTML = `
        <div class="p-6 flex-grow flex flex-col justify-between space-y-4">
          <!-- Card Header: Icon, Category & Seats Badge -->
          <div class="flex items-start justify-between gap-3">
            <div class="w-13 h-13 rounded-2xl bg-gradient-to-br from-amber-50 to-amber-100 border border-amber-200/80 flex items-center justify-center text-2xl shadow-inner group-hover:scale-110 group-hover:rotate-6 transition duration-300">
              ${event.icon || "🍨"}
            </div>
            <div class="flex flex-col items-end gap-1.5">
              <span class="text-[11px] font-extrabold px-3 py-1 rounded-full border shadow-xs ${catStyle.badge}">
                ${catStyle.label || event.category}
              </span>
              ${isFull 
                ? `<span class="text-[10px] font-black px-2.5 py-0.5 rounded-full bg-coffee-950 text-amber-200 border border-amber-600/50 uppercase tracking-wider">🍫 SOLD OUT</span>`
                : `<span class="text-[11px] font-semibold text-coffee-700 bg-amber-50/80 px-2.5 py-0.5 rounded-md border border-amber-200">🍨 ${event.available_seats} / ${event.capacity} scoops left</span>`
              }
            </div>
          </div>

          <!-- Title & Brief Description -->
          <div class="space-y-1.5">
            <h3 class="text-lg font-bold text-coffee-950 font-display group-hover:text-amber-700 transition leading-snug">
              ${escapeHtml(event.title)}
            </h3>
            <p class="text-xs text-coffee-600 line-clamp-2 leading-relaxed">
              ${escapeHtml(event.description || "Join this decadent campus fest experience at Zenith '26.")}
            </p>
          </div>

          <!-- Logistics: Date, Time, Venue -->
          <div class="space-y-1.5 pt-3 border-t border-amber-100 text-xs text-coffee-700">
            <div class="flex items-center gap-2">
              <span>📅</span>
              <span class="font-medium">${event.event_date} · ${event.event_time}</span>
            </div>
            <div class="flex items-center gap-2">
              <span>📍</span>
              <span class="font-medium truncate">${escapeHtml(event.venue)}</span>
            </div>
          </div>
        </div>

        <!-- Card Footer: Fee and Action Button -->
        <div class="px-6 py-4 bg-[#fffdf8] border-t-2 border-amber-100 flex items-center justify-between">
          <div class="text-xs font-black ${event.fee > 0 ? 'text-coffee-900' : 'text-amber-700'}">
            ${feeLabel}
          </div>
          <button 
            data-event-id="${event.id}" 
            class="view-details-btn px-4 py-2.5 rounded-2xl text-xs font-bold ${isFull ? 'bg-coffee-900 hover:bg-coffee-800 text-amber-200' : 'bg-gradient-to-r from-butter-300 via-butter-400 to-amber-500 hover:from-butter-200 hover:to-amber-400 text-coffee-950 shadow-md shadow-amber-900/10'} transition active:scale-95 flex items-center gap-1.5"
          >
            <span>${isFull ? 'View Scoop' : 'Taste & Reserve'}</span>
            <span>→</span>
          </button>
        </div>
      `;

      eventsGrid.appendChild(card);
    });

    // Attach click listeners to "View Details" buttons
    document.querySelectorAll(".view-details-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-event-id");
        openEventDetail(id);
      });
    });
  }

  /**
   * Fetch specific event details and open the modal
   */
  async function openEventDetail(eventId) {
    try {
      const res = await fetch(`/api/events/${eventId}`);
      if (!res.ok) {
        alert("Failed to load event details.");
        return;
      }
      activeEventData = await res.json();

      // Reset form states
      registrationForm.reset();
      clearFormAlerts();
      regEventId.value = activeEventData.id;

      // Populate Modal Info
      modalEventIcon.textContent = activeEventData.icon || "⚡";
      modalEventTitle.textContent = activeEventData.title;
      modalEventCategory.textContent = activeEventData.category;
      modalEventFee.textContent = activeEventData.fee > 0 ? `Fee: ₹${activeEventData.fee}` : "Free Entry";
      modalEventDateTime.textContent = `${activeEventData.event_date} · ${activeEventData.event_time}`;
      modalEventVenue.textContent = activeEventData.venue;

      if (activeEventData.available_seats <= 0) {
        modalEventSeats.innerHTML = `<span class="text-red-600 font-bold">FULL (0 / ${activeEventData.capacity} available)</span>`;
        submitRegBtn.disabled = true;
        submitRegBtn.classList.add("opacity-50", "cursor-not-allowed");
        submitBtnText.textContent = "Event Full";
      } else {
        modalEventSeats.innerHTML = `<span class="text-emerald-600 font-bold">${activeEventData.available_seats}</span> / ${activeEventData.capacity} seats remaining`;
        submitRegBtn.disabled = false;
        submitRegBtn.classList.remove("opacity-50", "cursor-not-allowed");
        submitBtnText.textContent = "Confirm Registration";
      }

      modalEventDescription.textContent = activeEventData.description || "No specific description provided.";

      // Display Modal
      detailModal.classList.remove("hidden");
      document.body.classList.add("overflow-hidden");
    } catch (err) {
      console.error("Error opening event detail:", err);
      alert("Error loading event information.");
    }
  }

  /**
   * Close the modal
   */
  function closeModal() {
    detailModal.classList.add("hidden");
    document.body.classList.remove("overflow-hidden");
    clearFormAlerts();
  }

  function clearFormAlerts() {
    formErrorAlert.classList.add("hidden");
    formSuccessAlert.classList.add("hidden");
    errorName.classList.add("hidden");
    errorRoll.classList.add("hidden");
    errorEmail.classList.add("hidden");
  }

  /**
   * Handle Registration Form Submission
   */
  registrationForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearFormAlerts();

    const eventId = regEventId.value;
    const studentName = regName.value.trim();
    const rollNumber = regRoll.value.trim();
    const email = regEmail.value.trim();
    const phone = regPhone.value.trim();

    // Client-side validation checks
    let isValid = true;
    if (!studentName) {
      errorName.classList.remove("hidden");
      isValid = false;
    }
    if (!rollNumber) {
      errorRoll.classList.remove("hidden");
      isValid = false;
    }
    if (!email || !email.includes("@")) {
      errorEmail.classList.remove("hidden");
      isValid = false;
    }

    if (!isValid) {
      return;
    }

    // Set loading state on submit button
    submitRegBtn.disabled = true;
    submitBtnSpinner.classList.remove("hidden");
    submitBtnText.textContent = "Registering...";

    try {
      const response = await fetch(`/api/events/${eventId}/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          student_name: studentName,
          roll_number: rollNumber,
          email: email,
          phone: phone || null
        })
      });

      const data = await response.json();

      if (response.status === 201) {
        // Step 7: On success (201), show clear confirmation message with event name
        formSuccessAlert.classList.remove("hidden");
        formSuccessText.textContent = `Congratulations, ${studentName}! You are officially registered for "${activeEventData.title}". A confirmation has been recorded.`;
        registrationForm.reset();

        // Refresh events list to reflect updated seat count
        await fetchEvents();

        // Also update seats in current open modal
        if (activeEventData) {
          activeEventData.available_seats = Math.max(0, activeEventData.available_seats - 1);
          modalEventSeats.innerHTML = `<span class="text-emerald-600 font-bold">${activeEventData.available_seats}</span> / ${activeEventData.capacity} seats remaining`;
        }

        // Auto close modal after 2.5 seconds
        setTimeout(() => {
          closeModal();
        }, 2200);

      } else {
        // Step 7: On failure (400/409), show exact error message the server sent back
        formErrorAlert.classList.remove("hidden");
        formErrorText.textContent = data.detail || "Registration failed. Please try again.";
      }
    } catch (err) {
      console.error("Registration request failed:", err);
      formErrorAlert.classList.remove("hidden");
      formErrorText.textContent = "Network error: unable to reach the registration server.";
    } finally {
      submitRegBtn.disabled = false;
      submitBtnSpinner.classList.add("hidden");
      submitBtnText.textContent = "Confirm Registration";
    }
  });

  // Modal close triggers
  closeModalBtn.addEventListener("click", closeModal);
  cancelModalBtn.addEventListener("click", closeModal);
  detailModal.addEventListener("click", (e) => {
    if (e.target === detailModal) closeModal();
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && !detailModal.classList.contains("hidden")) {
      closeModal();
    }
  });

  // Category filter buttons click handler
  categoryButtons.forEach(btn => {
    btn.addEventListener("click", () => {
      categoryButtons.forEach(b => {
        b.classList.remove("bg-coffee-900", "text-butter-200", "border-2", "border-amber-500", "butter-glow");
        b.classList.add("bg-white", "text-coffee-800", "border", "border-amber-200");
      });

      btn.classList.remove("bg-white", "text-coffee-800", "border", "border-amber-200");
      btn.classList.add("bg-coffee-900", "text-butter-200", "border-2", "border-amber-500", "butter-glow");

      currentCategory = btn.getAttribute("data-category");
      fetchEvents();
    });
  });

  // Search input change with debounce
  searchInput.addEventListener("input", (e) => {
    const val = e.target.value;
    currentSearch = val;
    if (val.length > 0) {
      clearSearchBtn.classList.remove("hidden");
    } else {
      clearSearchBtn.classList.add("hidden");
    }

    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(() => {
      fetchEvents();
    }, 250);
  });

  clearSearchBtn.addEventListener("click", () => {
    searchInput.value = "";
    currentSearch = "";
    clearSearchBtn.classList.add("hidden");
    fetchEvents();
  });

  retryBtn.addEventListener("click", fetchEvents);

  resetFiltersBtn.addEventListener("click", () => {
    currentSearch = "";
    searchInput.value = "";
    clearSearchBtn.classList.add("hidden");
    currentCategory = "All";
    categoryButtons.forEach(b => {
      if (b.getAttribute("data-category") === "All") {
        b.classList.remove("bg-white", "text-coffee-800", "border", "border-amber-200");
        b.classList.add("bg-coffee-900", "text-butter-200", "border-2", "border-amber-500", "butter-glow");
      } else {
        b.classList.remove("bg-coffee-900", "text-butter-200", "border-2", "border-amber-500", "butter-glow");
        b.classList.add("bg-white", "text-coffee-800", "border", "border-amber-200");
      }
    });
    fetchEvents();
  });

  function escapeHtml(str) {
    if (!str) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  // Initial load
  fetchEvents();
});
