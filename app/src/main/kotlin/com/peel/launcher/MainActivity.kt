package com.peel.launcher

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var launcher: AppLauncher
    private lateinit var swipeDetector: SwipeDownDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        launcher = AppLauncher(this)

        val grid = findViewById<RecyclerView>(R.id.tile_grid)
        grid.layoutManager = GridLayoutManager(this, 2)
        grid.adapter = AppTileAdapter(AppTile.defaultTiles()) { tile ->
            if (!launcher.launch(tile.packageName)) {
                Toast.makeText(
                    this,
                    "${tile.label} is not installed yet",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop * 4f
        swipeDetector = SwipeDownDetector(slopPx = touchSlop) {
            startActivity(Intent(this, ControlCenterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, 0)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        swipeDetector.onTouch(null, event)
        return super.dispatchTouchEvent(event)
    }
}
