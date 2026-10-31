# macOS: التثبيت والتشغيل الأول والأذونات

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · **العربية** · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

## التثبيت والفتح

قبل التحديث، انتظر انتهاء النقل واختر **Quit/Exit** من قائمة MultiRoute في شريط القوائم؛ الزر الأحمر قد يخفي النافذة فقط. نزّل PKG المناسب لـApple Silicon أو Intel من [التنزيلات](../../downloads/README.md)، وتحقق من SHA-256 وافتحه. أكد التثبيت داخل حوار مسؤول macOS. شغّل `/Applications/MultiRoute Downloader.app` من Finder أو Spotlight؛ لا تحتاج Java منفصلة أو Terminal.

## السماح بالفتح

إذا تعذر التحقق من المطور، حاول فتح PKG أو التطبيق أولًا. للملف الموثوق الذي تحققت منه فقط: **إعدادات النظام → الخصوصية والأمان → Open Anyway → Open**؛ قد تختلف التسميات. عند تحذير من برنامج ضار أو ملف تالف، توقف وتحقق من الملف. [إرشادات Apple](https://support.apple.com/en-us/102445).

## الشبكة وكلمة المرور

قد يطلب تشغيل التطبيق أول مرة أو تطبيق IP الافتراضي أو DHCP أو MAC التطبيق صلاحية مسؤول لفتح Ethernet BPF. تغيير الإعدادات أو إعادة تشغيل المكوّن قد يطلبها مجددًا. الفحوص العادية واختبار السرعة يعيدان استخدام المكوّن الجاهز، دون كلمة مرور لكل ملف أو بوابة. لا تتغير مسارات النظام أو MAC البطاقة الفعلية.

إذا ظهر طلب **الشبكة المحلية**، اسمح بإدخال MultiRoute الذي تعرفه ضمن الخصوصية والأمان → الشبكة المحلية. لا يظهر الطلب في كل تثبيت. [Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy).

كلمة مرور مسؤول Mac تُدخل في الحوار النظامي فقط، وليست ترخيصًا أو رمز Hugging Face. قد يظهر الطالب باسم `DesktopLauncher`/Java؛ أكد فقط عملية MultiRoute الموثوقة التي بدأتها. بعد الإلغاء أعد الفحص صراحة واقرأ السجل. لا تعطّل Gatekeeper/SIP عالميًا.

[MultiRoute Downloader — العربية](../ar.md) · [macOS — English / 12 languages](README.md)
