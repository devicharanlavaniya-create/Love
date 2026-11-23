package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AssessmentType
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import com.example.data.CampusFacility
import com.example.data.CampusNotice
import com.example.data.ClassSchedulePeriod
import com.example.data.DateKeyUtils
import com.example.data.ExamGrade
import com.example.data.FacilityCategory
import com.example.data.FacilityStatus
import com.example.data.FacultyMember
import com.example.data.FeeInvoice
import com.example.data.NoticeCategory
import com.example.data.ScheduleDay
import com.example.data.SchoolClass
import com.example.data.SchoolProfile
import com.example.data.SchoolRepository
import com.example.data.Student
import com.example.data.StudentDailyCheckIn
import com.example.data.StudentStatus
import com.example.data.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SchoolDashboardData(
    val profile: SchoolProfile? = null,
    val classes: List<SchoolClass> = emptyList(),
    val students: List<Student> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val studentCheckIns: List<StudentDailyCheckIn> = emptyList(),
    val schedules: List<ClassSchedulePeriod> = emptyList(),
    val faculty: List<FacultyMember> = emptyList(),
    val facilities: List<CampusFacility> = emptyList(),
    val fees: List<FeeInvoice> = emptyList(),
    val notices: List<CampusNotice> = emptyList(),
    val grades: List<ExamGrade> = emptyList()
)

class SchoolViewModel(
    private val repository: SchoolRepository,
    val currentUserId: String
) : ViewModel() {

    private val _selectedDateKey = MutableStateFlow(DateKeyUtils.todayKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _selectedClassId = MutableStateFlow<String?>(null)
    val selectedClassId: StateFlow<String?> = _selectedClassId.asStateFlow()

    private val _studentSearchQuery = MutableStateFlow("")
    val studentSearchQuery: StateFlow<String> = _studentSearchQuery.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private fun <T> Flow<T>.toSafeUiState(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }

    private val partA: Flow<UiState<Triple<SchoolProfile?, List<SchoolClass>, List<Student>>>> = combine(
        repository.observeProfile().toSafeUiState("profile"),
        repository.observeClasses().toSafeUiState("classes"),
        repository.observeStudents().toSafeUiState("students")
    ) { prof, cls, stu ->
        when {
            prof is UiState.Error -> UiState.Error(prof.message)
            cls is UiState.Error -> UiState.Error(cls.message)
            stu is UiState.Error -> UiState.Error(stu.message)
            prof is UiState.Success && cls is UiState.Success && stu is UiState.Success ->
                UiState.Success(Triple(prof.data, cls.data, stu.data))
            else -> UiState.Loading
        }
    }

    private val partB: Flow<UiState<Triple<List<AttendanceRecord>, List<FacultyMember>, List<CampusFacility>>>> = combine(
        repository.observeAttendance().toSafeUiState("attendance"),
        repository.observeFaculty().toSafeUiState("faculty"),
        repository.observeFacilities().toSafeUiState("facilities")
    ) { att, fac, facil ->
        when {
            att is UiState.Error -> UiState.Error(att.message)
            fac is UiState.Error -> UiState.Error(fac.message)
            facil is UiState.Error -> UiState.Error(facil.message)
            att is UiState.Success && fac is UiState.Success && facil is UiState.Success ->
                UiState.Success(Triple(att.data, fac.data, facil.data))
            else -> UiState.Loading
        }
    }

    private val partC: Flow<UiState<Triple<List<FeeInvoice>, List<CampusNotice>, List<ExamGrade>>>> = combine(
        repository.observeFees().toSafeUiState("fees"),
        repository.observeNotices().toSafeUiState("notices"),
        repository.observeGrades().toSafeUiState("grades")
    ) { fees, not, grd ->
        when {
            fees is UiState.Error -> UiState.Error(fees.message)
            not is UiState.Error -> UiState.Error(not.message)
            grd is UiState.Error -> UiState.Error(grd.message)
            fees is UiState.Success && not is UiState.Success && grd is UiState.Success ->
                UiState.Success(Triple(fees.data, not.data, grd.data))
            else -> UiState.Loading
        }
    }

    private val checkInsFlow: Flow<UiState<List<StudentDailyCheckIn>>> =
        repository.observeStudentCheckIns().toSafeUiState("student check-ins")

    private val schedulesFlow: Flow<UiState<List<ClassSchedulePeriod>>> =
        repository.observeSchedules().toSafeUiState("class schedules")

    val dashboardState: StateFlow<UiState<SchoolDashboardData>> = combine(
        partA,
        partB,
        partC,
        checkInsFlow,
        schedulesFlow
    ) { a, b, c, checkIns, schedules ->
        when {
            a is UiState.Error -> UiState.Error(a.message)
            b is UiState.Error -> UiState.Error(b.message)
            c is UiState.Error -> UiState.Error(c.message)
            checkIns is UiState.Error -> UiState.Error(checkIns.message)
            schedules is UiState.Error -> UiState.Error(schedules.message)
            a is UiState.Success && b is UiState.Success && c is UiState.Success &&
                checkIns is UiState.Success && schedules is UiState.Success -> {
                UiState.Success(
                    SchoolDashboardData(
                        profile = a.data.first,
                        classes = a.data.second.sortedBy { it.name },
                        students = a.data.third.sortedBy { it.rollNumber },
                        attendance = b.data.first,
                        studentCheckIns = checkIns.data.sortedByDescending { it.dateKey },
                        schedules = schedules.data.sortedWith(
                            compareBy<ClassSchedulePeriod> { it.dayOfWeek }
                                .thenBy { it.periodNumber }
                                .thenBy { it.startTime }
                        ),
                        faculty = b.data.second.sortedBy { it.fullName },
                        facilities = b.data.third.sortedBy { it.name },
                        fees = c.data.first.sortedByDescending { it.dueDateKey },
                        notices = c.data.second.sortedWith(
                            compareByDescending<CampusNotice> { it.pinned }
                                .thenByDescending { it.createdAt?.seconds ?: 0L }
                        ),
                        grades = c.data.third.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    )
                )
            }
            else -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    fun setDateKey(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun shiftDate(deltaDays: Int) {
        _selectedDateKey.value = DateKeyUtils.shiftDateKey(_selectedDateKey.value, deltaDays)
    }

    fun selectClassFilter(classId: String?) {
        _selectedClassId.value = classId
    }

    fun setStudentSearchQuery(query: String) {
        _studentSearchQuery.value = query
    }

    private fun launchMutation(
        successMessage: String? = null,
        block: suspend () -> Result<Unit>
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val result = block()
                result.onSuccess {
                    if (successMessage != null) _events.tryEmit(successMessage)
                }.onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Operation failed")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun saveSchoolProfile(schoolName: String, academicYear: String, principalName: String, motto: String) =
        launchMutation("School profile updated") {
            repository.saveSchoolProfile(schoolName, academicYear, principalName, motto)
        }

    fun addClass(
        name: String,
        gradeLevel: String,
        roomNumber: String,
        homeroomTeacher: String,
        scheduleSummary: String,
        capacity: Int
    ) = launchMutation("Class \"$name\" created") {
        repository.addClass(name, gradeLevel, roomNumber, homeroomTeacher, scheduleSummary, capacity)
    }

    fun deleteClass(classId: String) = launchMutation("Class removed") {
        if (_selectedClassId.value == classId) _selectedClassId.value = null
        repository.deleteClass(classId)
    }

    fun saveStudent(
        existingId: String?,
        fullName: String,
        rollNumber: String,
        schoolClass: SchoolClass,
        guardianName: String,
        guardianPhone: String,
        guardianEmail: String,
        status: StudentStatus,
        notes: String
    ) = launchMutation(if (existingId == null) "Student enrolled" else "Student updated") {
        repository.saveStudent(
            existingId, fullName, rollNumber, schoolClass,
            guardianName, guardianPhone, guardianEmail, status, notes
        )
    }

    fun deleteStudent(studentId: String) = launchMutation("Student removed") {
        repository.deleteStudent(studentId)
    }

    fun markAttendance(student: Student, dateKey: String, status: AttendanceStatus, remarks: String = "") =
        launchMutation {
            repository.markAttendance(student, dateKey, status, remarks)
        }

    fun submitStudentCheckIn(
        student: Student,
        dateKey: String,
        status: AttendanceStatus,
        sessionName: String,
        locationNote: String,
        remarks: String
    ) = launchMutation("Check-in saved for ${student.fullName}") {
        repository.submitStudentCheckIn(student, dateKey, status, sessionName, locationNote, remarks)
    }

    fun markAllPresent(students: List<Student>, dateKey: String, existingRecordStudentIds: Set<String>) =
        launchMutation("Marked ${students.size} students Present") {
            repository.markAllPresentBatch(students, dateKey, existingRecordStudentIds)
        }

    fun addFaculty(
        fullName: String,
        department: String,
        subject: String,
        assignedClass: String,
        email: String,
        phone: String
    ) = launchMutation("Faculty member added") {
        repository.addFaculty(fullName, department, subject, assignedClass, email, phone)
    }

    fun toggleFacultyStatus(member: FacultyMember) = launchMutation("Faculty status updated") {
        repository.toggleFacultyStatus(member)
    }

    fun deleteFaculty(facultyId: String) = launchMutation("Faculty removed") {
        repository.deleteFaculty(facultyId)
    }

    fun addFacility(name: String, category: FacilityCategory, building: String, capacity: Int, notes: String) =
        launchMutation("Facility added") {
            repository.addFacility(name, category, building, capacity, notes)
        }

    fun updateFacilityBooking(facilityId: String, status: FacilityStatus, bookedBy: String, scheduleSlot: String) =
        launchMutation("Facility updated") {
            repository.updateFacilityBooking(facilityId, status, bookedBy, scheduleSlot)
        }

    fun deleteFacility(facilityId: String) = launchMutation("Facility removed") {
        repository.deleteFacility(facilityId)
    }

    fun addFeeInvoice(student: Student, title: String, amount: Double, dueDateKey: String) =
        launchMutation("Invoice created") {
            repository.addFeeInvoice(student, title, amount, dueDateKey)
        }

    fun recordFeePayment(invoice: FeeInvoice, paymentAmount: Double) =
        launchMutation("Payment recorded") {
            repository.recordFeePayment(invoice, paymentAmount)
        }

    fun deleteFeeInvoice(feeId: String) = launchMutation("Invoice deleted") {
        repository.deleteFeeInvoice(feeId)
    }

    fun addNotice(title: String, body: String, category: NoticeCategory, audience: String, pinned: Boolean) =
        launchMutation("Notice published") {
            repository.addNotice(title, body, category, audience, pinned)
        }

    fun toggleNoticePin(notice: CampusNotice) = launchMutation {
        repository.toggleNoticePin(notice)
    }

    fun deleteNotice(noticeId: String) = launchMutation("Notice removed") {
        repository.deleteNotice(noticeId)
    }

    fun addExamGrade(
        student: Student,
        subject: String,
        examName: String,
        score: Double,
        maxScore: Double,
        term: String = "Term 2",
        assessmentType: AssessmentType = AssessmentType.EXAM,
        teacherNotes: String = ""
    ) = launchMutation("Exam grade recorded") {
        repository.addExamGrade(student, subject, examName, score, maxScore, term, assessmentType, teacherNotes)
    }

    fun deleteExamGrade(gradeId: String) = launchMutation("Grade removed") {
        repository.deleteExamGrade(gradeId)
    }

    fun saveSchedulePeriod(
        existingId: String?,
        schoolClass: SchoolClass,
        day: ScheduleDay,
        periodNumber: Int,
        subject: String,
        startTime: String,
        endTime: String,
        facultyId: String,
        facultyName: String,
        roomNumber: String,
        topicNotes: String
    ) = launchMutation(if (existingId == null) "Class period scheduled" else "Schedule updated") {
        repository.saveSchedulePeriod(
            existingId = existingId,
            schoolClass = schoolClass,
            day = day,
            periodNumber = periodNumber,
            subject = subject,
            startTime = startTime,
            endTime = endTime,
            facultyId = facultyId,
            facultyName = facultyName,
            roomNumber = roomNumber,
            topicNotes = topicNotes
        )
    }

    fun deleteSchedulePeriod(scheduleId: String) = launchMutation("Schedule period removed") {
        repository.deleteSchedulePeriod(scheduleId)
    }

    fun seedSampleCampus() = launchMutation("Sample school campus loaded") {
        repository.seedSampleCampus()
    }

    private companion object {
        const val TAG = "SchoolViewModel"
    }
}
