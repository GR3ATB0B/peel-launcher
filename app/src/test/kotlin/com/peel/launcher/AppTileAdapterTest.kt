package com.peel.launcher

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppTileAdapterTest {

    @Test
    fun `adapter exposes one item per tile`() {
        val tiles = AppTile.defaultTiles()
        val adapter = AppTileAdapter(tiles) { /* no-op */ }

        assertEquals(4, adapter.itemCount)
    }

    @Test
    fun `clicking a tile invokes the onTileClick callback with that tile`() {
        val tiles = AppTile.defaultTiles()
        var clicked: AppTile? = null
        val adapter = AppTileAdapter(tiles) { clicked = it }

        // Bind position 2 (Camera)
        val app: android.app.Application = ApplicationProvider.getApplicationContext()
        app.setTheme(R.style.Theme_Peel)
        val holder = adapter.onCreateViewHolder(
            android.widget.FrameLayout(app),
            0,
        )
        adapter.onBindViewHolder(holder, 2)
        holder.itemView.performClick()

        assertEquals(tiles[2], clicked)
    }
}
