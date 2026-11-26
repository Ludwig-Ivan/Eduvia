-- EduvIA: metadatos descriptivos del esquema. Esta migración solo añade COMMENT ON;
-- no crea, altera ni borra objetos, por lo que no cambia el comportamiento de la API.
-- Se versiona aparte para no modificar el checksum de V1__init_schema.sql (ya aplicada).
-- Los comentarios explican el origen de cada columna en los modelos de la app KMP y
-- las reglas del contrato que no son evidentes desde el nombre.

-- ---------------------------------------------------------------------------
-- Identidad y autenticación
-- ---------------------------------------------------------------------------

COMMENT ON TABLE users IS 'Cuentas de la comunidad escolar. role limita el alcance de los datos: TEACHER imparte grupos y PARENT tutela alumnos. Espejo de model/User.kt y model/RegisterModels.kt de la app.';
COMMENT ON COLUMN users.id IS 'Identificador bigint; la app usa Long, por lo que no se usa UUID.';
COMMENT ON COLUMN users.full_name IS 'Nombre completo; la app deriva firstName de este campo para mostrar el saludo.';
COMMENT ON COLUMN users.email IS 'Correo normalizado a minusculas y unico; duplicado responde 409 en registro.';
COMMENT ON COLUMN users.phone IS 'Telefono normalizado a solo digitos y unico; se comprueba en el paso de validacion de la app.';
COMMENT ON COLUMN users.role IS 'Rol del contrato: PARENT o TEACHER. No existe ADMIN (ver OPEN-QUESTIONS Q9).';
COMMENT ON COLUMN users.password_hash IS 'Hash bcrypt con prefijo {bcrypt}; nunca se expone en UserDto.';
COMMENT ON COLUMN users.created_at IS 'Auditoria de creacion; el backend la gestiona con now().';
COMMENT ON COLUMN users.updated_at IS 'Auditoria de ultima modificacion.';

COMMENT ON TABLE refresh_tokens IS 'Tokens de renovacion. La rotacion revoca el token anterior y guarda el nuevo hash; el access token dura 900 s y el refresh 7 dias.';
COMMENT ON COLUMN refresh_tokens.id IS 'Identificador de la fila del token.';
COMMENT ON COLUMN refresh_tokens.user_id IS 'Cuenta propietaria del token; ON DELETE CASCADE al borrar el usuario.';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 del refresh token; se guarda el hash para no persistir el secreto en claro.';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Fecha de caducidad; un token vencido responde 401 y WWW-Authenticate: Bearer.';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Marca de revocacion; NULL significa que todavia es valido.';
COMMENT ON COLUMN refresh_tokens.created_at IS 'Auditoria de emision del refresh token.';

COMMENT ON TABLE password_reset_codes IS 'Codigos de recuperacion de 6 digitos, 10 minutos de vigencia, 5 intentos y un solo uso; un codigo nuevo invalida los anteriores.';
COMMENT ON COLUMN password_reset_codes.id IS 'Identificador de la fila del codigo.';
COMMENT ON COLUMN password_reset_codes.user_id IS 'Cuenta a la que pertenece el codigo.';
COMMENT ON COLUMN password_reset_codes.code IS 'Codigo numerico de 6 digitos; en perfil dev se devuelve en resetCode para la bandeja demo de la app.';
COMMENT ON COLUMN password_reset_codes.expires_at IS 'Instante en que deja de ser valido (10 minutos).';
COMMENT ON COLUMN password_reset_codes.attempts_left IS 'Intentos restantes; se decrementa en cada uso fallido y nunca baja de cero.';
COMMENT ON COLUMN password_reset_codes.used_at IS 'Instante de consumo; un codigo usado responde 400 si se reutiliza.';
COMMENT ON COLUMN password_reset_codes.created_at IS 'Auditoria de generacion del codigo.';

-- ---------------------------------------------------------------------------
-- Grupos y alumnos
-- ---------------------------------------------------------------------------

COMMENT ON TABLE school_groups IS 'Grupos escolares creados por un docente; el code XX-YYYY se busca sin distinguir mayusculas desde GET /groups/by-code. Espejo de model/SchoolGroup.kt.';
COMMENT ON COLUMN school_groups.id IS 'Identificador del grupo usado como groupId en tareas, juntas y calendario.';
COMMENT ON COLUMN school_groups.name IS 'Nombre visible del grupo, por ejemplo 3° B.';
COMMENT ON COLUMN school_groups.school IS 'Escuela a la que pertenece el grupo.';
COMMENT ON COLUMN school_groups.subject IS 'Materia opcional del grupo; string vacio cuando no aplica.';
COMMENT ON COLUMN school_groups.code IS 'Codigo de clase XX-YYYY, unico y buscado con lower(code). Lo muestra GroupsReadyScreen.kt.';
COMMENT ON COLUMN school_groups.teacher_id IS 'Docente propietario; ON DELETE RESTRICT evita borrar un usuario con grupos activos.';
COMMENT ON COLUMN school_groups.student_count IS 'Contador cacheado de alumnos. Se actualiza en students:link para no repetir count(*) en cada listado; es derivado-but-cached (ver data-model.md).';
COMMENT ON COLUMN school_groups.created_at IS 'Auditoria de creacion del grupo.';
COMMENT ON COLUMN school_groups.updated_at IS 'Auditoria de ultima modificacion del grupo.';

COMMENT ON TABLE students IS 'Alumnos de un grupo, opcionalmente vinculados a un tutor. Espejo de model/User.kt (Student).';
COMMENT ON COLUMN students.id IS 'Identificador del alumno; aparece en deliveredStudentIds y en las respuestas de junta.';
COMMENT ON COLUMN students.name IS 'Nombre visible; la asistencia deriva familyName de este valor.';
COMMENT ON COLUMN students.group_id IS 'Grupo al que pertenece; ON DELETE CASCADE al borrar el grupo.';
COMMENT ON COLUMN students.parent_id IS 'Tutor opcional. NULL en BD equivale a parentId=0 en la app; el mapper de ambos lados hace la conversion.';
COMMENT ON COLUMN students.created_at IS 'Auditoria de alta del alumno.';
COMMENT ON COLUMN students.updated_at IS 'Auditoria de ultima modificacion del alumno.';

-- ---------------------------------------------------------------------------
-- Tareas y entregas
-- ---------------------------------------------------------------------------

COMMENT ON TABLE school_tasks IS 'Tareas publicadas por cada docente. Espejo de model/SchoolTask.kt; deliveredStudentIds no es columna, vive en task_deliveries.';
COMMENT ON COLUMN school_tasks.id IS 'Identificador de la tarea. El evento de calendario que genera usa este mismo id.';
COMMENT ON COLUMN school_tasks.group_id IS 'Grupo destinatario; GET /tasks exige este filtro.';
COMMENT ON COLUMN school_tasks.title IS 'Titulo de la tarea, validado como no vacio. La app muestra ademas un displayTitle calculado.';
COMMENT ON COLUMN school_tasks.subject IS 'Materia del enum Subject: MATEMATICAS, ESPANOL, CIENCIAS, HISTORIA u OTRA.';
COMMENT ON COLUMN school_tasks.description IS 'Descripcion larga; text vacio por defecto.';
COMMENT ON COLUMN school_tasks.due_date IS 'Fecha de entrega en YYYY-MM-DD (LocalDate, sin hora).';
COMMENT ON COLUMN school_tasks.published_date IS 'Fecha de publicacion en YYYY-MM-DD; no se exige que sea anterior a due_date.';
COMMENT ON COLUMN school_tasks.teacher_name IS 'Nombre del docente que publica; se muestra en el detalle sin hacer join.';
COMMENT ON COLUMN school_tasks.total_students IS 'Numero de alumnos del grupo al publicar; la app calcula missingCount y deliveredFraction a partir de el.';
COMMENT ON COLUMN school_tasks.has_photo IS 'Indica si la tarea incluye foto; la app lo usa como distintivo visual.';
COMMENT ON COLUMN school_tasks.created_at IS 'Auditoria de creacion de la tarea.';
COMMENT ON COLUMN school_tasks.updated_at IS 'Auditoria de ultima modificacion de la tarea.';

COMMENT ON TABLE task_deliveries IS 'Entregas por alumno. La PK compuesta hace idempotente marcar dos veces la misma entrega (T6).';
COMMENT ON COLUMN task_deliveries.task_id IS 'Tarea entregada; primera columna de la PK compuesta.';
COMMENT ON COLUMN task_deliveries.student_id IS 'Alumno que entrega; segunda columna de la PK compuesta y indice inverso para consultas por alumno.';
COMMENT ON COLUMN task_deliveries.delivered_at IS 'Instante en que se registra la entrega; no se expone en el DTO de la app.';

-- ---------------------------------------------------------------------------
-- Juntas y respuestas
-- ---------------------------------------------------------------------------

COMMENT ON TABLE meetings IS 'Juntas de padres y docentes. Espejo de model/MeetingModels.kt; isNext, relativeLabel y response son derivados y no se almacenan.';
COMMENT ON COLUMN meetings.id IS 'Identificador de la junta; su evento de calendario usa 1000 + este valor para no colisionar con las tareas.';
COMMENT ON COLUMN meetings.group_id IS 'Grupo convocado; forma parte del indice unico de fecha, hora y lugar.';
COMMENT ON COLUMN meetings.title IS 'Titulo de la junta, validado como no vacio.';
COMMENT ON COLUMN meetings.date IS 'Fecha de la junta en YYYY-MM-DD; ordena GET /meetings.';
COMMENT ON COLUMN meetings.time IS 'Hora en texto libre (por ejemplo 10:00 am); se normaliza para detectar duplicados.';
COMMENT ON COLUMN meetings.location IS 'Lugar de la junta; con groupId, date y time forman uk_meetings_unique (409).';
COMMENT ON COLUMN meetings.has_virtual_link IS 'Derivado de virtual_link; se expone para que la app no tenga que comprobar el texto.';
COMMENT ON COLUMN meetings.virtual_link IS 'Enlace virtual opcional; debe empezar por http o https.';
COMMENT ON COLUMN meetings.reminder_enabled IS 'Activa los recordatorios de pending families (M8); default true.';
COMMENT ON COLUMN meetings.created_at IS 'Auditoria de creacion de la junta.';
COMMENT ON COLUMN meetings.updated_at IS 'Auditoria de ultima modificacion, incluida la edicion M9.';

COMMENT ON TABLE meeting_responses IS 'Respuesta de cada familia a cada junta. La asistencia agregada se calcula aqui y nunca se almacena.';
COMMENT ON COLUMN meeting_responses.meeting_id IS 'Junta respondida; primera columna de la PK compuesta.';
COMMENT ON COLUMN meeting_responses.student_id IS 'Alumno cuyo tutor responde; segunda columna de la PK compuesta.';
COMMENT ON COLUMN meeting_responses.response IS 'Enum MeetingResponse: PENDING, ATTENDING o DECLINED.';
COMMENT ON COLUMN meeting_responses.updated_at IS 'Instante de la ultima respuesta; permite sobrescribir la anterior.';

-- ---------------------------------------------------------------------------
-- Calendario unificado
-- ---------------------------------------------------------------------------

COMMENT ON TABLE calendar_events IS 'Eventos unificados de la app. Una tarea usa su propio id; una junta usa 1000 + meetings.id (MEETING_ID_OFFSET) porque en la app original las dos listas tenian ids solapados. Los eventos EXAM los crea directamente la app.';
COMMENT ON COLUMN calendar_events.id IS 'PK compartida con la tarea o desplazada 1000 respecto a la junta; los eventos nuevos toman calendar_events_id_seq.';
COMMENT ON COLUMN calendar_events.group_id IS 'Grupo al que pertenece el evento; filtra la vista del calendario.';
COMMENT ON COLUMN calendar_events.date IS 'Fecha del evento en YYYY-MM-DD.';
COMMENT ON COLUMN calendar_events.type IS 'Enum CalendarEventType: TASK, MEETING o EXAM.';
COMMENT ON COLUMN calendar_events.title IS 'Titulo mostrado en el calendario; al editar una junta se actualiza desde M9.';
COMMENT ON COLUMN calendar_events.subtitle IS 'Texto secundario: la hora en las juntas. Para tareas y examenes puede estar vacio.';
COMMENT ON COLUMN calendar_events.created_at IS 'Auditoria de creacion del evento.';
COMMENT ON COLUMN calendar_events.updated_at IS 'Auditoria de ultima modificacion del evento.';

-- ---------------------------------------------------------------------------
-- Indices: se documenta la consulta que justifica cada uno
-- ---------------------------------------------------------------------------

COMMENT ON INDEX idx_refresh_tokens_user IS 'Limpieza y busqueda de tokens por usuario.';
COMMENT ON INDEX idx_password_reset_codes_user IS 'Invalidar codigos anteriores de un usuario al generar uno nuevo.';
COMMENT ON INDEX idx_school_groups_teacher IS 'Filtro teacherId de GET /groups.';
COMMENT ON INDEX idx_school_groups_code_lower IS 'Busqueda case-insensitive de GET /groups/by-code.';
COMMENT ON INDEX idx_students_group IS 'GET /groups/{id}/students y calculo de asistencia.';
COMMENT ON INDEX idx_students_parent IS 'GET /students?parentId=.';
COMMENT ON INDEX idx_school_tasks_group_due IS 'GET /tasks?groupId= ordenado por fecha de entrega.';
COMMENT ON INDEX idx_task_deliveries_student IS 'Consulta inversa de entregas por alumno.';
COMMENT ON INDEX idx_meetings_group_date IS 'Ordenacion de GET /meetings por grupo y fecha.';
COMMENT ON INDEX uk_meetings_unique IS 'Unicidad funcional de junta por grupo, fecha, hora y lugar (case-insensitive).';
COMMENT ON INDEX idx_meeting_responses_student IS 'GET /meetings/responses?studentId=.';
COMMENT ON INDEX idx_calendar_events_group_date IS 'GET /calendar/events por grupo y fecha.';
