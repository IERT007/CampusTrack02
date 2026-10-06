package com.example.ai

import com.example.BuildConfig
import com.example.data.local.entity.AcademicTaskEntity
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.QuickNoteEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Caliper Academic AI Engine
 * Dual-Tier Architecture:
 * 1. Tier 1: Instant Local Intelligence (Deterministic, 0ms latency, 100% offline, zero lag/glitch)
 * 2. Tier 2: Cloud Gemini AI (gemini-3.5-flash) for generative conversational assistance with automatic local fallback
 */
object CaliperAiEngine {

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

    /**
     * Synthesizes student's complete academic state into an actionable daily briefing.
     * Computes instantly without blocking main thread.
     */
    fun analyzeDailyStrategy(
        statsList: List<SubjectAttendanceStats>,
        todaySlots: List<SlotDisplayItem>,
        assessments: List<AssessmentEntity>,
        tasks: List<AcademicTaskEntity>
    ): DailyCollegeStrategy {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // 1. Analyze Bunk Safety for each subject scheduled today
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
        } else {
            // General subject-level analysis if no classes today
            statsList.take(4).forEach { stats ->
                val isSafe = stats.percentage >= 75.0 && stats.bunksAvailable > 0
                bunkAdvices.add(
                    BunkAdviceItem(
                        subjectCode = stats.subject.code,
                        subjectName = stats.subject.name,
                        currentPercentage = stats.percentage,
                        isSafe = isSafe,
                        safeBunkBuffer = stats.bunksAvailable,
                        catchUpNeeded = stats.classesNeeded,
                        recommendation = if (isSafe) "Buffer: ${stats.bunksAvailable} lectures" else "Needs ${stats.classesNeeded} classes"
                    )
                )
            }
        }

        // 2. Urgent Deadlines Watchdog (Sheets, assignments, submissions within next 3 days)
        val urgentDeadlines = mutableListOf<UrgentDeadlineItem>()
        assessments.filter { it.status != "submitted" && it.status != "appeared" }.forEach { a ->
            try {
                val due = LocalDate.parse(a.dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
                val diffDays = java.time.temporal.ChronoUnit.DAYS.between(today, due)
                if (diffDays in -1..4) {
                    urgentDeadlines.add(
                        UrgentDeadlineItem(
                            title = a.title,
                            type = a.type.replace("_", " ").replaceFirstChar { it.uppercase() },
                            dueDate = a.dueDate,
                            daysRemaining = diffDays,
                            isCritical = diffDays <= 1
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // 3. Pending tasks summary
        val pendingTasks = tasks.filter { !it.isCompleted }
        val highPriorityCount = pendingTasks.count { it.priority.equals("High", ignoreCase = true) }
        val pendingTasksText = if (pendingTasks.isEmpty()) {
            "All ${tasks.size} academic tasks cleared! No backlog."
        } else {
            "${pendingTasks.size} pending tasks ($highPriorityCount high priority)."
        }

        // 4. Headline & Strategy Action Plan
        val criticalSubjects = statsList.count { it.percentage < 75.0 }
        val headline = when {
            criticalSubjects > 0 -> "⚠️ Attendance Alert: $criticalSubjects subject(s) below statutory 75%"
            urgentDeadlines.any { it.isCritical } -> "⚡ Immediate Action Required: ${urgentDeadlines.first().title} due soon"
            canBunkAnyToday -> "🛡️ Safe Zone Maintained: Bunk buffers available today"
            else -> "📘 Focus Day: Standard schedule & clean academic record"
        }

        val badge = when {
            criticalSubjects > 0 -> "CRITICAL DEBAR RISK"
            urgentDeadlines.isNotEmpty() -> "DEADLINES PENDING"
            else -> "ALL SYSTEMS NOMINAL"
        }

        val actionPlan = mutableListOf<String>()
        if (criticalSubjects > 0) {
            actionPlan.add("Attend all scheduled lectures today to reverse negative attendance trend.")
        }
        if (urgentDeadlines.isNotEmpty()) {
            val top = urgentDeadlines.first()
            actionPlan.add("Finalize and submit \"${top.title}\" before deadline (${top.dueDate}).")
        }
        if (pendingTasks.isNotEmpty()) {
            val topTask = pendingTasks.first()
            actionPlan.add("Complete top task: \"${topTask.description}\".")
        }
        if (actionPlan.isEmpty()) {
            actionPlan.add("Review lecture notes and maintain current 100% academic consistency.")
        }

        return DailyCollegeStrategy(
            headline = headline,
            overallStatusBadge = badge,
            canBunkToday = canBunkAnyToday,
            bunkAdviceItems = bunkAdvices,
            urgentDeadlines = urgentDeadlines,
            pendingTasksSummary = pendingTasksText,
            actionPlanSteps = actionPlan
        )
    }

    /**
     * Extracts actionable tasks from free-form note text
     */
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

    /**
     * Generates a concise summary for student notes
     */
    fun summarizeNote(title: String, content: String): String {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size <= 2) return content

        val points = lines.take(3).map { "• ${it.trim().removePrefix("-").removePrefix("*").trim()}" }
        return "📌 Key Takeaways for \"$title\":\n" + points.joinToString("\n")
    }

    /**
     * Generative Gemini Query (gemini-3.5-flash) with robust local offline fallback
     */
    suspend fun queryAssistant(
        prompt: String,
        statsList: List<SubjectAttendanceStats>,
        slots: List<SlotDisplayItem>,
        assessments: List<AssessmentEntity>,
        tasks: List<AcademicTaskEntity>,
        notes: List<QuickNoteEntity>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        // If no API key is provided, execute local intelligence engine immediately with zero delay
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalIntelligenceResponse(prompt, statsList, slots, assessments, tasks, notes)
        }

        try {
            val modelName = "gemini-3.5-flash"
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val systemContext = buildString {
                append("You are Caliper AI, the tactical academic copilot for IERT Prayagraj engineering students. ")
                append("Provide crisp, concise, pragmatic advice about attendance, bunk safety, drawing sheets, and deadlines. ")
                append("Statutory attendance rule: 75% minimum. Bunk formula: B = floor((Attended - 0.75*Conducted)/0.75). ")
                append("\nCurrent Student Telemetry:\n")
                statsList.forEach { s ->
                    append("- ${s.subject.code} (${s.subject.name}): Attended ${s.attended}/${s.totalConducted} (${String.format("%.1f", s.percentage)}%), Buffer Bunks: ${s.bunksAvailable}, Needed: ${s.classesNeeded}\n")
                }
                append("Upcoming Slots Today: ${slots.joinToString { "${it.slot.startTime} ${it.subject?.code ?: "Lecture"}" }}\n")
                append("Pending Assessments: ${assessments.filter { it.status != "submitted" }.take(3).joinToString { "${it.title} (due ${it.dueDate})" }}\n")
                append("Pending Tasks: ${tasks.filter { !it.isCompleted }.take(3).joinToString { it.description }}\n")
            }

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", "$systemContext\n\nStudent Question: $prompt"))
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
                connectTimeout = 8000
                readTimeout = 12000
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
            // Fallback if HTTP response wasn't optimal
            generateLocalIntelligenceResponse(prompt, statsList, slots, assessments, tasks, notes)
        } catch (_: Exception) {
            // Instant, glitch-free fallback on any timeout or network error
            generateLocalIntelligenceResponse(prompt, statsList, slots, assessments, tasks, notes)
        }
    }

    private fun generateLocalIntelligenceResponse(
        prompt: String,
        statsList: List<SubjectAttendanceStats>,
        slots: List<SlotDisplayItem>,
        assessments: List<AssessmentEntity>,
        tasks: List<AcademicTaskEntity>,
        notes: List<QuickNoteEntity>
    ): String {
        val lower = prompt.lowercase()

        // 1. Question about Bunking / Attending
        if (lower.contains("bunk") || lower.contains("attend") || lower.contains("miss") || lower.contains("class") || lower.contains("today")) {
            val strategy = analyzeDailyStrategy(statsList, slots, assessments, tasks)
            val sb = StringBuilder()
            sb.append("📊 **Caliper Attendance & Bunk Analysis**\n\n")
            if (strategy.bunkAdviceItems.isNotEmpty()) {
                strategy.bunkAdviceItems.forEach { item ->
                    val statusIcon = if (item.isSafe) "✅" else "⚠️"
                    sb.append("$statusIcon **${item.subjectCode}**: ${item.recommendation}\n")
                }
            } else {
                sb.append("No lectures currently scheduled for today.\n")
            }
            val critical = statsList.filter { it.percentage < 75.0 }
            if (critical.isNotEmpty()) {
                sb.append("\n🚨 **Critical Warning**: ${critical.joinToString { it.subject.code }} below 75% statutory requirement.")
            }
            return sb.toString()
        }

        // 2. Question about Deadlines / Tasks / To-Do
        if (lower.contains("task") || lower.contains("todo") || lower.contains("deadline") || lower.contains("sheet") || lower.contains("pending") || lower.contains("assignment")) {
            val pendingAssessments = assessments.filter { it.status != "submitted" }
            val pendingTasks = tasks.filter { !it.isCompleted }

            val sb = StringBuilder()
            sb.append("📋 **Academic Deliverables & Deadlines**\n\n")
            if (pendingAssessments.isNotEmpty()) {
                sb.append("**Upcoming Submissions:**\n")
                pendingAssessments.take(3).forEach {
                    sb.append("• **${it.title}** (${it.type}) — Due: ${it.dueDate}\n")
                }
            } else {
                sb.append("• No pending assessment deadlines.\n")
            }

            if (pendingTasks.isNotEmpty()) {
                sb.append("\n**Top Priority To-Dos:**\n")
                pendingTasks.take(3).forEach {
                    sb.append("• [${it.priority}] ${it.description} (Due: ${it.dueDate})\n")
                }
            } else {
                sb.append("\n• To-Do list is completely clear!\n")
            }
            return sb.toString()
        }

        // 3. Question about Notes
        if (lower.contains("note") || lower.contains("viva") || lower.contains("formula")) {
            val sb = StringBuilder()
            sb.append("📝 **Quick Notes Vault**\n\n")
            if (notes.isNotEmpty()) {
                sb.append("Found ${notes.size} saved note(s):\n")
                notes.take(3).forEach {
                    sb.append("• **${it.title}** [${it.category}]: ${it.content.take(60)}...\n")
                }
            } else {
                sb.append("No notes recorded yet. Use the Quick Notes tab to jot down formulas, workshop jobs, or sheet notes.")
            }
            return sb.toString()
        }

        // 4. General comprehensive briefing
        val strategy = analyzeDailyStrategy(statsList, slots, assessments, tasks)
        return buildString {
            append("🎯 **Caliper Academic Executive Briefing**\n\n")
            append("• **Status**: ${strategy.headline}\n")
            append("• **Schedule**: ${if (slots.isEmpty()) "No classes today" else "${slots.size} lecture slots today"}\n")
            append("• **Tasks**: ${strategy.pendingTasksSummary}\n")
            if (strategy.actionPlanSteps.isNotEmpty()) {
                append("\n**Action Plan:**\n")
                strategy.actionPlanSteps.forEach { step ->
                    append("→ $step\n")
                }
            }
        }
    }
}
