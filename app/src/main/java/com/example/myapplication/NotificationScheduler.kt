package com.example.myapplication

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.data.HabitCompletion
import com.example.myapplication.data.HabitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class NotificationScheduler {
    companion object {
        private const val CHANNEL_ID = "habit_reminder_channel"
        private const val CHANNEL_NAME = "Habit Reminders"
        private const val TAG = "NotificationScheduler"
        
        fun scheduleNotification(
            context: Context,
            habitId: Int,
            habitName: String,
            hour: Int,
            minute: Int
        ) {
            createNotificationChannel(context)
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("habitId", habitId)
                putExtra("habitName", habitName)
                putExtra("hour", hour)
                putExtra("minute", minute)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                habitId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the time has passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            Log.d(TAG, "Scheduling notification for $habitName at $hour:$minute")
            Log.d(TAG, "Next alarm time: ${calendar.time}")
            
            try {
                // For Android 12+ (API 31+), use setExactAndAllowWhileIdle for better reliability
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Check if we can schedule exact alarms
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Scheduled exact alarm for API 31+")
                    } else {
                        // Fallback to inexact alarm
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Scheduled inexact alarm (no exact alarm permission)")
                    }
                } else {
                    // For older Android versions
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for older API")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling notification", e)
            }
        }
        
        fun cancelNotification(context: Context, habitId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                habitId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
        }
        
        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily reminders for your habits"
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getIntExtra("habitId", 0)
        val habitName = intent.getStringExtra("habitName") ?: "Habit"
        val hour = intent.getIntExtra("hour", 9)
        val minute = intent.getIntExtra("minute", 0)
        
        Log.d("NotificationReceiver", "Received alarm for $habitName")
        
        // Show the notification
        showNotification(context, habitId, habitName)
        
        // Reschedule for tomorrow (since we're using setExactAndAllowWhileIdle which is one-time)
        NotificationScheduler.scheduleNotification(context, habitId, habitName, hour, minute)
    }
    
    private fun showNotification(context: Context, habitId: Int, habitName: String) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            
            // Create "Yes" action intent
            val yesIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_COMPLETE"
                putExtra("habitId", habitId)
                putExtra("habitName", habitName)
            }
            val yesPendingIntent = PendingIntent.getBroadcast(
                context,
                habitId * 1000 + 1, // Unique request code
                yesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Create "No" action intent
            val noIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_DISMISS"
                putExtra("habitId", habitId)
            }
            val noPendingIntent = PendingIntent.getBroadcast(
                context,
                habitId * 1000 + 2, // Unique request code
                noIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, "habit_reminder_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Habit Reminder")
                .setContentText("Did you complete: $habitName?")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .addAction(android.R.drawable.ic_input_add, "Yes ✓", yesPendingIntent)
                .addAction(android.R.drawable.ic_delete, "No ✗", noPendingIntent)
                .build()
            
            notificationManager.notify(habitId, notification)
            Log.d("NotificationReceiver", "Notification shown for $habitName")
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error showing notification", e)
        }
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getIntExtra("habitId", 0)
        val habitName = intent.getStringExtra("habitName") ?: "Habit"
        
        when (intent.action) {
            "ACTION_COMPLETE" -> {
                // Mark habit as complete for today
                markHabitComplete(context, habitId)
                
                // Show a confirmation notification
                showConfirmationNotification(context, habitId, habitName, true)
                
                Log.d("NotificationAction", "Marked $habitName as complete")
            }
            "ACTION_DISMISS" -> {
                // Just dismiss the notification
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(habitId)
                
                Log.d("NotificationAction", "Dismissed notification for $habitName")
            }
        }
    }
    
    private fun markHabitComplete(context: Context, habitId: Int) {
        // Use a coroutine to mark the habit as complete
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = HabitDatabase.getDatabase(context)
                val dao = database.habitDao()
                
                // Get today's date
                val today = LocalDate.now()
                val dateString = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                // Check if already completed
                val existing = dao.getCompletion(habitId, dateString)
                if (existing == null) {
                    // Add completion
                    dao.insertCompletion(
                        HabitCompletion(
                            habitId = habitId,
                            date = dateString
                        )
                    )
                    Log.d("NotificationAction", "Added completion for habit $habitId on $dateString")
                }
            } catch (e: Exception) {
                Log.e("NotificationAction", "Error marking habit complete", e)
            }
        }
    }
    
    private fun showConfirmationNotification(context: Context, habitId: Int, habitName: String, completed: Boolean) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            
            // Cancel the original notification
            notificationManager.cancel(habitId)
            
            // Show a brief confirmation
            val notification = NotificationCompat.Builder(context, "habit_reminder_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Great job! ✓")
                .setContentText("$habitName marked as complete")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(3000) // Auto-dismiss after 3 seconds
                .build()
            
            notificationManager.notify(habitId + 10000, notification) // Different ID for confirmation
        } catch (e: Exception) {
            Log.e("NotificationAction", "Error showing confirmation", e)
        }
    }
}
