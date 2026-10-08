package com.example.jeenybot

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BotPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jeeny_garuda_prefs", Context.MODE_PRIVATE)

    // Language: "ar" or "en"
    fun getLanguage(): String = prefs.getString("key_app_language", "ar") ?: "ar"
    fun setLanguage(lang: String) { prefs.edit().putString("key_app_language", lang).apply() }

    // Country Mode: "SAUDI" or "INDONESIA"
    fun getCountryMode(): String = prefs.getString("key_country_mode", "SAUDI") ?: "SAUDI"
    fun setCountryMode(mode: String) { prefs.edit().putString("key_country_mode", mode).apply() }

    // Saudi Apps state (Isolated)
    fun isSaudiJeenyEnabled(): Boolean = prefs.getBoolean("key_saudi_app_jeeny", true)
    fun setSaudiJeenyEnabled(v: Boolean) { prefs.edit().putBoolean("key_saudi_app_jeeny", v).apply() }

    fun isSaudiUberEnabled(): Boolean = prefs.getBoolean("key_saudi_app_uber", true)
    fun setSaudiUberEnabled(v: Boolean) { prefs.edit().putBoolean("key_saudi_app_uber", v).apply() }

    // Indonesia Apps state (Isolated)
    fun isIndoGrabEnabled(): Boolean = prefs.getBoolean("key_indo_app_grab", true)
    fun setIndoGrabEnabled(v: Boolean) { prefs.edit().putBoolean("key_indo_app_grab", v).apply() }

    fun isIndoUberEnabled(): Boolean = prefs.getBoolean("key_indo_app_uber", true)
    fun setIndoUberEnabled(v: Boolean) { prefs.edit().putBoolean("key_indo_app_uber", v).apply() }

    // Active App Checks based on Country
    fun isJeenyEnabled(): Boolean = getCountryMode() == "SAUDI" && isSaudiJeenyEnabled()
    fun isGrabEnabled(): Boolean = getCountryMode() == "INDONESIA" && isIndoGrabEnabled()
    fun isUberEnabled(): Boolean = if (getCountryMode() == "SAUDI") isSaudiUberEnabled() else isIndoUberEnabled()

    // Engine Speed: "NORMAL" or "ULTRA"
    fun getEngineSpeedMode(): String = prefs.getString("key_engine_speed", "ULTRA") ?: "ULTRA"
    fun setEngineSpeedMode(mode: String) { prefs.edit().putString("key_engine_speed", mode).apply() }

    // Engine Controls (from Screenshots)
    fun isRunAutoEngine(): Boolean = prefs.getBoolean("key_run_auto_engine", true)
    fun setRunAutoEngine(v: Boolean) { prefs.edit().putBoolean("key_run_auto_engine", v).apply() }

    fun isAutoRefresh(): Boolean = prefs.getBoolean("key_auto_refresh", true)
    fun setAutoRefresh(v: Boolean) { prefs.edit().putBoolean("key_auto_refresh", v).apply() }

    fun isAutoRejectEnabled(): Boolean = prefs.getBoolean("key_auto_reject", false)
    fun setAutoRejectEnabled(v: Boolean) { prefs.edit().putBoolean("key_auto_reject", v).apply() }

    fun isFilterDistanceEnabled(): Boolean = prefs.getBoolean("key_filter_distance_enabled", true)
    fun setFilterDistanceEnabled(v: Boolean) { prefs.edit().putBoolean("key_filter_distance_enabled", v).apply() }

    fun isSoundEnabled(): Boolean = prefs.getBoolean("key_sound_enabled", true)
    fun setSoundEnabled(v: Boolean) { prefs.edit().putBoolean("key_sound_enabled", v).apply() }

    fun isFloatingControlEnabled(): Boolean = prefs.getBoolean("key_floating_control_enabled", true)
    fun setFloatingControlEnabled(v: Boolean) { prefs.edit().putBoolean("key_floating_control_enabled", v).apply() }

    fun isVibrationEnabled(): Boolean = prefs.getBoolean("key_vibration_enabled", true)
    fun setVibrationEnabled(v: Boolean) { prefs.edit().putBoolean("key_vibration_enabled", v).apply() }

    // Master Bot Active
    fun isBotActive(): Boolean = prefs.getBoolean("key_bot_active", true)
    fun setBotActive(v: Boolean) { prefs.edit().putBoolean("key_bot_active", v).apply() }

    // Target Price Range
    fun getMinPrice(): Double = prefs.getFloat("key_min_price", 10.0f).toDouble()
    fun setMinPrice(v: Double) { prefs.edit().putFloat("key_min_price", v.toFloat()).apply() }

    fun getMaxPrice(): Double = prefs.getFloat("key_max_price", 300.0f).toDouble()
    fun setMaxPrice(v: Double) { prefs.edit().putFloat("key_max_price", v.toFloat()).apply() }

    // Pickup Limits (from Screenshot)
    fun getMaxPickupTimeMinutes(): Int = prefs.getInt("key_max_pickup_time", 12)
    fun setMaxPickupTimeMinutes(v: Int) { prefs.edit().putInt("key_max_pickup_time", v).apply() }

    fun getMaxDistanceKm(): Double = prefs.getFloat("key_max_distance", 20.0f).toDouble()
    fun setMaxDistanceKm(v: Double) { prefs.edit().putFloat("key_max_distance", v.toFloat()).apply() }

    fun getMinPricePerKm(): Double = prefs.getFloat("key_min_price_km", 0.0f).toDouble()
    fun setMinPricePerKm(v: Double) { prefs.edit().putFloat("key_min_price_km", v.toFloat()).apply() }

    fun getBotDelayMs(): Long = if (getEngineSpeedMode() == "ULTRA") 0L else prefs.getLong("key_bot_delay", 80L)
    fun setBotDelayMs(v: Long) { prefs.edit().putLong("key_bot_delay", v).apply() }

    // Blocked Locations (Avoid Words) from Screenshot
    fun getAvoidWords(): Set<String> = prefs.getStringSet("key_avoid_words", emptySet()) ?: emptySet()
    fun setAvoidWords(words: Set<String>) { prefs.edit().putStringSet("key_avoid_words", words).apply() }

    // Preferred Locations (Liked Words) from Screenshot
    fun getLikedWords(): Set<String> = prefs.getStringSet("key_liked_words", emptySet()) ?: emptySet()
    fun setLikedWords(words: Set<String>) { prefs.edit().putStringSet("key_liked_words", words).apply() }

    // Preset Embassies
    fun getSelectedLocations(): Set<String> = prefs.getStringSet("key_selected_locations", emptySet()) ?: emptySet()
    fun setSelectedLocations(locations: Set<String>) { prefs.edit().putStringSet("key_selected_locations", locations).apply() }

    // Anti-Sleep WakeLock
    fun isWakeLockEnabled(): Boolean = prefs.getBoolean("key_wakelock_enabled", true)
    fun setWakeLockEnabled(v: Boolean) { prefs.edit().putBoolean("key_wakelock_enabled", v).apply() }

    // Analytics
    fun getAcceptedCount(): Int = prefs.getInt("key_stat_accepted", 0)
    fun incrementAccepted() { prefs.edit().putInt("key_stat_accepted", getAcceptedCount() + 1).apply() }

    fun getScannedCount(): Int = prefs.getInt("key_stat_scanned", 0)
    fun incrementScanned() { prefs.edit().putInt("key_stat_scanned", getScannedCount() + 1).apply() }
}

data class OrderHistoryItem(
    val id: String,
    val timestamp: Long,
    val appSource: String, // "Jeeny", "Grab", "Uber"
    val price: Double,
    val distance: Double,
    val pickup: String,
    val destination: String,
    val status: String, // "ACCEPTED", "REJECTED"
    val reason: String = ""
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

object OrderHistoryManager {
    private const val PREF_NAME = "garuda_orders_history"
    private const val KEY_ORDERS = "orders_json"

    fun addOrder(context: Context, item: OrderHistoryItem) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ORDERS, "[]") ?: "[]"
        val array = JSONArray(raw)

        val obj = JSONObject().apply {
            put("id", item.id)
            put("timestamp", item.timestamp)
            put("appSource", item.appSource)
            put("price", item.price)
            put("distance", item.distance)
            put("pickup", item.pickup)
            put("destination", item.destination)
            put("status", item.status)
            put("reason", item.reason)
        }

        val newArray = JSONArray()
        newArray.put(obj)
        for (i in 0 until Math.min(array.length(), 99)) {
            newArray.put(array.getJSONObject(i))
        }

        prefs.edit().putString(KEY_ORDERS, newArray.toString()).apply()
    }

    fun getOrders(context: Context): List<OrderHistoryItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ORDERS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val list = mutableListOf<OrderHistoryItem>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                OrderHistoryItem(
                    id = obj.optString("id", ""),
                    timestamp = obj.optLong("timestamp", 0L),
                    appSource = obj.optString("appSource", "Jeeny"),
                    price = obj.optDouble("price", 0.0),
                    distance = obj.optDouble("distance", 0.0),
                    pickup = obj.optString("pickup", ""),
                    destination = obj.optString("destination", ""),
                    status = obj.optString("status", "ACCEPTED"),
                    reason = obj.optString("reason", "")
                )
            )
        }
        return list
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

class SoundPlayer(private val context: Context) {
    private var soundPool: SoundPool? = null
    private var soundAcceptId: Int = 0
    private var soundRejectId: Int = 0

    init {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build()

            val afdAccept = context.assets.openFd("sn_accept.mp3")
            soundAcceptId = soundPool?.load(afdAccept, 1) ?: 0

            val afdReject = context.assets.openFd("sn_rejected.mp3")
            soundRejectId = soundPool?.load(afdReject, 1) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playAccept() {
        if (soundAcceptId != 0) {
            soundPool?.play(soundAcceptId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun playReject() {
        if (soundRejectId != 0) {
            soundPool?.play(soundRejectId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}

object PresetLocationsData {
    val allLocations = listOf(
        "Diplomatic Quarter (حي السفارات)",
        "King Khalid International Airport (مطار الملك خالد)",
        "Riyadh Front (واجهة الرياض)",
        "Kingdom Tower (برج المملكة)",
        "King Abdullah Financial District (KAFD)",
        "Digital City (المدينة الرقمية)",
        "وزارة الخارجية",
        "Embassy of Afghanistan",
        "Embassy of Albania",
        "Embassy of Algeria",
        "Embassy of Angola",
        "Embassy of Argentina",
        "Embassy of Armenia",
        "Embassy of Australia",
        "Embassy of Austria",
        "Embassy of Azerbaijan",
        "Embassy of Bahrain",
        "Embassy of Bangladesh",
        "Embassy of Belarus",
        "Embassy of Belgium",
        "Embassy of Benin",
        "Embassy of Bosnia and Herzegovina",
        "Embassy of Brazil",
        "Embassy of Brunei",
        "Embassy of Bulgaria",
        "Embassy of Burkina Faso",
        "Embassy of Burundi",
        "Embassy of Cameroon",
        "Embassy of Canada",
        "Embassy of Central African Republic",
        "Embassy of Chad",
        "Embassy of China",
        "Embassy of Comoros",
        "Embassy of Czech Republic",
        "Embassy of Côte d’Ivoire",
        "Embassy of Democratic Republic of the Congo",
        "Embassy of Denmark",
        "Embassy of Djibouti",
        "Embassy of Egypt",
        "Embassy of Eritrea",
        "Embassy of Ethiopia",
        "Embassy of Finland",
        "Embassy of France",
        "Embassy of Gabon",
        "Embassy of Gambia",
        "Embassy of Georgia",
        "Embassy of Germany",
        "Embassy of Ghana",
        "Embassy of Greece",
        "Embassy of Guinea",
        "Embassy of Guinea-Bissau",
        "Embassy of Hungary",
        "Embassy of India",
        "Embassy of Indonesia",
        "Embassy of Iran",
        "Embassy of Iraq",
        "Embassy of Ireland",
        "Embassy of Italy",
        "Embassy of Japan",
        "Embassy of Jordan",
        "Embassy of Kazakhstan",
        "Embassy of Kenya",
        "Embassy of Kuwait",
        "Embassy of Kyrgyzstan",
        "Embassy of Laos",
        "Embassy of Lebanon",
        "Embassy of Libya",
        "Embassy of Madagascar",
        "Embassy of Malawi",
        "Embassy of Malaysia",
        "Embassy of Maldives",
        "Embassy of Mali",
        "Embassy of Mauritania",
        "Embassy of Mauritius",
        "Embassy of Mongolia",
        "Embassy of Morocco",
        "Embassy of Mozambique",
        "Embassy of Myanmar",
        "Embassy of Namibia",
        "Embassy of Nepal",
        "Embassy of Netherlands",
        "Embassy of New Zealand",
        "Embassy of Niger",
        "Embassy of Nigeria",
        "Embassy of Norway",
        "Embassy of Oman",
        "Embassy of Pakistan",
        "Embassy of Palestine",
        "Embassy of Philippines",
        "Embassy of Poland",
        "Embassy of Portugal",
        "Embassy of Qatar",
        "Embassy of Republic of the Congo",
        "Embassy of Romania",
        "Embassy of Russia",
        "Embassy of Rwanda",
        "Embassy of Saudi Arabia (MFA)",
        "Embassy of Senegal",
        "Embassy of Sierra Leone",
        "Embassy of Singapore",
        "Embassy of Somalia",
        "Embassy of South Africa",
        "Embassy of South Korea",
        "Embassy of South Sudan",
        "Embassy of Spain",
        "Embassy of Sri Lanka",
        "Embassy of Sudan",
        "Embassy of Sweden",
        "Embassy of Switzerland",
        "Embassy of Syria",
        "Embassy of Tanzania",
        "Embassy of Thailand",
        "Embassy of Togo",
        "Embassy of Tunisia",
        "Embassy of Türkiye",
        "Embassy of Uganda",
        "Embassy of Ukraine",
        "Embassy of United Arab Emirates",
        "Embassy of United Kingdom",
        "Embassy of United States of America",
        "Embassy of Uzbekistan",
        "Embassy of Venezuela",
        "Embassy of Vietnam",
        "Embassy of Yemen",
        "Embassy of Zambia",
        "Embassy of Zimbabwe"
    )

    val saudiLocations = allLocations

    val indonesiaLocations = listOf(
        "Embassy of Saudi Arabia (Jakarta)",
        "Embassy of Indonesia (Kemenlu)",
        "Soekarno-Hatta International Airport",
        "Halim Perdanakusuma Airport",
        "Gambir Station (Stasiun Gambir)",
        "Senayan City / Gelora Bung Karno",
        "Sudirman Central Business District (SCBD)",
        "Mega Kuningan Diplomatic Zone",
        "Grand Indonesia Mall",
        "Terminal Kampung Rambutan"
    )
}
