package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.ceil
import kotlin.math.floor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CampusTrack IERT", appName)
  }

  @Test
  fun `verify safe bunk and catch-up formulas`() {
    // 20 attended out of 24 conducted = 83.33% (>= 75%)
    val attended = 20
    val total = 24
    val bunksAvailable = floor((attended - (0.75 * total)) / 0.75).toInt()
    assertEquals(2, bunksAvailable)

    // 14 attended out of 21 conducted = 66.67% (< 75%)
    val lowAttended = 14
    val lowTotal = 21
    val classesNeeded = ceil(((0.75 * lowTotal) - lowAttended) / 0.25).toInt()
    assertEquals(7, classesNeeded)
  }

  @Test
  fun `verify json backup structure contains required keys`() {
    val json = org.json.JSONObject()
    json.put("app", "CampusTrack IERT")
    json.put("version", 2)
    val subjects = org.json.JSONArray()
    val sub = org.json.JSONObject()
    sub.put("code", "ME-301")
    sub.put("name", "Manufacturing Process")
    subjects.put(sub)
    json.put("subjects", subjects)

    assertEquals("CampusTrack IERT", json.getString("app"))
    assertEquals(2, json.getInt("version"))
    assertEquals("ME-301", json.getJSONArray("subjects").getJSONObject(0).getString("code"))
  }
}
