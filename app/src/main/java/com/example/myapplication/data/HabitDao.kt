package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<Habit>>
    
    @Insert
    suspend fun insertHabit(habit: Habit): Long
    
    @Delete
    suspend fun deleteHabit(habit: Habit)
    
    @Update
    suspend fun updateHabit(habit: Habit)
    
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun getCompletion(habitId: Int, date: String): HabitCompletion?
    
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date >= :startDate AND date <= :endDate")
    suspend fun getCompletionsInRange(habitId: Int, startDate: String, endDate: String): List<HabitCompletion>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)
    
    @Delete
    suspend fun deleteCompletion(completion: HabitCompletion)
}
