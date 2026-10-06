package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.CaliperAiEngine
import com.example.audio.CaliperHardwareEngine
import com.example.data.local.entity.AssessmentEntity
import com.example.ui.MainViewModel
import com.example.ui.SubjectAttendanceStats
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class RoboMood(
    val title: String,
    val coreColor: Color,
    val glowColor: Color,
    val badgeLabel: String
) {
    ZEN("Zen Chill", Color(0xFF10B981), Color(0x6610B981), "🟢 ZEN (>=78% ALL)"),
    ALERT("Alert Focused", Color(0xFFF59E0B), Color(0x66F59E0B), "🟡 ALERT (DEADLINE/BUFFER)"),
    STRESSED("Urgent Stressed", Color(0xFFEF4444), Color(0x66EF4444), "🔴 STRESSED (<75% DEBAR RISK)")
}

data class CopilotChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val timestamp: String = "Now"
)

/**
 * 3D Floating Robo Copilot:
 * - Interactive, hardware-accelerated 3D vector/canvas Robo floating above the navigation bar
 * - Draggable physics across screen edges with spring snap-to-edge
 * - Continuous gentle vertical hovering/levitation idle animation (±6dp)
 * - Real-time dynamic mood matrix (Zen/Alert/Stressed)
 * - Search eye sync (eyes look upward towards search bar when typing)
 * - Tapping triggers spin/hop animation, sensory feedback, and opens Caliper Copilot HUD
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaliperRoboCompanion(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val statsList by viewModel.subjectStats.collectAsState()
    val summary by viewModel.globalSummary.collectAsState()
    val assessments by viewModel.assessments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showCopilotHud by remember { mutableStateOf(false) }

    // Dynamic Mood Matrix Calculation
    val currentMood = remember(statsList, summary, assessments) {
        val hasDebarDanger = summary.overallPercentage < 75.0 || statsList.any { it.percentage < 75.0 }
        if (hasDebarDanger) {
            RoboMood.STRESSED
        } else {
            val now = LocalDate.now()
            val hasUpcomingDeadline24h = assessments.any { a ->
                if (a.status == "submitted") false
                else {
                    try {
                        val due = LocalDate.parse(a.dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
                        val diff = ChronoUnit.DAYS.between(now, due)
                        diff in 0..1
                    } catch (_: Exception) {
                        false
                    }
                }
            }
            val hasLowBuffer = statsList.any { it.bunksAvailable <= 1 }
            if (hasUpcomingDeadline24h || hasLowBuffer) {
                RoboMood.ALERT
            } else {
                val allAbove78 = summary.overallPercentage >= 78.0 && statsList.all { it.percentage >= 78.0 }
                val zeroUpcoming48h = assessments.none { a ->
                    if (a.status == "submitted") false
                    else {
                        try {
                            val due = LocalDate.parse(a.dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            ChronoUnit.DAYS.between(now, due) in 0..2
                        } catch (_: Exception) {
                            false
                        }
                    }
                }
                if (allAbove78 && zeroUpcoming48h) RoboMood.ZEN else RoboMood.ALERT
            }
        }
    }

    val isSearching = searchQuery.isNotBlank()

    // Hover Animation (±6dp vertical bobbing)
    val infiniteTransition = rememberInfiniteTransition(label = "robo_hover_transition")
    val hoverOffsetDp by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hover_bobbing"
    )

    // Core Reactor Pulse
    val coreGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Interactive Tap Animations: 3D Spin and Hop
    val spinAngle = remember { Animatable(0f) }
    val hopOffsetY = remember { Animatable(0f) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // Default resting position: bottom-right, ~88dp above bottom nav
        val defaultXPx = with(density) { (maxWidth - 86.dp).toPx() }
        val defaultYPx = with(density) { (maxHeight - 165.dp).toPx() }

        val dragOffsetX = remember { Animatable(defaultXPx) }
        val dragOffsetY = remember { Animatable(defaultYPx) }

        val hoverOffsetPx = with(density) { hoverOffsetDp.dp.toPx() }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = dragOffsetX.value.roundToInt(),
                        y = (dragOffsetY.value + hoverOffsetPx + hopOffsetY.value).roundToInt()
                    )
                }
                .size(70.dp)
                .rotate(spinAngle.value)
                .testTag("caliper_robo_companion")
                .pointerInput(screenWidthPx) {
                    detectDragGestures(
                        onDragEnd = {
                            // Snap to nearest screen edge with spring physics
                            val targetX = if (dragOffsetX.value < screenWidthPx / 2f) {
                                with(density) { 16.dp.toPx() }
                            } else {
                                with(density) { (maxWidth - 86.dp).toPx() }
                            }
                            coroutineScope.launch {
                                dragOffsetX.animateTo(
                                    targetValue = targetX,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newX = (dragOffsetX.value + dragAmount.x).coerceIn(
                                with(density) { 10.dp.toPx() },
                                with(density) { (maxWidth - 80.dp).toPx() }
                            )
                            val newY = (dragOffsetY.value + dragAmount.y).coerceIn(
                                with(density) { 60.dp.toPx() },
                                with(density) { (maxHeight - 150.dp).toPx() }
                            )
                            dragOffsetX.snapTo(newX)
                            dragOffsetY.snapTo(newY)
                        }
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Tap trigger: Acoustic synth pip + micro-vernier haptic pulse + spin & hop
                    CaliperHardwareEngine.playSound(CaliperHardwareEngine.TICK)
                    CaliperHardwareEngine.pulseHaptic(context)

                    coroutineScope.launch {
                        launch {
                            spinAngle.snapTo(0f)
                            spinAngle.animateTo(
                                targetValue = 360f,
                                animationSpec = tween(420, easing = FastOutSlowInEasing)
                            )
                            spinAngle.snapTo(0f)
                        }
                        launch {
                            hopOffsetY.animateTo(-22f, tween(180, easing = EaseOutQuad))
                            hopOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    }
                    showCopilotHud = true
                }
        ) {
            // Hardware-Accelerated 3D Vector/Canvas Robo Graphic
            RoboCanvasRenderer(
                mood = currentMood,
                coreGlow = coreGlowPulse,
                isLookingUp = isSearching
            )
        }
    }

    // Caliper Copilot HUD Modal Bottom Sheet
    if (showCopilotHud) {
        CaliperCopilotHudSheet(
            viewModel = viewModel,
            mood = currentMood,
            onDismiss = { showCopilotHud = false }
        )
    }
}

/**
 * Procedural Vector Graphics for 3D Levitating Vernier Robo Companion
 */
@Composable
private fun RoboCanvasRenderer(
    mood: RoboMood,
    coreGlow: Float,
    isLookingUp: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Dual Levitation Thruster Glow Particles (Bottom)
        val thrusterY = h * 0.88f
        val thrusterLeftX = w * 0.32f
        val thrusterRightX = w * 0.68f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mood.coreColor.copy(alpha = 0.8f * coreGlow), Color.Transparent),
                center = Offset(thrusterLeftX, thrusterY),
                radius = w * 0.22f
            ),
            radius = w * 0.22f,
            center = Offset(thrusterLeftX, thrusterY)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mood.coreColor.copy(alpha = 0.8f * coreGlow), Color.Transparent),
                center = Offset(thrusterRightX, thrusterY),
                radius = w * 0.22f
            ),
            radius = w * 0.22f,
            center = Offset(thrusterRightX, thrusterY)
        )

        // 2. Vernier Caliper Mechanical Crown Antenna (Top)
        val crownPath = Path().apply {
            moveTo(cx - w * 0.16f, h * 0.12f)
            lineTo(cx, h * 0.04f)
            lineTo(cx + w * 0.16f, h * 0.12f)
            lineTo(cx + w * 0.08f, h * 0.22f)
            lineTo(cx - w * 0.08f, h * 0.22f)
            close()
        }
        drawPath(
            path = crownPath,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF64748B), Color(0xFF1E293B))
            )
        )
        // Antenna Tip Beacon
        drawCircle(
            color = mood.coreColor,
            radius = w * 0.045f,
            center = Offset(cx, h * 0.04f)
        )

        // 3. Spherical 3D Mechanical Head/Body Chassis
        val bodyCenter = Offset(cx, cy * 0.98f)
        val bodyRadius = w * 0.40f

        // Outer Metallic Chassis with 3D Specular Highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF334155),
                    Color(0xFF1E293B),
                    Color(0xFF090D16)
                ),
                center = Offset(cx - w * 0.1f, cy * 0.85f),
                radius = bodyRadius
            ),
            radius = bodyRadius,
            center = bodyCenter
        )

        // Vernier Caliper Precision Rim Ring
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    mood.coreColor.copy(alpha = 0.7f),
                    Color(0x3300E5FF),
                    mood.coreColor.copy(alpha = 0.7f)
                ),
                center = bodyCenter
            ),
            radius = bodyRadius,
            center = bodyCenter,
            style = Stroke(width = 2.dp.toPx())
        )

        // 4. Glossy Translucent Visor Display
        val visorRect = Size(w * 0.62f, h * 0.32f)
        val visorOffset = Offset(cx - visorRect.width / 2f, cy * 0.76f - visorRect.height / 2f)

        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF030712), Color(0xFF0F172A))
            ),
            topLeft = visorOffset,
            size = visorRect,
            cornerRadius = CornerRadius(visorRect.height / 2f, visorRect.height / 2f)
        )
        drawRoundRect(
            color = mood.coreColor.copy(alpha = 0.45f),
            topLeft = visorOffset,
            size = visorRect,
            cornerRadius = CornerRadius(visorRect.height / 2f, visorRect.height / 2f),
            style = Stroke(width = 1.dp.toPx())
        )

        // 5. Digital Reactive Expressive Eyes (Syncs with Search Bar & Mood)
        val eyeEyeY = if (isLookingUp) cy * 0.69f else cy * 0.76f // Look upward if searching!
        val eyeLeftX = cx - w * 0.15f
        val eyeRightX = cx + w * 0.15f
        val eyeRadius = w * 0.055f

        // Glowing Pupils
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mood.coreColor, mood.coreColor.copy(alpha = 0.4f)),
                center = Offset(eyeLeftX, eyeEyeY),
                radius = eyeRadius * 1.5f
            ),
            radius = eyeRadius,
            center = Offset(eyeLeftX, eyeEyeY)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mood.coreColor, mood.coreColor.copy(alpha = 0.4f)),
                center = Offset(eyeRightX, eyeEyeY),
                radius = eyeRadius * 1.5f
            ),
            radius = eyeRadius,
            center = Offset(eyeRightX, eyeEyeY)
        )

        // Eye White Core Glint
        drawCircle(color = Color.White, radius = eyeRadius * 0.35f, center = Offset(eyeLeftX - 1.dp.toPx(), eyeEyeY - 1.dp.toPx()))
        drawCircle(color = Color.White, radius = eyeRadius * 0.35f, center = Offset(eyeRightX - 1.dp.toPx(), eyeEyeY - 1.dp.toPx()))

        // 6. Chest Vernier Reactor Core (Pulsing Center)
        val coreCenter = Offset(cx, cy * 1.15f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mood.coreColor.copy(alpha = 0.85f * coreGlow), Color.Transparent),
                center = coreCenter,
                radius = w * 0.18f
            ),
            radius = w * 0.18f,
            center = coreCenter
        )
        drawCircle(
            color = mood.coreColor,
            radius = w * 0.065f,
            center = coreCenter
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = w * 0.03f,
            center = coreCenter
        )
    }
}

/**
 * Translucent Frosted Modal Bottom Sheet: Caliper Copilot HUD
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaliperCopilotHudSheet(
    viewModel: MainViewModel,
    mood: RoboMood,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = CaliperTheme.colors
    val listState = rememberLazyListState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var inputPrompt by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val fullContext = remember { viewModel.getFullSystemContext() }

    var messages by remember {
        mutableStateOf(
            listOf(
                CopilotChatMessage(
                    id = 1L,
                    isUser = false,
                    text = "🤖 **Caliper Tactical Copilot Online**\n\n" +
                            "I have 100% full awareness of your Room DB (all 6-day timetable periods, safe bunk margins for ME-301..ME-305L, and pending sheets).\n\n" +
                            "Tap any telemetry chip below or ask anything!"
                )
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2060A12),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row: Bot Identity + Live Mood Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(mood.coreColor.copy(alpha = 0.15f))
                            .border(1.dp, mood.coreColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrecisionManufacturing,
                            contentDescription = null,
                            tint = mood.coreColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CALIPER COPILOT HUD",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "IERT Deep RAG Context Engine",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Dynamic Mood Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(mood.coreColor.copy(alpha = 0.15f))
                        .border(0.5.dp, mood.coreColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = mood.badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = mood.coreColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Telemetry Prompt Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickPromptChip("Schedule Tomorrow?", Modifier.weight(1f)) {
                    sendQuery(
                        "What is my schedule tomorrow?",
                        fullContext,
                        coroutineScope,
                        onSend = { messages = messages + it },
                        onThinking = { isThinking = it }
                    )
                }
                QuickPromptChip("Bunks in Drawing?", Modifier.weight(1f)) {
                    sendQuery(
                        "How many bunks in Drawing?",
                        fullContext,
                        coroutineScope,
                        onSend = { messages = messages + it },
                        onThinking = { isThinking = it }
                    )
                }
                QuickPromptChip("Skip Thermal Today?", Modifier.weight(1f)) {
                    sendQuery(
                        "Can I skip Thermal today?",
                        fullContext,
                        coroutineScope,
                        onSend = { messages = messages + it },
                        onThinking = { isThinking = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Conversational Scroll Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(message = msg, mood = mood)
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = mood.coreColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Evaluating Room DB & Vernier buffer matrix...",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Frosted Input Capsule (Voice/Text Input)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF131C2E))
                    .border(0.5.dp, Color(0x3300E5FF), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = mood.coreColor,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                CaliperHardwareEngine.pulseHaptic(context)
                                inputPrompt = "What drawing sheets are pending?"
                            }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(mood.coreColor),
                        decorationBox = { innerTextField ->
                            if (inputPrompt.isEmpty()) {
                                Text(
                                    text = "Ask Caliper AI about slots, sheets, bunks...",
                                    style = TextStyle(
                                        color = Color(0xFF64748B),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputPrompt.isNotBlank()) {
                                val userText = inputPrompt.trim()
                                inputPrompt = ""
                                CaliperHardwareEngine.playSound(CaliperHardwareEngine.SNAP)
                                CaliperHardwareEngine.pulseHaptic(context)

                                sendQuery(
                                    userText,
                                    fullContext,
                                    coroutineScope,
                                    onSend = { messages = messages + it },
                                    onThinking = { isThinking = it }
                                )
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(mood.coreColor.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = mood.coreColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

private fun sendQuery(
    text: String,
    fullContext: CaliperAiEngine.FullAcademicContext,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onSend: (CopilotChatMessage) -> Unit,
    onThinking: (Boolean) -> Unit
) {
    val userMsg = CopilotChatMessage(
        id = System.currentTimeMillis(),
        isUser = true,
        text = text
    )
    onSend(userMsg)
    onThinking(true)

    coroutineScope.launch {
        val answer = CaliperAiEngine.queryOmniscientAssistant(text, fullContext)
        onThinking(false)
        val botMsg = CopilotChatMessage(
            id = System.currentTimeMillis() + 1,
            isUser = false,
            text = answer
        )
        onSend(botMsg)
    }
}

@Composable
private fun QuickPromptChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE2E8F0),
            maxLines = 1
        )
    }
}

@Composable
private fun ChatBubbleItem(
    message: CopilotChatMessage,
    mood: RoboMood
) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 14.dp
                    )
                )
                .background(
                    if (isUser) Color(0xFF1E3A8A) else Color(0xFF0F172A)
                )
                .border(
                    0.5.dp,
                    if (isUser) Color(0xFF3B82F6) else mood.coreColor.copy(alpha = 0.35f),
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) Color.White else Color(0xFFF1F5F9),
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )
        }
    }
}
