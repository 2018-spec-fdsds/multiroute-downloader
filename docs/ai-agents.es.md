# MultiRoute para agentes de IA — configúralo una vez, descarga por defecto

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <a href="ai-agents.de.md">Deutsch</a> · <strong>Español</strong> · <a href="ai-agents.fr.md">Français</a> · <a href="ai-agents.zh.md">中文</a><br />
<a href="ai-agents.ko.md">한국어</a> · <a href="ai-agents.ar.md">العربية</a> · <a href="ai-agents.it.md">Italiano</a> · <a href="ai-agents.ja.md">日本語</a> · <a href="ai-agents.hi.md">हिन्दी</a> · <a href="ai-agents.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

[English](ai-agents.en.md) · [Producto](overview/es.md) · [Referencia de la API](../API.ru.txt) · [OpenAPI](../openapi.yaml)

Añade una regla al proyecto y conecta una herramienta una sola vez. Claude Code, Codex y otros agentes con herramientas podrán entonces enviar a MultiRoute los pesos de modelos, conjuntos de datos, archivos comprimidos y otros archivos externos del proyecto, conservar los ID de las tareas, seguir su progreso y comprobar su finalización. Puedes pedir: **«Descarga el modelo que necesita este proyecto en su carpeta de recursos».** La regla establece el gestor de descargas preferido; no hace falta repetir los detalles de la API en cada petición.

El modelo puede ejecutarse en la nube o localmente. Lo que importa es **dónde se ejecuta su herramienta**: debe poder acceder al equipo donde se ejecuta MultiRoute. Un chat en el navegador o un entorno aislado remoto no obtiene acceso a tu PC por leer un archivo de instrucciones.

## 1. Elige una vez los valores predeterminados del proyecto

1. Instala e inicia MultiRoute en el equipo que almacenará los archivos. Usa el puerto de API configurado para esa instancia; los ejemplos usan `8765`.
2. Elige un directorio absoluto para los recursos del proyecto, como `C:/Projects/MyProject/assets` en Windows o `/Users/YOUR_USER/Projects/MyProject/assets` en macOS. Un recurso de red montado en Mac usa `/Volumes/Share/...`.
3. Elige explícitamente las rutas REST. La conexión normal del sistema operativo usa `[{"type":"direct","enabled":true}]`. Para usar las puertas de enlace independientes configuradas en MultiRoute, usa `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]` y añade otras puertas de enlace autorizadas si hace falta. Configura y verifica primero esas puertas de enlace en la aplicación. Las casillas de la interfaz no se copian automáticamente a las peticiones. La ruta directa usa el enrutamiento IP normal del sistema operativo, incluidas las rutas VPN del sistema/TUN que correspondan. No hereda la configuración del proxy HTTP/PAC ni las extensiones VPN exclusivas del navegador.
4. Crear descargas externas requiere la capacidad **`automation_api`**. La creación por lotes también requiere `batch_downloads`; los demás límites siguen vigentes. Comprueba `GET /license/policy`, incluido `features.automation_api`.
5. Mantén la API en la interfaz de bucle local. No tiene un token general de autenticación remota; no expongas su puerto en la LAN ni en Internet. Guarda las credenciales de origen en un perfil de sitio de MultiRoute y pasa su `site_profile_id` cuando sea necesario.

## 2. Añade la regla reutilizable al proyecto

Copia el **[bloque de instrucciones listo para usar](examples/ai-agent-instructions.es.md)** en las reglas existentes del proyecto. Sustituye una vez el destino y las rutas, conservando las demás instrucciones del proyecto.

| Agente / entorno | Dónde poner la regla | Conexión necesaria |
| --- | --- | --- |
| Claude Code en tu PC | `CLAUDE.md` en el proyecto. Si la regla ya está en `AGENTS.md`, añade `@AGENTS.md` a un `CLAUDE.md` existente en lugar de mantener dos copias. | Acceso local a shell/REST o el servidor MCP por stdio descrito abajo. |
| Codex en tu PC | `AGENTS.md` en el proyecto. | Acceso local a shell/REST o su servidor MCP configurado. |
| Otro agente local o anfitrión de modelos locales | Sus instrucciones persistentes de proyecto/sistema. | Activa una herramienta capaz de llamar a REST o un cliente MCP por stdio. Cargar un modelo no añade herramientas por sí solo. |
| Modelo en la nube con ejecutor local de agentes | Las reglas de proyecto del agente local. | Su herramienta local llama a MultiRoute en el mismo PC. No es necesario que el modelo se ejecute localmente. |
| ChatGPT web, Claude web o ejecutor remoto en la nube | Instrucciones del proyecto y una herramienta conectada compatible. | Necesita un puente configurado por separado hacia el equipo de MultiRoute. El adaptador local incluido aquí usa stdio; no es un servicio MCP remoto público. |

Los archivos de instrucciones guían el comportamiento; no eluden las aprobaciones de herramientas ni el entorno aislado del cliente. Las restricciones existentes del cliente o de la cuenta siguen vigentes. Consulta la documentación oficial de [instrucciones de proyecto de Claude](https://code.claude.com/docs/en/memory), [instrucciones de proyecto de Codex](https://developers.openai.com/codex/guides/agents-md/) y [requisitos de conexión MCP personalizada de ChatGPT](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

## 3. Ejemplo REST — un agente local con acceso a shell

Es la opción más sencilla cuando el agente ya dispone de un terminal local. **MultiRoute debe estar ya en ejecución.** Estos ejemplos usan una URL de origen ilustrativa; sustitúyela junto con el directorio de salida. Crear una tarea pone en cola una descarga real.

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'Se requiere MultiRoute automation_api' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # Añade expected_sha256 solo si tienes el hash hexadecimal de confianza del editor de 64 caracteres.
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# Conserva $job.id. Vuelve a consultarlo mientras esté queued/running/retrying/verifying:
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / shell local

Guarda el siguiente JSON como `download.json` en UTF-8. Sustituye `YOUR_USER` y la URL de origen; añade `expected_sha256` al mismo objeto cuando dispongas de un hash de confianza.

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "/Users/YOUR_USER/Projects/MyProject/assets",
  "filename": "model.gguf",
  "routes": [{"type": "direct", "enabled": true}],
  "idempotency_key": "myproject-model-v1"
}
```

```sh
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/health'
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/license/policy'
# Tras confirmar que features.automation_api es true:
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# Sustituye el id de ejemplo por el id de la respuesta:
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` significa que se ha creado la tarea, no que el archivo esté listo. Sigue consultando su estado hasta `complete`, o informa de un estado pausado o fallido. Al finalizar, comprueba `checksum_checking`, `integrity_status` y los hashes disponibles. Usa la misma clave de idempotencia para un reintento idéntico; usa una clave nueva para una petición deliberadamente nueva. Mantén las consultas de estado compactas y con intervalos razonables; los archivos grandes pueden permanecer activos durante mucho tiempo. Los gestores de paquetes y Git conservan sus procesos nativos de dependencias y clonación.

## 4. Opcional: herramientas MCP para un cliente local compatible

MultiRoute incluye un **adaptador MCP por stdio**, iniciado como subproceso Java. Reenvía las operaciones a la aplicación en ejecución. No hay un endpoint HTTP `/mcp` integrado. Su `download_add` actual requiere **direcciones IPv4 de puertas de enlace** explícitas; no acepta la ruta REST `direct`. Usa el método REST anterior para una ruta que utilice únicamente el sistema operativo.

Busca las rutas exactas de Java y del JAR instalados en **Configuración → MCP y CLI…**. Una instalación estándar de Windows tiene `runtime/bin/java.exe` y `app/MultiRoute-Downloader.jar` dentro de `%LOCALAPPDATA%/MultiRoute Downloader App`. En macOS son `Contents/runtime/Contents/Home/bin/java` y `Contents/app/MultiRoute-Downloader.jar` dentro de `/Applications/MultiRoute Downloader.app`. Pon entre comillas las rutas que contengan espacios. Estos paquetes nativos no necesitan una instalación de Java aparte.

Ejemplo de entrada de cliente stdio; sustituye ambas rutas absolutas. Los valores JSON **no** expanden automáticamente `%LOCALAPPDATA%`, `$HOME` ni las variables de shell:

```json
{
  "mcpServers": {
    "multiroute": {
      "command": "C:/Users/YOUR_USER/AppData/Local/MultiRoute Downloader App/runtime/bin/java.exe",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-cp",
        "C:/Users/YOUR_USER/AppData/Local/MultiRoute Downloader App/app/MultiRoute-Downloader.jar",
        "multiroute.MultiRouteMcp",
        "--api",
        "http://127.0.0.1:8765/api/v1/"
      ]
    }
  }
}
```

Para Claude Code, integra esta entrada en `.mcp.json` siguiendo la [configuración de proyecto documentada](https://code.claude.com/docs/en/mcp). Para Codex, configura el mismo comando y lista de argumentos bajo `[mcp_servers.multiroute]` en su configuración TOML o usa `codex mcp add`; consulta la [configuración oficial de MCP](https://developers.openai.com/codex/mcp/). Otros anfitriones usan sus propios ajustes de MCP. Esto es una configuración de ejemplo, no una conexión instalada.

Una vez que el cliente haya descubierto las herramientas, pídele que enumere las de MultiRoute. Los argumentos de `download_add` pueden ser:

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

La llamada posterior a `download_status` recibe `{"id":"012345abcdef"}`. Las herramientas disponibles son `download_add`, `download_batch`, `download_list`, `download_status`, `download_pause`, `download_resume` y `download_verify`. Todas las operaciones del adaptador requieren `automation_api`; `download_verify` vuelve a verificar un archivo completado. Los resultados MCP compactos omiten las URL de origen, las credenciales y el directorio de destino, así que conserva el directorio de la petición. `gateways: []` no significa enrutamiento automático.

## 5. Chats en la nube y entornos aislados remotos

**Una instrucción de proyecto por sí sola no puede conectar ChatGPT o Claude web a tu aplicación local.** `127.0.0.1` en una herramienta en la nube apunta al entorno de esa herramienta. La conexión MCP remota de ChatGPT requiere un servicio remoto compatible o un puente seguro admitido; la URL REST de MultiRoute no es un endpoint MCP de ese tipo. Esta versión no incluye ese puente remoto. Un modelo alojado en la nube utilizado por Claude Code, Codex u otro ejecutor local puede usar los métodos locales anteriores. [Requisitos de conexión de OpenAI](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

Los archivos se guardan en el equipo de MultiRoute. Una compilación remota necesita un paso aparte y autorizado de transferencia de archivos o almacenamiento compartido antes de poder utilizarlos. Las credenciales deben estar en perfiles de sitio configurados, no en el bloque de instrucciones ni en un chat.

## Informe esperado del agente

«Se ha descargado `model.gguf` mediante MultiRoute en la carpeta de recursos configurada. Tarea: `…`. Estado: complete. SHA-256: …; coincide con la referencia del editor». Si no se ha proporcionado ni encontrado una referencia de confianza, informa: «SHA-256 local calculado; verificación del editor no disponible». Si la aplicación no está disponible o devuelve `pro_required`, informa de ello y acuerda una alternativa en lugar de cambiar silenciosamente de gestor de descargas.
