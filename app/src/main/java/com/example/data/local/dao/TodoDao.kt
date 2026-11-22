package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TodoTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * TodoDao — Reactive Room DAO for Smart Auto-Prioritized To-Do tasks.
 */
@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_tasks ORDER BY isCompleted ASC, dueDateTimeMillis ASC")
    fun getAllTodoTasks(): Flow<List<TodoTaskEntity>>

    @Query("SELECT * FROM todo_tasks ORDER BY isCompleted ASC, dueDateTimeMillis ASC")
    suspend fun getAllTodoTasksList(): List<TodoTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoTask(task: TodoTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoTasks(tasks: List<TodoTaskEntity>)

    @Update
    suspend fun updateTodoTask(task: TodoTaskEntity)

    @Query("UPDATE todo_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateTodoCompletion(id: Long, completed: Boolean)

    @Delete
    suspend fun deleteTodoTask(task: TodoTaskEntity)

    @Query("DELETE FROM todo_tasks WHERE id = :id")
    suspend fun deleteTodoTaskById(id: Long)

    @Query("DELETE FROM todo_tasks")
    suspend fun deleteAllTodoTasks()
}
