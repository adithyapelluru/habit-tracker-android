package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.Habit
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.HabitViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5F5)
                ) {
                    HabitTrackerScreen()
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTrackerScreen(viewModel: HabitViewModel = viewModel()) {
    val habits by viewModel.habits.collectAsState(initial = emptyList())
    var currentView by remember { mutableStateOf("daily") } // "daily", "monthly", "streaks", or "add"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val currentMonth = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    
    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            if (currentView != "add") {
                BottomNavigationBar(
                    currentView = currentView,
                    onViewChange = { currentView = it }
                )
            }
        }
    ) { padding ->
        when (currentView) {
            "daily" -> DailyView(
                habits = habits,
                viewModel = viewModel,
                padding = padding
            )
            "monthly" -> MonthlyView(
                habits = habits,
                viewModel = viewModel,
                padding = padding
            )
            "streaks" -> StreaksView(
                habits = habits,
                viewModel = viewModel,
                padding = padding
            )
            "add" -> AddHabitView(
                onCancel = { currentView = "daily" },
                onSave = { name, hour, minute ->
                    viewModel.addHabit(name)
                    scope.launch {
                        kotlinx.coroutines.delay(500) // Wait for habit to be added
                        val allHabits = habits
                        val newHabit = allHabits.lastOrNull { habit -> habit.name == name }
                        if (newHabit != null) {
                            android.util.Log.d("MainActivity", "Scheduling notification for habit ${newHabit.id}: ${newHabit.name} at $hour:$minute")
                            NotificationScheduler.scheduleNotification(
                                context,
                                newHabit.id,
                                newHabit.name,
                                hour,
                                minute
                            )
                        } else {
                            android.util.Log.e("MainActivity", "Failed to find newly created habit")
                        }
                    }
                    currentView = "daily"
                },
                padding = padding
            )
        }
    }
}


@Composable
fun MonthlyCalendar(viewModel: HabitViewModel, habits: List<Habit>) {
    val currentDate = LocalDate.now()
    val yearMonth = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val firstDayOfMonth = currentDate.withDayOfMonth(1)
    val daysInMonth = currentDate.lengthOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    
    var allCompletions by remember { mutableStateOf<Map<String, Set<Int>>>(emptyMap()) }
    
    LaunchedEffect(habits) {
        val completionsMap = mutableMapOf<String, MutableSet<Int>>()
        habits.forEach { habit ->
            val startDate = viewModel.getDateString(firstDayOfMonth)
            val endDate = viewModel.getDateString(firstDayOfMonth.plusMonths(1).minusDays(1))
            val comps = viewModel.getCompletionsForWeek(habit.id, startDate, endDate)
            comps.forEach { completion ->
                val date = LocalDate.parse(completion.date)
                val day = date.dayOfMonth
                completionsMap.getOrPut(completion.date) { mutableSetOf() }.add(habit.id)
            }
        }
        allCompletions = completionsMap
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = yearMonth,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2C3E50),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7F8C8D),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val weeks = (daysInMonth + startDayOfWeek + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (week in 0 until weeks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (dayOfWeek in 0..6) {
                            val dayNumber = week * 7 + dayOfWeek - startDayOfWeek + 1
                            if (dayNumber in 1..daysInMonth) {
                                val date = firstDayOfMonth.withDayOfMonth(dayNumber)
                                val dateStr = viewModel.getDateString(date)
                                val hasCompletions = allCompletions[dateStr]?.isNotEmpty() == true
                                
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 14.sp,
                                    color = if (hasCompletions) Color(0xFF5DADE2) else Color(0xFF2C3E50),
                                    fontWeight = if (hasCompletions) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableHabitCard(habit: Habit, viewModel: HabitViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentDate = LocalDate.now()
    val firstDayOfMonth = currentDate.withDayOfMonth(1)
    val daysInMonth = currentDate.lengthOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    
    var completions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var refreshKey by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(habit.id, refreshKey) {
        val startDate = viewModel.getDateString(firstDayOfMonth)
        val endDate = viewModel.getDateString(firstDayOfMonth.plusMonths(1).minusDays(1))
        val comps = viewModel.getCompletionsForWeek(habit.id, startDate, endDate)
        completions = comps.map { it.date }.toSet()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { isExpanded = !isExpanded },
                        onLongPress = { showDeleteDialog = true }
                    )
                }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7F8C8D),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val weeks = (daysInMonth + startDayOfWeek + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (week in 0 until weeks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (dayOfWeek in 0..6) {
                                val dayNumber = week * 7 + dayOfWeek - startDayOfWeek + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val date = firstDayOfMonth.withDayOfMonth(dayNumber)
                                    val dateStr = viewModel.getDateString(date)
                                    val isCompleted = completions.contains(dateStr)
                                    val isToday = date == LocalDate.now()
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCompleted -> Color(0xFF5DADE2) // Blue for completed
                                                    isToday -> Color(0xFF95E1D3) // Light green for today
                                                    else -> Color(0xFFDDDDDD) // Gray for past/future
                                                }
                                            )
                                            .clickable(enabled = isToday && !isCompleted) {
                                                // Only allow marking TODAY as complete
                                                if (isToday && !isCompleted) {
                                                    scope.launch {
                                                        viewModel.toggleCompletion(habit.id, dateStr)
                                                        refreshKey++
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontSize = 12.sp,
                                            color = if (isCompleted) Color.White else Color(0xFF2C3E50),
                                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        DeleteHabitDialog(
            habitName = habit.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteHabit(habit)
                showDeleteDialog = false
            }
        )
    }
}




@Composable
fun DailyView(
    habits: List<Habit>,
    viewModel: HabitViewModel,
    padding: PaddingValues
) {
    val currentMonth = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Habit Tracker",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        
        Text(
            text = currentMonth,
            fontSize = 16.sp,
            color = Color(0xFF7F8C8D),
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        WeekDaysHeader()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(habits) { habit ->
                DailyHabitCard(habit = habit, viewModel = viewModel)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MonthlyView(
    habits: List<Habit>,
    viewModel: HabitViewModel,
    padding: PaddingValues
) {
    val currentMonth = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Habit Tracker",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        
        Text(
            text = currentMonth,
            fontSize = 16.sp,
            color = Color(0xFF7F8C8D),
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        MonthlyCalendar(viewModel = viewModel, habits = habits)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(habits) { habit ->
                ExpandableHabitCard(habit = habit, viewModel = viewModel)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StreaksView(
    habits: List<Habit>,
    viewModel: HabitViewModel,
    padding: PaddingValues
) {
    var selectedYear by remember { mutableStateOf(LocalDate.now().year) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    var showMonthPicker by remember { mutableStateOf(false) }
    
    val selectedDate = remember(selectedYear, selectedMonth) {
        LocalDate.of(selectedYear, selectedMonth, 1)
    }
    
    val monthYearText = remember(selectedYear, selectedMonth) {
        selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Streaks",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMonthPicker = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthYearText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = "▼",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(habits) { habit ->
                StreakCard(
                    habit = habit,
                    viewModel = viewModel,
                    selectedDate = selectedDate
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    if (showMonthPicker) {
        MonthYearPickerDialog(
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            onDismiss = { showMonthPicker = false },
            onConfirm = { year, month ->
                selectedYear = year
                selectedMonth = month
                showMonthPicker = false
            }
        )
    }
}

@Composable
fun StreakCard(
    habit: Habit,
    viewModel: HabitViewModel,
    selectedDate: LocalDate
) {
    val firstDayOfMonth = selectedDate.withDayOfMonth(1)
    val daysInMonth = selectedDate.lengthOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    
    var completions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var monthlyStreakCount by remember { mutableStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(habit.id, selectedDate) {
        val startDate = viewModel.getDateString(firstDayOfMonth)
        val endDate = viewModel.getDateString(firstDayOfMonth.plusMonths(1).minusDays(1))
        val comps = viewModel.getCompletionsForWeek(habit.id, startDate, endDate)
        completions = comps.map { it.date }.toSet()
        
        // Calculate monthly streak count (number of consecutive days completed in this month)
        val sortedDates = comps.map { LocalDate.parse(it.date) }.sorted()
        var currentStreakCount = 0
        var lastDate: LocalDate? = null
        
        sortedDates.forEach { date ->
            if (lastDate == null || date == lastDate!!.plusDays(1)) {
                currentStreakCount++
            } else {
                currentStreakCount = 1
            }
            lastDate = date
        }
        
        monthlyStreakCount = currentStreakCount
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { isExpanded = !isExpanded },
                            onLongPress = { showDeleteDialog = true }
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = monthlyStreakCount.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5DADE2)
                        )
                        Text(
                            text = "Streak",
                            fontSize = 12.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                    
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show day headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7F8C8D),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show calendar grid
                val weeks = (daysInMonth + startDayOfWeek + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (week in 0 until weeks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (dayOfWeek in 0..6) {
                                val dayNumber = week * 7 + dayOfWeek - startDayOfWeek + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val date = firstDayOfMonth.withDayOfMonth(dayNumber)
                                    val dateStr = viewModel.getDateString(date)
                                    val isCompleted = completions.contains(dateStr)
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCompleted) Color(0xFF5DADE2) else Color(0xFFDDDDDD)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontSize = 12.sp,
                                            color = if (isCompleted) Color.White else Color(0xFF2C3E50),
                                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show summary
                val completedDays = completions.size
                Text(
                    text = "$completedDays day${if (completedDays != 1) "s" else ""} completed this month",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        DeleteHabitDialog(
            habitName = habit.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteHabit(habit)
                showDeleteDialog = false
            }
        )
    }
}

@Composable
fun DeleteHabitDialog(
    habitName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Habit",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$habitName\"? This will remove all completion history for this habit.",
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE74C3C)
                )
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month & Year") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Text("◀", fontSize = 20.sp)
                    }
                    Text(
                        text = selectedYear.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Text("▶", fontSize = 20.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(months.size) { index ->
                        val month = index + 1
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (month == selectedMonth) Color(0xFF5DADE2) else Color.Transparent
                                )
                                .clickable { selectedMonth = month }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = months[index],
                                fontSize = 16.sp,
                                fontWeight = if (month == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                color = if (month == selectedMonth) Color.White else Color(0xFF2C3E50)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WeekDaysHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
            Text(
                text = day,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7F8C8D),
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun DailyHabitCard(habit: Habit, viewModel: HabitViewModel) {
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val weekDays = remember {
        (0..6).map { today.minusDays((6 - it).toLong()) }
    }
    
    var completions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var refreshKey by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(habit.id, refreshKey) {
        val startDate = viewModel.getDateString(weekDays.first())
        val endDate = viewModel.getDateString(weekDays.last())
        val comps = viewModel.getCompletionsForWeek(habit.id, startDate, endDate)
        completions = comps.map { it.date }.toSet()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            showDeleteDialog = true
                        }
                    )
                }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C3E50),
                modifier = Modifier.weight(1f)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekDays.forEach { date ->
                    val dateStr = viewModel.getDateString(date)
                    val isCompleted = completions.contains(dateStr)
                    val isToday = date == today
                    val dayLetter = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).first().toString()
                    
                    DayCircle(
                        dayLetter = dayLetter,
                        isCompleted = isCompleted,
                        isToday = isToday
                    ) {
                        // Only allow marking TODAY as complete, and only if not already completed
                        if (isToday && !isCompleted) {
                            scope.launch {
                                viewModel.toggleCompletion(habit.id, dateStr)
                                refreshKey++
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        DeleteHabitDialog(
            habitName = habit.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteHabit(habit)
                showDeleteDialog = false
            }
        )
    }
}

@Composable
fun DayCircle(dayLetter: String, isCompleted: Boolean, isToday: Boolean = false, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = dayLetter,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF5F6C6D)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Color(0xFF5DADE2) // Blue for completed
                        isToday -> Color(0xFF95E1D3) // Light green for today (clickable)
                        else -> Color(0xFFDDDDDD) // Gray for past/future (locked)
                    }
                )
                .clickable(
                    enabled = isToday && !isCompleted, // Only today can be clicked if not completed
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Show checkmark for completed days
            if (isCompleted) {
                Text(
                    text = "✓",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentView: String, onViewChange: (String) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Daily",
                tint = if (currentView == "daily") Color(0xFF2C3E50) else Color(0xFF7F8C8D),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onViewChange("daily") }
            )
            
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Monthly",
                tint = if (currentView == "monthly") Color(0xFF2C3E50) else Color(0xFF7F8C8D),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onViewChange("monthly") }
            )
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C3E50))
                    .clickable { onViewChange("add") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Streaks",
                tint = if (currentView == "streaks") Color(0xFF2C3E50) else Color(0xFF7F8C8D),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onViewChange("streaks") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitView(
    onCancel: () -> Unit,
    onSave: (String, Int, Int) -> Unit,
    padding: PaddingValues
) {
    var habitName by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Add New Habit",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Habit Name",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    placeholder = { Text("Enter habit name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF5DADE2),
                        unfocusedBorderColor = Color(0xFFE8E8E8)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Daily Reminder",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEEEEE))
                        .clickable { showTimePicker = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notification Time",
                        fontSize = 14.sp,
                        color = Color(0xFF5F6C6D)
                    )
                    
                    Text(
                        text = String.format("%02d:%02d", selectedHour, selectedMinute),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDDDDDD)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Cancel", fontSize = 16.sp, color = Color(0xFF2C3E50), fontWeight = FontWeight.Medium)
            }
            
            Button(
                onClick = { 
                    if (habitName.isNotBlank()) {
                        onSave(habitName, selectedHour, selectedMinute)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C3E50)
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = habitName.isNotBlank()
            ) {
                Text("Save", fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Select Time",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            "Hour",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2C3E50)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier
                                .width(100.dp)
                                .height(150.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE))
                        ) {
                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(24) { hour ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (hour == selectedHour) Color(0xFF5DADE2) else Color.Transparent
                                            )
                                            .clickable { selectedHour = hour }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = String.format("%02d", hour),
                                            fontSize = 20.sp,
                                            fontWeight = if (hour == selectedHour) FontWeight.Bold else FontWeight.Normal,
                                            color = if (hour == selectedHour) Color.White else Color(0xFF2C3E50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Minute Picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            "Minute",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2C3E50)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier
                                .width(100.dp)
                                .height(150.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE))
                        ) {
                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(60) { minute ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (minute == selectedMinute) Color(0xFF5DADE2) else Color.Transparent
                                            )
                                            .clickable { selectedMinute = minute }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = String.format("%02d", minute),
                                            fontSize = 20.sp,
                                            fontWeight = if (minute == selectedMinute) FontWeight.Bold else FontWeight.Normal,
                                            color = if (minute == selectedMinute) Color.White else Color(0xFF2C3E50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Selected: ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedHour, selectedMinute) }
            ) {
                Text("OK", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 16.sp)
            }
        }
    )
}
