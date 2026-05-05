package com.peel.launcher

import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TilePressAnimationTest {

    @Test
    fun `tile press scales and dims, release returns to default`() {
        val tiles = AppTile.defaultTiles()
        val adapter = AppTileAdapter(tiles) { /* no-op */ }
        val app: android.app.Application = ApplicationProvider.getApplicationContext()
        app.setTheme(R.style.Theme_Peel)

        val holder = adapter.onCreateViewHolder(android.widget.FrameLayout(app), 0)
        adapter.onBindViewHolder(holder, 0)

        val view = holder.tile

        // Default
        assertEquals(1f, view.scaleX, 0.0001f)
        assertEquals(1f, view.alpha, 0.0001f)

        // ACTION_DOWN
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        view.dispatchTouchEvent(down)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue("scale should drop on press", view.scaleX < 1f)
        assertTrue("alpha should drop on press", view.alpha < 1f)
        down.recycle()

        // ACTION_UP
        val up = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)
        view.dispatchTouchEvent(up)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals("scale returns to 1", 1f, view.scaleX, 0.0001f)
        assertEquals("alpha returns to 1", 1f, view.alpha, 0.0001f)
        up.recycle()
    }
}
