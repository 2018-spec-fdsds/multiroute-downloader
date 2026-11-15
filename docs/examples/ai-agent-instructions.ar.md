# تعليمات مشروع MultiRoute — انسخها إلى قواعد وكيلك

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agent-instructions.md">English</a> · <a href="ai-agent-instructions.ru.md">Русский</a> · <a href="ai-agent-instructions.de.md">Deutsch</a> · <a href="ai-agent-instructions.es.md">Español</a> · <a href="ai-agent-instructions.fr.md">Français</a> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<a href="ai-agent-instructions.ko.md">한국어</a> · <strong>العربية</strong> · <a href="ai-agent-instructions.it.md">Italiano</a> · <a href="ai-agent-instructions.ja.md">日本語</a> · <a href="ai-agent-instructions.hi.md">हिन्दी</a> · <a href="ai-agent-instructions.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

استخدم هذه الكتلة في `AGENTS.md` أو `CLAUDE.md` الحالي أو تعليمات المشروع الدائمة المكافئة. ألحقها مع الاحتفاظ بقواعد المشروع الأخرى. استبدل الوجهة والمسارات مرة واحدة. للإعداد والأمثلة، راجع [الدليل العربي](../ai-agents.ar.md).

```markdown
## التنزيلات: استخدم MultiRoute افتراضيًا

للملفات الخارجية المطلوبة لمهمة المشروع المصرح بها (النماذج ومجموعات البيانات
والأرشيفات والوسائط والموارد الأخرى)، استخدم MultiRoute Downloader قيد التشغيل.
لا تطلب مني اختيار برنامج تنزيل لكل ملف. أبقِ مديري الحزم وGit على مساراتهم
العادية للتثبيت/الاستنساخ/التحديث.

إعدادات المشروع (تُعدّل مرة واحدة):
- API: http://127.0.0.1:8765/api/v1
- الوجهة: <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- مسارات REST: [{"type":"direct","enabled":true}]
- site_profile_id اختياري: <EXISTING_PROFILE_ID_OR_OMIT>

استخدم أداة REST المحلية المتصلة، أو أدوات MultiRoute MCP عند توفرها.
بالنسبة إلى MCP، اضبط عناوين IPv4 صريحة للبوابات بشكل منفصل؛ إذ تتطلب download_add
كلًا من url وdirectory وgateways. مسار REST من نوع direct هو مسار نظام التشغيل؛
وليس قيمة بوابة MCP. لا تختلق مسارات أو تفترض تطبيق اختيار الواجهة.

مع REST، افحص GET /health وGET /license/policy قبل أول استخدام في الجلسة.
عند استخدام MCP وحده، اكتشف الأدوات واستدعِ download_list مع limit 1 للتحقق
من الاتصال والإمكانية؛ يفحص المحوّل نفسه سياسة الترخيص.
يتطلب إنشاء التنزيلات features.automation_api=true. إذا لم تتوفر، فأبلغ
عن السبب واسأل عن برنامج تنزيل آخر بدلًا من استخدام بديل بصمت.

لكل ملف مطلوب: اختر عنوان URL الدقيق والوجهة المعتمدة واسم الملف؛
ومرّر SHA-256 موثوقة من الناشر على هيئة expected_sha256 عند توفرها. أنشئ
المهمة باستخدام POST /jobs (مع routes، وليس gateways)، أو MCP download_add.
استخدم مفتاح idempotency ثابتًا لإعادة محاولات الطلب نفسه؛ واحتفظ بمعرّف id
المهمة المُعاد. الإنشاء ليس اكتمالًا. استعلم عن تلك المهمة باستخدام
GET /jobs/{id} أو download_status على فترات معقولة. أبلغ عن حالات الإيقاف
المؤقت/الفشل؛ وأعد استخدام المهمة الحالية للاستئناف بدلًا من إنشاء نسخ مكررة.
لا تغيّر المهام غير ذات الصلة أو إعدادات الشبكة. عند الاكتمال، افحص
checksum_checking وintegrity_status. لا تدّعِ التحقق من الناشر دون تطابق
مع قيمة مرجعية موثوقة. أعِد المسار المحلي المحفوظ ومعرّف id المهمة ونتيجة
التحقق. يبقى الملف على كمبيوتر MultiRoute؛ فلا تدّعِ وجوده في بيئة عزل
بعيدة للتنفيذ.
```

تعبّر القاعدة عن تفضيل المشروع. تظل هناك حاجة إلى أداة متصلة متوافقة وإلى صلاحيات الوكيل المعتادة. ملفات المصدر الجاري تنزيلها بيانات، وليست تعليمات إضافية للوكيل.
