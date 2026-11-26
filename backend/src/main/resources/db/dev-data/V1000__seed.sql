-- Datos de ejemplo para el perfil dev. SOLO se cargan con SPRING_PROFILES_ACTIVE=dev.
-- Espejo de SchoolSampleData / MeetingSampleData / CalendarSampleData de la app KMP.
-- Contraseñas: bcrypt(10) de "Maestra123" y "Padre1234".

insert into users (id, full_name, email, phone, role, password_hash) values
    (1, 'Laura Hernández', 'laura@eduvia.mx', '5512345678', 'TEACHER', '{bcrypt}$2b$10$IIclsfZKcwe8PsJVJcJIqefWN2JDFQve7Vi926zLxqhktevoUpG6i'),
    (2, 'María Pérez',     'maria@correo.com','5598765432', 'PARENT',  '{bcrypt}$2b$10$CeNdEziOxNUheWq7wCt9du2lUkEbcL9BVLbMAnlQ3fWgT2J9AJGMy');
select setval(pg_get_serial_sequence('users', 'id'), (select max(id) from users));

insert into school_groups (id, name, school, subject, code, teacher_id, student_count) values
    (1, '3° B', 'Escuela Primaria Benito Juárez', 'Matemáticas', '3B-7K2P', 1, 28),
    (2, '2° A', 'Escuela Primaria Benito Juárez', '',            '2A-9M4X', 1, 0),
    -- Grupo de los alumnos 100/101 (en la app estaba comentado; aquí se activa para satisfacer la FK).
    (3, '2° B', 'Secundaria Técnica',               'Geografía',  '2B-4W7D', 1, 2);
select setval(pg_get_serial_sequence('school_groups', 'id'), (select max(id) from school_groups));

-- 28 alumnos del grupo 1 (el primero es hijo de María) + 2 alumnos del grupo 3 (demo de código compartido).
insert into students (id, name, group_id, parent_id)
select i, case when i = 1 then 'Sofía Pérez' else 'Alumno ' || (i + 1) end, 1, case when i = 1 then 2 else null end
from generate_series(1, 28) as i;

insert into students (id, name, group_id, parent_id) values
    (100, 'Luis Gómez', 3, null),
    (101, 'Ana Gómez',  3, null);
select setval(pg_get_serial_sequence('students', 'id'), (select max(id) from students));

insert into school_tasks (id, group_id, title, subject, description, due_date, published_date, teacher_name, total_students, has_photo) values
    (1, 1, 'Resolver página 45', 'MATEMATICAS', 'Ejercicios 1 al 10. Traer regla y lápiz de colores para la actividad del final.', '2026-10-12', '2026-10-09', 'Laura', 28, true),
    (2, 1, 'Ciclo del agua',     'CIENCIAS',    'Dibujo con foto.',                                                           '2026-10-13', '2026-10-09', 'Laura', 28, false),
    (3, 1, 'Lectura cap. 4',     'ESPANOL',     'Leer el capítulo 4 y escribir un resumen.',                                  '2026-10-15', '2026-10-10', 'Laura', 28, false),
    (4, 1, 'Mapa de México',     'HISTORIA',    'Mapa con los estados y sus capitales.',                                      '2026-10-08', '2026-10-05', 'Laura', 28, false),
    (5, 1, 'Pág. 38',            'MATEMATICAS', 'Problemas de la página 38.',                                                 '2026-10-06', '2026-10-02', 'Laura', 28, false),
    (6, 1, 'Lectura cap. 3',     'ESPANOL',     'Resumen del capítulo 3.',                                                    '2026-10-02', '2026-09-28', 'Laura', 28, false);
select setval(pg_get_serial_sequence('school_tasks', 'id'), (select max(id) from school_tasks));

-- Entregas: task 1 -> alumnos 2..23; task 2 -> 2..15; task 3 -> 2..4; task 4 -> 1..23; task 5 -> 1..28; task 6 -> 2..27.
insert into task_deliveries (task_id, student_id)
select 1, i from generate_series(2, 23) i
union all select 2, i from generate_series(2, 15) i
union all select 3, i from generate_series(2, 4) i
union all select 4, i from generate_series(1, 23) i
union all select 5, i from generate_series(1, 28) i
union all select 6, i from generate_series(2, 27) i;

insert into meetings (id, group_id, title, date, time, location, has_virtual_link, virtual_link, reminder_enabled) values
    (1, 1, 'Junta de padres',    '2026-10-15', '8:00 am', 'Salón 3B', true, 'https://meet.example.com/eduvia', true),
    (2, 1, 'Entrega de boletas', '2026-10-29', '9:00 am', 'Dirección', false, null, true);
select setval(pg_get_serial_sequence('meetings', 'id'), (select max(id) from meetings));

-- Asistencia demo: junta 1 -> 18 ATTENDING, 8 DECLINED, resto PENDING; junta 2 -> 2 ATTENDING.
insert into meeting_responses (meeting_id, student_id, response)
select 1, i, case when i <= 18 then 'ATTENDING' when i <= 26 then 'DECLINED' else 'PENDING' end
from generate_series(1, 28) i
union all
select 2, i, case when i <= 2 then 'ATTENDING' else 'PENDING' end
from generate_series(1, 28) i;

-- Eventos de calendario con el MISMO id que su tarea/junta (regla de negocio de la app).
insert into calendar_events (id, group_id, date, type, title, subtitle)
select id, group_id, due_date, 'TASK',
       case subject when 'MATEMATICAS' then 'Matemáticas' when 'ESPANOL' then 'Español'
                    when 'CIENCIAS' then 'Ciencias' when 'HISTORIA' then 'Historia' else 'Otra' end || ' · ' || title,
       case subject when 'MATEMATICAS' then 'Matemáticas' when 'ESPANOL' then 'Español'
                    when 'CIENCIAS' then 'Ciencias' when 'HISTORIA' then 'Historia' else 'Otra' end
from school_tasks;

-- Los eventos de junta usan 1000 + meeting.id para no colisionar con los ids de tareas
-- (la app mantenía dos listas separadas; en una sola tabla hay que desambiguar).
insert into calendar_events (id, group_id, date, type, title, subtitle)
select 1000 + id, group_id, date, 'MEETING', title, time || ' · ' || location from meetings;

-- Exámenes de ejemplo y eventos de relleno (ids altos para no colisionar).
insert into calendar_events (id, group_id, date, type, title, subtitle) values
    (50, 1, '2026-10-07', 'EXAM', 'Examen de matemáticas', '10:00 am · Salón 2A'),
    (51, 1, '2026-10-21', 'EXAM', 'Examen de ciencias',    '10:00 am · Salón 2A');
insert into calendar_events (id, group_id, date, type, title, subtitle)
select 100 + d, 1, make_date(2026, 10, d), 'TASK', 'Tarea del día ' || d, 'Pendiente'
from (values (2),(5),(6),(9),(14),(16),(19),(20),(22),(23),(26),(27),(30)) as x(d);
select setval('calendar_events_id_seq', (select max(id) from calendar_events));
