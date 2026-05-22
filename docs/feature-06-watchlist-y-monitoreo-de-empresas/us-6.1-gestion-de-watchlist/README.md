# User Story 6.1 - Gestión de watchlist

**Feature:** 6 - Watchlist y monitoreo de empresas

**Tipo:** Mixta

## Historia

**Como** usuario

**Quiero** agregar, eliminar y visualizar empresas en mi watchlist

**Para** monitorear compañías que me interesan actualmente.

## Criterios de aceptación

1. Dado un usuario autenticado, cuando agrega una empresa válida, entonces aparece en su watchlist.
2. Dado una empresa ya agregada, cuando intenta agregarla otra vez, entonces responde 409 Conflict.
3. Dado un ticker inexistente, cuando intenta agregarlo, entonces responde 404 Not Found.
4. Dado una adición exitosa desde modal, cuando se cierra el modal, entonces la lista se actualiza.
5. Dado una empresa en watchlist, cuando se elimina, entonces deja de aparecer.
6. Dado una empresa que no está en watchlist, cuando se intenta eliminar, entonces responde 404 Not Found.
7. Dado una eliminación cancelada, cuando el usuario no confirma, entonces la watchlist no cambia.
8. Dado empresas agregadas, cuando el usuario abre la watchlist, entonces las ve listadas.
9. Dado una watchlist vacía, cuando se consulta, entonces devuelve lista vacía con 200 OK.
10. Dado una empresa en watchlist, cuando se muestra, entonces incluye ticker, nombre y datos relevantes.
11. Dado un usuario autenticado, cuando consulta la watchlist de otro usuario, entonces el sistema rechaza el acceso.
