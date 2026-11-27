package com.example.librerias.di

import com.example.librerias.data.DemoTodayProvider
import com.example.librerias.data.FakeEmailSender
import com.example.librerias.data.remote.ApiClient
import com.example.librerias.data.remote.RemoteAuthRepository
import com.example.librerias.data.remote.RemoteCalendarRepository
import com.example.librerias.data.remote.RemoteGroupRepository
import com.example.librerias.data.remote.RemoteErrorReporter
import com.example.librerias.data.remote.RemoteMeetingRepository
import com.example.librerias.data.remote.RemoteTaskRepository
import com.example.librerias.data.remote.SessionStore
import com.example.librerias.model.AppError
import com.example.librerias.domain.repository.*
import com.example.librerias.domain.usecase.*
import com.example.librerias.domain.util.TodayProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Contenedor manual de dependencias de la app KMP.
 *
 * Las implementaciones son las remotas de `data/remote` (capa de red Ktor): la UI consume
 * las interfaces de dominio y todos los datos llegan de la API, única fuente de datos.
 * No hay repositorios en memoria ni datos de ejemplo en la app.
 *
 * `FakeEmailSender` no envía correos: solo alimenta `demoInbox` con el código que devuelve
 * el backend en el perfil `dev` (OPEN-QUESTIONS Q5).
 */
class AppContainer {
    /** Fecha y hora de la demo que usan los casos de uso; ver `DemoTodayProvider` y Q7. */
    val todayProvider: TodayProvider = DemoTodayProvider()

    // Capa de red (Fase 5). La UI sigue consumiendo las mismas interfaces de dominio.
    /** Sesión remota: conserva el token que el cliente Ktor adjunta a cada petición. */
    val sessionStore = SessionStore()

    /** Cliente Ktor compartido por todos los repositorios remotos. */
    val apiClient = ApiClient(session = sessionStore)

    /** Canal compartido por las cachés para avisar de fallos de carga (API/RED apagadas). */
    private val errorReporter = RemoteErrorReporter()

    /** Último fallo de carga ya traducido a un mensaje seguro para la UI. */
    val loadError: Flow<AppError?> = errorReporter.lastError.map { error ->
        error?.let { apiClient.errorOf(it) }
    }

    /** Traduce una excepción de red a un error amigable; lo usan los ViewModels. */
    fun errorOf(error: Throwable): AppError = apiClient.errorOf(error)

    /** Igual que [errorOf], pero devuelve solo el texto listo para mostrar. */
    fun errorMessage(error: Throwable): String = apiClient.errorMessage(error)

    /** Cierra el aviso de conexión sin recargar (conserva el último listado conocido). */
    fun clearLoadError() = errorReporter.clear()

    /** Reintenta todas las cargas fallidas y limpia el aviso de conexión. */
    suspend fun retryLoads() {
        errorReporter.clear()
        remoteGroupRepository.refreshAll()
        remoteTaskRepository.refreshAll()
        remoteMeetingRepository.refreshAll()
        calendarRepository.refreshEvents()
    }

    /** Repositorio remoto de grupos, reutilizado también por juntas y tareas. */
    private val remoteGroupRepository = RemoteGroupRepository(apiClient, errorReporter)

    /** Interfaz de grupos inyectada: implementación activa, la remota Ktor. */
    private val groupRepository: GroupRepository = remoteGroupRepository

    /** Calendario remoto; coordina los eventos creados junto a tareas y juntas. */
    private val calendarRepository: RemoteCalendarRepository =
        RemoteCalendarRepository(apiClient, errorReporter)

    /** Repositorio concreto de tareas, conservado para el reintento de cargas. */
    private val remoteTaskRepository =
        RemoteTaskRepository(apiClient, calendarRepository, errorReporter)

    /** Interfaz de tareas inyectada: implementación activa, la remota Ktor. */
    private val taskRepository: TaskRepository = remoteTaskRepository

    /** Repositorio concreto de juntas: se expone como interfaz para las pantallas. */
    private val remoteMeetingRepository = RemoteMeetingRepository(
        apiClient,
        remoteGroupRepository,
        calendarRepository,
        errorReporter,
    )

    /** Repositorio de juntas: se expone porque las pantallas consultan también sus respuestas. */
    val meetingRepository: MeetingRepository = remoteMeetingRepository

    /** Interfaz de autenticación inyectada: implementación activa, la remota Ktor. */
    private val authRepository: AuthRepository = RemoteAuthRepository(apiClient, sessionStore)

    // El correo de recuperación sigue siendo simulado: el backend devuelve el código en dev
    // y este FakeEmailSender solo alimenta la bandeja de demostración de la app.
    private val fakeEmailSender = FakeEmailSender()

    /** Último correo simulado que la UI puede mostrar durante la recuperación de contraseña. */
    val demoInbox: StateFlow<String?> = fakeEmailSender.lastMessage
    fun clearDemoInbox() = fakeEmailSender.clear()

    // Auth
    /** Caso de uso que observa la sesión del usuario en la navegación raíz. */
    val observeSession = ObserveSessionUseCase(authRepository)

    /** Inicio de sesión usado por `LoginViewModel`. */
    val login = LoginUseCase(authRepository)

    /** Validación de los pasos del formulario de registro. */
    val validateAccountStep = ValidateAccountStepUseCase(authRepository)

    /** Registro de la cuenta y de los grupos creados por el maestro. */
    val register = RegisterUseCase(authRepository, groupRepository)

    /** Cierre de sesión invocado desde la cabecera de las pantallas. */
    val logout = LogoutUseCase(authRepository)

    /** Solicitud de código de recuperación: entrega el código al `fakeEmailSender` de demo. */
    val sendResetCode = SendResetCodeUseCase(authRepository, fakeEmailSender)

    /** Restablecimiento de contraseña con el código recibido. */
    val resetPassword = ResetPasswordUseCase(authRepository)

    // Grupos
    /** Grupos del maestro autenticado, usados para elegir el grupo activo. */
    val observeGroups = ObserveGroupsUseCase(groupRepository)

    /** Alumnos vinculados al padre/tutor, usados para elegir el hijo activo. */
    val observeStudents = ObserveStudentsUseCase(groupRepository)

    // Juntas
    /** Juntas del grupo/hijo activo con `isNext` y `relativeLabel` ya calculados. */
    val observeMeetings = ObserveMeetingsUseCase(meetingRepository, todayProvider)

    /** Registro de la respuesta (asistirá / no asistirá) de una familia. */
    val respondToMeeting = RespondToMeetingUseCase(meetingRepository)

    /** Respuestas de los alumnos que ve el maestro en el detalle de la junta. */
    val observeStudentResponses = ObserveStudentResponsesUseCase(meetingRepository)

    /** Creación de junta y de su evento de calendario asociado (offset 1000). */
    val createMeeting = CreateMeetingUseCase(meetingRepository, calendarRepository, todayProvider)

    /** Edición de junta y de su evento de calendario asociado. */
    val updateMeeting = UpdateMeetingUseCase(meetingRepository, calendarRepository, todayProvider)

    /** Resumen derivado de asistencia para el maestro. */
    val observeAttendance = ObserveAttendanceUseCase(meetingRepository)

    /** Borrado de la junta y de su evento de calendario en la API. */
    val deleteMeeting = DeleteMeetingUseCase(meetingRepository)

    /** Recordatorio a las familias que aún no respondieron. */
    val remindPending = RemindPendingFamiliesUseCase(meetingRepository)

    // Tareas
    /** Tareas del grupo activo que consume `TasksViewModel`. */
    val observeTasks = ObserveTasksUseCase(taskRepository)

    /** Marca una tarea como entregada para un alumno. */
    val markTaskDelivered = MarkTaskDeliveredUseCase(taskRepository)

    /** Creación de tarea y de su evento de calendario en la misma operación remota. */
    val createTask = CreateTaskUseCase(taskRepository, groupRepository, calendarRepository, todayProvider)

    /** Edición de una tarea existente. */
    val updateTask = UpdateTaskUseCase(taskRepository, todayProvider)

    /** Borrado de una tarea (el backend elimina su evento de calendario en la misma transacción). */
    val deleteTask = DeleteTaskUseCase(taskRepository)

    // Calendario
    /** Eventos del calendario del grupo/hijo activo que consume `CalendarViewModel`. */
    val observeCalendar = ObserveCalendarUseCase(calendarRepository)

    /** Borrado de eventos creados directamente desde el calendario. */
    val deleteEvent = DeleteEventUseCase(calendarRepository)
}