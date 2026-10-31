# macOS: स्थापना, पहला आरंभ और अनुमतियाँ

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · **हिन्दी** · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

## स्थापित करें और खोलें

अपडेट से पहले स्थानांतरण पूरे होने दें और मेनू बार के MultiRoute मेनू में **Quit/Exit** चुनें। लाल बंद बटन केवल विंडो छिपा सकता है। [डाउनलोड](../../downloads/README.md) से Apple Silicon या Intel का सही PKG लें, SHA-256 जाँचकर खोलें। macOS प्रशासक सिस्टम संवाद में स्थापना स्वीकार करें। Finder या Spotlight से `/Applications/MultiRoute Downloader.app` खोलें; अलग Java या Terminal आवश्यक नहीं है।

## खोलने की पुष्टि

डेवलपर सत्यापित न हो तो पहले PKG या ऐप खोलने का प्रयास करें। केवल जाँची हुई विश्वसनीय फ़ाइल के लिए **System Settings → Privacy & Security → Open Anyway → Open** चुनें; स्थानीय नाम अलग हो सकते हैं। मैलवेयर या क्षतिग्रस्त फ़ाइल की चेतावनी पर रुककर फ़ाइल जाँचें। [Apple निर्देश](https://support.apple.com/en-us/102445)।

## नेटवर्क और पासवर्ड

ऐप पहली बार शुरू करने या वर्चुअल IP, DHCP अथवा ऐप MAC लागू करते समय Ethernet BPF खोलने के लिए प्रशासक पुष्टि माँगी जा सकती है। सेटिंग बदलने या घटक फिर शुरू होने पर दोबारा पूछ सकता है। सामान्य जाँच और गति परीक्षण तैयार घटक का उपयोग करते हैं; हर फ़ाइल या गेटवे पर पासवर्ड नहीं चाहिए। OS रूट और भौतिक MAC नहीं बदलते।

**Local Network** अनुरोध आने पर Privacy & Security → Local Network में पहचानी हुई MultiRoute प्रविष्टि अनुमति दें। हर स्थापना में अनुरोध नहीं आता। [Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy)।

Mac प्रशासक पासवर्ड केवल सिस्टम संवाद में दें, लाइसेंस या Hugging Face फ़ील्ड में नहीं। अनुरोधकर्ता `DesktopLauncher`/Java दिख सकता है: केवल अपनी अपेक्षित विश्वसनीय MultiRoute क्रिया स्वीकार करें। रद्द करने के बाद स्पष्ट रूप से जाँच फिर चलाएँ और लॉग पढ़ें। Gatekeeper/SIP वैश्विक रूप से बंद न करें।

[MultiRoute Downloader — हिन्दी](../hi.md) · [macOS — English / 12 languages](README.md)
