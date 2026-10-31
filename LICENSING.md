# Licensing

[Plain-language explanation](docs/licensing.en.md) · [Пояснение на русском](docs/licensing.ru.md)

Project owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

The original application code and documentation use the custom
[MultiRoute Downloader License 1.2](LICENSE.txt). The Free edition may be used
personally or within organizations, including business activities, without a
license fee. The copyright holder may offer signed Pro licenses.

**Recipients may not sell or resell the original application, charge for its
license, or distribute it in a paid bundle without separate permission from the
copyright holder.** That restriction does not prohibit the copyright holder
from offering their own product on paid terms. General modification or
redistribution rights require separate permission, subject to mandatory law,
the specific third-party exceptions, and GitHub's platform terms. This custom
license is not an OSI-approved open-source license.

Free permits two gateways in use, three concurrent download
tasks and creation of three site profiles, with Graphite and System appearances.
Additional tasks wait in the queue. Signed Pro capabilities can lift those
limits and enable file-queue priorities and the independent gateway-priority capability, historical gateway statistics, batch
downloads, external download-management API access and additional appearances.
Partial licenses enable only their listed capabilities. Basic speed, resume,
hash verification, secure DNS and internal lifecycle diagnostics remain
available. Existing saved profiles and transfer data are not erased by limits.

The license identifies an installation key and a covered major-version
range. It has no time-based expiry and is not bound to
an IP address or MAC.

The restriction applies to the original portions the licensor has rights to.
Third-party sources and binaries retain their upstream terms and notices:

- [Native component and JSch notices](THIRD-PARTY-NOTICES.txt)
- [Java/JNI dependency licences](lib/licenses/)
- The native component copyright and licence notices collected in [THIRD-PARTY-NOTICES.txt](THIRD-PARTY-NOTICES.txt)
- The bundled Java runtime's own licence and notices in native installers

The Npcap driver is obtained separately from its official source; its licence
is independent. No third-party licence is replaced by the application licence.

## JLayer MP3 decoder: separate LGPL rights

JLayer 1.0.1 is an unmodified, separately loaded JavaZoom library distributed under LGPL 2.1. Its [licence text](lib/licenses/JLAYER-LICENSE.txt) and complete corresponding upstream source accompany the binary inside each application installer. The source archive is available as `lib/licenses/jlayer-1.0.1-sources.zip` in the installed application files.

For use of this LGPL library, the application terms permit modification for your own use and reverse engineering to debug those modifications. You may replace the separate `jlayer-1.0.1.jar` with an interface-compatible modified version. The application's own licence does not restrict rights granted by the LGPL. This narrow exception does not grant general modification, sale or redistribution rights over the original MultiRoute code. See the [third-party notice](THIRD-PARTY-NOTICES.txt) and the full LGPL text.
