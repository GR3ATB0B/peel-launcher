package com.peel.launcher

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class AppTile(
    val label: String,
    val packageName: String,
    @ColorRes val colorRes: Int,
    @DrawableRes val iconRes: Int,
) {
    companion object {
        fun defaultTiles(): List<AppTile> = listOf(
            AppTile(
                label = "Phone",
                packageName = "com.simplemobiletools.dialer",
                colorRes = R.color.tile_phone,
                iconRes = R.drawable.ic_phone,
            ),
            AppTile(
                label = "Messages",
                packageName = "com.simplemobiletools.smsmessenger",
                colorRes = R.color.tile_messages,
                iconRes = R.drawable.ic_messages,
            ),
            AppTile(
                label = "Camera",
                packageName = "net.sourceforge.opencamera",
                colorRes = R.color.tile_camera,
                iconRes = R.drawable.ic_camera,
            ),
            AppTile(
                label = "Claude",
                packageName = "com.anthropic.claude",
                colorRes = R.color.tile_claude,
                iconRes = R.drawable.ic_claude,
            ),
        )
    }
}
