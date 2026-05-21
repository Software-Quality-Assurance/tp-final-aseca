# User Story 2.3 - Gestión segura de usuarios

**Feature:** 2 - Identidad y acceso

**Tipo:** Backend

## Historia

**Como** usuario

**Quiero** actualizar o eliminar mi cuenta sin exponer información sensible

**Para** mantener control sobre mis datos personales dentro del sistema.

## Criterios de aceptación

1. Dado un usuario autenticado, cuando actualiza datos válidos, entonces el sistema guarda los cambios.
2. Dado datos inválidos, cuando intenta actualizar, entonces el sistema responde 400 Bad Request.
3. Dado un usuario que intenta modificar o consultar datos de otro usuario, cuando envía la request, entonces el sistema rechaza la operación.
4. Dado un usuario que intenta modificar o consultar datos suyos, se utiliza el ID embebido del JWT token para hacer esas consultas
5. Dado una actualización exitosa, cuando se consulta el usuario, entonces se ven los datos actualizados.
6. Dado un usuario existente, cuando solicita eliminar su cuenta, entonces el sistema elimina la cuenta correctamente utilizando el ID embebido del JWT.
7. Dado un usuario inexistente, cuando se intenta eliminar, entonces el sistema responde 404 Not Found.
8. Dado cualquier endpoint de usuario, listado, perfil o respuesta de autenticación, cuando devuelve datos, entonces no incluye password, passwordHash, tokens internos ni secretos (si incluye el JWT).
9. Dado logs de aplicación, cuando se registra una operación, entonces no se imprimen contraseñas ni tokens completos.
