package com.peel.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeDownDetectorTest {

    @Test
    fun `triggers when fling exceeds threshold and is mostly downward`() {
        var calls = 0
        val detector = SwipeDownDetector(
            slopPx = 50f,
            onSwipeDown = { calls++ },
        )

        detector.onTouch(downX = 100f, downY = 50f, upX = 110f, upY = 800f)

        assertEquals(1, calls)
    }

    @Test
    fun `does not trigger when vertical delta is below threshold`() {
        var calls = 0
        val detector = SwipeDownDetector(
            slopPx = 50f,
            onSwipeDown = { calls++ },
        )

        detector.onTouch(downX = 100f, downY = 50f, upX = 110f, upY = 70f)

        assertEquals(0, calls)
    }

    @Test
    fun `does not trigger when motion is mostly horizontal`() {
        var calls = 0
        val detector = SwipeDownDetector(
            slopPx = 50f,
            onSwipeDown = { calls++ },
        )

        detector.onTouch(downX = 50f, downY = 50f, upX = 800f, upY = 200f)

        assertEquals(0, calls)
    }

    @Test
    fun `does not trigger when motion is upward`() {
        var calls = 0
        val detector = SwipeDownDetector(
            slopPx = 50f,
            onSwipeDown = { calls++ },
        )

        detector.onTouch(downX = 100f, downY = 800f, upX = 110f, upY = 50f)

        assertEquals(0, calls)
    }
}
