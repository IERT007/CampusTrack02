package com.example.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.CampusRepository
import kotlinx.coroutines.flow.first

class CaliperTodoWidget : GlanceAppWidget() {

    companion object {
        val PARAM_TASK_ID = ActionParameters.Key<Long>("task_id")
        val PARAM_NEW_STATUS = ActionParameters.Key<Boolean>("new_status")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val allTasks = db.academicTaskDao().getAllTasks().first()
        val pendingTasks = allTasks.filter { !it.isCompleted }.sortedBy { it.dueDate }
        val displayTasks = pendingTasks.take(2)

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(18.dp)
                    .background(Color(0xFF05070B))
                    .padding(14.dp)
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALIPER • TO-DO",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF38BDF8)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(6.dp)
                                .background(Color(0x1AFBBF24))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${pendingTasks.size} Pending",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFFBBF24)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (displayTasks.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All academic tasks & sheets completed! ✓",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF34D399)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    } else {
                        for (task in displayTasks) {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = task.description,
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFFF8FAFC)),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${task.category} • Due: ${task.dueDate} • ${task.priority}",
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFF94A3B8)),
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                Button(
                                    text = "Done",
                                    onClick = actionRunCallback<ToggleTaskActionCallback>(
                                        actionParametersOf(
                                            PARAM_TASK_ID to task.id,
                                            PARAM_NEW_STATUS to true
                                        )
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = ColorProvider(Color(0xFF34D399)),
                                        contentColor = ColorProvider(Color(0xFF05070B))
                                    ),
                                    modifier = GlanceModifier.height(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class CaliperTodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaliperTodoWidget()
}

class ToggleTaskActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[CaliperTodoWidget.PARAM_TASK_ID] ?: return
        val newStatus = parameters[CaliperTodoWidget.PARAM_NEW_STATUS] ?: true
        val db = AppDatabase.getDatabase(context)

        db.academicTaskDao().updateTaskCompletion(taskId, newStatus)
        CampusRepository(context, db).triggerSilentAutoBackup()
        CaliperTodoWidget().update(context, glanceId)
    }
}
