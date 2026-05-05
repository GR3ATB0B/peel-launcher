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
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class ControlCenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_center)

        val scrim = findViewById<View>(R.id.control_scrim)
        val panel = findViewById<View>(R.id.control_panel)

        animatePanelIn(scrim, panel)

        scrim.setOnClickListener { dismiss(scrim, panel) }
        panel.setOnClickListener { /* consume */ }

        val slop = ViewConfiguration.get(this).scaledTouchSlop * 4f
        scrim.setOnTouchListener(object : View.OnTouchListener {
            private var downY = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> downY = event.y
                    MotionEvent.ACTION_UP -> {
                        if (downY - event.y > slop && abs(downY - event.y) > slop) {
                            dismiss(scrim, panel)
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

        val silentRow = findViewById<View>(R.id.silent_toggle)
        val silentState = findViewById<TextView>(R.id.silent_toggle_state)
        fun refreshSilent() {
            silentState.setText(
                if (audio.ringerMode == AudioManager.RINGER_MODE_SILENT) R.string.silent_mode_on
                else R.string.silent_mode_off
            )
        }
        refreshSilent()
        silentRow.setOnClickListener {
            try {
                audio.ringerMode = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL)
                    AudioManager.RINGER_MODE_SILENT
                else
                    AudioManager.RINGER_MODE_NORMAL
                refreshSilent()
            } catch (e: SecurityException) {
                startActivity(Intent("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS"))
            }
        }
        attachChipPress(silentRow)

        val wifi = findViewById<View>(R.id.wifi_btn)
        wifi.setOnClickListener { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        attachChipPress(wifi)

        val bt = findViewById<View>(R.id.bluetooth_btn)
        bt.setOnClickListener { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        attachChipPress(bt)

        val settings = findViewById<View>(R.id.settings_btn)
        settings.setOnClickListener { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        attachChipPress(settings)
    }

    private fun animatePanelIn(scrim: View, panel: View) {
        scrim.alpha = 0f
        panel.post {
            panel.translationY = -panel.height.toFloat()
            panel.animate()
                .translationY(0f)
                .setDuration(220L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            scrim.animate()
                .alpha(1f)
                .setDuration(220L)
                .start()
        }
    }

    private fun dismiss(scrim: View, panel: View) {
        panel.animate()
            .translationY(-panel.height.toFloat())
            .setDuration(180L)
            .setInterpolator(AccelerateInterpolator())
            .start()
        scrim.animate()
            .alpha(0f)
            .setDuration(180L)
            .withEndAction { finish(); overridePendingTransition(0, 0) }
            .start()
    }

    private fun attachChipPress(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.85f).setDuration(120L).start()
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(180L).start()
            }
            false
        }
    }

    private fun currentSystemBrightness(): Int =
        try { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) }
        catch (e: Settings.SettingNotFoundException) { 128 }
}
