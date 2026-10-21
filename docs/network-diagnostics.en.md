# Network validation and diagnostic logs

MultiRoute checks the actual IPv4 prefix of the selected LAN interface. The computer's address, the application's virtual IP and every configured gateway must be usable addresses in one subnet. The virtual IP and gateways must differ from all IPv4 addresses assigned to the computer. Network and broadcast addresses are checked using the real mask, including networks such as /16, /23 and /25.

While its network component is running, MultiRoute checks the interface configuration again approximately every two seconds. If the computer's IPv4 changes within the same subnet on the same interface, MultiRoute keeps the virtual IP, private TCP/IP stack and existing proxy ports. The application does not set the computer's IP or change system routes.

A different subnet, changed mask, unavailable interface or an address conflict stops the private transport and requires a new network check. Downloads retain their verified segments. MultiRoute does not silently switch the transfer to the system gateway. This protects the selection of gateways; it is not a claim that every router has working Internet access.

The desktop network settings show the observed computer IP, subnet, virtual IP and number of gateways. `GET /api/v1/network` exposes the same evidence in `network_validation` with `pending`, `valid` or `error` status. Reading this endpoint does not start the network component. `POST /api/v1/routes/inspect` with `{"gateway":"192.168.1.1"}` checks that configured gateway and starts the component if needed. This is an ARP/network check, not a speed test.

## Errors, retries and 502

`logs/network-errors.log` is an append-only UTF-8 journal under the application's data directory. The default Windows location is `%LOCALAPPDATA%\MultiRoute Resume\data\logs` for a reused older profile, or `%LOCALAPPDATA%\MultiRoute Downloader\data\logs` for a new profile. The actual data directory is returned by `GET /api/v1/instance`.

The journal records timestamps, selected gateway, destination server, DNS-resolved IPv4 when available, failure stage, elapsed time and native error code. Download retries also retain job ID, segment and byte range. A later successful retry does not erase the earlier failure. The journal rotates at 4 MiB and keeps three archives; it survives the bounded on-screen history and application restarts. Existing records keep the language used when they were written, while the on-screen history can be translated again.

A local CONNECT 502 means the application's gateway component could not establish the tunnel. The new journal distinguishes parsing, ARP, DNS, socket creation, binding and TCP connection failures. It does not reconstruct details discarded by an older build. Signed URL paths/queries, credentials, request headers and proxy response bodies are excluded from the new diagnostic records.

Design references: [OWASP logging guidance](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html), [IPv4 address conflict detection, RFC 5227](https://www.rfc-editor.org/info/rfc5227/), [Hugging Face storage/CDN download hosts](https://huggingface.co/docs/hub/models-downloading).
