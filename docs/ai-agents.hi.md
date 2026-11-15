# AI एजेंटों के लिए MultiRoute — एक बार सेटअप करें, डिफ़ॉल्ट रूप से डाउनलोड करें

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <a href="ai-agents.de.md">Deutsch</a> · <a href="ai-agents.es.md">Español</a> · <a href="ai-agents.fr.md">Français</a> · <a href="ai-agents.zh.md">中文</a><br />
<a href="ai-agents.ko.md">한국어</a> · <a href="ai-agents.ar.md">العربية</a> · <a href="ai-agents.it.md">Italiano</a> · <a href="ai-agents.ja.md">日本語</a> · <strong>हिन्दी</strong> · <a href="ai-agents.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

[उत्पाद का परिचय](overview/hi.md) · [API संदर्भ](../API.ru.txt) · [OpenAPI](../openapi.yaml)

प्रोजेक्ट का एक नियम जोड़ें और एक बार टूल कनेक्ट करें। इसके बाद Claude Code, Codex और अन्य टूल-सक्षम एजेंट प्रोजेक्ट के मॉडल वेट, डेटासेट, आर्काइव और अन्य बाहरी फ़ाइलें MultiRoute को सौंप सकते हैं, जॉब ID रख सकते हैं, प्रगति देख सकते हैं और पूरा होना जाँच सकते हैं। आप कह सकते हैं: **“इस प्रोजेक्ट के लिए आवश्यक मॉडल उसके assets फ़ोल्डर में डाउनलोड करो।”** नियम डाउनलोडर की प्राथमिकता बताता है; हर अनुरोध में API का विवरण दोहराने की आवश्यकता नहीं है।

मॉडल क्लाउड या स्थानीय रूप से चल सकता है। महत्वपूर्ण यह है कि **उसका टूल कहाँ चलता है**: वह MultiRoute चलाने वाले कंप्यूटर तक पहुँच सके। ब्राउज़र चैट या दूरस्थ सैंडबॉक्स को केवल निर्देश फ़ाइल पढ़कर आपके PC तक पहुँच नहीं मिलती।

## 1. प्रोजेक्ट के डिफ़ॉल्ट विकल्प एक बार चुनें

1. फ़ाइलें संग्रहीत करने वाले कंप्यूटर पर MultiRoute इंस्टॉल करके शुरू करें। उस इंस्टेंस के लिए कॉन्फ़िगर किया गया API पोर्ट इस्तेमाल करें; उदाहरणों में `8765` है।
2. प्रोजेक्ट assets के लिए पूर्ण पथ वाला फ़ोल्डर चुनें, जैसे Windows पर `C:/Projects/MyProject/assets` या macOS पर `/Users/YOUR_USER/Projects/MyProject/assets`। Mac पर माउंट किया गया नेटवर्क शेयर `/Volumes/Share/...` इस्तेमाल करता है।
3. REST रूट स्पष्ट रूप से चुनें। सामान्य OS कनेक्शन `[{"type":"direct","enabled":true}]` इस्तेमाल करता है। MultiRoute के कॉन्फ़िगर किए गए स्वतंत्र गेटवे इस्तेमाल करने के लिए `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]` इस्तेमाल करें और आवश्यकता होने पर अन्य अनुमोदित गेटवे जोड़ें। पहले ऐप में उन गेटवे को कॉन्फ़िगर और सत्यापित करें। UI के चेकबॉक्स अपने आप अनुरोधों में कॉपी नहीं होते। direct रूट सामान्य OS IP रूटिंग इस्तेमाल करता है, जिसमें लागू सिस्टम/TUN VPN रूट भी शामिल हैं। यह HTTP प्रॉक्सी/PAC सेटिंग या केवल ब्राउज़र वाले VPN एक्सटेंशन विरासत में नहीं लेता।
4. बाहरी डाउनलोड बनाने के लिए **`automation_api`** क्षमता आवश्यक है। बैच बनाने के लिए `batch_downloads` भी आवश्यक है; अन्य सीमाएँ लागू रहती हैं। `GET /license/policy` जाँचें, जिसमें `features.automation_api` भी शामिल है।
5. API को लूपबैक पर रखें। इसमें सामान्य दूरस्थ प्रमाणीकरण टोकन नहीं है; इसका पोर्ट LAN या इंटरनेट पर प्रकाशित न करें। स्रोत के क्रेडेंशियल MultiRoute की साइट प्रोफ़ाइल में रखें और ज़रूरत होने पर उसका `site_profile_id` भेजें।

## 2. दोबारा इस्तेमाल करने योग्य प्रोजेक्ट नियम जोड़ें

**[तैयार निर्देश ब्लॉक](examples/ai-agent-instructions.hi.md)** को प्रोजेक्ट के मौजूदा नियमों में कॉपी करें। अन्य प्रोजेक्ट निर्देश रखते हुए गंतव्य और रूट एक बार बदलें।

| एजेंट / परिवेश | नियम कहाँ रखें | आवश्यक कनेक्शन |
| --- | --- | --- |
| आपके PC पर Claude Code | प्रोजेक्ट में `CLAUDE.md`। यदि नियम पहले से `AGENTS.md` में है, तो दो प्रतियाँ बनाए रखने के बजाय मौजूदा `CLAUDE.md` में `@AGENTS.md` जोड़ें। | स्थानीय shell/REST पहुँच, या नीचे दिया stdio MCP सर्वर। |
| आपके PC पर Codex | प्रोजेक्ट में `AGENTS.md`। | स्थानीय shell/REST पहुँच, या उसका कॉन्फ़िगर किया MCP सर्वर। |
| अन्य स्थानीय एजेंट या स्थानीय मॉडल होस्ट | उसके स्थायी प्रोजेक्ट/सिस्टम निर्देश। | REST कॉल कर सकने वाला टूल या stdio MCP क्लाइंट सक्षम करें। केवल मॉडल लोड करने से टूल नहीं जुड़ते। |
| स्थानीय एजेंट निष्पादक के साथ क्लाउड मॉडल | स्थानीय एजेंट के प्रोजेक्ट नियम। | उसका स्थानीय टूल उसी PC पर MultiRoute को कॉल करता है। मॉडल को स्वयं स्थानीय रूप से चलना आवश्यक नहीं है। |
| ChatGPT web, Claude web, या दूरस्थ क्लाउड निष्पादक | प्रोजेक्ट निर्देश और एक समर्थित कनेक्टेड टूल। | MultiRoute कंप्यूटर तक अलग से कॉन्फ़िगर किया गया ब्रिज चाहिए। यहाँ दिया स्थानीय अडैप्टर stdio है, सार्वजनिक दूरस्थ MCP सेवा नहीं। |

निर्देश फ़ाइलें व्यवहार का मार्गदर्शन करती हैं; वे क्लाइंट के टूल अनुमोदन या सैंडबॉक्स को पार नहीं करतीं। मौजूदा क्लाइंट/खाता प्रतिबंध लागू रहते हैं। आधिकारिक [Claude प्रोजेक्ट निर्देश](https://code.claude.com/docs/en/memory), [Codex प्रोजेक्ट निर्देश](https://developers.openai.com/codex/guides/agents-md/) और [ChatGPT कस्टम MCP कनेक्शन आवश्यकताएँ](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt) देखें।

## 3. REST उदाहरण — shell पहुँच वाला स्थानीय एजेंट

जब एजेंट के पास पहले से स्थानीय टर्मिनल हो, तब यह सबसे सरल तरीका है। **MultiRoute पहले से चलना चाहिए।** ये उदाहरण सांकेतिक स्रोत URL इस्तेमाल करते हैं; उसे और आउटपुट फ़ोल्डर को बदलें। जॉब बनाना वास्तविक डाउनलोड को कतार में डालता है।

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'MultiRoute automation_api आवश्यक है' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # expected_sha256 केवल तब जोड़ें जब प्रकाशक का विश्वसनीय 64-अक्षर वाला हेक्स हैश उपलब्ध हो।
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# $job.id सहेजें। queued/running/retrying/verifying स्थिति में होने तक इसे फिर से पूछें:
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / स्थानीय shell

निम्न JSON को UTF-8 में `download.json` के रूप में सहेजें। `YOUR_USER` और स्रोत URL बदलें; विश्वसनीय हैश उपलब्ध हो तो उसी ऑब्जेक्ट में `expected_sha256` जोड़ें।

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
# features.automation_api का मान true होने की पुष्टि करने के बाद:
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# उदाहरण वाले id को प्रतिक्रिया से मिले id से बदलें:
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` का अर्थ है कि जॉब बन गया है, फ़ाइल तैयार होना नहीं। `complete` तक उसकी स्थिति पढ़ते रहें, या रुकी/विफल स्थिति की रिपोर्ट करें। पूरा होने पर `checksum_checking`, `integrity_status` और उपलब्ध हैश जाँचें। समान पुनःप्रयास के लिए वही idempotency कुंजी इस्तेमाल करें; जानबूझकर नए अनुरोध के लिए नई कुंजी लें। पोलिंग संक्षिप्त और उचित अंतराल पर रखें; बड़ी फ़ाइलें लंबे समय तक सक्रिय रह सकती हैं। पैकेज मैनेजर और Git अपने मूल निर्भरता/क्लोन कार्यप्रवाह बनाए रखते हैं।

## 4. वैकल्पिक: संगत स्थानीय क्लाइंट के लिए MCP टूल

MultiRoute में **stdio MCP अडैप्टर** शामिल है, जो Java उपप्रक्रिया के रूप में शुरू होता है। यह चल रहे ऐप को ऑपरेशन भेजता है। कोई अंतर्निहित HTTP `/mcp` एंडपॉइंट नहीं है। इसके वर्तमान `download_add` को स्पष्ट **गेटवे IPv4 पते** चाहिए; यह REST `direct` रूट स्वीकार नहीं करता। केवल OS वाले रूट के लिए ऊपर दिया REST इस्तेमाल करें।

इंस्टॉल किए गए सटीक Java/JAR पथ **सेटिंग → MCP और CLI…** में देखें। मानक Windows इंस्टॉलेशन में `%LOCALAPPDATA%/MultiRoute Downloader App` के अंतर्गत `runtime/bin/java.exe` और `app/MultiRoute-Downloader.jar` होते हैं। macOS पर `/Applications/MultiRoute Downloader.app` के अंदर वे `Contents/runtime/Contents/Home/bin/java` और `Contents/app/MultiRoute-Downloader.jar` हैं। स्पेस वाले पथ उद्धरण चिह्नों में रखें। इन मूल प्लेटफ़ॉर्म पैकेजों के लिए अलग Java इंस्टॉलेशन आवश्यक नहीं है।

stdio क्लाइंट प्रविष्टि का उदाहरण; दोनों पूर्ण पथ बदलें। JSON मान `%LOCALAPPDATA%`, `$HOME` या shell चर को अपने आप विस्तारित **नहीं** करते:

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

Claude Code के लिए [दस्तावेज़ में दी गई प्रोजेक्ट कॉन्फ़िगरेशन](https://code.claude.com/docs/en/mcp) के अनुसार इस प्रविष्टि को `.mcp.json` में मिलाएँ। Codex के लिए उसके TOML कॉन्फ़िगरेशन में `[mcp_servers.multiroute]` के अंतर्गत वही कमांड और तर्क सूची सेट करें, या `codex mcp add` इस्तेमाल करें; [आधिकारिक MCP सेटअप](https://developers.openai.com/codex/mcp/) देखें। अन्य होस्ट अपनी MCP सेटिंग इस्तेमाल करते हैं। यह कॉन्फ़िगरेशन का उदाहरण है, इंस्टॉल किया हुआ कनेक्शन नहीं।

क्लाइंट द्वारा टूल खोजने के बाद उससे MultiRoute टूल की सूची माँगें। फिर उसके `download_add` के तर्क इस प्रकार हो सकते हैं:

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

अगली `download_status` कॉल `{"id":"012345abcdef"}` लेती है। उपलब्ध टूल `download_add`, `download_batch`, `download_list`, `download_status`, `download_pause`, `download_resume` और `download_verify` हैं। सभी अडैप्टर ऑपरेशन के लिए `automation_api` आवश्यक है; `download_verify` पूरी हुई फ़ाइल दोबारा जाँचता है। संक्षिप्त MCP परिणामों में स्रोत URL, क्रेडेंशियल और गंतव्य फ़ोल्डर नहीं होते, इसलिए अनुरोध का फ़ोल्डर सहेजें। `gateways: []` स्वचालित रूटिंग नहीं है।

## 5. क्लाउड चैट और दूरस्थ सैंडबॉक्स

**केवल प्रोजेक्ट निर्देश ChatGPT या Claude web को आपके स्थानीय ऐप से नहीं जोड़ सकता।** क्लाउड टूल में `127.0.0.1` उस टूल के परिवेश को दर्शाता है। ChatGPT के दूरस्थ MCP कनेक्शन के लिए संगत दूरस्थ सेवा या समर्थित सुरक्षित ब्रिज आवश्यक है; MultiRoute का REST URL ऐसा MCP एंडपॉइंट नहीं है। इस रिलीज़ में वह दूरस्थ ब्रिज नहीं मिलता। स्थानीय Claude Code, Codex या अन्य स्थानीय निष्पादक द्वारा इस्तेमाल किया गया क्लाउड-होस्टेड मॉडल ऊपर के स्थानीय तरीके इस्तेमाल कर सकता है। [OpenAI कनेक्शन आवश्यकताएँ](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt)।

फ़ाइलें MultiRoute कंप्यूटर पर सहेजी जाती हैं। दूरस्थ बिल्ड को उनका इस्तेमाल करने से पहले अलग अनुमोदित फ़ाइल स्थानांतरण/साझा स्टोरेज चरण चाहिए। क्रेडेंशियल कॉन्फ़िगर की गई साइट प्रोफ़ाइल में होने चाहिए, निर्देश ब्लॉक या चैट में नहीं।

## एजेंट की अपेक्षित रिपोर्ट

“MultiRoute से `model.gguf` कॉन्फ़िगर किए assets फ़ोल्डर में डाउनलोड किया। जॉब: `…`। स्थिति: complete। SHA-256: …; प्रकाशक के संदर्भ से मिलान हुआ।” यदि कोई विश्वसनीय संदर्भ दिया या मिला नहीं, तो रिपोर्ट करें “स्थानीय SHA-256 की गणना हुई; प्रकाशक सत्यापन उपलब्ध नहीं है।” यदि ऐप अनुपलब्ध हो या `pro_required` लौटाए, तो इसकी रिपोर्ट करें और चुपचाप डाउनलोडर बदलने के बजाय वैकल्पिक तरीका तय करें।
