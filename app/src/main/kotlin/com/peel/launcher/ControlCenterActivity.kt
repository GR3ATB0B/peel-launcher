package com.peel.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class ControlCenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_center)

        val scrim = findViewById<View>(R.id.control_scrim)
        val panel = findViewById<View>(R.id.control_panel)

        scrim.setOnClickListener { finishWithFade() }
        panel.setOnClickListener { /* consume */ }

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
                        v?.performClick()
                    }
                }
                return false
            }
        })

        val brightness = findViewById<SeekBar>(R.id.brightness_seek)
        brightness.progress = currentSystemBrightness()
        brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                if (!Settings.System.canWrite(this@ControlCenterActivity)) {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            .setData(Uri.parse("package:$packageName"))
                    )
                    return
                }
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    progress.coerceIn(0, 255),
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volume = findViewById<SeekBar>(R.id.volume_seek)
        volume.max = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
        volume.progress = audio.getStreamVolume(AudioManager.STREAM_RING)
        volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) audio.setStreamVolume(AudioManager.STREAM_RING, progress, 0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun currentSystemBrightness(): Int =
        try { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) }
        catch (e: Settings.SettingNotFoundException) { 128 }

    private fun finishWithFade() {
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }
}
