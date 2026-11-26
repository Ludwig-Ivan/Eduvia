/**
 * Equivalente al `conftest.py` de pytest para los tests remotos: centraliza el `MockEngine`,
 * los builders de DTOs y el JSON del cliente usados por la capa de red.
 */
package com.example.librerias.data.remote

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.ContentType

/** Crea un `ApiClient` aislado con `MockEngine` y una sesión nueva para cada prueba. */
internal fun testApi(handler: MockRequestHandler): ApiClient =
    ApiClient("http://test/api/v1/", SessionStore(), io.ktor.client.engine.mock.MockEngine(handler))

internal fun MockRequestHandleScope.jsonResponse(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) = respond(
    content = content,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)

internal const val LOGIN_JSON = """{
  "user": {"id":1,"fullName":"Laura Martínez","email":"laura@eduvia.mx","phone":"5512345678","role":"TEACHER"},
  "createdGroups": [],
  "accessToken": "access-1",
  "refreshToken": "refresh-1",
  "expiresIn": 900
}"""

internal const val GROUP_JSON = """{
  "id": 1, "name": "Matemáticas 6A", "school": "Escuela Primaria", "subject": "MATEMATICAS",
  "code": "MA-4K7P", "teacherId": 1, "studentCount": 10
}"""

internal const val TASK_JSON = """{
  "id": 5, "groupId": 1, "title": "Fracciones", "subject": "MATEMATICAS",
  "description": "Ejercicios del tema 4", "dueDate": "2026-12-15", "publishedDate": "2026-11-20",
  "teacherName": "Laura Martínez", "totalStudents": 10, "hasPhoto": false,
  "deliveredStudentIds": [7, 8], "status": "PENDING"
}"""

internal const val MEETING_JSON = """{
  "id": 3, "groupId": 1, "title": "Junta de padres", "date": "2026-12-10", "time": "8:00",
  "location": "Aula 1", "hasVirtualLink": false, "isNext": false, "relativeLabel": null,
  "response": "ATTENDING", "virtualLink": null, "reminderEnabled": true
}"""

internal const val CALENDAR_PAGE_JSON = """{
  "items": [
    {"id":5,"groupId":1,"date":"2026-12-15","type":"TASK","title":"Fracciones","subtitle":"MATEMATICAS","attendance":null},
    {"id":1003,"groupId":1,"date":"2026-12-10","type":"MEETING","title":"Junta de padres","subtitle":"8:00",
     "attendance":{"confirmed":3,"declined":1,"pending":6}}
  ],
  "page": 0, "size": 50, "totalElements": 2, "totalPages": 1
}"""

internal const val PROBLEM_JSON = """{
  "type": "about:blank", "title": "Not Found", "status": 404,
  "detail": "Grupo no encontrado", "instance": "/api/v1/groups/999", "errors": []
}"""

internal fun pageOf(items: String, total: Int = 1) = """{"items":[$items],"page":0,"size":50,"totalElements":$total,"totalPages":1}"""
