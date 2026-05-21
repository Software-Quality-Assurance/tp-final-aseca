# User Story 2.1 - Registro de usuarios

**Feature:** 2 - Identidad y acceso

**Tipo:** Backend

## Historia

**Como** usuario

**Quiero** registrarme con datos válidos, email y contraseña

**Para** poder crear una cuenta personal y acceder a la aplicación.

## Criterios de aceptación

1. Dado un visitante con datos válidos, cuando se registra, entonces el sistema crea el usuario y responde 201 Created.
2. Dado un registro exitoso, cuando se consulta la base de datos, entonces el usuario existe con un identificador único.
3. Dado un registro exitoso, cuando se almacena la contraseña, entonces queda guardada de forma hasheada.
4. Dado la respuesta de registro, cuando se devuelve el usuario, entonces no se expone contraseña ni hash.
5. Dado un email ya registrado, cuando otro usuario intenta registrarse con el mismo email, entonces el sistema responde 409 Conflict.
6. Dado un email existente con distinto uso de mayúsculas, cuando se intenta registrar nuevamente, entonces el sistema lo considera duplicado.
7. Dado un intento de registro inválido, cuando faltan campos obligatorios o el email tiene formato incorrecto entonces el sistema responde 400 Bad Request con mensajes claros.
