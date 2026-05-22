# User Story 5.2 - Gestión de posiciones del portfolio

**Feature:** 5 - Portfolio del usuario

**Tipo:** Frontend

## Historia

**Como** usuario

**Quiero** agregar, editar y eliminar acciones de mi portfolio desde modales

**Para** mantener actualizada mi tenencia de inversiones.

## Criterios de aceptación

1. Dado un usuario autenticado, cuando agrega una acción válida, entonces se crea la posición.
2. Dado una posición agregada desde un modal, cuando se confirma, entonces el portfolio se actualiza visualmente.
3. Dado una cantidad menor o igual a 0, cuando intenta agregarla o editarla, entonces el sistema responde 400 Bad Request.
4. Dado un ticker inexistente, cuando intenta agregarlo, entonces el sistema responde 404 Not Found.
5. Dado una posición existente, cuando se edita la cantidad con un valor válido, entonces se actualiza la posición.
6. Dado una edición exitosa, cuando se vuelve al portfolio, entonces se ve la nueva cantidad.
7. Dado una posición inexistente, cuando se intenta editar o eliminar, entonces responde 404 Not Found.
8. Dado una posición existente, cuando se elimina desde el modal, entonces desaparece del portfolio.
9. Dado una eliminación cancelada, cuando el usuario cierra el modal, entonces no se modifica el portfolio.
