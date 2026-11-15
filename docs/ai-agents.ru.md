# MultiRoute для ИИ-агентов — настройте один раз, скачивайте по умолчанию

[English](ai-agents.en.md) · [Приложение](../README.ru.md) · [Описание API](../API.ru.txt) · [OpenAPI](../openapi.yaml)

Добавьте одно правило проекта и один раз подключите инструмент. После этого Claude Code, Codex и другие агенты с инструментами смогут передавать MultiRoute загрузку моделей, датасетов, архивов и ресурсов проекта, сохранять ID заданий, следить за прогрессом и проверять завершение. Достаточно попросить: **«Скачай нужную проекту модель в папку ресурсов».** Выбор загрузчика и адрес API не придётся повторять для каждого файла.

Модель может работать в облаке или локально. Важно, **где исполняется её инструмент**: он должен иметь доступ к компьютеру с MultiRoute. Обычный веб-чат или удалённая песочница не получают доступ к вашему ПК только от чтения инструкции.

## 1. Один раз выберите настройки проекта

1. Установите и запустите MultiRoute на компьютере, где будут храниться файлы. Используйте порт API этого экземпляра; в примерах — `8765`.
2. Укажите абсолютную папку ресурсов: например, `C:/Projects/MyProject/assets` в Windows или `/Users/YOUR_USER/Projects/MyProject/assets` в macOS. Подключённая сетевая папка Mac имеет путь `/Volumes/Share/...`.
3. Явно выберите маршруты REST. Обычное подключение ОС: `[{"type":"direct","enabled":true}]`. Для настроенных независимых шлюзов MultiRoute: `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]`; при необходимости добавьте другие согласованные шлюзы. Сначала настройте и проверьте их в приложении. Галочки интерфейса не копируются в запрос автоматически. Маршрут direct использует обычную IP-маршрутизацию ОС, в том числе подходящие маршруты системного/TUN VPN. Настройки HTTP-прокси/PAC и VPN-расширения браузера он не наследует.
4. Создание загрузок внешним агентом требует права **`automation_api`**. Пакетное создание дополнительно требует `batch_downloads`; остальные ограничения сохраняются. Проверка: `GET /license/policy`, поле `features.automation_api`.
5. API оставьте на localhost. Общего токена для удалённой авторизации у него нет; не открывайте порт в локальную сеть или Интернет. Данные авторизации источников храните в профилях сайтов MultiRoute; при необходимости передавайте `site_profile_id`.

## 2. Добавьте постоянное правило агенту

Скопируйте **[готовый блок инструкции](examples/ai-agent-instructions.md)** в существующие правила проекта. Один раз замените папку и маршруты, сохранив остальные правила. Английский блок общий для разных клиентов и моделей; пояснения ниже — на русском.

| Агент / среда | Куда добавить правило | Как подключить |
| --- | --- | --- |
| Claude Code на вашем ПК | В `CLAUDE.md` проекта. Если правило уже хранится в `AGENTS.md`, добавьте `@AGENTS.md` в существующий `CLAUDE.md`, чтобы не поддерживать две копии. | Локальный терминал с REST или stdio MCP ниже. |
| Codex на вашем ПК | В `AGENTS.md` проекта. | Локальный терминал с REST или настроенный MCP. |
| Другой локальный агент / оболочка локальной модели | В постоянные инструкции проекта или системы этой оболочки. | Подключите инструмент REST или клиент stdio MCP. Сама загрузка модели не добавляет ей инструменты. |
| Облачная модель с локальным исполнителем агента | В правила локального агента. | Инструмент вызывается на том же ПК, что и MultiRoute. Сама модель может работать в облаке. |
| ChatGPT в браузере, Claude в браузере, удалённый облачный исполнитель | В инструкции проекта вместе с доступным подключённым инструментом. | Нужен отдельно настроенный мост к ПК. В поставке MultiRoute есть локальный stdio-адаптер, а не публичный удалённый MCP-сервис. |

Правило задаёт предпочтение; обычные разрешения инструментов и ограничения среды сохраняются. Официальные инструкции: [Claude](https://code.claude.com/docs/en/memory), [Codex](https://developers.openai.com/codex/guides/agents-md/), [требования подключения MCP к ChatGPT](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

## 3. Пример REST для агента с локальным терминалом

Это самый простой способ, если агент уже умеет выполнять команды на вашем ПК. **MultiRoute должен быть запущен.** Ссылка источника в примере иллюстративная: замените её и папку своими значениями. Создание задания ставит настоящую загрузку в очередь.

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'MultiRoute automation_api is required' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # Add expected_sha256 only when you have the publisher's trusted 64-character hex hash.
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# Keep $job.id. Query it again while it is queued/running/retrying/verifying:
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / локальный терминал

Сохраните следующий JSON как `download.json` в UTF-8. Замените `YOUR_USER` и ссылку источника; при наличии доверенной суммы добавьте поле `expected_sha256` в этот же объект.

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
# After confirming features.automation_api is true:
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# Replace the example id with the id from the response:
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` означает создание задания, а не готовый файл. Продолжайте получать статус до `complete` либо сообщите о паузе/ошибке. При завершении проверяйте `checksum_checking`, `integrity_status` и суммы. Для повтора того же запроса используйте прежний ключ идемпотентности; для намеренно новой загрузки — новый. Статусы опрашивайте кратко и с разумным интервалом: большой файл может скачиваться долго. Установку зависимостей пакетными менеджерами и операции Git оставьте их штатным инструментам.

## 4. Дополнительно: MCP для совместимого локального клиента

В MultiRoute есть **stdio MCP-адаптер**: отдельный дочерний процесс Java передаёт запросы уже работающему приложению. Встроенного HTTP-адреса `/mcp` нет. Текущий инструмент `download_add` требует явного списка **IPv4-адресов шлюзов** и не принимает REST-маршрут `direct`. Для обычного пути ОС используйте REST-пример выше.

Точные пути установленной Java и JAR можно посмотреть в **Настройки → MCP и CLI…**. При стандартной установке Windows это `runtime/bin/java.exe` и `app/MultiRoute-Downloader.jar` внутри `%LOCALAPPDATA%/MultiRoute Downloader App`. В macOS — `Contents/runtime/Contents/Home/bin/java` и `Contents/app/MultiRoute-Downloader.jar` внутри `/Applications/MultiRoute Downloader.app`. Пути с пробелами заключайте в кавычки. В нативных пакетах Java уже включена.

Пример записи для stdio-клиента; замените оба абсолютных пути. В JSON переменные `%LOCALAPPDATA%`, `$HOME` и переменные оболочки автоматически **не раскрываются**:

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

Для Claude Code объедините эту запись с существующим `.mcp.json` по [официальной инструкции](https://code.claude.com/docs/en/mcp). В Codex задайте те же `command` и `args` в разделе TOML `[mcp_servers.multiroute]` либо через `codex mcp add`; см. [настройку MCP](https://developers.openai.com/codex/mcp/). У других оболочек свой формат настройки. Этот пример сам ничего не устанавливает и не подключает.

После обнаружения сервера попросите клиента показать инструменты MultiRoute. Пример аргументов `download_add`:

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

Затем `download_status` получает `{"id":"012345abcdef"}`. Доступны `download_add`, `download_batch`, `download_list`, `download_status`, `download_pause`, `download_resume`, `download_verify`. Все операции адаптера требуют `automation_api`; `download_verify` повторно проверяет уже завершённый файл. Краткие MCP-ответы не включают ссылку источника, учётные данные и папку назначения: папку сохраняйте из своего запроса. `gateways: []` не включает автоматический выбор.

## 5. Облачные чаты и удалённые песочницы

**Одна инструкция не подключает ChatGPT или Claude в браузере к вашему приложению.** `127.0.0.1` у облачного инструмента указывает на его собственную среду. Для удалённого MCP-подключения ChatGPT нужен совместимый сервис или поддерживаемый защищённый мост; REST-адрес MultiRoute не является таким MCP-сервисом. В этом релизе удалённый мост не поставляется. Облачная модель в локальном Claude Code, Codex или другом локальном исполнителе может использовать способы выше. [Официальные требования OpenAI](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

Файл сохраняется на компьютере MultiRoute. Для сборки на удалённом сервере понадобится отдельно согласованная передача файла или общее хранилище. Данные авторизации источников задавайте через профили сайтов, не через постоянную инструкцию и не в чате.

## Какой результат должен сообщить агент

«Скачал `model.gguf` через MultiRoute в заданную папку ресурсов. Задание: `…`. Статус: complete. SHA-256: …; совпала с эталоном издателя». Если доверенного эталона нет: «Локальная SHA-256 рассчитана; сверка с издателем недоступна». Если приложение недоступно или отвечает `pro_required`, сообщите причину и согласуйте другой способ вместо незаметной смены загрузчика.
