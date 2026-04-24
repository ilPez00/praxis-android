package com.praxis.app.integrations

/**
 * Enum representing all supported external app integrations
 */
enum class IntegrationType(
    val id: String,
    val displayName: String,
    val description: String,
    val iconResId: Int,
    val requiresOAuth: Boolean,
    val requiredPermissions: List<String> = emptyList()
) {
    HEALTH_CONNECT(
        id = "health_connect",
        displayName = "Health Connect",
        description = "Steps, workouts, sleep, heart rate, weight",
        iconResId = android.R.drawable.ic_menu_today,
        requiresOAuth = false,
        requiredPermissions = listOf(
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_WORKOUTS",
            "android.permission.health.READ_SLEEP",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_WEIGHT"
        )
    ),
    STRAVA(
        id = "strava",
        displayName = "Strava",
        description = "Running, cycling, hiking activities",
        iconResId = android.R.drawable.ic_menu_compass,
        requiresOAuth = true
    ),
    FITBIT(
        id = "fitbit",
        displayName = "Fitbit",
        description = "Activity, sleep, heart rate tracking",
        iconResId = android.R.drawable.ic_menu_mylocation,
        requiresOAuth = true
    ),
    GOOGLE_CALENDAR(
        id = "google_calendar",
        displayName = "Google Calendar",
        description = "Events and appointments",
        iconResId = android.R.drawable.ic_menu_my_calendar,
        requiresOAuth = true
    ),
    YAZIO(
        id = "yazio",
        displayName = "Yazio",
        description = "Nutrition and meal tracking",
        iconResId = android.R.drawable.ic_menu_edit,
        requiresOAuth = true
    ),
    GOOGLE_FIT(
        id = "google_fit",
        displayName = "Google Fit",
        description = "Activity and health metrics",
        iconResId = android.R.drawable.ic_menu_today,
        requiresOAuth = true
    );

    companion object {
        fun fromId(id: String): IntegrationType? = values().find { it.id == id }
    }
}
