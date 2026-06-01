# User Story 7.3 - Conversión mobile de acceso

**Feature:** 7 - Integración mobile

**Tipo:** Frontend Mobile

## Historia

**Como** usuario

**Quiero** usar en mobile pantallas claras de registro, login y errores de autenticación equivalentes a la web

**Para** crear mi cuenta, iniciar sesión y corregir problemas de acceso fácilmente desde el celular.

## Criterios de aceptación

1. Dado un visitante en mobile, cuando abre registro, entonces ve campos necesarios para crear cuenta.
2. Dado datos válidos, cuando envía el formulario de registro desde mobile, entonces se registra correctamente.
3. Dado datos inválidos, cuando envía el formulario desde mobile, entonces ve mensajes de error.
4. Dado registro exitoso, cuando termina, entonces se informa éxito o redirige a login.
5. Dado un usuario registrado, cuando abre login en mobile, entonces puede ingresar email y contraseña.
6. Dado credenciales válidas, cuando envía el formulario desde mobile, entonces entra a la aplicación y se guarda la sesión.
7. Dado credenciales inválidas, cuando envía el formulario desde mobile, entonces ve un mensaje de error genérico.
8. Dado usuario no autenticado, token expirado o error de red, cuando intenta operar o acceder a pantalla protegida desde mobile, entonces se redirige a login o se muestra un mensaje comprensible.
