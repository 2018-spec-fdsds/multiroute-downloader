# MultiRoute Downloader 4.2.0 — Hướng dẫn tiếng Việt

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md) · [Home](../README.md)

<a name="dns-settings"></a>

## Thiết lập DNS trong 4.2.0

**Hướng dẫn cho phiên bản 4.2.0. Đối chiếu phiên bản bộ cài và tổng kiểm tra đã công bố; xem NATIVE-INSTALLER-TESTS.json tương ứng để biết kết quả xác minh nền tảng và gói.**

Mở **Mạng → Phân giải DNS**. Mặc định là **DNS qua HTTPS (DoH), Cloudflare**. Thiết lập chỉ thuộc MultiRoute, không đổi DNS, bảng định tuyến hoặc IP giao diện của hệ điều hành.

DoH hỗ trợ HTTP/2 có giới hạn khi TLS ALPN chọn và HTTP/1.1 khi giao thức đó được thương lượng. Tương thích nhà cung cấp và kết quả cổng thực tế nằm trong báo cáo phát hành cuối cùng tương ứng.

- **DoH:** DNS mã hóa qua HTTPS trên từng cổng mạng đã chọn; kiểm tra chứng chỉ TLS theo tên máy trong URL.
- **DoT:** DNS mã hóa qua TLS trên từng cổng mạng, mặc định cổng 853; kiểm tra tên máy chủ trên chứng chỉ.
- **DNS hệ thống (máy tính):** chủ động dùng chính sách DNS của máy, OS hoặc VPN. DNS không được bảo đảm đi theo từng cổng đã chọn, nhưng kết nối tệp vẫn dùng cổng của mình. Chế độ này không tự bảo đảm mã hóa DNS.

Chọn Cloudflare, Quad9, Google hoặc **Tùy chỉnh**. Giá trị mẫu là cố định; Tùy chỉnh cho sửa URL/tên máy chủ, cổng và IPv4 công cộng dạng số để kết nối ban đầu. Địa chỉ số tránh phải phân giải tên nhà cung cấp qua OS trước. DoH/DoT xác minh chuỗi tin cậy, tên máy và SNI; không âm thầm chuyển sang DNS rõ hoặc DNS hệ thống. Lỗi DNS bảo mật làm lần thử đó thất bại. Nhà cung cấp phải truy cập được qua cổng đã chọn.

Sửa bản nháp, chọn cổng mạng rồi nhấn **Kiểm tra DNS**. Kiểm tra không lưu, hiển thị kết quả, thời gian và IPv4 công cộng hoặc lỗi cho từng cổng. Lỗi một phần vẫn hiện rõ. Thành công ở chế độ Hệ thống chỉ kiểm tra phân giải OS, không chứng minh WAN độc lập của từng router. **Áp dụng thiết lập DNS** kiểm tra và lưu; số phiên bản trả về xác định cấu hình đã áp dụng. Cập nhật trạng thái và đổi ngôn ngữ giữ bản nháp chưa lưu, con trỏ và vị trí cuộn.

Mỗi truy vấn có tổng thời hạn 8 giây. Giữ ý nghĩa TTL bằng 0 và khấu trừ HTTP Age của DoH; bộ đệm gốc tối đa 300 giây, tách theo cổng/phiên bản cấu hình. Java không thêm bộ đệm DNS khác. Chế độ Hệ thống theo chính sách bộ đệm OS/JVM. Hỗ trợ câu trả lời IPv4 công cộng, không xác thực DNSSEC cục bộ hoặc phân giải IPv6.

API: `GET /api/v1/network/dns` đọc cấu hình và trạng thái chỉ đọc; `PUT`/`POST` cùng đường dẫn áp dụng cấu hình. `POST /api/v1/network/dns/test` kiểm tra bản nháp; `GET /api/v1/network/dns/providers` liệt kê mẫu. Chỉ gửi trường cấu hình, không gửi phiên bản/trạng thái từ GET. [API](../API.ru.txt) · [OpenAPI](../openapi.yaml).

**Bản sửa lỗi 4.1.3:** nếu CDN như Civitai trả HTTP 200 cho vùng byte bắt đầu từ 0, ứng dụng kiểm tra thêm vùng bắt đầu sau 0. Đoạn đầu chỉ được đọc trong giới hạn của đoạn; dữ liệu tải tuần tự đã lưu và khớp nguồn có thể tiếp tục theo từng đoạn. HTTP 200 tạm thời cho vùng phía sau sẽ được thử lại sau 2–30 giây, giữ dữ liệu và luồng xử lý cổng mạng. Nhật ký truyền bền vững theo cổng phân biệt lưu lượng nhận với tiến độ đã lưu. [Ghi chú và giới hạn kiểm tra](releases/4.1.3.en.md) · [Chẩn đoán truyền](transfer-diagnostics.en.md).

[Bộ cài 4.2.0](https://github.com/2018-spec-fdsds/multiroute-downloader/tree/main/downloads/v4.2.0) kèm Java 21 riêng. Trình cài Windows dùng tiếng Anh và tạo lối tắt trên màn hình; vẫn cần cài Npcap riêng. macOS dùng libpcap của hệ thống. Làm theo các bước cài đặt và xác nhận bảo mật bên dưới.

Chủ dự án: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## 1. Mục đích và cách hoạt động

MultiRoute Downloader là ứng dụng Java trên máy tính để tải tệp qua nhiều **cổng Internet** trong cùng mạng Ethernet nội bộ. Ví dụ, có thể kết hợp đường truyền cố định và bộ định tuyến di động 4G/5G. Các cổng được chọn lấy những đoạn tệp từ hàng đợi chung; cổng nhanh hơn có thể tải nhiều đoạn hơn. Khi một kết nối mất, các cổng khác tiếp tục, đoạn chưa xong trở về hàng đợi và dữ liệu đã xác minh được giữ lại. Có thể nhập danh sách liên kết hoặc dùng API cục bộ từ ứng dụng khác để tạo nhóm tải.

Mức tăng tốc phụ thuộc băng thông độc lập của từng đường truyền, giới hạn máy chủ và khả năng tiếp tục an toàn của giao thức; không bảo đảm tăng theo số bộ định tuyến. Đây là ứng dụng desktop độc lập, không phải giao diện web. Hồ sơ mới mặc định tiếng Anh; hỗ trợ tiếng Anh, Nga, Đức, Tây Ban Nha, Pháp, Trung, Hàn, Ả Rập, Ý, Nhật, Hindi và Việt. Lựa chọn được lưu và áp dụng cho thông báo đã đăng ký, trạng thái, nhật ký, chú giải. Thông báo thô từ máy chủ/thư viện ngoài có thể giữ nguyên ngôn ngữ. Tên tệp, đường dẫn, URL và khóa JSON không bị dịch.

## 2. Môi trường cần thiết

| Nền tảng | Hệ điều hành và CPU | Thành phần |
| --- | --- | --- |
| Windows | Windows 11 x64; chưa có gói ARM64 | Bộ cài gốc có Java 21 và thành phần mạng x64; Npcap cài riêng |
| Mac Apple Silicon | M1/M2/M3/M4 trở lên, macOS 26+ | PKG arm64 có Java và thành phần mạng đã biên dịch |
| Mac Intel | x86_64, macOS 15+ | PKG x86_64 có Java và thành phần đã biên dịch |
| Linux | Bản phân phối glibc, x86_64 hoặc ARM64; torrent/magnet cần glibc 2.38+ | Java 17+ đầy đủ, sudo, trình biên dịch C, binutils, pkg-config, tệp phát triển libpcap; GUI cần X11 hoặc Wayland+XWayland và thư viện phông chữ |

Java phải có `java.desktop`, `java.net.http`, `jdk.httpserver`, `java.xml`, kể cả `--headless`. Bộ cài gốc Windows/macOS không cần Java riêng. ZIP portable 3.0 cũ và triển khai Linux có thể lấy Java riêng từ nguồn chính thức khi cần. Không ghép các tệp lẻ của nhiều phiên bản.

Cần bộ điều hợp **Ethernet có dây** trong cùng mạng con IPv4 với mọi cổng đã chọn. Mỗi router phải cho địa chỉ ảo của ứng dụng truy cập Internet. Chỉ Wi-Fi, chỉ VPN hoặc chỉ IPv6 không phải cấu hình được hỗ trợ cho cơ chế này. Khuyến nghị 4 GB RAM để dùng thoải mái, không phải mức tối thiểu đã đo hay bị ép buộc. Không cần GPU, CUDA hay mô hình AI. Thư mục ứng dụng/dữ liệu/tải xuống phải ghi được. Triển khai kiểm tra tối thiểu 512 MiB trống, ngoài dung lượng tệp, đoạn và dữ liệu tạm. Torrent cần dự trù toàn bộ mảnh, ranh giới tệp, đích và bộ nhớ đệm.

Hệ thống cần phông có các ký tự của ngôn ngữ đã chọn. Trên Linux tối giản, cài gói phù hợp từ bản phân phối, ví dụ Noto CJK cho Nhật/Trung/Hàn và họ Noto cho Ả Rập hoặc Devanagari. Java dùng cơ chế phông thay thế của nền tảng; ứng dụng không cài hay đóng gói bộ phông. Khả năng hiển thị có thể khác giữa các máy.

## 3. Cài đặt và xác nhận bảo mật

Chọn một bộ cài trong [bản phát hành 4.2.0](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.2.0), nếu được đính kèm, và đối chiếu SHA-256 công bố.

- Windows: [MultiRoute-Downloader-4.2.0-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/MultiRoute-Downloader-4.2.0-Windows-x64-Setup.exe).
- Apple Silicon, macOS 26+: [MultiRoute-Downloader-4.2.0-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/MultiRoute-Downloader-4.2.0-macOS-arm64.pkg).
- Intel, macOS 15+: [MultiRoute-Downloader-4.2.0-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/MultiRoute-Downloader-4.2.0-macOS-x86_64.pkg).

### Windows

Mở EXE, cài cho người dùng hiện tại rồi chạy từ menu Start hoặc biểu tượng desktop. Ứng dụng không chạy như cửa sổ lệnh script. Văn bản trình cài của chúng tôi bằng tiếng Anh; hộp bảo mật theo ngôn ngữ Windows.

Lần đầu kiểm tra Npcap. Nếu thiếu, đồng ý tải trong ứng dụng: SHA-256 và nhà phát hành Authenticode của bản cài chính thức cố định được kiểm tra trước khi mở trình cài Npcap bình thường. Đọc giấy phép và chấp thuận UAC tiêu chuẩn. Khởi động lại Windows nếu được yêu cầu. Npcap không đi kèm và không cài âm thầm; bước này cần Internet nếu chưa có. Bản AdminOnly có thể cần thêm quyền bắt gói, nhưng vẫn chạy Java bằng người dùng thường.

Ứng dụng/bộ cài chưa ký nhà phát hành. Nếu SmartScreen báo không rõ tác giả, xác minh nguồn và mã băm trước khi quyết định dùng “More info → Run anyway” khi có. Phát hiện mã độc là trường hợp khác; không tắt antivirus hoặc tạo ngoại lệ để cài.

### macOS

Mở PKG đúng CPU trong Apple Installer và chấp thuận quyền quản trị để đặt thành phần mạng được bảo vệ. Sau đó mở `/Applications/MultiRoute Downloader.app` từ Finder, Spotlight hoặc Dock. Gói này không cần Java riêng, Terminal, Command Line Tools hay Homebrew.

Khi cần mở thiết bị Ethernet BPF, xuất hiện xác nhận quản trị macOS thông thường. Thành phần hạ quyền sau khi mở thiết bị bắt gói; Java giữ tài khoản thường. Gói và ứng dụng **chưa ký Developer ID hoặc notarized**. Nếu hệ thống cho ngoại lệ với nhà phát triển không xác minh được, kiểm tra nguồn và SHA-256 rồi theo [hướng dẫn Apple](https://support.apple.com/en-us/102445): System Settings → Privacy & Security → Open Anyway, sau đó Open. Máy được quản lý có thể cấm ngoại lệ.

Xác nhận cài đặt, quyền mạng về sau và ngoại lệ tác giả là các bước riêng. Khi được hỏi, cho truy cập mạng cục bộ và đúng thư mục tải đã chọn. Không tắt Gatekeeper/SIP, không xóa quarantine toàn hệ thống và không chạy toàn bộ Java bằng root. Cảnh báo mã độc/tệp hỏng không phải cảnh báo tác giả thông thường; dừng và kiểm tra tệp.

### Linux và ZIP portable 3.0

Giải nén ZIP đầy đủ. Linux chạy đầu tiên `bash ./Deploy-Linux.sh`, sau đó `bash ./Start-Linux.sh`; Windows portable dùng `Deploy-Windows.cmd` rồi `Start-Windows.cmd`; macOS portable dùng `Deploy-macOS.command` và `Start-macOS.command`. Các bản cũ giữ cách chạy script, khác bộ cài gốc 4.0. Linux cần gói compiler/libpcap của bản phân phối; macOS portable có thể yêu cầu Apple Command Line Tools và sudo. Giữ phiên Terminal cần thiết, không chạy tất cả Java dưới root. Không bao gồm Alpine/musl. Triển khai không nâng cấp OS hoặc thay glibc.

Có đủ 12 hướng dẫn ngoại tuyến. Windows: `%LOCALAPPDATA%/MultiRoute Downloader App/app/docs/`. macOS chọn Show Package Contents, mở `Contents/app/docs/vi.md`. Liên kết trợ giúp theo phiên bản trong trình duyệt cần Internet.

## 4. Cổng kết nối và IP ảo

Bốn cổng mặc định là `192.168.1.1`, `192.168.1.2`, `192.168.1.3`, `192.168.1.4`, tất cả được chọn. Có thể sửa và thêm đến **1–32** địa chỉ khác nhau. Việc máy tính đã dùng một cổng làm mặc định không tạo ra đường truyền thứ năm.

IPv4 ảo ban đầu là `192.168.1.70`. Kiểm tra/áp dụng trong cài đặt mạng, chắc rằng thiết bị khác không dùng và dành ngoài dải DHCP tự động. Địa chỉ dành riêng cho truyền tải nội bộ ứng dụng: **không thêm IP vào adapter hệ điều hành và không đổi bảng định tuyến hệ thống**. Chương trình khác giữ kết nối thông thường. Không tự thêm IP này trong cài đặt mạng OS. Xem dấu hỏi và chú giải bên cạnh.

Kiểm tra rồi chọn một, nhiều hoặc tất cả cổng. Thử tốc độ giới hạn **16 MiB hoặc 5 giây**, có thể thêm thời gian chuẩn bị. Đó chỉ là giới hạn thử, không giới hạn tải thực. Nếu thiếu URL HTTP trực tiếp thích hợp thì dùng tệp thử Selectel.

Có thể thêm/xóa hồ sơ cổng và đổi các cổng của công việc đang tải, miễn mỗi việc chưa xong còn ít nhất một cổng. Cổng mới không tự được chọn cho việc cũ. Đổi IP ảo cần tạm dừng việc đang hoạt động. Địa chỉ MAC được ẩn khỏi giao diện.

## 5. Quản lý tải và giao diện

1. Dán vào ô URL dài bằng Ctrl+V, Cmd+V trên Mac hoặc nút dán. Có thể dán magnet hay mở `.torrent`.
2. Chọn nơi lưu bằng hộp thư mục OS. Mặc định Windows `%USERPROFILE%\Downloads`, macOS/Linux `~/Downloads`; lựa chọn thủ công được giữ lại.
3. Chọn cổng và nhấn bắt đầu cạnh URL. Hugging Face, WebDAV, Metalink có xem trước/chọn tệp. Nhập hàng loạt nhận một link mỗi dòng hoặc tệp văn bản.
4. Xem tiến độ riêng, thời gian qua/còn lại, tổng tốc độ và tốc độ/dung lượng mỗi cổng. Đoạn HTTP mặc định **20 MiB**, chỉnh 1–64 MiB. Không có dữ liệu trong **60 giây** thì đoạn bị hủy và trả về nhóm chung cho cổng khác. Kích thước mảnh torrent do metadata quy định.
5. Hỗ trợ tạm dừng/tiếp tục, ưu tiên, giới hạn, lịch và đổi thư mục việc đã dừng. Mặc định hai việc hoạt động. Sau khởi động lại, việc đang chạy trở về tạm dừng. Seeding torrent mặc định tắt và có giới hạn thời gian/tỷ lệ.

FlatLaf có giao diện sáng, tối và theo hệ thống, lựa chọn được lưu. Tổng quan đếm mọi việc/đang chạy/hoàn tất và tổng tốc độ không phụ thuộc bộ lọc. Mỗi việc có thanh riêng; các cột đầy chiều rộng, sắp xếp, tìm kiếm, lọc trạng thái và cuộn giữ vị trí vẫn có.

Rê chuột lên dòng đang tải hoặc đã tải để thấy **tên tệp đầy đủ và đường dẫn tuyệt đối**, đúng cả sau sắp xếp/lọc. Tiêu đề tệp được chọn có cùng chú giải; menu chuột phải sao chép toàn bộ đường dẫn. Ctrl+L/Cmd+L vào URL, Ctrl+F/Cmd+F vào tìm kiếm, Escape xóa tìm kiếm.

Việc được chọn có thẻ Chi tiết và Lịch sử. Báo cáo giữ vị trí đọc/phần chọn khi làm mới hay đổi ngôn ngữ; chọn việc khác mở báo cáo từ đầu. Nhật ký chung tách sự kiện ứng dụng và lịch sử tải. Ở cuối sẽ theo sự kiện mới, không nhảy khi đang đọc phần cũ.

Đóng cửa sổ vẫn tiếp tục ở khay hệ thống. Biểu tượng thể hiện tốc độ của tất cả việc đến 10 Gbit/s; chú giải hiển thị chính xác hơn. Mở ứng dụng để khôi phục, Thoát ứng dụng để lưu/kết thúc. Nếu không có khay thì thu nhỏ xuống taskbar. **Thoát từ khay trước khi cập nhật**.

Chọn nhiều hàng tải xuống bằng **Ctrl+nhấp chuột** trên Windows/Linux hoặc **Cmd+nhấp chuột** trên macOS; **Shift+nhấp chuột** chọn một dải. Khi bảng đang có tiêu điểm, **Ctrl+A** hoặc **Cmd+A** chỉ chọn tất cả hàng hiện đang hiển thị theo tìm kiếm và bộ lọc trạng thái. Thao tác xóa áp dụng cho cả nhóm đã chọn với một lần xác nhận. Tùy chọn xóa cả tệp hoàn chỉnh và các phần đã tải **mặc định không được chọn**, nên có thể chỉ bỏ mục khỏi danh sách mà giữ dữ liệu. Chỉ chọn tùy chọn đó khi muốn xóa tệp của các công việc ấy; các tệp khác và thư mục đích vẫn được giữ. Kết quả hiển thị số mục đã xóa. Công việc không xóa được vẫn nằm trong danh sách cùng bản tóm tắt lỗi để kiểm tra rồi thử lại; mục đã xóa thành công không bị xử lý lại. Xóa công việc hoặc tệp không xóa lịch sử tải xuống lâu dài.

## 6. Giao thức và kiểm tra toàn vẹn

| Nguồn | Hành vi/điều kiện |
| --- | --- |
| HTTP/HTTPS | Chia đoạn/tiếp tục nếu định danh an toàn và byte ranges; nếu không, luồng toàn tệp trên một cổng, đứt phải tải lại từ đầu |
| Hugging Face | Chọn tệp/kho và giải quyết Hub/Xet; tài nguyên riêng/hạn chế cần quyền nguồn phù hợp |
| FTP | EPSV/PASV thụ động; FTP thường không mã hóa mật khẩu/dữ liệu |
| FTPS | TLS tường minh cho điều khiển/dữ liệu; không hỗ trợ FTPS ngầm định |
| SFTP | SSH bắt buộc fingerprint SHA256 host-key đã xác minh độc lập và ghim; không tự chấp nhận khóa lạ |
| WebDAV | Danh sách HTTPS và chọn tệp |
| Metalink | `.meta4`, SHA-256 đáng tin, mirror và hash mảnh nếu có |
| Torrent/magnet | Qua cổng đã chọn, bắt buộc kiểm mảnh theo metadata, chọn tệp/ưu tiên |

Trong quyền truy cập nguồn có thể lưu user/password, Bearer token, Cookie hoặc header cho phép theo origin chính xác, được mã hóa cục bộ. Đây không phải token cho router. Không đặt password trong URL. FTP/FTPS/SFTP thiếu SHA-256 hoặc SHA-512 công bố/thủ công đáng tin sẽ dùng toàn tệp.

Sau ghép, một lượt đọc tính **MD5, SHA-256, SHA-512**. Ô chính tự nhận 32/64/128 ký tự thập lục phân và có thể thêm tham chiếu. Tìm digest toàn biểu diễn và `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512`, `SHA512SUMS`, giữ Hugging Face LFS/Metalink. Digest của phản hồi HTTP một phần không phải toàn tệp.

Mọi tham chiếu đã biết phải khớp trước khi xuất bản tệp cuối. Lệch thì thất bại và giữ dữ liệu một phần. Không có tham chiếu chỉ là giá trị tính cục bộ, không xác nhận khớp nhà phát hành. MD5 phát hiện hỏng ngẫu nhiên, không thay thế định danh mạnh hoặc SHA-256/SHA-512. Kiểm mảnh torrent và hash toàn tệp riêng biệt; checksum thủ công toàn tệp yêu cầu chọn đúng một tệp.

<a name="multiroute-api"></a>

## 7. API cho ứng dụng khác

Khi đang chạy, API ở `http://127.0.0.1:8765/api/v1`. Yêu cầu/đáp ứng JSON UTF-8, body dùng `Content-Type: application/json`. Không cần API token để tạo/quản lý việc cục bộ; thông tin nguồn là riêng. Chỉ cùng máy truy cập được; không mở cổng ra LAN/Internet. `--headless` chỉ chạy API. Dùng launcher gốc; ví dụ `.cmd` dành cho portable.

Kiểm tra: `GET /api/v1/health`, bình thường `{"ok":true,"version":"4.2.0"}`. Cấu hình mạng trước trong UI hoặc gửi tới `POST /api/v1/network`:

```json
{
  "virtual_ip": "192.168.1.70",
  "gateways": ["192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4"]
}
```

Gửi JSON này tới `POST /api/v1/jobs`, thay URL và thư mục thực, cũng như cổng nếu cần. Linux: `/home/your-user/Downloads`; Mac: `/Users/your-user/Downloads`. Cần ghi rõ `routes`, API không tự lấy checkbox của giao diện.

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

`HTTP 201` là tạo việc, không phải hoàn tất. Giữ `id` và truy vấn `GET /api/v1/jobs/{id}` đến `status: complete` hoặc đọc `error`. `speed_bps` là **byte/giây**, nhân 8 chia 1,000,000 để có Mbit/s.

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

Tạm dừng/tiếp tục nhận `{}`. Xem [OpenAPI](../openapi.yaml) cho body xóa, giữ tệp mặc định, `Idempotency-Key`, queue, batch, SSE, quyền và giới hạn. Có thể gửi `expected_md5`, `expected_sha256`, `expected_sha512`; kết quả có `md5`, `sha256`, `sha512`, `checksum_sources` và cảnh báo. JSON keys/trạng thái không đổi theo ngôn ngữ. [Ví dụ chi tiết](../API.ru.txt) bằng tiếng Nga.

Ví dụ Python 3: thay URL thực, cấu hình mạng và để ứng dụng chạy. UTF-8 bảo toàn cả đường dẫn tiếng Việt/Unicode.

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

## 8. Lịch sử, dữ liệu và cập nhật

| Hệ thống | Dữ liệu | Ứng dụng/runtime |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app` |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/` |

Thành phần Mac gốc là `/Library/PrivilegedHelperTools/MultiRouteResume/4.2.0/gateway-net`. Windows portable 3.0 dùng `data/` tại chỗ launcher, đổi bằng `data-location.config`/`--data-dir`. Java riêng Linux ở `~/.local/share/MultiRouteResume/runtime`, thành phần bảo vệ ở `/usr/local/libexec/MultiRouteResume/3.0/gateway-net`. Tệp tải ở đích bạn chọn, có thể nằm ngoài các nơi này.

`logs/downloads-history.jsonl` là JSON từng dòng UTF-8, `logs/downloads-history.log` là văn bản có ngày/giờ. Được thêm qua các lần chạy, không viết lại quá khứ khi đổi ngôn ngữ hoặc xóa việc. Text giữ ngôn ngữ lúc ghi; UI dùng ngôn ngữ hiện tại. Cửa sổ chỉ đọc tối đa 1,000 sự kiện gần nhất/2 MiB, còn toàn bộ hồ sơ trên đĩa tiếp tục lớn, có thể lưu trữ riêng khi cần. Có tên và đường dẫn tuyệt đối nhưng không có URL nguồn/password/token; xem thông tin cá nhân trước khi chia sẻ.

Sự kiện `lifecycle_history` có `event_id`, `job_id`, `kind`, `time`, `filename`, `path`; torrent có thể thêm `paths`, pause có `reason` (`pause`, `shutdown`, `maintenance`). Loại `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`. Thời gian ISO 8601 UTC như `2026-10-18T12:34:56.123Z`; text có ngày/giờ địa phương đầy đủ và UTC offset. `first_started_at`, `last_resumed_at`, `completed_at` tùy chọn chỉ ghi bắt đầu, tiếp tục mới nhất, hoàn tất sau xác minh/xuất bản đã quan sát. Nằm trong queue chưa là bắt đầu, không bịa ngày cũ. `recovered` kiểm tra hiện tại một tệp đã xuất bản trước, không khẳng định biết ngày hoàn tất xưa.

Cập nhật bằng cách thoát khay, cài gói mới đầy đủ và giữ dữ liệu/đoạn. Windows gỡ ở Installed apps. Xóa `.app` trên Mac không tự xóa lịch sử/thành phần bảo vệ. Không xóa thư mục tải chỉ để gỡ chương trình. Di chuyển thư mục Linux thì triển khai lại.

## 9. Khắc phục lỗi và phạm vi kiểm chứng

- Không khởi động: kiểm phiên bản, OS/CPU, gói đầy đủ, module Java và `JAVA_EXE`/`JAVA_HOME`. Giữ bản sửa khóa tệp xuất tạm từ 3.9 và kiểm danh tính instance cũ.
- Tốc độ 0: kiểm Ethernet, mạng con, IP ảo trống và quyền Internet của cổng/nguồn. Test tốc độ thành công không chứng minh mọi tải thật đều thành công.
- Lỗi torrent glibc/macOS: dùng OS hỗ trợ, không tự thay thư viện hệ thống.
- SFTP sai khóa: xác minh với quản trị viên, không tắt kiểm khóa.
- 8765 do ứng dụng khác giữ: dùng `--port 8766`, không kết thúc tiến trình không liên quan. Chỉ thay instance đã xác minh là MultiRoute.
- Hết chỗ/không ghi được: kiểm tệp/đoạn/cache và dừng trước đổi đích. Tiếp tục việc cũ thay vì tạo trùng cùng tệp.

Báo phiên bản, OS/CPU, giai đoạn và lỗi chính xác, loại password/token/link riêng và dữ liệu cá nhân. Test Windows/POSIX có kiểm soát, Mac CI và kiểm gói không chứng minh tốc độ/failover router thật, cài đặt người dùng hoặc hộp Finder/Gatekeeper/BPF. Gói chưa ký/notarize là **bản tiền phát hành**. Hãy thử tệp nhỏ biết trước qua từng cổng rồi nhóm kết hợp.

## 10. Giấy phép

Ứng dụng gốc được dùng miễn phí cho cá nhân, tổ chức và hoạt động kinh doanh. Không được bán, thu phí giấy phép hoặc phân phối có thu tiền nếu chưa có phép riêng. Giấy phép tùy chỉnh không cấp quyền chung sửa/phân phối lại và không được OSI phê duyệt là nguồn mở. Bản tiếng Anh [LICENSE.txt](../LICENSE.txt) có hiệu lực. FlatLaf (Apache-2.0) và các thành phần khác giữ giấy phép riêng. Chủ dự án: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## Tên mới và hồ sơ cũ

MultiRoute Downloader trước đây tên MultiRoute Resume. Hồ sơ mới dùng đường dẫn mới phía trên. Nếu chưa có thư mục mới mà có hồ sơ cũ thì tái dùng, không di chuyển/xóa tệp. Dữ liệu cũ có thể ở `%LOCALAPPDATA%\MultiRoute Resume\data` hoặc `~/Library/Application Support/MultiRoute Resume/data`. Giữ tương thích thư mục bảo vệ `MultiRouteResume`, ID nâng cấp Windows, danh tính API nội bộ và cổng. Sau cài đặt dùng shortcut/app MultiRoute Downloader mới. Tệp của các bản cũ giữ nguyên tên.

## Trang đã lưu, tải lên và xem trước tệp nén (4.0)

Mở **Hồ sơ trang web…** để tạo/sửa hồ sơ rồi chọn trong **Tải xuống** hoặc **Tải lên**. Cổng và IP ảo ở **Cài đặt mạng**. Trong **Tải lên**, nhập **URL thư mục từ xa** rồi dùng **Chọn tệp và tải lên…** chọn một hoặc nhiều tệp cục bộ bằng hộp thoại hệ thống. Giao diện tự thêm tên; API `target_url` chỉ đầy đủ một tệp từ xa. Dán URL tệp nén vào ô chính và bấm **Nội dung tệp nén…** để xem trước mà không bắt đầu tải.

Hồ sơ trang lưu tên, URL gốc, thư mục tải cục bộ, thư mục từ xa và các cổng. Hỗ trợ ẩn danh, HTTP Basic/Bearer/Cookie/header riêng, FTP và SFTP với dấu vân tay SHA-256 máy chủ đã xác minh trước. Nhiều tài khoản trên cùng máy chủ được tách biệt. Thư mục/cổng chỉ định rõ được ưu tiên; URL tương đối dùng URL gốc. Bí mật được mã hóa trong `site-profiles/vault.bin` với khóa cục bộ `site-profiles/key.bin`, chỉ chủ sở hữu có quyền. Người kiểm soát tài khoản và khóa vẫn đọc được. API công khai không trả bí mật. Trường trống chỉ giữ thông tin cũ khi origin và loại xác thực không đổi; `replace_credentials:true` thay thế rõ ràng. Chuyển hướng HTTP sang origin khác không nhận thông tin đăng nhập. Xóa hồ sơ không xóa tác vụ hay tệp cục bộ/từ xa; tác vụ giữ liên kết hồ sơ gốc và không thể đổi tài khoản; nếu hồ sơ gốc đã bị xóa, hãy tạo lại tác vụ với hồ sơ hợp lệ khi cần.

Tải lên và duyệt thư mục từ xa hỗ trợ **FTP, FTPS tường minh và SFTP** qua các cổng đã chọn. Chọn tệp cục bộ thông thường và URL tệp từ xa đầy đủ (`target_url`), không chỉ thư mục. Tệp tạm cho phép tiếp tục sau khi so sánh phần đầu đã có. Thành công yêu cầu đọc lại toàn bộ tệp từ xa và kiểm tra SHA-256; cần quyền **đọc, ghi và đổi tên**, tốn thêm lưu lượng. Sai mã băm hoặc không được đọc là lỗi. Mỗi tệp dùng một cổng tại một thời điểm; các tệp độc lập được phân phối và tải song song. Sau giới hạn không hoạt động có thể chuyển cổng, nhưng không chia một lượt tải lên thành các phần song song. Tạm dừng, tiếp tục và tiến độ được lưu qua khởi động lại. Xóa tác vụ giữ tệp cục bộ và tệp hoàn chỉnh/tạm từ xa.

Mặc định không ghi đè. FTP/SFTP không luôn bảo đảm đổi tên nguyên tử chỉ khi đích chưa tồn tại; ứng dụng khác có thể sửa cùng đích khi công bố. Hãy phối hợp các đích dùng chung. Duyệt thư mục không tải lên hoặc xóa tệp.

Xem trước **ZIP, TAR, 7Z, RAR4 và RAR5** liệt kê tên, đường dẫn, kích thước, cờ thư mục và mã hóa mà không giải nén hay thực thi. Cần cổng đã chọn, khoảng byte và ETag hoặc Last-Modified ổn định. Giới hạn 16 MiB siêu dữ liệu, 10.000 mục, 60 giây; giải mã header 7z tối đa 64 MiB bộ nhớ. Danh sách có thể bị cắt và không xác minh tính toàn vẹn nội dung. Chọn bộ mã gốc nếu tên sai. Mật khẩu cho header 7z mã hóa được hỗ trợ không được lưu. Header RAR mã hóa báo không hỗ trợ/cần mật khẩu; tệp nhiều phần có thể chỉ liệt kê một phần.

API nhận `site_profile_id` cho tải xuống, tải lên, duyệt và xem trước. `GET /api/v1/site-profiles` trả `{profiles:[...]}`; `POST`/`PUT` lưu đối tượng phẳng và trả dữ liệu công khai; `DELETE /api/v1/site-profiles/{id}` chỉ xóa hồ sơ. `POST /api/v1/uploads` nhận `local_path`, `target_url`, `routes`, `site_profile_id` tùy chọn, `overwrite:false`, trả HTTP 201. GET trả `{uploads:[...]}`; có GET/DELETE từng tác vụ và POST `/{id}/pause` / `/{id}/resume`. `POST /api/v1/remote/list` dùng `url`, `route`/`routes`; `POST /api/v1/archives/preview` còn nhận `password`, `charset` tùy chọn. Xem [openapi.yaml](../openapi.yaml). Manifest UTF-8 trong `data/uploads/` lưu ID hồ sơ, không lưu mật khẩu. Xem trước không tạo tác vụ tải.

## Lịch tải xuống và ưu tiên

Đặt `max_active_jobs` (1–32), giới hạn tốc độ chung/theo cổng và khung giờ hằng ngày (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm) trong phần hàng đợi/lịch. Dùng múi giờ máy tính, trả về qua `schedule_timezone`; có thể qua nửa đêm, hai giờ bằng nhau nghĩa là cả ngày. Ngoài giờ, tải trở lại hàng đợi; tạm dừng thủ công vẫn giữ. `priority` -100 đến 100 ưu tiên số cao trước rồi thời điểm tạo, không ngắt tác vụ đang chạy. `start_after_ms` là thời điểm Unix mili giây sớm nhất, 0 bỏ trì hoãn. Cấu hình được lưu và chỉ áp dụng tải xuống, không cho hàng đợi tải lên độc lập. Ứng dụng và máy phải chạy, không ngủ; không đánh thức OS. API: `GET`/`PUT`/`POST /api/v1/queue`, `PUT`/`POST /api/v1/jobs/{id}/options`.

## Tích hợp Chrome và Edge

Mở tích hợp **Chrome / Edge** trong ứng dụng, bật gửi liên kết và mở thư mục tiện ích. Một lần: vào `chrome://extensions` hoặc `edge://extensions`, bật chế độ nhà phát triển, chọn **Tải tiện ích đã giải nén** và thư mục `browser-extension` chứa `manifest.json`. Bản cài gốc chứa thư mục cạnh JAR; mã nguồn cũng có [browser-extension/](../browser-extension/). Giữ nguyên vị trí; giải nén `MultiRoute-Downloader-4.2.0-Chrome-Edge.zip` trước nếu dùng. Chưa phát hành trên cửa hàng.

MultiRoute phải chạy, kể cả trong khay. Gửi URL bằng menu liên kết hoặc popup tiện ích. Dùng thư mục, cổng và hồ sơ hiện tại; vẫn hiện lựa chọn repository/torrent khi cần. Cổng mặc định 8765 phải khớp API nếu đổi. Chỉ gửi URL, không sao chép cookie/mật khẩu và không chặn tải khác của trình duyệt. Cấu hình quyền nguồn trong ứng dụng. Chỉ cấp quyền kết nối cục bộ được hỏi cho tiện ích đã chọn này.

Cầu nối mặc định tắt, chỉ nhận `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` tại `/api/v1/browser/status`, `/api/v1/browser/download`; quy tắc API khác không đổi. HTTP 202 chỉ xác nhận gửi tới giao diện, không phải tải xong. Máy chủ headless riêng không mở hộp thoại desktop. Xóa tiện ích hoặc tắt tích hợp để ngừng gửi.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
