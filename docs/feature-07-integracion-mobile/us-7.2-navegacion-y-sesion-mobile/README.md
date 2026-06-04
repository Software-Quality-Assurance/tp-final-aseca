# User Story 7.2 - Navegación y sesión mobile

**Feature:** 7 - Integración mobile

**Tipo:** Frontend Mobile

## Historia

**Como** usuario

**Quiero** usar la aplicación mobile con navegación, pantallas adaptadas y sesión persistente

**Para** gestionar mi portfolio cómodamente desde el celular.

## Criterios de aceptación

1. Dado la app mobile, cuando el usuario inicia sesión, entonces puede navegar entre las pantallas principales.
2. Dado navegación mobile, cuando cambia de pantalla, entonces mantiene sesión y estado básico.
3. Dado una pantalla protegida, cuando no hay sesión, entonces redirige a login.
4. Dado un dispositivo mobile, cuando se usa la app, entonces los elementos son accesibles y no se cortan.
5. Dado mobile, cuando se abre login, portfolio, historial, valor actual o watchlist, entonces la pantalla se adapta al tamaño.
6. Dado tablas o listados, cuando se ven en mobile, entonces son legibles.
7. Dado modales, cuando se usan en mobile, entonces permiten confirmar, cancelar y cerrar correctamente.
8. Dado una operación desde mobile, cuando se completa, entonces impacta en la misma API y base de datos.
9. Dado un login exitoso en mobile, cuando se cierra y abre la app, entonces la sesión sigue activa si el token es válido.
10. Dado token expirado o logout, cuando se abre la app, entonces redirige a login.
11. Dado almacenamiento seguro del token, cuando se revisa la app, entonces no queda hardcodeado ni expuesto.
