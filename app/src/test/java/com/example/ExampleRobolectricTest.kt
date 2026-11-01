package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.PresetRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GuitarFX Pro", appName)
  }

  @Test
  fun `verify factory presets`() {
    val presets = PresetRepository.factoryPresets
    assertEquals(5, presets.size)
    val first = presets[0]
    val json = PresetRepository.presetToJson(first)
    val restored = PresetRepository.jsonToPreset(json)
    assertNotNull(restored)
    assertEquals(first.name, restored?.name)
  }
}
