package com.example.edgeglow

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun addButton(root: LinearLayout, text: String, onClick: () -> Unit) {
        val b = Button(this)
        b.text = text
        b.setOnClickListener { onClick() }
        root.addView(b)
    }

    private fun addSeek(
        root: LinearLayout, label: String, key: String,
        min: Int, max: Int, def: Int
    ) {
        val prefs = getSharedPreferences("glow", MODE_PRIVATE)
        val tv = TextView(this)
        val sb = SeekBar(this)
        sb.max = max - min
        sb.progress = prefs.getInt(key, def) - min
        tv.text = label + ": " + prefs.getInt(key, def)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                tv.text = label + ": " + (p + min)
                prefs.edit().putInt(key, p + min).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        tv.setPadding(0, dp(16), 0, 0)
        root.addView(tv)
        root.addView(sb)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("glow", MODE_PRIVATE)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(40), dp(20), dp(20))

        val title = TextView(this)
        title.text = "EdgeGlow - 알림 엣지 라이팅"
        title.textSize = 22f
        root.addView(title)

        addButton(root, "1. 알림 접근 허용") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        addButton(root, "2. 다른 앱 위에 표시 허용") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
            )
        }
        addButton(root, "테스트") { Glow.show(this) }

        val colorLabel = TextView(this)
        colorLabel.text = "색상"
        colorLabel.setPadding(0, dp(16), 0, 0)
        root.addView(colorLabel)

        val row = LinearLayout(this)
        val colors = listOf("#00E5FF", "#FF4081", "#69F0AE", "#FFAB40", "#B388FF", "#FFFFFF")
        for (c in colors) {
            val b = Button(this)
            b.setBackgroundColor(Color.parseColor(c))
            b.setOnClickListener {
                prefs.edit().putInt("color", Color.parseColor(c)).apply()
                Glow.show(this)
            }
            row.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        root.addView(row)

        addSeek(root, "두께(dp)", "thick", 2, 16, 6)
        addSeek(root, "지속 시간(초)", "dur", 2, 8, 3)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }
}
