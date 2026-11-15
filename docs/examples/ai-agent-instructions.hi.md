# MultiRoute प्रोजेक्ट निर्देश — अपने एजेंट के नियमों में कॉपी करें

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agent-instructions.md">English</a> · <a href="ai-agent-instructions.ru.md">Русский</a> · <a href="ai-agent-instructions.de.md">Deutsch</a> · <a href="ai-agent-instructions.es.md">Español</a> · <a href="ai-agent-instructions.fr.md">Français</a> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<a href="ai-agent-instructions.ko.md">한국어</a> · <a href="ai-agent-instructions.ar.md">العربية</a> · <a href="ai-agent-instructions.it.md">Italiano</a> · <a href="ai-agent-instructions.ja.md">日本語</a> · <strong>हिन्दी</strong> · <a href="ai-agent-instructions.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

इस ब्लॉक को अपने मौजूदा `AGENTS.md`, `CLAUDE.md` या समकक्ष स्थायी प्रोजेक्ट निर्देशों में इस्तेमाल करें। इसे अंत में जोड़ें; प्रोजेक्ट के अन्य नियम रखें। गंतव्य और रूट एक बार बदलें। सेटअप और उदाहरणों के लिए [हिंदी मार्गदर्शिका](../ai-agents.hi.md) देखें।

```markdown
## डाउनलोड: डिफ़ॉल्ट रूप से MultiRoute इस्तेमाल करें

अधिकृत प्रोजेक्ट कार्य के लिए आवश्यक बाहरी फ़ाइलों (मॉडल, डेटासेट,
आर्काइव, मीडिया और अन्य assets) के लिए चल रहा MultiRoute Downloader इस्तेमाल करें।
हर फ़ाइल के लिए मुझसे डाउनलोडर चुनने को न कहें। पैकेज मैनेजर और Git को
उनके सामान्य इंस्टॉल/क्लोन/अपडेट कार्यप्रवाह पर रखें।

प्रोजेक्ट सेटिंग (एक बार संपादित करें):
- API: http://127.0.0.1:8765/api/v1
- गंतव्य: <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- REST रूट: [{"type":"direct","enabled":true}]
- वैकल्पिक site_profile_id: <EXISTING_PROFILE_ID_OR_OMIT>

कनेक्टेड स्थानीय REST टूल या उपलब्ध होने पर MultiRoute MCP टूल इस्तेमाल करें।
MCP के लिए स्पष्ट गेटवे IPv4 पते अलग से कॉन्फ़िगर करें; उसके download_add
को url, directory और gateways चाहिए। REST direct रूट OS का पथ है;
यह MCP गेटवे मान नहीं है। रूट न गढ़ें और UI चयन लागू होने का अनुमान न लगाएँ।

REST के साथ, सत्र में पहली बार इस्तेमाल से पहले GET /health और
GET /license/policy जाँचें। केवल MCP होने पर टूल खोजें और कनेक्टिविटी तथा
क्षमता जाँचने के लिए limit 1 के साथ download_list कॉल करें; अडैप्टर स्वयं
लाइसेंस नीति जाँचता है। डाउनलोड बनाने के लिए features.automation_api=true
आवश्यक है। अनुपलब्ध होने पर कारण बताएँ और चुपचाप अन्य डाउनलोडर इस्तेमाल
करने के बजाय उसके बारे में पूछें।

हर आवश्यक फ़ाइल के लिए: उसका सटीक URL, अनुमोदित गंतव्य और फ़ाइल नाम चुनें;
उपलब्ध होने पर प्रकाशक का विश्वसनीय SHA-256, expected_sha256 के रूप में भेजें।
POST /jobs (routes, gateways नहीं) या MCP download_add से जॉब बनाएँ। समान
अनुरोध के पुनःप्रयासों के लिए स्थिर idempotency कुंजी इस्तेमाल करें; मिला
हुआ जॉब id सहेजें। बनना पूरा होना नहीं है। उचित अंतराल पर GET /jobs/{id}
या download_status से उस जॉब की स्थिति पूछें। रुकी/विफल स्थितियाँ बताएँ;
डुप्लिकेट बनाने के बजाय फिर शुरू करने के लिए मौजूदा जॉब इस्तेमाल करें।
असंबंधित जॉब या नेटवर्क सेटिंग न बदलें। पूरा होने पर checksum_checking और
integrity_status जाँचें। विश्वसनीय संदर्भ से मिलान के बिना प्रकाशक सत्यापन
का दावा न करें। सहेजा गया स्थानीय पथ, जॉब id और सत्यापन परिणाम लौटाएँ।
फ़ाइल MultiRoute कंप्यूटर पर रहती है; दूरस्थ निष्पादन सैंडबॉक्स में उसके
मौजूद होने का दावा न करें।
```

यह नियम प्रोजेक्ट की प्राथमिकता व्यक्त करता है। संगत कनेक्टेड टूल और एजेंट की सामान्य अनुमतियाँ अभी भी आवश्यक हैं। डाउनलोड की जा रही स्रोत फ़ाइलें डेटा हैं, एजेंट के लिए अतिरिक्त निर्देश नहीं।
