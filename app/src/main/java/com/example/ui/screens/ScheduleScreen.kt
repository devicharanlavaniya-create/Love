package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ClassSchedulePeriod
import com.example.data.FacultyMember
import com.example.data.FacultyStatus
import com.example.data.ScheduleDay
import com.example.ui.components.EmptyCampusCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCampusStatusColors
import com.example.ui.viewmodel.SchoolDashboardData

@Composable
fun ScheduleScreen(
    data: SchoolDashboardData,
    selectedDay: ScheduleDay,
    onSelectDay: (ScheduleDay) -> Unit,
    selectedClassId: String?,
    onSelectClassId: (String?) -> Unit,
    onAddPeriod: () -> Unit,
    onEditPeriod: (ClassSchedulePeriod) -> Unit,
    onDeletePeriod: (String) -> Unit,
    onAddClass: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { ScheduleDay.today() }
    var searchQuery by remember { mutableStateOf("") }

    val facultyById = remember(data.faculty) {
        data.faculty.associateBy { it.id }
    }
    val facultyByName = remember(data.faculty) {
        data.faculty.associateBy { it.fullName.trim().lowercase() }
    }

    val classFilteredAllDays = remember(data.schedules, selectedClassId) {
        data.schedules.filter { selectedClassId == null || it.classId == selectedClassId }
    }

    val dayPeriods = remember(classFilteredAllDays, selectedDay, searchQuery) {
        val q = searchQuery.trim().lowercase()
        classFilteredAllDays
            .filter { period ->
                period.dayOfWeek == selectedDay.name && (
                    q.isEmpty() ||
                        period.subject.lowercase().contains(q) ||
                        period.facultyName.lowercase().contains(q) ||
                        period.roomNumber.lowercase().contains(q) ||
                        period.className.lowercase().contains(q)
                    )
            }
            .sortedWith(
                compareBy<ClassSchedulePeriod> { it.periodNumber }
                    .thenBy { it.startTime }
                    .thenBy { it.className }
            )
    }

    val selectedClass = data.classes.firstOrNull { it.id == selectedClassId }
    val uniqueFacultyCount = remember(dayPeriods) {
        dayPeriods.map { it.facultyName.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().size
    }
    val timeSpanLabel = remember(dayPeriods) {
        val firstStart = dayPeriods.firstOrNull()?.startTime.orEmpty()
        val lastEnd = dayPeriods.maxByOrNull { it.endTime }?.endTime.orEmpty()
        if (firstStart.isNotBlank() && lastEnd.isNotBlank()) "$firstStart – $lastEnd" else "No timings"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${selectedDay.label} Class Schedule",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (selectedDay == today) {
                                StatusPill(
                                    label = "Today",
                                    textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            }
                        }

                        Text(
                            text = selectedClass?.let { "${it.name} · ${it.roomNumber}" }
                                ?: "Viewing all ${data.classes.size} classes · Tap a class below to filter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScheduleMetricChip(
                                icon = Icons.Filled.MenuBook,
                                label = "${dayPeriods.size} ${if (dayPeriods.size == 1) "Period" else "Periods"}"
                            )
                            ScheduleMetricChip(
                                icon = Icons.Filled.AccessTime,
                                label = timeSpanLabel,
                                mono = true
                            )
                            ScheduleMetricChip(
                                icon = Icons.Filled.Person,
                                label = "$uniqueFacultyCount Faculty"
                            )
                        }
                    }
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("schedule_day_selector")
            ) {
                items(ScheduleDay.entries, key = { it.name }) { day ->
                    val isSelected = day == selectedDay
                    val isToday = day == today
                    val countForDay = classFilteredAllDays.count { it.dayOfWeek == day.name }
                    Surface(
                        onClick = { onSelectDay(day) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 60.dp, minHeight = 48.dp)
                            .testTag("schedule_day_${day.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = day.shortLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = when {
                                    isToday -> "Today · $countForDay"
                                    else -> "$countForDay cls"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }

        if (data.classes.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("schedule_class_filter")
                ) {
                    item {
                        FilterChip(
                            selected = selectedClassId == null,
                            onClick = { onSelectClassId(null) },
                            label = { Text("All Classes") }
                        )
                    }
                    items(data.classes, key = { it.id }) { schoolClass ->
                        FilterChip(
                            selected = selectedClassId == schoolClass.id,
                            onClick = { onSelectClassId(schoolClass.id) },
                            label = { Text(schoolClass.name) }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter by subject, faculty, or room") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_search_input")
            )
        }

        if (data.classes.isEmpty()) {
            item {
                EmptyCampusCard(
                    icon = Icons.Filled.Class,
                    title = "No Classes Configured",
                    description = "Create an academic class first so you can build its daily subject timetable and assign faculty.",
                    primaryActionLabel = "Add Class",
                    onPrimaryAction = onAddClass
                )
            }
        } else if (dayPeriods.isEmpty()) {
            item {
                EmptyCampusCard(
                    icon = Icons.Filled.CalendarToday,
                    title = "No Classes Scheduled for ${selectedDay.label}",
                    description = if (searchQuery.isNotBlank()) {
                        "No periods match \"$searchQuery\". Try clearing the filter or adding a new period."
                    } else {
                        "Add periods with subjects, start/end timings, and faculty members to build the ${selectedDay.label} timetable."
                    },
                    primaryActionLabel = "Add Class Period",
                    onPrimaryAction = onAddPeriod,
                    modifier = Modifier.testTag("schedule_empty_card")
                )
            }
        } else {
            item {
                SectionHeader(
                    title = "Daily Timetable",
                    subtitle = "${dayPeriods.size} scheduled sessions for ${selectedDay.label}",
                    actionLabel = "Add Period",
                    onActionClick = onAddPeriod
                )
            }
            items(dayPeriods, key = { it.id }) { period ->
                val matchedFaculty = facultyById[period.facultyId]
                    ?: facultyByName[period.facultyName.trim().lowercase()]
                SchedulePeriodCard(
                    period = period,
                    faculty = matchedFaculty,
                    showClassBadge = selectedClassId == null,
                    onClick = { onEditPeriod(period) },
                    onDelete = { onDeletePeriod(period.id) }
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun ScheduleMetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    mono: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = if (mono) JetBrainsMonoFontFamily else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SchedulePeriodCard(
    period: ClassSchedulePeriod,
    faculty: FacultyMember?,
    showClassBadge: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColors = LocalCampusStatusColors.current
    val isFacultyOnLeave = faculty?.status == FacultyStatus.ON_LEAVE.name

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.testTag("schedule_period_card_${period.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left timing rail
            Column(
                modifier = Modifier.width(68.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "P${period.periodNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = period.startTime,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = period.endTime,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Vertical timeline bar
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            // Right subject, room, and faculty details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = period.subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showClassBadge) {
                            Text(
                                text = period.className,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_schedule_${period.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove ${period.subject} from schedule"
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = period.facultyName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isFacultyOnLeave) {
                        StatusPill(
                            label = "On Leave",
                            textColor = statusColors.late,
                            backgroundColor = statusColors.lateContainer
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (period.roomNumber.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Room,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = period.roomNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (faculty != null && faculty.department.isNotBlank()) {
                        StatusPill(
                            label = faculty.department,
                            textColor = MaterialTheme.colorScheme.secondary,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                }

                if (period.topicNotes.isNotBlank()) {
                    Text(
                        text = period.topicNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
