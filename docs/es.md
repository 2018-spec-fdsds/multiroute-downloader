# MultiRoute Downloader 4.1.3 — Guía en español

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · **Español** · [Français](fr.md) · [Inicio](../README.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

**Correcciones de 4.1.3:** si un CDN como Civitai devuelve HTTP 200 para un intervalo que empieza en cero, se comprueba otro intervalo que empieza después de cero. El primer segmento se limita a su tamaño; los datos secuenciales guardados compatibles pueden continuar como segmentos. Un HTTP 200 temporal para un intervalo posterior se reintenta tras 2–30 segundos, conservando los datos y el trabajador de la puerta de enlace. El registro persistente por puerta de enlace distingue el tráfico recibido del progreso guardado. [Notas y límites de validación](releases/4.1.3.en.md) · [Diagnóstico de transferencias](transfer-diagnostics.en.md).

[Los instaladores 4.1.3](../downloads/v4.1.3/) incluyen Java 21 privado. El asistente de Windows está en inglés y crea un acceso directo en el escritorio; Npcap se requiere por separado. macOS usa libpcap del sistema. Siga las instrucciones de instalación y seguridad que aparecen abajo.

MultiRoute Downloader es una aplicación de escritorio Java para descargar archivos a través de una o varias **puertas de enlace** de su red local. Distribuye los segmentos disponibles entre ellas, conserva los datos comprobados y reúne el archivo final. Incluye una API local para otras aplicaciones. La ventana de escritorio no es una interfaz web.

Titular del proyecto: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## ¿Para qué sirve?

Si su red dispone de varias conexiones independientes a Internet, por ejemplo una línea fija y un router móvil 4G/5G, la aplicación aprovecha su ancho de banda simultáneamente. El equipo accede a las puertas de enlace a través de la misma red Ethernet local.

- **Mayor velocidad total:** todas las puertas de enlace seleccionadas y disponibles descargan segmentos en paralelo desde una cola compartida. Las más rápidas pueden obtener más segmentos.
- **Resistencia a fallos:** en las descargas segmentadas reanudables, las demás puertas de enlace siguen trabajando si una falla. Los segmentos incompletos vuelven a la cola para otra puerta de enlace y se conservan los datos verificados.
- **Descargas por lotes:** importe una lista de URL, gestione varios archivos en la cola o envíe un grupo de descargas mediante la API local.

La mejora de velocidad depende de la capacidad independiente de las conexiones y de los límites del servidor. Reanudar y reasignar segmentos requiere que el origen y el protocolo permitan una reanudación segura.

**Solo ZIP portátil 3.0:** descargue el paquete de su plataforma desde [GitHub Releases](https://github.com/2018-spec-fdsds/multiroute-downloader/releases) y extraiga **todo** su contenido. Los lanzadores necesitan el JAR, las bibliotecas y los componentes nativos que acompañan al paquete. El archivo «Source code» de GitHub corresponde al código fuente: compílelo primero o use un paquete listo para ejecutar. Los instaladores comprueban los archivos de la aplicación antes de descargar dependencias.

## Capturas de pantalla

Interfaz de escritorio real en inglés, dibujada desde un **perfil de demostración** aislado. Los trabajos y contadores son datos de ejemplo, no descargas reales ni velocidades de Internet medidas.

![Descargas: cola, progreso y estadísticas por puerta de enlace](images/desktop-downloads-en.png)

<details>
<summary>Ajustes de red e importación en grupo</summary>

Cuatro puertas de enlace seleccionadas y una dirección virtual de la aplicación. La demostración no comprueba puertas de enlace ni velocidades.

![Cuatro puertas de enlace seleccionadas y una dirección virtual de la aplicación. La demostración no comprueba puertas de enlace ni velocidades.](images/desktop-network-en.png)

Importación en grupo: una URL por línea, revisión antes de añadir.

![Importación en grupo: una URL por línea, revisión antes de añadir.](images/desktop-bulk-import-en.png)

</details>

## 1. Entorno necesario

Los nuevos instaladores Windows/macOS descritos más abajo incluyen Java y el componente compilado. La selección de Java, su descarga privada y la compilación local de esta sección corresponden a los **ZIP portátiles 3.0 y al despliegue Linux**. Windows sigue necesitando Npcap aparte. Los nuevos paquetes macOS tienen el mínimo más estricto 26+/15+ indicado abajo.

| Plataforma | Sistema y arquitectura | Java y componentes adicionales |
| --- | --- | --- |
| Windows | Windows 11 de 64 bits, x86-64 nativo | Java 17 o posterior de 64 bits; controlador Npcap. El componente de red incluido es Windows x64; Windows ARM64 no está incluido. |
| Linux | Distribución con glibc, x86-64 o ARM64/aarch64; **glibc 2.38+ para torrent/magnet** | Java 17 o posterior de la arquitectura nativa; `sudo`; compilador C, binutils, pkg-config y archivos de desarrollo de libpcap. Para la ventana: X11, o Wayland con XWayland, y bibliotecas X11/fuentes. Alpine/musl no está incluido. |
| macOS | El instalador admite macOS 11+; Intel x86-64 o Apple Silicon ARM64. **Torrent/magnet: macOS 15+ en Intel, macOS 26+ en Apple Silicon** por el JNI incluido. | Java 17 o posterior de la arquitectura nativa; Apple Command Line Tools; libpcap del sistema y acceso BPF mediante el componente autorizado con `sudo`. No requiere Homebrew ni Xcode completo. |

Use un **JRE/JDK completo**, con los módulos `java.desktop`, `java.net.http`, `jdk.httpserver` y `java.xml`. El modo `--headless` evita la ventana; sigue necesitando un entorno Java compatible, no una imagen mínima recortada que omita esos módulos. Los lanzadores comprueban módulos y arquitectura.

La biblioteca nativa incluida para torrent impone requisitos adicionales al instalador Java: se comprobó sin ejecutarla que necesita glibc 2.38 en Linux y las versiones macOS indicadas en la tabla. El transporte HTTP, con su componente compilado localmente, tiene requisitos diferentes; no se ha certificado su compatibilidad con todas las distribuciones o versiones anteriores. El instalador advierte de bibliotecas torrent incompatibles, pero **no actualiza el sistema operativo ni sustituye glibc**. Para usar torrent en un equipo incompatible, actualice normalmente a un sistema admitido. No sustituya manualmente la libc para forzar la carga. La biblioteca JNI Linux incluida no exige dinámicamente `libstdc++.so.6` ni versiones `GLIBCXX`/`CXXABI`; otras dependencias de su distribución pueden necesitar sus propios paquetes.

Requisitos comunes:

- Adaptador **Ethernet por cable**, activo y conectado a la misma subred IPv4 que todas las puertas de enlace seleccionadas. Cada router debe tener una salida a Internet que funcione para esa dirección virtual. Para sumar capacidad se necesitan salidas WAN con ancho de banda independiente; compartir la misma conexión o los límites del servidor puede impedir aumentar la velocidad.
- Una dirección IPv4 libre de esa subred para el tráfico de la aplicación. No utilice la dirección del propio ordenador, de otro dispositivo, de red ni de difusión.
- Carpeta de programa y carpeta de descargas escribibles por su usuario.
- El despliegue de Linux/macOS y la instalación de Java privado en Windows comprueban **512 MiB libres**, además del espacio de las descargas. Es una comprobación del instalador, no un límite de archivo. Las herramientas de desarrollo de Apple pueden necesitar bastante más espacio que esa comprobación.
- Para torrent, espacio adicional para partes y almacenamiento temporal. Reserve varios tamaños del contenido seleccionado: la comprobación conservadora considera las piezas completas, que pueden incluir límites entre archivos, además del destino y los temporales. La cantidad necesaria no es un múltiplo exacto universal del tamaño de los archivos elegidos.
- **Recomendación:** 4 GB de RAM o más para un uso cómodo; aumente la memoria disponible cuando use muchos routers o torrents simultáneos. No existe una comprobación de RAM mínima fija en este instalador. No necesita una GPU, CUDA ni modelos de IA para descargar.

La versión 3.0 utiliza un transporte IPv4 sobre Ethernet. Wi-Fi, una conexión disponible solamente por VPN y fuentes accesibles únicamente por IPv6 **no son configuraciones admitidas** para este transporte. Tampoco promete sumar el ancho de banda si varios routers comparten la misma conexión, o si el servidor limita la descarga.

El idioma elegido necesita fuentes instaladas con sus caracteres. En Linux mínimo instale paquetes de la distribución, por ejemplo Noto CJK para chino/coreano y una familia Noto árabe. Java usa las fuentes de sustitución de la plataforma; la aplicación no instala ni incluye paquetes de fuentes. La cobertura depende del equipo.

## Instaladores nativos 4.1.3: Windows y macOS

Elija el instalador de su sistema en [GitHub Releases](https://github.com/2018-spec-fdsds/multiroute-downloader/releases), si está adjunto a la publicación. Cada paquete es **un solo archivo de instalación**, con aplicación, lanzador nativo, Java completo y componente de red. Las fuentes y los ZIP portátiles 3.0 son distribuciones distintas.

| Sistema | Instalador | Inicio después de instalar |
| --- | --- | --- |
| Windows 11 x64 | `MultiRoute-Downloader-4.1.3-Windows-x64-Setup.exe` | **MultiRoute Downloader** en Inicio o su acceso directo de escritorio |
| Apple Silicon, macOS 26+ | `MultiRoute-Downloader-4.1.3-macOS-arm64.pkg` | **MultiRoute Downloader** en Aplicaciones, Spotlight o Dock |
| Intel, macOS 15+ | `MultiRoute-Downloader-4.1.3-macOS-x86_64.pkg` | **MultiRoute Downloader** en Aplicaciones, Spotlight o Dock |

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

## ZIP portátiles 3.0 y Linux: instalación y primer inicio

### Windows

1. Extraiga el paquete Windows completo a una carpeta permanente y escribible, por ejemplo dentro de su carpeta de usuario.
2. Haga doble clic en `Deploy-Windows.cmd`. Comprueba Windows/CPU, los archivos de la aplicación, Java y Npcap; abre la aplicación cuando están preparados.
3. Si falta Java compatible, descarga una versión fijada del **JRE oficial Eclipse Temurin 21**, verifica su SHA-256 y la instala en `runtime/` de la aplicación. No sustituye Java del sistema. Puede reutilizar sin conexión una instalación válida de `JAVA_EXE`, `runtime/bin/java.exe`, `JAVA_HOME` o `PATH`.
4. Si falta Npcap, descarga su instalador de **npcap.com**, verifica la firma Authenticode y el editor, y abre la instalación **interactiva**. Lea la licencia, elija opciones y confirme **UAC** si desea instalar el controlador. Npcap no se incluye en el ZIP ni se instala silenciosamente; no se da permiso de administrador a toda la aplicación Java.

En los siguientes inicios use `Start-Windows.cmd`: hace las mismas comprobaciones y puede instalar componentes que falten. Se necesita Internet para descargar archivos o dependencias ausentes. Consulte la [instalación oficial de Temurin](https://adoptium.net/installation/) y la [guía de Npcap](https://npcap.com/guide/npcap-users-guide.html).

El lanzador puede usar `JAVA_EXE`, un Java privado en `runtime/bin/java.exe`, `JAVA_HOME` o Java disponible en `PATH`. Para indicar un ejecutable concreto desde `cmd.exe`:

```bat
set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe"
Start-Windows.cmd
```

Cambie esa ruta por la real. Si define explícitamente un `JAVA_EXE` incorrecto o incompatible, la instalación de Windows se detiene: corrija o quite esa variable y reintente. No ignora silenciosamente el ejecutable elegido. La instalación de Npcap requiere autorización administrativa. Si se instaló con **«Restrict Npcap driver's access to Administrators only»**, el proceso de captura necesita autorización y Npcap puede mostrar otro UAC al abrir el dispositivo. Una sesión sin esos permisos no puede utilizar el controlador. El instalador no cambia el antivirus ni añade excepciones.

Para ejecutar solamente la API:

```bat
Start-Windows.cmd --headless
```

### Linux

1. Extraiga el paquete a una carpeta permanente de su usuario.
2. Abra `Deploy-Linux.desktop`. Si su gestor de archivos no admite este lanzador portátil, abra una terminal en la carpeta y ejecute:

   ```bash
   bash ./Deploy-Linux.sh
   ```

3. Introduzca su contraseña cuando `sudo` la solicite. Si faltan dependencias, el instalador usa paquetes concretos mediante `apt-get`, `dnf` o `pacman`; no hace una actualización completa de la distribución. Arch necesita sus repositorios actualizados mediante el mantenimiento normal de la distribución.
4. El instalador compila el componente de red para su equipo, ejecuta su prueba sin tráfico de red e instala una copia protegida propiedad de `root`.
5. La ventana Java se inicia con su usuario normal. **Mantenga abierta la terminal** mientras usa la aplicación: la autorización temporal del componente está ligada a esa sesión.

Si encuentra un Java 17+ nativo adecuado, lo reutiliza. Si no, descarga una versión fijada del JRE oficial Temurin 21, comprueba su SHA-256 y la instala **solo para esta aplicación**, sin sustituir Java del sistema. Necesita Internet para descargar Java o paquetes que falten.

Dependencias de compilación habituales:

| Distribución | Paquetes de compilación/red |
| --- | --- |
| Debian/Ubuntu | `build-essential`, `binutils`, `libpcap-dev`, `pkg-config` |
| Fedora | `gcc`, `glibc-devel`, `make`, `binutils`, `libpcap-devel`, `pkgconf-pkg-config` |
| Arch | `gcc`, `make`, `binutils`, `pkgconf`, `libpcap` |

El instalador también puede añadir `curl`, certificados CA, `tar`, `gzip` y las bibliotecas X11/fontconfig necesarias. Comprueba `libX11`, `libXext`, `libXi`, `libXrender`, `libXtst` y `libfontconfig` para la ventana Java.

Para los siguientes inicios, use la entrada del menú de aplicaciones o:

```bash
bash ./Start-Linux.sh
```

Modo API sin escritorio, incluida la primera instalación:

```bash
bash ./Deploy-Linux.sh --headless
# En los siguientes inicios:
bash ./Start-Linux.sh --headless
```

Ejecute estos lanzadores como su usuario normal, **sin anteponer `sudo`**. Solo el componente de captura recibe autorización administrativa; después de abrir los dispositivos de captura, reduce permanentemente sus privilegios al usuario que lo lanzó.

### macOS — solo ZIP portátil 3.0

1. Extraiga el paquete macOS completo a una carpeta escribible. No ejecute el instalador dentro del ZIP.
2. Abra `Deploy-macOS.command` con Terminal. Comprueba macOS, arquitectura nativa, espacio, Java y el componente de red.
3. Si faltan Apple Command Line Tools, se abrirá el **instalador oficial de Apple**. Pulse **Instalar**, acepte la licencia si está de acuerdo y espere: el lanzador comprueba su disponibilidad durante **hasta 30 minutos** y continúa cuando estén listas. Mantenga Terminal abierta. Si cancela o agota el tiempo, complete la instalación y vuelva a abrir `Deploy-macOS.command`. No requiere Homebrew ni Xcode completo. Consulte la [guía de Apple](https://developer.apple.com/documentation/xcode/installing-the-command-line-tools/).
4. Se reutiliza Java 17+ de la arquitectura nativa o se instala un JRE privado Temurin 21 con SHA-256 comprobada. En Apple Silicon se utiliza ARM64, incluso si Terminal se abrió bajo Rosetta.
5. Introduzca su contraseña de administrador cuando se solicite para instalar y autorizar el componente de red. El diálogo `sudo` de Terminal normalmente **no muestra caracteres** al escribir: introduzca la contraseña y pulse Return. Autoriza el componente protegido, no toda la aplicación Java como root.
6. Se crea `~/Applications/MultiRoute Resume.app` y se abre la aplicación.

Después, abra esa aplicación o `Start-macOS.command`. **Mantenga abierta la sesión de Terminal** durante su funcionamiento. Java se ejecuta como su usuario, y el componente abre el dispositivo Ethernet BPF antes de reducir sus privilegios. No ejecute la aplicación completa con `sudo`.

#### Confirmaciones de seguridad de macOS

El paquete **no está firmado con Developer ID ni notarizado**. Tras intentar abrir el lanzador o la aplicación, un aviso de desarrollador no identificado puede requerir **Ajustes del Sistema → Privacidad y seguridad → Abrir igualmente**, y después **Abrir**, si macOS ofrece esa opción. Confirme solo tras comprobar su procedencia. Consulte la [guía de Apple](https://support.apple.com/es-es/102445). En macOS anteriores se usa Preferencias del Sistema → Seguridad y privacidad. Un Mac administrado puede prohibir excepciones.

Si se solicita acceso a la **Red local** para Terminal, Java o la aplicación, permita el acceso para utilizar los routers LAN. La contraseña de administrador y el permiso de red local son confirmaciones distintas del aviso del desarrollador. Al elegir una carpeta puede aparecer un permiso de archivos: autorice el destino que necesita.

No desactive Gatekeeper/SIP ni quite atributos de cuarentena globalmente. Un aviso de malware o de daños al ordenador no es la confirmación normal de desarrollador desconocido: detenga la instalación e investigue la causa. El instalador no desactiva esas protecciones. El flujo de Finder/permisos del paquete portátil 3.0 sigue sin verificarse en un Mac del usuario. Las compilaciones nativas 4.0 y el inicio/API se comprobaron en runners Mac reales de GitHub; los diálogos del usuario y la captura BPF siguen sin comprobarse.

Modo API, desde la carpeta extraída:

```bash
./Start-macOS.command --headless
```

## 3. Configurar la red

La lista inicial contiene `192.168.1.1`, `192.168.1.2`, `192.168.1.3` y `192.168.1.4`, seleccionadas por defecto. La conexión habitual del ordenador **no se añade como una quinta puerta de enlace**.

1. Abra los ajustes de red y cambie esas direcciones si su LAN es diferente.
2. Configure la dirección virtual de la aplicación; el valor inicial es `192.168.1.70`.
3. Use la comprobación de dirección y la comprobación de las puertas de enlace antes de descargar. Si otro dispositivo usa la dirección virtual, elija otra. Reserve esa dirección fuera del rango DHCP automático para evitar que el router la asigne posteriormente.
4. Seleccione una, varias o todas las puertas de enlace que quiera utilizar.
5. Use **«Añadir puerta de enlace»** para ampliar la lista: admite entre **1 y 32** direcciones distintas. Se pueden añadir o quitar perfiles mientras hay descargas, siempre que los trabajos afectados conserven al menos una puerta de enlace activa. Para que un trabajo activo use la nueva puerta de enlace, inclúyala también en la selección de ese trabajo.

La dirección virtual pertenece al transporte interno de MultiRoute. **No se añade al adaptador del sistema operativo y no cambia sus rutas**, de modo que las demás aplicaciones conservan su conexión habitual. No escriba esa dirección en la configuración de red de Windows, Linux o macOS. El botón **?** junto a la dirección virtual y las ayudas al dejar el cursor sobre los controles explican esta función.

La prueba de velocidad usa hasta **16 MiB o 5 segundos de transferencia**; la preparación de la conexión puede añadir tiempo. Estos límites solo afectan a la prueba. Una descarga normal no tiene ese límite. Sin un enlace HTTP directo adecuado, se utiliza un archivo de prueba de Selectel.

## 4. Descargar, reanudar y gestionar

1. Elija **Español** en el selector de idioma. Los nuevos perfiles comienzan en inglés; también hay ruso, alemán, francés, chino, coreano, árabe, italiano, japonés, hindi y vietnamita. Se conserva el idioma guardado.
2. Pegue la URL en el campo largo mediante **Ctrl+V**; en macOS también funciona **Cmd+V**. Existe además el botón de pegar. Puede introducir magnet o abrir un archivo `.torrent`.
3. Elija la carpeta mediante el selector de su sistema. El destino inicial de nuevos perfiles es la carpeta `Downloads` de su usuario; confirme el destino antes de empezar.
4. Seleccione las puertas de enlace y pulse el botón de iniciar junto a la URL. Para una página de Hugging Face, WebDAV o Metalink, revise la lista y seleccione los archivos que desee.
5. Observe la velocidad total, velocidad y volumen de cada puerta de enlace, tiempo transcurrido, tiempo restante aproximado y gráfico opcional.

Los segmentos HTTP son de **20 MiB por defecto**, ajustables entre 1 y 64 MiB. Los routers toman segmentos de una cola común: el más rápido puede descargar más partes. Si un segmento deja de recibir datos durante **60 segundos**, su transferencia se cancela y la parte vuelve al conjunto disponible para otro router. Las piezas torrent utilizan el tamaño definido por sus metadatos, independientemente del tamaño HTTP configurado.

Puede pausar, continuar, cambiar las puertas de enlace de un trabajo activo, ajustar prioridad, límites o programación y cambiar la carpeta de un trabajo detenido. La cola permite dos trabajos activos por defecto. Las descargas que estaban ejecutándose se recuperan pausadas después de reiniciar; use continuar cuando corresponda.

La importación masiva admite listas de enlaces y archivos de texto. También hay búsqueda, filtros, menú contextual y selección de archivos torrent. La distribución voluntaria de un torrent completado está desactivada por defecto; si la activa, configure sus límites de tiempo y proporción.

Seleccione varias filas de descarga con **Ctrl+clic** en Windows/Linux o **Cmd+clic** en macOS; **Shift+clic** selecciona un intervalo. Con el foco en la tabla, **Ctrl+A** o **Cmd+A** selecciona todas las filas visibles según la búsqueda y el filtro de estado. La acción de eliminar se aplica a toda la selección con una sola confirmación. La opción de borrar también los archivos completos y las partes descargadas está **desmarcada por defecto**, por lo que puede quitar solo las entradas y conservar los datos. Márquela únicamente si desea borrar los archivos de esos trabajos; se conservan los demás archivos y la carpeta de destino. El resultado indica cuántas entradas se eliminaron. Los trabajos que no pudieron eliminarse permanecen en la lista con un resumen de errores para revisarlos y volver a intentarlo; las eliminaciones correctas no se repiten. Eliminar trabajos o archivos no borra el historial permanente de descargas.

## 5. Protocolos y comprobación

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

## 6. API para otras aplicaciones

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

## 7. Datos, actualización y desinstalación

### Instaladores nativos 4.1.3

| Sistema | Ajustes, historial y registros | Aplicación/Java | Componente de red |
| --- | --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App`, con Java incluido | Componente x64 incluido; Npcap instalado aparte |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app`, con Java | `/Library/PrivilegedHelperTools/MultiRouteResume/4.1.3/gateway-net` |

Cierre la aplicación antes de actualizar con un nuevo instalador. En Windows, elimínela desde **Configuración → Aplicaciones → Aplicaciones instaladas**. En macOS, eliminar la `.app` no borra automáticamente el historial ni el componente protegido separado. Los archivos descargados siguen en el destino elegido; no borre esa carpeta solo para desinstalar. La ruta de datos es distinta de Windows portátil 3.0; conserve su perfil anterior antes de migrar.

### Paquetes portátiles 3.0 y Linux

| Plataforma | Ajustes, historial y registros | Java privado si se instala | Componente protegido |
| --- | --- | --- | --- |
| Windows | `data/` dentro de la carpeta de lanzamiento, salvo selección mediante `data-location.config` o `--data-dir` | `runtime/` en la carpeta de aplicación | `native/windows-x64/gateway-net.exe` dentro del paquete; Npcap es un controlador instalado por separado |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/runtime` | `/usr/local/libexec/MultiRouteResume/3.0/gateway-net` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `~/Library/Application Support/MultiRoute Downloader/runtime` | `/Library/PrivilegedHelperTools/MultiRouteResume/3.0/gateway-net` |

Los archivos descargados se guardan en el destino elegido, que puede estar fuera de esas carpetas. En Linux el lanzador de menú está en `~/.local/share/applications/MultiRouteResume.desktop`; en macOS la aplicación está en `~/Applications/MultiRoute Resume.app`.

Para actualizar, cierre la aplicación y despliegue el nuevo paquete completo con el lanzador de su plataforma. En Linux, vuelva a ejecutar el despliegue si mueve la carpeta del programa. Conserve el historial y las partes si quiere reanudar; no elimine la carpeta de descargas para desinstalar. Quitar la ventana/lanzador no elimina automáticamente el JRE privado, datos, componentes protegidos ni dependencias instaladas del sistema.

## 8. Problemas frecuentes

| Síntoma | Qué comprobar |
| --- | --- |
| Falla la descarga de dependencias | Acceso HTTPS a los lanzamientos oficiales de Temurin en GitHub, `npcap.com` o repositorios de su distribución; espacio libre. Reintente o instale manualmente el componente oficial. No utilice un ejecutable sin verificar. |
| No encuentra Java, arquitectura incorrecta o no abre la ventana | Java 17+ de la arquitectura nativa; carpeta completa; `JAVA_EXE`/`JAVA_HOME`. En Linux compruebe escritorio/XWayland y bibliotecas; use `--headless` si necesita únicamente API. |
| Torrent informa `GLIBC_2.38 not found` o rechaza su biblioteca en macOS | Compruebe los mínimos específicos de JNI de la tabla: glibc 2.38+, macOS 15+ Intel o 26+ Apple Silicon. El mínimo del instalador Java no reduce esos requisitos. No copie ni sustituya bibliotecas del sistema manualmente. |
| Npcap no está disponible o acceso denegado | Instalación oficial del controlador x64 y permisos de captura; autorice el diálogo que requiera la instalación Admin-only. No copie DLL de procedencia desconocida. |
| No encuentra un adaptador Ethernet | Ethernet activo, dirección virtual y routers en la misma subred; Wi-Fi/VPN-only no sirven como sustitutos. Si dos adaptadores coinciden con esa LAN, resuelva la ambigüedad antes de usarla. |
| Dirección virtual ocupada | Elija una dirección libre y vuelva a comprobarla. No debe estar configurada en el propio adaptador de la OS. Evite el rango DHCP automático. |
| Router no disponible o velocidad cero | El router debe responder en la LAN y proporcionar salida a Internet al IP virtual. Pruebe cada puerta de enlace; compruebe URL, permisos del origen, identidad del archivo y mensaje del trabajo. |
| SFTP rechaza la conexión | Compare la huella SHA256 con un dato fiable del administrador del servidor y configure la misma huella; no desactive la comprobación. |
| macOS/Linux pierde autorización | macOS nativo: vuelva a abrir la aplicación y confirme el diálogo de administrador cuando se solicite. Portátil 3.0/Linux: renueve `sudo` en el lanzador de terminal y manténgala abierta. No ejecute toda Java como root. |
| Carpeta inaccesible o disco lleno | Permisos del destino y espacio para archivo, partes y temporales torrent. Pause antes de cambiar la carpeta de un trabajo. |
| Puerto 8765 ocupado | El inicio sustituye su instancia anterior. Si el puerto pertenece a otro programa, elija otro mediante `--port 8766`; no cierre procesos ajenos a MultiRoute. |
| Descarga antigua repetida | Seleccione el trabajo compatible existente y use continuar. Si desea eliminarlo, decida en el diálogo si conserva sus archivos; compruebe el error del trabajo antes de repetirlo. |

Para informar de un fallo, indique versión, plataforma/arquitectura, etapa y mensaje exacto. Oculte credenciales, cookies, URLs privadas y datos personales antes de compartir registros.

## 9. Alcance de la verificación

Las comprobaciones del lanzamiento incluyen Java, protocolos con datos sintéticos, lógica de reparto/reanudación, comprobación de SHA-256 y pruebas nativas sin captura de red en Windows y POSIX. Los cambios de instaladores se verifican con pruebas controladas; eso no demuestra la instalación real correcta de todos los paquetes externos. Los paquetes nativos 4.0 se compilaron y verificaron en runners de GitHub Apple Silicon/macOS 26 e Intel/macOS 15: inicio/API, torrent JNI, contenido y pruebas del componente de red. No se han comprobado sus routers físicos, la instalación del usuario, los diálogos Finder/seguridad ni la captura BPF.

macOS portátil 3.0 requiere compilar el componente en el Mac de destino. Los paquetes nativos 4.0 incluyen componentes precompilados distintos para Intel/Apple Silicon, sin firma Developer ID ni notarización. Las comprobaciones de empaquetado o CI no demuestran su funcionamiento en su Mac. Tras instalar, pruebe una descarga pequeña conocida por cada puerta de enlace y después por el conjunto antes de confiar en su velocidad combinada. Los resultados detallados figuran en los informes de comprobación del paquete. Esta publicación tiene carácter de **versión preliminar**.

## Bandeja del sistema y salida (4.0)

Cerrar la ventana principal mantiene la aplicación y las descargas en la bandeja del sistema. El icono muestra la velocidad total de descarga de **todos los trabajos**, sin depender de la selección ni del filtro. La información al pasar el ratón muestra el valor exacto en Mbit/s o Gbit/s (desde 1 Gbit/s), además de bytes/s, hasta 10 Gbit/s; el icono pequeño lo redondea. Elija **Abrir aplicación** en el menú de la bandeja para restaurar la ventana. En Windows/Linux también puede pulsar el icono. **Salir de la aplicación**, en la bandeja o ventana, guarda el estado y termina el programa. Utilice esta opción antes de actualizar.

Sin soporte de bandeja, la ventana se minimiza a la barra de tareas y las descargas continúan; Salir sigue disponible. Si desaparece la bandeja, vuelve a abrirse la ventana oculta. Windows puede situar el icono en el área de notificaciones ocultas.

El setup crea un acceso directo de escritorio y, en Windows, una entrada de Inicio. macOS crea un enlace Desktop a la `.app` instalada para el usuario conectado y vuelve a intentarlo en el primer inicio gráfico. Los elementos existentes se conservan. Si la privacidad impide escribir en Desktop, permita el acceso solicitado o abra desde Aplicaciones. Nuestros textos del setup son ingleses; los diálogos de seguridad y permisos del sistema siguen el idioma del SO.

## Licencia

Puede instalar y usar gratuitamente la aplicación original para uso personal o en organizaciones, incluidas actividades empresariales. **Se prohíbe vender la aplicación, cobrar por su licencia o distribuirla por dinero** sin permiso separado. La licencia propia no concede derechos generales de modificación o redistribución y no es una licencia de código abierto aprobada por OSI. Los componentes de terceros conservan sus licencias. Este texto es un resumen traducido; rige el texto inglés de [LICENSE.txt](../LICENSE.txt).

En 4.0 el icono de velocidad tiene fondo transparente y cifras y unidades negras, incluso a 10 Gbit/s. Se mantienen la velocidad total y los comandos de la bandeja.

## Verificación MD5, SHA-256 y SHA-512

Tras ensamblar el archivo, la aplicación calcula las tres sumas del archivo completo en una sola lectura. Pegue una referencia en el campo principal opcional con Ctrl+V o Cmd+V. El tipo se reconoce automáticamente: 32 caracteres hexadecimales corresponden a MD5, 64 a SHA-256 y 128 a SHA-512. La sección desplegable de sumas adicionales permite introducir varias referencias.

Las referencias publicadas se buscan en encabezados compatibles del resumen de toda la representación y en archivos auxiliares: `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512` y `SHA512SUMS`. El resumen de una respuesta HTTP parcial no se considera la suma del archivo completo. Se mantienen las comprobaciones específicas de Hugging Face LFS y Metalink.

Todas las referencias conocidas deben coincidir antes de publicar el archivo final. Una discrepancia marca la tarea como fallida y conserva los datos parciales. Sin referencia, el valor es una suma calculada localmente, no una coincidencia confirmada con el origen. MD5 detecta daños accidentales; SHA-256 o SHA-512 permiten una comprobación de integridad más sólida. MD5 no sustituye la identidad segura del archivo ni los requisitos de espejos de confianza.

La verificación de piezas torrent sigue siendo obligatoria y es independiente de las sumas de los archivos ensamblados. Una referencia manual del archivo completo exige seleccionar exactamente un archivo; para un grupo, proporcione una referencia propia a cada tarea.

La API local acepta `expected_md5`, `expected_sha256` y `expected_sha512` en `POST /api/v1/jobs`. Los resultados incluyen `md5`, `sha256`, `sha512` y los orígenes de referencia en `checksum_sources`. Las peticiones existentes con `expected_sha256` siguen siendo compatibles.

## Progreso de cada descarga (4.0)

Cada descarga tiene su propia barra en la columna Progreso. Las columnas ocupan todo el ancho del panel de descargas. La lista permite desplazarse verticalmente cuando hay muchas tareas; los paneles estrechos también permiten desplazarse horizontalmente sin comprimir las columnas hasta hacerlas ilegibles. Al actualizar se mantiene la primera tarea visible. La ordenación y los filtros mantienen cada barra vinculada a su descarga. Si se desconoce el tamaño total, se muestran los datos recibidos en lugar de un porcentaje; la ayuda emergente muestra tamaño, estado y errores. La fila seleccionada sigue mostrando progreso detallado, tiempos y velocidades de las puertas de enlace.

La carpeta de descarga predeterminada se determina al iniciar para el usuario actual: `%USERPROFILE%\Downloads` en Windows y `~/Downloads` en macOS/Linux. La carpeta predeterminada guardada sigue el perfil actual; se conserva cualquier carpeta elegida manualmente.

## Visor del registro (3.6)

La ventana del registro mantiene la posición de lectura al actualizar automática o manualmente. Al desplazarse al final, las nuevas entradas permanecen visibles. Desplazarse hacia arriba pausa el seguimiento, sin detener el registro. Se conserva la selección de texto para copiar. Al cambiar el idioma se mantiene el evento visible; al volver a abrir la ventana se muestran las entradas más recientes.

## Detalles de descarga e informe de sumas (3.7)

El panel inferior de detalles de la descarga seleccionada mantiene la posición de lectura y la selección de texto durante las actualizaciones automáticas repetidas. Puede leer el final del informe de sumas sin que el panel vuelva al principio. Al seleccionar otra descarga, su informe se abre al principio. La ventana independiente del registro conserva su comportamiento de desplazamiento.

## Instrucciones y guía de la API (3.8)

Los botones **Instrucciones** y **Guía de la API** están arriba a la derecha, en una fila independiente debajo del selector de idioma. Abren la guía en el idioma seleccionado en el navegador predeterminado. **Guía de la API** lleva directamente a la sección de la API local. Los enlaces corresponden a la versión de la aplicación; se necesita Internet para abrir las páginas de GitHub.

## Gestión de comandos al iniciar (3.9)

La versión 3.9 corrige un fallo de inicio cuando una consulta de procesos supera su tiempo de espera y deja bloqueado su archivo temporal de salida en Windows. La salida se lee ahora mediante canales con búfer limitado, sin archivos temporales de salida. Los comandos que agotan el tiempo de espera o se interrumpen se detienen y se espera su finalización. La sustitución de una instancia anterior de MultiRoute sigue exigiendo verificar que el proceso pertenece a la aplicación. Los protocolos de descarga y el contrato de la API local no cambian.

## Interfaz de escritorio e historial de descargas (4.0)

La interfaz utiliza FlatLaf con temas **Sistema**, **Claro** y **Oscuro**. La elección en la cabecera se guarda. La cola mantiene barras individuales, columnas que llenan el ancho, ordenación y filtros. El resumen muestra todos los trabajos, los activos, los completados y la velocidad agregada de todos, independientemente del filtro.

Pase el puntero sobre una fila activa o completada para ver el **nombre completo del archivo y su ruta de destino absoluta**, incluso después de ordenar o filtrar. La cabecera del archivo seleccionado ofrece la misma ayuda. **Copiar ruta completa** en el menú contextual copia el destino.

**Ctrl+L** en Windows/Linux o **Cmd+L** en macOS enfoca la URL; **Ctrl+F** o **Cmd+F** enfoca la búsqueda; **Escape** borra la búsqueda. Se conserva pegar con Ctrl+V/Cmd+V.

La descarga seleccionada tiene pestañas **Detalles** e **Historial**. El historial registra eventos con fecha y hora: primer inicio, cada reanudación y finalización correcta. No se inventan fechas para trabajos antiguos sin registros. Más abajo se describen el almacenamiento y los campos de la API.

Se conservan los protocolos, selección de puertas de enlace, IP virtual, pruebas de velocidad, importación por lotes, acceso a fuentes, MD5/SHA-256/SHA-512, bandeja, API local y desplazamiento del registro y de los detalles. El nuevo diseño no cambia las rutas del sistema.

### Almacenamiento del historial y API

La carpeta de datos contiene `logs/downloads-history.jsonl` (UTF-8, un objeto JSON por línea) y `logs/downloads-history.log` (entradas legibles con fecha). Se añaden eventos tras reinicios y se conservan al eliminar un trabajo o sus archivos. El registro de texto mantiene el idioma al escribir el evento; la interfaz muestra los eventos guardados en el idioma actual. La ventana lee solo los últimos 1.000 eventos / 2 MiB como máximo; el archivo completo queda en disco y crece con el uso. Archívelo aparte si lo necesita. Contiene nombres y rutas de destino completas, pero no URL de origen, contraseñas ni tokens. Revise las rutas antes de compartirlo.

Windows nativo: `%LOCALAPPDATA%\MultiRoute Downloader\data\logs\`. macOS nativo: `~/Library/Application Support/MultiRoute Downloader/data/logs/`. Linux y perfiles portátiles usan la carpeta de datos descrita arriba; `--data-dir` permite elegir otra.

`GET /api/v1/jobs` y la respuesta de cada trabajo incluyen `lifecycle_history`. Cada evento tiene `event_id`, `job_id`, `kind`, `time`, `filename` y `path`; los torrents pueden añadir `paths` y las pausas `reason`. Tipos: `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`. Las fechas usan ISO 8601 UTC, por ejemplo `2026-10-18T12:34:56.123Z`; el registro legible incluye fecha, hora local y desplazamiento UTC. Los campos opcionales `first_started_at`, `last_resumed_at`, `completed_at` resumen eventos observados. Estar en cola no cuenta como inicio. La finalización se registra tras verificar y publicar el archivo. `recovered` indica verificación actual de un archivo publicado antes, no una fecha histórica de finalización conocida. No se inventan fechas antiguas. Las operaciones existentes se conservan; no hace falta otro endpoint.

La ventana general **Registro** separa **Eventos de la aplicación** e **Historial de descargas**. El historial muestra eventos fechados de distintos trabajos, incluidos los eliminados; el historial de la descarga seleccionada corresponde solo a ese trabajo.

## Nuevo nombre y perfiles anteriores

MultiRoute Downloader se llamaba MultiRoute Resume. Los perfiles nuevos usan las rutas anteriores. Si no existe la carpeta nueva y hay un perfil anterior, se reutiliza sin mover ni borrar archivos. Los datos antiguos pueden seguir en `%LOCALAPPDATA%\MultiRoute Resume\data` o `~/Library/Application Support/MultiRoute Resume/data`. Se conservan el directorio protegido `MultiRouteResume`, el identificador de actualización Windows, la identidad interna de API y el puerto. Tras instalar, use el acceso o la app MultiRoute Downloader. Las versiones históricas mantienen sus nombres de archivo.

## Sitios guardados, subidas y vista previa de archivos (4.0)

Abra **Perfiles de sitios…**, cree/edite un perfil y selecciónelo en **Descargas** o **Subidas**. Los controles de puertas e IP virtual están en **Configuración de red**. En **Subidas**, escriba la **URL de la carpeta remota** y pulse **Elegir archivos y subir…** para elegir uno o varios archivos locales con el selector del sistema. La interfaz añade los nombres; API `target_url` identifica un archivo remoto completo. Para una vista previa, pegue la URL del archivo comprimido en el campo principal y pulse **Contenido del archivo…**; no inicia la descarga.

Guarde un perfil con nombre, URL base, carpeta local, carpeta remota y puertas de enlace. Admite acceso anónimo, HTTP Basic/Bearer/Cookie/cabeceras propias y credenciales FTP/SFTP con huella SHA-256 del servidor verificada previamente. Puede guardar varias cuentas del mismo servidor de forma aislada. Las carpetas/puertas explícitas prevalecen; las URL relativas se resuelven con la URL base. Los secretos se cifran en `site-profiles/vault.bin` con la clave local `site-profiles/key.bin` y permisos del propietario. No protege frente a quien controle su cuenta y clave. La API no devuelve secretos. Los campos vacíos conservan credenciales solo si origen y tipo no cambian; `replace_credentials:true` las sustituye. Una redirección HTTP a otro origen no recibe las credenciales. Eliminar el perfil conserva tareas y archivos locales/remotos; la tarea conserva su perfil original y no puede cambiar de cuenta; si se eliminó ese perfil, cree de nuevo la tarea con uno válido cuando sea necesario.

Las subidas y la exploración remota admiten **FTP, FTPS explícito y SFTP** mediante las puertas elegidas. Seleccione un archivo local normal y una URL completa de archivo remoto (`target_url`), no solo un directorio. Se usa un archivo temporal; reanudar exige comparar el prefijo existente. La finalización requiere releer todo el archivo remoto y comprobar SHA-256, con permisos de **lectura, escritura y cambio de nombre**. La lectura adicional consume ancho de banda. Una suma distinta o lectura denegada es un error. Cada archivo usa una puerta a la vez; varios archivos se distribuyen y suben en paralelo. Tras el límite de inactividad puede utilizarse otra puerta. No se divide una misma subida entre puertas. Pausa, reanudación y progreso persisten. Eliminar la tarea conserva el archivo local y los remotos finales/temporales.

La sobrescritura está desactivada inicialmente. FTP/SFTP no garantizan universalmente un cambio de nombre atómico solo si el destino no existe; otro cliente puede modificarlo durante la publicación. Coordine los destinos compartidos. Listar carpetas no sube ni elimina archivos.

La vista previa lista nombres, rutas, tamaños, carpetas y cifrado de **ZIP, TAR, 7Z, RAR4 y RAR5**, sin extraer ni ejecutar. Requiere una puerta, rangos de bytes y ETag o Last-Modified estable. Límites: 16 MiB de metadatos, 10.000 entradas, 60 segundos; cabeceras 7z con hasta 64 MiB de memoria. Puede ser parcial y no verifica el contenido. Elija la codificación original si los nombres fallan. Las cabeceras 7z cifradas compatibles admiten contraseña no persistida. Las cabeceras RAR cifradas se indican como no compatibles/contraseña necesaria; los volúmenes divididos pueden mostrar listas parciales.

La API acepta `site_profile_id` para descargas, subidas, directorios y vistas previas. `GET /api/v1/site-profiles` devuelve `{profiles:[...]}`; `POST` o `PUT` guarda un perfil plano y devuelve metadatos públicos; `DELETE /api/v1/site-profiles/{id}` elimina solo ese perfil. `POST /api/v1/uploads` recibe `local_path`, `target_url`, `routes`, `site_profile_id` opcional y `overwrite:false`; devuelve HTTP 201. `GET /api/v1/uploads` devuelve `{uploads:[...]}`. Hay GET/DELETE individuales y POST `/{id}/pause` / `/{id}/resume`. `POST /api/v1/remote/list` recibe `url` y `route`/`routes`; `POST /api/v1/archives/preview` añade `password` y `charset` opcionales. Consulte [openapi.yaml](../openapi.yaml). Los manifiestos UTF-8 de `data/uploads/` guardan ID de perfil, no contraseñas. La vista previa no crea una descarga.

## Programación y prioridad de descargas

Configure `max_active_jobs` (1–32), límites globales/por puerta y ventana diaria (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm) en los controles de cola/programación. Se usa la zona horaria local, expuesta como `schedule_timezone`. Se permite cruzar medianoche; inicio igual a fin significa todo el día. Fuera de ventana las descargas vuelven a la cola; las pausadas manualmente siguen pausadas. `priority` de -100 a 100 ordena las pendientes de mayor a menor y luego por creación; no interrumpe tareas activas. `start_after_ms` es el inicio mínimo opcional en milisegundos Unix; 0 elimina la demora. Persiste entre reinicios y afecta a descargas, no a la cola de subidas independiente. Mantenga la aplicación y el ordenador despiertos; no programa el despertar del sistema. API: `GET`/`PUT`/`POST /api/v1/queue` y `PUT`/`POST /api/v1/jobs/{id}/options`.

## Integración con Chrome y Edge

Abra **Chrome / Edge** en la aplicación, permita enviar enlaces y abra la carpeta de la extensión. Una vez: visite `chrome://extensions` o `edge://extensions`, active modo desarrollador, pulse **Cargar descomprimida** y elija `browser-extension` con `manifest.json`. Los instaladores nativos la incluyen junto al JAR; también está en [browser-extension/](../browser-extension/). Mantenga la carpeta en su ubicación. Extraiga primero `MultiRoute-Downloader-4.1.3-Chrome-Edge.zip` si lo usa. No está publicada en una tienda de extensiones.

MultiRoute debe estar abierto, incluso en la bandeja. Envíe un enlace desde su menú contextual o la ventana de la extensión. Se usan carpeta, puertas y perfil actuales; la selección de repositorio/torrent sigue disponible. El puerto es 8765; ajústelo si cambió el de API. Solo se envía URL, sin cookies ni contraseñas; no intercepta otras descargas del navegador. Configure el acceso al origen en la aplicación. Si el navegador pide permiso de conexión local, concédalo solo a esta extensión elegida.

El puente está desactivado inicialmente y acepta solo `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` en `/api/v1/browser/status` y `/api/v1/browser/download`; las otras reglas API no cambian. HTTP 202 confirma entrega a la interfaz, no descarga completada. Solo un servidor headless no puede abrir la selección de escritorio. Para desactivar, quite la extensión o el permiso en la aplicación.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
