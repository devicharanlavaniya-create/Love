package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.SchoolClass
import com.example.data.Student
import com.example.data.StudentStatus
import com.example.ui.components.EmptyCampusCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCampusStatusColors
import com.example.ui.viewmodel.SchoolDashboardData
import java.text.NumberFormat
import java.util.Locale

enum class AcademicsTab(val label: String) {
    STUDENTS("Students"),
    CLASSES("Classes"),
    GRADES("Grades")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicsScreen(
    data: SchoolDashboardData,
    activeTab: AcademicsTab,
    onSelectTab: (AcademicsTab) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedClassId: String?,
    onSelectClassId: (String?) -> Unit,
    onAddClass: () -> Unit,
    onDeleteClass: (String) -> Unit,
    onViewClassSchedule: (String) -> Unit,
    onAddStudent: () -> Unit,
    onEditStudent: (Student) -> Unit,
    onDeleteStudent: (String) -> Unit,
    selectedGradeStudentId: String?,
    onSelectGradeStudentId: (String) -> Unit,
    onAddGrade: () -> Unit,
    onDeleteGrade: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalCampusStatusColors.current
    var inspectedStudent by remember { mutableStateOf<Student?>(null) }

    val filteredStudents = remember(data.students, searchQuery, selectedClassId) {
        data.students.filter { student ->
            val matchesClass = selectedClassId == null || student.classId == selectedClassId
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                student.fullName.lowercase().contains(q) ||
                student.rollNumber.lowercase().contains(q) ||
                student.className.lowercase().contains(q)
            matchesClass && matchesQuery
        }
    }

    if (inspectedStudent != null) {
        BackHandler { inspectedStudent = null }
        val student = data.students.firstOrNull { it.id == inspectedStudent!!.id } ?: inspectedStudent!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val studentAttendance = data.attendance.filter { it.studentId == student.id }
            .sortedByDescending { it.dateKey }
        val presentDays = studentAttendance.count {
            it.status == AttendanceStatus.PRESENT.name || it.status == AttendanceStatus.LATE.name
        }
        val attRate = if (studentAttendance.isEmpty()) 0 else ((presentDays * 100f) / studentAttendance.size).toInt()
        val studentGrades = data.grades.filter { it.studentId == student.id }
        val studentFees = data.fees.filter { it.studentId == student.id }
        val currency = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }

        ModalBottomSheet(
            onDismissRequest = { inspectedStudent = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudentAvatar(
                        fullName = student.fullName,
                        accentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(student.fullName, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "${student.rollNumber} · ${student.className}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        label = "$attRate% Att.",
                        textColor = if (attRate >= 80 || studentAttendance.isEmpty()) statusColors.present else statusColors.absent,
                        backgroundColor = if (attRate >= 80 || studentAttendance.isEmpty()) statusColors.presentContainer else statusColors.absentContainer
                    )
                }

                if (student.guardianName.isNotBlank() || student.guardianPhone.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = "Guardian: ${student.guardianName.ifBlank { "Not specified" }}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (student.guardianPhone.isNotBlank()) {
                                    Text(
                                        text = student.guardianPhone,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text("Recent Attendance (${studentAttendance.size} logs)", style = MaterialTheme.typography.titleSmall)
                if (studentAttendance.isEmpty()) {
                    Text(
                        "No attendance logs recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    studentAttendance.take(4).forEach { rec ->
                        val st = AttendanceStatus.fromRaw(rec.status)
                        val (fg, bg) = when (st) {
                            AttendanceStatus.PRESENT -> statusColors.present to statusColors.presentContainer
                            AttendanceStatus.ABSENT -> statusColors.absent to statusColors.absentContainer
                            AttendanceStatus.LATE -> statusColors.late to statusColors.lateContainer
                            AttendanceStatus.EXCUSED -> statusColors.excused to statusColors.excusedContainer
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                rec.dateKey,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                            StatusPill(st.label, fg, bg)
                        }
                    }
                }

                if (studentGrades.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exam Grades (${studentGrades.size})", style = MaterialTheme.typography.titleSmall)
                        androidx.compose.material3.TextButton(
                            onClick = {
                                val targetId = student.id
                                inspectedStudent = null
                                onSelectGradeStudentId(targetId)
                                onSelectTab(AcademicsTab.GRADES)
                            }
                        ) {
                            Text("View by Subject")
                        }
                    }
                    studentGrades.take(3).forEach { gr ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${gr.subject} · ${gr.examName}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${gr.score.toInt()}/${gr.maxScore.toInt()} (${gr.gradeLetter})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (studentFees.isNotEmpty()) {
                    val balance = studentFees.sumOf { it.amount - it.paidAmount }
                    Text(
                        text = "Fee Balance Remaining: ${currency.format(balance)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (balance > 0) statusColors.absent else statusColors.present
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            inspectedStudent = null
                            onDeleteStudent(student.id)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Remove")
                    }
                    Button(
                        onClick = {
                            inspectedStudent = null
                            onEditStudent(student)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Edit Profile")
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                .testTag("academics_segmented_tabs")
        ) {
            AcademicsTab.entries.forEachIndexed { idx, tab ->
                SegmentedButton(
                    selected = activeTab == tab,
                    onClick = { onSelectTab(tab) },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = AcademicsTab.entries.size)
                ) {
                    Text(tab.label)
                }
            }
        }

        if (activeTab == AcademicsTab.GRADES) {
            StudentGradesContent(
                students = data.students,
                grades = data.grades,
                selectedStudentId = selectedGradeStudentId,
                onSelectStudentId = onSelectGradeStudentId,
                onAddStudent = onAddStudent,
                onAddGrade = onAddGrade,
                onDeleteGrade = onDeleteGrade,
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
                when (activeTab) {
                    AcademicsTab.STUDENTS -> {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search by student name or roll #") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_search_input")
                    )
                }
                if (data.classes.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedClassId == null,
                                    onClick = { onSelectClassId(null) },
                                    label = { Text("All (${data.students.size})") }
                                )
                            }
                            items(data.classes, key = { it.id }) { cls ->
                                val count = data.students.count { it.classId == cls.id }
                                FilterChip(
                                    selected = selectedClassId == cls.id,
                                    onClick = { onSelectClassId(cls.id) },
                                    label = { Text("${cls.name} ($count)") }
                                )
                            }
                        }
                    }
                }
                if (filteredStudents.isEmpty()) {
                    item {
                        EmptyCampusCard(
                            icon = Icons.Filled.Groups,
                            title = if (data.classes.isEmpty()) "Create a Class First" else "No Students Enrolled",
                            description = if (data.classes.isEmpty()) {
                                "Add at least one academic class before enrolling students."
                            } else {
                                "Enroll students with roll numbers and guardian contacts."
                            },
                            primaryActionLabel = if (data.classes.isEmpty()) "Add Class" else "Enroll Student",
                            onPrimaryAction = if (data.classes.isEmpty()) onAddClass else onAddStudent
                        )
                    }
                } else {
                    items(filteredStudents, key = { it.id }) { student ->
                        val history = data.attendance.filter { it.studentId == student.id }
                        val present = history.count {
                            it.status == AttendanceStatus.PRESENT.name || it.status == AttendanceStatus.LATE.name
                        }
                        val pct = if (history.isEmpty()) null else ((present * 100f) / history.size).toInt()
                        val stStatus = StudentStatus.fromRaw(student.status)
                        Card(
                            onClick = { inspectedStudent = student },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.testTag("student_card_${student.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
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
                                }
                                if (pct != null) {
                                    StatusPill(
                                        label = "$pct%",
                                        textColor = if (pct >= 80) statusColors.present else statusColors.absent,
                                        backgroundColor = if (pct >= 80) statusColors.presentContainer else statusColors.absentContainer
                                    )
                                } else {
                                    StatusPill(
                                        label = stStatus.label,
                                        textColor = MaterialTheme.colorScheme.primary,
                                        backgroundColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AcademicsTab.CLASSES -> {
                if (data.classes.isEmpty()) {
                    item {
                        EmptyCampusCard(
                            icon = Icons.Filled.Class,
                            title = "No Academic Classes Yet",
                            description = "Create homeroom classes and grade sections with room numbers and capacity.",
                            primaryActionLabel = "Add Class",
                            onPrimaryAction = onAddClass
                        )
                    }
                } else {
                    items(data.classes, key = { it.id }) { schoolClass ->
                        val enrolledCount = data.students.count { it.classId == schoolClass.id }
                        val cap = schoolClass.capacity.coerceAtLeast(1L)
                        val scheduledCount = data.schedules.count { it.classId == schoolClass.id }
                        Card(
                            onClick = { onViewClassSchedule(schoolClass.id) },
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
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(schoolClass.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${schoolClass.gradeLevel} · ${schoolClass.roomNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteClass(schoolClass.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete class ${schoolClass.name}")
                                    }
                                }
                                if (schoolClass.homeroomTeacher.isNotBlank() || schoolClass.scheduleSummary.isNotBlank()) {
                                    Text(
                                        text = listOf(schoolClass.homeroomTeacher, schoolClass.scheduleSummary)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Enrollment", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "$enrolledCount / $cap seats · $scheduledCount periods",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (enrolledCount.toFloat() / cap).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                )
                            }
                        }
                    }
                }
            }

                AcademicsTab.GRADES -> Unit
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
        }
    }
}
