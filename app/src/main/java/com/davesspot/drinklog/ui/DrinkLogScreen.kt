package com.davesspot.drinklog.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.davesspot.drinklog.data.ConsumptionLog
import com.davesspot.drinklog.data.DrinkType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DrinkLogScreen(viewModel: DrinkViewModel = viewModel(factory = DrinkViewModel.Factory)) {
    var expandedSection by remember { mutableIntStateOf(0) }
    val sections = listOf("Log Drink", "Create Drink", "View Log")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        sections.forEachIndexed { index, title ->
            AccordionSection(
                title = title,
                expanded = expandedSection == index,
                onHeaderClick = { expandedSection = index }
            ) {
                when (index) {
                    0 -> LogDrinkSection(viewModel)
                    1 -> CreateDrinkSection(
                        onSave = { name, units ->
                            viewModel.addDrinkType(name, units)
                            expandedSection = 0 // Auto-collapse and go to Log
                        },
                        onCancel = {
                            // Optionally just collapse or clear form (handled inside)
                        }
                    )
                    2 -> ViewLogSection(viewModel)
                }
            }
        }
    }
}

@Composable
fun AccordionSection(
    title: String,
    expanded: Boolean,
    onHeaderClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun LogDrinkSection(viewModel: DrinkViewModel) {
    val drinkTypesState by viewModel.drinkTypes.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // Use a list state that is stable across recompositions for smooth dragging
    val localDrinkTypes = remember { mutableStateListOf<DrinkType>() }
    
    var draggedItemId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(drinkTypesState) {
        // Only sync from DB if we aren't currently dragging
        if (draggedItemId == null) {
            localDrinkTypes.clear()
            localDrinkTypes.addAll(drinkTypesState)
        }
    }

    if (localDrinkTypes.isEmpty()) {
        Text("No drinks created yet. Go to 'Create Drink' to get started.")
    } else {
        val lazyListState = rememberLazyListState()

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            itemsIndexed(localDrinkTypes, key = { _, item -> item.id }) { index, drink ->
                val isDragging = draggedItemId == drink.id
                val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "")
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .zIndex(if (isDragging) 1f else 0f)
                        .shadow(elevation)
                        .offset {
                            if (isDragging) {
                                IntOffset(0, dragOffset.roundToInt())
                            } else {
                                IntOffset.Zero
                            }
                        }
                        .clickable { 
                            if (draggedItemId == null) {
                                viewModel.logDrink(drink.standardUnits)
                                Toast.makeText(context, "Drink Logged", Toast.LENGTH_SHORT).show()
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = "Reorder",
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .pointerInput(drink.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { 
                                            draggedItemId = drink.id 
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            viewModel.updateDrinkOrder(localDrinkTypes.toList())
                                            draggedItemId = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedItemId = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y
                                            
                                            val currentIndex = localDrinkTypes.indexOfFirst { it.id == draggedItemId }
                                            if (currentIndex != -1) {
                                                // threshold for swapping: roughly the height of an item
                                                val thresholdPx = with(density) { 56.dp.toPx() } 
                                                
                                                if (dragOffset > thresholdPx && currentIndex < localDrinkTypes.size - 1) {
                                                    localDrinkTypes.add(currentIndex + 1, localDrinkTypes.removeAt(currentIndex))
                                                    dragOffset -= thresholdPx
                                                } else if (dragOffset < -thresholdPx && currentIndex > 0) {
                                                    localDrinkTypes.add(currentIndex - 1, localDrinkTypes.removeAt(currentIndex))
                                                    dragOffset += thresholdPx
                                                }
                                            }
                                        }
                                    )
                                }
                        )

                        Text(
                            text = "${drink.name} - ${"%.1f".format(drink.standardUnits)} Units",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.deleteDrinkType(drink) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Saved Drink",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

data class Ingredient(
    val sizeOz: String = "",
    val percent: String = ""
)

@Composable
fun CreateDrinkSection(onSave: (String, Double) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf(listOf(Ingredient())) }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Drink Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        ingredients.forEachIndexed { index, ingredient ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ingredient.sizeOz,
                    onValueChange = { v -> ingredients = ingredients.toMutableList().also { it[index] = ingredient.copy(sizeOz = v) } },
                    label = { Text("Oz") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ingredient.percent,
                    onValueChange = { v -> ingredients = ingredients.toMutableList().also { it[index] = ingredient.copy(percent = v) } },
                    label = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                if (ingredients.size > 1) {
                    IconButton(onClick = { ingredients = ingredients.toMutableList().also { it.removeAt(index) } }) {
                        Icon(Icons.Filled.Delete, "Remove")
                    }
                }
            }
        }

        Button(onClick = { ingredients = ingredients + Ingredient() }) {
            Icon(Icons.Filled.Add, null)
            Text("Add Ingredient")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                name = ""
                ingredients = listOf(Ingredient())
                onCancel()
            }) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                // Calculate
                val totalUnits = ingredients.sumOf { 
                    val size = it.sizeOz.toDoubleOrNull() ?: 0.0
                    val pct = it.percent.toDoubleOrNull() ?: 0.0
                    // Units = (Oz * (Pct/100)) / 0.6
                    (size * (pct / 100.0)) / 0.6
                }
                
                val finalName = name.ifBlank { "Drink (${"%.1f".format(totalUnits)} U)" }
                onSave(finalName, totalUnits)
                
                // Reset
                name = ""
                ingredients = listOf(Ingredient())
            }) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewLogSection(viewModel: DrinkViewModel) {
    val allLogs by viewModel.allLogs.collectAsState()
    val drinkTypes by viewModel.drinkTypes.collectAsState()
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    
    var logToEdit by remember { mutableStateOf<ConsumptionLog?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddDrinkMenu by remember { mutableStateOf(false) }

    // Summary Stats
    val today = LocalDate.now()
    val weekFields = WeekFields.of(Locale.getDefault())
    
    val logsThisYear = allLogs.filter { 
        val date = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        date.year == today.year
    }
    val logsThisMonth = logsThisYear.filter {
        val date = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        date.month == today.month
    }
    val logsThisWeek = logsThisMonth.filter {
        val date = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        date.get(weekFields.weekOfWeekBasedYear()) == today.get(weekFields.weekOfWeekBasedYear())
    } 

    val sumYear = logsThisYear.sumOf { it.standardUnits }
    val sumMonth = logsThisMonth.sumOf { it.standardUnits }
    val sumWeek = logsThisWeek.sumOf { it.standardUnits }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Calendar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { yearMonth = yearMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Prev")
            }
            Text(yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { yearMonth = yearMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Day Indicator
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(40.dp)) // Offset for week totals
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar Grid
        val daysInMonth = yearMonth.lengthOfMonth()
        val startOffset = yearMonth.atDay(1).dayOfWeek.value % 7 

        val logsForViewMonth = allLogs.filter {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            YearMonth.from(date) == yearMonth
        }
        val logsByDay = logsForViewMonth.groupBy { 
            Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth 
        }

        Column {
            val totalCells = daysInMonth + startOffset
            val rows = (totalCells + 6) / 7
            
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Week Total
                    var weekTotal = 0.0
                    for (col in 0 until 7) {
                        val day = (row * 7 + col) - startOffset + 1
                        if (day in 1..daysInMonth) {
                            weekTotal += logsByDay[day]?.sumOf { it.standardUnits } ?: 0.0
                        }
                    }
                    
                    Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                        if (weekTotal > 0) {
                            Text(
                                text = "%.1f".format(weekTotal),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    for (col in 0 until 7) {
                        val day = (row * 7 + col) - startOffset + 1
                        if (day in 1..daysInMonth) {
                            val dayLogs = logsByDay[day] ?: emptyList()
                            val units = dayLogs.sumOf { it.standardUnits }
                            val color = when {
                                units == 0.0 -> Color.Transparent
                                units < 2.0 -> Color.Green
                                units <= 3.0 -> Color(0xFF90EE90) 
                                units <= 4.0 -> Color.Yellow
                                units <= 5.0 -> Color(0xFFFF9800) 
                                else -> Color.Red
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .background(color, shape = CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .clickable {
                                        selectedDate = yearMonth.atDay(day)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (units > 0.0) Color.Black else Color.Unspecified
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats
        Text("Summary", style = MaterialTheme.typography.titleMedium)
        Text("Current Week: ${"%.1f".format(sumWeek)} units")
        Text("Current Month: ${"%.1f".format(sumMonth)} units")
        Text("Current Year: ${"%.1f".format(sumYear)} units")
    }

    // Detail Dialog
    selectedDate?.let { date ->
        val selectedDateStr = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        val dayLogs = allLogs.filter { 
            Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() == date
        }

        AlertDialog(
            onDismissRequest = { 
                selectedDate = null 
                showAddDrinkMenu = false
            },
            title = {
                Text("Drinks on $selectedDateStr")
            },
            text = {
                Column {
                    if (showAddDrinkMenu) {
                        Text("Select Drink to Add:", style = MaterialTheme.typography.labelLarge)
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(items = drinkTypes) { drink ->
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        viewModel.logDrinkForDate(drink.standardUnits, date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                                        showAddDrinkMenu = false
                                    }
                                ) {
                                    Text("${drink.name} (${"%.1f".format(drink.standardUnits)} U)")
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    if (dayLogs.isEmpty() && !showAddDrinkMenu) {
                        Text("No drinks logged for this day.")
                    } else if (dayLogs.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(items = dayLogs) { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${"%.1f".format(log.standardUnits)} Units", modifier = Modifier.weight(1f))
                                    
                                    IconButton(onClick = { 
                                        logToEdit = log
                                        showDatePicker = true
                                    }) {
                                        Icon(Icons.Filled.EditCalendar, "Change Date")
                                    }
                                    
                                    IconButton(onClick = { 
                                        viewModel.deleteLog(log)
                                    }) {
                                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showAddDrinkMenu = !showAddDrinkMenu }) {
                        Text("Add")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { 
                        selectedDate = null 
                        showAddDrinkMenu = false
                    }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showDatePicker && logToEdit != null) {
        // Fix for off-by-one: Convert local midnight to UTC midnight for the DatePicker
        val localDate = Instant.ofEpochMilli(logToEdit!!.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val utcMillis = localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { newMillis ->
                        viewModel.updateLogTimestamp(logToEdit!!, newMillis)
                        // Close both
                        showDatePicker = false
                        selectedDate = null
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
