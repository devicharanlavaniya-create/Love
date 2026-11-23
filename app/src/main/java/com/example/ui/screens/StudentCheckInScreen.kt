package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.CheckInSession
import com.example.data.DateKeyUtils
import com.example.data.Student
import com.example.data.StudentDailyCheckIn
import com.example.data.StudentStatus
import com.example.ui.components.EmptyCampusCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCampusStatusColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentCheckInContent(
    students: List<Student>,
    checkIns: List<StudentDailyCheckIn>,
    selectedDateKey: String,
    isBusy: Boolean,
    onSubmitCheckIn: (Student, AttendanceStatus, String, String, String) -> Unit,
    onNavigateToAcademics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalCampusStatusColors.current
    val activeStudents = remember(students) {
        students.filter { it.status == StudentStatus.ACTIVE.name }
    }

    var selectedStudentId by remember(activeStudents) {
        mutableStateOf(activeStudents.firstOrNull()?.id)
    }
    val selectedStudent = activeStudents.firstOrNull { it.id == selectedStudentId } ?: activeStudents.firstOrNull()

    var studentMenuExpanded by remember { mutableStateOf(false) }

    val existingCheckIn = remember(checkIns, selectedStudent?.id, selectedDateKey) {
        selectedStudent?.let { stu ->
            checkIns.firstOrNull { it.studentId == stu.id && it.dateKey == selectedDateKey }
        }
    }

    var selectedStatus by remember(selectedStudent?.id, selectedDateKey, existingCheckIn?.status) {
        mutableStateOf(
            existingCheckIn?.let { AttendanceStatus.fromRaw(it.status) } ?: AttendanceStatus.PRESENT
        )
    }
    var selectedSession by remember(selectedStudent?.id, selectedDateKey, existingCheckIn?.sessionName) {
        mutableStateOf(
            existingCheckIn?.sessionName?.ifBlank { CheckInSession.MORNING_HOMEROOM.label }
                ?: CheckInSession.MORNING_HOMEROOM.label
        )
    }
    var locationNote by remember(selectedStudent?.id, selectedDateKey, existingCheckIn?.locationNote) {
        mutableStateOf(existingCheckIn?.locationNote ?: "Main Campus")
    }
    var remarks by remember(selectedStudent?.id, selectedDateKey, existingCheckIn?.remarks) {
        mutableStateOf(existingCheckIn?.remarks ?: "")
    }

    LaunchedEffect(activeStudents) {
        if (selectedStudentId == null && activeStudents.isNotEmpty()) {
            selectedStudentId = activeStudents.first().id
        }
    }

    if (activeStudents.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            EmptyCampusCard(
                icon = Icons.Filled.Person,
                title = "No Enrolled Students",
                description = "Enroll at least one active student in Academics so students can submit their daily attendance check-in.",
                primaryActionLabel = "Enroll Student",
                onPrimaryAction = onNavigateToAcademics
            )
        }
        return
    }

    val currentStudent = selectedStudent ?: return
    val studentHistory = remember(checkIns, currentStudent.id) {
        checkIns.filter { it.studentId == currentStudent.id }.sortedByDescending { it.dateKey }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Student Self Check-In",
                        style = MaterialTheme.typography.titleLarge
                    )
                    ExposedDropdownMenuBox(
                        expanded = studentMenuExpanded,
                        onExpandedChange = { studentMenuExpanded = !studentMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${currentStudent.fullName} · ${currentStudent.rollNumber}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Student") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("student_checkin_selector")
                        )
                        ExposedDropdownMenu(
                            expanded = studentMenuExpanded,
                            onDismissRequest = { studentMenuExpanded = false }
                        ) {
                            activeStudents.forEach { stu ->
                                DropdownMenuItem(
                                    text = { Text("${stu.fullName} (${stu.rollNumber} · ${stu.className})") },
                                    onClick = {
                                        selectedStudentId = stu.id
                                        studentMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudentAvatar(
                            fullName = currentStudent.fullName,
                            accentColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentStudent.fullName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${currentStudent.className} · ${DateKeyUtils.formatKeyForDisplay(selectedDateKey)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (existingCheckIn != null) {
                            val st = AttendanceStatus.fromRaw(existingCheckIn.status)
                            val (fg, bg) = when (st) {
                                AttendanceStatus.PRESENT -> statusColors.present to statusColors.presentContainer
                                AttendanceStatus.ABSENT -> statusColors.absent to statusColors.absentContainer
                                AttendanceStatus.LATE -> statusColors.late to statusColors.lateContainer
                                AttendanceStatus.EXCUSED -> statusColors.excused to statusColors.excusedContainer
                            }
                            StatusPill("Marked: ${st.label}", fg, bg)
                        } else {
                            StatusPill(
                                "Pending",
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                    }

                    if (existingCheckIn != null) {
                        Surface(
                            color = statusColors.presentContainer.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = statusColors.present,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Checked in at ${existingCheckIn.checkInTimeLabel} · ${existingCheckIn.sessionName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Mark Today's Status",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttendanceStatus.entries.forEach { status ->
                            val selected = selectedStatus == status
                            val (fg, bg) = when (status) {
                                AttendanceStatus.PRESENT -> statusColors.present to statusColors.presentContainer
                                AttendanceStatus.ABSENT -> statusColors.absent to statusColors.absentContainer
                                AttendanceStatus.LATE -> statusColors.late to statusColors.lateContainer
                                AttendanceStatus.EXCUSED -> statusColors.excused to statusColors.excusedContainer
                            }
                            Surface(
                                onClick = { selectedStatus = status },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) bg else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = if (selected) null else androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("self_checkin_status_${status.name}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                    Text(
                        text = "Session",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CheckInSession.entries.forEach { session ->
                            FilterChip(
                                selected = selectedSession == session.label,
                                onClick = { selectedSession = session.label },
                                label = { Text(session.label) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = locationNote,
                        onValueChange = { locationNote = it },
                        label = { Text("Campus Location / Room") },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_checkin_location_input")
                    )

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Optional Note (e.g., Shuttle arrival, Medical leave)") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_checkin_remarks_input")
                    )

                    Button(
                        onClick = {
                            onSubmitCheckIn(
                                currentStudent,
                                selectedStatus,
                                selectedSession,
                                locationNote,
                                remarks
                            )
                        },
                        enabled = !isBusy,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_student_checkin_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (existingCheckIn == null) "Mark My Daily Attendance" else "Update Today's Check-In",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Recent Self Check-Ins",
                subtitle = "${studentHistory.size} recorded entries for ${currentStudent.fullName}"
            )
        }

        if (studentHistory.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Text(
                        text = "No self check-ins logged yet for ${currentStudent.fullName}. Tap \"Mark My Daily Attendance\" above to record today's attendance in Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(studentHistory, key = { it.id }) { item ->
                val st = AttendanceStatus.fromRaw(item.status)
                val (fg, bg) = when (st) {
                    AttendanceStatus.PRESENT -> statusColors.present to statusColors.presentContainer
                    AttendanceStatus.ABSENT -> statusColors.absent to statusColors.absentContainer
                    AttendanceStatus.LATE -> statusColors.late to statusColors.lateContainer
                    AttendanceStatus.EXCUSED -> statusColors.excused to statusColors.excusedContainer
                }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.dateKey} · ${item.checkInTimeLabel}",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                            Text(
                                text = listOf(item.sessionName, item.locationNote)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.remarks.isNotBlank()) {
                                Text(
                                    text = item.remarks,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        StatusPill(st.label, fg, bg)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
