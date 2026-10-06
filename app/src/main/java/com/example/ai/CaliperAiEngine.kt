package com.example.ai

import com.example.BuildConfig
import com.example.data.local.entity.AcademicTaskEntity
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.HolidayRangeEntity
import com.example.data.local.entity.QuickNoteEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.GlobalAttendanceSummary
import com.example.ui.SlotDisplayItem
import com.example.ui.SubjectAttendanceStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Caliper Academic AI Engine
 * Omniscient Context Assembler & Dual-Tier Intelligence Architecture:
 * - Deterministic Context Assembler: Builds an exhaustive system state JSON from Room DB.
 * - Context-Aware Query Processing: Natural language resolution for temporal schedules,
 *   exact subject bunk margins, skip eligibility, and academic deliverables.
 * - Generative Gemini Tier (gemini-2.5-flash) with full system state injection and instant offline fallback.
 */
object CaliperAiEngine {

    data class FullAcademicContext(
        val timetableByDay: Map<Int, List<SlotDisplayItem>>, // Day 1 (Mon) .. Day 6 (Sat)
        val subjectStatsList: List<SubjectAttendanceStats>,
        val assessments: List<AssessmentEntity>,
        val tasks: List<AcademicTaskEntity>,
        val holidays: List<HolidayRangeEntity>,
        val summary: GlobalAttendanceSummary,
        val notes: List<QuickNoteEntity> = emptyList()
    )

    data class DailyCollegeStrategy(
        val headline: String,
        val overallStatusBadge: String,
        val canBunkToday: Boolean,
        val bunkAdviceItems: List<BunkAdviceItem>,
        val urgentDeadlines: List<UrgentDeadlineItem>,
        val pendingTasksSummary: String,
        val actionPlanSteps: List<String>
    )

    data class BunkAdviceItem(
        val subjectCode: String,
        val subjectName: String,
        val currentPercentage: Double,
        val isSafe: Boolean,
        val safeBunkBuffer: Int,
        val catchUpNeeded: Int,
        val recommendation: String,
        val todaySlotTime: String? = null
    )

    data class UrgentDeadlineItem(
        val title: String,
        val type: String,
        val dueDate: String,
        val daysRemaining: Long,
        val isCritical: Boolean
    )

    // =========================================================================
    // 1. DETERMINISTIC CONTEXT ASSEMBLER
    // =========================================================================

    /**
     * Constructs an exhaustive, deterministic system state JSON representation
     * containing the complete 6-day timetable, subject attendance ledger,
     * academic deliverables, and holiday records.
     */
    fun assembleSystemStateJson(context: FullAcademicContext): JSONObject {
        val root = JSONObject()

        // 1. Full 6-Day Timetable Mapping (Mon - Sat)
        val timetableObj = JSONObject()
        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        for (dayIdx in 1..6) {
            val dayName = dayNames[dayIdx - 1]
            val slotsArray = JSONArray()
            val daySlots = context.timetableByDay[dayIdx]?.sortedBy { it.slot.startTime } ?: emptyList()
            for (item in daySlots) {
                val sObj = JSONObject().apply {
                    put("time", "${item.slot.startTime} - ${item.slot.endTime}")
                    put("room", item.slot.roomNo)
                    put("code", item.subject?.code ?: "GEN")
                    put("name", item.subject?.name ?: "Lecture")
                    put("faculty", item.subject?.facultyName ?: "Staff")
                    put("type", item.subject?.type ?: "theory")
                }
                slotsArray.put(sObj)
            }
            timetableObj.put(dayName, slotsArray)
        }
        root.put("timetable_schedule", timetableObj)

        // 2. Subject Attendance Ledger
        val ledgerArray = JSONArray()
        for (stat in context.subjectStatsList) {
            val sObj = JSONObject().apply {
                put("code", stat.subject.code)
                put("name", stat.subject.name)
                put("faculty", stat.subject.facultyName)
                put("attended", stat.attended)
                put("totalConducted", stat.totalConducted)
                put("bunked", stat.bunked)
                put("facultyCancelled", stat.facultyCancelled)
                put("percentage", String.format("%.2f", stat.percentage))
                put("safeBunkBuffer", stat.bunksAvailable)
                put("classesNeeded", stat.classesNeeded)
                put("isSafe", stat.percentage >= 75.0)
            }
            ledgerArray.put(sObj)
        }
        root.put("subject_attendance_ledger", ledgerArray)

        // 3. Academic Submission Tracker
        val deliverablesArray = JSONArray()
        val now = LocalDate.now()
        for (assessment in context.assessments) {
            val parsedDue = try {
                LocalDate.parse(assessment.dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                now
            }
            val daysDiff = ChronoUnit.DAYS.between(now, parsedDue)
            val dObj = JSONObject().apply {
                put("title", assessment.title)
                put("type", assessment.type) // Drawing Sheet, Workshop Job, Lab File, Sessional
                put("dueDate", assessment.dueDate)
                put("daysRemaining", daysDiff)
                put("status", assessment.status)
                put("maxMarks", assessment.maxMarks)
            }
            deliverablesArray.put(dObj)
        }
        root.put("academic_submissions_tracker", deliverablesArray)

        // 4. Institutional Holidays
        val holidaysArray = JSONArray()
        for (h in context.holidays) {
            val hObj = JSONObject().apply {
                put("title", h.title)
                put("startDate", h.startDate)
                put("endDate", h.endDate)
                put("type", h.type)
            }
            holidaysArray.put(hObj)
        }
        root.put("institutional_holidays", holidaysArray)

        // 5. Global Telemetry
        val globalObj = JSONObject().apply {
            put("overallPercentage", String.format("%.2f", context.summary.overallPercentage))
            put("totalAttended", context.summary.totalAttended)
            put("totalConducted", context.summary.totalConducted)
            put("atRiskCount", context.summary.atRiskSubjectCount)
            put("criticalCount", context.summary.criticalSubjectCount)
            put("statutoryCutoff", 75.0)
        }
        root.put("global_telemetry", globalObj)

        return root
    }

    // =========================================================================
    // 2. CONTEXT-AWARE QUERY PROCESSING (Deep Academic Local Intelligence)
    // =========================================================================

    /**
     * Resolves natural language inquiries with deterministic database awareness.
     * Can answer timetable queries, bunk margins, skip evaluations, and deadlines.
     */
    fun processDeterministicAcademicQuery(
        query: String,
        context: FullAcademicContext
    ): String {
        val q = query.trim().lowercase()
        val today = LocalDate.now()

        // -------------------------------------------------------------
        // A. TIMETABLE & SCHEDULE INQUIRIES
        // -------------------------------------------------------------
        if (q.contains("schedule") || q.contains("timetable") || q.contains("classes") || q.contains("routine") || q.contains("periods")) {
            // Check day target
            val targetDay: Int
            val targetLabel: String

            when {
                q.contains("tomorrow") -> {
                    val tom = today.plusDays(1)
                    targetDay = tom.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
                    targetLabel = "Tomorrow (${tom.format(DateTimeFormatter.ofPattern("EEEE, dd MMM"))})"
                }
                q.contains("today") -> {
                    targetDay = today.dayOfWeek.value
                    targetLabel = "Today (${today.format(DateTimeFormatter.ofPattern("EEEE, dd MMM"))})"
                }
                q.contains("monday") -> { targetDay = 1; targetLabel = "Monday" }
                q.contains("tuesday") -> { targetDay = 2; targetLabel = "Tuesday" }
                q.contains("wednesday") -> { targetDay = 3; targetLabel = "Wednesday" }
                q.contains("thursday") -> { targetDay = 4; targetLabel = "Thursday" }
                q.contains("friday") -> { targetDay = 5; targetLabel = "Friday" }
                q.contains("saturday") -> { targetDay = 6; targetLabel = "Saturday" }
                q.contains("sunday") -> { targetDay = 7; targetLabel = "Sunday" }
                else -> {
                    // Default to today if morning/afternoon, or tomorrow if evening
                    targetDay = today.dayOfWeek.value
                    targetLabel = "Today (${today.format(DateTimeFormatter.ofPattern("EEEE, dd MMM"))})"
                }
            }

            if (targetDay == 7) {
                return "📅 **$targetLabel Schedule**\n\n🎉 **Sunday / Institutional Off Day**.\nNo regular academic lecture slots scheduled. Use this day for drawing sheet drafting or workshop job journals."
            }

            val slots = context.timetableByDay[targetDay]?.sortedBy { it.slot.startTime } ?: emptyList()
            if (slots.isEmpty()) {
                return "📅 **$targetLabel Schedule**\n\nNo lecture periods registered in the database for this day."
            }

            return buildString {
                append("📅 **$targetLabel Schedule (${slots.size} Periods)**\n\n")
                slots.forEachIndexed { idx, item ->
                    val code = item.subject?.code ?: "GEN"
                    val name = item.subject?.name ?: "Lecture"
                    val room = item.slot.roomNo
                    val time = "${item.slot.startTime} – ${item.slot.endTime}"
                    val faculty = item.subject?.facultyName ?: "Faculty"
                    append("${idx + 1}. **$time** • `$code`\n")
                    append("   📖 $name\n")
                    append("   📍 Room: **$room** | 👨‍🏫 $faculty\n\n")
                }
                append("💡 *Check room assignments on arrival at IERT LT blocks.*")
            }
        }

        // -------------------------------------------------------------
        // B. SPECIFIC SUBJECT BUNK INQUIRIES & BUFFER CALCULATIONS
        // -------------------------------------------------------------
        // Check if query targets a specific subject
        val matchedSubject = findSubjectInQuery(q, context.subjectStatsList)

        if (matchedSubject != null) {
            val stats = matchedSubject
            val subName = stats.subject.name
            val subCode = stats.subject.code
            val attended = stats.attended
            val conducted = stats.totalConducted
            val pct = stats.percentage
            val bu = stats.bunksAvailable
            val needed = stats.classesNeeded

            // Query asking "Can I skip / miss / bunk?"
            if (q.contains("skip") || q.contains("miss") || q.contains("can i bunk") || q.contains("should i attend")) {
                val newConducted = conducted + 1
                val newPct = if (newConducted > 0) (attended.toDouble() / newConducted) * 100.0 else 100.0
                val wouldStaySafe = newPct >= 75.0

                return buildString {
                    if (wouldStaySafe) {
                        append("🟢 **YES, YOU CAN SAFELY SKIP $subCode ($subName)**\n\n")
                        append("• **Current Attendance:** $attended / $conducted (${String.format("%.1f", pct)}%)\n")
                        append("• **If you skip 1 lecture today:** Attendance will become $attended / $newConducted (**${String.format("%.1f", newPct)}%**)\n")
                        append("• **Buffer Margin:** You will still have **${max(0, bu - 1)}** safe bunks remaining above the 75% cutoff.\n\n")
                        append("✅ Safe Zone intact. No debar risk.")
                    } else {
                        append("🔴 **NO! DO NOT SKIP $subCode ($subName)**\n\n")
                        append("• **Current Attendance:** $attended / $conducted (${String.format("%.1f", pct)}%)\n")
                        append("• **If you skip 1 lecture today:** Attendance will drop to $attended / $newConducted (**${String.format("%.1f", newPct)}%**)\n")
                        append("• **Debar Hazard:** This is **BELOW** the statutory 75% cutoff.\n\n")
                        append("⚠️ **Mandatory Action:** Attend this lecture! You currently need to attend **$needed** consecutive class(es) to maintain eligibility.")
                    }
                }
            }

            // Query asking "How many bunks in Drawing / etc."
            return buildString {
                append("📊 **$subCode: $subName Attendance Analysis**\n\n")
                append("• **Attended / Conducted:** $attended / $conducted\n")
                append("• **Current Percentage:** ${String.format("%.1f", pct)}%\n")
                append("• **Statutory Cutoff:** 75.0%\n\n")
                if (pct >= 75.0) {
                    append("🛡️ **Statutory Bunk Buffer:** **$bu class(es)**\n")
                    append("You can safely miss up to **$bu** lecture(s) without dropping below 75%.\n")
                    append("📐 *Formula:* `floor(($attended - 0.75 * $conducted) / 0.75) = $bu`")
                } else {
                    append("🚨 **BELOW 75% CUTOFF (Debar Risk!)**\n")
                    append("You cannot bunk any classes! You must attend the next **$needed** lecture(s) without missing to restore 75% attendance.\n")
                    append("📐 *Formula:* `ceil((0.75 * $conducted - $attended) / 0.25) = $needed`")
                }
            }
        }

        // -------------------------------------------------------------
        // C. GENERAL BUNK / ATTENDANCE INQUIRIES
        // -------------------------------------------------------------
        if (q.contains("bunk") || q.contains("attend") || q.contains("safe zone") || q.contains("percentage") || q.contains("status")) {
            val strategy = analyzeDailyStrategy(
                context.subjectStatsList,
                context.timetableByDay[today.dayOfWeek.value] ?: emptyList(),
                context.assessments,
                context.tasks
            )

            return buildString {
                append("🎯 **Caliper Tactical Bunk Intelligence**\n\n")
                append("• **Overall Academic Aggregate:** ${String.format("%.1f", context.summary.overallPercentage)}%\n")
                append("• **Total Conducted:** ${context.summary.totalConducted} | **Attended:** ${context.summary.totalAttended}\n\n")

                if (strategy.bunkAdviceItems.isNotEmpty()) {
                    append("**Today's Scheduled Subject Bunk Safety:**\n")
                    strategy.bunkAdviceItems.forEach { item ->
                        val icon = if (item.isSafe) "🟢" else "🔴"
                        append("$icon **${item.subjectCode}** (${String.format("%.1f", item.currentPercentage)}%): ${item.recommendation}\n")
                    }
                } else {
                    append("No periods scheduled for today.\n\n")
                    append("**Global Subject Buffer Tally:**\n")
                    context.subjectStatsList.forEach { s ->
                        val icon = if (s.percentage >= 75.0) "🟢" else "🔴"
                        append("$icon `${s.subject.code}`: ${String.format("%.1f", s.percentage)}% (Buffer: ${s.bunksAvailable} bunks)\n")
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // D. SUBMISSIONS, ASSIGNMENTS, DRAWING SHEETS & LAB FILES
        // -------------------------------------------------------------
        if (q.contains("sheet") || q.contains("drawing") || q.contains("job") || q.contains("workshop") || q.contains("lab") || q.contains("assignment") || q.contains("submission") || q.contains("deadline") || q.contains("due") || q.contains("task")) {
            val pendingAssessments = context.assessments.filter { it.status != "submitted" }
            val pendingTasks = context.tasks.filter { !it.isCompleted }

            return buildString {
                append("📋 **Academic Deliverables & Submissions Tracker**\n\n")

                if (pendingAssessments.isNotEmpty()) {
                    append("**Pending Major Deliverables:**\n")
                    pendingAssessments.forEach { a ->
                        val parsed = try { LocalDate.parse(a.dueDate, DateTimeFormatter.ISO_LOCAL_DATE) } catch (_: Exception) { today }
                        val diff = ChronoUnit.DAYS.between(today, parsed)
                        val badge = when {
                            diff < 0 -> "⚠️ OVERDUE"
                            diff <= 2 -> "🔥 URGENT (${diff}d left)"
                            else -> "⏳ ${diff} days left"
                        }
                        append("• **${a.title}** [`${a.type}`] — $badge (Due: ${a.dueDate})\n")
                    }
                } else {
                    append("✅ All drawing sheets, workshop jobs, and lab files are fully submitted!\n")
                }

                if (pendingTasks.isNotEmpty()) {
                    append("\n**Actionable Academic To-Dos:**\n")
                    pendingTasks.take(4).forEach { t ->
                        append("• [${t.priority.uppercase()}] ${t.description} (Due: ${t.dueDate})\n")
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // E. HOLIDAYS & VACATIONS
        // -------------------------------------------------------------
        if (q.contains("holiday") || q.contains("vacation") || q.contains("break") || q.contains("diwali") || q.contains("off")) {
            if (context.holidays.isNotEmpty()) {
                return buildString {
                    append("🏖️ **Institutional Holidays & Declared Breaks**\n\n")
                    context.holidays.forEach { h ->
                        append("• **${h.title}**: ${h.startDate} to ${h.endDate} (${h.type.replace('_', ' ')})\n")
                    }
                    append("\n💡 *Attendance penalties are automatically suppressed during official break windows.*")
                }
            } else {
                return "🏖️ No official institutional break ranges currently declared in the academic calendar."
            }
        }

        // -------------------------------------------------------------
        // F. GENERAL BRIEFING
        // -------------------------------------------------------------
        val strategy = analyzeDailyStrategy(
            context.subjectStatsList,
            context.timetableByDay[today.dayOfWeek.value] ?: emptyList(),
            context.assessments,
            context.tasks
        )

        return buildString {
            append("⚙️ **Caliper Executive Academic Briefing**\n\n")
            append("• **Student Status:** ${strategy.headline}\n")
            append("• **Statutory Posture:** ${strategy.overallStatusBadge}\n")
            append("• **Deliverables:** ${strategy.pendingTasksSummary}\n\n")
            append("💡 *Try asking:*\n")
            append("→ *\"What is my schedule tomorrow?\"*\n")
            append("→ *\"How many bunks in Drawing?\"*\n")
            append("→ *\"Can I skip Thermal today?\"*\n")
            append("→ *\"What drawing sheets are due?\"*")
        }
    }

    private fun findSubjectInQuery(query: String, statsList: List<SubjectAttendanceStats>): SubjectAttendanceStats? {
        val q = query.lowercase()
        return statsList.firstOrNull { stat ->
            val code = stat.subject.code.lowercase()
            val name = stat.subject.name.lowercase()

            when {
                q.contains(code) -> true
                q.contains(name) -> true
                (code.contains("ed") || name.contains("drawing")) && (q.contains("drawing") || q.contains("ed-301") || q.contains("machine drawing") || q.contains("m/c drawing")) -> true
                (code.contains("302") || name.contains("thermal")) && (q.contains("thermal") || q.contains("me-302")) -> true
                (code.contains("301") && name.contains("manufacturing")) && (q.contains("manufacturing") || q.contains("process") || q.contains("me-301")) -> true
                (code.contains("303") || name.contains("workshop")) && (q.contains("workshop") || q.contains("me-303")) -> true
                (code.contains("cs") || name.contains("computer")) && (q.contains("computer") || q.contains("cs-301")) -> true
                (code.contains("304") || name.contains("material")) && (q.contains("material") || q.contains("me-304")) -> true
                (code.contains("305") || name.contains("lab")) && (q.contains("lab") || q.contains("me-305l")) -> true
                else -> false
            }
        }
    }

    // =========================================================================
    // 3. GENERATIVE GEMINI INTEGRATION WITH DETERMINISTIC JSON CONTEXT
    // =========================================================================

    /**
     * Dual-Tier Query Handler:
     * - Injects the full JSON system state into Gemini 2.5 / 3.5.
     * - Falls back instantly and deterministically to the local intelligence engine on any error.
     */
    suspend fun queryOmniscientAssistant(
        prompt: String,
        context: FullAcademicContext
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        // If no API key is provided, execute deterministic local intelligence immediately
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext processDeterministicAcademicQuery(prompt, context)
        }

        try {
            val modelName = "gemini-2.5-flash"
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val fullStateJson = assembleSystemStateJson(context)

            val systemInstruction = """
                You are Caliper AI, the omniscient academic copilot and systems engineer for IERT Prayagraj engineering students (Mechanical & Tool Engg).
                You have 100% full awareness of the student's database state.
                Below is the exhaustive, deterministic system state JSON:
                ${fullStateJson.toString(2)}
                
                Rules:
                1. Statutory attendance rule is strictly 75% minimum.
                2. Bunk formula: Bunks available = floor((Attended - 0.75 * Conducted) / 0.75).
                3. Classes needed if below 75%: ceil((0.75 * Conducted - Attended) / 0.25).
                4. Answer concisely, pragmatically, and with exact times, room numbers, and subject codes.
                5. Do NOT hallucinate lectures not in the timetable.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", "$systemInstruction\n\nStudent Question: $prompt"))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 7000
                readTimeout = 10000
                doOutput = true
                doInput = true
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val reply = parts.getJSONObject(0).optString("text")
                        if (reply.isNotBlank()) return@withContext reply.trim()
                    }
                }
            }
            // Fallback to deterministic local engine
            processDeterministicAcademicQuery(prompt, context)
        } catch (_: Exception) {
            // Instant zero-latency fallback
            processDeterministicAcademicQuery(prompt, context)
        }
    }

    // =========================================================================
    // 4. STRATEGY ANALYSIS & UTILITIES
    // =========================================================================

    fun analyzeDailyStrategy(
        statsList: List<SubjectAttendanceStats>,
        todaySlots: List<SlotDisplayItem>,
        assessments: List<AssessmentEntity>,
        tasks: List<AcademicTaskEntity>
    ): DailyCollegeStrategy {
        val today = LocalDate.now()
        val bunkAdvices = mutableListOf<BunkAdviceItem>()
        var canBunkAnyToday = false

        if (todaySlots.isNotEmpty()) {
            todaySlots.forEach { slotItem ->
                val subject = slotItem.subject
                val stats = statsList.find { it.subject.id == subject?.id }
                if (subject != null && stats != null) {
                    val isSafe = stats.percentage >= 75.0 && stats.bunksAvailable > 0
                    if (isSafe) canBunkAnyToday = true

                    val rec = if (isSafe) {
                        "Safe to miss. You have ${stats.bunksAvailable} buffer lecture(s) above 75% cutoff."
                    } else if (stats.percentage < 75.0) {
                        "MUST ATTEND! Attendance is ${String.format("%.1f", stats.percentage)}%. Attend next ${stats.classesNeeded} class(es) to exit Debar zone."
                    } else {
                        "Borderline at ${String.format("%.1f", stats.percentage)}%. Missing today will drop you below 75%."
                    }

                    val timeStr = "${slotItem.slot.startTime} - ${slotItem.slot.endTime} (${slotItem.slot.roomNo})"

                    bunkAdvices.add(
                        BunkAdviceItem(
                            subjectCode = subject.code,
                            subjectName = subject.name,
                            currentPercentage = stats.percentage,
                            isSafe = isSafe,
                            safeBunkBuffer = stats.bunksAvailable,
                            catchUpNeeded = stats.classesNeeded,
                            recommendation = rec,
                            todaySlotTime = timeStr
                        )
                    )
                }
            }
        }

        // Urgent deadlines
        val urgentDeadlines = mutableListOf<UrgentDeadlineItem>()
        assessments.filter { it.status != "submitted" }.forEach { a ->
            try {
                val due = LocalDate.parse(a.dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
                val diff = ChronoUnit.DAYS.between(today, due)
                if (diff in 0..5) {
                    urgentDeadlines.add(
                        UrgentDeadlineItem(
                            title = a.title,
                            type = a.type,
                            dueDate = a.dueDate,
                            daysRemaining = diff,
                            isCritical = diff <= 2
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        val pendingTasksCount = tasks.count { !it.isCompleted }
        val pendingTasksSummary = when {
            urgentDeadlines.isNotEmpty() -> "${urgentDeadlines.size} critical academic submission(s) due within 5 days!"
            pendingTasksCount > 0 -> "$pendingTasksCount to-do task(s) active on your roster."
            else -> "All assignments and tasks are up to date."
        }

        val overallCriticalCount = statsList.count { it.percentage < 75.0 }
        val overallBadge = when {
            overallCriticalCount >= 2 -> "DEBAR RISK: $overallCriticalCount SUBJECTS UNDER 75%"
            overallCriticalCount == 1 -> "WARNING: 1 SUBJECT UNDER 75%"
            else -> "STATUTORY SAFE ZONE (ALL >= 75%)"
        }

        val headline = if (todaySlots.isEmpty()) {
            "No classes scheduled today. Review pending submissions."
        } else if (canBunkAnyToday) {
            "Tactical Safe Buffer Available for selected classes."
        } else {
            "High Attendance Priority: Mandatory classes scheduled."
        }

        val actionSteps = mutableListOf<String>()
        if (urgentDeadlines.isNotEmpty()) {
            actionSteps.add("Finish and sign: ${urgentDeadlines.first().title}")
        }
        if (overallCriticalCount > 0) {
            val criticalNames = statsList.filter { it.percentage < 75.0 }.joinToString { it.subject.code }
            actionSteps.add("Attend all periods for $criticalNames to avoid exam hall bar.")
        }

        return DailyCollegeStrategy(
            headline = headline,
            overallStatusBadge = overallBadge,
            canBunkToday = canBunkAnyToday,
            bunkAdviceItems = bunkAdvices,
            urgentDeadlines = urgentDeadlines,
            pendingTasksSummary = pendingTasksSummary,
            actionPlanSteps = actionSteps
        )
    }

    /**
     * Backward-compatible alias for older queryAssistant calls
     */
    suspend fun queryAssistant(
        prompt: String,
        statsList: List<SubjectAttendanceStats>,
        slots: List<SlotDisplayItem>,
        assessments: List<AssessmentEntity>,
        tasks: List<AcademicTaskEntity>,
        notes: List<QuickNoteEntity>
    ): String {
        val today = LocalDate.now().dayOfWeek.value
        val context = FullAcademicContext(
            timetableByDay = mapOf(today to slots),
            subjectStatsList = statsList,
            assessments = assessments,
            tasks = tasks,
            holidays = emptyList(),
            summary = GlobalAttendanceSummary(
                totalAttended = statsList.sumOf { it.attended },
                totalConducted = statsList.sumOf { it.totalConducted },
                totalFacultyCancelled = statsList.sumOf { it.facultyCancelled },
                totalCollegeOff = statsList.sumOf { it.collegeOff },
                totalProxy = statsList.sumOf { it.proxyCount },
                overallPercentage = if (statsList.sumOf { it.totalConducted } > 0) {
                    (statsList.sumOf { it.attended }.toDouble() / statsList.sumOf { it.totalConducted }) * 100.0
                } else 100.0,
                atRiskSubjectCount = statsList.count { it.percentage < 75.0 && it.percentage >= 65.0 },
                criticalSubjectCount = statsList.count { it.percentage < 65.0 }
            ),
            notes = notes
        )
        return queryOmniscientAssistant(prompt, context)
    }

    fun extractActionItemsFromText(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val actionKeywords = listOf("submit", "draw", "bring", "complete", "finish", "viva", "assignment", "sheet", "write", "buy", "lab", "print", "todo", "task")
        val results = mutableListOf<String>()

        for (line in lines) {
            val clean = line.removePrefix("-").removePrefix("*").removePrefix("•").trim()
            if (clean.length in 5..120) {
                val hasKeyword = actionKeywords.any { clean.contains(it, ignoreCase = true) }
                if (hasKeyword || clean.startsWith("1.") || clean.startsWith("2.") || clean.startsWith("3.")) {
                    results.add(clean.removePrefix("1.").removePrefix("2.").removePrefix("3.").trim())
                }
            }
        }
        if (results.isEmpty() && lines.isNotEmpty()) {
            results.add(lines.first().take(80))
        }
        return results.distinct().take(5)
    }

    fun summarizeNote(title: String, content: String): String {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size <= 2) return content

        val points = lines.take(3).map { "• ${it.trim().removePrefix("-").removePrefix("*").trim()}" }
        return "📌 Key Takeaways for \"$title\":\n" + points.joinToString("\n")
    }
}
