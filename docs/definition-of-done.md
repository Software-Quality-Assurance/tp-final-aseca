# Definición de Hecho - DoD / Criterios Globales

Estos criterios aplican transversalmente a todas las Features y User Stories del producto.

## Criterios globales

1. Dado un usuario no autenticado, cuando intenta acceder a un endpoint protegido, entonces el sistema responde 401 Unauthorized.
2. Dado un usuario autenticado, cuando intenta acceder a recursos de otro usuario, entonces el sistema responde 403 Forbidden o no expone información ajena.
3. Dado un request inválido, cuando faltan campos obligatorios o los datos tienen formato incorrecto, entonces el sistema responde 400 Bad Request con un mensaje claro.
4. Dado un recurso inexistente, cuando se intenta consultarlo, editarlo o eliminarlo, entonces el sistema responde 404 Not Found.
5. Dado un error externo de Yahoo Finance o SEC EDGAR, cuando el servicio no responde, entonces el sistema no rompe el flujo completo y devuelve un mensaje controlado.
6. Dado cualquier respuesta de la API, cuando contiene datos sensibles, entonces no expone password, passwordHash, tokens internos ni secretos.
7. Dado un cálculo financiero, cuando se realiza sobre precios, cantidades o porcentajes, entonces el resultado usa los últimos datos almacenados y mantiene consistencia decimal.
8. Dado un flujo implementado, cuando se entrega como parte del TP, entonces debe tener evidencia mínima de test, documentación o justificación técnica.
9. Dado una funcionalidad incluida en el backlog, cuando se considera terminada, entonces debe estar integrada con la API común si aplica.
10. Dado una funcionalidad visible para el usuario, cuando se entrega, entonces debe manejar estados de éxito, error, carga y estado vacío.
11. Dado una funcionalidad que modifica datos, cuando falla por validación, permisos o error externo, entonces no debe dejar datos parcialmente inconsistentes.
12. Dado una integración externa, cuando se implementa, entonces debe registrar errores útiles para debug sin exponer secretos.
13. Dado una release parcial o final, cuando se entrega, entonces debe quedar versionada y documentada de forma trazable.
14. Aplicar TDD dentro de lo posible
15. Integración de cypress dentro de lo posible.
16. Integración de appium dentro de lo posible.
17. Coverage mayor a 90 por ciento.
