# MultiRoute لوكلاء الذكاء الاصطناعي — إعداد مرة واحدة وتنزيل افتراضيًا

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <a href="ai-agents.de.md">Deutsch</a> · <a href="ai-agents.es.md">Español</a> · <a href="ai-agents.fr.md">Français</a> · <a href="ai-agents.zh.md">中文</a><br />
<a href="ai-agents.ko.md">한국어</a> · <strong>العربية</strong> · <a href="ai-agents.it.md">Italiano</a> · <a href="ai-agents.ja.md">日本語</a> · <a href="ai-agents.hi.md">हिन्दी</a> · <a href="ai-agents.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

[نظرة عامة على المنتج](overview/ar.md) · [مرجع API](../API.ru.txt) · [OpenAPI](../openapi.yaml)

أضف قاعدة واحدة للمشروع واربط أداة مرة واحدة. بعد ذلك يمكن لـ Claude Code وCodex وغيرهما من الوكلاء المزودين بأدوات إسناد تنزيل أوزان نماذج المشروع ومجموعات البيانات والأرشيفات والملفات الخارجية الأخرى إلى MultiRoute، والاحتفاظ بمعرّفات المهام، ومتابعة التقدم والتحقق من الاكتمال. يمكنك أن تطلب: **«نزّل النموذج الذي يحتاجه هذا المشروع إلى مجلد موارده.»** تحدد القاعدة برنامج التنزيل المفضل؛ ولا تحتاج إلى تكرار تفاصيل API مع كل طلب.

يمكن أن يعمل النموذج في السحابة أو محليًا. المهم هو **مكان تنفيذ أداته**: يجب أن تتمكن من الوصول إلى الكمبيوتر الذي يشغّل MultiRoute. لا تمنح قراءة ملف تعليمات محادثة المتصفح أو بيئة العزل البعيدة وصولًا إلى حاسوبك.

## 1. اختر الإعدادات الافتراضية للمشروع مرة واحدة

1. ثبّت MultiRoute وشغّله على الكمبيوتر الذي سيخزن الملفات. استخدم منفذ API المضبوط لتلك النسخة قيد التشغيل؛ تستخدم الأمثلة `8765`.
2. اختر مسارًا مطلقًا لمجلد موارد المشروع، مثل `C:/Projects/MyProject/assets` على Windows أو `/Users/YOUR_USER/Projects/MyProject/assets` على macOS. تستخدم مشاركة الشبكة المركّبة على Mac المسار `/Volumes/Share/...`.
3. اختر مسارات REST صراحةً. يستخدم اتصال نظام التشغيل العادي `[{"type":"direct","enabled":true}]`. لاستخدام بوابات MultiRoute المستقلة المضبوطة، استخدم `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]` وأضف بوابات أخرى معتمدة عند الحاجة. اضبط هذه البوابات وتحقق منها في التطبيق أولًا. لا تُنسخ مربعات اختيار الواجهة تلقائيًا إلى الطلبات. يستخدم المسار direct توجيه IP العادي لنظام التشغيل، بما في ذلك مسارات VPN النظامية/TUN المنطبقة. ولا يرث إعدادات وكيل HTTP/PAC أو إضافات VPN الخاصة بالمتصفح فقط.
4. يتطلب إنشاء التنزيلات خارجيًا إمكانية **`automation_api`**. ويتطلب الإنشاء الدفعي أيضًا `batch_downloads`؛ وتظل الحدود الأخرى سارية. افحص `GET /license/policy` بما في ذلك `features.automation_api`.
5. أبقِ API على عنوان الاسترجاع المحلي loopback. لا توجد له آلية رمز مصادقة عام للوصول البعيد؛ فلا تنشر منفذه على الشبكة المحلية أو الإنترنت. خزّن بيانات اعتماد المصدر في ملف تعريف موقع داخل MultiRoute ومرّر `site_profile_id` الخاص به عند الحاجة.

## 2. أضف قاعدة المشروع القابلة لإعادة الاستخدام

انسخ **[كتلة التعليمات الجاهزة للاستخدام](examples/ai-agent-instructions.ar.md)** إلى قواعد المشروع الحالية. استبدل الوجهة والمسارات مرة واحدة، مع الاحتفاظ بتعليمات المشروع الأخرى.

| الوكيل / البيئة | موضع القاعدة | الاتصال المطلوب |
| --- | --- | --- |
| Claude Code على حاسوبك | ملف `CLAUDE.md` في المشروع. إذا كانت القاعدة موجودة بالفعل في `AGENTS.md`، فأضف `@AGENTS.md` إلى `CLAUDE.md` الحالي بدلًا من صيانة نسختين. | وصول محلي إلى shell/REST أو خادم MCP عبر stdio أدناه. |
| Codex على حاسوبك | ملف `AGENTS.md` في المشروع. | وصول محلي إلى shell/REST أو خادم MCP المضبوط له. |
| وكيل محلي آخر أو مضيف نموذج محلي | تعليماته الدائمة للمشروع/النظام. | فعّل أداة قادرة على استدعاء REST أو عميل MCP عبر stdio. تحميل النموذج وحده لا يضيف أدوات. |
| نموذج سحابي مع منفّذ وكيل محلي | قواعد المشروع الخاصة بالوكيل المحلي. | تستدعي أداته المحلية MultiRoute على الحاسوب نفسه. لا يلزم تشغيل النموذج نفسه محليًا. |
| ChatGPT web أو Claude web أو منفّذ سحابي بعيد | تعليمات المشروع مع أداة متصلة ومدعومة. | يتطلب جسرًا مضبوطًا بشكل منفصل إلى كمبيوتر MultiRoute. المحوّل المحلي المرفق هنا يعمل عبر stdio، وليس خدمة MCP بعيدة عامة. |

توجّه ملفات التعليمات السلوك؛ ولا تتجاوز موافقات أدوات العميل أو بيئة العزل. تظل قيود العميل/الحساب الحالية سارية. راجع الوثائق الرسمية: [تعليمات مشروع Claude](https://code.claude.com/docs/en/memory)، و[تعليمات مشروع Codex](https://developers.openai.com/codex/guides/agents-md/)، و[متطلبات اتصال MCP المخصص في ChatGPT](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

## 3. مثال REST — وكيل محلي لديه وصول إلى shell

هذا أبسط مسار عندما يكون لدى الوكيل طرفية محلية بالفعل. **يجب أن يكون MultiRoute قيد التشغيل مسبقًا.** تستخدم هذه الأمثلة عنوان URL توضيحيًا للمصدر؛ استبدله واستبدل مجلد الإخراج. يؤدي إنشاء مهمة إلى وضع تنزيل فعلي في قائمة الانتظار.

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'إمكانية automation_api في MultiRoute مطلوبة' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # أضف expected_sha256 فقط عند توفر بصمة سداسية عشرية موثوقة من الناشر بطول 64 حرفًا.
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# احتفظ بـ $job.id. استعلم عنه مجددًا ما دامت حالته queued/running/retrying/verifying:
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / shell محلي

احفظ JSON التالي باسم `download.json` بترميز UTF-8. استبدل `YOUR_USER` وعنوان URL للمصدر؛ وأضف `expected_sha256` إلى الكائن نفسه عند توفر بصمة موثوقة.

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
# بعد التأكد من أن features.automation_api تساوي true:
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# استبدل id التوضيحي بقيمة id من الاستجابة:
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

يعني HTTP `201` أن المهمة أُنشئت، وليس أن الملف جاهز. واصل قراءة حالتها حتى `complete`، أو أبلغ عن حالة الإيقاف المؤقت/الفشل. عند الاكتمال افحص `checksum_checking` و`integrity_status` والبصمات المتاحة. استخدم مفتاح idempotency نفسه لإعادة محاولة مطابقة؛ واستخدم مفتاحًا جديدًا لطلب جديد مقصود. اجعل الاستطلاع مقتضبًا وعلى فترات معقولة؛ فقد تبقى الملفات الكبيرة نشطة وقتًا طويلًا. يحتفظ مديرو الحزم وGit بسير عملهم الأصلي للتبعيات/الاستنساخ.

## 4. اختياري: أدوات MCP لعميل محلي متوافق

يتضمن MultiRoute **محوّل MCP عبر stdio** يُشغّل كعملية Java فرعية. وهو يمرر العمليات إلى التطبيق قيد التشغيل. لا توجد نقطة نهاية HTTP `/mcp` مدمجة. تتطلب `download_add` الحالية **عناوين IPv4 صريحة للبوابات**؛ ولا تقبل مسار REST من نوع `direct`. استخدم REST أعلاه لمسار يعتمد على نظام التشغيل فقط.

اعثر على مسارات Java/JAR المثبتة الدقيقة في **الإعدادات → MCP وCLI…**. يحتوي تثبيت Windows القياسي على `runtime/bin/java.exe` و`app/MultiRoute-Downloader.jar` ضمن `%LOCALAPPDATA%/MultiRoute Downloader App`. وعلى macOS هما `Contents/runtime/Contents/Home/bin/java` و`Contents/app/MultiRoute-Downloader.jar` داخل `/Applications/MultiRoute Downloader.app`. ضع المسارات المحتوية على مسافات بين علامتي اقتباس. لا تتطلب حزم النظام الأصلية هذه تثبيت Java منفصلًا.

مثال لإدخال عميل stdio؛ استبدل كلا المسارين المطلقين. قيم JSON **لا** توسّع `%LOCALAPPDATA%` أو `$HOME` أو متغيرات shell تلقائيًا:

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

بالنسبة إلى Claude Code، ادمج هذا الإدخال في `.mcp.json` وفق [إعداد المشروع الموثّق](https://code.claude.com/docs/en/mcp). وبالنسبة إلى Codex، اضبط الأمر نفسه وقائمة الوسائط نفسها تحت `[mcp_servers.multiroute]` في إعدادات TOML، أو استخدم `codex mcp add`؛ وراجع [إعداد MCP الرسمي](https://developers.openai.com/codex/mcp/). تستخدم المضيفات الأخرى إعدادات MCP الخاصة بها. هذا مثال إعداد، وليس اتصالًا مثبتًا.

بعد اكتشاف الأدوات في العميل، اطلب منه سرد أدوات MultiRoute. يمكن عندئذ أن تكون وسائط `download_add` كما يلي:

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

يأخذ استدعاء `download_status` اللاحق `{"id":"012345abcdef"}`. الأدوات المتاحة هي `download_add` و`download_batch` و`download_list` و`download_status` و`download_pause` و`download_resume` و`download_verify`. تتطلب جميع عمليات المحوّل `automation_api`؛ وتعيد `download_verify` فحص ملف مكتمل. تحذف نتائج MCP المقتضبة عناوين URL للمصدر وبيانات الاعتماد ومجلد الوجهة، لذا احتفظ بالمجلد الوارد في الطلب. لا يعني `gateways: []` توجيهًا تلقائيًا.

## 5. المحادثات السحابية وبيئات العزل البعيدة

**لا تستطيع تعليمات المشروع وحدها ربط ChatGPT أو Claude web بتطبيقك المحلي.** يشير `127.0.0.1` في الأداة السحابية إلى بيئة تلك الأداة. يتطلب اتصال MCP البعيد في ChatGPT خدمة بعيدة متوافقة أو جسرًا آمنًا مدعومًا؛ وعنوان REST الخاص بـ MultiRoute ليس نقطة نهاية MCP من هذا النوع. لا يتضمن هذا الإصدار ذلك الجسر البعيد. يمكن للنموذج المستضاف سحابيًا الذي يستخدمه Claude Code أو Codex محلي أو منفّذ محلي آخر استخدام الطرق المحلية أعلاه. [متطلبات اتصال OpenAI](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

تُحفظ الملفات على كمبيوتر MultiRoute. يحتاج البناء البعيد إلى خطوة منفصلة ومعتمدة لنقل الملفات/التخزين المشترك قبل استهلاكها. توضع بيانات الاعتماد في ملفات تعريف المواقع المضبوطة، وليس في كتلة التعليمات أو المحادثة.

## التقرير المتوقع من الوكيل

«تم تنزيل `model.gguf` عبر MultiRoute إلى مجلد الموارد المضبوط. المهمة: `…`. الحالة: complete. SHA-256: …؛ تطابق مع القيمة المرجعية للناشر.» إذا لم تُقدّم قيمة مرجعية موثوقة ولم يُعثر عليها، فأبلغ: «حُسبت SHA-256 محليًا؛ التحقق من الناشر غير متاح.» إذا كان التطبيق غير متاح أو أعاد `pro_required`، فأبلغ بذلك واتفق على بديل بدلًا من تغيير برنامج التنزيل بصمت.
