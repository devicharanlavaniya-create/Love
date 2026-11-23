package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AssessmentType
import com.example.data.AttendanceStatus
import com.example.data.CampusFacility
import com.example.data.ClassSchedulePeriod
import com.example.data.DateKeyUtils
import com.example.data.FacilityCategory
import com.example.data.FacilityStatus
import com.example.data.FacultyMember
import com.example.data.FeeInvoice
import com.example.data.NoticeCategory
import com.example.data.ScheduleDay
import com.example.data.SchoolClass
import com.example.data.SchoolProfile
import com.example.data.Student
import com.example.data.StudentStatus

@Composable
fun SchoolProfileDialog(
    initial: SchoolProfile?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var schoolName by remember { mutableStateOf(initial?.schoolName ?: "") }
    var academicYear by remember { mutableStateOf(initial?.academicYear?.ifBlank { "2026-2027" } ?: "2026-2027") }
    var principalName by remember { mutableStateOf(initial?.principalName ?: "") }
    var motto by remember { mutableStateOf(initial?.motto ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("School Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it; error = null },
                    label = { Text("School Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_school_name")
                )
                OutlinedTextField(
                    value = academicYear,
                    onValueChange = { academicYear = it; error = null },
                    label = { Text("Academic Year *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = principalName,
                    onValueChange = { principalName = it; error = null },
                    label = { Text("Principal / Admin *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motto,
                    onValueChange = { motto = it },
                    label = { Text("Motto (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (schoolName.isBlank() || academicYear.isBlank() || principalName.isBlank()) {
                    error = "Please fill in all required fields."
                } else {
                    onSave(schoolName, academicYear, principalName, motto)
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddClassDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("Mon-Fri 08:15 - 14:30") }
    var capacityText by remember { mutableStateOf("32") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Academic Class") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Class Name (e.g. Grade 10 - A) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_class_name")
                )
                OutlinedTextField(
                    value = gradeLevel,
                    onValueChange = { gradeLevel = it; error = null },
                    label = { Text("Grade Level (e.g. Grade 10) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it; error = null },
                    label = { Text("Room Number (e.g. Room 204) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Homeroom Teacher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("Schedule Summary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter(Char::isDigit) },
                    label = { Text("Seat Capacity (1-200) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cap = capacityText.toIntOrNull() ?: 0
                if (name.isBlank() || gradeLevel.isBlank() || roomNumber.isBlank() || cap !in 1..200) {
                    error = "Enter valid name, grade, room, and capacity (1-200)."
                } else {
                    onSave(name, gradeLevel, roomNumber, teacher, schedule, cap)
                    onDismiss()
                }
            }) { Text("Create Class") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentDialog(
    initial: Student?,
    classes: List<SchoolClass>,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, SchoolClass, String, String, String, StudentStatus, String) -> Unit
) {
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var rollNumber by remember { mutableStateOf(initial?.rollNumber ?: "") }
    var selectedClass by remember {
        mutableStateOf(classes.firstOrNull { it.id == initial?.classId } ?: classes.firstOrNull())
    }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var guardianName by remember { mutableStateOf(initial?.guardianName ?: "") }
    var guardianPhone by remember { mutableStateOf(initial?.guardianPhone ?: "") }
    var guardianEmail by remember { mutableStateOf(initial?.guardianEmail ?: "") }
    var status by remember { mutableStateOf(StudentStatus.fromRaw(initial?.status ?: StudentStatus.ACTIVE.name)) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Enroll Student" else "Edit Student") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; error = null },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_student_name")
                )
                OutlinedTextField(
                    value = rollNumber,
                    onValueChange = { rollNumber = it; error = null },
                    label = { Text("Roll / Admission # *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = classDropdownExpanded,
                    onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClass?.name ?: "Select Class",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        classes.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls.name) },
                                onClick = {
                                    selectedClass = cls
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    label = { Text("Guardian Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = guardianPhone,
                    onValueChange = { guardianPhone = it },
                    label = { Text("Guardian Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = guardianEmail,
                    onValueChange = { guardianEmail = it },
                    label = { Text("Guardian Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudentStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cls = selectedClass
                if (fullName.isBlank() || rollNumber.isBlank() || cls == null) {
                    error = "Student name, roll number, and class are required."
                } else {
                    onSave(initial?.id, fullName, rollNumber, cls, guardianName, guardianPhone, guardianEmail, status, notes)
                    onDismiss()
                }
            }) { Text(if (initial == null) "Enroll" else "Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceRemarkDialog(
    student: Student,
    initialStatus: AttendanceStatus,
    initialRemarks: String,
    onDismiss: () -> Unit,
    onSave: (AttendanceStatus, String) -> Unit
) {
    var status by remember { mutableStateOf(initialStatus) }
    var remarks by remember { mutableStateOf(initialRemarks) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attendance Note · ${student.fullName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AttendanceStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (e.g. Medical note, Bus delayed)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(status, remarks)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddFacultyDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var assignedClass by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Faculty Member") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it; error = null }, label = { Text("Full Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = department, onValueChange = { department = it; error = null }, label = { Text("Department *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subject, onValueChange = { subject = it; error = null }, label = { Text("Subject / Role *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = assignedClass, onValueChange = { assignedClass = it }, label = { Text("Assigned Class") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (fullName.isBlank() || department.isBlank() || subject.isBlank()) {
                    error = "Name, department, and subject are required."
                } else {
                    onSave(fullName, department, subject, assignedClass, email, phone)
                    onDismiss()
                }
            }) { Text("Add Faculty") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFacilityDialog(
    onDismiss: () -> Unit,
    onSave: (String, FacilityCategory, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(FacilityCategory.LAB) }
    var building by remember { mutableStateOf("") }
    var capacityText by remember { mutableStateOf("40") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Campus Facility") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("Facility Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FacilityCategory.entries.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                    }
                }
                OutlinedTextField(value = building, onValueChange = { building = it; error = null }, label = { Text("Building / Location *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter(Char::isDigit) },
                    label = { Text("Capacity (1-2000) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Equipment / Notes") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cap = capacityText.toIntOrNull() ?: 0
                if (name.isBlank() || building.isBlank() || cap !in 1..2000) {
                    error = "Enter facility name, location, and capacity (1-2000)."
                } else {
                    onSave(name, category, building, cap, notes)
                    onDismiss()
                }
            }) { Text("Create Facility") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookFacilityDialog(
    facility: CampusFacility,
    onDismiss: () -> Unit,
    onSave: (FacilityStatus, String, String) -> Unit
) {
    var status by remember { mutableStateOf(FacilityStatus.fromRaw(facility.status)) }
    var bookedBy by remember { mutableStateOf(facility.bookedBy) }
    var scheduleSlot by remember { mutableStateOf(facility.scheduleSlot) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage · ${facility.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FacilityStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = {
                                status = st
                                if (st == FacilityStatus.AVAILABLE) {
                                    bookedBy = ""
                                    scheduleSlot = ""
                                }
                            },
                            label = { Text(st.label) }
                        )
                    }
                }
                if (status != FacilityStatus.AVAILABLE) {
                    OutlinedTextField(
                        value = bookedBy,
                        onValueChange = { bookedBy = it },
                        label = { Text(if (status == FacilityStatus.MAINTENANCE) "Assigned Crew / Reason" else "Booked By (Class or Faculty)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = scheduleSlot,
                        onValueChange = { scheduleSlot = it },
                        label = { Text("Schedule Slot (e.g. 10:00 - 11:30)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(status, bookedBy, scheduleSlot)
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeeInvoiceDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onSave: (Student, String, Double, String) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("Term Tuition Fee") }
    var amountText by remember { mutableStateOf("1200") }
    var dueDateKey by remember { mutableStateOf(DateKeyUtils.shiftDateKey(DateKeyUtils.todayKey(), 14)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Fee Invoice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedStudent?.fullName ?: "Select Student",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Student *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        students.forEach { stu ->
                            DropdownMenuItem(
                                text = { Text("${stu.fullName} (${stu.rollNumber})") },
                                onClick = { selectedStudent = stu; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it; error = null }, label = { Text("Fee Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (USD) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDateKey,
                    onValueChange = { dueDateKey = it; error = null },
                    label = { Text("Due Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val stu = selectedStudent
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (stu == null || title.isBlank() || amt <= 0.0 || !dueDateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
                    error = "Select student, positive amount, and YYYY-MM-DD due date."
                } else {
                    onSave(stu, title, amt, dueDateKey)
                    onDismiss()
                }
            }) { Text("Issue Invoice") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RecordFeePaymentDialog(
    invoice: FeeInvoice,
    onDismiss: () -> Unit,
    onPay: (Double) -> Unit
) {
    val remaining = (invoice.amount - invoice.paidAmount).coerceAtLeast(0.0)
    var paymentText by remember { mutableStateOf(remaining.toInt().toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment · ${invoice.studentName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remaining balance: $${String.format("%.2f", remaining)}", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = paymentText,
                    onValueChange = { paymentText = it; error = null },
                    label = { Text("Payment Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = paymentText.toDoubleOrNull() ?: 0.0
                if (amt <= 0.0 || amt > remaining + 0.01) {
                    error = "Enter an amount between 0 and $remaining."
                } else {
                    onPay(amt)
                    onDismiss()
                }
            }) { Text("Confirm Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNoticeDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, NoticeCategory, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(NoticeCategory.GENERAL) }
    var audience by remember { mutableStateOf("All Students & Staff") }
    var pinned by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Campus Notice") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it; error = null }, label = { Text("Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NoticeCategory.entries.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                    }
                }
                OutlinedTextField(value = audience, onValueChange = { audience = it }, label = { Text("Audience *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it; error = null }, label = { Text("Announcement Message *") }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                    Text("Pin to Campus Overview", style = MaterialTheme.typography.bodyMedium)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank() || body.isBlank() || audience.isBlank()) {
                    error = "Title, audience, and message are required."
                } else {
                    onSave(title, body, category, audience, pinned)
                    onDismiss()
                }
            }) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExamGradeDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onSave: (Student, String, String, Double, Double, String, AssessmentType, String) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var examName by remember { mutableStateOf("Midterm Exam") }
    var scoreText by remember { mutableStateOf("90") }
    var maxScoreText by remember { mutableStateOf("100") }
    var term by remember { mutableStateOf("Term 2") }
    var assessmentType by remember { mutableStateOf(AssessmentType.EXAM) }
    var teacherNotes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Subject Grade") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedStudent?.fullName ?: "Select Student",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Student *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        students.forEach { stu ->
                            DropdownMenuItem(
                                text = { Text("${stu.fullName} (${stu.rollNumber})") },
                                onClick = { selectedStudent = stu; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; error = null },
                    label = { Text("Subject *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_grade_subject")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssessmentType.entries.forEach { type ->
                        FilterChip(
                            selected = assessmentType == type,
                            onClick = { assessmentType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = examName,
                    onValueChange = { examName = it; error = null },
                    label = { Text("Assessment Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text("Academic Term") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = { scoreText = it },
                        label = { Text("Score *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxScoreText,
                        onValueChange = { maxScoreText = it },
                        label = { Text("Max Score *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = teacherNotes,
                    onValueChange = { teacherNotes = it },
                    label = { Text("Teacher Remarks (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val stu = selectedStudent
                val score = scoreText.toDoubleOrNull()
                val max = maxScoreText.toDoubleOrNull()
                if (stu == null || subject.isBlank() || examName.isBlank() || score == null || max == null || max <= 0.0 || score !in 0.0..max) {
                    error = "Provide valid student, subject, exam, and score <= max."
                } else {
                    onSave(stu, subject, examName, score, max, term, assessmentType, teacherNotes)
                    onDismiss()
                }
            }) { Text("Save Grade") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SchedulePeriodDialog(
    initial: ClassSchedulePeriod?,
    classes: List<SchoolClass>,
    faculty: List<FacultyMember>,
    defaultDay: ScheduleDay,
    defaultClassId: String?,
    onDismiss: () -> Unit,
    onSave: (String?, SchoolClass, ScheduleDay, Int, String, String, String, String, String, String, String) -> Unit
) {
    val initialClass = classes.firstOrNull { it.id == (initial?.classId ?: defaultClassId) } ?: classes.firstOrNull()
    var selectedClass by remember { mutableStateOf(initialClass) }
    var classMenuExpanded by remember { mutableStateOf(false) }

    var selectedDay by remember {
        mutableStateOf(initial?.let { ScheduleDay.fromRaw(it.dayOfWeek) } ?: defaultDay)
    }
    var periodText by remember { mutableStateOf((initial?.periodNumber ?: 1L).toString()) }
    var subject by remember { mutableStateOf(initial?.subject ?: "") }
    var startTime by remember { mutableStateOf(initial?.startTime ?: "08:15") }
    var endTime by remember { mutableStateOf(initial?.endTime ?: "09:10") }

    var facultyId by remember { mutableStateOf(initial?.facultyId ?: faculty.firstOrNull()?.id.orEmpty()) }
    var facultyName by remember {
        mutableStateOf(initial?.facultyName ?: faculty.firstOrNull()?.fullName.orEmpty())
    }
    var facultyMenuExpanded by remember { mutableStateOf(false) }

    var roomNumber by remember {
        mutableStateOf(initial?.roomNumber ?: initialClass?.roomNumber.orEmpty())
    }
    var topicNotes by remember { mutableStateOf(initial?.topicNotes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val timePresets = remember {
        listOf(
            Triple(1, "08:15", "09:10"),
            Triple(2, "09:20", "10:15"),
            Triple(3, "10:35", "11:30"),
            Triple(4, "11:40", "12:35"),
            Triple(5, "13:30", "14:25")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Schedule Class Period" else "Edit Schedule Period") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = classMenuExpanded && initial == null,
                    onExpandedChange = { if (initial == null) classMenuExpanded = !classMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClass?.name ?: "Select Class",
                        onValueChange = {},
                        readOnly = true,
                        enabled = initial == null,
                        label = { Text("Class *") },
                        trailingIcon = {
                            if (initial == null) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenuExpanded)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = classMenuExpanded && initial == null,
                        onDismissRequest = { classMenuExpanded = false }
                    ) {
                        classes.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls.name) },
                                onClick = {
                                    selectedClass = cls
                                    if (roomNumber.isBlank()) roomNumber = cls.roomNumber
                                    classMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScheduleDay.entries.forEach { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(day.shortLabel) }
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    timePresets.forEach { (num, s, e) ->
                        FilterChip(
                            selected = periodText == num.toString() && startTime == s && endTime == e,
                            onClick = {
                                periodText = num.toString()
                                startTime = s
                                endTime = e
                            },
                            label = { Text("P$num · $s") }
                        )
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; error = null },
                    label = { Text("Subject *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_schedule_subject")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = periodText,
                        onValueChange = { periodText = it.filter(Char::isDigit); error = null },
                        label = { Text("Period # *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it; error = null },
                        label = { Text("Start *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it; error = null },
                        label = { Text("End *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (faculty.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = facultyMenuExpanded,
                        onExpandedChange = { facultyMenuExpanded = !facultyMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = faculty.firstOrNull { it.id == facultyId }?.fullName ?: "Select from Directory",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Faculty Directory Preset") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = facultyMenuExpanded,
                            onDismissRequest = { facultyMenuExpanded = false }
                        ) {
                            faculty.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text("${member.fullName} · ${member.subject}") },
                                    onClick = {
                                        facultyId = member.id
                                        facultyName = member.fullName
                                        if (subject.isBlank()) subject = member.subject
                                        facultyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = facultyName,
                    onValueChange = {
                        facultyName = it
                        facultyId = ""
                        error = null
                    },
                    label = { Text("Faculty Member Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_schedule_faculty")
                )

                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room / Lab") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = topicNotes,
                    onValueChange = { topicNotes = it },
                    label = { Text("Topic / Lesson Notes (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cls = selectedClass
                    val periodNum = periodText.toIntOrNull() ?: 0
                    if (cls == null || periodNum !in 1..12 || subject.isBlank() ||
                        startTime.isBlank() || endTime.isBlank() || facultyName.isBlank()
                    ) {
                        error = "Select a class, period (1-12), subject, timings, and faculty name."
                    } else {
                        onSave(
                            initial?.id,
                            cls,
                            selectedDay,
                            periodNum,
                            subject,
                            startTime,
                            endTime,
                            facultyId,
                            facultyName,
                            roomNumber,
                            topicNotes
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_schedule_period_button")
            ) {
                Text(if (initial == null) "Save Period" else "Update Period")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

