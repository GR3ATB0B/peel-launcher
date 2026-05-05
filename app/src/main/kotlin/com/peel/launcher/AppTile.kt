package com.peel.launcher

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class AppTile(
    val label: String,
    val packageName: String,
    @ColorRes val colorRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val backgroundRes: Int,
) {
    companion object {
        fun defaultTiles(): List<AppTile> = listOf(
            AppTile(
                label = "Phone",
                packageName = "com.simplemobiletools.dialer",
                colorRes = R.color.tile_phone,
                iconRes = R.drawable.ic_phone,
                backgroundRes = R.drawable.tile_phone_bg,
            ),
            AppTile(
                label = "Messages",
                packageName = "com.simplemobiletools.smsmessenger",
                colorRes = R.color.tile_messages,
                iconRes = R.drawable.ic_messages,
                backgroundRes = R.drawable.tile_messages_bg,
            ),
            AppTile(
                label = "Camera",
                packageName = "net.sourceforge.opencamera",
                colorRes = R.color.tile_camera,
                iconRes = R.drawable.ic_camera,
                backgroundRes = R.drawable.tile_camera_bg,
            ),
            AppTile(
                label = "Claude",
                packageName = "com.anthropic.claude",
                colorRes = R.color.tile_claude,
                iconRes = R.drawable.ic_claude,
                backgroundRes = R.drawable.tile_claude_bg,
            ),
        )
    }
}
