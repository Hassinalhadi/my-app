package com.example.jeenybot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast

class FloatingControlService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingButton: Button? = null
    private lateinit var prefs: BotPreferences

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = BotPreferences(this)
        startForegroundNotification()
        setupFloatingView()
    }

    private fun startForegroundNotification() {
        val channelId = "floating_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "${getString(R.string.bot_title)} Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("${getString(R.string.bot_title)} ACTIVE")
                .setContentText("الزر العائم نشط ويراقب Jeeny / Grab / Uber")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("${getString(R.string.bot_title)} ACTIVE")
                .setContentText("الزر العائم نشط")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
        startForeground(1001, notification)
    }

    private fun getButtonBackground(isActive: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 48f
            setColor(if (isActive) Color.parseColor("#E600C853") else Color.parseColor("#E6D50000"))
            setStroke(4, if (isActive) Color.parseColor("#00E5FF") else Color.parseColor("#FF1744"))
        }
    }

    private fun setupFloatingView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        floatingButton = Button(this).apply {
            text = if (prefs.isBotActive()) "${getString(R.string.bot_title)} ON 🟢" else "${getString(R.string.bot_title)} OFF 🔴"
            setTextColor(Color.WHITE)
            background = getButtonBackground(prefs.isBotActive())
            textSize = 12f
            setPadding(28, 14, 28, 14)
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 220
        }

        floatingButton?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(floatingButton, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            val newState = !prefs.isBotActive()
                            prefs.setBotActive(newState)
                            if (newState) {
                                floatingButton?.text = "${getString(R.string.bot_title)} ON 🟢"
                                floatingButton?.background = getButtonBackground(true)
                                Toast.makeText(this@FloatingControlService, "تم تفعيل القبول التلقائي", Toast.LENGTH_SHORT).show()
                            } else {
                                floatingButton?.text = "${getString(R.string.bot_title)} OFF 🔴"
                                floatingButton?.background = getButtonBackground(false)
                                Toast.makeText(this@FloatingControlService, "تم إيقاف القبول التلقائي", Toast.LENGTH_SHORT).show()
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(floatingButton, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingButton != null) {
            windowManager?.removeView(floatingButton)
        }
    }
}
