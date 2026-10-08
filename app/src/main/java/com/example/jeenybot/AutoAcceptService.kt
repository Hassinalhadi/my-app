package com.example.jeenybot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

class AutoAcceptService : AccessibilityService() {

    companion object {
        var instance: AutoAcceptService? = null

        // Pre-compiled fast regex patterns
        private val REGEX_PRICE = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)")
        private val REGEX_DIST = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*(?:كم|km|KM|Km)")
        private val REGEX_TIME = Pattern.compile("([0-9]+)\\s*(?:دقيقة|دقائق|min|mins|menit)")
    }

    private lateinit var prefs: BotPreferences
    private var soundPlayer: SoundPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastHandledTime = 0L
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefs = BotPreferences(this)
        soundPlayer = SoundPlayer(this)
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null && prefs.isWakeLockEnabled()) {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GarudaBot:FastCaptureLock")?.apply {
                acquire(24 * 60 * 60 * 1000L) // 24 hours
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!prefs.isBotActive() || !prefs.isRunAutoEngine() || event == null) return

        val pkg = event.packageName?.toString() ?: ""

        val appName = when (pkg) {
            "me.com.easytaxista" -> if (prefs.isJeenyEnabled()) "Jeeny" else null
            "com.grabtaxi.driver2" -> if (prefs.isGrabEnabled()) "Grab" else null
            "com.ubercab.driver" -> if (prefs.isUberEnabled()) "Uber" else null
            else -> null
        } ?: return

        val now = System.currentTimeMillis()
        if (now - lastHandledTime < 180) return

        val root = rootInActiveWindow ?: return
        processRideOffer(root, appName)
    }

    private fun processRideOffer(root: AccessibilityNodeInfo, appName: String) {
        prefs.incrementScanned()

        // كلمات القبول المخصصة لكل تطبيق ودولة
        val acceptWords = when (appName) {
            "Jeeny" -> listOf("قبول", "Accept", "تأكيد", "ACCEPT", "موافق", "استلام", "قبول الطلب", "تأكيد الطلب")
            "Uber" -> listOf("قبول", "اضغط للقبول", "Tap to accept", "Accept", "MATCH", "Match", "ACCEPT", "تأكيد")
            "Grab" -> listOf("Terima", "TERIMA", "Grab", "Ambil", "AMBIL", "ACCEPT", "Accept", "قبول")
            else -> listOf("قبول", "Accept", "Terima", "MATCH")
        }

        var acceptNode: AccessibilityNodeInfo? = null
        for (word in acceptWords) {
            val list = root.findAccessibilityNodeInfosByText(word)
            if (list.isNotEmpty()) {
                acceptNode = list[0]
                break
            }
        }

        if (acceptNode == null) return

        // قراءة نصوص الشاشة لاستخراج تفاصيل الطلب
        val allTexts = extractAllText(root)
        val extractedPrice: Double? = extractPrice(allTexts, prefs.getCountryMode())
        val extractedDistance: Double? = extractDistance(allTexts)
        val extractedPickupTime: Int? = extractPickupTime(allTexts)

        val minPrice: Double = prefs.getMinPrice()
        val maxPrice: Double = prefs.getMaxPrice()
        val maxDistance: Double = prefs.getMaxDistanceKm()
        val minRateKm: Double = prefs.getMinPricePerKm()
        val maxPickupLimit: Int = prefs.getMaxPickupTimeMinutes()

        var shouldAccept = true
        var rejectReason = ""

        // 1. فحص الكلمات والأماكن المحظورة (Blocked Locations / Avoid Words)
        val avoidWords = prefs.getAvoidWords()
        if (avoidWords.isNotEmpty()) {
            for (avoid in avoidWords) {
                if (avoid.isNotBlank() && allTexts.contains(avoid.trim(), ignoreCase = true)) {
                    shouldAccept = false
                    rejectReason = "منطقة محظورة ($avoid)"
                    break
                }
            }
        }

        // 2. فحص أقصى وقت للوصول (Pickup Limits)
        if (shouldAccept && extractedPickupTime != null && extractedPickupTime > maxPickupLimit) {
            shouldAccept = false
            rejectReason = "وقت الوصول ($extractedPickupTime د) يتجاوز الحد الأقصى ($maxPickupLimit د)"
        }

        // 3. فحص نطاق السعر
        if (shouldAccept && extractedPrice != null) {
            val p: Double = extractedPrice
            if (p < minPrice) {
                shouldAccept = false
                rejectReason = "السعر ($p) أقل من الحد الأدنى ($minPrice)"
            } else if (p > maxPrice) {
                shouldAccept = false
                rejectReason = "السعر ($p) أعلى من الحد الأقصى ($maxPrice)"
            }
        }

        // 4. فحص المسافة (إذا كانت مفعلة)
        if (shouldAccept && prefs.isFilterDistanceEnabled() && extractedDistance != null) {
            val d: Double = extractedDistance
            if (d > maxDistance) {
                shouldAccept = false
                rejectReason = "المسافة ($d كم) أكبر من الحد المسموح ($maxDistance كم)"
            }

            // فحص سعر الكيلو متر الواحد (ربحية المشوار)
            if (shouldAccept && minRateKm > 0.0 && extractedPrice != null && d > 0.3) {
                val rate = extractedPrice / d
                if (rate < minRateKm) {
                    shouldAccept = false
                    rejectReason = "سعر الكيلو (%.1f) أقل من المطلوب (%.1f)".format(rate, minRateKm)
                }
            }
        }

        // 5. فحص المواقع المفضلة (Preferred Locations / Liked Words)
        val likedWords = prefs.getLikedWords()
        if (shouldAccept && likedWords.isNotEmpty()) {
            val matched = likedWords.any { liked ->
                liked.isNotBlank() && allTexts.contains(liked.trim(), ignoreCase = true)
            }
            if (!matched) {
                shouldAccept = false
                rejectReason = "الطلب ليس ضمن الأماكن المفضلة المحددة"
            }
        }

        // 6. فحص السفارات المحددة
        val selectedLocations = prefs.getSelectedLocations()
        if (shouldAccept && selectedLocations.isNotEmpty()) {
            val matched = selectedLocations.any { loc ->
                val cleanLoc = loc.replace("Embassy of ", "").replace("سفارة ", "").trim()
                allTexts.contains(loc, ignoreCase = true) ||
                allTexts.contains(cleanLoc, ignoreCase = true) ||
                (loc.contains("(") && allTexts.contains(loc.substringAfter("(").substringBefore(")").trim(), ignoreCase = true))
            }
            if (!matched) {
                shouldAccept = false
                rejectReason = "الموقع لا يطابق السفارات المحددة"
            }
        }

        lastHandledTime = System.currentTimeMillis()

        if (shouldAccept) {
            val isUltra = prefs.getEngineSpeedMode() == "ULTRA"
            val delay = if (isUltra) 0L else prefs.getBotDelayMs()
            if (delay == 0L) {
                executeAccept(acceptNode, appName, extractedPrice, extractedDistance)
            } else {
                handler.postDelayed({
                    executeAccept(acceptNode, appName, extractedPrice, extractedDistance)
                }, delay)
            }
        } else if (prefs.isAutoRejectEnabled()) {
            val rejectWords = listOf("رفض", "Reject", "REJECT", "Tolak", "TOLAK", "Abaikan", "تجاهل", "Skip", "تخطي")
            for (word in rejectWords) {
                val rList = root.findAccessibilityNodeInfosByText(word)
                if (rList.isNotEmpty()) {
                    clickNode(rList[0])
                    triggerRejectFeedback()
                    OrderHistoryManager.addOrder(
                        this,
                        OrderHistoryItem(
                            id = System.currentTimeMillis().toString(),
                            timestamp = System.currentTimeMillis(),
                            appSource = appName,
                            price = extractedPrice ?: 0.0,
                            distance = extractedDistance ?: 0.0,
                            pickup = "$appName تم رفضه تلقائياً",
                            destination = "غير مطابق للفلاتر",
                            status = "REJECTED",
                            reason = rejectReason
                        )
                    )
                    break
                }
            }
        }
    }

    private fun executeAccept(node: AccessibilityNodeInfo, appName: String, price: Double?, dist: Double?) {
        if (clickNode(node)) {
            triggerAcceptFeedback()
            prefs.incrementAccepted()
            OrderHistoryManager.addOrder(
                this,
                OrderHistoryItem(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    appSource = appName,
                    price = price ?: 0.0,
                    distance = dist ?: 0.0,
                    pickup = "$appName طلب مقبول ⚡",
                    destination = "مطابق للشروط وفلاتر الربحية",
                    status = "ACCEPTED"
                )
            )
        }
    }

    private fun extractAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        val text = node.text
        if (!text.isNullOrEmpty()) sb.append(text).append(" ")
        val desc = node.contentDescription
        if (!desc.isNullOrEmpty()) sb.append(desc).append(" ")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            sb.append(extractAllText(child))
        }
        return sb.toString()
    }

    private fun extractPrice(text: String, country: String): Double? {
        val matcher = REGEX_PRICE.matcher(text)
        while (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", ".")
            val num = raw?.toDoubleOrNull()
            if (num != null) {
                if (country == "SAUDI" && num in 4.0..2500.0) return num
                if (country == "INDONESIA" && num in 3000.0..8000000.0) return num
            }
        }
        return null
    }

    private fun extractDistance(text: String): Double? {
        val matcher = REGEX_DIST.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", ".")
            return raw?.toDoubleOrNull()
        }
        return null
    }

    private fun extractPickupTime(text: String): Int? {
        val matcher = REGEX_TIME.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull()
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        // 1. Direct Node Click
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true

        // 2. Parent Container Click (up to 4 levels)
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 4) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            parent = parent.parent
            depth++
        }

        // 3. Ultra-fast Gesture Click (Precision Touch Center)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() > 0 && bounds.height() > 0) {
            val clickPath = Path().apply {
                moveTo(bounds.exactCenterX(), bounds.exactCenterY())
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(clickPath, 0, 15))
                .build()
            return dispatchGesture(gesture, null, null)
        }
        return false
    }

    private fun triggerAcceptFeedback() {
        if (prefs.isSoundEnabled()) soundPlayer?.playAccept()
        if (prefs.isVibrationEnabled()) vibrate(140)
    }

    private fun triggerRejectFeedback() {
        if (prefs.isSoundEnabled()) soundPlayer?.playReject()
        if (prefs.isVibrationEnabled()) vibrate(70)
    }

    private fun vibrate(ms: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(ms)
        }
    }

    override fun onInterrupt() { instance = null }
    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        soundPlayer?.release()
        instance = null
    }
}
