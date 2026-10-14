# MultiRoute Resume 3.2 installers

The actual installer files are in this folder. Open the file page and click **Download raw file**, or download them from [Release 3.2](https://github.com/2018-spec-fdsds/multiroute-resume/releases/tag/v3.2).

| Computer | Installer |
| --- | --- |
| Windows 11 x64 | [Windows Setup EXE](MultiRoute-Resume-3.2-Windows-x64-Setup.exe) |
| Apple Silicon, macOS 26+ | [macOS ARM64 PKG](MultiRoute-Resume-3.2-macOS-arm64.pkg) |
| Intel, macOS 15+ | [macOS Intel PKG](MultiRoute-Resume-3.2-macOS-x86_64.pkg) |

Java and the compiled gateway component are included. Setup creates a desktop shortcut by default; on macOS an existing Desktop item is preserved and denied Desktop access does not block installation. Our setup text is English. System and third-party security dialogs follow their own language.

**New in 3.2:** closing the window keeps downloads running in the system tray. Its icon and tooltip show the aggregate speed of all active downloads, switching from Mbit/s to Gbit/s at 1 Gbit/s, including 10 Gbit/s. Use **Open application** to restore the window or **Exit application** to stop it. If the system has no tray, the window minimizes to the taskbar and has an explicit Exit button.

**Before updating, choose Exit application in the tray.**

**Русский:** установщики находятся прямо в этой папке. Откройте нужный файл и нажмите **Download raw file**. В версии 3.2 закрытие окна сворачивает приложение в трей, где отображается суммарная скорость до 10 Гбит/с с переключением единиц. Для полного закрытия выберите **Выйти из приложения** в меню трея. Перед обновлением завершите приложение через это меню. Java включена, ярлык на рабочем столе создаётся по умолчанию, текст наших окон установки — на английском.

Instructions and security confirmations:

[English](../../docs/en.md) · [Русский](../../docs/ru.md) · [Deutsch](../../docs/de.md) · [Español](../../docs/es.md) · [Français](../../docs/fr.md).

[SHA-256 checksums](SHA256SUMS.txt) · [Verification report](NATIVE-INSTALLER-TESTS.json).

These files are identical to the release assets. Version 3.2 is a prerelease. End-user installation/security dialogs, actual macOS tray interaction and physical gateway transfers remain unverified. Packages are not publisher-signed, and macOS packages are not notarized. Speed display tests used synthetic rates.

Project owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds). Free use; sale of the original application or its licence is prohibited without separate permission. See [LICENSE.txt](../../LICENSE.txt).
