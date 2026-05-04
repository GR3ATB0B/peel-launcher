package com.peel.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLauncherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val launcher = AppLauncher(context)

    @Test
    fun `launch returns false when package is not installed`() {
        val launched = launcher.launch("com.does.not.exist")

        assertFalse(launched)
    }

    @Test
    fun `launch returns true and starts an Intent when package is installed`() {
        // Robolectric ships with the host package installed; reuse it
        val installedPkg = context.packageName

        val launched = launcher.launch(installedPkg)

        assertTrue(launched)
        val started = shadowOf(context as android.app.Application).nextStartedActivity
        assertNotNull(started)
        assertTrue(
            "Started intent should resolve to $installedPkg",
            started.`package` == installedPkg || started.component?.packageName == installedPkg
        )
    }
}
