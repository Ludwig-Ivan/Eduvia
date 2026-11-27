package com.example.librerias.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.librerias.di.AppContainer
import com.example.librerias.model.CalendarEventType
import com.example.librerias.model.MEETING_ID_OFFSET
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.RegistrationResult
import com.example.librerias.model.SchoolTask
import com.example.librerias.model.UserRole
import com.example.librerias.ui.auth.ForgotPasswordScreen
import com.example.librerias.ui.auth.GroupsReadyScreen
import com.example.librerias.ui.auth.LoginScreen
import com.example.librerias.ui.auth.RegisterScreen
import com.example.librerias.ui.auth.WelcomeScreen
import com.example.librerias.ui.calendar.CalendarScreen
import com.example.librerias.ui.calendar.CalendarViewModel
import com.example.librerias.ui.calendar.TeacherCalendarScreen
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.home.ParentHomeScreen
import com.example.librerias.ui.home.TeacherHomeScreen
import com.example.librerias.ui.meetings.CreateMeetingScreen
import com.example.librerias.ui.meetings.MeetingsScreen
import com.example.librerias.ui.meetings.MeetingsViewModel
import com.example.librerias.ui.meetings.TeacherMeetingsScreen
import com.example.librerias.ui.tasks.CreateTaskScreen
import com.example.librerias.ui.tasks.ParentTasksScreen
import com.example.librerias.ui.tasks.TaskDetailScreen
import com.example.librerias.ui.tasks.TasksViewModel
import com.example.librerias.ui.tasks.TeacherTasksScreen
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.example.librerias.ui.auth.LoginViewModel
/**
 * Nodo raíz de navegación: elige las pantallas según la sesión y el rol del usuario.
 *
 * Sin sesión: `WelcomeScreen` -> `LoginScreen` / `RegisterScreen` / `ForgotPasswordScreen`, y
 * `GroupsReadyScreen` justo después de registrar a un maestro. `LoginViewModel` se crea aquí
 * desde el contenedor y aporta el error, la carga y el bloqueo temporal del login.
 *
 * Con sesión: resuelve el grupo (maestro) o el hijo (padre) activo y muestra una de las cuatro
 * rutas de la barra inferior:
 * - HOME: resumen de juntas y tareas; el maestro usa `MeetingsViewModel` para responder,
 *   el padre navega al detalle de tareas o a la pestaña de juntas.
 * - TASKS: listado por rol; las acciones de crear, editar, borrar y marcar como entregada
 *   pasan por `TasksViewModel`.
 * - CALENDAR: agenda por rol; `CalendarViewModel` aporta los eventos y, en las juntas, se resta
 *   `MEETING_ID_OFFSET` (1000) para volver al id de la junta antes de editar o responder.
 * - MEETINGS: juntas y asistencia; `MeetingsViewModel` gestiona respuestas, creación, borrado
 *   y recordatorios.
 *
 * Las pantallas de crear/editar junta o tarea y el detalle de tarea se muestran como vistas
 * temporales por encima de la pestaña activa. Los ViewModels se crean con claves por grupo y
 * hijo para que cada selección tenga su propio estado; las pantallas solo reciben estados y
 * callbacks, y todos los datos remotos llegan de los casos de uso de [AppContainer].
 */
@Composable
fun AppNavHost(container: AppContainer) {
    val currentUser by container.observeSession().collectAsState()
    val scope = rememberCoroutineScope()

    var showWelcome by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    var showCreateMeeting by remember { mutableStateOf(false) }
    var meetingToEdit by remember { mutableStateOf<Meeting?>(null) }
    var showCreateTask by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<SchoolTask?>(null) }
    var taskDetailId by remember { mutableStateOf<Long?>(null) }
    var showRegister by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var teacherRegistrationResult by remember { mutableStateOf<RegistrationResult?>(null) }

    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var selectedChildId by remember { mutableStateOf<Long?>(null) }
    var fechaParaNuevaJunta by remember { mutableStateOf("") }

    var reminderMessage by remember { mutableStateOf<String?>(null) }

// El aviso verde se quita solo a los 3 segundos
    LaunchedEffect(reminderMessage) {
        if (reminderMessage != null) {
            delay(3000)
            reminderMessage = null
        }
    }

    if (teacherRegistrationResult != null) {
        GroupsReadyScreen(
            result = teacherRegistrationResult,
            onGoHome = {
                teacherRegistrationResult = null
                currentTab = BottomTab.HOME
            }
        )
        return
    }

    if (currentUser == null) {
        if (showWelcome) {
            WelcomeScreen(onStartClick = { showWelcome = false })
        } else {
            when {
                showRegister -> RegisterScreen(
                    container = container,
                    onBack = { showRegister = false },
                    onLoginClick = { showRegister = false },
                    onSuccessParent = { showRegister = false },
                    onSuccessTeacher = { result ->
                        teacherRegistrationResult = result
                        showRegister = false
                    }
                )
                showForgotPassword -> ForgotPasswordScreen(
                    container = container,
                    onBack = { showForgotPassword = false },
                    onLoginClick = { showForgotPassword = false },
                    onDone = { showForgotPassword = false }
                )

                else -> {
                    val loginVm: LoginViewModel = viewModel { LoginViewModel(container) }
                    val loginError by loginVm.error.collectAsState()
                    val loginLoading by loginVm.isLoading.collectAsState()
                    val lockSeconds by loginVm.lockSeconds.collectAsState()

                    // Al abrir el login, no arrastres errores de una sesión anterior
                    LaunchedEffect(Unit) { loginVm.clearError() }

                    LoginScreen(
                        errorMessage = loginError,
                        isLoading = loginLoading,
                        lockSeconds = lockSeconds,
                        onClearError = { loginVm.clearError() },
                        onLogin = { email, pass -> loginVm.login(email, pass) },
                        onRegisterClick = { showRegister = true },
                        onForgotPasswordClick = { showForgotPassword = true },
                        onBackClick = { showWelcome = true }
                    )
                }
            }
        }
    } else {
        val user = currentUser!!
        val isTeacher = user.role == UserRole.TEACHER
        val today = container.todayProvider.today()

        val maestroGroups by container.observeGroups(user.id).collectAsState(initial = emptyList())
        val parentChildren by container.observeStudents(user.id).collectAsState(initial = emptyList())

        val currentGroup = maestroGroups.find { it.id == selectedGroupId } ?: maestroGroups.firstOrNull()
        val selectedChild = parentChildren.find { it.id == selectedChildId } ?: parentChildren.firstOrNull()

        val currentGroupId = if (isTeacher) (currentGroup?.id ?: 0L) else (selectedChild?.groupId ?: 0L)

        val groupName = currentGroup?.name ?: "Sin grupo"
        val groupCode = currentGroup?.code ?: "Sin código"
        val groupSize = currentGroup?.studentCount ?: 0

        // Para el padre, cada hijo tiene su propio ViewModel. El maestro usa "teacher".
        val childKey = if (isTeacher) "teacher" else "child_${selectedChild?.id}"

        val meetingsVm: MeetingsViewModel = viewModel(key = "meetings_${currentGroupId}_$childKey") {
            MeetingsViewModel(
                container,
                currentGroupId,
                studentId = if (isTeacher) null else selectedChild?.id
            )
        }
        val tasksVm: TasksViewModel = viewModel(key = "tasks_$currentGroupId") {
            TasksViewModel(container, currentGroupId, user)
        }
        val calendarVm: CalendarViewModel = viewModel(key = "calendar_${currentGroupId}_$childKey") {
            CalendarViewModel(container, user.role, currentGroupId, studentId = selectedChild?.id)
        }

        val meetings by meetingsVm.meetings.collectAsState()
        val tasks by tasksVm.tasks.collectAsState()
        val events by calendarVm.events.collectAsState()

        val meetingError by meetingsVm.creationError.collectAsState()
        val taskError by tasksVm.creationError.collectAsState()

        if (showCreateMeeting) {
            CreateMeetingScreen(
                meetingToEdit = meetingToEdit,
                initialDate = fechaParaNuevaJunta,
                errorMessage = meetingError,
                onClearError = { meetingsVm.clearCreationError() },
                onClose = {
                    meetingsVm.clearCreationError()
                    showCreateMeeting = false
                    meetingToEdit = null
                    fechaParaNuevaJunta = ""
                },
                onPublish = { title, date, time, place, link, reminder ->
                    val input = com.example.librerias.model.NewMeetingInput(title, date, time, place, link, reminder)
                    val onDone = {
                        showCreateMeeting = false
                        meetingToEdit = null
                        fechaParaNuevaJunta = ""
                    }
                    val editing = meetingToEdit
                    if (editing != null) meetingsVm.updateMeeting(editing, input, onDone)
                    else meetingsVm.createMeeting(input, onDone)
                }
            )
        } else if (showCreateTask) {
            CreateTaskScreen(
                taskToEdit = taskToEdit,
                errorMessage = taskError,
                onClearError = { tasksVm.clearCreationError() },
                onClose = {
                    tasksVm.clearCreationError()
                    showCreateTask = false
                    taskToEdit = null
                },
                onPublish = { title, subjectLabel, description, date ->
                    val subject = com.example.librerias.model.Subject.fromLabel(subjectLabel)
                    val editing = taskToEdit
                    if (editing != null) {
                        tasksVm.updateTask(editing, title, subject, description, date) {
                            showCreateTask = false; taskToEdit = null
                        }
                    } else {
                        val input = com.example.librerias.model.NewTaskInput(
                            title = title, subject = subject, description = description, dateText = date
                        )
                        tasksVm.createTask(input) { showCreateTask = false; taskToEdit = null }
                    }
                }
            )
        } else if (taskDetailId != null) {
            val selectedTask = tasks.find { it.id == taskDetailId }
            TaskDetailScreen(
                task = selectedTask,
                studentId = selectedChild?.id ?: -1L,
                onBack = { taskDetailId = null },
                onMarkAsDelivered = {
                    val child = selectedChild
                    if (selectedTask != null && child != null) {
                        tasksVm.markAsDelivered(selectedTask.id, child.id)
                    }
                    taskDetailId = null
                }
            )
        } else {
            when (currentTab) {
                BottomTab.HOME -> {
                    if (isTeacher) {
                        TeacherHomeScreen(
                            meetings = meetings,
                            tasks = tasks,
                            viewModel = meetingsVm,
                            userName = user.fullName,
                            groupName = groupName,
                            groupSize = groupSize,
                            groups = maestroGroups,
                            onGroupSelected = { selectedGroupId = it.id },
                            onTabSelected = { currentTab = it },
                            onCreateMeeting = { showCreateMeeting = true },
                            onLogoutClick = { container.logout() }
                        )
                    } else {
                        ParentHomeScreen(
                            tasks = tasks, meetings = meetings, today = today,
                            userName = user.fullName,
                            children = parentChildren,
                            selectedChildId = selectedChild?.id,
                            onChildSelected = { selectedChildId = it.id },
                            studentId = selectedChild?.id ?: -1L,
                            onTabSelected = { currentTab = it },
                            onTaskClick = { task -> taskDetailId = task.id },
                            onSeeAllTasksClick = { currentTab = BottomTab.TASKS },
                            onMeetingClick = { currentTab = BottomTab.MEETINGS },
                            onLogoutClick = { container.logout() }
                        )
                    }
                }
                BottomTab.TASKS -> {
                    if (isTeacher) {
                        TeacherTasksScreen(
                            tasks = tasks, today = today,
                            groupName = groupName,
                            groupSize = groupSize,
                            onTabSelected = { currentTab = it },
                            onCreateTask = { showCreateTask = true },
                            onEditTask = { taskToEdit = it; showCreateTask = true },
                            onDeleteTask = { task ->
                                tasksVm.deleteTask(task.id)
                                scope.launch {
                                    container.deleteEvent(task.id)
                                }
                            }
                        )
                    } else {
                        ParentTasksScreen(
                            tasks = tasks, today = today, userName = user.fullName,
                            children = parentChildren,
                            selectedChildId = selectedChild?.id,
                            onChildSelected = { selectedChildId = it.id },
                            studentId = selectedChild?.id ?: -1L,
                            onTabSelected = { currentTab = it },
                            onTaskClick = { task -> taskDetailId = task.id },
                            onLogoutClick = { container.logout() }
                        )
                    }
                }
                BottomTab.CALENDAR -> {
                    if (isTeacher) {
                        TeacherCalendarScreen(
                            events = events, today = today,
                            groupName = groupName,
                            groupCode = groupCode,
                            onTabSelected = { currentTab = it },
                            onCreate = { showCreateMeeting = true },
                            onCreateAtDate = { fechaSeleccionada ->
                                val dia = fechaSeleccionada.day.toString().padStart(2, '0')
                                val mes = fechaSeleccionada.month.toString().padStart(2, '0')
                                val anio = fechaSeleccionada.year
                                fechaParaNuevaJunta = "$dia/$mes/$anio"
                                showCreateMeeting = true
                            },
                            onEditEvent = { event ->
                                if (event.type == CalendarEventType.MEETING) {
                                    val meeting = meetings.find { it.id == event.id - MEETING_ID_OFFSET }
                                    if (meeting != null) { meetingToEdit = meeting; showCreateMeeting = true }
                                } else if (event.type == CalendarEventType.TASK) {
                                    val task = tasks.find { it.id == event.id }
                                    if (task != null) { taskToEdit = task; showCreateTask = true }
                                }
                            }
                        )
                    } else {
                        CalendarScreen(
                            events = events, meetings = meetings, today = today, userName = user.fullName,
                            children = parentChildren,
                            selectedChildId = selectedChild?.id,
                            onChildSelected = { selectedChildId = it.id },
                            onTabSelected = { currentTab = it },
                            onRsvp = { event, attending ->
                                val child = selectedChild
                                val realMeeting = meetings.find {
                                    it.id == if (event.type == CalendarEventType.MEETING) event.id - MEETING_ID_OFFSET else event.id
                                }
                                if (child != null && realMeeting != null) {
                                    meetingsVm.respondToMeeting(
                                        realMeeting.id,
                                        child.id,
                                        if (attending) MeetingResponse.ATTENDING else MeetingResponse.DECLINED
                                    )
                                }
                            },
                            onEventClick = { event ->
                                if (event.type == CalendarEventType.TASK) {
                                    // Solo abre la tarea si realmente existe (antes abría la primera por error)
                                    val tareaEncontrada = tasks.find { it.id == event.id }
                                        ?: tasks.find { it.title.contains(event.title) || event.title.contains(it.title) }
                                    if (tareaEncontrada != null) taskDetailId = tareaEncontrada.id
                                }
                            },
                            onLogoutClick = { container.logout() }
                        )
                    }
                }
                BottomTab.MEETINGS -> {
                    if (isTeacher) {
                        TeacherMeetingsScreen(
                            meetings = meetings,
                            viewModel = meetingsVm,
                            groupName = groupName,
                            groupCode = groupCode,
                            groupSize = groupSize,
                            message = reminderMessage,
                            onTabSelected = { currentTab = it },
                            onCreate = { showCreateMeeting = true },
                            onEditMeeting = { meetingToEdit = it; showCreateMeeting = true },
                            onRemind = { meeting ->
                                scope.launch {
                                    val n = container.remindPending(meeting.id)
                                    reminderMessage = when (n) {
                                        0 -> "Todas las familias ya respondieron."
                                        1 -> "Recordatorio enviado a 1 familia."
                                        else -> "Recordatorio enviado a $n familias."
                                    }
                                }
                            },
                            onDeleteMeeting = { meeting ->
                                // El backend borra el evento 1000+id en la misma transacción;
                                // RemoteMeetingRepository ya limpia la caché local del calendario.
                                meetingsVm.deleteMeeting(meeting.id)
                            }
                        )
                    } else {
                        MeetingsScreen(
                            meetings = meetings, userName = user.fullName,
                            children = parentChildren,
                            selectedChildId = selectedChild?.id,
                            onChildSelected = { selectedChildId = it.id },
                            onTabSelected = { currentTab = it },
                            onRespond = { meeting, response ->
                                selectedChild?.let { child ->
                                    meetingsVm.respondToMeeting(meeting.id, child.id, response)
                                }
                            },
                            onLogoutClick = { container.logout() }
                        )
                    }
                }
            }
        }
    }
}