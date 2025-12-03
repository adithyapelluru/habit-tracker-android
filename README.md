# Habit Tracker Android App

A modern, feature-rich habit tracking application built with Kotlin and Jetpack Compose.

## Features

### 📅 Multiple Views
- **Daily View**: Track your habits for the current week with a rolling 7-day view
- **Monthly View**: See your entire month at a glance with expandable habit calendars
- **Streaks View**: Monitor your habit streaks and consistency over time

### ✅ Smart Tracking
- Mark habits as complete only for TODAY (prevents backdating/future-dating)
- Once marked complete, habits cannot be unchecked (maintains data integrity)
- Visual indicators: Blue (completed), Light Green (today), Gray (past/future)
- Automatic streak calculation

### 🔔 Interactive Notifications
- Schedule daily reminders for each habit
- Action buttons in notifications: "Yes ✓" or "No ✗"
- Mark habits complete directly from notifications
- Confirmation feedback when completed via notification

### 🗑️ Habit Management
- Long-press (2 seconds) on any habit to delete
- Cascade delete removes all completion history
- Clean, intuitive UI without clutter

### 💾 Data Persistence
- Room Database for reliable local storage
- Foreign key relationships with cascade delete
- Automatic data refresh across all views

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room
- **Notifications**: AlarmManager + NotificationCompat
- **Async**: Kotlin Coroutines + Flow

## Screenshots

*(Add screenshots here)*

## Installation

1. Clone this repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on an Android device or emulator (API 26+)

## Permissions

- `POST_NOTIFICATIONS` - For daily habit reminders
- `SCHEDULE_EXACT_ALARM` - For precise notification timing
- `RECEIVE_BOOT_COMPLETED` - To reschedule notifications after device restart

## Usage

### Adding a Habit
1. Tap the "+" button in the bottom navigation
2. Enter habit name
3. Set notification time
4. Save

### Marking Complete
- **From App**: Tap today's circle (light green) in Daily or Monthly view
- **From Notification**: Tap "Yes ✓" button when notification appears

### Viewing Streaks
1. Navigate to Streaks view (star icon)
2. Select month from dropdown
3. Tap habit name to expand and see detailed calendar

### Deleting a Habit
1. Long-press (hold for 2 seconds) on any habit card
2. Confirm deletion in dialog

## Project Structure

```
app/src/main/java/com/example/myapplication/
├── data/
│   ├── Habit.kt                 # Habit entity
│   ├── HabitCompletion.kt       # Completion entity with foreign key
│   ├── HabitDao.kt              # Database access object
│   └── HabitDatabase.kt         # Room database
├── viewmodel/
│   └── HabitViewModel.kt        # ViewModel for habit operations
├── ui/theme/                    # Theme configuration
├── MainActivity.kt              # Main UI with Compose
├── NotificationScheduler.kt     # Notification handling
└── BootReceiver.kt              # Boot completion receiver
```

## Key Features Implementation

### Today-Only Marking
- Prevents habit manipulation by locking past and future dates
- Only current day is clickable and highlighted in light green
- Encourages honest, real-time tracking

### Permanent Completions
- Once marked complete, habits cannot be unchecked
- Maintains data integrity and honest tracking history
- Visual feedback with checkmark on completed days

### Cascade Delete
- Foreign key relationship ensures all completions are deleted with habit
- Database version 2 with proper schema migration

## Future Enhancements

- [ ] Habit categories/tags
- [ ] Custom habit colors
- [ ] Statistics and insights
- [ ] Export data to CSV
- [ ] Dark mode
- [ ] Widget support
- [ ] Backup and restore

## License

MIT License - feel free to use this project for learning or personal use.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
