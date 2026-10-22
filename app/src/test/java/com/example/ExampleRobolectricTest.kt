package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Stickman Dismount", appName)
  }

  @Test
  fun `test GameRepository cash transactions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = GameRepository(context)

    val initialCash = repository.cash.value
    repository.addCash(500)
    assertEquals(initialCash + 500, repository.cash.value)

    val spendSuccess = repository.spendCash(200)
    assertTrue(spendSuccess)
    assertEquals(initialCash + 300, repository.cash.value)
  }
}
