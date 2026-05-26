# tp-final-aseca

## Variables de Entorno

Antes de ejecutar la aplicación, copia el archivo `.env.example` a `.env` y completa las siguientes variables:

```bash
cp .env.example .env
```

### Configuración JWT

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `APP_JWT_SECRET` | Clave secreta para firmar y validar tokens JWT. **Importante**: Usa una clave segura en producción (mínimo 32 caracteres). | `your-super-secret-key-min-32-chars` |
| `APP_JWT_ISSUER` | Identificador único de quién emite los tokens. Usado para validar que el token fue creado por esta aplicación. | `portfolio-tracker` |
| `APP_JWT_EXPIRATION_SECONDS` | Tiempo de vida de los tokens JWT en segundos. Después de este tiempo, el usuario debe volver a autenticarse. | `3600` (1 hora) |

### Ejemplo de `.env`
```
APP_JWT_SECRET=my-super-secret-key-that-is-at-least-32-characters-long
APP_JWT_ISSUER=portfolio-tracker
APP_JWT_EXPIRATION_SECONDS=3600
```

---

## Correr solo la base de datos

Usar este modo para desarrollo local. PostgreSQL corre en Docker y Spring Boot se corre desde IntelliJ o localmente.

```powershell
docker compose up -d db
```

Conexion local:

```text
jdbc:postgresql://localhost:5433/portfolio_tracker
```

## Correr todo en Docker

Usar este modo para levantar PostgreSQL y `portfolio-api` dentro de Docker con el perfil `api`.

```powershell
docker compose --profile api up -d
```

La API se conecta a Postgres usando:

```text
jdbc:postgresql://db:5432/portfolio_tracker
```

## Bajar servicios

```powershell
docker compose down
```

Para bajar servicios y borrar volumenes:

```powershell
docker compose down -v
```

Borrar volumenes resetea la base de datos.

## Activar hooks de Git

Si los hooks del repositorio estan en `.github/hooks`, hay que indicarselo a Git:

```bash
git config core.hooksPath .github/hooks
```

Tambien hay que dar permisos de ejecucion a los hooks:

```bash
chmod +x .github/hooks/*
```

Eso lo configura para este repositorio. Para verificarlo:

```bash
git config --get core.hooksPath
```