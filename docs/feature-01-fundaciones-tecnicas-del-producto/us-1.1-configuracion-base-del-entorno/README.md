# User Story 1.1 - Configuración base del entorno

**Feature:** 1 - Fundaciones técnicas del producto

**Tipo:** Infraestructura / Backend

## Historia

**Como** desarrollador

**Quiero** configurar el entorno completo del producto con Docker Compose, PostgreSQL, volúmenes persistentes, modelo de datos inicial y API común

**Para** que el sistema pueda ejecutarse localmente de forma consistente y ser consumido tanto por web como por mobile.

## Criterios de aceptación

1. Dado el repositorio clonado, cuando un desarrollador ejecuta docker compose up, entonces se levantan backend principal, backend secundario Python y base de datos.
2. Dado que los servicios están levantados, cuando el backend principal y el módulo Python intentan conectarse a PostgreSQL, entonces ambos se conectan correctamente a la misma base de datos.
3. Dado el modelo de dominio, cuando se revisa la base de datos, entonces existen tablas para usuario, compañía, historial, watchlist, portfolio y precios, con claves primarias y foráneas correctamente definidas.
4. Dado un volumen persistente configurado para PostgreSQL, cuando se baja y vuelve a levantar Docker Compose, entonces los datos persistidos siguen disponibles.
5. Dado el frontend web y la app mobile (apk), cuando consumen funcionalidades equivalentes, entonces utilizan la misma API común.
6. Dado un error de configuración en un contenedor, cuando el servicio falla, entonces el log permite identificar qué servicio falló.
