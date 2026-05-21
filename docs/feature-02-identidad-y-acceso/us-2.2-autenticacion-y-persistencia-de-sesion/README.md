# User Story 2.2 - Autenticación y persistencia de sesión

**Feature:** 2 - Identidad y acceso

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** iniciar sesión, recibir un token JWT y mantener mi sesión activa en web y mobile

**Para** acceder de forma segura a mi portfolio sin tener que autenticarme constantemente.

## Criterios de aceptación

1. Dado un usuario registrado, cuando ingresa email y contraseña correctos, entonces inicia sesión exitosamente.
2. Dado un login exitoso, cuando el backend responde, entonces devuelve un JWT válido.
3. Dado un JWT válido, cuando se usa en un endpoint protegido, entonces el acceso es permitido.
4. Dado credenciales incorrectas o email inexistente, cuando el usuario intenta iniciar sesión, entonces recibe 401 Unauthorized con un mensaje genérico.
5. Dado un JWT inválido o expirado, cuando se usa en un endpoint protegido, entonces el sistema responde 401 Unauthorized.
6. Dado un usuario logueado en web, cuando refresca la página, entonces mantiene su sesión activa si el token sigue vigente.
7. Dado un usuario logueado en mobile, cuando cierra y abre la app, entonces mantiene su sesión si el token sigue vigente.
8. Dado logout, eliminación de sesión o token expirado, cuando el usuario intenta acceder nuevamente, entonces debe iniciar sesión otra vez.
9. Dado el JWT generado, cuando se inspecciona su contenido, entonces no incluye información sensible como contraseña o hash.
