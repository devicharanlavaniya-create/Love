package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.DateKeyUtils
import com.example.data.Student
import com.example.data.StudentStatus
import com.example.ui.components.EmptyCampusCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCampusStatusColors
import com.example.ui.viewmodel.SchoolDashboardData

enum class AttendanceMode(val label: String) {
    ROLL_CALL("Roll Call"),
    SELF_CHECK_IN("Check-In"),
    ANALYTICS("Analytics")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    data: SchoolDashboardData,
    mode: AttendanceMode,
    onModeChange: (AttendanceMode) -> Unit,
    selectedDateKey: String,
    selectedClassId: String?,
    isBusy: Boolean,
    onShiftDate: (Int) -> Unit,
    onSelectDateKey: (String) -> Unit,
    onSelectClassId: (String?) -> Unit,
    onMarkAttendance: (Student, AttendanceStatus, String) -> Unit,
    onMarkAllPresent: (List<Student>, Set<String>) -> Unit,
    onOpenRemarkDialog: (Student, AttendanceStatus, String) -> Unit,
    onSubmitStudentCheckIn: (Student, AttendanceStatus, String, String, String) -> Unit,
    onNavigateToAcademics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalCampusStatusColors.current
    var showDatePicker by remember { mutableStateOf(false) }

    val activeStudents = remember(data.students, selectedClassId) {
        data.students.filter { student ->
            student.status == StudentStatus.ACTIVE.name &&
                (selectedClassId == null || student.classId == selectedClassId)
        }
    }

    val recordsForDate = remember(data.attendance, selectedDateKey) {
        data.attendance.filter { it.dateKey == selectedDateKey }.associateBy { it.studentId }
    }

    val presentCount = activeStudents.count { recordsForDate[it.id]?.status == AttendanceStatus.PRESENT.name }
    val absentCount = activeStudents.count { recordsForDate[it.id]?.status == AttendanceStatus.ABSENT.name }
    val lateCount = activeStudents.count { recordsForDate[it.id]?.status == AttendanceStatus.LATE.name }
    val excusedCount = activeStudents.count { recordsForDate[it.id]?.status == AttendanceStatus.EXCUSED.name }
    val unmarkedCount = activeStudents.size - (presentCount + absentCount + lateCount + excusedCount)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onSelectDateKey(DateKeyUtils.formatMillisToKey(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                .testTag("attendance_mode_segmented")
        ) {
            AttendanceMode.entries.forEachIndexed { index, entry ->
                val icon = when (entry) {
                    AttendanceMode.ROLL_CALL -> Icons.Filled.FactCheck
                    AttendanceMode.SELF_CHECK_IN -> Icons.Filled.HowToReg
                    AttendanceMode.ANALYTICS -> Icons.Filled.BarChart
                }
                SegmentedButton(
                    selected = mode == entry,
                    onClick = { onModeChange(entry) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = AttendanceMode.entries.size),
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (mode == AttendanceMode.SELF_CHECK_IN) {
            StudentCheckInContent(
                students = data.students,
                checkIns = data.studentCheckIns,
                selectedDateKey = selectedDateKey,
                isBusy = isBusy,
                onSubmitCheckIn = onSubmitStudentCheckIn,
                onNavigateToAcademics = onNavigateToAcademics,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (mode == AttendanceMode.ROLL_CALL) {
                    item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { onShiftDate(-1) },
                            modifier = Modifier.testTag("attendance_prev_day_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("attendance_date_picker_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = DateKeyUtils.formatKeyForDisplay(selectedDateKey),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = { onShiftDate(1) },
                            modifier = Modifier.testTag("attendance_next_day_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                        }
                    }
                }
            }
        }

        if (data.classes.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        if (activeStudents.isEmpty()) {
            item {
                EmptyCampusCard(
                    icon = Icons.Filled.Groups,
                    title = "No Active Students Found",
                    description = "Enroll students into classes in the Academics tab to start taking daily attendance.",
                    primaryActionLabel = "Go to Academics",
                    onPrimaryAction = onNavigateToAcademics
                )
            }
        } else if (mode == AttendanceMode.ROLL_CALL) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${activeStudents.size} Students in Roll Call",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "$unmarkedCount unmarked today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    onMarkAllPresent(activeStudents, recordsForDate.keys)
                                },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("mark_all_present_button")
                            ) {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("All Present")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusPill("$presentCount Present", statusColors.present, statusColors.presentContainer)
                            StatusPill("$absentCount Absent", statusColors.absent, statusColors.absentContainer)
                            StatusPill("$lateCount Late", statusColors.late, statusColors.lateContainer)
                            if (excusedCount > 0) {
                                StatusPill("$excusedCount Excused", statusColors.excused, statusColors.excusedContainer)
                            }
                        }
                    }
                }
            }

            items(activeStudents, key = { it.id }) { student ->
                val record = recordsForDate[student.id]
                val currentStatus = record?.let { AttendanceStatus.fromRaw(it.status) }
                AttendanceRollCallCard(
                    student = student,
                    currentStatus = currentStatus,
                    remarks = record?.remarks.orEmpty(),
                    onSelectStatus = { status ->
                        onMarkAttendance(student, status, record?.remarks.orEmpty())
                    },
                    onEditRemark = {
                        onOpenRemarkDialog(
                            student,
                            currentStatus ?: AttendanceStatus.PRESENT,
                            record?.remarks.orEmpty()
                        )
                    }
                )
            }
        } else {
            items(activeStudents, key = { it.id }) { student ->
                val studentHistory = data.attendance.filter { it.studentId == student.id }
                val totalDays = studentHistory.size
                val presentDays = studentHistory.count {
                    it.status == AttendanceStatus.PRESENT.name || it.status == AttendanceStatus.LATE.name
                }
                val absentDays = studentHistory.count { it.status == AttendanceStatus.ABSENT.name }
                val pct = if (totalDays == 0) 0 else ((presentDays * 100f) / totalDays).toInt()

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StudentAvatar(
                                fullName = student.fullName,
                                accentColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${student.rollNumber} · ${student.className}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = if (pct >= 80) statusColors.present else statusColors.absent
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (totalDays == 0) 0f else presentDays.toFloat() / totalDays },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = if (pct >= 80) statusColors.present else statusColors.late,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "$presentDays present/late · $absentDays absent across $totalDays recorded days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    }
    }
}

@Composable
private fun AttendanceRollCallCard(
    student: Student,
    currentStatus: AttendanceStatus?,
    remarks: String,
    onSelectStatus: (AttendanceStatus) -> Unit,
    onEditRemark: () -> Unit
) {
    val statusColors = LocalCampusStatusColors.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.testTag("attendance_card_${student.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudentAvatar(
                    fullName = student.fullName,
                    accentColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${student.rollNumber} · ${student.className}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (remarks.isNotBlank()) {
                        Text(
                            text = "Note: $remarks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onEditRemark,
                    modifier = Modifier.testTag("attendance_remark_${student.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = "Add attendance remark for ${student.fullName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceStatus.entries.forEach { status ->
                    val selected = currentStatus == status
                    val (fg, bg) = when (status) {
                        AttendanceStatus.PRESENT -> statusColors.present to statusColors.presentContainer
                        AttendanceStatus.ABSENT -> statusColors.absent to statusColors.absentContainer
                        AttendanceStatus.LATE -> statusColors.late to statusColors.lateContainer
                        AttendanceStatus.EXCUSED -> statusColors.excused to statusColors.excusedContainer
                    }
                    Surface(
                        onClick = { onSelectStatus(status) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) bg else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = if (selected) null else androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("att_${student.id}_${status.name}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = fg,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = status.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) fg else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
