package com.peel.launcher

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class ControlCenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_center)

        val scrim = findViewById<View>(R.id.control_scrim)
        val panel = findViewById<View>(R.id.control_panel)

        // Tap on scrim (outside panel) -> dismiss
        scrim.setOnClickListener { finishWithFade() }
        // Block taps on the panel itself from bubbling to scrim
        panel.setOnClickListener { /* consume */ }

        // Swipe up anywhere -> dismiss
        val slop = ViewConfiguration.get(this).scaledTouchSlop * 4f
        scrim.setOnTouchListener(object : View.OnTouchListener {
            private var downY = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> downY = event.y
                    MotionEvent.ACTION_UP -> {
                        if (downY - event.y > slop && abs(downY - event.y) > slop) {
                            finishWithFade()
                            return true
                        }
                        // Treat as tap on scrim
                        v?.performClick()
                    }
                }
                return false
            }
        })
    }

    private fun finishWithFade() {
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }
}
