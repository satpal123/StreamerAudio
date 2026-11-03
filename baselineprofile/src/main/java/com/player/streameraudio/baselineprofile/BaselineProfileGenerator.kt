package com.player.streameraudio.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupAndPrimaryNavigation() = baselineProfileRule.collect(
        packageName = "com.player.streameraudio",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
        device.findObject(androidx.test.uiautomator.By.text("Browse"))?.click()
        device.waitForIdle()
        device.findObject(androidx.test.uiautomator.By.text("Live"))?.click()
        device.waitForIdle()
    }
}
