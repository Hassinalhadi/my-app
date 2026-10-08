package com.example.jeenybot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Color Palette matching Original Screenshots
val BgColor = Color(0xFFF1F5F9)
val CardBg = Color(0xFFFFFFFF)
val CardBorder = Color(0xFFE2E8F0)
val InputBg = Color(0xFFF1F5F9)
val PrimaryBlue = Color(0xFF2563EB)
val AccentRed = Color(0xFFDC2626)
val AccentGreen = Color(0xFF16A34A)
val TextDark = Color(0xFF0F172A)
val TextMuted = Color(0xFF64748B)

class MainActivity : ComponentActivity() {

    private lateinit var prefs: BotPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = BotPreferences(this)

        setContent {
            OriginalBotScreen(
                prefs = prefs,
                onToggleBot = { newState ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, if (prefs.getLanguage() == "ar") "يرجى تفعيل إذن النافذة العائمة أولاً" else "Please enable floating overlay permission first", Toast.LENGTH_SHORT).show()
                    } else {
                        prefs.setBotActive(newState)
                        val serviceIntent = Intent(this, FloatingControlService::class.java)
                        if (newState) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(serviceIntent)
                            } else {
                                startService(serviceIntent)
                            }
                        }
                    }
                },
                onOpenAccessibility = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    val msg = if (prefs.getLanguage() == "ar") "يرجى تفعيل خدمة الوصول للبوت" else "Please activate Bot Accessibility Service"
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                },
                onOpenOverlay = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    } else {
                        val msg = if (prefs.getLanguage() == "ar") "إذن النافذة العائمة مفعل بالفعل" else "Floating overlay already enabled"
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenBattery = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        } catch (e: Exception) {
                            val msg = if (prefs.getLanguage() == "ar") "عطل تحسين البطارية للبوت للعمل دون توقف" else "Disable battery optimization for uninterrupted work"
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onOpenHistory = {
                    startActivity(Intent(this, OrderHistoryActivity::class.java))
                },
                onOpenTelegram = {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/h200_x")))
                    } catch (e: Exception) {
                        Toast.makeText(this, "Telegram: @h200_x", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginalBotScreen(
    prefs: BotPreferences,
    onToggleBot: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTelegram: () -> Unit
) {
    val context = LocalContext.current
    var lang by remember { mutableStateOf(prefs.getLanguage()) }
    val isAr = lang == "ar"

    var botActive by remember { mutableStateOf(prefs.isBotActive()) }
    var engineSpeed by remember { mutableStateOf(prefs.getEngineSpeedMode()) } // "NORMAL" or "ULTRA"

    // Country and Isolated Toggles
    var countryMode by remember { mutableStateOf(prefs.getCountryMode()) }
    var saudiJeeny by remember { mutableStateOf(prefs.isSaudiJeenyEnabled()) }
    var saudiUber by remember { mutableStateOf(prefs.isSaudiUberEnabled()) }
    var indoGrab by remember { mutableStateOf(prefs.isIndoGrabEnabled()) }
    var indoUber by remember { mutableStateOf(prefs.isIndoUberEnabled()) }

    // Automation switches (from Screenshot 1)
    var runAutoEngine by remember { mutableStateOf(prefs.isRunAutoEngine()) }
    var autoRefresh by remember { mutableStateOf(prefs.isAutoRefresh()) }
    var autoReject by remember { mutableStateOf(prefs.isAutoRejectEnabled()) }
    var filterDistance by remember { mutableStateOf(prefs.isFilterDistanceEnabled()) }
    var soundNotification by remember { mutableStateOf(prefs.isSoundEnabled()) }
    var floatingControl by remember { mutableStateOf(prefs.isFloatingControlEnabled()) }

    // Price and Limits (from Screenshot 2)
    var minPrice by remember { mutableStateOf(prefs.getMinPrice().toString()) }
    var maxPrice by remember { mutableStateOf(prefs.getMaxPrice().toString()) }
    var maxPickupTime by remember { mutableStateOf(prefs.getMaxPickupTimeMinutes().toString()) }

    // Blocked and Preferred locations
    var avoidInput by remember { mutableStateOf("") }
    var avoidWords by remember { mutableStateOf(prefs.getAvoidWords()) }

    var likedInput by remember { mutableStateOf("") }
    var likedWords by remember { mutableStateOf(prefs.getLikedWords()) }

    // Full embassies dialog
    var selectedLocations by remember { mutableStateOf(prefs.getSelectedLocations()) }
    var showLocationsDialog by remember { mutableStateOf(false) }

    fun checkAccessibility(): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabledServices.contains(context.packageName) || AutoAcceptService.instance != null
    }

    fun checkOverlay(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
    }

    val accessibilityOk = remember { checkAccessibility() }
    val overlayOk = remember { checkOverlay() }

    fun saveAll() {
        prefs.setLanguage(lang)
        prefs.setCountryMode(countryMode)
        prefs.setSaudiJeenyEnabled(saudiJeeny)
        prefs.setSaudiUberEnabled(saudiUber)
        prefs.setIndoGrabEnabled(indoGrab)
        prefs.setIndoUberEnabled(indoUber)
        prefs.setEngineSpeedMode(engineSpeed)

        prefs.setRunAutoEngine(runAutoEngine)
        prefs.setAutoRefresh(autoRefresh)
        prefs.setAutoRejectEnabled(autoReject)
        prefs.setFilterDistanceEnabled(filterDistance)
        prefs.setSoundEnabled(soundNotification)
        prefs.setFloatingControlEnabled(floatingControl)

        prefs.setMinPrice(minPrice.toDoubleOrNull() ?: 10.0)
        prefs.setMaxPrice(maxPrice.toDoubleOrNull() ?: 300.0)
        prefs.setMaxPickupTimeMinutes(maxPickupTime.toIntOrNull() ?: 12)

        prefs.setAvoidWords(avoidWords)
        prefs.setLikedWords(likedWords)
        prefs.setSelectedLocations(selectedLocations)

        val toastMsg = if (isAr) "تم حفظ جميع الإعدادات والفلاتر بنجاح 💾" else "All settings and filters saved successfully 💾"
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            Surface(
                color = CardBg,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.bot_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Dev: @h200_x • v1.10 PRO",
                                fontSize = 11.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onOpenTelegram() }
                            )
                        }
                    }

                    // Language Toggle
                    Surface(
                        color = InputBg,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.clickable {
                            val nextLang = if (isAr) "en" else "ar"
                            lang = nextLang
                            prefs.setLanguage(nextLang)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAr) "🇸🇦 العربية" else "🇺🇸 English",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = CardBg,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { saveAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isAr) "حفظ التغييرات" else "Save Settings", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenHistory,
                        border = BorderStroke(1.dp, PrimaryBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isAr) "سجل العمليات" else "Order Logs")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // CARD 1: Bot Engine Speed (EXACTLY AS SCREENSHOT 1)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "سرعة محرك البوت" else "Bot Engine Speed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isUltra = engineSpeed == "ULTRA"

                            // NORMAL Button
                            Surface(
                                color = if (!isUltra) Color(0xFFE2E8F0) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (!isUltra) PrimaryBlue else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { engineSpeed = "NORMAL" }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (isAr) "عادي" else "NORMAL",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (!isUltra) TextDark else TextMuted
                                    )
                                }
                            }

                            // ULTRA Button (Bright Red Pill as in Screenshot)
                            Surface(
                                color = if (isUltra) AccentRed else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isUltra) AccentRed else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { engineSpeed = "ULTRA" }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (isUltra) Color.White else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isAr) "ألترا" else "ULTRA",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (isUltra) Color.White else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // CARD 2: Automation Controls (EXACTLY AS SCREENSHOT 1)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 1. Run Auto Engine
                        SwitchRowItem(
                            icon = Icons.Default.PlayCircle,
                            iconTint = PrimaryBlue,
                            title = if (isAr) "تشغيل المحرك التلقائي" else "Run Auto Engine",
                            checked = runAutoEngine,
                            onCheckedChange = {
                                runAutoEngine = it
                                botActive = it
                                onToggleBot(it)
                            }
                        )

                        Divider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // 2. Auto Refresh
                        SwitchRowItem(
                            icon = Icons.Default.Refresh,
                            iconTint = PrimaryBlue,
                            title = if (isAr) "تحديث الشاشة تلقائياً" else "Auto Refresh",
                            checked = autoRefresh,
                            onCheckedChange = { autoRefresh = it }
                        )

                        Divider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // 3. Auto Reject
                        SwitchRowItem(
                            icon = Icons.Default.Cancel,
                            iconTint = AccentRed,
                            title = if (isAr) "الرفض التلقائي للطلبات" else "Auto Reject",
                            checked = autoReject,
                            onCheckedChange = { autoReject = it }
                        )

                        Divider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // 4. Filter Distance
                        SwitchRowItem(
                            icon = Icons.Default.Place,
                            iconTint = PrimaryBlue,
                            title = if (isAr) "تصفية المسافة" else "Filter Distance",
                            checked = filterDistance,
                            onCheckedChange = { filterDistance = it }
                        )

                        Divider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // 5. Sound Notification
                        SwitchRowItem(
                            icon = Icons.Default.Notifications,
                            iconTint = Color(0xFFF59E0B),
                            title = if (isAr) "التنبيهات الصوتية" else "Sound Notification",
                            checked = soundNotification,
                            onCheckedChange = { soundNotification = it }
                        )

                        Divider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // 6. Floating Control
                        SwitchRowItem(
                            icon = Icons.Default.PictureInPicture,
                            iconTint = PrimaryBlue,
                            title = if (isAr) "الزر العائم فوق التطبيقات" else "Floating Control",
                            checked = floatingControl,
                            onCheckedChange = { floatingControl = it }
                        )
                    }
                }
            }

            // CARD 3: Target Price Range & Pickup Limits (EXACTLY AS SCREENSHOT 2)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "نطاق السعر المستهدف" else "Target Price Range",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Min Price Box with green down arrow
                            Surface(
                                color = InputBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(4.dp))
                                    TextField(
                                        value = minPrice,
                                        onValueChange = { minPrice = it },
                                        placeholder = { Text(if (isAr) "أدنى سعر" else "Min Price", fontSize = 12.sp, color = TextMuted) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Max Price Box with red up arrow
                            Surface(
                                color = InputBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(4.dp))
                                    TextField(
                                        value = maxPrice,
                                        onValueChange = { maxPrice = it },
                                        placeholder = { Text(if (isAr) "أعلى سعر" else "Max Price", fontSize = 12.sp, color = TextMuted) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Pickup Limits Section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "حدود وقت الاستلام" else "Pickup Limits",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = InputBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = maxPickupTime,
                                onValueChange = { maxPickupTime = it },
                                placeholder = { Text(if (isAr) "أقصى وقت استلام (دقائق)" else "Max Pickup Time (mins)", fontSize = 13.sp, color = TextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // CARD 4: Blocked & Preferred Locations (EXACTLY AS SCREENSHOT 2)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Blocked Locations Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isAr) "المواقع المحظورة للتجنب" else "Blocked Locations",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                            }
                            Icon(Icons.Default.List, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }

                        Spacer(Modifier.height(10.dp))

                        // Avoid Words Input Field with + Button
                        Surface(
                            color = InputBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = avoidInput,
                                    onValueChange = { avoidInput = it },
                                    placeholder = { Text(if (isAr) "اكتب كلمة للتجنب (مثال: البطحاء)" else "Avoid Words", fontSize = 13.sp, color = TextMuted) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (avoidInput.isNotBlank()) {
                                            avoidWords = avoidWords + avoidInput.trim()
                                            avoidInput = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = PrimaryBlue)
                                }
                            }
                        }

                        // Avoid Words Chips
                        if (avoidWords.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(avoidWords.toList()) { word ->
                                    Surface(
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(word, color = AccentRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = AccentRed,
                                                modifier = Modifier.size(14.dp).clickable { avoidWords = avoidWords - word }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Preferred Locations Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "المواقع المفضلة للاستهداف" else "Preferred Locations",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Liked Words Input Field with + Button
                        Surface(
                            color = InputBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = likedInput,
                                    onValueChange = { likedInput = it },
                                    placeholder = { Text(if (isAr) "اكتب موقعاً مفضلاً (مثال: حي السفارات)" else "Liked Words", fontSize = 13.sp, color = TextMuted) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (likedInput.isNotBlank()) {
                                            likedWords = likedWords + likedInput.trim()
                                            likedInput = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = AccentGreen)
                                }
                            }
                        }

                        // Liked Words Chips
                        if (likedWords.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(likedWords.toList()) { word ->
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(word, color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = AccentGreen,
                                                modifier = Modifier.size(14.dp).clickable { likedWords = likedWords - word }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CARD 5: Target Apps & Country Mode (Fixing State Overwrite Bug!)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "تطبيقات التوصيل والدولة:" else "Target Apps & Country:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isSaudi = countryMode == "SAUDI"

                            Surface(
                                color = if (isSaudi) PrimaryBlue.copy(alpha = 0.1f) else InputBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSaudi) PrimaryBlue else CardBorder),
                                modifier = Modifier.weight(1f).clickable {
                                    countryMode = "SAUDI"
                                    minPrice = "10"
                                    maxPrice = "300"
                                }
                            ) {
                                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                    Text("🇸🇦 ${if (isAr) "السعودية (SAR)" else "Saudi Arabia"}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                }
                            }

                            Surface(
                                color = if (!isSaudi) PrimaryBlue.copy(alpha = 0.1f) else InputBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (!isSaudi) PrimaryBlue else CardBorder),
                                modifier = Modifier.weight(1f).clickable {
                                    countryMode = "INDONESIA"
                                    minPrice = "15000"
                                    maxPrice = "500000"
                                }
                            ) {
                                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                    Text("🇮🇩 ${if (isAr) "إندونيسيا (IDR)" else "Indonesia"}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // App switches preserved independently!
                        if (countryMode == "SAUDI") {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("Jeeny Driver (جيني درايفر)", color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Switch(checked = saudiJeeny, onCheckedChange = { saudiJeeny = it })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("Uber Driver (أوبر درايفر)", color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Switch(checked = saudiUber, onCheckedChange = { saudiUber = it })
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("Grab Driver (غراب درايفر)", color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Switch(checked = indoGrab, onCheckedChange = { indoGrab = it })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("Uber Driver (أوبر درايفر)", color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Switch(checked = indoUber, onCheckedChange = { indoUber = it })
                            }
                        }
                    }
                }
            }

            // CARD 6: Preset Embassies Button (133 Embassies)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth().clickable { showLocationsDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = PrimaryBlue)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isAr) "قائمة السفارات والمواقع الدبلوماسية (130+)" else "Embassies & Diplomatic Missions",
                                color = TextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                if (isAr) "تم اختيار ${selectedLocations.size} سفارة للاستهداف الحصري" else "${selectedLocations.size} target locations selected",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                }
            }

            // CARD 7: Developer Rights & Telegram Support (@h200_x)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenTelegram() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) "حقوق التطوير والمطور" else "Developer & Rights",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Telegram: @h200_x",
                                fontSize = 13.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(14.dp)) }
        }
    }

    // Full Locations Dialog with Search
    if (showLocationsDialog) {
        val allList = if (countryMode == "SAUDI") PresetLocationsData.saudiLocations else PresetLocationsData.indonesiaLocations
        var searchQuery by remember { mutableStateOf("") }
        val filteredList = remember(searchQuery) {
            if (searchQuery.isBlank()) allList else allList.filter { it.contains(searchQuery, ignoreCase = true) }
        }

        Dialog(onDismissRequest = { showLocationsDialog = false }) {
            Surface(
                color = CardBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) "📍 قائمة السفارات (${selectedLocations.size}/${allList.size})" else "📍 Select Embassies (${selectedLocations.size}/${allList.size})",
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (isAr) "بحث عن سفارة..." else "Search embassy...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { selectedLocations = allList.toSet() }) {
                            Text(if (isAr) "تحديد الكل" else "Select All", color = PrimaryBlue)
                        }
                        TextButton(onClick = { selectedLocations = emptySet() }) {
                            Text(if (isAr) "إلغاء التحديد" else "Clear All", color = AccentRed)
                        }
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredList) { location ->
                            val isChecked = selectedLocations.contains(location)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLocations = if (isChecked) selectedLocations - location else selectedLocations + location
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedLocations = if (checked) selectedLocations + location else selectedLocations - location
                                    }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(location, color = TextDark, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showLocationsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (isAr) "حفظ واختيار" else "Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = TextDark, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1)
            )
        )
    }
}
