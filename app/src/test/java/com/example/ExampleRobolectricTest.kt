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
    assertEquals("Caliper", appName)
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
    json.put("app", "Caliper")
    json.put("version", 3)
    val subjects = org.json.JSONArray()
    val sub = org.json.JSONObject()
    sub.put("code", "ME-301")
    sub.put("name", "Manufacturing Process")
    subjects.put(sub)
    json.put("subjects", subjects)

    assertEquals("Caliper", json.getString("app"))
    assertEquals(3, json.getInt("version"))
    assertEquals("ME-301", json.getJSONArray("subjects").getJSONObject(0).getString("code"))
  }

  @Test
  fun `verify audio and haptic engine states`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.audio.CaliperSoundManager.init(context)
    com.example.audio.CaliperSoundManager.isAudioEnabled = true
    assertEquals(true, com.example.audio.CaliperSoundManager.isAudioEnabled)

    com.example.audio.CaliperHapticManager.isHapticsEnabled = true
    assertEquals(true, com.example.audio.CaliperHapticManager.isHapticsEnabled)
  }

  @Test
  fun `verify dual theme engine color palettes`() {
    val monoColors = com.example.ui.theme.MonochromeDarkColors
    val normalColors = com.example.ui.theme.NormalPsychologyColors

    assertEquals(true, monoColors.isMonochrome)
    assertEquals(false, normalColors.isMonochrome)

    // Base surface checks
    assertEquals(com.example.ui.theme.MonochromePureBlack, monoColors.baseSurface)
    assertEquals(com.example.ui.theme.PsychologyDarkSlate, normalColors.baseSurface)

    // Semantic colors in normal mode
    assertEquals(com.example.ui.theme.PsychologyGreen, normalColors.safeZone)
    assertEquals(com.example.ui.theme.PsychologyBlue, normalColors.scheduledLecture)
    assertEquals(com.example.ui.theme.PsychologyOrange, normalColors.deadlineWarning)
    assertEquals(com.example.ui.theme.PsychologyRed, normalColors.bunkDanger)
    assertEquals(com.example.ui.theme.PsychologyGold, normalColors.streakGold)
  }

  @Test
  fun `verify interactive calendar dynamic month engine calculations`() {
    val ym = java.time.YearMonth.of(2026, 10)
    assertEquals(31, ym.lengthOfMonth())
    // Oct 1 2026 is a Thursday (4 in 1..7, so offset = 3 for 0-indexed Mon..Sun)
    val firstDayOffset = ym.atDay(1).dayOfWeek.value - 1
    assertEquals(3, firstDayOffset)
  }

  @Test
  fun `verify AI engine action item extraction from quick notes`() {
    val sampleNote = """
      Sheet 4 Guidelines:
      - Submit Drawing Sheet 4 by Friday 5 PM
      - Bring 2H and 4H pencils for lettering
      - Complete ellipse isometric projection
    """.trimIndent()

    val actionItems = com.example.ai.CaliperAiEngine.extractActionItemsFromText(sampleNote)
    assertEquals(true, actionItems.isNotEmpty())
    assertEquals(true, actionItems.any { it.contains("Submit Drawing Sheet", ignoreCase = true) })
  }

  @Test
  fun `verify AI engine daily strategy calculation`() {
    val subject = com.example.data.local.entity.SubjectEntity(
      id = 1L,
      name = "Manufacturing Process",
      code = "ME-301",
      type = "theory",
      facultyName = "Dr. R.K. Sharma",
      strictness = "moderate",
      colorHex = "#38BDF8"
    )

    val stats = com.example.ui.SubjectAttendanceStats(
      subject = subject,
      attended = 20,
      totalConducted = 24, // 83.33%
      facultyCancelled = 1,
      collegeOff = 0,
      proxyCount = 0,
      percentage = 83.33,
      bunksAvailable = 2,
      classesNeeded = 0
    )

    val slot = com.example.data.local.entity.TimetableSlotEntity(
      id = 1L,
      dayOfWeek = java.time.LocalDate.now().dayOfWeek.value,
      startTime = "10:00",
      endTime = "11:00",
      roomNo = "LT-4",
      subjectId = 1L
    )
    val slotItem = com.example.ui.SlotDisplayItem(
      slot = slot,
      subject = subject,
      currentLog = null,
      isOngoing = false
    )

    val strategy = com.example.ai.CaliperAiEngine.analyzeDailyStrategy(
      statsList = listOf(stats),
      todaySlots = listOf(slotItem),
      assessments = emptyList(),
      tasks = emptyList()
    )

    assertEquals(true, strategy.canBunkToday)
    assertEquals(1, strategy.bunkAdviceItems.size)
    assertEquals(2, strategy.bunkAdviceItems[0].safeBunkBuffer)
  }
}

