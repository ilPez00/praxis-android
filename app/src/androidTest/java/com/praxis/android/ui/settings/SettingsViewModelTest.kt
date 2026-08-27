package com.praxis.android.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private lateinit var context: Context
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = SettingsViewModel(context)
    }

    @Test
    fun defaultSettings_areReasonable() {
        val settings = viewModel.settings.value
        assertTrue(settings.notificationsEnabled)
        assertTrue(settings.messagesEnabled)
        assertTrue(settings.matchesEnabled)
        assertFalse(settings.biometricEnabled)
    }
}
