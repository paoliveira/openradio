package com.yuriy.openradio.shared.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yuriy.openradio.R
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageValidatorTest {

    @Test
    fun testClientPackages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val id = 1002
        val pkgNames = arrayOf("com.android.bluetooth", "com.android.systemui")
        val validator = PackageValidator(context, R.xml.allowed_media_browser_callers)

        for (pkg in pkgNames) {
            MatcherAssert.assertThat(
                    "'$pkg' did not pass validation",
                    validator.isKnownCaller(pkg, id, false), CoreMatchers.`is`(false)
            )
        }
    }
}