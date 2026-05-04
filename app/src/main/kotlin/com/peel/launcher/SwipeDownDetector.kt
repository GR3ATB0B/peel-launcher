package com.peel.launcher

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class SwipeDownDetector(
    private val slopPx: Float,
    private val onSwipeDown: () -> Unit,
) : View.OnTouchListener {

    private var downX: Float = 0f
    private var downY: Float = 0f

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_UP -> {
                onTouch(downX, downY, event.x, event.y)
            }
        }
        return false
    }

    /** Pure-Kotlin entry point used by unit tests and ACTION_UP. */
    fun onTouch(downX: Float, downY: Float, upX: Float, upY: Float) {
        val dx = upX - downX
        val dy = upY - downY
        val verticalDominant = abs(dy) > abs(dx)
        val downward = dy > slopPx
        if (verticalDominant && downward) {
            onSwipeDown()
        }
    }
}
