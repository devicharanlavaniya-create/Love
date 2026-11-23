package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.AttendanceStatus
import com.example.data.CampusFacility
import com.example.data.ClassSchedulePeriod
import com.example.data.FeeInvoice
import com.example.data.ScheduleDay
import com.example.data.SchoolRepository
import com.example.data.Student
import com.example.data.UiState
import com.example.data.signOut
import com.example.ui.components.EmptyCampusCard
import com.example.ui.screens.AcademicsScreen
import com.example.ui.screens.AcademicsTab
import com.example.ui.screens.AddClassDialog
import com.example.ui.screens.AddExamGradeDialog
import com.example.ui.screens.AddFacilityDialog
import com.example.ui.screens.AddFacultyDialog
import com.example.ui.screens.AddFeeInvoiceDialog
import com.example.ui.screens.AddNoticeDialog
import com.example.ui.screens.AttendanceMode
import com.example.ui.screens.AttendanceRemarkDialog
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BookFacilityDialog
import com.example.ui.screens.CampusScreen
import com.example.ui.screens.CampusTab
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.RecordFeePaymentDialog
import com.example.ui.screens.SchedulePeriodDialog
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SchoolProfileDialog
import com.example.ui.screens.StudentDialog
import com.example.ui.viewmodel.SchoolDashboardData
import com.example.ui.viewmodel.SchoolViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_OVERVIEW = "overview"
const val ROUTE_ATTENDANCE = "attendance"
const val ROUTE_SCHEDULE = "schedule"
const val ROUTE_ACADEMICS = "academics"
const val ROUTE_CAMPUS = "campus"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val Destinations = listOf(
    NavDestination(ROUTE_OVERVIEW, "Overview", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_overview"),
    NavDestination(ROUTE_ATTENDANCE, "Attendance", Icons.Filled.FactCheck, Icons.Outlined.FactCheck, "nav_attendance"),
    NavDestination(ROUTE_SCHEDULE, "Schedule", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday, "nav_schedule"),
    NavDestination(ROUTE_ACADEMICS, "Academics", Icons.Filled.School, Icons.Outlined.School, "nav_academics"),
    NavDestination(ROUTE_CAMPUS, "Campus", Icons.Filled.Apartment, Icons.Outlined.Apartment, "nav_campus")
)

private sealed interface ActiveDialog {
    data object EditProfile : ActiveDialog
    data object AddClass : ActiveDialog
    data class EditStudent(val student: Student?) : ActiveDialog
    data class AttendanceRemark(val student: Student, val status: AttendanceStatus, val remarks: String) : ActiveDialog
    data class EditSchedulePeriod(val period: ClassSchedulePeriod?) : ActiveDialog
    data object AddFaculty : ActiveDialog
    data object AddFacility : ActiveDialog
    data class BookFacility(val facility: CampusFacility) : ActiveDialog
    data object AddFeeInvoice : ActiveDialog
    data class RecordPayment(val invoice: FeeInvoice) : ActiveDialog
    data object AddNotice : ActiveDialog
    data object AddGrade : ActiveDialog
}

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            val schoolViewModel: SchoolViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        SchoolViewModel(SchoolRepository(db), uid)
                    }
                }
            )
            CampusPulseMainShell(viewModel = schoolViewModel)
        }
    }
}

@Composable
private fun CampusPulseMainShell(viewModel: SchoolViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_OVERVIEW
    val snackbarHostState = remember { SnackbarHostState() }

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val selectedClassId by viewModel.selectedClassId.collectAsStateWithLifecycle()
    val studentSearchQuery by viewModel.studentSearchQuery.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var attendanceMode by remember { mutableStateOf(AttendanceMode.ROLL_CALL) }
    var selectedScheduleDay by remember { mutableStateOf(ScheduleDay.today()) }
    var selectedGradeStudentId by remember { mutableStateOf<String?>(null) }
    var academicsTab by remember { mutableStateOf(AcademicsTab.STUDENTS) }
    var campusTab by remember { mutableStateOf(CampusTab.FACILITIES) }
    var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val currentData = (dashboardState as? UiState.Success<SchoolDashboardData>)?.data

    val fabAction: (() -> Unit)? = if (currentData == null) null else when (currentRoute) {
        ROUTE_SCHEDULE -> {
            {
                if (currentData.classes.isEmpty()) {
                    activeDialog = ActiveDialog.AddClass
                } else {
                    activeDialog = ActiveDialog.EditSchedulePeriod(null)
                }
            }
        }
        ROUTE_ACADEMICS -> when (academicsTab) {
            AcademicsTab.STUDENTS -> {
                {
                    if (currentData.classes.isEmpty()) {
                        activeDialog = ActiveDialog.AddClass
                    } else {
                        activeDialog = ActiveDialog.EditStudent(null)
                    }
                }
            }
            AcademicsTab.CLASSES -> { { activeDialog = ActiveDialog.AddClass } }
            AcademicsTab.GRADES -> {
                {
                    if (currentData.students.isEmpty()) {
                        academicsTab = AcademicsTab.STUDENTS
                        activeDialog = if (currentData.classes.isEmpty()) ActiveDialog.AddClass else ActiveDialog.EditStudent(null)
                    } else {
                        activeDialog = ActiveDialog.AddGrade
                    }
                }
            }
        }
        ROUTE_CAMPUS -> when (campusTab) {
            CampusTab.FACILITIES -> { { activeDialog = ActiveDialog.AddFacility } }
            CampusTab.FACULTY -> { { activeDialog = ActiveDialog.AddFaculty } }
            CampusTab.FEES -> {
                {
                    if (currentData.students.isEmpty()) {
                        navigateToTab(ROUTE_ACADEMICS)
                    } else {
                        activeDialog = ActiveDialog.AddFeeInvoice
                    }
                }
            }
            CampusTab.NOTICES -> { { activeDialog = ActiveDialog.AddNotice } }
        }
        else -> null
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        Destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (fabAction != null) {
                    FloatingActionButton(
                        onClick = fabAction,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("contextual_fab")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add item")
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.weight(0.1f))
                        Destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                    ) {
                        when (val state = dashboardState) {
                            UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyCampusCard(
                                        icon = Icons.Filled.School,
                                        title = "Unable to Load School Data",
                                        description = state.message,
                                        primaryActionLabel = "Sign Out",
                                        onPrimaryAction = { signOut(credentialManager, scope) }
                                    )
                                }
                            }
                            is UiState.Success -> {
                                val data = state.data
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_OVERVIEW,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable(ROUTE_OVERVIEW) {
                                        OverviewScreen(
                                            data = data,
                                            isBusy = isBusy,
                                            onNavigateAttendance = {
                                                attendanceMode = AttendanceMode.ROLL_CALL
                                                navigateToTab(ROUTE_ATTENDANCE)
                                            },
                                            onNavigateStudentCheckIn = {
                                                attendanceMode = AttendanceMode.SELF_CHECK_IN
                                                navigateToTab(ROUTE_ATTENDANCE)
                                            },
                                            onNavigateSchedule = {
                                                navigateToTab(ROUTE_SCHEDULE)
                                            },
                                            onNavigateGrades = {
                                                academicsTab = AcademicsTab.GRADES
                                                navigateToTab(ROUTE_ACADEMICS)
                                            },
                                            onNavigateAcademics = { navigateToTab(ROUTE_ACADEMICS) },
                                            onNavigateCampus = { navigateToTab(ROUTE_CAMPUS) },
                                            onEditProfile = { activeDialog = ActiveDialog.EditProfile },
                                            onQuickAddClass = { activeDialog = ActiveDialog.AddClass },
                                            onQuickAddStudent = { activeDialog = ActiveDialog.EditStudent(null) },
                                            onSeedSampleCampus = { viewModel.seedSampleCampus() },
                                            onSignOut = { signOut(credentialManager, scope) }
                                        )
                                    }
                                    composable(ROUTE_ATTENDANCE) {
                                        AttendanceScreen(
                                            data = data,
                                            mode = attendanceMode,
                                            onModeChange = { attendanceMode = it },
                                            selectedDateKey = selectedDateKey,
                                            selectedClassId = selectedClassId,
                                            isBusy = isBusy,
                                            onShiftDate = { viewModel.shiftDate(it) },
                                            onSelectDateKey = { viewModel.setDateKey(it) },
                                            onSelectClassId = { viewModel.selectClassFilter(it) },
                                            onMarkAttendance = { stu, st, rem ->
                                                viewModel.markAttendance(stu, selectedDateKey, st, rem)
                                            },
                                            onMarkAllPresent = { students, existingIds ->
                                                viewModel.markAllPresent(students, selectedDateKey, existingIds)
                                            },
                                            onOpenRemarkDialog = { stu, st, rem ->
                                                activeDialog = ActiveDialog.AttendanceRemark(stu, st, rem)
                                            },
                                            onSubmitStudentCheckIn = { stu, st, sess, loc, rem ->
                                                viewModel.submitStudentCheckIn(stu, selectedDateKey, st, sess, loc, rem)
                                            },
                                            onNavigateToAcademics = { navigateToTab(ROUTE_ACADEMICS) }
                                        )
                                    }
                                    composable(ROUTE_SCHEDULE) {
                                        ScheduleScreen(
                                            data = data,
                                            selectedDay = selectedScheduleDay,
                                            onSelectDay = { selectedScheduleDay = it },
                                            selectedClassId = selectedClassId,
                                            onSelectClassId = { viewModel.selectClassFilter(it) },
                                            onAddPeriod = { activeDialog = ActiveDialog.EditSchedulePeriod(null) },
                                            onEditPeriod = { activeDialog = ActiveDialog.EditSchedulePeriod(it) },
                                            onDeletePeriod = { viewModel.deleteSchedulePeriod(it) },
                                            onAddClass = { activeDialog = ActiveDialog.AddClass }
                                        )
                                    }
                                    composable(ROUTE_ACADEMICS) {
                                        AcademicsScreen(
                                            data = data,
                                            activeTab = academicsTab,
                                            onSelectTab = { academicsTab = it },
                                            searchQuery = studentSearchQuery,
                                            onSearchQueryChange = { viewModel.setStudentSearchQuery(it) },
                                            selectedClassId = selectedClassId,
                                            onSelectClassId = { viewModel.selectClassFilter(it) },
                                            onAddClass = { activeDialog = ActiveDialog.AddClass },
                                            onDeleteClass = { viewModel.deleteClass(it) },
                                            onViewClassSchedule = { classId ->
                                                viewModel.selectClassFilter(classId)
                                                navigateToTab(ROUTE_SCHEDULE)
                                            },
                                            onAddStudent = { activeDialog = ActiveDialog.EditStudent(null) },
                                            onEditStudent = { activeDialog = ActiveDialog.EditStudent(it) },
                                            onDeleteStudent = { viewModel.deleteStudent(it) },
                                            selectedGradeStudentId = selectedGradeStudentId,
                                            onSelectGradeStudentId = { selectedGradeStudentId = it },
                                            onAddGrade = { activeDialog = ActiveDialog.AddGrade },
                                            onDeleteGrade = { viewModel.deleteExamGrade(it) }
                                        )
                                    }
                                    composable(ROUTE_CAMPUS) {
                                        CampusScreen(
                                            data = data,
                                            activeTab = campusTab,
                                            onSelectTab = { campusTab = it },
                                            onAddFacility = { activeDialog = ActiveDialog.AddFacility },
                                            onBookFacility = { activeDialog = ActiveDialog.BookFacility(it) },
                                            onDeleteFacility = { viewModel.deleteFacility(it) },
                                            onAddFaculty = { activeDialog = ActiveDialog.AddFaculty },
                                            onToggleFacultyStatus = { viewModel.toggleFacultyStatus(it) },
                                            onDeleteFaculty = { viewModel.deleteFaculty(it) },
                                            onAddFeeInvoice = { activeDialog = ActiveDialog.AddFeeInvoice },
                                            onRecordPayment = { activeDialog = ActiveDialog.RecordPayment(it) },
                                            onDeleteFeeInvoice = { viewModel.deleteFeeInvoice(it) },
                                            onAddNotice = { activeDialog = ActiveDialog.AddNotice },
                                            onToggleNoticePin = { viewModel.toggleNoticePin(it) },
                                            onDeleteNotice = { viewModel.deleteNotice(it) }
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

    if (currentData != null) {
        when (val dlg = activeDialog) {
            ActiveDialog.EditProfile -> SchoolProfileDialog(
                initial = currentData.profile,
                onDismiss = { activeDialog = null },
                onSave = { name, year, principal, motto ->
                    viewModel.saveSchoolProfile(name, year, principal, motto)
                }
            )
            ActiveDialog.AddClass -> AddClassDialog(
                onDismiss = { activeDialog = null },
                onSave = { name, grade, room, teacher, sched, cap ->
                    viewModel.addClass(name, grade, room, teacher, sched, cap)
                }
            )
            is ActiveDialog.EditStudent -> StudentDialog(
                initial = dlg.student,
                classes = currentData.classes,
                onDismiss = { activeDialog = null },
                onSave = { id, name, roll, cls, gName, gPhone, gEmail, status, notes ->
                    viewModel.saveStudent(id, name, roll, cls, gName, gPhone, gEmail, status, notes)
                }
            )
            is ActiveDialog.AttendanceRemark -> AttendanceRemarkDialog(
                student = dlg.student,
                initialStatus = dlg.status,
                initialRemarks = dlg.remarks,
                onDismiss = { activeDialog = null },
                onSave = { st, rem ->
                    viewModel.markAttendance(dlg.student, selectedDateKey, st, rem)
                }
            )
            is ActiveDialog.EditSchedulePeriod -> SchedulePeriodDialog(
                initial = dlg.period,
                classes = currentData.classes,
                faculty = currentData.faculty,
                defaultDay = selectedScheduleDay,
                defaultClassId = selectedClassId,
                onDismiss = { activeDialog = null },
                onSave = { id, cls, day, periodNum, subj, start, end, facId, facName, room, notes ->
                    selectedScheduleDay = day
                    viewModel.saveSchedulePeriod(
                        existingId = id,
                        schoolClass = cls,
                        day = day,
                        periodNumber = periodNum,
                        subject = subj,
                        startTime = start,
                        endTime = end,
                        facultyId = facId,
                        facultyName = facName,
                        roomNumber = room,
                        topicNotes = notes
                    )
                }
            )
            ActiveDialog.AddFaculty -> AddFacultyDialog(
                onDismiss = { activeDialog = null },
                onSave = { name, dept, subj, cls, email, phone ->
                    viewModel.addFaculty(name, dept, subj, cls, email, phone)
                }
            )
            ActiveDialog.AddFacility -> AddFacilityDialog(
                onDismiss = { activeDialog = null },
                onSave = { name, cat, bldg, cap, notes ->
                    viewModel.addFacility(name, cat, bldg, cap, notes)
                }
            )
            is ActiveDialog.BookFacility -> BookFacilityDialog(
                facility = dlg.facility,
                onDismiss = { activeDialog = null },
                onSave = { st, by, slot ->
                    viewModel.updateFacilityBooking(dlg.facility.id, st, by, slot)
                }
            )
            ActiveDialog.AddFeeInvoice -> AddFeeInvoiceDialog(
                students = currentData.students,
                onDismiss = { activeDialog = null },
                onSave = { stu, title, amt, due ->
                    viewModel.addFeeInvoice(stu, title, amt, due)
                }
            )
            is ActiveDialog.RecordPayment -> RecordFeePaymentDialog(
                invoice = dlg.invoice,
                onDismiss = { activeDialog = null },
                onPay = { amt -> viewModel.recordFeePayment(dlg.invoice, amt) }
            )
            ActiveDialog.AddNotice -> AddNoticeDialog(
                onDismiss = { activeDialog = null },
                onSave = { title, body, cat, aud, pinned ->
                    viewModel.addNotice(title, body, cat, aud, pinned)
                }
            )
            ActiveDialog.AddGrade -> AddExamGradeDialog(
                students = currentData.students,
                onDismiss = { activeDialog = null },
                onSave = { stu, subj, exam, score, max, term, type, notes ->
                    selectedGradeStudentId = stu.id
                    viewModel.addExamGrade(stu, subj, exam, score, max, term, type, notes)
                }
            )
            null -> Unit
        }
    }
}
