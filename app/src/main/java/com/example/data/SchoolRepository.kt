package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SchoolRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun <T> observeCollection(
        subcollection: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/$subcollection"
        emitAll(
            userDoc(uid).collection(subcollection)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeProfile(): Flow<SchoolProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            userDoc(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(SchoolProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeClasses(): Flow<List<SchoolClass>> = observeCollection("classes", SchoolClass::class.java)
    fun observeStudents(): Flow<List<Student>> = observeCollection("students", Student::class.java)
    fun observeAttendance(): Flow<List<AttendanceRecord>> = observeCollection("attendance", AttendanceRecord::class.java)
    fun observeStudentCheckIns(): Flow<List<StudentDailyCheckIn>> = observeCollection("student_checkins", StudentDailyCheckIn::class.java)
    fun observeSchedules(): Flow<List<ClassSchedulePeriod>> = observeCollection("schedules", ClassSchedulePeriod::class.java)
    fun observeFaculty(): Flow<List<FacultyMember>> = observeCollection("faculty", FacultyMember::class.java)
    fun observeFacilities(): Flow<List<CampusFacility>> = observeCollection("facilities", CampusFacility::class.java)
    fun observeFees(): Flow<List<FeeInvoice>> = observeCollection("fees", FeeInvoice::class.java)
    fun observeNotices(): Flow<List<CampusNotice>> = observeCollection("notices", CampusNotice::class.java)
    fun observeGrades(): Flow<List<ExamGrade>> = observeCollection("grades", ExamGrade::class.java)

    suspend fun saveSchoolProfile(
        schoolName: String,
        academicYear: String,
        principalName: String,
        motto: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanName = schoolName.trim().take(120)
            val cleanYear = academicYear.trim().take(40)
            val cleanPrincipal = principalName.trim().take(100)
            val cleanMotto = motto.trim().take(160)
            require(cleanName.isNotEmpty()) { "School name is required" }
            require(cleanYear.isNotEmpty()) { "Academic year is required" }
            require(cleanPrincipal.isNotEmpty()) { "Principal name is required" }

            val docRef = userDoc(uid)
            path = docRef.path
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "schoolName" to cleanName,
                        "academicYear" to cleanYear,
                        "principalName" to cleanPrincipal,
                        "motto" to cleanMotto,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "schoolName" to cleanName,
                        "academicYear" to cleanYear,
                        "principalName" to cleanPrincipal,
                        "motto" to cleanMotto,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addClass(
        name: String,
        gradeLevel: String,
        roomNumber: String,
        homeroomTeacher: String,
        scheduleSummary: String,
        capacity: Int
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/classes"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(60)
            val cleanGrade = gradeLevel.trim().take(30)
            val cleanRoom = roomNumber.trim().take(30)
            require(cleanName.isNotEmpty()) { "Class name is required" }
            require(cleanGrade.isNotEmpty()) { "Grade level is required" }
            require(cleanRoom.isNotEmpty()) { "Room number is required" }
            require(capacity in 1..200) { "Capacity must be 1..200" }

            val docRef = userDoc(uid).collection("classes").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "name" to cleanName,
                    "gradeLevel" to cleanGrade,
                    "roomNumber" to cleanRoom,
                    "homeroomTeacher" to homeroomTeacher.trim().take(80),
                    "scheduleSummary" to scheduleSummary.trim().take(120),
                    "capacity" to capacity,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteClass(classId: String): Result<Unit> =
        deleteSubdocument("classes", classId)

    suspend fun saveStudent(
        existingId: String?,
        fullName: String,
        rollNumber: String,
        schoolClass: SchoolClass,
        guardianName: String,
        guardianPhone: String,
        guardianEmail: String,
        status: StudentStatus,
        notes: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/students"
        return try {
            val uid = requireUserId()
            val cleanName = fullName.trim().take(100)
            val cleanRoll = rollNumber.trim().take(30)
            require(cleanName.isNotEmpty()) { "Student name is required" }
            require(cleanRoll.isNotEmpty()) { "Roll number is required" }
            require(schoolClass.id.isNotBlank()) { "Valid class must be selected" }

            val col = userDoc(uid).collection("students")
            if (existingId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "fullName" to cleanName,
                        "rollNumber" to cleanRoll,
                        "classId" to schoolClass.id,
                        "className" to schoolClass.name.take(60),
                        "guardianName" to guardianName.trim().take(100),
                        "guardianPhone" to guardianPhone.trim().take(30),
                        "guardianEmail" to guardianEmail.trim().take(120),
                        "status" to status.name,
                        "notes" to notes.trim().take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = col.document(existingId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "fullName" to cleanName,
                        "rollNumber" to cleanRoll,
                        "classId" to schoolClass.id,
                        "className" to schoolClass.name.take(60),
                        "guardianName" to guardianName.trim().take(100),
                        "guardianPhone" to guardianPhone.trim().take(30),
                        "guardianEmail" to guardianEmail.trim().take(120),
                        "status" to status.name,
                        "notes" to notes.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(studentId: String): Result<Unit> =
        deleteSubdocument("students", studentId)

    suspend fun markAttendance(
        student: Student,
        dateKey: String,
        status: AttendanceStatus,
        remarks: String
    ): Result<Unit> {
        val docId = "${dateKey}_${student.id}"
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/attendance/$docId"
        return try {
            val uid = requireUserId()
            require(student.id.isNotBlank()) { "Student ID required" }
            require(dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Invalid dateKey" }

            val docRef = userDoc(uid).collection("attendance").document(docId)
            path = docRef.path
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "studentName" to student.fullName.take(100),
                        "rollNumber" to student.rollNumber.take(30),
                        "classId" to student.classId,
                        "className" to student.className.take(60),
                        "status" to status.name,
                        "remarks" to remarks.trim().take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "studentId" to student.id,
                        "studentName" to student.fullName.take(100),
                        "rollNumber" to student.rollNumber.take(30),
                        "classId" to student.classId,
                        "className" to student.className.take(60),
                        "dateKey" to dateKey,
                        "status" to status.name,
                        "remarks" to remarks.trim().take(200),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitStudentCheckIn(
        student: Student,
        dateKey: String,
        status: AttendanceStatus,
        sessionName: String,
        locationNote: String,
        remarks: String
    ): Result<Unit> {
        val docId = "${dateKey}_${student.id}"
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/student_checkins/$docId"
        return try {
            val uid = requireUserId()
            require(student.id.isNotBlank()) { "Student must be selected" }
            require(dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Invalid date format" }
            val cleanSession = sessionName.trim().ifEmpty { CheckInSession.MORNING_HOMEROOM.label }.take(60)
            val cleanTime = DateKeyUtils.currentTimeLabel().take(30)
            val cleanLocation = locationNote.trim().take(80)
            val cleanRemarks = remarks.trim().take(200)

            val checkInRef = userDoc(uid).collection("student_checkins").document(docId)
            val attendanceRef = userDoc(uid).collection("attendance").document(docId)
            path = checkInRef.path

            val checkInExists = checkInRef.get().await().exists()
            val attendanceExists = attendanceRef.get().await().exists()

            val batch = db.batch()
            if (checkInExists) {
                batch.update(
                    checkInRef,
                    mapOf(
                        "studentName" to student.fullName.take(100),
                        "rollNumber" to student.rollNumber.take(30),
                        "classId" to student.classId,
                        "className" to student.className.take(60),
                        "status" to status.name,
                        "sessionName" to cleanSession,
                        "checkInTimeLabel" to cleanTime,
                        "locationNote" to cleanLocation,
                        "remarks" to cleanRemarks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    checkInRef,
                    mapOf(
                        "studentId" to student.id,
                        "studentName" to student.fullName.take(100),
                        "rollNumber" to student.rollNumber.take(30),
                        "classId" to student.classId,
                        "className" to student.className.take(60),
                        "dateKey" to dateKey,
                        "status" to status.name,
                        "sessionName" to cleanSession,
                        "checkInTimeLabel" to cleanTime,
                        "locationNote" to cleanLocation,
                        "remarks" to cleanRemarks,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val syncedRemark = if (cleanRemarks.isNotEmpty()) {
                "$cleanRemarks ($cleanSession)"
            } else {
                "Self check-in · $cleanSession"
            }.take(200)

            if (attendanceExists) {
                batch.update(
                    attendanceRef,
                    mapOf(
                        "studentName" to student.fullName.take(100),
                        "rollNumber" to student.rollNumber.take(30),
                        "classId" to student.classId,
                        "className" to student.className.take(60),
                        "status" to status.name,
                        "remarks" to syncedRemark,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    attendanceRef,
                    mapOf(
                        "studentId" to student.id,
                        "studentName" to student.fullName.take(100),
                        "rollNumber" to student.rollNumber.take(30),
                        "classId" to student.classId,
                        "className" to student.className.take(60),
                        "dateKey" to dateKey,
                        "status" to status.name,
                        "remarks" to syncedRemark,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllPresentBatch(
        students: List<Student>,
        dateKey: String,
        existingRecordStudentIds: Set<String>
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/attendance"
        return try {
            val uid = requireUserId()
            if (students.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            val col = userDoc(uid).collection("attendance")
            students.take(400).forEach { student ->
                val docId = "${dateKey}_${student.id}"
                val docRef = col.document(docId)
                path = docRef.path
                if (student.id in existingRecordStudentIds) {
                    batch.update(
                        docRef,
                        mapOf(
                            "studentName" to student.fullName.take(100),
                            "rollNumber" to student.rollNumber.take(30),
                            "classId" to student.classId,
                            "className" to student.className.take(60),
                            "status" to AttendanceStatus.PRESENT.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        docRef,
                        mapOf(
                            "studentId" to student.id,
                            "studentName" to student.fullName.take(100),
                            "rollNumber" to student.rollNumber.take(30),
                            "classId" to student.classId,
                            "className" to student.className.take(60),
                            "dateKey" to dateKey,
                            "status" to AttendanceStatus.PRESENT.name,
                            "remarks" to "",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addFaculty(
        fullName: String,
        department: String,
        subject: String,
        assignedClass: String,
        email: String,
        phone: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/faculty"
        return try {
            val uid = requireUserId()
            require(fullName.trim().isNotEmpty() && department.trim().isNotEmpty() && subject.trim().isNotEmpty()) {
                "Name, department, and subject are required"
            }
            val docRef = userDoc(uid).collection("faculty").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "fullName" to fullName.trim().take(100),
                    "department" to department.trim().take(60),
                    "subject" to subject.trim().take(80),
                    "assignedClass" to assignedClass.trim().take(60),
                    "email" to email.trim().take(120),
                    "phone" to phone.trim().take(30),
                    "status" to FacultyStatus.ACTIVE.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFacultyStatus(member: FacultyMember): Result<Unit> {
        val nextStatus = if (member.status == FacultyStatus.ACTIVE.name) {
            FacultyStatus.ON_LEAVE.name
        } else {
            FacultyStatus.ACTIVE.name
        }
        return updateSubdocument("faculty", member.id, mapOf("status" to nextStatus))
    }

    suspend fun deleteFaculty(facultyId: String): Result<Unit> =
        deleteSubdocument("faculty", facultyId)

    suspend fun addFacility(
        name: String,
        category: FacilityCategory,
        building: String,
        capacity: Int,
        notes: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/facilities"
        return try {
            val uid = requireUserId()
            require(name.trim().isNotEmpty() && building.trim().isNotEmpty()) { "Name and building required" }
            require(capacity in 1..2000) { "Capacity must be 1..2000" }
            val docRef = userDoc(uid).collection("facilities").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "name" to name.trim().take(80),
                    "category" to category.name,
                    "building" to building.trim().take(60),
                    "capacity" to capacity,
                    "status" to FacilityStatus.AVAILABLE.name,
                    "bookedBy" to "",
                    "scheduleSlot" to "",
                    "notes" to notes.trim().take(250),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFacilityBooking(
        facilityId: String,
        status: FacilityStatus,
        bookedBy: String,
        scheduleSlot: String
    ): Result<Unit> {
        return updateSubdocument(
            "facilities",
            facilityId,
            mapOf(
                "status" to status.name,
                "bookedBy" to bookedBy.trim().take(80),
                "scheduleSlot" to scheduleSlot.trim().take(80)
            )
        )
    }

    suspend fun deleteFacility(facilityId: String): Result<Unit> =
        deleteSubdocument("facilities", facilityId)

    suspend fun addFeeInvoice(
        student: Student,
        title: String,
        amount: Double,
        dueDateKey: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/fees"
        return try {
            val uid = requireUserId()
            require(student.id.isNotBlank()) { "Student required" }
            require(title.trim().isNotEmpty()) { "Invoice title required" }
            require(amount > 0.0 && amount <= 1000000.0) { "Amount must be positive" }
            require(dueDateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) { "Due date format YYYY-MM-DD" }

            val docRef = userDoc(uid).collection("fees").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "studentId" to student.id,
                    "studentName" to student.fullName.take(100),
                    "className" to student.className.take(60),
                    "title" to title.trim().take(100),
                    "amount" to amount,
                    "paidAmount" to 0.0,
                    "dueDateKey" to dueDateKey,
                    "status" to FeeStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordFeePayment(invoice: FeeInvoice, paymentAmount: Double): Result<Unit> {
        val newPaid = (invoice.paidAmount + paymentAmount).coerceIn(0.0, invoice.amount)
        val newStatus = when {
            newPaid >= invoice.amount -> FeeStatus.PAID.name
            newPaid > 0.0 -> FeeStatus.PARTIAL.name
            else -> FeeStatus.PENDING.name
        }
        return updateSubdocument(
            "fees",
            invoice.id,
            mapOf(
                "paidAmount" to newPaid,
                "status" to newStatus
            )
        )
    }

    suspend fun deleteFeeInvoice(feeId: String): Result<Unit> =
        deleteSubdocument("fees", feeId)

    suspend fun addNotice(
        title: String,
        body: String,
        category: NoticeCategory,
        audience: String,
        pinned: Boolean
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notices"
        return try {
            val uid = requireUserId()
            require(title.trim().isNotEmpty() && body.trim().isNotEmpty()) { "Title and message required" }
            val docRef = userDoc(uid).collection("notices").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "title" to title.trim().take(120),
                    "body" to body.trim().take(1000),
                    "category" to category.name,
                    "audience" to audience.trim().ifEmpty { "All Students & Staff" }.take(60),
                    "pinned" to pinned,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleNoticePin(notice: CampusNotice): Result<Unit> =
        updateSubdocument("notices", notice.id, mapOf("pinned" to !notice.pinned))

    suspend fun deleteNotice(noticeId: String): Result<Unit> =
        deleteSubdocument("notices", noticeId)

    suspend fun addExamGrade(
        student: Student,
        subject: String,
        examName: String,
        score: Double,
        maxScore: Double,
        term: String = "Term 2",
        assessmentType: AssessmentType = AssessmentType.EXAM,
        teacherNotes: String = ""
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/grades"
        return try {
            val uid = requireUserId()
            require(student.id.isNotBlank()) { "Student required" }
            require(subject.trim().isNotEmpty() && examName.trim().isNotEmpty()) { "Subject and exam required" }
            require(maxScore > 0.0 && maxScore <= 1000.0 && score in 0.0..maxScore) { "Score must be within 0..maxScore" }
            val gradeLetter = DateKeyUtils.computeGradeLetter(score, maxScore)
            val cleanTerm = term.trim().ifEmpty { "Term 2" }.take(40)
            val cleanNotes = teacherNotes.trim().take(200)
            val docRef = userDoc(uid).collection("grades").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "studentId" to student.id,
                    "studentName" to student.fullName.take(100),
                    "className" to student.className.take(60),
                    "subject" to subject.trim().take(60),
                    "examName" to examName.trim().take(80),
                    "score" to score,
                    "maxScore" to maxScore,
                    "gradeLetter" to gradeLetter,
                    "term" to cleanTerm,
                    "assessmentType" to assessmentType.name,
                    "teacherNotes" to cleanNotes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteExamGrade(gradeId: String): Result<Unit> =
        deleteSubdocument("grades", gradeId)

    suspend fun saveSchedulePeriod(
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
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/schedules"
        return try {
            val uid = requireUserId()
            val cleanSubject = subject.trim().take(80)
            val cleanStart = startTime.trim().take(20)
            val cleanEnd = endTime.trim().take(20)
            val cleanFacultyName = facultyName.trim().take(100)
            require(schoolClass.id.isNotBlank()) { "Class must be selected" }
            require(periodNumber in 1..12) { "Period number must be 1..12" }
            require(cleanSubject.isNotEmpty()) { "Subject is required" }
            require(cleanStart.isNotEmpty() && cleanEnd.isNotEmpty()) { "Start and end times are required" }
            require(cleanFacultyName.isNotEmpty()) { "Faculty member name is required" }

            val col = userDoc(uid).collection("schedules")
            if (existingId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "classId" to schoolClass.id,
                        "className" to schoolClass.name.take(60),
                        "dayOfWeek" to day.name,
                        "periodNumber" to periodNumber,
                        "subject" to cleanSubject,
                        "startTime" to cleanStart,
                        "endTime" to cleanEnd,
                        "facultyId" to facultyId.trim().take(128),
                        "facultyName" to cleanFacultyName,
                        "roomNumber" to roomNumber.trim().take(40),
                        "topicNotes" to topicNotes.trim().take(200),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = col.document(existingId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "className" to schoolClass.name.take(60),
                        "dayOfWeek" to day.name,
                        "periodNumber" to periodNumber,
                        "subject" to cleanSubject,
                        "startTime" to cleanStart,
                        "endTime" to cleanEnd,
                        "facultyId" to facultyId.trim().take(128),
                        "facultyName" to cleanFacultyName,
                        "roomNumber" to roomNumber.trim().take(40),
                        "topicNotes" to topicNotes.trim().take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSchedulePeriod(scheduleId: String): Result<Unit> =
        deleteSubdocument("schedules", scheduleId)

    private suspend fun updateSubdocument(
        subcollection: String,
        docId: String,
        fields: Map<String, Any>
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$subcollection/$docId"
        return try {
            val uid = requireUserId()
            require(docId.isNotBlank()) { "Document ID required" }
            val docRef = userDoc(uid).collection(subcollection).document(docId)
            path = docRef.path
            val payload = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun deleteSubdocument(subcollection: String, docId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$subcollection/$docId"
        return try {
            val uid = requireUserId()
            require(docId.isNotBlank()) { "Document ID required" }
            val docRef = userDoc(uid).collection(subcollection).document(docId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedSampleCampus(): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val root = userDoc(uid)
            path = root.path
            val batch = db.batch()
            val today = DateKeyUtils.todayKey()
            val yesterday = DateKeyUtils.shiftDateKey(today, -1)

            val profileExists = root.get().await().exists()
            if (!profileExists) {
                batch.set(
                    root,
                    mapOf(
                        "userId" to uid,
                        "schoolName" to "Oakridge International Academy",
                        "academicYear" to "2026-2027",
                        "principalName" to (auth.currentUser?.displayName ?: "Dr. Elena Vance"),
                        "motto" to "Curiosity, Integrity & Excellence",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            data class SeedClass(val id: String, val name: String, val room: String, val grade: String, val teacher: String, val schedule: String)
            val classes = listOf(
                SeedClass("cls_10a", "Grade 10 - Section A", "Room 204", "Grade 10", "Dr. Sarah Jenkins", "Mon-Fri 08:15 - 14:45"),
                SeedClass("cls_9b", "Grade 9 - Section B", "Room 108", "Grade 9", "Prof. Marcus Cole", "Mon-Fri 08:15 - 14:30"),
                SeedClass("cls_8a", "Grade 8 - Section A", "Room 103", "Grade 8", "Ms. Priya Nair", "Mon-Fri 08:30 - 14:15")
            )
            classes.forEach { c ->
                batch.set(
                    root.collection("classes").document(c.id),
                    mapOf(
                        "name" to c.name,
                        "gradeLevel" to c.grade,
                        "roomNumber" to c.room,
                        "homeroomTeacher" to c.teacher,
                        "scheduleSummary" to c.schedule,
                        "capacity" to 32,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            data class SeedStudent(val id: String, val name: String, val roll: String, val clsId: String, val clsName: String, val guardian: String, val phone: String)
            val students = listOf(
                SeedStudent("stu_01", "Aarav Sharma", "10A-01", "cls_10a", "Grade 10 - Section A", "Priya Sharma", "+1-555-0142"),
                SeedStudent("stu_02", "Maya Lin", "10A-02", "cls_10a", "Grade 10 - Section A", "David Lin", "+1-555-0188"),
                SeedStudent("stu_03", "Lucas Bennett", "10A-03", "cls_10a", "Grade 10 - Section A", "Claire Bennett", "+1-555-0194"),
                SeedStudent("stu_04", "Zara Al-Mansoor", "9B-01", "cls_9b", "Grade 9 - Section B", "Tariq Al-Mansoor", "+1-555-0127"),
                SeedStudent("stu_05", "Ethan O'Connor", "9B-02", "cls_9b", "Grade 9 - Section B", "Nora O'Connor", "+1-555-0165"),
                SeedStudent("stu_06", "Sofia Morales", "8A-01", "cls_8a", "Grade 8 - Section A", "Carlos Morales", "+1-555-0173")
            )
            students.forEachIndexed { idx, s ->
                batch.set(
                    root.collection("students").document(s.id),
                    mapOf(
                        "fullName" to s.name,
                        "rollNumber" to s.roll,
                        "classId" to s.clsId,
                        "className" to s.clsName,
                        "guardianName" to s.guardian,
                        "guardianPhone" to s.phone,
                        "status" to StudentStatus.ACTIVE.name,
                        "notes" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val todayStatus = when (idx) {
                    2 -> AttendanceStatus.LATE
                    4 -> AttendanceStatus.ABSENT
                    else -> AttendanceStatus.PRESENT
                }
                batch.set(
                    root.collection("attendance").document("${today}_${s.id}"),
                    mapOf(
                        "studentId" to s.id,
                        "studentName" to s.name,
                        "rollNumber" to s.roll,
                        "classId" to s.clsId,
                        "className" to s.clsName,
                        "dateKey" to today,
                        "status" to todayStatus.name,
                        "remarks" to if (todayStatus == AttendanceStatus.LATE) "Shuttle Route 3 delayed" else "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    root.collection("attendance").document("${yesterday}_${s.id}"),
                    mapOf(
                        "studentId" to s.id,
                        "studentName" to s.name,
                        "rollNumber" to s.roll,
                        "classId" to s.clsId,
                        "className" to s.clsName,
                        "dateKey" to yesterday,
                        "status" to AttendanceStatus.PRESENT.name,
                        "remarks" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val faculty = listOf(
                listOf("fac_01", "Dr. Sarah Jenkins", "Sciences", "Physics & Robotics", "Grade 10 - Section A", "s.jenkins@oakridge.edu", FacultyStatus.ACTIVE.name),
                listOf("fac_02", "Prof. Marcus Cole", "Mathematics", "Applied Calculus & Algebra", "Grade 9 - Section B", "m.cole@oakridge.edu", FacultyStatus.ACTIVE.name),
                listOf("fac_03", "Ms. Priya Nair", "Humanities", "World Literature", "Grade 8 - Section A", "p.nair@oakridge.edu", FacultyStatus.ACTIVE.name),
                listOf("fac_04", "Coach Daniel Brooks", "Athletics", "Track & Swimming", "All Grades", "d.brooks@oakridge.edu", FacultyStatus.ON_LEAVE.name)
            )
            faculty.forEach { f ->
                batch.set(
                    root.collection("faculty").document(f[0]),
                    mapOf(
                        "fullName" to f[1],
                        "department" to f[2],
                        "subject" to f[3],
                        "assignedClass" to f[4],
                        "email" to f[5],
                        "status" to f[6],
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val facilities = listOf(
                listOf("facil_01", "STEM & Robotics Lab", FacilityCategory.LAB.name, "Innovation Wing · 2F", "32", FacilityStatus.OCCUPIED.name, "Grade 10 - Section A", "09:30 - 11:00"),
                listOf("facil_02", "Central Commons Library", FacilityCategory.LIBRARY.name, "Main Pavilion · 1F", "120", FacilityStatus.AVAILABLE.name, "", ""),
                listOf("facil_03", "Founders Auditorium", FacilityCategory.HALL.name, "East Hall · GF", "450", FacilityStatus.AVAILABLE.name, "", ""),
                listOf("facil_04", "Aquatic & Turf Complex", FacilityCategory.SPORTS.name, "North Campus", "80", FacilityStatus.MAINTENANCE.name, "Facility Ops", "Pool filter service"),
                listOf("facil_05", "Campus Shuttle Bus #4", FacilityCategory.TRANSPORT.name, "Transport Bay B", "44", FacilityStatus.OCCUPIED.name, "North Ridge Route", "15:00 Departure")
            )
            facilities.forEach { fac ->
                batch.set(
                    root.collection("facilities").document(fac[0]),
                    mapOf(
                        "name" to fac[1],
                        "category" to fac[2],
                        "building" to fac[3],
                        "capacity" to fac[4].toInt(),
                        "status" to fac[5],
                        "bookedBy" to fac[6],
                        "scheduleSlot" to fac[7],
                        "notes" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                root.collection("fees").document("fee_01"),
                mapOf(
                    "studentId" to "stu_01",
                    "studentName" to "Aarav Sharma",
                    "className" to "Grade 10 - Section A",
                    "title" to "Term 2 Tuition & STEM Lab Fee",
                    "amount" to 1450.0,
                    "paidAmount" to 1450.0,
                    "dueDateKey" to "2026-12-05",
                    "status" to FeeStatus.PAID.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                root.collection("fees").document("fee_02"),
                mapOf(
                    "studentId" to "stu_02",
                    "studentName" to "Maya Lin",
                    "className" to "Grade 10 - Section A",
                    "title" to "Term 2 Tuition & Shuttle Pass",
                    "amount" to 1600.0,
                    "paidAmount" to 800.0,
                    "dueDateKey" to "2026-12-05",
                    "status" to FeeStatus.PARTIAL.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                root.collection("fees").document("fee_03"),
                mapOf(
                    "studentId" to "stu_04",
                    "studentName" to "Zara Al-Mansoor",
                    "className" to "Grade 9 - Section B",
                    "title" to "Term 2 Tuition Fee",
                    "amount" to 1350.0,
                    "paidAmount" to 0.0,
                    "dueDateKey" to "2026-12-05",
                    "status" to FeeStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.set(
                root.collection("notices").document("not_01"),
                mapOf(
                    "title" to "Winter Midterm Schedule Released",
                    "body" to "Midterm assessments begin December 7 across Grades 8–10. Science Labs are reserved for practical reviews all week.",
                    "category" to NoticeCategory.EXAM.name,
                    "audience" to "Grades 8–10 & Parents",
                    "pinned" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                root.collection("notices").document("not_02"),
                mapOf(
                    "title" to "Aquatic Complex Scheduled Maintenance",
                    "body" to "Swimming pool filtration maintenance runs through Wednesday. Indoor track remains open for athletics.",
                    "category" to NoticeCategory.URGENT.name,
                    "audience" to "All Campus",
                    "pinned" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            data class SeedGrade(
                val id: String,
                val stuId: String,
                val stuName: String,
                val clsName: String,
                val subject: String,
                val exam: String,
                val score: Double,
                val max: Double,
                val term: String,
                val type: AssessmentType,
                val notes: String
            )
            val seedGrades = listOf(
                SeedGrade("grd_01", "stu_01", "Aarav Sharma", "Grade 10 - Section A", "Physics & Robotics", "Lab Practical #2", 95.0, 100.0, "Term 2", AssessmentType.LAB, "Excellent sensor calibration"),
                SeedGrade("grd_02", "stu_01", "Aarav Sharma", "Grade 10 - Section A", "Physics & Robotics", "Kinematics Quiz", 46.0, 50.0, "Term 2", AssessmentType.QUIZ, "Strong derivation steps"),
                SeedGrade("grd_03", "stu_01", "Aarav Sharma", "Grade 10 - Section A", "Applied Calculus", "Midterm Exam", 89.0, 100.0, "Term 2", AssessmentType.EXAM, "Clean optimization proofs"),
                SeedGrade("grd_04", "stu_01", "Aarav Sharma", "Grade 10 - Section A", "World Literature", "Comparative Essay", 42.0, 50.0, "Term 2", AssessmentType.ASSIGNMENT, "Insightful primary source quotes"),
                SeedGrade("grd_05", "stu_02", "Maya Lin", "Grade 10 - Section A", "Applied Calculus", "Calculus Quiz 3", 88.0, 100.0, "Term 2", AssessmentType.QUIZ, "Consistent accuracy"),
                SeedGrade("grd_06", "stu_02", "Maya Lin", "Grade 10 - Section A", "Physics & Robotics", "Autonomous Rover Project", 96.0, 100.0, "Term 2", AssessmentType.PROJECT, "Top prototype demonstration"),
                SeedGrade("grd_07", "stu_02", "Maya Lin", "Grade 10 - Section A", "World Literature", "Term Paper", 91.0, 100.0, "Term 2", AssessmentType.EXAM, "Cohesive thesis structure"),
                SeedGrade("grd_08", "stu_03", "Lucas Bennett", "Grade 10 - Section A", "Applied Calculus", "Midterm Exam", 76.0, 100.0, "Term 2", AssessmentType.EXAM, "Review integration by parts"),
                SeedGrade("grd_09", "stu_04", "Zara Al-Mansoor", "Grade 9 - Section B", "Algebra II", "Quadratic Modeling Exam", 94.0, 100.0, "Term 2", AssessmentType.EXAM, "Top score in section"),
                SeedGrade("grd_10", "stu_04", "Zara Al-Mansoor", "Grade 9 - Section B", "Foundations of Physics", "Force & Motion Lab", 45.0, 50.0, "Term 2", AssessmentType.LAB, "Well-documented data tables")
            )
            seedGrades.forEach { g ->
                batch.set(
                    root.collection("grades").document(g.id),
                    mapOf(
                        "studentId" to g.stuId,
                        "studentName" to g.stuName,
                        "className" to g.clsName,
                        "subject" to g.subject,
                        "examName" to g.exam,
                        "score" to g.score,
                        "maxScore" to g.max,
                        "gradeLetter" to DateKeyUtils.computeGradeLetter(g.score, g.max),
                        "term" to g.term,
                        "assessmentType" to g.type.name,
                        "teacherNotes" to g.notes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            data class SeedPeriod(
                val id: String,
                val clsId: String,
                val clsName: String,
                val day: ScheduleDay,
                val period: Int,
                val subject: String,
                val start: String,
                val end: String,
                val facId: String,
                val facName: String,
                val room: String,
                val topic: String
            )
            val weekdays = listOf(
                ScheduleDay.MONDAY,
                ScheduleDay.TUESDAY,
                ScheduleDay.WEDNESDAY,
                ScheduleDay.THURSDAY,
                ScheduleDay.FRIDAY
            )
            val seedPeriods = mutableListOf<SeedPeriod>()
            weekdays.forEach { day ->
                seedPeriods += SeedPeriod(
                    "sch_10a_${day.shortLabel.lowercase()}_1", "cls_10a", "Grade 10 - Section A",
                    day, 1, "Physics & Robotics", "08:15", "09:10",
                    "fac_01", "Dr. Sarah Jenkins", "STEM Lab · 2F", "Motion & sensor systems"
                )
                seedPeriods += SeedPeriod(
                    "sch_10a_${day.shortLabel.lowercase()}_2", "cls_10a", "Grade 10 - Section A",
                    day, 2, "Applied Calculus", "09:20", "10:15",
                    "fac_02", "Prof. Marcus Cole", "Room 204", "Derivatives & rate problems"
                )
                seedPeriods += SeedPeriod(
                    "sch_10a_${day.shortLabel.lowercase()}_3", "cls_10a", "Grade 10 - Section A",
                    day, 3, "World Literature", "10:35", "11:30",
                    "fac_03", "Ms. Priya Nair", "Room 204", "Comparative essay workshop"
                )
                seedPeriods += SeedPeriod(
                    "sch_9b_${day.shortLabel.lowercase()}_1", "cls_9b", "Grade 9 - Section B",
                    day, 1, "Algebra II", "08:15", "09:10",
                    "fac_02", "Prof. Marcus Cole", "Room 108", "Quadratic modeling"
                )
                seedPeriods += SeedPeriod(
                    "sch_9b_${day.shortLabel.lowercase()}_2", "cls_9b", "Grade 9 - Section B",
                    day, 2, "Foundations of Physics", "09:20", "10:15",
                    "fac_01", "Dr. Sarah Jenkins", "Room 108", "Newtonian mechanics lab"
                )
                seedPeriods += SeedPeriod(
                    "sch_8a_${day.shortLabel.lowercase()}_1", "cls_8a", "Grade 8 - Section A",
                    day, 1, "World Literature", "08:30", "09:25",
                    "fac_03", "Ms. Priya Nair", "Room 103", "Reading & narrative analysis"
                )
            }
            seedPeriods.forEach { p ->
                batch.set(
                    root.collection("schedules").document(p.id),
                    mapOf(
                        "classId" to p.clsId,
                        "className" to p.clsName,
                        "dayOfWeek" to p.day.name,
                        "periodNumber" to p.period,
                        "subject" to p.subject,
                        "startTime" to p.start,
                        "endTime" to p.end,
                        "facultyId" to p.facId,
                        "facultyName" to p.facName,
                        "roomNumber" to p.room,
                        "topicNotes" to p.topic,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
