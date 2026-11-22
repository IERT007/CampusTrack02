package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TodoTaskEntity — Smart Auto-Prioritized Academic To-Do Task
 * Persisted in Room `todo_tasks` table (added cleanly via AppDatabase MIGRATION_6_7).
 */
@Entity(tableName = "todo_tasks")
data class TodoTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val subjectId: Long? = null,
    val dueDateTimeMillis: Long,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val semesterId: Long = 1L
)
