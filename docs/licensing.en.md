# What the licenses limit

[Русский](licensing.ru.md) · [Application license](../LICENSE.txt) · [Full component notices](../THIRD-PARTY-NOTICES.txt)

Three different subjects apply: the author's contract for MultiRoute, technical Free/Pro capabilities, and upstream rights to distribute third-party components.

| Action | Meaning |
| --- | --- |
| Use Free personally or in business | Allowed by the original application's License 1.2, within its Free feature limits. Npcap and other components still have independent conditions. |
| The copyright holder sells their own MultiRoute or issues paid Pro licenses | The holder may offer those terms. A third-party distribution audit still has to be completed for the actual package; selling is not automatically a GPL/LGPL violation. |
| Another recipient sells/resells the original app, its license, or a paid bundle | The application license requires separate permission from its copyright holder. This restriction does not replace upstream open-source rights. |

## Technical limits

Free permits two gateways in use, three concurrent download tasks and creation of three site profiles. Extra tasks wait; older saved data is retained. Nine independent signed rights are listed in the [current feature table](en.md#current-free-pro). The independent `gateway_priorities` right controls gateway allocation and is not included in the file-queue `download_priorities` right. The edition label alone does not grant unlisted capabilities. Speed, supported resume, hash verification, secure DNS, live meters and Graphite/System remain available in Free.

The signed file confirms issuance by the publisher for a particular installation key, major range and rights. A holder label is not proof of personal identity. There is no expiry clock. A different installation may need a new request. Technical activation does not grant copyright ownership or resale rights, and local feature checks are not a claim that binaries cannot be modified.

## Component formalities

The separate in-app **Third-party components and licenses** page inventories the actual packaged platform/runtime. It provides names, versions, publishers/copyright, full original legal texts and referenced source-material files/hashes.

OpenJDK uses GPLv2 with the Classpath Exception; the applicable notices and paired corresponding source/build materials accompany the installer. JNA is distributed under its Apache 2.0 option with native notices. Unmodified JLayer remains a separate replaceable LGPL 2.1 JAR with its corresponding source and library-debugging/modification exception in [License§5a](../LICENSE.txt). Other permissive components retain their own notices. Source material for dependencies is distinct from the original MultiRoute implementation; this public distribution tree does not publish the original application C/Java.

For the macOS native components, unmodified public Darwin headers have APSL 2.0 and original BSD/CMU/OSF/WIDE notices, Clang headers have Apache 2.0 with LLVM exception, and Zig compilation materials have MIT terms. They are not all MIT. The installer includes the exact third-party header archive and source-availability notice; see [full Darwin notices](../NATIVE-DARWIN-NOTICES.txt). [APSL§2.1/2.3/4](https://opensource.apple.com/apsl/) addresses unchanged code, source availability for object distribution and larger works. Those conditions apply to the covered third-party material; they are not a blanket grant over the rest of the app.

The English [License 1.2](../LICENSE.txt) controls copies supplied under it; translated explanations do not add rights.
