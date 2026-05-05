package com.peel.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class AppTileTest {

    @Test
    fun `AppTile carries package, color, icon, and label`() {
        val tile = AppTile(
            label = "Phone",
            packageName = "com.simplemobiletools.dialer",
            colorRes = android.R.color.holo_green_light,
            iconRes = android.R.drawable.sym_call_outgoing,
        )

        assertEquals("Phone", tile.label)
        assertEquals("com.simplemobiletools.dialer", tile.packageName)
        assertEquals(android.R.color.holo_green_light, tile.colorRes)
        assertEquals(android.R.drawable.sym_call_outgoing, tile.iconRes)
    }

    @Test
    fun `AppTile_defaultTiles returns the four core apps in grid order`() {
        val tiles = AppTile.defaultTiles()

        assertEquals(4, tiles.size)
        assertEquals("Phone",    tiles[0].label)
        assertEquals("Messages", tiles[1].label)
        assertEquals("Camera",   tiles[2].label)
        assertEquals("Claude",   tiles[3].label)
    }
}
