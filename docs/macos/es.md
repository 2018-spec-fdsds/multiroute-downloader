# macOS: instalación, inicio y permisos

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · **Español** · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

## Instalar y abrir

Antes de actualizar, termina las transferencias y usa **Quit/Exit** en el menú de MultiRoute de la barra superior. El botón rojo puede ocultar la ventana sin cerrar la aplicación. Descarga el PKG para Apple Silicon o Intel desde [Descargas](../../downloads/README.md), comprueba SHA-256 y ábrelo. Confirma la instalación en el diálogo de administrador de macOS. Inicia `/Applications/MultiRoute Downloader.app` desde Finder o Spotlight; no hace falta instalar Java ni usar Terminal.

## Confirmar la apertura

Si macOS no puede verificar al desarrollador, intenta abrir primero el PKG o la aplicación. Solo para un archivo verificado de origen fiable: **Ajustes del Sistema → Privacidad y seguridad → Open Anyway → Open**; los nombres varían. Si avisa de malware o archivo dañado, detente y verifica el archivo. [Instrucciones de Apple](https://support.apple.com/en-us/102445).

## Red y contraseña

El primer inicio de la aplicación o aplicar IP virtual, DHCP o MAC puede pedir autorización de administrador para Ethernet BPF. Reiniciar el componente o cambiar su configuración puede volver a pedirla. Las comprobaciones y pruebas de velocidad normales reutilizan el componente preparado: no se necesita contraseña por archivo o puerta. No cambia las rutas del sistema ni la MAC física.

Si aparece un aviso de **Red local**, permite la entrada reconocida de MultiRoute en Privacidad y seguridad → Red local. No aparece en todas las instalaciones. [Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy).

La contraseña del administrador va solo en el diálogo del sistema, nunca en campos de licencia o Hugging Face. El solicitante puede figurar como `DesktopLauncher`/Java: confirma únicamente tu acción esperada de MultiRoute. Tras cancelar, reintenta explícitamente y lee el registro. No desactives Gatekeeper/SIP globalmente.

[MultiRoute Downloader — Español](../es.md) · [macOS — English / 12 languages](README.md)
