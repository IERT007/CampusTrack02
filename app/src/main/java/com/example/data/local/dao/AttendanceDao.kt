package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AttendanceLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * AttendanceDao alias / interface for granular log deletion and manipulation
 */
typealias AttendanceDao = AttendanceLogDao
