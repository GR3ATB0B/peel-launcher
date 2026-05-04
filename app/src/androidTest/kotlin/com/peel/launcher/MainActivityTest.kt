package com.peel.launcher

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun gridIsVisible() {
        onView(withId(R.id.tile_grid)).check(matches(isDisplayed()))
    }

    @Test
    fun gridShowsFourTiles() {
        rule.scenario.onActivity { activity ->
            val grid = activity.findViewById<RecyclerView>(R.id.tile_grid)
            assertEquals(4, grid.adapter?.itemCount)
        }
    }
}
