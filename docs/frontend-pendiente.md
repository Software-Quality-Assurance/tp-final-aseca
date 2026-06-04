# Frontend pendiente

Lista de user stories o criterios de backend/mixtos que requieren cobertura explícita en frontend o mobile para cerrar la experiencia de usuario.

- **US 2.3:** Pantalla de perfil/cuenta para editar datos propios y eliminar cuenta, usando el ID del JWT sin exponer datos sensibles.
- **US 3.1:** Pantalla o flujo de búsqueda/listado de compañías, incluyendo paginación, estados vacíos, errores y selección por ticker/nombre.
- **US 3.1:** Si el alcance incluye administración, UI para crear/editar/eliminar compañías y resolver conflictos de ticker.
- **US 4.1:** Formulario explícito de compra/venta de acciones que use precio almacenado, valide cantidades y muestre errores 400/404/422.
- **US 4.2:** Acciones de editar/eliminar entradas del historial con confirmación y feedback de consistencia del portfolio.
- **US 4.3:** Tratamiento visual de precios faltantes o desactualizados en valor actual, con timestamp de actualización.
- **US 4.4:** Vista de P&L por posición y total, con estados para ganancia, pérdida, sin precio y sin datos suficientes.
- **US 4.5:** Mensajes de valuación cuando faltan precios, Yahoo Finance está caído o se usan precios almacenados.
- **US 4.6:** Indicadores de timestamp/fuente de precios y señales visuales de datos desactualizados.
- **US 6.1:** Modal/flujo completo para agregar y eliminar empresas de watchlist, con duplicados y estados vacíos.
- **US 6.2:** Vista comparativa de métricas entre watchlist y portfolio, diferenciando valores faltantes de valores cero.
- **US 6.3:** Búsqueda financiera por ticker/nombre con manejo de empresa inexistente, SEC caído y resultados vacíos.
- **US 6.4:** Pantalla de detalle financiero con métricas, filings recientes y evolución histórica.
- **US 6.5:** Mensajes frontend para rate limit, timeout, respuestas parciales y datos no disponibles de EDGAR.
- **US 7.1 a 7.4:** Replicar en mobile los flujos web principales con navegación, sesión, formularios, modales, estados vacíos y errores adaptados a pantalla chica.
