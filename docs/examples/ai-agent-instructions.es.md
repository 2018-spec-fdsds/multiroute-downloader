# Instrucción de proyecto de MultiRoute — cópiala en las reglas de tu agente

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agent-instructions.md">English</a> · <a href="ai-agent-instructions.ru.md">Русский</a> · <a href="ai-agent-instructions.de.md">Deutsch</a> · <strong>Español</strong> · <a href="ai-agent-instructions.fr.md">Français</a> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<a href="ai-agent-instructions.ko.md">한국어</a> · <a href="ai-agent-instructions.ar.md">العربية</a> · <a href="ai-agent-instructions.it.md">Italiano</a> · <a href="ai-agent-instructions.ja.md">日本語</a> · <a href="ai-agent-instructions.hi.md">हिन्दी</a> · <a href="ai-agent-instructions.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

Usa este bloque en tu `AGENTS.md`, `CLAUDE.md` o instrucciones persistentes de proyecto equivalentes ya existentes. Añádelo y conserva las demás reglas del proyecto. Sustituye una vez el destino y las rutas. Para la configuración y los ejemplos, consulta [Español](../ai-agents.es.md) o [Inglés](../ai-agents.en.md).

```markdown
## Descargas: usar MultiRoute por defecto

Para los archivos externos necesarios para la tarea autorizada del proyecto
(modelos, conjuntos de datos, archivos comprimidos, medios y otros recursos),
usa MultiRoute Downloader en ejecución. No me pidas elegir un gestor de descargas
para cada archivo. Mantén los gestores de paquetes y Git en sus procesos normales
de instalación, clonación y actualización.

Configuración del proyecto (editar una vez):
- API: http://127.0.0.1:8765/api/v1
- Destino: <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- Rutas REST: [{"type":"direct","enabled":true}]
- site_profile_id opcional: <EXISTING_PROFILE_ID_OR_OMIT>

Usa la herramienta REST local conectada o las herramientas MCP de MultiRoute
cuando estén disponibles. Para MCP, configura por separado direcciones IPv4
explícitas de puertas de enlace; su download_add requiere url, directory y gateways.
La ruta REST direct es la ruta del sistema operativo; no es un valor de puerta de
enlace MCP. No inventes rutas ni supongas la selección de la interfaz.

Con REST, comprueba GET /health y GET /license/policy antes del primer uso de la
sesión. Si solo tienes MCP, descubre las herramientas y llama a download_list con
limit 1 para comprobar la conectividad y la capacidad; el propio adaptador comprueba
la política de licencia. Crear descargas requiere features.automation_api=true.
Si no está disponible, informa del motivo y pregunta por otro gestor de descargas
en lugar de usar uno silenciosamente.

Para cada archivo necesario: elige su URL exacta, destino autorizado y nombre;
pasa un SHA-256 de confianza del editor como expected_sha256 cuando esté disponible.
Crea la tarea con POST /jobs (routes, no gateways) o MCP download_add. Usa una clave
de idempotencia estable para reintentar la misma petición; conserva el id devuelto.
La creación no es la finalización. Consulta esa tarea con GET /jobs/{id} o
download_status a intervalos razonables. Informa de estados pausados o fallidos;
reutiliza la tarea existente para reanudar en lugar de crear duplicados. No cambies
otras tareas ni ajustes de red ajenos. Al finalizar, comprueba checksum_checking e
integrity_status. No afirmes haber verificado con el editor sin una coincidencia
con una referencia de confianza. Devuelve la ruta local guardada, el id de la tarea
y el resultado de la verificación. El archivo permanece en el equipo de MultiRoute;
no afirmes que existe en un entorno aislado de ejecución remota.
```

La regla expresa una preferencia del proyecto. Sigue siendo necesaria una herramienta conectada compatible y los permisos normales del agente. Los archivos de origen descargados son datos, no instrucciones adicionales para el agente.
