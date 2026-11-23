package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.AssessmentType
import com.example.data.DateKeyUtils
import com.example.data.ExamGrade
import com.example.data.Student
import com.example.data.SubjectGradeSummary
import com.example.ui.components.EmptyCampusCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCampusStatusColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradesContent(
    students: List<Student>,
    grades: List<ExamGrade>,
    selectedStudentId: String?,
    onSelectStudentId: (String) -> Unit,
    onAddStudent: () -> Unit,
    onAddGrade: () -> Unit,
    onDeleteGrade: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalCampusStatusColors.current

    if (students.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            EmptyCampusCard(
                icon = Icons.Filled.Groups,
                title = "No Enrolled Students",
                description = "Enroll at least one student first so academic grades can be tracked by subject.",
                primaryActionLabel = "Enroll Student",
                onPrimaryAction = onAddStudent
            )
        }
        return
    }

    val currentStudent = remember(students, selectedStudentId) {
        students.firstOrNull { it.id == selectedStudentId } ?: students.first()
    }

    LaunchedEffect(students, selectedStudentId) {
        if (selectedStudentId == null || students.none { it.id == selectedStudentId }) {
            onSelectStudentId(students.first().id)
        }
    }

    var studentMenuExpanded by remember { mutableStateOf(false) }
    var selectedTerm by remember(currentStudent.id) { mutableStateOf<String?>(null) }

    val availableTerms = remember(grades, currentStudent.id) {
        grades.filter { it.studentId == currentStudent.id }
            .map { it.term.ifBlank { "Term 2" } }
            .distinct()
            .sorted()
    }

    val report = remember(currentStudent, grades, selectedTerm) {
        DateKeyUtils.buildStudentAcademicReport(
            student = currentStudent,
            grades = grades,
            termFilter = selectedTerm
        )
    }

    val expandedSubjects = remember(currentStudent.id, selectedTerm) {
        mutableStateMapOf<String, Boolean>().apply {
            report.subjectSummaries.firstOrNull()?.let { put(it.subject, true) }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ExposedDropdownMenuBox(
                expanded = studentMenuExpanded,
                onExpandedChange = { studentMenuExpanded = !studentMenuExpanded }
            ) {
                OutlinedTextField(
                    value = "${currentStudent.fullName} · ${currentStudent.rollNumber}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Student Academic Report") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("student_grades_selector")
                )
                ExposedDropdownMenu(
                    expanded = studentMenuExpanded,
                    onDismissRequest = { studentMenuExpanded = false }
                ) {
                    students.forEach { stu ->
                        val count = grades.count { it.studentId == stu.id }
                        DropdownMenuItem(
                            text = { Text("${stu.fullName} (${stu.rollNumber} · $count grades)") },
                            onClick = {
                                onSelectStudentId(stu.id)
                                studentMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (availableTerms.size > 1) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("grades_term_filter")
                ) {
                    item {
                        FilterChip(
                            selected = selectedTerm == null,
                            onClick = { selectedTerm = null },
                            label = { Text("All Terms") }
                        )
                    }
                    items(availableTerms, key = { it }) { term ->
                        FilterChip(
                            selected = selectedTerm == term,
                            onClick = { selectedTerm = term },
                            label = { Text(term) }
                        )
                    }
                }
            }
        }

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
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StudentAvatar(
                                fullName = currentStudent.fullName,
                                accentColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentStudent.fullName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${currentStudent.rollNumber} · ${currentStudent.className}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                                )
                            }
                            StatusPill(
                                label = if (report.totalAssessments == 0) "No Scores" else "Overall ${report.overallLetter}",
                                textColor = if (report.overallPercentage >= 75.0 || report.totalAssessments == 0) {
                                    statusColors.present
                                } else {
                                    statusColors.late
                                },
                                backgroundColor = if (report.overallPercentage >= 75.0 || report.totalAssessments == 0) {
                                    statusColors.presentContainer
                                } else {
                                    statusColors.lateContainer
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportMetricTile(
                                label = "Average",
                                value = if (report.totalAssessments == 0) "—" else String.format(Locale.US, "%.1f%%", report.overallPercentage),
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetricTile(
                                label = "GPA (4.0)",
                                value = if (report.totalAssessments == 0) "—" else String.format(Locale.US, "%.2f", report.gpa4Scale),
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetricTile(
                                label = "Subjects",
                                value = "${report.subjectSummaries.size} (${report.totalAssessments})",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (report.subjectSummaries.isEmpty()) {
            item {
                EmptyCampusCard(
                    icon = Icons.Filled.Grade,
                    title = "No Subject Grades Recorded",
                    description = "No exam, quiz, lab, or assignment scores are recorded yet for ${currentStudent.fullName}.",
                    primaryActionLabel = "Record Subject Grade",
                    onPrimaryAction = onAddGrade,
                    modifier = Modifier.testTag("student_grades_empty_card")
                )
            }
        } else {
            item {
                SectionHeader(
                    title = "Grades by Subject",
                    subtitle = "${report.subjectSummaries.size} subjects · Tap a subject for individual assessments",
                    actionLabel = "Add Grade",
                    onActionClick = onAddGrade
                )
            }

            items(report.subjectSummaries, key = { it.subject }) { summary ->
                val isExpanded = expandedSubjects[summary.subject] ?: false
                SubjectGradeCard(
                    summary = summary,
                    expanded = isExpanded,
                    onToggleExpand = {
                        expandedSubjects[summary.subject] = !isExpanded
                    },
                    onDeleteGrade = onDeleteGrade
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun ReportMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SubjectGradeCard(
    summary: SubjectGradeSummary,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteGrade: (String) -> Unit
) {
    val statusColors = LocalCampusStatusColors.current
    val isStrong = summary.averagePercentage >= 80.0
    val isWarning = summary.averagePercentage < 65.0
    val accentFg = when {
        isStrong -> statusColors.present
        isWarning -> statusColors.absent
        else -> statusColors.late
    }
    val accentBg = when {
        isStrong -> statusColors.presentContainer
        isWarning -> statusColors.absentContainer
        else -> statusColors.lateContainer
    }

    Card(
        onClick = onToggleExpand,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.testTag("subject_grade_card_${summary.subject}")
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${summary.assessments.size} ${if (summary.assessments.size == 1) "assessment" else "assessments"} · ${summary.totalScore.toInt()}/${summary.totalMaxScore.toInt()} pts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    StatusPill(
                        label = summary.overallLetter,
                        textColor = accentFg,
                        backgroundColor = accentBg
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f%%", summary.averagePercentage),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse ${summary.subject} grades" else "Expand ${summary.subject} grades",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { (summary.averagePercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = accentFg,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                    summary.assessments.forEach { assessment ->
                        val type = AssessmentType.fromRaw(assessment.assessmentType)
                        val itemPct = if (assessment.maxScore > 0.0) {
                            (assessment.score / assessment.maxScore) * 100.0
                        } else 0.0
                        val (itemFg, itemBg) = when {
                            itemPct >= 80.0 -> statusColors.present to statusColors.presentContainer
                            itemPct < 65.0 -> statusColors.absent to statusColors.absentContainer
                            else -> statusColors.late to statusColors.lateContainer
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = assessment.examName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(
                                            label = type.label,
                                            textColor = MaterialTheme.colorScheme.primary,
                                            backgroundColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                        Text(
                                            text = assessment.term.ifBlank { "Term 2" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (assessment.teacherNotes.isNotBlank()) {
                                        Text(
                                            text = assessment.teacherNotes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    StatusPill(
                                        label = assessment.gradeLetter,
                                        textColor = itemFg,
                                        backgroundColor = itemBg
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${assessment.score.toInt()}/${assessment.maxScore.toInt()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteGrade(assessment.id) },
                                    modifier = Modifier.testTag("delete_grade_${assessment.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete ${assessment.examName} grade"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
