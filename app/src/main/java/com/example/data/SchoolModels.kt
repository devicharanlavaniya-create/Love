package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    ABSENT("Absent"),
    LATE("Late"),
    EXCUSED("Excused");

    companion object {
        fun fromRaw(raw: String): AttendanceStatus =
            entries.firstOrNull { it.name == raw } ?: PRESENT
    }
}

enum class StudentStatus(val label: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    GRADUATED("Graduated");

    companion object {
        fun fromRaw(raw: String): StudentStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class FacultyStatus(val label: String) {
    ACTIVE("Active"),
    ON_LEAVE("On Leave");

    companion object {
        fun fromRaw(raw: String): FacultyStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class FacilityCategory(val label: String) {
    LAB("Lab"),
    CLASSROOM("Classroom"),
    LIBRARY("Library"),
    SPORTS("Sports"),
    TRANSPORT("Transport"),
    HALL("Hall");

    companion object {
        fun fromRaw(raw: String): FacilityCategory =
            entries.firstOrNull { it.name == raw } ?: CLASSROOM
    }
}

enum class FacilityStatus(val label: String) {
    AVAILABLE("Available"),
    OCCUPIED("Occupied"),
    MAINTENANCE("Maintenance");

    companion object {
        fun fromRaw(raw: String): FacilityStatus =
            entries.firstOrNull { it.name == raw } ?: AVAILABLE
    }
}

enum class FeeStatus(val label: String) {
    PENDING("Pending"),
    PARTIAL("Partial"),
    PAID("Paid"),
    OVERDUE("Overdue");

    companion object {
        fun fromRaw(raw: String): FeeStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class NoticeCategory(val label: String) {
    GENERAL("General"),
    EXAM("Exam"),
    EVENT("Event"),
    URGENT("Urgent");

    companion object {
        fun fromRaw(raw: String): NoticeCategory =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

@Keep
@IgnoreExtraProperties
data class SchoolProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val schoolName: String = "",
    val academicYear: String = "",
    val principalName: String = "",
    val motto: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SchoolClass(
    @DocumentId val id: String = "",
    val name: String = "",
    val gradeLevel: String = "",
    val roomNumber: String = "",
    val homeroomTeacher: String = "",
    val scheduleSummary: String = "",
    val capacity: Long = 30L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Student(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val rollNumber: String = "",
    val classId: String = "",
    val className: String = "",
    val guardianName: String = "",
    val guardianPhone: String = "",
    val guardianEmail: String = "",
    val status: String = StudentStatus.ACTIVE.name,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rollNumber: String = "",
    val classId: String = "",
    val className: String = "",
    val dateKey: String = "",
    val status: String = AttendanceStatus.PRESENT.name,
    val remarks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FacultyMember(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val department: String = "",
    val subject: String = "",
    val assignedClass: String = "",
    val email: String = "",
    val phone: String = "",
    val status: String = FacultyStatus.ACTIVE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CampusFacility(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = FacilityCategory.CLASSROOM.name,
    val building: String = "",
    val capacity: Long = 30L,
    val status: String = FacilityStatus.AVAILABLE.name,
    val bookedBy: String = "",
    val scheduleSlot: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FeeInvoice(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val className: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueDateKey: String = "",
    val status: String = FeeStatus.PENDING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CampusNotice(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = NoticeCategory.GENERAL.name,
    val audience: String = "All Students & Staff",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AssessmentType(val label: String) {
    EXAM("Exam"),
    QUIZ("Quiz"),
    LAB("Lab"),
    ASSIGNMENT("Assignment"),
    PROJECT("Project");

    companion object {
        fun fromRaw(raw: String): AssessmentType =
            entries.firstOrNull { it.name == raw } ?: EXAM
    }
}

@Keep
@IgnoreExtraProperties
data class ExamGrade(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val className: String = "",
    val subject: String = "",
    val examName: String = "",
    val score: Double = 0.0,
    val maxScore: Double = 100.0,
    val gradeLetter: String = "A",
    val term: String = "Term 2",
    val assessmentType: String = AssessmentType.EXAM.name,
    val teacherNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SubjectGradeSummary(
    val subject: String,
    val assessments: List<ExamGrade>,
    val totalScore: Double,
    val totalMaxScore: Double,
    val averagePercentage: Double,
    val overallLetter: String,
    val highestPercentage: Double,
    val latestAssessmentName: String
)

data class StudentAcademicReport(
    val student: Student,
    val subjectSummaries: List<SubjectGradeSummary>,
    val overallPercentage: Double,
    val overallLetter: String,
    val gpa4Scale: Double,
    val totalAssessments: Int
)

enum class CheckInSession(val label: String) {
    MORNING_HOMEROOM("Morning Homeroom"),
    MIDDAY_CLASSES("Midday Classes"),
    AFTERNOON_LAB("Afternoon Lab"),
    FULL_DAY("Full School Day")
}

@Keep
@IgnoreExtraProperties
data class StudentDailyCheckIn(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rollNumber: String = "",
    val classId: String = "",
    val className: String = "",
    val dateKey: String = "",
    val status: String = AttendanceStatus.PRESENT.name,
    val sessionName: String = CheckInSession.MORNING_HOMEROOM.label,
    val checkInTimeLabel: String = "",
    val locationNote: String = "",
    val remarks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class ScheduleDay(val label: String, val shortLabel: String) {
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri"),
    SATURDAY("Saturday", "Sat");

    companion object {
        fun fromRaw(raw: String): ScheduleDay =
            entries.firstOrNull { it.name == raw } ?: MONDAY

        fun today(): ScheduleDay = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> MONDAY
            Calendar.TUESDAY -> TUESDAY
            Calendar.WEDNESDAY -> WEDNESDAY
            Calendar.THURSDAY -> THURSDAY
            Calendar.FRIDAY -> FRIDAY
            Calendar.SATURDAY -> SATURDAY
            else -> MONDAY
        }
    }
}

@Keep
@IgnoreExtraProperties
data class ClassSchedulePeriod(
    @DocumentId val id: String = "",
    val classId: String = "",
    val className: String = "",
    val dayOfWeek: String = ScheduleDay.MONDAY.name,
    val periodNumber: Long = 1L,
    val subject: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val facultyId: String = "",
    val facultyName: String = "",
    val roomNumber: String = "",
    val topicNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object DateKeyUtils {
    private val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.US)

    fun todayKey(): String = keyFormatter.format(Date())

    fun currentTimeLabel(): String = timeFormatter.format(Date())

    fun shiftDateKey(dateKey: String, deltaDays: Int): String {
        val parsed = runCatching { keyFormatter.parse(dateKey) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }
        return keyFormatter.format(cal.time)
    }

    fun formatMillisToKey(millis: Long): String = keyFormatter.format(Date(millis))

    fun formatKeyForDisplay(dateKey: String): String {
        val today = todayKey()
        if (dateKey == today) return "Today · ${displayFormatter.format(Date())}"
        val parsed = runCatching { keyFormatter.parse(dateKey) }.getOrNull() ?: return dateKey
        return displayFormatter.format(parsed)
    }

    fun computeGradeLetter(score: Double, maxScore: Double): String {
        if (maxScore <= 0.0) return "N/A"
        val pct = (score / maxScore) * 100.0
        return when {
            pct >= 93 -> "A+"
            pct >= 85 -> "A"
            pct >= 78 -> "B+"
            pct >= 70 -> "B"
            pct >= 62 -> "C+"
            pct >= 52 -> "C"
            pct >= 40 -> "D"
            else -> "F"
        }
    }

    fun computeGpa4Scale(percentage: Double): Double = when {
        percentage >= 93.0 -> 4.0
        percentage >= 85.0 -> 3.8
        percentage >= 78.0 -> 3.4
        percentage >= 70.0 -> 3.0
        percentage >= 62.0 -> 2.5
        percentage >= 52.0 -> 2.0
        percentage >= 40.0 -> 1.2
        else -> 0.0
    }

    fun buildStudentAcademicReport(
        student: Student,
        grades: List<ExamGrade>,
        termFilter: String? = null
    ): StudentAcademicReport {
        val studentGrades = grades.filter { grade ->
            grade.studentId == student.id &&
                (termFilter.isNullOrBlank() || grade.term.ifBlank { "Term 2" } == termFilter)
        }
        val subjectSummaries = studentGrades
            .groupBy { it.subject.trim().ifEmpty { "General" } }
            .entries
            .map { (subject, items) ->
                val sortedItems = items.sortedByDescending { it.createdAt?.seconds ?: 0L }
                val totalScore = sortedItems.sumOf { it.score }
                val totalMax = sortedItems.sumOf { it.maxScore.coerceAtLeast(1.0) }
                val avgPct = if (totalMax > 0.0) (totalScore / totalMax) * 100.0 else 0.0
                val highestPct = sortedItems.maxOfOrNull {
                    if (it.maxScore > 0.0) (it.score / it.maxScore) * 100.0 else 0.0
                } ?: 0.0
                SubjectGradeSummary(
                    subject = subject,
                    assessments = sortedItems,
                    totalScore = totalScore,
                    totalMaxScore = totalMax,
                    averagePercentage = avgPct,
                    overallLetter = computeGradeLetter(totalScore, totalMax),
                    highestPercentage = highestPct,
                    latestAssessmentName = sortedItems.firstOrNull()?.examName.orEmpty()
                )
            }
            .sortedByDescending { it.averagePercentage }

        val overallPct = if (subjectSummaries.isEmpty()) {
            0.0
        } else {
            subjectSummaries.map { it.averagePercentage }.average()
        }
        return StudentAcademicReport(
            student = student,
            subjectSummaries = subjectSummaries,
            overallPercentage = overallPct,
            overallLetter = if (subjectSummaries.isEmpty()) "N/A" else computeGradeLetter(overallPct, 100.0),
            gpa4Scale = if (subjectSummaries.isEmpty()) 0.0 else computeGpa4Scale(overallPct),
            totalAssessments = studentGrades.size
        )
    }
}
