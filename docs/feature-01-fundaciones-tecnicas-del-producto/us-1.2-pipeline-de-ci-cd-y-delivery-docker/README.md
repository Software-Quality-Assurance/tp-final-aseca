# User Story 1.2 - Pipeline de CI/CD y delivery Docker

**Feature:** 1 - Fundaciones técnicas del producto

**Tipo:** Infraestructura

## Historia

**Como** desarrollador

**Quiero** configurar un pipeline de CI/CD que compile, ejecute tests, versione releases y publique imágenes Docker

**Para** que cada cambio pueda validarse automáticamente y el sistema pueda distribuirse de forma trazable.

## Criterios de aceptación

1. Dado que hay errores de build o tests fallidos, cuando corre el pipeline, entonces el workflow falla y bloquea la integración.
2. Dado que build y tests pasan, cuando finaliza el pipeline, entonces queda registro visible en GitHub Actions.
3. Dado un release o tag válido, cuando se ejecuta el workflow, entonces se publican imágenes Docker en GHCR.
4. Dado una versión SemVer, cuando se publica una imagen, entonces queda tagueada con la versión correspondiente.
5. Dado una imagen publicada, cuando se revisa GHCR, entonces existe correspondencia entre tag Docker y release de GitHub.
6. Dado un error de autenticación o publicación en GHCR, cuando el workflow intenta publicar, entonces falla de forma visible.
7. Dado que los tests fallan, cuando corre el workflow, entonces no se publica la imagen Docker.
