package com.example.ui.screens.notes

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.CaliperAiEngine
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.AcademicTaskEntity
import com.example.data.local.entity.QuickNoteEntity
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.CaliperTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QuickNotesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val notes by viewModel.notes.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var viewingAiSummary by remember { mutableStateOf<Pair<String, String>?>(null) }
    var viewingActionItems by remember { mutableStateOf<Pair<QuickNoteEntity, List<String>>?>(null) }

    val categories = listOf("All", "Drawing Sheet", "Workshop", "Viva", "Lecture", "Important", "General")

    val filteredNotes = remember(notes, searchQuery, selectedCategory) {
        notes.filter { note ->
            val matchesCategory = (selectedCategory == "All" || note.category.equals(selectedCategory, ignoreCase = true))
            val matchesQuery = (searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true) ||
                    (note.subjectCode?.contains(searchQuery, ignoreCase = true) == true))
            matchesCategory && matchesQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick Notes & Vault",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Capture formulas, drawing guidelines & viva hints",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }

                Button(
                    onClick = {
                        CaliperSoundManager.playSnap()
                        CaliperHapticManager.tick(context)
                        showAddNoteDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryAccent.copy(alpha = 0.2f),
                        contentColor = colors.primaryAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.border(0.5.dp, colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Note", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(colors.primaryAccent),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Search notes, formulas, viva hints...", color = colors.textMuted, fontSize = 13.sp)
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.textMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = (selectedCategory == cat)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) colors.primaryAccent.copy(alpha = 0.22f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                0.5.dp,
                                if (isSelected) colors.primaryAccent.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                CaliperSoundManager.playSnap()
                                selectedCategory = cat
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.primaryAccent else colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes List
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NoteAlt,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No notes yet in $selectedCategory" else "No notes match \"$searchQuery\"",
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap '+ New Note' to quickly capture important details",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onTogglePin = { viewModel.toggleNotePin(note.id, note.isPinned) },
                            onDelete = { viewModel.deleteNote(note.id) },
                            onAiSummarize = {
                                val summary = CaliperAiEngine.summarizeNote(note.title, note.content)
                                viewingAiSummary = Pair(note.title, summary)
                            },
                            onAiExtractTasks = {
                                val items = CaliperAiEngine.extractActionItemsFromText(note.content)
                                viewingActionItems = Pair(note, items)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        AddNoteDialog(
            subjects = subjects.map { it.code },
            onDismiss = { showAddNoteDialog = false },
            onSave = { title, content, category, subCode ->
                viewModel.addNote(title, content, category, subCode)
                showAddNoteDialog = false
            }
        )
    }

    // AI Summary Modal
    viewingAiSummary?.let { (title, summary) ->
        Dialog(onDismissRequest = { viewingAiSummary = null }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                borderColors = listOf(colors.primaryAccent.copy(alpha = 0.6f), colors.cardBorderBottom)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Key Points: $title", fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 14.sp)
                        }
                        IconButton(onClick = { viewingAiSummary = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(summary, color = colors.textPrimary, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }

    // AI Extract Action Items Modal
    viewingActionItems?.let { (note, actionItems) ->
        Dialog(onDismissRequest = { viewingActionItems = null }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                borderColors = listOf(colors.safeZone.copy(alpha = 0.6f), colors.cardBorderBottom)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = colors.safeZone, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Extracted To-Dos", fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 14.sp)
                        }
                        IconButton(onClick = { viewingActionItems = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Found ${actionItems.size} action item(s) in this note. Tap to add directly to your academic task list:", fontSize = 11.sp, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(10.dp))

                    actionItems.forEach { actionText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(actionText, color = colors.textPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    viewModel.addTask(
                                        AcademicTaskEntity(
                                            description = actionText,
                                            priority = "High",
                                            category = note.category,
                                            dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        )
                                    )
                                    CaliperSoundManager.playSuccess()
                                    Toast.makeText(context, "Added to To-Do list!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.safeZone.copy(alpha = 0.2f), contentColor = colors.safeZone),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("+ Add Task", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCardItem(
    note: QuickNoteEntity,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onAiSummarize: () -> Unit,
    onAiExtractTasks: () -> Unit
) {
    val colors = CaliperTheme.colors
    val dateStr = remember(note.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(note.timestamp))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = if (note.isPinned) {
            listOf(colors.primaryAccent.copy(alpha = 0.5f), colors.cardBorderBottom)
        } else {
            listOf(colors.cardBorderTop, colors.cardBorderBottom)
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Category pill, Subject badge, Pinned icon, Delete icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primaryAccent.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(note.category, color = colors.primaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!note.subjectCode.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(note.subjectCode, color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateStr, color = colors.textMuted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Default.BookmarkBorder,
                            contentDescription = "Pin",
                            tint = if (note.isPinned) colors.primaryAccent else colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = colors.bunkDanger.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Note Title
            Text(
                text = note.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Note Content
            Text(
                text = note.content,
                fontSize = 12.sp,
                color = colors.textSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // AI Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAiSummarize,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryAccent),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Summary", fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onAiExtractTasks,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.safeZone),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI To-Dos", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun AddNoteDialog(
    subjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, subjectCode: String?) -> Unit
) {
    val colors = CaliperTheme.colors
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Drawing Sheet") }
    var subjectCode by remember { mutableStateOf(subjects.firstOrNull() ?: "") }

    val categories = listOf("Drawing Sheet", "Workshop", "Viva", "Lecture", "Important", "General")

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            borderColors = listOf(colors.primaryAccent.copy(alpha = 0.6f), colors.cardBorderBottom)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New Quick Note", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title (e.g. Sheet #4 Ellipse Guidelines)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Content Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content / Key points / Submission details...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Selector
                Text("Category:", fontSize = 11.sp, color = colors.textMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            onSave(title, content, category, subjectCode)
                        }
                    },
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent)
                ) {
                    Text("Save Quick Note", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
