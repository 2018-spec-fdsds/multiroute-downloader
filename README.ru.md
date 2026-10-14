# MultiRoute Resume 3.0

[Полная инструкция на русском](docs/ru.md)

[English](docs/en.md) · [Deutsch](docs/de.md) · [Español](docs/es.md) · [Français](docs/fr.md)

Настольное Java-приложение для распределённой докачки через выбранные шлюзы
локальной сети, с проверкой файлов и локальным API.

Готовые дистрибутивы: [GitHub Releases](https://github.com/2018-spec-fdsds/multiroute-resume/releases).
Первый запуск: `Deploy-Windows.cmd`, `Deploy-Linux.sh` или `Deploy-macOS.command`.
Исходники в этом репозитории требуют сборки; готовый JAR и Windows EXE входят
в дистрибутивы Releases.

Минимальная совместимость поставляемого BitTorrent JNI: Linux glibc 2.38+;
macOS 26+ для Apple Silicon, 15+ для Intel. Установщик macOS допускает 11+,
но это не минимальная ОС для всех протоколов. Нужны Java 17+ и Ethernet
в общей IPv4-подсети выбранных роутеров.

Подробности: [инструкция](docs/ru.md), [API](API.ru.txt),
[проверки](CHECKS.ru.md), [аудит совместимости](docs/COMPATIBILITY.json).
