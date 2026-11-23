package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AttendanceStatus
import com.example.data.DateKeyUtils
import com.example.data.FacilityStatus
import com.example.data.FeeStatus
import com.example.data.NoticeCategory
import com.example.data.StudentStatus
import com.example.ui.components.EmptyCampusCard
import com.example.ui.components.KpiStatCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCampusStatusColors
import com.example.ui.viewmodel.SchoolDashboardData
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OverviewScreen(
    data: SchoolDashboardData,
    isBusy: Boolean,
    onNavigateAttendance: () -> Unit,
    onNavigateStudentCheckIn: () -> Unit,
    onNavigateSchedule: () -> Unit,
    onNavigateGrades: () -> Unit,
    onNavigateAcademics: () -> Unit,
    onNavigateCampus: () -> Unit,
    onEditProfile: () -> Unit,
    onQuickAddClass: () -> Unit,
    onQuickAddStudent: () -> Unit,
    onSeedSampleCampus: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalCampusStatusColors.current
    val todayKey = DateKeyUtils.todayKey()
    val activeStudents = data.students.filter { it.status == StudentStatus.ACTIVE.name }
    val todayRecords = data.attendance.filter { it.dateKey == todayKey }
    val presentToday = todayRecords.count {
        it.status == AttendanceStatus.PRESENT.name || it.status == AttendanceStatus.LATE.name
    }
    val attendancePct = if (activeStudents.isEmpty()) 0 else ((presentToday * 100f) / activeStudents.size).toInt()

    val availableFacilities = data.facilities.count { it.status == FacilityStatus.AVAILABLE.name }
    val totalCollected = data.fees.sumOf { it.paidAmount }
    val totalBilled = data.fees.sumOf { it.amount }
    val currency = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_campus_hero),
                    contentDescription = "School campus courtyard banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x660B1A2B),
                                    Color(0xE60B1A2B)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onEditProfile,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("edit_school_profile_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit school profile")
                    }
                    IconButton(
                        onClick = onSignOut,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = data.profile?.academicYear?.ifBlank { "2026-2027" } ?: "2026-2027",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = data.profile?.schoolName?.ifBlank { "CampusPulse Academy" } ?: "CampusPulse Academy",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            val principal = data.profile?.principalName
                            if (!principal.isNullOrBlank()) append("Principal: $principal")
                            else append("Tap edit to set up school profile")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.86f)
                    )
                }
            }
        }

        if (data.classes.isEmpty() && data.students.isEmpty() && data.facilities.isEmpty()) {
            item {
                EmptyCampusCard(
                    icon = Icons.Filled.AutoAwesome,
                    title = "Welcome to CampusPulse",
                    description = "Start by adding your first academic class and students, or load a sample school campus with classes, attendance, labs, faculty, fees, and notices.",
                    primaryActionLabel = if (isBusy) "Loading..." else "Load Sample Campus",
                    onPrimaryAction = onSeedSampleCampus,
                    secondaryActionLabel = "Add First Class",
                    onSecondaryAction = onQuickAddClass,
                    modifier = Modifier.testTag("seed_sample_campus_card")
                )
            }
        }

        item {
            SectionHeader(
                title = "Today's Campus Pulse",
                subtitle = DateKeyUtils.formatKeyForDisplay(todayKey)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Attendance",
                        value = "$attendancePct%",
                        subtitle = "$presentToday/${activeStudents.size} present",
                        icon = Icons.Filled.FactCheck,
                        accentColor = statusColors.present,
                        containerColor = statusColors.presentContainer,
                        onClick = onNavigateAttendance,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_attendance_card")
                    )
                    KpiStatCard(
                        title = "Students",
                        value = "${activeStudents.size}",
                        subtitle = "${data.classes.size} classes",
                        icon = Icons.Filled.Groups,
                        accentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onClick = onNavigateAcademics,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_students_card")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Facilities",
                        value = "$availableFacilities/${data.facilities.size}",
                        subtitle = "ready for booking",
                        icon = Icons.Filled.Apartment,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = onNavigateCampus,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_facilities_card")
                    )
                    KpiStatCard(
                        title = "Fee Ledger",
                        value = currency.format(totalCollected),
                        subtitle = "of ${currency.format(totalBilled)} billed",
                        icon = Icons.Filled.AttachMoney,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = onNavigateCampus,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_fees_card")
                    )
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Button(
                        onClick = onNavigateAttendance,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("quick_take_attendance_button")
                    ) {
                        Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Take Attendance")
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = onNavigateStudentCheckIn,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("quick_student_checkin_button")
                    ) {
                        Icon(Icons.Filled.HowToReg, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Student Check-In")
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = onNavigateSchedule,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("quick_class_schedule_button")
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Class Schedule")
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = onNavigateGrades,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("quick_subject_grades_button")
                    ) {
                        Icon(Icons.Filled.Grade, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Subject Grades")
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = if (data.classes.isEmpty()) onQuickAddClass else onQuickAddStudent,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("quick_enroll_button")
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (data.classes.isEmpty()) "Add Class" else "Enroll Student")
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = onNavigateCampus,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("quick_facilities_button")
                    ) {
                        Icon(Icons.Filled.Apartment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Campus Facilities")
                    }
                }
            }
        }

        if (data.classes.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Class Roll Call Progress",
                    subtitle = "Today's attendance coverage by class",
                    actionLabel = "Mark All",
                    onActionClick = onNavigateAttendance
                )
            }
            items(data.classes, key = { it.id }) { schoolClass ->
                val classStudents = activeStudents.filter { it.classId == schoolClass.id }
                val classToday = todayRecords.filter { it.classId == schoolClass.id }
                val classPresent = classToday.count {
                    it.status == AttendanceStatus.PRESENT.name || it.status == AttendanceStatus.LATE.name
                }
                val progress = if (classStudents.isEmpty()) 0f else classPresent.toFloat() / classStudents.size
                Card(
                    onClick = onNavigateAttendance,
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
                                Text(
                                    text = schoolClass.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${schoolClass.roomNumber} · ${schoolClass.homeroomTeacher.ifBlank { "Unassigned" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$classPresent/${classStudents.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = statusColors.present,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        if (data.notices.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Campus Circulars",
                    subtitle = "${data.notices.size} announcements",
                    actionLabel = "View All",
                    onActionClick = onNavigateCampus
                )
            }
            items(data.notices.take(3), key = { it.id }) { notice ->
                val cat = NoticeCategory.fromRaw(notice.category)
                val (pillFg, pillBg) = when (cat) {
                    NoticeCategory.URGENT -> statusColors.absent to statusColors.absentContainer
                    NoticeCategory.EXAM -> statusColors.late to statusColors.lateContainer
                    NoticeCategory.EVENT -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondaryContainer
                    NoticeCategory.GENERAL -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(label = cat.label, textColor = pillFg, backgroundColor = pillBg)
                                if (notice.pinned) {
                                    Icon(
                                        Icons.Filled.PushPin,
                                        contentDescription = "Pinned notice",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = notice.audience,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = notice.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = notice.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (data.faculty.isNotEmpty()) {
            item {
                Card(
                    onClick = onNavigateCampus,
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Faculty & Campus Operations",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${data.faculty.size} faculty members · ${data.fees.count { it.status != FeeStatus.PAID.name }} pending fee invoices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
