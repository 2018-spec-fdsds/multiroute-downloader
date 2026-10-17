# MultiRoute Resume 3.9 installers

Open a file page and click **Download raw file**, or use [Release 3.9](https://github.com/2018-spec-fdsds/multiroute-resume/releases/tag/v3.9). These are the actual installer binaries, identical to the release assets.

| Computer | Installer |
| --- | --- |
| Windows 11 x64 | [Windows EXE](MultiRoute-Resume-3.9-Windows-x64-Setup.exe) |
| Apple Silicon, macOS 26+ | [macOS ARM64 PKG](MultiRoute-Resume-3.9-macOS-arm64.pkg) |
| Intel, macOS 15+ | [macOS Intel PKG](MultiRoute-Resume-3.9-macOS-x86_64.pkg) |

**New in 3.9:** startup captures process-lookup command output through bounded pipes instead of temporary files, so a locked temporary output file after a timeout cannot abort launch. Interrupted and timed-out lookup commands are stopped and awaited. Application identity checks for replacing an earlier MultiRoute instance remain in place.

**Retained from 3.8:** Instructions and API guide buttons open the matching release guide in the selected language. The API button goes directly to its API section.

**Retained from 3.7:** the lower details and checksum report keeps its reading position and text selection during refresh. Selecting a different download opens its own report at the beginning. The separate Log dialog fix from 3.6 is retained.

**Retained:** a progress bar for every download, columns filling the panel width, scrolling for long lists with a stable position on refresh, and a default download folder that follows the current user profile. A manually selected folder is retained.

MD5, SHA-256 and SHA-512 whole-file verification is retained. The main optional checksum field recognizes 32/64/128 hexadecimal characters automatically. Additional references are available in a collapsed section; all known references must match before publishing the completed file. The UI, tooltips and new diagnostics are available in five languages.

Java is included. Setup creates a desktop shortcut and our setup text is English. Closing the window keeps downloads running in the tray, with a transparent speed icon and black digits. **Before updating, use Exit application in the tray.**

**Русский:** при запуске вывод команд поиска процесса читается через каналы с ограниченным буфером вместо временных файлов; заблокированный файл вывода после превышения времени ожидания больше не прерывает запуск. При прерывании или таймауте команда останавливается с ожиданием завершения. Проверка принадлежности прежнего процесса MultiRoute сохранена. Сохранены кнопки «Инструкция» и «Работа с API» версии 3.8. Сохранено исправление 3.7: нижняя панель подробностей загрузки и отчёта контрольных сумм сохраняет место чтения и выделение при обновлениях. Выбор другой загрузки открывает её отчёт с начала. Исправление 3.6 относилось к отдельному окну журнала и сохранено. Сохранены отдельная полоса каждой загрузки, колонки на всю ширину, прокрутка с сохранением положения и папка загрузок текущего пользователя по умолчанию. Ручной выбор папки сохраняется. Версия 3.9 проверяет MD5, SHA-256 и SHA-512. Основное поле само распознаёт тип по 32/64/128 hex-символам; дополнительные эталоны находятся в раскрываемом разделе. До публикации файла должны совпасть все известные эталоны. Java включена. Перед обновлением завершите приложение через меню трея.

Instructions: [English](https://github.com/2018-spec-fdsds/multiroute-resume/blob/v3.9/docs/en.md) · [Русский](https://github.com/2018-spec-fdsds/multiroute-resume/blob/v3.9/docs/ru.md) · [Deutsch](https://github.com/2018-spec-fdsds/multiroute-resume/blob/v3.9/docs/de.md) · [Español](https://github.com/2018-spec-fdsds/multiroute-resume/blob/v3.9/docs/es.md) · [Français](https://github.com/2018-spec-fdsds/multiroute-resume/blob/v3.9/docs/fr.md).

Installer sums: [MD5](MD5SUMS.txt) · [SHA-256](SHA256SUMS.txt) · [SHA-512](SHA512SUMS.txt). [Verification report](NATIVE-INSTALLER-TESTS.json).

This is a prerelease. Packages are not publisher-signed and macOS packages are not notarized. End-user installation/security dialogs, actual macOS tray interaction and physical gateway transfers remain unverified. System/third-party security dialogs use their own language.

Project owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds). Free use; selling the original application or its licence requires separate permission under [LICENSE.txt](../../LICENSE.txt). Third-party licences remain applicable.
