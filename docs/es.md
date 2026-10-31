# MultiRoute Downloader 4.3.9 — Guía en español

[macOS: instalación, inicio y permisos](macos/es.md)

<!-- product-overview:start -->
[**Descripción del producto**](overview/es.md)
<!-- product-overview:end -->

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · **Español** · [Français](fr.md) · [Inicio](../README.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

<!-- current437:start -->

<a name="current-free-pro"></a>

## 4.3.9 — Funciones actuales, Free y Pro

Esta guía describe MultiRoute Downloader 4.3.9. Los instaladores nativos y las sumas de comprobación publicadas están en la [versión 4.3.9](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.3.9). Las secciones siguientes describen sus funciones actuales y capacidades de licencia.

### Límites y funciones de licencia

Free incluye descargas individuales normales, reanudación compatible, toda la velocidad disponible, comprobación de integridad, DNS protegido y velocidades/estados actuales de las puertas de enlace. No reduce artificialmente el ancho de banda. Los permisos firmados son independientes: una licencia Pro parcial solo habilita los que contiene.

| Función | Free | Con permiso en la licencia | ID |
| --- | --- | --- | --- |
| Puertas de enlace simultáneas | ≤ 2 | ≤ 8 | `extra_gateways` |
| Descargas simultáneas | ≤ 3 | ≤ 32 | `extra_concurrent_downloads` |
| Perfiles de sitios | ≤ 3 | Sin cuota de licencia | `extra_site_profiles` |
| Prioridades de descarga | Solo normal | Seleccionable | `download_priorities` |
| Prioridades de las puertas de enlace | 0 | Seleccionable | `gateway_priorities` |
| Historial e informes por puerta | No incluido | Incluido | `gateway_statistics` |
| Descargas / subidas por lotes | No incluido | Incluido | `batch_downloads` |
| API para aplicaciones externas | No incluido | Incluido | `automation_api` |
| Temas adicionales | Graphite / System | Todos los temas | `premium_appearance` |

Para un lote, abra **Nueva descarga…** y elija **Lista de enlaces…** en la fila fija de acciones al pie de esa ventana.

Solo se activan los permisos de su licencia firmada. Se conservan archivos y perfiles. Los demás archivos esperan en cola; siguen disponibles la velocidad y el estado actuales de las puertas. Se mantienen los límites técnicos de almacenamiento.

El límite de puertas de enlace cuenta el conjunto compartido por descargas y subidas, sin una ruta adicional del sistema. El límite de tres tareas simultáneas se aplica a descargas, no a subidas; las demás esperan. Un límite de cola configurado menor sigue vigente. No se borran puertas de enlace, perfiles, progreso ni prioridades guardadas. La cuota de perfiles limita su creación, no los anteriores. El historial/CSV requiere su permiso; los indicadores en vivo siguen en Free. La programación sigue disponible; las prioridades distintas de la normal requieren su permiso. Se mantienen los límites técnicos del almacenamiento y del origen.

La licencia firmada corresponde a la clave de instalación, no a una MAC, IP o router. No caduca; 4–4 cubre 4.x. Cambiar puertas de enlace, DHCP o MAC de la aplicación no exige reactivar. Otro equipo, usuario o perfil puede necesitar una nueva solicitud. No se promete código imposible de modificar ni vinculación universal al hardware.

### MAC de la aplicación

MultiRoute usa su propio MAC estable con IP manual y DHCP. No cambia el MAC del adaptador del sistema. Se comprueban conflictos con dispositivos observados y otras instancias; la falta de respuesta no descarta equipos apagados. Grupo: MAC unicast locales con 46 bits aleatorios. No es un grupo IP de DHCP.

**Cambiar MAC…** — Use el botón al pausar o terminar las transferencias activas/en cola y finalizar las comprobaciones locales. Se comprueban direcciones observadas y anuncios cooperativos de otras instancias. «No se detectó conflicto» no garantiza que un dispositivo apagado o silencioso no use la dirección. La MAC de la aplicación debe ser distinta de la MAC física; el adaptador y las rutas del sistema no cambian.

### Suma de comprobación…

La referencia se guarda sin reiniciar ni borrar el progreso. Se detecta MD5, SHA-256 o SHA-512 por 32, 64 o 128 dígitos hexadecimales. El archivo ensamblado se comprobará al terminar. Guarde la referencia y pulse Comprobar ahora. Se lee el archivo local terminado. Una diferencia no vuelve a descargar ni elimina el archivo.

La verificación final usa la última referencia guardada. Editarla no borra bytes ni reinicia la transferencia. La comprobación local conserva el archivo terminado e informa coincidencia, discrepancia o error por separado. Las referencias del editor se conservan de forma independiente. Una suma corresponde a un único archivo torrent. La integridad no demuestra que el contenido sea seguro; prefiera referencias SHA-256/SHA-512 fiables.

### Acceso a la API en 4.3.9

La API JSON local usa UTF-8 sin token API ordinario. El control externo necesita automation_api; se comprueban además los otros permisos. Ese permiso por sí solo no permite lotes ni puertas adicionales. Free conserva diagnóstico limitado de lectura, ciclo de vida con autenticación privada y envío opcional de un enlace desde el navegador. Consulte [API](../API.ru.txt) y [OpenAPI](../openapi.yaml); no exponga el puerto a LAN/Internet.


La aplicación original usa la [Licencia 1.2](../LICENSE.txt): Free admite uso personal y empresarial; el titular puede ofrecer Pro de pago. Otros destinatarios no pueden venderla sin permiso separado. OpenJDK GPLv2 con Classpath Exception exige avisos/fuentes correspondientes; JNA usa Apache-2.0 con avisos; JLayer LGPL-2.1 separada conserva derechos de sustitución/depuración y fuentes. Consulte [condiciones](../LICENSING.md), [catálogo completo](../THIRD-PARTY-NOTICES.txt) y la página de licencias.

El estilo System mantiene legibles el texto de tablas, campos y controles sobre el fondo del sistema; Graphite y System están incluidos en Free.

El nombre y el tamaño del archivo tienen columnas separadas. El tamaño se ordena numéricamente, independientemente del nombre; el tamaño desconocido se indica con una raya. El nombre y la ruta completos aparecen en la ayuda emergente.

Al situar el puntero sobre el nombre se muestran el nombre completo, la ruta de destino, el protocolo y la dirección o ruta local de origen. Las tareas torrent indican BitTorrent aunque los metadatos se abrieran por HTTPS. La dirección del repositorio, si existe, aparece después de la URL real del archivo. Se ocultan credenciales, valores de consulta y fragmentos; puede mantenerse un infohash público magnet reconocido. La URL guardada y el estado de transferencia no cambian.


### Prioridades de las puertas de enlace y diagnóstico en 4.3.9

En los ajustes de red, asigne a cada puerta de enlace una prioridad de **0 a 1000**; su indicador principal también muestra el valor. Un número mayor le da preferencia para archivos pequeños o transferencias completas y le asigna más segmentos paralelos en archivos grandes. Las demás puertas seleccionadas que funcionen siguen ayudando. Los valores iguales conservan el orden de selección; otra puerta seleccionada puede asumir el trabajo si la preferida falla o se detiene. No se garantiza una proporción concreta de velocidades.

Se requiere el permiso firmado independiente **`gateway_priorities`**. El permiso de prioridad de tareas **`download_priorities`** no lo incluye. Sin el permiso de puertas de enlace, todas las prioridades efectivas son 0; se conservan los valores guardados. Puede cambiarlas durante una descarga sin borrar partes verificadas. Cada puerta seleccionada utiliza entre uno y cuatro procesos de segmentos en paralelo, con un máximo de 32 por tarea.

Los ajustes permiten elegir el número de descargas simultáneas dentro del límite de la licencia y un número finito o ilimitado de reintentos automáticos. Las pausas manuales se mantienen. El menú contextual permite pausar/reanudar las tareas seleccionadas compatibles y copiar nombres de archivo. El doble clic abre solo la carpeta; las propiedades, estadísticas y el registro individual están en el menú contextual.

El registro individual sigue disponible para archivos completados. Ajustes → Errores y soluciones abre una guía sin conexión con búsqueda: 25 temas en los 12 idiomas de la interfaz. El destino y las propiedades muestran espacio disponible en GB. La falta de espacio local se indica expresamente, conserva las partes guardadas y no se repite indefinidamente como error de red. El prefijo guardado sigue visible en el progreso mientras un segmento pequeño reanudado espera datos.

### Datos que faltan por descargar · 4.3.9

La tabla incluye la columna **Datos restantes**. Muestra los bytes pendientes por separado del tiempo estimado y se ordena numéricamente. Incluye el progreso guardado y en curso, nunca baja de cero y muestra un guion mientras se desconoce el tamaño total.

### Inicio de Mac y configuración de puertas más fiables · 4.3.9

El inicio nativo de Mac reconoce la antigua ruta predeterminada relativa, incluso si falta la barra inicial. Usa un perfil existente verificado o una carpeta predeterminada vacía del usuario. Las ubicaciones personalizadas ambiguas requieren una elección explícita.

Si la preparación de una puerta falla o se cancela, las otras puertas y las comprobaciones en segundo plano dejan de repetir la autorización hasta que reintentes la configuración de red explícitamente. El primer inicio privilegiado aún puede requerir la confirmación normal de administrador de macOS para abrir el dispositivo BPF de Ethernet.

El componente de red Mac 4.3.2 admite que macOS oculte la MAC del adaptador y conserva las comprobaciones que protegen la tarjeta física. Se mantiene la MAC propia de la aplicación, sin cambiar las rutas del sistema ni la MAC física. El icono y nombre de Mac usan la misma identidad MultiRoute que Windows. Cierra la aplicación antes de actualizar con PKG. El nombre del solicitante en el diálogo real y las transferencias LAN/DHCP aún necesitan prueba en hardware; este transporte sigue requiriendo Ethernet por cable.

**macOS:** `/Applications/MultiRoute Downloader.app` · `/Library/PrivilegedHelperTools/MultiRouteResume/4.3.2/{gateway-net,gateway-launch}`.

### Respuestas de las puertas de Mac con dirección manual · 4.3.9

Se corrige la captura POSIX para que tanto la dirección manual de la aplicación como DHCP reciban respuestas dirigidas a la MAC propia, como ya sucede en Windows. Un ping del sistema correcto comprueba la conexión del equipo, no la recepción en la dirección virtual de la aplicación.

Los diagnósticos DHCP conservan su texto en doce idiomas en vez de un error genérico. La autorización existente no cambia: las comprobaciones normales reutilizan el componente preparado. No cambian los endpoints API, controles, rutas del sistema ni MAC física. El primer acceso privilegiado a Ethernet BPF aún puede requerir la confirmación normal de administrador.

Se corrige una diferencia del modo de captura, no todas las causas de una puerta inaccesible. Siguen activas las comprobaciones de dirección libre, subred y concesiones/conflictos DHCP. Las transferencias Mac con dirección manual aún necesitan confirmación en hardware; sigue siendo necesario Ethernet por cable.

<!-- current437:end -->

<a name="appearance-and-sounds"></a>

## Apariencia y sonidos de finalización

Elige entre nueve apariencias: Graphite, Nordic, Streams Studio, Neon, Community, Chat, Studio, Rose y System. Graphite es la predeterminada. Graphite y System están incluidos en Free; las demás opciones requieren la función Pro de apariencia correspondiente. Abre Ajustes → Apariencia para cambiar el estilo y los controles redondeados. Tus descargas continúan mientras eliges. **Ventanas y botones redondeados** es una opción independiente. Los controles admiten esquinas redondeadas en todos los estilos; el marco exterior depende del sistema operativo. Windows 11 puede mantener rectangulares las ventanas maximizadas o acopladas; macOS y Linux controlan sus propios contornos. La interfaz ofrece un gráfico de velocidad y los ajustes de red utilizan tarjetas compactas para las puertas de enlace.

Los sonidos están **desactivados de forma predeterminada**. Active la casilla principal y elija por separado los avisos **por cada archivo** y **por todas las descargas**. Hay **Campanillas suaves**, **Piano** y **Marimba**; puede escuchar ambas melodías antes de elegir. Al terminar todas las descargas, si el aviso general está activado, el último archivo solo reproduce la melodía final, sin otro sonido superpuesto. Los trabajos pausados o con error no están terminados. El historial ya completado no suena al iniciar ni al cambiar preferencias. También influyen el volumen del sistema y la salida de audio.

Para usar audio propio, elija **Melodía personalizada** y seleccione archivos **MP3 o WAV** distintos para el aviso de archivo y el de todas las descargas. Cada fragmento debe durar **como máximo 10 segundos**. La aplicación comprueba la duración del audio decodificado, sin confiar solo en la extensión o los metadatos. Los archivos aceptados se copian al perfil local de la aplicación; después puede mover los originales.

El mensajero de escritorio opcional convierte el progreso de las descargas en una carga visible. Elige al hombre o a la mujer en los ajustes de apariencia y establece el tamaño entre el 40% y el 100%. El saco muestra el total de datos restantes conocidos de las descargas activas y se reduce a medida que llegan los bytes. El movimiento sigue su velocidad de descarga combinada; a velocidad cero, el mensajero permanece quieto. Las cargas pequeñas se llevan al hombro, las mayores se arrastran y las muy grandes muestran más esfuerzo.

Usa el botón Avatar para mostrar u ocultar al mensajero. Arrástralo para moverlo, haz doble clic con el botón izquierdo del ratón para mostrar la aplicación o haz clic con el botón derecho para ver sus opciones. Es un acompañante visual: los archivos permanecen en las carpetas seleccionadas.

Al terminar una descarga, el mensajero deja la carga entregada y se desvanece. Si quedan descargas activas, regresa con los datos restantes.

Apariencia, esquinas y sonidos son preferencias locales del escritorio. No añaden métodos a la API de descargas ni cambian la transferencia, las sumas de comprobación o la seguridad DNS.

<a name="startup-profile-location"></a>

## Carpeta de datos de la aplicación

La aplicación nativa usa una ruta absoluta guardada para la carpeta de datos, independiente del directorio de trabajo al iniciar sesión. El historial de descargas, los ajustes y las partes guardadas se almacenan en el perfil seleccionado. Se admiten las rutas de la versión portátil por línea de comandos; `--data-dir` explícito elige la carpeta de datos.

<a name="automatic-startup"></a>

## Inicio automático

Active **Iniciar con el sistema operativo** junto a los controles de idioma y apariencia. Está **desactivado de forma predeterminada**. Registra MultiRoute para el siguiente inicio de sesión de su cuenta; al desactivarlo se elimina la entrada propia. Se ejecuta al iniciar sesión, no como servicio del sistema ni antes del inicio de sesión. No se necesitan permisos de administrador para crear la entrada.

El inicio automático usa la bandeja cuando está disponible; de lo contrario queda una ventana normal accesible. Se mantienen las prioridades, pausas y horarios de la cola; no se fuerzan las descargas pausadas. Abra primero la aplicación manualmente para completar la instalación y los requisitos de red. Mantenga la aplicación y, en uso portátil, Java y las bibliotecas en las rutas registradas. Tras moverlas, desactive y reactive el inicio desde la nueva ubicación. Desactívelo antes de desinstalar.

El sistema o la política del escritorio puede desactivar una entrada registrada. En macOS, revise **Ajustes del Sistema → General → Ítems de inicio y extensiones** si se solicita permiso; la autorización de red es independiente. El registro no demuestra un futuro inicio correcto. Las pruebas con perfiles propios no incluyen un reinicio o cierre de sesión real; consulte el informe del lanzamiento.

Si no puede abrir la aplicación, elimine **solo su propia entrada**:

- **Windows:** la aplicación guarda su propio valor `REG_SZ` **MultiRoute Downloader** en `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`. Es preferible desactivar el inicio desde la app. Si no puede abrirla, elimine **solo ese valor** con el Editor del Registro, conservando la clave Run y las demás entradas. Si el sistema lo desactivó, use **Configuración → Aplicaciones → Inicio**; la app respeta esa decisión. Windows limita la orden completa de inicio a 260 caracteres; si es demasiado larga, utilice una ruta de aplicación/perfil más corta y vuelva a registrarla.
- **macOS:** elimine `~/Library/LaunchAgents/io.github.2018-spec-fdsds.multiroute-downloader.plist`.
- **Linux:** elimine `${XDG_CONFIG_HOME:-~/.config}/autostart/io.github.2018-spec-fdsds.multiroute-downloader.desktop` del directorio real de configuración XDG. Se requiere un escritorio gráfico compatible con el inicio XDG; no se instala un servicio systemd.

<a name="dns-settings"></a>

## Configuración DNS

Los resultados de verificación de plataformas e instaladores están en [NATIVE-INSTALLER-TESTS.json](../NATIVE-INSTALLER-TESTS.json).

Abra **Red → Resolución DNS**. El valor predeterminado es **DNS sobre HTTPS (DoH), Cloudflare**. Solo afecta a MultiRoute; no cambia DNS, rutas ni direcciones IP del sistema.

DoH admite HTTP/2 acotado seleccionado por TLS ALPN y HTTP/1.1 cuando se negocia ese protocolo. La compatibilidad del proveedor y los resultados reales constan en el informe final de la versión.

- **DoH:** DNS cifrado por HTTPS a través de cada puerta de enlace seleccionada. Se verifica el certificado TLS con el nombre de la URL.
- **DoT:** DNS cifrado por TLS a través de cada puerta de enlace, puerto 853 por defecto; se verifica el nombre del servidor.
- **DNS del sistema (equipo):** usar expresamente la política DNS del equipo, SO o VPN. El DNS puede no seguir cada puerta de enlace; las conexiones de archivos conservan la seleccionada. Este modo no garantiza cifrado DNS.

Elija Cloudflare, Quad9, Google o **Personalizado**. Los preajustes son fijos; Personalizado permite editar URL/nombre, puerto e IPv4 públicas numéricas de arranque. Estas direcciones evitan resolver antes el proveedor mediante el SO. DoH/DoT verifican la cadena de confianza, nombre y SNI; nunca recurren silenciosamente a DNS sin cifrar ni al sistema. Un fallo seguro termina ese intento. El proveedor debe ser accesible por la puerta de enlace elegida.

Edite un borrador, seleccione puertas de enlace y pulse **Probar DNS**. No lo guarda; muestra resultado, tiempo e IPv4 públicas o error por puerta de enlace. Los fallos parciales siguen visibles. En modo Sistema el éxito comprueba el resolvedor del SO, no la WAN independiente de cada router. **Aplicar configuración DNS** valida y guarda. La revisión devuelta identifica la configuración aplicada; las actualizaciones y los cambios de idioma conservan borrador, cursor y desplazamiento.

Cada consulta tiene 8 segundos en total, incluidas las consultas DNSSEC auxiliares. El TTL validado también respeta la caducidad de firma; la caché nativa se limita a 300 segundos y se separa por puerta/revisión. Siguen vigentes TTL cero y HTTP Age de DoH. El modo Sistema sigue la caché OS/JVM. Se admiten destinos IPv4 públicos; no se admite resolución IPv6.

API: `GET /api/v1/network/dns` devuelve configuración y estado de solo lectura; `PUT` o `POST` aplica configuración. `POST /api/v1/network/dns/test` prueba un borrador; `GET /api/v1/network/dns/providers` enumera preajustes. Envíe solo campos de configuración, sin revisión/estado de GET. [API](../API.ru.txt) · [OpenAPI](../openapi.yaml).

<a name="dnssec-validation"></a>

## Validación DNSSEC local

En **Red → Resolución DNS → Validación DNSSEC**, elija **Validar dominios firmados** (predeterminado para DoH/DoT), **Exigir respuestas DNSSEC seguras** o **DNSSEC desactivado**. Java valida localmente mediante dnsjava y anclas raíz incluidas. El indicador AD del proveedor no basta como prueba. DoH/DoT cifra el transporte; DNSSEC autentica los datos DNS por separado.

**Validar** admite respuestas seguras y sin protección confirmada; **Exigir** solo admite seguras. Sin protección puede indicar delegación sin firmar autenticada o algoritmos no admitidos; lea el motivo. Se rechazan resultados inválidos e indeterminados mientras la validación esté activa. DNS del sistema desactiva la validación local. Al volver a DoH/DoT se recupera la última política; cambiar de proveedor la conserva. Aplicar guarda; Probar DNS usa el borrador sin guardar.

Cada consulta distingue `secure`, `insecure`, `bogus`, `indeterminate` y `not_checked`; la falta de resultado se muestra como no disponible. Una conexión correcta no demuestra DNSSEC seguro. Las firmas caducadas, claves/pruebas ausentes, errores de consulta y tiempos agotados tienen motivos distintos. El reloj debe ser correcto. Las consultas auxiliares usan la misma puerta y presupuesto total. DNSSEC no oculta nombres ni examina el contenido descargado.

API: `dnssec` acepta `off`, `validate`, `require`; el modo Sistema solo `off`. Son de solo lectura `dnssec_validation`, `dnssec_trust_anchors` y, por consulta, `dnssec_status`, `dnssec_reason_code`, `dnssec_reason`, `dnssec_validated_ttl`, `dnssec_aux_queries`. No los envíe al guardar. Consulte API/OpenAPI arriba y las [DNSSEC](#dnssec-validation).

**Tratamiento de intervalos HTTP:** si un CDN como Civitai devuelve HTTP 200 para un intervalo que empieza en cero, se comprueba otro intervalo que empieza después de cero. El primer segmento se limita a su tamaño; los datos secuenciales guardados compatibles pueden continuar como segmentos. Un HTTP 200 temporal para un intervalo posterior se reintenta tras 2–30 segundos, conservando los datos y el trabajador de la puerta de enlace. El registro persistente por puerta de enlace distingue el tráfico recibido del progreso guardado. [Diagnóstico de transferencias](transfer-diagnostics.en.md).

[Los instaladores 4.3.9](../downloads/v4.3.9/README.md) incluyen Java 21 privado. El asistente de Windows está en inglés y crea un acceso directo en el escritorio; Npcap se requiere por separado. macOS usa libpcap del sistema. Siga las instrucciones de instalación y seguridad que aparecen abajo.

MultiRoute Downloader es una aplicación de escritorio Java para descargar archivos a través de una o varias **puertas de enlace** de su red local. Distribuye los segmentos disponibles entre ellas, conserva los datos comprobados y reúne el archivo final. Incluye una API local para otras aplicaciones. La ventana de escritorio no es una interfaz web.

Titular del proyecto: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## ¿Para qué sirve?

Si su red dispone de varias conexiones independientes a Internet, por ejemplo una línea fija y un router móvil 4G/5G, la aplicación aprovecha su ancho de banda simultáneamente. El equipo accede a las puertas de enlace a través de la misma red Ethernet local.

- **Mayor velocidad total:** todas las puertas de enlace seleccionadas y disponibles descargan segmentos en paralelo desde una cola compartida. Las más rápidas pueden obtener más segmentos.
- **Resistencia a fallos:** en las descargas segmentadas reanudables, las demás puertas de enlace siguen trabajando si una falla. Los segmentos incompletos vuelven a la cola para otra puerta de enlace y se conservan los datos verificados.
- **Descargas por lotes:** importe una lista de URL, gestione varios archivos en la cola o envíe un grupo de descargas mediante la API local.

La mejora de velocidad depende de la capacidad independiente de las conexiones y de los límites del servidor. Reanudar y reasignar segmentos requiere que el origen y el protocolo permitan una reanudación segura.

<a name="application-virtual-ip"></a>

### Por qué la aplicación utiliza una IP virtual

**Elija las puertas de enlace de MultiRoute sin cambiar la conexión de las demás aplicaciones.** El programa utiliza una dirección IPv4 local libre y propia para su transporte de red interno. Así puede abrir conexiones mediante las puertas elegidas independientemente de la puerta predeterminada del ordenador, sin modificar las direcciones de los adaptadores del sistema, la tabla de rutas ni la puerta de enlace predeterminada del sistema operativo. El navegador y los demás programas conservan su configuración de red habitual.

Por ejemplo, en una LAN `192.168.1.0/24`, el ordenador mantiene su dirección normal `192.168.1.x`, MultiRoute puede usar la dirección libre `192.168.1.70` y las descargas pueden utilizar `192.168.1.1`–`192.168.1.4`. El ordenador, la dirección virtual y las puertas elegidas deben pertenecer a la misma subred IPv4 real. Compruebe y aplique una dirección libre en los ajustes de red de la aplicación; no la añada manualmente al adaptador del sistema. La IP virtual no es otra conexión a Internet: el ancho de banda lo proporcionan los routers y sus enlaces externos.

#### Elegir y comprobar la dirección

1. Compruebe la IPv4 y la máscara del adaptador Ethernet del ordenador. Elija una dirección de host válida en esa misma subred. `192.168.1.70` es solo un ejemplo: no use la IP del propio ordenador, una puerta de enlace, la dirección de red ni la de difusión.
2. Abra la administración del router y compruebe el rango DHCP real. Use una dirección fuera de la asignación automática o resérvela/exclúyala en el servidor DHCP. Si varios servidores DHCP atienden esa LAN, todos deben impedir que la dirección se asigne a otro dispositivo.
3. En MultiRoute, pulse **Comprobar y aplicar** para la dirección virtual y después compruebe las puertas de enlace. La aplicación valida la subred y busca conflictos mediante ARP. La ausencia de respuesta ARP solo indica que no se detectó un conflicto en ese momento; no demuestra que la dirección esté fuera del rango DHCP ni impide una asignación posterior.

## Capturas de pantalla

A continuación se muestran las ventanas actuales de la aplicación. Abre la imagen a tamaño completo para leer cada control. Los valores de transferencia de ejemplo son ilustrativos.

![MultiRoute Downloader — cola de descargas y velocidades de las puertas de enlace](images/v4.3.7/downloads-en-ae5b55131583.png)

*Interfaz actual de Windows, capturada con un perfil de demostración aislado. Los archivos de ejemplo, el progreso, las velocidades y las vistas habilitadas con Pro ilustran los controles y no son mediciones de rendimiento.*

<table>
<tr>
<td width="50%" valign="top"><strong>Ajustes de la aplicación</strong><br /><a href="images/v4.3.7/settings-8abcb12a782d.png"><img src="images/v4.3.7/settings-8abcb12a782d.png" width="420" alt="Ajustes de la aplicación" /></a></td>
<td width="50%" valign="top"><strong>Apariencia, mensajero y sonidos</strong><br /><a href="images/v4.3.7/appearance-cebf121d5de7.png"><img src="images/v4.3.7/appearance-cebf121d5de7.png" width="420" alt="Apariencia, mensajero y sonidos" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Dirección de la aplicación y DNS protegido</strong><br /><a href="images/v4.3.7/network-dns-e29dfdba1007.png"><img src="images/v4.3.7/network-dns-e29dfdba1007.png" width="420" alt="Dirección de la aplicación y DNS protegido" /></a></td>
<td width="50%" valign="top"><strong>Selección de puertas de enlace y pruebas de velocidad</strong><br /><a href="images/v4.3.7/network-gateways-aee0a7897f48.png"><img src="images/v4.3.7/network-gateways-aee0a7897f48.png" width="420" alt="Selección de puertas de enlace y pruebas de velocidad" /></a></td>
</tr>
</table>






## Entorno necesario

Los instaladores de Windows/macOS incluyen Java 21 privado y el componente de red compilado. Windows requiere por separado el controlador Npcap. No hace falta instalar Java aparte.

| Plataforma | Sistema operativo | Componentes |
| --- | --- | --- |
| Windows x64 | Windows 11 | Java 21 |
| Apple Silicon | macOS 26+ | Java 21 |
| Intel Mac | macOS 15+ | Java 21 |

Se necesita Ethernet por cable en la misma subred IPv4 que todas las puertas de enlace elegidas, una IPv4 virtual libre, carpetas de datos y descarga con escritura y espacio para el archivo y sus partes temporales. La mejora depende de conexiones WAN independientes y del servidor de origen. No se necesita GPU.

La aplicación admite Linux, pero este repositorio de distribuciones no publica un instalador Linux 4.x. No utilice en Linux los paquetes de Windows o macOS.

Antes de actualizar, espere a que terminen las descargas, subidas y verificaciones finales activas. Salga desde la bandeja y ejecute el instalador nuevo completo. Conserve la carpeta de datos, los archivos descargados y las partes guardadas.

## Instaladores nativos 4.3.9: Windows y macOS

| Sistema | Instalador | Inicio después de instalar |
| --- | --- | --- |
| Windows 11 x64 | [MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe) | **MultiRoute Downloader** en Inicio o su acceso directo de escritorio |
| Apple Silicon, macOS 26+ | [MultiRoute-Downloader-4.3.9-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-arm64.pkg) | **MultiRoute Downloader** en Aplicaciones, Spotlight o Dock |
| Intel, macOS 15+ | [MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg) | **MultiRoute Downloader** en Aplicaciones, Spotlight o Dock |

Las doce guías y las capturas están incluidas para consultarlas sin Internet. Windows: `%LOCALAPPDATA%/MultiRoute Downloader App/app/docs/`. macOS: en Finder elija **Mostrar contenido del paquete** en la aplicación instalada y abra `Contents/app/docs/`. Abra `en.md`, `ru.md`, `de.md`, `es.md`, `fr.md`, `zh.md`, `ko.md`, `ar.md`, `it.md`, `ja.md`, `hi.md` o `vi.md` con un editor de texto o visor Markdown.

### Windows: instalar y abrir

1. Descargue el `.exe` y compare su SHA-256 con la publicada. Abra el archivo y siga el asistente. La aplicación se instala para su cuenta; elija las opciones de accesos directos ofrecidas.
2. Abra **MultiRoute Downloader** desde Inicio o el acceso directo. Aparece como aplicación de escritorio, sin ventana de comandos. Java está incluido; no necesita instalarlo por separado.
3. La aplicación comprueba Npcap. Si falta el controlador, confirme su descarga en la aplicación. Se obtiene el instalador oficial fijado, se verifican SHA-256, firma Authenticode y editor, y se abre la instalación normal de Npcap. Lea la licencia y confirme UAC de Windows. Si se pide reiniciar, reinicie Windows y abra MultiRoute de nuevo. Npcap es un controlador aparte: **no está incluido ni se instala silenciosamente**. Si falta, este paso necesita Internet.

Aplicación e instalador no tienen firma de editor. SmartScreen puede mostrar un editor desconocido: compruebe la publicación oficial y el archivo antes de elegir **Más información → Ejecutar de todas formas**, si se ofrece. Una detección de malware es otra situación. No desactive el antivirus ni añada excepciones como paso de instalación. Npcap con AdminOnly puede pedir otra autorización de captura. Abra Java como usuario normal, sin ejecutar toda la aplicación como administrador.

### macOS: instalar y abrir

1. Use el `.pkg` correspondiente al procesador **nativo**: arm64 para Apple Silicon, x86_64 para Intel. Los nuevos paquetes exigen **macOS 26+ en Apple Silicon** o **15+ en Intel**, incluido torrent/magnet. El mínimo HTTP del antiguo ZIP portátil no se aplica a estos paquetes.
2. Descargue el `.pkg`, compruebe la SHA-256 publicada y ábralo con el Instalador de Apple. Confirme la instalación y la autorización de administrador para colocar el componente de red protegido.
3. Abra **`/Applications/MultiRoute Downloader.app`** en Finder o Spotlight. Puede añadir su icono al Dock. Java y el componente ya compilado están incluidos: **no necesita Terminal, Command Line Tools, Homebrew ni instalar Java aparte** para este paquete.
4. Cuando se solicite, confirme el diálogo normal de administrador de macOS para que el componente protegido abra el dispositivo Ethernet BPF. Después reduce sus privilegios; la ventana Java funciona con su usuario normal. No necesita mantener una sesión de Terminal abierta.

#### Confirmaciones de seguridad del paquete nativo macOS

El paquete **no está firmado con Developer ID ni notarizado**. Una firma técnica ad hoc no acredita la identidad de un desarrollador Apple. Primero intente abrir normalmente el `.pkg` o la aplicación. Si macOS informa de un desarrollador no verificado y ofrece una excepción, elija **Ajustes del Sistema → Privacidad y seguridad → Abrir igualmente**, luego **Abrir**, únicamente después de comprobar el origen y la suma del archivo. Un Mac administrado puede impedirlo. Consulte [Apple](https://support.apple.com/es-es/102445).

La autorización de instalación, la posterior del componente y la excepción del desarrollador son confirmaciones distintas. Si se solicita **Red local** para MultiRoute/Java, permita el acceso para utilizar sus puertas de enlace LAN. Autorice los permisos de carpeta para el destino elegido. Cancelar no implica ejecutar toda la aplicación Java como root.

El instalador no desactiva Gatekeeper/SIP, no cambia las rutas del sistema, no crea una excepción global de `sudoers` ni elimina globalmente la cuarentena. Un aviso de malware o archivo dañado no es la confirmación normal de desarrollador desconocido: deténgase y compruebe el archivo. El texto depende de macOS y de la política de la organización.

<a name="3-configurar-la-red"></a>

## Configurar la red

Las puertas de enlace de ejemplo editables son `192.168.1.1`, `192.168.1.2`, `192.168.1.3` y `192.168.1.4`. Selecciónelas manualmente para su LAN: Free permite usar hasta **2 puertas de enlace** a la vez y la función Pro correspondiente permite hasta **8**. La conexión habitual del ordenador **no se añade como una puerta de enlace adicional**.

1. Abra los ajustes de red y cambie esas direcciones si su LAN es diferente.
2. Configure la dirección virtual de la aplicación; el valor inicial es `192.168.1.70`.
3. Use la comprobación de dirección y la comprobación de las puertas de enlace antes de descargar. Si otro dispositivo usa la dirección virtual, elija otra. Reserve esa dirección fuera del rango DHCP automático para evitar que el router la asigne posteriormente.
4. Seleccione al menos una puerta de enlace, dentro del límite de su licencia.
5. Use **«Añadir puerta de enlace»** para ampliar la lista: admite entre **1 y 8** direcciones distintas. Se pueden añadir o quitar perfiles mientras hay descargas, siempre que los trabajos afectados conserven al menos una puerta de enlace activa. Para que un trabajo activo use la nueva puerta de enlace, inclúyala también en la selección de ese trabajo.

La dirección virtual pertenece al transporte interno de MultiRoute. **No se añade al adaptador del sistema operativo y no cambia sus rutas**, de modo que las demás aplicaciones conservan su conexión habitual. No escriba esa dirección en la configuración de red de Windows, Linux o macOS. El botón **?** junto a la dirección virtual y las ayudas al dejar el cursor sobre los controles explican esta función.

La prueba de velocidad usa hasta **16 MiB o 5 segundos de transferencia**; la preparación de la conexión puede añadir tiempo. Estos límites solo afectan a la prueba. Una descarga normal no tiene ese límite. Sin un enlace HTTP directo adecuado, se utiliza un archivo de prueba de Selectel.

<a name="4-descargar-reanudar-y-gestionar"></a>

## Descargar, reanudar y gestionar

1. Elija **Español** en el selector de idioma. Los nuevos perfiles comienzan en inglés; también hay ruso, alemán, francés, chino, coreano, árabe, italiano, japonés, hindi y vietnamita. Se conserva el idioma guardado.
2. Pegue la URL en el campo largo mediante **Ctrl+V**; en macOS también funciona **Cmd+V**. Existe además el botón de pegar. Puede introducir magnet o abrir un archivo `.torrent`.
3. Elija la carpeta mediante el selector de su sistema. El destino inicial de nuevos perfiles es la carpeta `Downloads` de su usuario; confirme el destino antes de empezar.
4. Seleccione las puertas de enlace y pulse el botón de iniciar junto a la URL. Para una página de Hugging Face, WebDAV o Metalink, revise la lista y seleccione los archivos que desee.
5. Observe la velocidad total, velocidad y volumen de cada puerta de enlace, tiempo transcurrido, tiempo restante aproximado y gráfico opcional.

Los segmentos HTTP son de **20 MiB por defecto**, ajustables entre 1 y 64 MiB. Los routers toman segmentos de una cola común: el más rápido puede descargar más partes. Si un segmento deja de recibir datos durante **60 segundos**, su transferencia se cancela y la parte vuelve al conjunto disponible para otro router. Las piezas torrent utilizan el tamaño definido por sus metadatos, independientemente del tamaño HTTP configurado.

Puede pausar, continuar, cambiar las puertas de enlace de un trabajo activo, ajustar prioridad, límites o programación y cambiar la carpeta de un trabajo detenido. La cola permite dos trabajos activos por defecto. Las descargas que estaban ejecutándose se recuperan pausadas después de reiniciar; use continuar cuando corresponda.

La importación masiva admite listas de enlaces y archivos de texto. También hay búsqueda, filtros, menú contextual y selección de archivos torrent. La distribución voluntaria de un torrent completado está desactivada por defecto; si la activa, configure sus límites de tiempo y proporción.

Seleccione varias filas de descarga con **Ctrl+clic** en Windows/Linux o **Cmd+clic** en macOS; **Shift+clic** selecciona un intervalo. Con el foco en la tabla, **Ctrl+A** o **Cmd+A** selecciona todas las filas visibles según la búsqueda y el filtro de estado. La acción de eliminar se aplica a toda la selección con una sola confirmación. La opción de borrar también los archivos completos y las partes descargadas está **desmarcada por defecto**, por lo que puede quitar solo las entradas y conservar los datos. Márquela únicamente si desea borrar los archivos de esos trabajos; se conservan los demás archivos y la carpeta de destino. El resultado indica cuántas entradas se eliminaron. Los trabajos que no pudieron eliminarse permanecen en la lista con un resumen de errores para revisarlos y volver a intentarlo; las eliminaciones correctas no se repiten. Eliminar trabajos o archivos no borra el historial permanente de descargas.

<a name="5-protocolos-y-comprobación"></a>

## Protocolos y comprobación

| Fuente | Funcionamiento y condiciones |
| --- | --- |
| HTTP/HTTPS | Descarga por segmentos cuando el servidor permite rangos y ofrece identidad segura del archivo. Búsqueda de SHA-256 publicada y verificación final cuando está disponible. |
| Hugging Face | Enlaces a archivos y selección desde repositorios; resoluciones del Hub/Xet utilizadas para la descarga. El acceso privado requiere credenciales del origen y sus permisos correspondientes. |
| FTP | Modo pasivo EPSV/PASV. FTP sin TLS no cifra las credenciales ni los datos. |
| FTPS | TLS explícito en la conexión FTP, incluido el canal de datos; no es FTPS implícito. |
| SFTP | SSH con una huella **SHA256 de la clave del servidor verificada y fijada**. Configure las credenciales y la huella del servidor antes de conectar. No acepta silenciosamente claves desconocidas. |
| WebDAV | Listado y descarga por HTTPS; seleccione los archivos del listado. |
| Metalink | Archivos `.meta4`, SHA-256 de confianza, espejos y hashes de partes cuando los metadatos los incluyen. |
| Torrent/magnet | Piezas verificadas frente a los metadatos torrent, selección/prioridad de archivos y transferencia por las puertas de enlace seleccionadas. |

Configure usuarios, contraseñas, Bearer tokens, cookies o cabeceras permitidas en los ajustes de acceso **del origen** cuando la fuente lo necesite. Estos datos son distintos de las puertas de enlace; sus routers no requieren tokens para este programa. Las credenciales guardadas se cifran localmente y se restringen a su usuario; no coloque contraseñas dentro de la URL.

Si un servidor HTTP ignora los rangos, se utiliza un flujo completo mediante **una** puerta de enlace: no hay distribución ni reanudación por segmentos, y una interrupción exige comenzar ese flujo desde cero. FTP/FTPS/SFTP sin una SHA-256 o SHA-512 publicada o proporcionada de confianza también recurren al flujo completo.

No todos los servidores publican SHA-256. El programa distingue entre calcular una suma local y **compararla con la publicada por el origen**. Una suma local por sí sola no confirma que el archivo coincida con el del editor. En torrent, la verificación corresponde a sus metadatos y no identifica por sí sola a su autor. Puede proporcionar una SHA-256 de confianza si dispone de ella.

<a name="multiroute-api"></a>

<a name="6-api-para-otras-aplicaciones"></a>

## API para otras aplicaciones

Mientras MultiRoute esté abierto, su API está en `http://127.0.0.1:8765/api/v1`. Los cuerpos y respuestas son JSON UTF-8. No se necesita un token para crear o gestionar trabajos. Solo admite conexiones locales: no publique este puerto como un servicio remoto.

Comprobación:

```http
GET http://127.0.0.1:8765/api/v1/health
```

Crear una descarga mediante `POST http://127.0.0.1:8765/api/v1/jobs`, con `Content-Type: application/json`:

```json
{
  "url": "https://example.org/large-file.bin",
  "directory": "C:/Users/your-user/Downloads",
  "chunk_mb": 20,
  "routes": [
    {"type": "gateway", "gateway": "192.168.1.1", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.2", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.3", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.4", "enabled": true}
  ]
}
```

Sustituya la URL y la carpeta por las reales. En Linux, por ejemplo, use `/home/your-user/Downloads`; en macOS, `/Users/your-user/Downloads`. La carpeta pertenece al ordenador que ejecuta MultiRoute. Indique explícitamente las puertas de enlace en `routes`: las casillas de la ventana no se copian automáticamente al pedido API. No envíe direcciones de agentes ni tokens de router.

`HTTP 201` indica creación del trabajo, no finalización. Conserve el `id` de la respuesta. Consulte `GET /jobs/{id}` hasta `status: "complete"`, o examine `error` si falla. Las velocidades API `speed_bps` están en **bytes por segundo**; para Mbit/s multiplique por 8 y divida entre 1 000 000. La API también permite pausa/continuación, rutas de trabajo, importación por lotes, cola y eventos SSE `/events`.

La configuración inicial de red se establece en la ventana o con `POST /network`; consulte los esquemas en [openapi.yaml](../openapi.yaml) y la descripción detallada [API.ru.txt](../API.ru.txt) (en ruso). Las claves JSON y los valores de estado no cambian al elegir idioma.

<a name="7-datos-actualización-y-desinstalación"></a>

## Datos, actualización y desinstalación

### Instaladores nativos 4.3.9

| Sistema | Ajustes, historial y registros | Aplicación/Java | Componente de red |
| --- | --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App`, con Java incluido | Componente x64 incluido; Npcap instalado aparte |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app`, con Java | `/Library/PrivilegedHelperTools/MultiRouteResume/` |

Los archivos descargados se guardan en el destino elegido, que puede estar fuera de esas carpetas. En macOS la aplicación nativa está en `/Applications/MultiRoute Downloader.app`.

Para actualizar, cierre la aplicación y despliegue el nuevo paquete completo con el lanzador de su plataforma. En Linux, vuelva a ejecutar el despliegue si mueve la carpeta del programa. Conserve el historial y las partes si quiere reanudar; no elimine la carpeta de descargas para desinstalar. Quitar la ventana/lanzador no elimina automáticamente el JRE privado, datos, componentes protegidos ni dependencias instaladas del sistema.

<a name="8-problemas-frecuentes"></a>

## Problemas frecuentes

Para informar de un fallo, indique versión, plataforma/arquitectura, etapa y mensaje exacto. Oculte credenciales, cookies, URLs privadas y datos personales antes de compartir registros.

<a name="bandeja-del-sistema-y-salida-40"></a>

## Bandeja del sistema y salida

Cerrar la ventana principal mantiene la aplicación y las descargas en la bandeja del sistema. El icono muestra la velocidad total de descarga de **todos los trabajos**, sin depender de la selección ni del filtro. La información al pasar el ratón muestra el valor exacto en Mbit/s o Gbit/s (desde 1 Gbit/s), además de bytes/s, hasta 10 Gbit/s; el icono pequeño lo redondea. Elija **Abrir aplicación** en el menú de la bandeja para restaurar la ventana. En Windows/Linux también puede pulsar el icono. **Salir de la aplicación**, en la bandeja o ventana, guarda el estado y termina el programa. Utilice esta opción antes de actualizar.

Sin soporte de bandeja, la ventana se minimiza a la barra de tareas y las descargas continúan; Salir sigue disponible. Si desaparece la bandeja, vuelve a abrirse la ventana oculta. Windows puede situar el icono en el área de notificaciones ocultas.

El setup crea un acceso directo de escritorio y, en Windows, una entrada de Inicio. macOS crea un enlace Desktop a la `.app` instalada para el usuario conectado y vuelve a intentarlo en el primer inicio gráfico. Los elementos existentes se conservan. Si la privacidad impide escribir en Desktop, permita el acceso solicitado o abra desde Aplicaciones. Nuestros textos del setup son ingleses; los diálogos de seguridad y permisos del sistema siguen el idioma del SO.

## Licencia

Puede instalar y usar gratuitamente la aplicación original para uso personal o en organizaciones, incluidas actividades empresariales. **Se prohíbe vender la aplicación, cobrar por su licencia o distribuirla por dinero** sin permiso separado. La licencia propia no concede derechos generales de modificación o redistribución y no es una licencia de código abierto aprobada por OSI. Los componentes de terceros conservan sus licencias. Este texto es un resumen traducido; rige el texto inglés de [LICENSE.txt](../LICENSE.txt).

El icono de velocidad tiene fondo transparente y cifras y unidades negras, incluso a 10 Gbit/s. Muestra la velocidad total y ofrece los comandos de la bandeja descritos arriba.

## Verificación MD5, SHA-256 y SHA-512

Tras ensamblar el archivo, la aplicación calcula las tres sumas del archivo completo en una sola lectura. Pegue una referencia en el campo principal opcional con Ctrl+V o Cmd+V. El tipo se reconoce automáticamente: 32 caracteres hexadecimales corresponden a MD5, 64 a SHA-256 y 128 a SHA-512. La sección desplegable de sumas adicionales permite introducir varias referencias.

Las referencias publicadas se buscan en encabezados compatibles del resumen de toda la representación y en archivos auxiliares: `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512` y `SHA512SUMS`. El resumen de una respuesta HTTP parcial no se considera la suma del archivo completo. Se mantienen las comprobaciones específicas de Hugging Face LFS y Metalink.

Todas las referencias conocidas deben coincidir antes de publicar el archivo final. Una discrepancia marca la tarea como fallida y conserva los datos parciales. Sin referencia, el valor es una suma calculada localmente, no una coincidencia confirmada con el origen. MD5 detecta daños accidentales; SHA-256 o SHA-512 permiten una comprobación de integridad más sólida. MD5 no sustituye la identidad segura del archivo ni los requisitos de espejos de confianza.

La verificación de piezas torrent sigue siendo obligatoria y es independiente de las sumas de los archivos ensamblados. Una referencia manual del archivo completo exige seleccionar exactamente un archivo; para un grupo, proporcione una referencia propia a cada tarea.

La API local acepta `expected_md5`, `expected_sha256` y `expected_sha512` en `POST /api/v1/jobs`. Los resultados incluyen `md5`, `sha256`, `sha512` y los orígenes de referencia en `checksum_sources`. Las peticiones existentes con `expected_sha256` siguen siendo compatibles.

<a name="progreso-de-cada-descarga-40"></a>

## Progreso de cada descarga

Cada descarga tiene su propia barra en la columna Progreso. Las columnas ocupan todo el ancho del panel de descargas. La lista permite desplazarse verticalmente cuando hay muchas tareas; los paneles estrechos también permiten desplazarse horizontalmente sin comprimir las columnas hasta hacerlas ilegibles. Al actualizar se mantiene la primera tarea visible. La ordenación y los filtros mantienen cada barra vinculada a su descarga. Si se desconoce el tamaño total, se muestran los datos recibidos en lugar de un porcentaje; la ayuda emergente muestra tamaño, estado y errores. La fila seleccionada sigue mostrando progreso detallado, tiempos y velocidades de las puertas de enlace.

La carpeta de descarga predeterminada se determina al iniciar para el usuario actual: `%USERPROFILE%\Downloads` en Windows y `~/Downloads` en macOS/Linux. La carpeta predeterminada guardada sigue el perfil actual; se conserva cualquier carpeta elegida manualmente.

<a name="visor-del-registro-36"></a>

## Visor del registro

La ventana del registro mantiene la posición de lectura al actualizar automática o manualmente. Al desplazarse al final, las nuevas entradas permanecen visibles. Desplazarse hacia arriba pausa el seguimiento, sin detener el registro. Se conserva la selección de texto para copiar. Al cambiar el idioma se mantiene el evento visible; al volver a abrir la ventana se muestran las entradas más recientes.

<a name="detalles-de-descarga-e-informe-de-sumas-37"></a>

## Detalles de descarga e informe de sumas

El panel inferior de detalles de la descarga seleccionada mantiene la posición de lectura y la selección de texto durante las actualizaciones automáticas repetidas. Puede leer el final del informe de sumas sin que el panel vuelva al principio. Al seleccionar otra descarga, su informe se abre al principio. La ventana independiente del registro conserva su comportamiento de desplazamiento.

<a name="instrucciones-y-guía-de-la-api-38"></a>

## Instrucciones y guía de la API

Los botones **Instrucciones** y **Guía de la API** están arriba a la derecha, en una fila independiente debajo del selector de idioma. Abren la guía en el idioma seleccionado en el navegador predeterminado. **Guía de la API** lleva directamente a la sección de la API local. Los enlaces corresponden a la versión de la aplicación; se necesita Internet para abrir las páginas de GitHub.

<a name="interfaz-de-escritorio-e-historial-de-descargas-40"></a>

## Interfaz de escritorio e historial de descargas

La interfaz utiliza FlatLaf. Elija **Graphite** o **System** en **Ajustes de apariencia**; los estilos adicionales requieren la función Pro correspondiente. La elección se guarda localmente. La cola mantiene barras individuales, columnas que llenan el ancho, ordenación y filtros. El resumen muestra todos los trabajos, los activos, los completados y la velocidad agregada de todos, independientemente del filtro.

Pase el puntero sobre una fila activa o completada para ver el **nombre completo del archivo y su ruta de destino absoluta**, incluso después de ordenar o filtrar. La cabecera del archivo seleccionado ofrece la misma ayuda. **Copiar ruta completa** en el menú contextual copia el destino.

**Ctrl+L** en Windows/Linux o **Cmd+L** en macOS enfoca la URL; **Ctrl+F** o **Cmd+F** enfoca la búsqueda; **Escape** borra la búsqueda. Se conserva pegar con Ctrl+V/Cmd+V.

La descarga seleccionada tiene pestañas **Detalles** e **Historial**. El historial registra eventos con fecha y hora: primer inicio, cada reanudación y finalización correcta. No se inventan fechas para trabajos antiguos sin registros. Más abajo se describen el almacenamiento y los campos de la API.

La aplicación admite protocolos de descarga, selección de puertas de enlace, IP virtual, pruebas de velocidad, importación por lotes, acceso a fuentes, MD5/SHA-256/SHA-512, bandeja, API local y lectura con desplazamiento del registro y los detalles. La interfaz no cambia las rutas del sistema.

### Almacenamiento del historial y API

La carpeta de datos contiene `logs/downloads-history.jsonl` (UTF-8, un objeto JSON por línea) y `logs/downloads-history.log` (entradas legibles con fecha). Se añaden eventos tras reinicios y se conservan al eliminar un trabajo o sus archivos. El registro de texto mantiene el idioma al escribir el evento; la interfaz muestra los eventos guardados en el idioma actual. La ventana lee solo los últimos 1.000 eventos / 2 MiB como máximo; el archivo completo queda en disco y crece con el uso. Archívelo aparte si lo necesita. Contiene nombres y rutas de destino completas, pero no URL de origen, contraseñas ni tokens. Revise las rutas antes de compartirlo.

Windows nativo: `%LOCALAPPDATA%\MultiRoute Downloader\data\logs\`. macOS nativo: `~/Library/Application Support/MultiRoute Downloader/data/logs/`. Linux y perfiles portátiles usan la carpeta de datos descrita arriba; `--data-dir` permite elegir otra.

`GET /api/v1/jobs` y la respuesta de cada trabajo incluyen `lifecycle_history`. Cada evento tiene `event_id`, `job_id`, `kind`, `time`, `filename` y `path`; los torrents pueden añadir `paths` y las pausas `reason`. Tipos: `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`. Las fechas usan ISO 8601 UTC, por ejemplo `2026-10-18T12:34:56.123Z`; el registro legible incluye fecha, hora local y desplazamiento UTC. Los campos opcionales `first_started_at`, `last_resumed_at`, `completed_at` resumen eventos observados. Estar en cola no cuenta como inicio. La finalización se registra tras verificar y publicar el archivo. `recovered` indica verificación actual de un archivo publicado antes, no una fecha histórica de finalización conocida. No se inventan fechas antiguas. Las operaciones existentes se conservan; no hace falta otro endpoint.

La ventana general **Registro** separa **Eventos de la aplicación** e **Historial de descargas**. El historial muestra eventos fechados de distintos trabajos, incluidos los eliminados; el historial de la descarga seleccionada corresponde solo a ese trabajo.

## Perfiles guardados de la aplicación

Los perfiles usan las rutas de datos indicadas arriba. La carpeta elegida contiene ajustes, historial de descargas y partes guardadas; conserve esa carpeta y los archivos descargados al actualizar. La aplicación instalada utiliza el directorio protegido del componente de red `MultiRouteResume` e identificadores internos de la aplicación. Ábrala desde el acceso directo o la `.app` de MultiRoute Downloader.

<a name="sitios-guardados-subidas-y-vista-previa-de-archivos-40"></a>

## Sitios guardados, subidas y vista previa de archivos

Abra **Perfiles de sitios…**, cree/edite un perfil y selecciónelo en **Descargas** o **Subidas**. Los controles de puertas e IP virtual están en **Configuración de red**. En **Subidas**, escriba la **URL de la carpeta remota** y pulse **Elegir archivos y subir…** para elegir uno o varios archivos locales con el selector del sistema. La interfaz añade los nombres; API `target_url` identifica un archivo remoto completo. Para una vista previa, pegue la URL del archivo comprimido en el campo principal y pulse **Contenido del archivo…**; no inicia la descarga.

Guarde un perfil con nombre, URL base, carpeta local, carpeta remota y puertas de enlace. Admite acceso anónimo, HTTP Basic/Bearer/Cookie/cabeceras propias y credenciales FTP/SFTP con huella SHA-256 del servidor verificada previamente. Puede guardar varias cuentas del mismo servidor de forma aislada. Las carpetas/puertas explícitas prevalecen; las URL relativas se resuelven con la URL base. Los secretos se cifran en `site-profiles/vault.bin` con la clave local `site-profiles/key.bin` y permisos del propietario. No protege frente a quien controle su cuenta y clave. La API no devuelve secretos. Los campos vacíos conservan credenciales solo si origen y tipo no cambian; `replace_credentials:true` las sustituye. Una redirección HTTP a otro origen no recibe las credenciales. Eliminar el perfil conserva tareas y archivos locales/remotos; la tarea conserva su perfil original y no puede cambiar de cuenta; si se eliminó ese perfil, cree de nuevo la tarea con uno válido cuando sea necesario.

Las subidas y la exploración remota admiten **FTP, FTPS explícito y SFTP** mediante las puertas elegidas. Seleccione un archivo local normal y una URL completa de archivo remoto (`target_url`), no solo un directorio. Se usa un archivo temporal; reanudar exige comparar el prefijo existente. La finalización requiere releer todo el archivo remoto y comprobar SHA-256, con permisos de **lectura, escritura y cambio de nombre**. La lectura adicional consume ancho de banda. Una suma distinta o lectura denegada es un error. Cada archivo usa una puerta a la vez; varios archivos se distribuyen y suben en paralelo. Tras el límite de inactividad puede utilizarse otra puerta. No se divide una misma subida entre puertas. Pausa, reanudación y progreso persisten. Eliminar la tarea conserva el archivo local y los remotos finales/temporales.

La sobrescritura está desactivada inicialmente. FTP/SFTP no garantizan universalmente un cambio de nombre atómico solo si el destino no existe; otro cliente puede modificarlo durante la publicación. Coordine los destinos compartidos. Listar carpetas no sube ni elimina archivos.

La vista previa lista nombres, rutas, tamaños, carpetas y cifrado de **ZIP, TAR, 7Z, RAR4 y RAR5**, sin extraer ni ejecutar. Requiere una puerta, rangos de bytes y ETag o Last-Modified estable. Límites: 16 MiB de metadatos, 10.000 entradas, 60 segundos; cabeceras 7z con hasta 64 MiB de memoria. Puede ser parcial y no verifica el contenido. Elija la codificación original si los nombres fallan. Las cabeceras 7z cifradas compatibles admiten contraseña no persistida. Las cabeceras RAR cifradas se indican como no compatibles/contraseña necesaria; los volúmenes divididos pueden mostrar listas parciales.

La API acepta `site_profile_id` para descargas, subidas, directorios y vistas previas. `GET /api/v1/site-profiles` devuelve `{profiles:[...]}`; `POST` o `PUT` guarda un perfil plano y devuelve metadatos públicos; `DELETE /api/v1/site-profiles/{id}` elimina solo ese perfil. `POST /api/v1/uploads` recibe `local_path`, `target_url`, `routes`, `site_profile_id` opcional y `overwrite:false`; devuelve HTTP 201. `GET /api/v1/uploads` devuelve `{uploads:[...]}`. Hay GET/DELETE individuales y POST `/{id}/pause` / `/{id}/resume`. `POST /api/v1/remote/list` recibe `url` y `route`/`routes`; `POST /api/v1/archives/preview` añade `password` y `charset` opcionales. Consulte [openapi.yaml](../openapi.yaml). Los manifiestos UTF-8 de `data/uploads/` guardan ID de perfil, no contraseñas. La vista previa no crea una descarga.

## Programación y prioridad de descargas

Configure `max_active_jobs` (1–32), límites globales/por puerta y ventana diaria (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm) en los controles de cola/programación. Se usa la zona horaria local, expuesta como `schedule_timezone`. Se permite cruzar medianoche; inicio igual a fin significa todo el día. Fuera de ventana las descargas vuelven a la cola; las pausadas manualmente siguen pausadas. `priority` de -100 a 100 ordena las pendientes de mayor a menor y luego por creación; no interrumpe tareas activas. `start_after_ms` es el inicio mínimo opcional en milisegundos Unix; 0 elimina la demora. Persiste entre reinicios y afecta a descargas, no a la cola de subidas independiente. Mantenga la aplicación y el ordenador despiertos; no programa el despertar del sistema. API: `GET`/`PUT`/`POST /api/v1/queue` y `PUT`/`POST /api/v1/jobs/{id}/options`.

## Integración con Chrome y Edge

Abra la integración **Chrome / Edge** en la aplicación, permita enviar enlaces y abra la carpeta de extensión incluida con el instalador nativo. Vaya a `chrome://extensions` o `edge://extensions`, active el modo de desarrollador y use **Cargar descomprimida** con esa carpeta. Mantenga la carpeta elegida en su ubicación.

MultiRoute debe estar abierto, incluso en la bandeja. Envíe un enlace desde su menú contextual o la ventana de la extensión. Se usan carpeta, puertas y perfil actuales; la selección de repositorio/torrent sigue disponible. El puerto es 8765; ajústelo si cambió el de API. Solo se envía URL, sin cookies ni contraseñas; no intercepta otras descargas del navegador. Configure el acceso al origen en la aplicación. Si el navegador pide permiso de conexión local, concédalo solo a esta extensión elegida.

El puente está desactivado inicialmente y acepta solo `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` en `/api/v1/browser/status` y `/api/v1/browser/download`; las otras reglas API no cambian. HTTP 202 confirma entrega a la interfaz, no descarga completada. Solo un servidor headless no puede abrir la selección de escritorio. Para desactivar, quite la extensión o el permiso en la aplicación.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
