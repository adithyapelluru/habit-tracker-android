package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val habitDao = HabitDatabase.getDatabase(application).habitDao()
    val habits: Flow<List<Habit>> = habitDao.getAllHabits()
    
    fun addHabit(name: String, hour: Int = 9, minute: Int = 0) {
        viewModelScope.launch {
            habitDao.insertHabit(Habit(name = name, notificationHour = hour, notificationMinute = minute))
        }
    }
    
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
        }
    }
    
    fun updateHabit(habit: Habit, newName: String, newHour: Int, newMinute: Int) {
        viewModelScope.launch {
            habitDao.updateHabit(habit.copy(name = newName, notificationHour = newHour, notificationMinute = newMinute))
        }
    }
    
    fun toggleCompletion(habitId: Int, date: String) {
        viewModelScope.launch {
            val existing = habitDao.getCompletion(habitId, date)
            if (existing != null) {
                habitDao.deleteCompletion(existing)
            } else {
                habitDao.insertCompletion(HabitCompletion(habitId = habitId, date = date))
            }
        }
    }
    
    suspend fun getCompletionsForWeek(habitId: Int, startDate: String, endDate: String): List<HabitCompletion> {
        return habitDao.getCompletionsInRange(habitId, startDate, endDate)
    }
    
    fun getDateString(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
