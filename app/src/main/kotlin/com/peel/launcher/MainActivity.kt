package com.peel.launcher

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
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
        val gapPx = resources.getDimensionPixelSize(R.dimen.tile_gap)
        grid.addItemDecoration(GridSpacingDecoration(gapPx))

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

    private class GridSpacingDecoration(private val gapPx: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % 2
            outRect.left = if (column == 0) 0 else gapPx / 2
            outRect.right = if (column == 0) gapPx / 2 else 0
            outRect.top = if (position < 2) 0 else gapPx
        }
    }
}
