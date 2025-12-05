package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
