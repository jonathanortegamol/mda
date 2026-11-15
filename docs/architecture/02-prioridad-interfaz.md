# Decisión — priorizar la demostración gráfica de MDA

El usuario aclaró que las pantallas y la propagación visual de cambios son prioritarias frente a completar backend/base de datos. Se incorporó un segundo destino M2T, `frontend`, sin retirar el PSM, PostgreSQL, ATL o el backend existente.

Se reutiliza el PSM actual: sus atributos, cardinalidades, tipos, restricciones básicas y relaciones son suficientes para generar formularios sencillos. No se añade un metamodelo completo de diseño de interfaces para esta demostración académica. La distribución visual y las etiquetas del perfil están definidas en una plantilla Acceleo especializada.

Se distinguen dos modos:

- **Demostración local:** interfaz ejecutable en el navegador, datos ficticios y localStorage. Permite evaluar el flujo gráfico sin resolver dependencias de servidor.
- **API:** la misma interfaz se conecta al backend generado. La persistencia definitiva y la integración PostgreSQL siguen pendientes de verificación.

El modo local no se presenta como ejecución del backend, de OCL o de PostgreSQL. Su objetivo es demostrar pantallas generadas y el cambio del modelo hasta un campo visible en el HTML y en los listados. Las transformaciones siguen ejecutándose con ATL y Acceleo reales; no son simuladas por el adaptador del navegador.

La estructura visual se conserva al añadir campos: el generador produce un input nuevo y metadatos de columna. Se comprobó con telefono en una copia del modelo, sin editar los archivos finales de la interfaz.
