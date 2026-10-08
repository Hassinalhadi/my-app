package com.example.jeenybot

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OrderHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#050508"))
            setPadding(28, 36, 28, 36)
        }

        val title = TextView(this).apply {
            text = "📋 سجل العمليات (Order Logs)"
            textSize = 20f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        root.addView(title)

        val clearBtn = Button(this).apply {
            text = "مسح السجل"
            setBackgroundColor(Color.parseColor("#1A1A22"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                OrderHistoryManager.clear(this@OrderHistoryActivity)
                recreate()
            }
        }
        root.addView(clearBtn)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }

        val orders = OrderHistoryManager.getOrders(this)
        if (orders.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "لا توجد طلبات مسجلة بعد"
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 0)
            }
            listContainer.addView(emptyTv)
        } else {
            for (order in orders) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 16, 20, 16)
                    val bg = if (order.status == "ACCEPTED") Color.parseColor("#004D40") else Color.parseColor("#4A148C")
                    setBackgroundColor(bg)
                    val lp = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 12) }
                    layoutParams = lp
                }

                val statusLine = TextView(this).apply {
                    text = "[${order.appSource}] ${order.status} • ${order.formattedTime}"
                    setTextColor(Color.WHITE)
                    textSize = 14f
                }
                card.addView(statusLine)

                val details = TextView(this).apply {
                    val priceStr = if (order.price > 0.0) "${order.price}" else "غير محدد"
                    val distStr = if (order.distance > 0.0) "${order.distance} كم" else "غير محدد"
                    text = "السعر: $priceStr | المسافة: $distStr"
                    setTextColor(Color.parseColor("#ECEFF1"))
                    textSize = 12f
                }
                card.addView(details)

                if (order.reason.isNotEmpty()) {
                    val reasonTv = TextView(this).apply {
                        text = "السبب: ${order.reason}"
                        setTextColor(Color.parseColor("#FF8A80"))
                        textSize = 11f
                    }
                    card.addView(reasonTv)
                }

                listContainer.addView(card)
            }
        }

        scroll.addView(listContainer)
        root.addView(scroll)
        setContentView(root)
    }
}
