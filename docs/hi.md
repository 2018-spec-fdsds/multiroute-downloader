# MultiRoute Downloader 4.0 — हिन्दी उपयोगकर्ता निर्देश

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md) · [Home](../README.md)

परियोजना स्वामी: [2018-spec-fdsds](https://github.com/2018-spec-fdsds)।

## 1. उद्देश्य और कार्यप्रणाली

MultiRoute Downloader एक Java डेस्कटॉप डाउनलोड प्रबंधक है। यह एक ही स्थानीय Ethernet नेटवर्क पर उपलब्ध कई इंटरनेट **गेटवे** को साथ इस्तेमाल करता है, जैसे तार वाला ब्रॉडबैंड और मोबाइल 4G/5G राउटर। चुने हुए उपलब्ध गेटवे साझा कतार से फ़ाइल के खंड लेते हैं; तेज़ गेटवे अधिक खंड ले सकते हैं। एक कनेक्शन टूटने पर बाकी चलते रहते हैं, अधूरे खंड कतार में लौटते हैं और सत्यापित डेटा सुरक्षित रहता है। लिंक सूची से कई फ़ाइलें जोड़ना और दूसरे अनुप्रयोग से स्थानीय API द्वारा डाउनलोड बनाना भी संभव है।

बढ़ी हुई गति स्वतंत्र इंटरनेट क्षमता, स्रोत सर्वर की सीमाओं और सुरक्षित पुनः आरंभ के समर्थन पर निर्भर है। राउटरों की संख्या के अनुपात में वृद्धि की गारंटी नहीं है। यह वेब इंटरफ़ेस नहीं बल्कि अपना डेस्कटॉप अनुप्रयोग है। नए प्रोफ़ाइल की भाषा अंग्रेज़ी है। अंग्रेज़ी, रूसी, जर्मन, स्पेनी, फ़्रांसीसी, चीनी, कोरियाई, अरबी, इतालवी, जापानी, हिन्दी और वियतनामी उपलब्ध हैं। चुनाव सहेजा जाता है और पंजीकृत संदेश, स्थिति, लॉग तथा संकेत उसी भाषा में दिखते हैं। बाहरी सर्वर/लाइब्रेरी का मूल संदेश कभी अनूदित नहीं हो सकता। फ़ाइल नाम, पथ, URL और JSON कुंजियाँ नहीं बदली जातीं।

## 2. कंप्यूटर और नेटवर्क की आवश्यकताएँ

| प्लेटफ़ॉर्म | सिस्टम और CPU | आवश्यक घटक |
| --- | --- | --- |
| Windows | Windows 11 x64; ARM64 पैकेज नहीं | मूल इंस्टॉलर में Java 21 और x64 नेटवर्क घटक; Npcap अलग स्थापित होता है |
| Apple Silicon Mac | M1/M2/M3/M4 या नया, macOS 26+ | arm64 PKG में Java और संकलित नेटवर्क घटक |
| Intel Mac | x86_64, macOS 15+ | x86_64 PKG में Java और संकलित घटक |
| Linux | glibc वितरण, x86_64/ARM64; torrent/magnet के लिए glibc 2.38+ | पूर्ण Java 17+, sudo, C compiler, binutils, pkg-config, libpcap विकास फ़ाइलें; GUI के लिए X11 या Wayland+XWayland और फ़ॉन्ट लाइब्रेरी |

Java में `java.desktop`, `java.net.http`, `jdk.httpserver`, `java.xml` आवश्यक हैं, `--headless` में भी। मूल Windows/macOS पैकेज के लिए अलग Java नहीं चाहिए। पुराने portable ZIP 3.0 और Linux परिनियोजन आवश्यकता पर आधिकारिक स्रोत से निजी Java प्राप्त करते हैं। अलग संस्करणों की केवल कुछ फ़ाइलें मिलाकर न चलाएँ।

सभी चुने हुए गेटवे के समान IPv4 सबनेट पर सक्रिय **तार वाला Ethernet** अडैप्टर चाहिए। प्रत्येक राउटर को अनुप्रयोग के आभासी पते के लिए इंटरनेट देना होगा। केवल Wi-Fi, केवल VPN या केवल IPv6 इस परिवहन के समर्थित विन्यास नहीं हैं। सुविधाजनक उपयोग के लिए 4 GB RAM सुझाई जाती है; यह मापी हुई अनिवार्य सीमा नहीं है। GPU, CUDA या AI मॉडल नहीं चाहिए। प्रोग्राम, डेटा और डाउनलोड फ़ोल्डर लिखने योग्य हों। परिनियोजन कम से कम 512 MiB खाली स्थान जाँचता है; वास्तविक फ़ाइलों, खंडों और अस्थायी डेटा के लिए अतिरिक्त जगह चाहिए। torrent में पूरे आवश्यक टुकड़ों, फ़ाइल सीमाओं, गंतव्य और कैश की जगह रखें।

चुनी हुई लिपि का समर्थन करने वाले फ़ॉन्ट सिस्टम में होने चाहिए। न्यूनतम Linux पर वितरण के उचित पैकेज लगाएँ, जैसे जापानी/चीनी/कोरियाई के लिए Noto CJK और देवनागरी/अरबी के लिए उपयुक्त Noto परिवार। Java प्लेटफ़ॉर्म फ़ॉन्ट fallback इस्तेमाल करता है। अनुप्रयोग फ़ॉन्ट पैक स्थापित या बंडल नहीं करता; समर्थन कंप्यूटर के अनुसार बदल सकता है।

## 3. स्थापना और सुरक्षा स्वीकृतियाँ

[रिलीज़ 4.0](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.0) में संलग्न होने पर उपयुक्त एक इंस्टॉलर डाउनलोड करें और प्रकाशित SHA-256 से तुलना करें।

- Windows: `MultiRoute-Downloader-4.0-Windows-x64-Setup.exe`।
- Apple Silicon, macOS 26+: `MultiRoute-Downloader-4.0-macOS-arm64.pkg`।
- Intel, macOS 15+: `MultiRoute-Downloader-4.0-macOS-x86_64.pkg`।

### Windows

EXE खोलकर अपने उपयोगकर्ता के लिए स्थापना पूरी करें, फिर Start menu या डेस्कटॉप शॉर्टकट से खोलें। यह कमांड स्क्रिप्ट की खिड़की में नहीं चलता। हमारे सेटअप का पाठ अंग्रेज़ी में है; सिस्टम सुरक्षा संवाद Windows की भाषा में होते हैं।

पहले आरंभ पर Npcap जाँचा जाता है। न होने पर अनुप्रयोग में डाउनलोड की स्वीकृति दें: निश्चित आधिकारिक इंस्टॉलर का SHA-256 और Authenticode प्रकाशक जाँचकर सामान्य Npcap इंस्टॉलर खुलता है। लाइसेंस पढ़ें और मानक UAC अनुरोध स्वीकारें। माँगे जाने पर Windows पुनः शुरू करें। Npcap बंडल या चुपचाप स्थापित नहीं होता; उसके न होने पर इस चरण में इंटरनेट चाहिए। AdminOnly ड्राइवर अतिरिक्त capture अनुमति माँग सकता है; पूरी Java को administrator के रूप में न चलाएँ।

प्रोग्राम और इंस्टॉलर में प्रकाशक हस्ताक्षर नहीं हैं। SmartScreen के अज्ञात प्रकाशक संदेश पर आधिकारिक स्रोत और हैश जाँचने के बाद ही “More info → Run anyway” उपलब्ध होने पर उसका उपयोग तय करें। मैलवेयर चेतावनी अलग स्थिति है। antivirus बंद न करें और स्थापना के लिए exclusion न बनाएँ।

### macOS

मूल CPU से मेल खाता PKG Apple Installer में खोलें और सुरक्षित नेटवर्क घटक रखने की administrator स्वीकृति दें। फिर Finder, Spotlight या Dock से `/Applications/MultiRoute Downloader.app` खोलें। इस पैकेज को अलग Java, Terminal, Command Line Tools या Homebrew की आवश्यकता नहीं है।

Ethernet BPF उपकरण खोलने के लिए मानक macOS administrator संवाद आता है। capture खुलने के बाद घटक अपने अधिकार घटाता है और Java सामान्य खाते में रहती है। ऐप/PKG **Developer ID से हस्ताक्षरित या notarized नहीं हैं**। अज्ञात developer के लिए अपवाद उपलब्ध हो तो पहले स्रोत और SHA-256 सत्यापित करें, फिर [Apple के निर्देश](https://support.apple.com/en-us/102445) अनुसार System Settings → Privacy & Security → Open Anyway, फिर Open चुनें। प्रबंधित Mac पर नीति इसे रोक सकती है।

स्थापना, बाद की नेटवर्क अनुमति और अज्ञात developer अपवाद अलग पुष्टियाँ हैं। माँगे जाने पर स्थानीय नेटवर्क और चुने हुए डाउनलोड फ़ोल्डर की पहुँच दें। Gatekeeper/SIP बंद न करें, पूरे सिस्टम की quarantine विशेषताएँ न हटाएँ और पूरी Java को root न चलाएँ। मैलवेयर/क्षतिग्रस्त फ़ाइल संदेश साधारण developer चेतावनी नहीं है; रुककर फ़ाइल जाँचें।

### Linux और portable ZIP 3.0

पूरा ZIP निकालें। Linux में पहले `bash ./Deploy-Linux.sh`, बाद में `bash ./Start-Linux.sh`। Windows portable में `Deploy-Windows.cmd`, फिर `Start-Windows.cmd`; macOS portable में `Deploy-macOS.command` और `Start-macOS.command`। पुराने ZIP स्क्रिप्ट विधि रखते हैं और मूल 4.0 इंस्टॉलर से अलग हैं। Linux को वितरण के compiler/libpcap पैकेज और macOS portable को Apple Command Line Tools तथा sudo चाहिए हो सकते हैं। आवश्यक अनुमति वाला Terminal खुला रखें, लेकिन पूरी Java को root न चलाएँ। Alpine/musl शामिल नहीं है। परिनियोजन OS को upgrade या glibc को प्रतिस्थापित नहीं करता।

बारह भाषाओं के ऑफ़लाइन निर्देश शामिल हैं। Windows: `%LOCALAPPDATA%/MultiRoute Downloader App/app/docs/`। macOS: ऐप पर Show Package Contents से `Contents/app/docs/hi.md` खोलें। ब्राउज़र में संस्करण-विशिष्ट सहायता लिंक को इंटरनेट चाहिए।

## 4. गेटवे और आभासी पता

प्रारंभिक चार गेटवे `192.168.1.1`, `192.168.1.2`, `192.168.1.3`, `192.168.1.4` हैं और सभी चुने रहते हैं। पते बदलें या **1–32** अलग गेटवे तक बढ़ाएँ। कंप्यूटर का किसी एक को default gateway बनाना पाँचवाँ इंटरनेट कनेक्शन नहीं बनाता।

अनुप्रयोग का आभासी IPv4 शुरू में `192.168.1.70` है। नेटवर्क सेटिंग में जाँचें/लागू करें, सुनिश्चित करें कि कोई उपकरण उपयोग नहीं करता, और DHCP के स्वतः आवंटन से बाहर आरक्षित करें। यह आंतरिक परिवहन के लिए है: **OS अडैप्टर में IP नहीं जुड़ता और सिस्टम routing table नहीं बदलती**। अन्य अनुप्रयोग सामान्य कनेक्शन रखते हैं। OS नेटवर्क सेटिंग में इसे हाथ से न जोड़ें। प्रश्न चिह्न और hover संकेत देखें।

गेटवे जाँचकर एक, कई या सभी चुनें। गति परीक्षण **16 MiB या 5 सेकंड** तक है; तैयारी में अतिरिक्त समय लग सकता है। ये केवल परीक्षण की सीमाएँ हैं, वास्तविक डाउनलोड की नहीं। उचित सीधा HTTP लिंक न हो तो Selectel परीक्षण फ़ाइल ली जाती है।

डाउनलोड चलते समय प्रोफ़ाइल जोड़/हटा सकते हैं और कार्य के चुने गेटवे बदल सकते हैं, लेकिन हर अधूरे कार्य के लिए कम से कम एक रहे। नया प्रोफ़ाइल मौजूदा कार्य में अपने-आप नहीं चुना जाता। आभासी IP बदलने से पहले सक्रिय कार्य रोकें। MAC पते इंटरफ़ेस में छिपे हैं।

## 5. डाउनलोड और इंटरफ़ेस

1. लंबे URL क्षेत्र में Ctrl+V, Mac पर Cmd+V, या पेस्ट बटन इस्तेमाल करें। magnet चिपकाएँ या `.torrent` खोलें।
2. OS फ़ोल्डर चयनक से गंतव्य चुनें। आरंभिक Windows `%USERPROFILE%\Downloads`, macOS/Linux `~/Downloads`; हाथ से चुना फ़ोल्डर सुरक्षित रहता है।
3. गेटवे चुनकर URL के पास डाउनलोड शुरू करें। Hugging Face संग्रह, WebDAV और Metalink में पूर्वावलोकन व फ़ाइल चयन उपलब्ध हैं। समूह आयात एक लिंक प्रति पंक्ति या पाठ फ़ाइल स्वीकारता है।
4. प्रत्येक कार्य की प्रगति, बीता/शेष समय, संयुक्त और हर गेटवे की गति/मात्रा देखें। HTTP खंड default **20 MiB**, सीमा 1–64 MiB हैं। **60 सेकंड** बिना डेटा वाला खंड रद्द होकर साझा पूल में आता है और अन्य गेटवे ले सकते हैं। torrent टुकड़े अपने metadata से आकार लेते हैं।
5. रोकना/जारी रखना, प्राथमिकता, सीमा, समयसूची और रुके कार्य का फ़ोल्डर बदलना संभव है। default दो सक्रिय कार्य हैं। पुनः आरंभ पर पहले चल रहे कार्य paused लौटते हैं। torrent seeding default बंद है, समय/अनुपात सीमा रख सकते हैं।

FlatLaf में हल्की, गहरी और सिस्टम theme चुनी व सहेजी जाती है। सारांश फ़िल्टर से अलग सभी/सक्रिय/पूर्ण कार्य और कुल गति बताता है। अलग progress bars, पूरी चौड़ाई के कॉलम, sort, search, status filter और स्थिर scroll बने रहते हैं।

चलती या पूरी पंक्ति पर hover से **पूरा फ़ाइल नाम और पूर्ण निरपेक्ष गंतव्य पथ** मिलता है, sort/filter के बाद भी सही फ़ाइल का। चुनी फ़ाइल के शीर्षक में वही संकेत है और right-click menu से पूरा पथ कॉपी होता है। Ctrl+L/Cmd+L URL, Ctrl+F/Cmd+F search, Escape search साफ़ करता है।

चुने कार्य में विवरण और इतिहास tabs हैं। refresh/भाषा परिवर्तन पर पढ़ने की जगह और चयन सुरक्षित रहते हैं; दूसरा कार्य चुनने पर उसके विवरण का आरंभ खुलता है। सामान्य लॉग में अनुप्रयोग घटनाएँ और डाउनलोड इतिहास अलग हैं। अंत पर नए संदेश follow होते हैं, पुराना पढ़ते समय दृश्य नहीं कूदता।

खिड़की बंद होने पर tray में डाउनलोड जारी रहता है। चिह्न सभी कार्यों की कुल गति 10 Gbit/s तक और tooltip में सटीक मूल्य दिखाता है। tray से खोलें या Exit application द्वारा सहेजकर बंद करें। tray न हो तो taskbar में minimize होता है। **update से पहले tray से बाहर निकलें**।

कार्य हटाते समय फ़ाइल मिटाने का विकल्प default **अचिह्नित** है। केवल सूची से हटाकर पूरा/अधूरा फ़ाइल रखें; फ़ाइल तभी मिटाएँ जब सच में चाहें। कार्य या फ़ाइल हटाने से स्थायी इतिहास लॉग नहीं मिटता।

## 6. प्रोटोकॉल, पहुँच और जाँच

| स्रोत | व्यवहार व शर्तें |
| --- | --- |
| HTTP/HTTPS | सुरक्षित फ़ाइल पहचान और byte ranges पर खंड/पुनः आरंभ; अन्यथा एक गेटवे पर पूरा stream, टूटने के बाद शून्य से |
| Hugging Face | फ़ाइल/संग्रह चयन और Hub/Xet resolution; private/सीमित स्रोत के लिए उचित अनुमति |
| FTP | passive EPSV/PASV; plain FTP credentials या डेटा encrypt नहीं करता |
| FTPS | control/data के लिए explicit TLS; implicit FTPS समर्थित नहीं |
| SFTP | स्वतंत्र रूप से जाँचा व pin किया SHA256 SSH host-key fingerprint अनिवार्य; अज्ञात key चुपचाप स्वीकार नहीं |
| WebDAV | HTTPS सूची और फ़ाइल चयन |
| Metalink | `.meta4`, विश्वसनीय SHA-256, mirrors और उपलब्ध piece hashes |
| Torrent/magnet | चुने गेटवे से परिवहन, metadata के अनुसार अनिवार्य piece verification, फ़ाइल चयन/प्राथमिकता |

source access में सही origin के लिए user/password, Bearer token, Cookie या स्वीकृत headers स्थानीय encryption के साथ सहेज सकते हैं। ये राउटर tokens नहीं हैं। URL में password न डालें। विश्वसनीय प्रकाशित/हाथ से दिया SHA-256 या SHA-512 न हो तो FTP/FTPS/SFTP पूरा-file mode इस्तेमाल करते हैं।

assembly के बाद एक पढ़ाई में **MD5, SHA-256, SHA-512** गणना होती है। मुख्य field 32/64/128 hexadecimal अक्षरों से प्रकार पहचानता है और अतिरिक्त references दिए जा सकते हैं। पूरे representation digest headers तथा `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512`, `SHA512SUMS` खोजे जाते हैं; Hugging Face LFS और Metalink बने रहते हैं। आंशिक HTTP उत्तर का digest पूरे फ़ाइल का नहीं माना जाता।

अंतिम फ़ाइल प्रकाशित होने से पहले हर ज्ञात reference मेल खाना चाहिए। असंगति पर कार्य विफल और आंशिक डेटा सुरक्षित रहता है। reference न होने पर केवल स्थानीय गणना है, प्रकाशक से पुष्टि नहीं। MD5 आकस्मिक खराबी के लिए है, मजबूत पहचान या SHA-256/SHA-512 का विकल्प नहीं। torrent piece जाँच और whole-file hashes अलग हैं; हाथ से पूरा checksum देने के लिए केवल एक फ़ाइल चुनी हो।

<a name="multiroute-api"></a>

## 7. दूसरे अनुप्रयोग से API

चलते समय API `http://127.0.0.1:8765/api/v1` पर है। JSON UTF-8 और body के लिए `Content-Type: application/json` इस्तेमाल करें। स्थानीय कार्य बनाने/चलाने में API token नहीं चाहिए; source credentials अलग हैं। केवल उसी कंप्यूटर से पहुँच है; LAN/इंटरनेट पर port न खोलें। `--headless` केवल API चलाता है। native launcher इस्तेमाल करें; `.cmd` उदाहरण portable के हैं।

जाँच: `GET /api/v1/health`, सामान्य उत्तर `{"ok":true,"version":"4.0"}`। पहले UI में नेटवर्क सेट करें या `POST /api/v1/network` को यह दें:

```json
{
  "virtual_ip": "192.168.1.70",
  "gateways": ["192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4"]
}
```

यह JSON `POST /api/v1/jobs` को दें। URL, वास्तविक स्थानीय folder और आवश्यकता पर gateway बदलें। Linux उदाहरण `/home/your-user/Downloads`, Mac `/Users/your-user/Downloads`। `routes` स्पष्ट दें, API UI checkboxes अपने-आप नहीं लेता।

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

`HTTP 201` निर्माण है, completion नहीं। उत्तर का `id` रखें और `GET /api/v1/jobs/{id}` से `status: complete` तक जाँचें या `error` पढ़ें। `speed_bps` **bytes/second** है; 8 से गुणा और 1,000,000 से भाग देकर Mbit/s मिलता है।

| HTTP | API |
| --- | --- |
| GET | `/api/v1/health` |
| GET / POST | `/api/v1/jobs` |
| GET | `/api/v1/jobs/{id}` |
| POST | `/api/v1/jobs/{id}/pause` |
| POST | `/api/v1/jobs/{id}/resume` |
| POST | `/api/v1/jobs/{id}/routes` |
| DELETE | `/api/v1/jobs/{id}` |
| GET / POST | `/api/v1/network` |
| POST | `/api/v1/jobs/batch` |
| GET / POST | `/api/v1/queue` |
| GET | `/api/v1/events` |

pause/resume को `{}` दें। delete body, default file preservation, `Idempotency-Key`, queue, batch, SSE, access और limits के लिए [OpenAPI](../openapi.yaml) देखें। `expected_md5`, `expected_sha256`, `expected_sha512` भेज सकते हैं; परिणाम `md5`, `sha256`, `sha512`, `checksum_sources` और warnings देते हैं। भाषा से JSON keys/status नहीं बदलते। [विस्तृत उदाहरण](../API.ru.txt) रूसी में हैं।

Python 3 उदाहरण: पहले वास्तविक URL दें, नेटवर्क सेट करें और अनुप्रयोग खुला रखें। UTF-8 हिन्दी व अन्य Unicode पथों को सुरक्षित रखता है।

```python
import json
import urllib.request
from pathlib import Path

data = {
    "url": "https://example.org/large-file.bin",
    "directory": str(Path.home() / "Downloads"),
    "chunk_mb": 20,
    "routes": [
        {"type": "gateway", "gateway": f"192.168.1.{n}", "enabled": True}
        for n in range(1, 5)
    ],
}
request = urllib.request.Request(
    "http://127.0.0.1:8765/api/v1/jobs",
    data=json.dumps(data, ensure_ascii=False).encode("utf-8"),
    headers={"Content-Type": "application/json; charset=utf-8"},
    method="POST",
)
with urllib.request.urlopen(request, timeout=30) as response:
    job = json.loads(response.read().decode("utf-8"))
print(job["id"])
```

## 8. इतिहास, डेटा और अपडेट

| सिस्टम | डेटा | अनुप्रयोग/runtime |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app` |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/` |

native Mac घटक `/Library/PrivilegedHelperTools/MultiRouteResume/4.0/gateway-net` है। Windows portable 3.0 launch folder का `data/` इस्तेमाल करता है, जिसे `data-location.config`/`--data-dir` बदल सकते हैं। Linux private Java `~/.local/share/MultiRouteResume/runtime`, protected component `/usr/local/libexec/MultiRouteResume/3.0/gateway-net` है। वास्तविक डाउनलोड आपके चुने स्थान पर अलग रह सकते हैं।

`logs/downloads-history.jsonl` UTF-8 line JSON और `logs/downloads-history.log` तारीख/समय वाला readable text है। पुनः आरंभ के बाद append होते हैं और कार्य हटाने/भाषा बदलने से पुराना पाठ नहीं लिखा जाता। text लिखने के समय की भाषा रखता है; UI वर्तमान भाषा में दिखाता है। viewer अधिकतम पिछली 1,000 घटनाएँ/2 MiB पढ़ता है; पूरा बढ़ता archive disk पर है, ज़रूरत पर अलग संग्रह करें। नाम और निरपेक्ष path रहते हैं, source URL/password/token नहीं। साझा करने से पहले निजी paths जाँचें।

`lifecycle_history` में `event_id`, `job_id`, `kind`, `time`, `filename`, `path`, torrent में वैकल्पिक `paths`, pause में `reason` (`pause`, `shutdown`, `maintenance`) हैं। प्रकार `started`, `resumed`, `paused`, `completed`, `recovered`, `failed` हैं। समय ISO 8601 UTC (`2026-10-18T12:34:56.123Z`), readable log में स्थानीय पूरी तारीख/समय व UTC offset है। वैकल्पिक `first_started_at`, `last_resumed_at`, `completed_at` सच में देखा आरंभ/हाल की पुनः शुरुआत/जाँच और प्रकाशन बाद completion बताते हैं। queue में होना start नहीं। पुराने समय नहीं गढ़े जाते। `recovered` अभी पुराने प्रकाशित फ़ाइल की जाँच है, उसकी पुरानी completion तारीख जानना नहीं।

update से पहले tray से exit करें, पूरा नया package लगाएँ और डेटा/खंड रखें। Windows को Installed apps से हटाएँ। Mac पर `.app` हटाने से इतिहास/सुरक्षित घटक अपने-आप नहीं हटते। uninstall करने को डाउनलोड folder न मिटाएँ। Linux folder स्थान बदलने पर deploy दोबारा चलाएँ।

## 9. समस्या समाधान और परीक्षण सीमा

- start समस्या: version, OS/CPU, पूरा package, Java modules और गलत `JAVA_EXE`/`JAVA_HOME` जाँचें। 3.9 का temporary-output lock fix और पुरानी instance पहचान सुरक्षित हैं।
- शून्य गति: Ethernet, subnet, खाली virtual IP और gateway/source इंटरनेट अनुमति जाँचें। speed test सफलता हर असली download की गारंटी नहीं।
- torrent glibc/macOS error: समर्थित OS इस्तेमाल करें, सिस्टम libraries हाथ से न बदलें।
- SFTP key mismatch: administrator से पुष्टि करें, key verification बंद न करें।
- किसी और का port 8765: `--port 8766` दें और असंबंधित process न रोकें; केवल पहचानी MultiRoute instance बदली जाती है।
- जगह/permission: file, parts, cache जगह जाँचें; destination बदलने से पहले pause। समान target का duplicate न बनाएँ, पुराना कार्य जारी रखें।

version, OS/CPU, चरण और सही error भेजें, password/token, private URL और निजी डेटा निकालकर। नियंत्रित Windows/POSIX, Mac CI और package checks वास्तविक router speed/failover, user installation या Finder/Gatekeeper/BPF अनुमति का प्रमाण नहीं। बिना publisher signature/notarization के इसे **पूर्व-रिलीज़** समझें। पहले हर gateway पर ज्ञात छोटी फ़ाइल और फिर संयुक्त समूह जाँचें।

## 10. लाइसेंस

मूल अनुप्रयोग व्यक्ति/संस्था और व्यापारिक काम में मुफ़्त इस्तेमाल किया जा सकता है। अलग अनुमति के बिना उसे बेचना, लाइसेंस शुल्क लेना या भुगतान पर बाँटना मना है। custom license सामान्य modification/redistribution अधिकार नहीं देता और OSI-approved open-source license नहीं है। अंग्रेज़ी [LICENSE.txt](../LICENSE.txt) लागू है। FlatLaf (Apache-2.0) और अन्य घटक अपने licenses रखते हैं। स्वामी: [2018-spec-fdsds](https://github.com/2018-spec-fdsds)।

## नया नाम और पुराने प्रोफ़ाइल

MultiRoute Downloader का पुराना नाम MultiRoute Resume था। नया प्रोफ़ाइल ऊपर के नए पथ इस्तेमाल करता है। नई डेटा जगह न हो और पुराना प्रोफ़ाइल मौजूद हो तो बिना फ़ाइल हटाए/स्थान बदले वही इस्तेमाल होता है। पुराना डेटा `%LOCALAPPDATA%\MultiRoute Resume\data` या `~/Library/Application Support/MultiRoute Resume/data` में रह सकता है। सुरक्षित `MultiRouteResume` folder, Windows upgrade ID, आंतरिक API पहचान और port संगत रखे गए हैं। स्थापना बाद नया MultiRoute Downloader shortcut/app इस्तेमाल करें। पुरानी रिलीज़ों के फ़ाइल नाम नहीं बदलते।

## सहेजी गई साइटें, अपलोड और संग्रह पूर्वावलोकन (4.0)

**साइट प्रोफ़ाइल…** में प्रोफ़ाइल बनाएँ/संपादित करें और **डाउनलोड** या **अपलोड** की सूची में चुनें। गेटवे और आभासी IP **नेटवर्क सेटिंग** में हैं। **अपलोड** में **दूरस्थ फ़ोल्डर URL** दें और **फ़ाइलें चुनें और अपलोड करें…** से सिस्टम चयनक में एक या अनेक स्थानीय फ़ाइलें चुनें। इंटरफ़ेस नाम जोड़ता है; API `target_url` पूरा दूरस्थ फ़ाइल URL है। संग्रह URL मुख्य फ़ील्ड में पेस्ट करके **आर्काइव की सामग्री…** दबाएँ; डाउनलोड शुरू किए बिना पूर्वावलोकन मिलता है।

साइट प्रोफ़ाइल में नाम, आधार URL, स्थानीय डाउनलोड फ़ोल्डर, दूरस्थ फ़ोल्डर और गेटवे सहेजें। अनाम पहुँच, HTTP Basic/Bearer/Cookie/अपने हेडर, FTP और पहले सत्यापित SHA-256 सर्वर कुंजी वाला SFTP समर्थित हैं। एक सर्वर के अलग खातों की पहचान अलग रहती है। स्पष्ट फ़ोल्डर/गेटवे प्राथमिक हैं; सापेक्ष URL आधार से बनता है। रहस्य केवल मालिक की पहुँच वाले `site-profiles/vault.bin` में स्थानीय `site-profiles/key.bin` से एन्क्रिप्ट होते हैं; आपके खाते और कुंजी पर नियंत्रण रखने वाला उन्हें पढ़ सकता है। सार्वजनिक API रहस्य नहीं लौटाता। खाली फ़ील्ड पुराने रहस्य तभी रखते हैं जब मूल सर्वर और प्रकार समान हों; `replace_credentials:true` उन्हें बदलता है। अन्य मूल पर HTTP पुनर्निर्देशन को ये पहचान नहीं मिलती। प्रोफ़ाइल हटाने से कार्य या स्थानीय/दूरस्थ फ़ाइलें नहीं मिटतीं; कार्य मूल प्रोफ़ाइल से जुड़ा रहता है और खाता नहीं बदल सकता; मूल हटने पर ज़रूरत के अनुसार वैध प्रोफ़ाइल के साथ कार्य दोबारा बनाएँ।

अपलोड और दूरस्थ फ़ोल्डर देखना चुने गेटवे से **FTP, स्पष्ट FTPS और SFTP** पर उपलब्ध है। सामान्य स्थानीय फ़ाइल और पूरा दूरस्थ फ़ाइल URL (`target_url`) दें, केवल फ़ोल्डर नहीं। अस्थायी दूरस्थ फ़ाइल का मौजूदा आरंभ मिलाकर अपलोड जारी होता है। सफलता के लिए पूरी दूरस्थ फ़ाइल वापस पढ़कर SHA-256 जाँचना आवश्यक है; **पढ़ने, लिखने और नाम बदलने** के अधिकार तथा अतिरिक्त बैंडविड्थ चाहिए। असमान जाँच या पढ़ने की मनाही त्रुटि है। एक फ़ाइल एक समय एक गेटवे लेती है; अलग फ़ाइलें गेटवे पर बँटकर समानांतर चलती हैं। निष्क्रियता सीमा बाद दूसरा गेटवे ले सकता है, लेकिन एक अपलोड समानांतर खंडों में नहीं बँटता। रुकना, जारी करना और प्रगति पुनः आरंभ बाद बने रहते हैं। कार्य हटाने पर स्थानीय और दूरस्थ अंतिम/अस्थायी फ़ाइलें सुरक्षित रहती हैं।

ओवरराइट डिफ़ॉल्ट रूप से बंद है। FTP/SFTP में लक्ष्य न होने पर ही परमाणु नाम परिवर्तन हर जगह उपलब्ध नहीं है; दूसरा क्लाइंट प्रकाशन के दौरान वही लक्ष्य बदल सकता है। साझा लक्ष्य समन्वय से उपयोग करें। फ़ोल्डर सूची कुछ अपलोड या हटाती नहीं है।

**ZIP, TAR, 7Z, RAR4 और RAR5** पूर्वावलोकन नाम, पथ, आकार, फ़ोल्डर और एन्क्रिप्शन दिखाता है; निकालता या चलाता नहीं। चुना गेटवे, बाइट-रेंज और स्थिर ETag या Last-Modified चाहिए। सीमा: 16 MiB मेटाडेटा, 10,000 प्रविष्टियाँ, 60 सेकंड; 7z हेडर डिकोडिंग मेमोरी 64 MiB। सूची अधूरी हो सकती है और सामग्री की अखंडता नहीं जाँचती। नाम बिगड़ने पर मूल charset दें। समर्थित एन्क्रिप्टेड 7z हेडर के लिए दिया पासवर्ड सहेजा नहीं जाता। एन्क्रिप्टेड RAR हेडर असमर्थित/पासवर्ड आवश्यक दिखते हैं; बहुखंड संग्रह आंशिक हो सकते हैं।

API डाउनलोड, अपलोड, सूची और पूर्वावलोकन में `site_profile_id` लेता है। `GET /api/v1/site-profiles` से `{profiles:[...]}` मिलता है; `POST`/`PUT` सपाट प्रोफ़ाइल सहेजकर सार्वजनिक विवरण देता है; `DELETE /api/v1/site-profiles/{id}` केवल प्रोफ़ाइल मिटाता है। `POST /api/v1/uploads` में `local_path`, `target_url`, `routes`, वैकल्पिक `site_profile_id`, `overwrite:false` दें; HTTP 201 मिलता है। GET से `{uploads:[...]}`; व्यक्तिगत GET/DELETE और POST `/{id}/pause` / `/{id}/resume` उपलब्ध हैं। `POST /api/v1/remote/list` में `url`, `route`/`routes`; `POST /api/v1/archives/preview` में अतिरिक्त वैकल्पिक `password`, `charset` हैं। [openapi.yaml](../openapi.yaml) देखें। `data/uploads/` के UTF-8 मैनिफ़ेस्ट में प्रोफ़ाइल ID होता है, पासवर्ड नहीं। पूर्वावलोकन डाउनलोड कार्य नहीं बनाता।

## डाउनलोड समय और प्राथमिकता

कतार/समय में `max_active_jobs` (1–32), कुल/गेटवे गति सीमा और दैनिक समय (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm) चुनें। कंप्यूटर का स्थानीय समय `schedule_timezone` में मिलता है; आधी रात पार हो सकती है और समान समय का अर्थ पूरा दिन है। अवधि बाहर डाउनलोड कतार में लौटते हैं; हाथ से रोके कार्य रुके रहते हैं। `priority` -100 से 100 में ऊँचा पहले, फिर निर्माण समय लागू होता है; सक्रिय डाउनलोड नहीं कटते। `start_after_ms` न्यूनतम Unix मिलीसेकंड आरंभ है, 0 विलंब हटाता है। सेटिंग पुनः शुरू होने पर रहती है और केवल डाउनलोड पर लागू है, स्वतंत्र अपलोड कतार पर नहीं। ऐप और कंप्यूटर चालू तथा जागे रहें; OS जगाने का प्रबंध नहीं है। API: `GET`/`PUT`/`POST /api/v1/queue`, `PUT`/`POST /api/v1/jobs/{id}/options`।

## Chrome और Edge एकीकरण

ऐप में **Chrome / Edge** एकीकरण खोलकर लिंक भेजने की अनुमति दें और एक्सटेंशन फ़ोल्डर खोलें। एक बार `chrome://extensions` या `edge://extensions` में डेवलपर मोड चालू करें, “अनपैक किया गया लोड करें” चुनें और `manifest.json` वाला `browser-extension` दें। नेटिव इंस्टॉलर इसे JAR के पास देता है; स्रोत में [browser-extension/](../browser-extension/) भी है। चुना फ़ोल्डर न हटाएँ। अलग `MultiRoute-Downloader-4.0-Chrome-Edge.zip` पहले निकालें। यह स्टोर में प्रकाशित नहीं है।

MultiRoute चलता रहे, ट्रे भी पर्याप्त है। लिंक के मेनू या एक्सटेंशन पॉपअप से URL भेजें। वर्तमान फ़ोल्डर, गेटवे और साइट प्रोफ़ाइल लगते हैं; आवश्यक repository/torrent चयन बना रहता है। डिफ़ॉल्ट पोर्ट 8765 को बदले API पोर्ट से मिलाएँ। केवल URL भेजता है, cookies/पासवर्ड नहीं और अन्य ब्राउज़र डाउनलोड नहीं पकड़ता। स्रोत पहुँच ऐप में सेट करें। स्थानीय कनेक्शन अनुमति माँगी जाए तो केवल इस चुने एक्सटेंशन को दें।

ब्रिज डिफ़ॉल्ट बंद है और सिर्फ `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` के `/api/v1/browser/status`, `/api/v1/browser/download` स्वीकारता है; अन्य API नियम नहीं बदलते। HTTP 202 का अर्थ डेस्कटॉप तक पहुँचना है, फ़ाइल पूरी होना नहीं। केवल headless सर्वर चयन विंडो नहीं खोल सकता। ब्राउज़र से एक्सटेंशन हटाकर या ऐप एकीकरण बंद करके रोकें।

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
