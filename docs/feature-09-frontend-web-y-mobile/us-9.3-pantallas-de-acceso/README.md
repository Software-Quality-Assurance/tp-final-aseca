# User Story 9.3 - Pantallas de acceso

**Feature:** 9 - Frontend web y mobile

**Tipo:** Frontend

## Historia

**Como** usuario

**Quiero** usar pantallas claras de registro, login y errores de autenticación

**Para** crear mi cuenta, iniciar sesión y corregir problemas de acceso fácilmente.

## Criterios de aceptación

1. Dado un visitante, cuando abre registro, entonces ve campos necesarios para crear cuenta.
2. Dado datos válidos, cuando envía el formulario de registro, entonces se registra correctamente.
3. Dado datos inválidos, cuando envía el formulario, entonces ve mensajes de error.
4. Dado registro exitoso, cuando termina, entonces se informa éxito o redirige a login.
5. Dado un usuario registrado, cuando abre login, entonces puede ingresar email y contraseña.
6. Dado credenciales válidas, cuando envía el formulario, entonces entra a la aplicación y se guarda la sesión.
7. Dado credenciales inválidas, cuando envía el formulario, entonces ve un mensaje de error genérico.
8. Dado usuario no autenticado, token expirado o error de red, cuando intenta operar o acceder a pantalla protegida, entonces se redirige a login o se muestra un mensaje comprensible.
