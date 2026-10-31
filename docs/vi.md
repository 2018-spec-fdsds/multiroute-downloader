# MultiRoute Downloader 4.3.9 — Hướng dẫn tiếng Việt

[macOS: cài đặt, khởi động và quyền truy cập](macos/vi.md)

<!-- product-overview:start -->
[**Tổng quan sản phẩm**](overview/vi.md)
<!-- product-overview:end -->

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md) · [Home](../README.md)

<!-- current437:start -->

<a name="current-free-pro"></a>

## 4.3.9 — Tính năng hiện tại, Free và Pro

Hướng dẫn này mô tả MultiRoute Downloader 4.3.9 và các thiết lập hiện hành. Lấy gói dành cho nền tảng của bạn từ [bản tải xuống đã công bố](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/latest), đối chiếu SHA-256 rồi làm theo hướng dẫn cài đặt và bảo mật bên dưới. Các giới hạn Free và khả năng Pro dưới đây áp dụng cho 4.3.9.

### Giới hạn và tính năng giấy phép

Free có tải từng tệp thông thường, tiếp tục tải khi được hỗ trợ, toàn bộ tốc độ khả dụng, kiểm tra toàn vẹn, DNS được bảo vệ và tốc độ/trạng thái gateway trực tiếp. Không cố ý giới hạn băng thông. Các quyền đã ký độc lập: giấy phép Pro một phần chỉ mở những quyền có trong đó.

| Tính năng | Free | Có quyền trong giấy phép | ID |
| --- | --- | --- | --- |
| Cổng mạng dùng đồng thời | ≤ 2 | ≤ 8 | `extra_gateways` |
| Tải xuống đồng thời | ≤ 3 | ≤ 32 | `extra_concurrent_downloads` |
| Hồ sơ trang web | ≤ 3 | Không có hạn mức giấy phép | `extra_site_profiles` |
| Mức ưu tiên tải xuống | Chỉ bình thường | Có thể chọn | `download_priorities` |
| Mức ưu tiên của cổng kết nối | 0 | Có thể chọn | `gateway_priorities` |
| Lịch sử và báo cáo cổng mạng | Không bao gồm | Bao gồm | `gateway_statistics` |
| Tải xuống / tải lên hàng loạt | Không bao gồm | Bao gồm | `batch_downloads` |
| API cho ứng dụng bên ngoài | Không bao gồm | Bao gồm | `automation_api` |
| Giao diện bổ sung | Graphite / System | Mọi giao diện | `premium_appearance` |

Để nhập hàng loạt, mở **Tải xuống mới…** rồi chọn **Danh sách URL…** trong hàng thao tác cố định ở cuối cửa sổ.

Chỉ các quyền trong giấy phép đã ký được mở khóa. Tệp và hồ sơ hiện có được giữ nguyên. Tệp bổ sung chờ trong hàng đợi; tốc độ và trạng thái cổng mạng trực tiếp vẫn khả dụng. Giới hạn an toàn lưu trữ vẫn áp dụng.

Giới hạn gateway tính tập hợp dùng chung cho tải xuống và tải lên, không thêm đường hệ thống. Giới hạn ba tác vụ đồng thời chỉ áp dụng cho tải xuống, không phải tải lên; các lượt khác chờ hàng đợi. Giới hạn hàng đợi thấp hơn do người dùng đặt vẫn có hiệu lực. Gateway, hồ sơ, tiến độ và ưu tiên cũ không bị xóa. Hạn mức hồ sơ chỉ giới hạn việc tạo mới; hồ sơ cũ vẫn dùng được. Lịch sử/CSV cần quyền tương ứng; đồng hồ trực tiếp vẫn thuộc Free. Lập lịch vẫn có, còn ưu tiên khác mặc định cần quyền. Các giới hạn kỹ thuật của bộ nhớ và nguồn vẫn áp dụng.

Giấy phép đã ký gắn với khóa cài đặt, không phải MAC, IP hay router. Không có ngày hết hạn; 4–4 bao gồm 4.x. Đổi gateway, DHCP hay MAC ứng dụng không cần kích hoạt lại. Máy, tài khoản hoặc hồ sơ dữ liệu mới có thể cần yêu cầu mới. Đây không phải cam kết mã không thể sửa hay ràng buộc phần cứng phổ quát.

### MAC ứng dụng

MultiRoute dùng MAC ổn định riêng ở chế độ IP thủ công và DHCP, không đổi MAC của card hệ thống. Có kiểm tra xung đột với thiết bị quan sát được và các bản MultiRoute khác; không có phản hồi không chứng minh thiết bị đang tắt không tồn tại. Phạm vi: MAC unicast quản trị cục bộ, 46 bit ngẫu nhiên. Đây không phải dải địa chỉ IP DHCP.

**Đổi MAC…** — Dùng nút sau khi tạm dừng/hoàn tất các lượt đang chạy và chờ, đồng thời hoàn tất kiểm tra cục bộ. Ứng dụng kiểm tra địa chỉ nguồn quan sát được và thông báo hợp tác của các phiên MultiRoute khác. ‘Không phát hiện xung đột’ không bảo đảm thiết bị tắt hoặc im lặng không dùng địa chỉ đó. MAC ứng dụng phải khác MAC vật lý; không đổi bộ điều hợp hay tuyến OS.

### Mã kiểm tra…

Lưu giá trị tham chiếu không khởi động lại hay xóa tiến độ. MD5, SHA-256 hoặc SHA-512 được nhận diện từ 32, 64 hoặc 128 ký tự hex. Tệp ghép xong sẽ được kiểm tra sau khi tải. Lưu giá trị tham chiếu rồi chọn Kiểm tra ngay. Chỉ đọc tệp cục bộ đã hoàn tất. Nếu không khớp, tệp không bị tải lại hay xóa.

Kiểm tra cuối dùng giá trị tham chiếu được lưu gần nhất. Sửa giá trị không xóa byte đã tải hoặc khởi động lại truyền. Kiểm tra lại cục bộ giữ tệp hoàn tất và báo khớp, không khớp hoặc lỗi riêng biệt. Tham chiếu của nhà phát hành được giữ độc lập. Một checksum toàn tệp chỉ áp dụng cho một tệp torrent. Toàn vẹn không chứng minh nội dung vô hại; nên dùng tham chiếu SHA-256/SHA-512 đáng tin cậy.

### Truy cập API trong 4.3.9

API JSON cục bộ dùng UTF-8, không có token API thông thường. Điều khiển bên ngoài cần automation_api; các quyền khác vẫn được kiểm tra, nên quyền này không tự cho phép tải hàng loạt hoặc thêm gateway. Free giữ chẩn đoán đọc giới hạn, vòng đời được xác thực riêng và cầu nối tùy chọn cho một liên kết trình duyệt. Xem [API](../API.ru.txt), [OpenAPI](../openapi.yaml); không mở cổng ra LAN/Internet.


Ứng dụng gốc dùng [Giấy phép 1.2](../LICENSE.txt): Free cho phép dùng cá nhân và tổ chức/kinh doanh; chủ quyền có thể cung cấp Pro trả phí. Người nhận khác không được bán nếu chưa có phép riêng. OpenJDK GPLv2 với Classpath Exception yêu cầu thông báo/nguồn tương ứng; JNA dùng Apache-2.0 với thông báo; JLayer LGPL-2.1 riêng giữ quyền thay thế/gỡ lỗi/nguồn. Xem [điều kiện](../LICENSING.md), [danh mục đầy đủ](../THIRD-PARTY-NOTICES.txt) và trang giấy phép.

Kiểu System bảo đảm chữ trong bảng, ô nhập và điều khiển dễ đọc trên nền hệ thống; Graphite và System có trong Free.

Tên và kích thước tệp nay có các cột riêng. Kích thước được sắp xếp theo số, độc lập với tên; kích thước chưa biết được hiển thị bằng dấu gạch ngang. Tên và đường dẫn đầy đủ vẫn có trong chú giải.

Di chuột lên tên tệp để xem tên đầy đủ, đường dẫn lưu, giao thức truyền và địa chỉ hoặc đường dẫn nguồn cục bộ. Tác vụ torrent hiển thị BitTorrent kể cả khi siêu dữ liệu được mở qua HTTPS. Địa chỉ kho, nếu có, xuất hiện sau URL thực của tệp. Thông tin xác thực nhúng, giá trị truy vấn và đoạn fragment bị ẩn; infohash magnet công khai được nhận diện có thể còn hiển thị. URL đã lưu và trạng thái truyền không thay đổi.


### Ưu tiên cổng mạng và chẩn đoán truyền tải trong 4.3.9

Trong cài đặt mạng, đặt ưu tiên cho mỗi cổng mạng từ **0 đến 1000**; giá trị cũng xuất hiện cạnh chỉ báo chính. Số lớn hơn ưu tiên cổng đó cho tệp nhỏ hoặc lượt truyền toàn bộ tệp và giao thêm phân đoạn song song cho tệp lớn. Các cổng được chọn khác đang hoạt động vẫn hỗ trợ. Giá trị bằng nhau giữ nguyên thứ tự chọn; cổng được chọn khác có thể tiếp nhận công việc khi cổng ưu tiên bị lỗi hoặc ngừng truyền. Điều này không bảo đảm một tỷ lệ tốc độ cố định.

Cần quyền ký độc lập **`gateway_priorities`**. Quyền ưu tiên tác vụ **`download_priorities`** không bao gồm quyền này. Khi không có quyền cổng mạng, mọi mức ưu tiên hiệu lực là 0 và các giá trị đã lưu vẫn được giữ. Có thể thay đổi trong lúc tải mà không xóa phần đã xác minh. Mỗi cổng được chọn có từ một đến bốn tiến trình phân đoạn song song, tối đa 32 cho mỗi tác vụ.

Cài đặt cho phép chọn số lượt tải đồng thời trong giới hạn giấy phép và số lần thử lại tự động hữu hạn hoặc không giới hạn. Tác vụ tạm dừng thủ công vẫn giữ trạng thái đó. Menu chuột phải hỗ trợ tạm dừng/tiếp tục tác vụ phù hợp đã chọn và sao chép tên tệp. Nhấp đúp chỉ mở thư mục; thuộc tính, thống kê và nhật ký tải riêng nằm trong menu chuột phải.

Tệp đã tải xong vẫn có nhật ký riêng. Mục Lỗi và giải pháp trong cài đặt mở hướng dẫn ngoại tuyến có tìm kiếm với 25 chủ đề bằng cả 12 ngôn ngữ giao diện. Vị trí lưu và thuộc tính hiển thị dung lượng trống theo GB. Khi đĩa cục bộ đầy, ứng dụng báo rõ, giữ các phần đã lưu và không lặp lại vô hạn như lỗi mạng. Phần đầu đã lưu vẫn được tính trong tiến độ khi một phân đoạn nhỏ được tiếp tục đang chờ dữ liệu mới.

### Dữ liệu còn phải tải · 4.3.9

Bảng tải xuống có cột **Dữ liệu còn lại** riêng. Cột này hiển thị số byte còn phải tải, tách biệt với thời gian ước tính và được sắp xếp theo số. Giá trị tính cả tiến độ đã lưu và đang nhận, không xuống dưới 0 và hiển thị dấu gạch ngang khi chưa biết tổng kích thước.

### Khởi động Mac và thiết lập cổng mạng tin cậy hơn · 4.3.9

Khởi động gốc trên Mac nhận ra đường dẫn mặc định tương đối cũ, kể cả khi thiếu dấu gạch chéo đầu, và dùng hồ sơ hiện có đã xác minh hoặc thư mục mặc định trống thuộc người dùng. Vị trí tùy chỉnh mơ hồ vẫn yêu cầu chọn rõ ràng thay vì đoán.

Sau khi chuẩn bị cổng mạng thất bại hoặc bị hủy, các cổng còn lại và kiểm tra nền dừng lặp yêu cầu cấp quyền cho đến khi bạn chủ động thử lại thiết lập mạng. Lần khởi chạy đặc quyền đầu tiên vẫn có thể cần xác nhận quản trị macOS thông thường để mở thiết bị Ethernet BPF.

Thành phần mạng Mac 4.3.2 xử lý MAC bộ điều hợp bị macOS ẩn và vẫn giữ kiểm tra bảo vệ card vật lý. Ứng dụng dùng MAC riêng, không đổi tuyến hệ điều hành hoặc MAC vật lý. Biểu tượng và tên Mac thống nhất với nhận diện MultiRoute trên Windows. Hãy thoát ứng dụng trước khi cập nhật PKG. Tên bên yêu cầu trong hộp thoại thật và truyền LAN/DHCP vẫn cần xác nhận trên phần cứng; cách truyền này vẫn yêu cầu Ethernet có dây.

**macOS:** `/Applications/MultiRoute Downloader.app` · `/Library/PrivilegedHelperTools/MultiRouteResume/4.3.2/{gateway-net,gateway-launch}`.

### Phản hồi cổng mạng Mac với địa chỉ thủ công · 4.3.9

Thiết lập bắt gói POSIX được sửa để địa chỉ thủ công của ứng dụng và DHCP đều nhận phản hồi gửi đến MAC riêng của ứng dụng, giống hành vi Windows hiện có. Ping hệ thống thành công chỉ kiểm tra kết nối của máy tính, không chứng minh địa chỉ ảo của ứng dụng nhận được phản hồi.

Chẩn đoán DHCP giữ lời giải thích cụ thể bằng mười hai ngôn ngữ thay vì lỗi chung. Hành vi cấp quyền hiện có không đổi: kiểm tra mạng thông thường tái sử dụng thành phần đã chuẩn bị. API, điều khiển giao diện, tuyến hệ điều hành và MAC vật lý không thay đổi. Lần truy cập Ethernet BPF đặc quyền đầu tiên vẫn có thể cần xác nhận quản trị thông thường.

Đây là sửa một khác biệt của chế độ bắt gói, không phải mọi nguyên nhân cổng mạng không truy cập được. Kiểm tra địa chỉ trống, mạng con và thuê/xung đột DHCP vẫn hoạt động. Truyền Mac với địa chỉ thủ công còn cần xác nhận trên phần cứng; vẫn cần Ethernet có dây.

<!-- current437:end -->

<a name="appearance-and-sounds"></a>

<a name="giao-diện-và-âm-báo-hoàn-tất-trong-424"></a>

## Giao diện và âm báo hoàn tất

Mở **Cài đặt giao diện** và chọn một trong chín kiểu: **Graphite** (mặc định), **Nordic**, **StreamsStudio**, **Neon**, **Community**, **Chat**, **Studio**, **Rose**, **System**. Free có Graphite và System; các kiểu còn lại cần khả năng Pro tương ứng `premium_appearance`. Lựa chọn được lưu cục bộ và không khởi động lại tải xuống. **Bo góc cửa sổ và nút** là tùy chọn riêng. Các điều khiển có thể bo góc trong mọi kiểu; viền ngoài cửa sổ do hệ điều hành quyết định. Windows 11 có thể giữ góc vuông khi cửa sổ được phóng to hoặc ghim theo bố cục; macOS và Linux cũng tự quản lý viền cửa sổ. Biểu đồ tốc độ hiển thị hoạt động truyền, còn thẻ cổng mạng tập hợp thiết lập tương ứng trong cửa sổ mạng.

Âm báo hoàn tất **mặc định tắt**. Bật hộp kiểm chính rồi chọn riêng thông báo **sau mỗi tệp** và **sau tất cả lượt tải**. Có **Chuông nhẹ**, **Piano** và **Marimba**; bạn có thể nghe thử cả hai giai điệu. Khi mọi lượt tải đã hoàn tất và thông báo tổng được bật, tệp cuối chỉ phát giai điệu kết thúc, không chồng thêm âm báo riêng. Tác vụ tạm dừng hoặc lỗi chưa được xem là hoàn tất. Lịch sử đã hoàn thành không phát lại âm thanh lúc mở ứng dụng hay đổi cài đặt. Âm lượng hệ thống và đầu ra âm thanh cũng quyết định khả năng nghe.

Để dùng âm thanh riêng, chọn **Giai điệu tùy chỉnh** rồi chọn các tệp **MP3 hoặc WAV** riêng cho thông báo từng tệp và toàn bộ lượt tải. Mỗi đoạn phải **không quá 10 giây**. Ứng dụng kiểm tra thời lượng âm thanh đã giải mã, không chỉ dựa vào đuôi tệp hay siêu dữ liệu. Các tệp hợp lệ được sao chép vào hồ sơ ứng dụng cục bộ; sau đó có thể di chuyển tệp gốc.

Trong **Cài đặt giao diện**, bạn có thể bật hoặc ẩn **nhân vật MultiRoute**. Đây là tiện ích tùy chọn nằm riêng trên màn hình máy tính; chọn nhân vật nam hoặc nữ. Điều chỉnh tỷ lệ **40–100%** và nhấp đúp nút chuột trái vào nhân vật để khôi phục cửa sổ ứng dụng. Chuyển động đi bộ hoặc chạy phụ thuộc vào **tổng tốc độ tải thực tế**; nhân vật dừng khi không hoạt động. Túi và nhãn phản ánh **tổng số byte còn lại đã biết của các lượt tải đang hoạt động**; mỗi tệp chỉ được tính một lần dù có nhiều cổng mạng, và không tự đặt số byte còn lại cho tệp chưa biết kích thước. Lựa chọn hiển thị được lưu, hoạt ảnh dừng khi bị ẩn hoặc không hoạt động. Đây chỉ là hiệu ứng hiển thị: tệp vẫn nằm trong thư mục đã chọn, và nhân vật hoạt động độc lập với âm báo hoàn tất. Các nhân vật là hình ảnh gốc được tạo ra, không liên kết với Codex hay OpenAI.

Giao diện, bo góc, âm thanh và nhân vật là thiết lập cục bộ của ứng dụng máy tính. Chúng không thêm điểm cuối vào API tải xuống và không thay đổi truyền tệp, mã kiểm tra hay bảo mật DNS.

<a name="startup-profile-location"></a>

<a name="sửa-đường-dẫn-hồ-sơ-khi-khởi-động-trong-423"></a>

## Đường dẫn hồ sơ khi khởi động

Ứng dụng đã cài lưu đường dẫn tuyệt đối của thư mục dữ liệu, không phụ thuộc thư mục làm việc lúc đăng nhập. Đường dẫn tương đối đã lưu chỉ được chuyển thành tuyệt đối khi có thể nhận diện rõ một hồ sơ hiện có. Lịch sử tải, thiết lập và các phần đã lưu giữ nguyên vị trí. Đường dẫn tương đối của bản portable và `--data-dir` chỉ định rõ được hỗ trợ theo cách khởi chạy tương ứng.

<a name="automatic-startup"></a>

<a name="tự-khởi-động-trong-421"></a>

## Tự khởi động

Bật **Khởi động cùng hệ điều hành** cạnh phần chọn ngôn ngữ và giao diện. Tùy chọn này **tắt mặc định**. Bật sẽ đăng ký MultiRoute cho lần đăng nhập tiếp theo của tài khoản bạn; tắt sẽ xóa mục khởi động do ứng dụng sở hữu. Đây là khởi động khi người dùng đăng nhập, không phải dịch vụ hệ thống hay chạy trước khi đăng nhập. Tạo mục này không cần quyền quản trị.

Khi tự khởi động, ứng dụng dùng khay hệ thống nếu có; nếu không sẽ hiển thị cửa sổ bình thường có thể truy cập. Ưu tiên, trạng thái tạm dừng và lịch của hàng đợi được giữ nguyên; không ép tiếp tục mọi bản tải đã tạm dừng. Hãy mở ứng dụng thủ công trước để hoàn tất cài đặt và điều kiện mạng. Giữ nguyên đường dẫn đã đăng ký của ứng dụng; khi dùng bản di động, giữ cả Java và thư viện. Sau khi chuyển vị trí, tắt rồi bật lại tự khởi động từ vị trí mới. Tắt trước khi gỡ cài đặt.

Hệ điều hành hoặc chính sách môi trường máy tính có thể vô hiệu hóa mục đã đăng ký. Trên macOS, kiểm tra **Cài đặt hệ thống → Cài đặt chung → Mục đăng nhập & Phần mở rộng** nếu được yêu cầu cấp phép; quyền của thành phần mạng là riêng biệt. Đăng ký không chứng minh lần đăng nhập sau chắc chắn chạy thành công. Kiểm tra bằng hồ sơ thử riêng không bao gồm khởi động lại hoặc đăng xuất thật; xem báo cáo bản phát hành tương ứng.

Nếu không mở được ứng dụng, **chỉ xóa mục của chính ứng dụng**:

- **Windows:** ứng dụng lưu giá trị `REG_SZ` riêng tên **MultiRoute Downloader** tại `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`. Nên tắt tự khởi động trong ứng dụng. Nếu không mở được, dùng Registry Editor để **chỉ xóa giá trị đó**, giữ nguyên khóa Run và các mục khác. Nếu hệ điều hành đã tắt, dùng **Cài đặt → Ứng dụng → Khởi động**; ứng dụng không ghi đè quyết định này. Windows giới hạn toàn bộ lệnh khởi động ở 260 ký tự; nếu quá dài, dùng đường dẫn ứng dụng/hồ sơ ngắn hơn rồi đăng ký lại.
- **macOS:** xóa `~/Library/LaunchAgents/io.github.2018-spec-fdsds.multiroute-downloader.plist`.
- **Linux:** xóa `${XDG_CONFIG_HOME:-~/.config}/autostart/io.github.2018-spec-fdsds.multiroute-downloader.desktop` khỏi thư mục cấu hình XDG thực tế. Cần môi trường đồ họa hỗ trợ XDG autostart; tùy chọn này không cài dịch vụ systemd.

<a name="dns-settings"></a>

<a name="thiết-lập-dns-trong-422"></a>

## Thiết lập DNS

**Đối chiếu phiên bản bộ cài và SHA-256 đã công bố; xem NATIVE-INSTALLER-TESTS.json tương ứng để biết kết quả xác minh nền tảng và gói.**

Mở **Mạng → Phân giải DNS**. Mặc định là **DNS qua HTTPS (DoH), Cloudflare**. Thiết lập chỉ thuộc MultiRoute, không đổi DNS, bảng định tuyến hoặc IP giao diện của hệ điều hành.

DoH hỗ trợ HTTP/2 có giới hạn khi TLS ALPN chọn và HTTP/1.1 khi giao thức đó được thương lượng. Tương thích nhà cung cấp và kết quả cổng thực tế nằm trong báo cáo phát hành cuối cùng tương ứng.

- **DoH:** DNS mã hóa qua HTTPS trên từng cổng mạng đã chọn; kiểm tra chứng chỉ TLS theo tên máy trong URL.
- **DoT:** DNS mã hóa qua TLS trên từng cổng mạng, mặc định cổng 853; kiểm tra tên máy chủ trên chứng chỉ.
- **DNS hệ thống (máy tính):** chủ động dùng chính sách DNS của máy, OS hoặc VPN. DNS không được bảo đảm đi theo từng cổng đã chọn, nhưng kết nối tệp vẫn dùng cổng của mình. Chế độ này không tự bảo đảm mã hóa DNS.

Chọn Cloudflare, Quad9, Google hoặc **Tùy chỉnh**. Giá trị mẫu là cố định; Tùy chỉnh cho sửa URL/tên máy chủ, cổng và IPv4 công cộng dạng số để kết nối ban đầu. Địa chỉ số tránh phải phân giải tên nhà cung cấp qua OS trước. DoH/DoT xác minh chuỗi tin cậy, tên máy và SNI; không âm thầm chuyển sang DNS rõ hoặc DNS hệ thống. Lỗi DNS bảo mật làm lần thử đó thất bại. Nhà cung cấp phải truy cập được qua cổng đã chọn.

Sửa bản nháp, chọn cổng mạng rồi nhấn **Kiểm tra DNS**. Kiểm tra không lưu, hiển thị kết quả, thời gian và IPv4 công cộng hoặc lỗi cho từng cổng. Lỗi một phần vẫn hiện rõ. Thành công ở chế độ Hệ thống chỉ kiểm tra phân giải OS, không chứng minh WAN độc lập của từng router. **Áp dụng thiết lập DNS** kiểm tra và lưu; số phiên bản trả về xác định cấu hình đã áp dụng. Cập nhật trạng thái và đổi ngôn ngữ giữ bản nháp chưa lưu, con trỏ và vị trí cuộn.

Mỗi truy vấn có tổng hạn 8 giây, kể cả truy vấn DNSSEC phụ. TTL đã xác thực còn bị giới hạn bởi hạn chữ ký; bộ đệm gốc tối đa 300 giây và tách theo cổng/phiên bản cấu hình. TTL bằng 0 và HTTP Age DoH vẫn có hiệu lực. Chế độ Hệ thống theo bộ đệm OS/JVM. Hỗ trợ đích tệp IPv4 công cộng; bản này không thêm phân giải IPv6.

API: `GET /api/v1/network/dns` đọc cấu hình và trạng thái chỉ đọc; `PUT`/`POST` cùng đường dẫn áp dụng cấu hình. `POST /api/v1/network/dns/test` kiểm tra bản nháp; `GET /api/v1/network/dns/providers` liệt kê mẫu. Chỉ gửi trường cấu hình, không gửi phiên bản/trạng thái từ GET. [API](../API.ru.txt) · [OpenAPI](../openapi.yaml).

<a name="dnssec-validation"></a>

<a name="xác-thực-dnssec-cục-bộ-trong-422"></a>

## Xác thực DNSSEC cục bộ

Trong **Mạng → Phân giải DNS → Xác thực DNSSEC**, chọn **Xác thực tên miền đã ký** (mặc định DoH/DoT), **Yêu cầu câu trả lời DNSSEC an toàn** hoặc **DNSSEC đã tắt**. Java xác thực cục bộ bằng dnsjava và điểm neo gốc đi kèm. Chỉ cờ AD của nhà cung cấp không đủ làm bằng chứng. DoH/DoT mã hóa đường truyền; DNSSEC xác thực dữ liệu DNS riêng biệt.

**Xác thực** nhận câu trả lời an toàn và chưa xác nhận bảo vệ; **Yêu cầu** chỉ nhận an toàn. Chưa xác nhận có thể là ủy quyền không ký đã được chứng thực hoặc thuật toán không hỗ trợ; đọc lý do. Kết quả không hợp lệ và chưa xác định bị từ chối khi bật xác thực. DNS hệ thống tắt xác thực cục bộ; trở lại DoH/DoT khôi phục chính sách trước. Đổi nhà cung cấp giữ chính sách. Áp dụng lưu; Kiểm tra DNS dùng bản nháp chưa lưu.

Mỗi truy vấn phân biệt `secure`, `insecure`, `bogus`, `indeterminate`, `not_checked`; thiếu kết quả được ghi là không có. Kết nối thành công không chứng minh DNSSEC an toàn. Chữ ký hết hạn, thiếu khóa/bằng chứng, lỗi truy vấn và hết giờ có lý do riêng. Đồng hồ hệ thống phải đúng. Truy vấn xác thực phụ dùng cùng cổng và tổng thời hạn. DNSSEC không giấu tên hay kiểm tra nội dung tệp tải.

API thêm `dnssec`: `off`, `validate`, `require`; chế độ Hệ thống chỉ nhận `off`. Các trường `dnssec_validation`, `dnssec_trust_anchors` và từng truy vấn `dnssec_status`, `dnssec_reason_code`, `dnssec_reason`, `dnssec_validated_ttl`, `dnssec_aux_queries` chỉ đọc; không gửi lại khi lưu cấu hình. Xem API/OpenAPI phía trên và [DNSSEC](#xác-thực-dnssec-cục-bộ-trong-422).

**Xử lý phạm vi byte HTTP:** nếu CDN như Civitai trả HTTP 200 cho vùng byte bắt đầu từ 0, ứng dụng kiểm tra thêm vùng bắt đầu sau 0. Đoạn đầu chỉ được đọc trong giới hạn của đoạn; dữ liệu tải tuần tự đã lưu và khớp nguồn có thể tiếp tục theo từng đoạn. HTTP 200 tạm thời cho vùng phía sau sẽ được thử lại sau 2–30 giây, giữ dữ liệu và luồng xử lý cổng mạng. Nhật ký truyền bền vững theo cổng phân biệt lưu lượng nhận với tiến độ đã lưu. [Chẩn đoán truyền](transfer-diagnostics.en.md).

[Bộ cài 4.3.9](../downloads/v4.3.9/README.md) kèm Java 21 riêng. Trình cài Windows dùng tiếng Anh và tạo lối tắt trên màn hình; vẫn cần cài Npcap riêng. macOS dùng libpcap của hệ thống. Làm theo các bước cài đặt và xác nhận bảo mật bên dưới.

Chủ dự án: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

<a name="1-mục-đích-và-cách-hoạt-động"></a>

## Mục đích và cách hoạt động

MultiRoute Downloader là ứng dụng Java trên máy tính để tải tệp qua nhiều **cổng Internet** trong cùng mạng Ethernet nội bộ. Ví dụ, có thể kết hợp đường truyền cố định và bộ định tuyến di động 4G/5G. Các cổng được chọn lấy những đoạn tệp từ hàng đợi chung; cổng nhanh hơn có thể tải nhiều đoạn hơn. Khi một kết nối mất, các cổng khác tiếp tục, đoạn chưa xong trở về hàng đợi và dữ liệu đã xác minh được giữ lại. Có thể nhập danh sách liên kết hoặc dùng API cục bộ từ ứng dụng khác để tạo nhóm tải.

Mức tăng tốc phụ thuộc băng thông độc lập của từng đường truyền, giới hạn máy chủ và khả năng tiếp tục an toàn của giao thức; không bảo đảm tăng theo số bộ định tuyến. Đây là ứng dụng desktop độc lập, không phải giao diện web. Hồ sơ mới mặc định tiếng Anh; hỗ trợ tiếng Anh, Nga, Đức, Tây Ban Nha, Pháp, Trung, Hàn, Ả Rập, Ý, Nhật, Hindi và Việt. Lựa chọn được lưu và áp dụng cho thông báo đã đăng ký, trạng thái, nhật ký, chú giải. Thông báo thô từ máy chủ/thư viện ngoài có thể giữ nguyên ngôn ngữ. Tên tệp, đường dẫn, URL và khóa JSON không bị dịch.

<a name="application-virtual-ip"></a>

### Vì sao ứng dụng dùng IP ảo

**Chọn cổng kết nối cho MultiRoute mà không thay đổi kết nối của các ứng dụng khác.** Ứng dụng dùng một địa chỉ IPv4 cục bộ riêng, chưa bị chiếm, cho truyền tải mạng nội bộ. Nhờ đó, ứng dụng kết nối qua các cổng đã chọn độc lập với cổng mặc định của máy tính, không đổi địa chỉ adapter, bảng định tuyến hay cổng mặc định của hệ điều hành. Trình duyệt và các chương trình khác giữ thiết lập mạng thông thường.

Ví dụ, trong LAN `192.168.1.0/24`, máy tính giữ địa chỉ bình thường `192.168.1.x`, MultiRoute có thể dùng địa chỉ trống `192.168.1.70` và tải qua `192.168.1.1`–`192.168.1.4`. Máy tính, IP ảo và các cổng đã chọn phải thực sự thuộc cùng một mạng con IPv4. Kiểm tra rồi áp dụng địa chỉ trống trong cài đặt mạng của ứng dụng; không thêm thủ công vào adapter hệ điều hành. IP ảo không tạo thêm đường Internet: băng thông đến từ các bộ định tuyến và đường truyền bên ngoài của chúng.

#### Chọn và kiểm tra địa chỉ

1. Xem IPv4 và mặt nạ mạng của adapter Ethernet trên máy tính. Chọn địa chỉ máy hợp lệ trong cùng mạng con. `192.168.1.70` chỉ là ví dụ; không dùng IP của chính máy tính, bất kỳ cổng kết nối nào, địa chỉ mạng hoặc địa chỉ quảng bá.
2. Mở trang quản trị bộ định tuyến và xem dải cấp phát DHCP thực tế. Dùng địa chỉ ngoài dải tự cấp hoặc đặt chỗ/loại trừ trên máy chủ DHCP. Nếu nhiều máy chủ DHCP phục vụ cùng LAN, mọi máy chủ phải ngăn cấp địa chỉ đó cho thiết bị khác.
3. Trong MultiRoute, chọn **Kiểm tra và áp dụng** cho IP ảo rồi kiểm tra các cổng. Ứng dụng xác minh mạng con và tìm xung đột bằng ARP. Không có phản hồi ARP chỉ có nghĩa là chưa phát hiện xung đột tại thời điểm đó; không chứng minh địa chỉ nằm ngoài dải DHCP và không ngăn DHCP cấp nó sau này.

## Môi trường cần thiết

Bộ cài Windows/macOS có Java 21 riêng và thành phần mạng đã biên dịch. Windows cần trình điều khiển Npcap riêng; không cần cài Java riêng.

| Nền tảng | Hệ điều hành | Thành phần |
| --- | --- | --- |
| Windows x64 | Windows 11 | Java 21 |
| Apple Silicon | macOS 26+ | Java 21 |
| Intel Mac | macOS 15+ | Java 21 |

Cần Ethernet có dây cùng mạng con IPv4 với tất cả cổng kết nối đã chọn, IPv4 ảo chưa dùng, thư mục dữ liệu/tải xuống có quyền ghi và đủ chỗ cho tệp cùng các phần tạm. Mức tăng tốc phụ thuộc dung lượng WAN độc lập và máy chủ nguồn; không cần GPU.

Ứng dụng hỗ trợ Linux nhưng kho phân phối này chưa phát hành bộ cài Linux 4.x. Không dùng gói Windows hoặc macOS trên Linux.

Trước khi cập nhật, hãy chờ tải xuống, tải lên và kiểm tra tệp cuối cùng đang chạy hoàn tất. Sau đó thoát từ khay hệ thống và chạy bộ cài mới đầy đủ. Giữ thư mục dữ liệu, tệp đã tải và các phần đã lưu.

<a name="3-cài-đặt-và-xác-nhận-bảo-mật"></a>

## Cài đặt và xác nhận bảo mật

Chọn một bộ cài trong [bản phát hành 4.3.9](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.3.9), nếu được đính kèm, và đối chiếu SHA-256 công bố.

- Windows: [MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe).
- Apple Silicon, macOS 26+: [MultiRoute-Downloader-4.3.9-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-arm64.pkg).
- Intel, macOS 15+: [MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg).

### Windows

Mở EXE, cài cho người dùng hiện tại rồi chạy từ menu Start hoặc biểu tượng desktop. Ứng dụng không chạy như cửa sổ lệnh script. Văn bản trình cài của chúng tôi bằng tiếng Anh; hộp bảo mật theo ngôn ngữ Windows.

Lần đầu kiểm tra Npcap. Nếu thiếu, đồng ý tải trong ứng dụng: SHA-256 và nhà phát hành Authenticode của bản cài chính thức cố định được kiểm tra trước khi mở trình cài Npcap bình thường. Đọc giấy phép và chấp thuận UAC tiêu chuẩn. Khởi động lại Windows nếu được yêu cầu. Npcap không đi kèm và không cài âm thầm; bước này cần Internet nếu chưa có. Bản AdminOnly có thể cần thêm quyền bắt gói, nhưng vẫn chạy Java bằng người dùng thường.

Ứng dụng/bộ cài chưa ký nhà phát hành. Nếu SmartScreen báo không rõ tác giả, xác minh nguồn và mã băm trước khi quyết định dùng “More info → Run anyway” khi có. Phát hiện mã độc là trường hợp khác; không tắt antivirus hoặc tạo ngoại lệ để cài.

### macOS

Mở PKG đúng CPU trong Apple Installer và chấp thuận quyền quản trị để đặt thành phần mạng được bảo vệ. Sau đó mở `/Applications/MultiRoute Downloader.app` từ Finder, Spotlight hoặc Dock. Gói này không cần Java riêng, Terminal, Command Line Tools hay Homebrew.

Khi cần mở thiết bị Ethernet BPF, xuất hiện xác nhận quản trị macOS thông thường. Thành phần hạ quyền sau khi mở thiết bị bắt gói; Java giữ tài khoản thường. Gói và ứng dụng **chưa ký Developer ID hoặc notarized**. Nếu hệ thống cho ngoại lệ với nhà phát triển không xác minh được, kiểm tra nguồn và SHA-256 rồi theo [hướng dẫn Apple](https://support.apple.com/en-us/102445): System Settings → Privacy & Security → Open Anyway, sau đó Open. Máy được quản lý có thể cấm ngoại lệ.

Xác nhận cài đặt, quyền mạng về sau và ngoại lệ tác giả là các bước riêng. Khi được hỏi, cho truy cập mạng cục bộ và đúng thư mục tải đã chọn. Không tắt Gatekeeper/SIP, không xóa quarantine toàn hệ thống và không chạy toàn bộ Java bằng root. Cảnh báo mã độc/tệp hỏng không phải cảnh báo tác giả thông thường; dừng và kiểm tra tệp.

<a name="4-cổng-kết-nối-và-ip-ảo"></a>

## Cổng kết nối và IP ảo

Các hồ sơ cổng mạng mẫu dùng `192.168.1.1`, `192.168.1.2`, `192.168.1.3` và `192.168.1.4`. Có thể sửa hồ sơ và cấu hình **1–8** địa chỉ khác nhau. Chỉnh địa chỉ theo mạng thực tế, kiểm tra rồi tự chọn cổng cần dùng; có bốn hồ sơ không có nghĩa cả bốn cổng đều hoạt động. Free dùng đồng thời tối đa **2 cổng mạng**; khả năng Pro tương ứng `extra_gateways` cho phép tối đa **8 cổng mạng**. Giới hạn áp dụng cho tập hợp cổng mạng dùng chung giữa tải xuống và tải lên. Máy tính dùng một cổng làm mặc định không tạo thêm một đường Internet.

IPv4 ảo ban đầu là `192.168.1.70`. Kiểm tra/áp dụng trong cài đặt mạng, chắc rằng thiết bị khác không dùng và dành ngoài dải DHCP tự động. Địa chỉ dành riêng cho truyền tải nội bộ ứng dụng: **không thêm IP vào adapter hệ điều hành và không đổi bảng định tuyến hệ thống**. Chương trình khác giữ kết nối thông thường. Không tự thêm IP này trong cài đặt mạng OS. Xem dấu hỏi và chú giải bên cạnh.

Kiểm tra rồi chọn một hoặc nhiều cổng trong giới hạn giấy phép. Thử tốc độ giới hạn **16 MiB hoặc 5 giây**, có thể thêm thời gian chuẩn bị. Đó chỉ là giới hạn thử, không giới hạn tải thực. Nếu thiếu URL HTTP trực tiếp thích hợp thì dùng tệp thử Selectel.

Có thể thêm/xóa hồ sơ cổng và đổi cổng của công việc đang tải trong giới hạn giấy phép, miễn mỗi việc chưa xong còn ít nhất một cổng. Cổng vừa thêm không tự được chọn cho công việc hiện có. Đổi IP ảo cần tạm dừng công việc đang hoạt động. MAC ứng dụng được quản lý trong thiết lập mạng; MAC của bộ điều hợp vật lý không đổi, và các điều kiện tạm dừng/kiểm tra ở mục MAC phía trên vẫn áp dụng.

<a name="5-quản-lý-tải-và-giao-diện"></a>

## Quản lý tải và giao diện

1. Dán vào ô URL dài bằng Ctrl+V, Cmd+V trên Mac hoặc nút dán. Có thể dán magnet hay mở `.torrent`.
2. Chọn nơi lưu bằng hộp thư mục OS. Mặc định Windows `%USERPROFILE%\Downloads`, macOS/Linux `~/Downloads`; lựa chọn thủ công được giữ lại.
3. Chọn cổng và nhấn bắt đầu cạnh URL. Hugging Face, WebDAV, Metalink có xem trước/chọn tệp. Nhập hàng loạt nhận một link mỗi dòng hoặc tệp văn bản.
4. Xem tiến độ riêng, thời gian qua/còn lại, tổng tốc độ và tốc độ/dung lượng mỗi cổng. Đoạn HTTP mặc định **20 MiB**, chỉnh 1–64 MiB. Không có dữ liệu trong **60 giây** thì đoạn bị hủy và trả về nhóm chung cho cổng khác. Kích thước mảnh torrent do metadata quy định.
5. Hỗ trợ tạm dừng/tiếp tục, ưu tiên, giới hạn, lịch và đổi thư mục việc đã dừng. Mặc định hai việc hoạt động. Sau khởi động lại, việc đang chạy trở về tạm dừng. Seeding torrent mặc định tắt và có giới hạn thời gian/tỷ lệ.

FlatLaf cung cấp các kiểu giao diện nêu trên và lưu lựa chọn cục bộ. Tổng quan đếm mọi công việc, công việc đang chạy, đã hoàn tất và tổng tốc độ không phụ thuộc bộ lọc. Thanh tiến độ riêng, cột đầy chiều rộng, sắp xếp, tìm kiếm, lọc trạng thái và cuộn giữ vị trí đều có sẵn.

Rê chuột lên dòng đang tải hoặc đã tải để thấy **tên tệp đầy đủ và đường dẫn tuyệt đối**, đúng cả sau sắp xếp/lọc. Tiêu đề tệp được chọn có cùng chú giải; menu chuột phải sao chép toàn bộ đường dẫn. Ctrl+L/Cmd+L vào URL, Ctrl+F/Cmd+F vào tìm kiếm, Escape xóa tìm kiếm.

Việc được chọn có thẻ Chi tiết và Lịch sử. Báo cáo giữ vị trí đọc/phần chọn khi làm mới hay đổi ngôn ngữ; chọn việc khác mở báo cáo từ đầu. Nhật ký chung tách sự kiện ứng dụng và lịch sử tải. Ở cuối sẽ theo sự kiện mới, không nhảy khi đang đọc phần cũ.

Đóng cửa sổ vẫn tiếp tục ở khay hệ thống. Biểu tượng thể hiện tốc độ của tất cả việc đến 10 Gbit/s; chú giải hiển thị chính xác hơn. Mở ứng dụng để khôi phục, Thoát ứng dụng để lưu/kết thúc. Nếu không có khay thì thu nhỏ xuống taskbar. **Thoát từ khay trước khi cập nhật**.

Chọn nhiều hàng tải xuống bằng **Ctrl+nhấp chuột** trên Windows/Linux hoặc **Cmd+nhấp chuột** trên macOS; **Shift+nhấp chuột** chọn một dải. Khi bảng đang có tiêu điểm, **Ctrl+A** hoặc **Cmd+A** chỉ chọn tất cả hàng hiện đang hiển thị theo tìm kiếm và bộ lọc trạng thái. Thao tác xóa áp dụng cho cả nhóm đã chọn với một lần xác nhận. Tùy chọn xóa cả tệp hoàn chỉnh và các phần đã tải **mặc định không được chọn**, nên có thể chỉ bỏ mục khỏi danh sách mà giữ dữ liệu. Chỉ chọn tùy chọn đó khi muốn xóa tệp của các công việc ấy; các tệp khác và thư mục đích vẫn được giữ. Kết quả hiển thị số mục đã xóa. Công việc không xóa được vẫn nằm trong danh sách cùng bản tóm tắt lỗi để kiểm tra rồi thử lại; mục đã xóa thành công không bị xử lý lại. Xóa công việc hoặc tệp không xóa lịch sử tải xuống lâu dài.

<a name="6-giao-thức-và-kiểm-tra-toàn-vẹn"></a>

## Giao thức và kiểm tra toàn vẹn

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

<a name="7-api-cho-ứng-dụng-khác"></a>

## API cho ứng dụng khác

Khi đang chạy, API ở `http://127.0.0.1:8765/api/v1`. Yêu cầu/đáp ứng JSON UTF-8, body dùng `Content-Type: application/json`. Không cần API token để tạo/quản lý việc cục bộ; thông tin nguồn là riêng. Chỉ cùng máy truy cập được; không mở cổng ra LAN/Internet. `--headless` chỉ chạy API. Dùng launcher gốc; ví dụ `.cmd` dành cho portable.

Các ví dụ API dưới đây cần khả năng `automation_api`. Ví dụ bật bốn tuyến nên cũng cần khả năng `extra_gateways` phù hợp; đây không phải lựa chọn mặc định trong giao diện. Tự chỉ định tuyến theo cổng sẵn có và giới hạn giấy phép: tối đa 2 trong Free hoặc tối đa 8 với khả năng Pro tương ứng.

Kiểm tra: `GET /api/v1/health`, bình thường `{"ok":true,"version":"4.3.9"}`. Cấu hình mạng trước trong UI hoặc gửi tới `POST /api/v1/network`:

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

<a name="8-lịch-sử-dữ-liệu-và-cập-nhật"></a>

## Lịch sử, dữ liệu và cập nhật

| Hệ thống | Dữ liệu | Ứng dụng/runtime |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app` |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/` |

`logs/downloads-history.jsonl` là JSON từng dòng UTF-8, `logs/downloads-history.log` là văn bản có ngày/giờ. Được thêm qua các lần chạy, không viết lại quá khứ khi đổi ngôn ngữ hoặc xóa việc. Text giữ ngôn ngữ lúc ghi; UI dùng ngôn ngữ hiện tại. Cửa sổ chỉ đọc tối đa 1,000 sự kiện gần nhất/2 MiB, còn toàn bộ hồ sơ trên đĩa tiếp tục lớn, có thể lưu trữ riêng khi cần. Có tên và đường dẫn tuyệt đối nhưng không có URL nguồn/password/token; xem thông tin cá nhân trước khi chia sẻ.

Sự kiện `lifecycle_history` có `event_id`, `job_id`, `kind`, `time`, `filename`, `path`; torrent có thể thêm `paths`, pause có `reason` (`pause`, `shutdown`, `maintenance`). Loại `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`. Thời gian ISO 8601 UTC như `2026-10-18T12:34:56.123Z`; text có ngày/giờ địa phương đầy đủ và UTC offset. `first_started_at`, `last_resumed_at`, `completed_at` tùy chọn chỉ ghi bắt đầu, tiếp tục mới nhất, hoàn tất sau xác minh/xuất bản đã quan sát. Nằm trong queue chưa là bắt đầu, không tự đặt thời điểm chưa quan sát. `recovered` là kiểm tra hiện tại một tệp đã xuất bản đang có, không khẳng định biết ngày hoàn tất tải.

Cập nhật bằng cách thoát khay, cài gói mới đầy đủ và giữ dữ liệu/đoạn. Windows gỡ ở Installed apps. Xóa `.app` trên Mac không tự xóa lịch sử/thành phần bảo vệ. Không xóa thư mục tải chỉ để gỡ chương trình. Di chuyển thư mục Linux thì triển khai lại.

<a name="10-giấy-phép"></a>

## Giấy phép

Ứng dụng gốc được dùng miễn phí cho cá nhân, tổ chức và hoạt động kinh doanh. Không được bán, thu phí giấy phép hoặc phân phối có thu tiền nếu chưa có phép riêng. Giấy phép tùy chỉnh không cấp quyền chung sửa/phân phối lại và không được OSI phê duyệt là nguồn mở. Bản tiếng Anh [LICENSE.txt](../LICENSE.txt) có hiệu lực. FlatLaf (Apache-2.0) và các thành phần khác giữ giấy phép riêng. Chủ dự án: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

<a name="tên-mới-và-hồ-sơ-cũ"></a>

## Thư mục hồ sơ và thành phần cục bộ

Thư mục dữ liệu và vị trí ứng dụng được liệt kê trong bảng phía trên. Hồ sơ cũng có thể nằm ở `%LOCALAPPDATA%\MultiRoute Resume\data` hoặc `~/Library/Application Support/MultiRoute Resume/data`. Nếu không có thư mục dữ liệu mặc định và ứng dụng nhận diện rõ hồ sơ hiện có tại một trong các vị trí này, hồ sơ được sử dụng mà không di chuyển hay xóa tệp. Không đổi tên các thư mục đó khi ứng dụng đang chạy. Thành phần được bảo vệ dùng tên thư mục `MultiRouteResume`; định danh cài đặt Windows, danh tính API nội bộ và cổng gắn với thành phần đã cài. Dùng lối tắt hoặc ứng dụng MultiRoute Downloader đã cài để mở.

<a name="trang-đã-lưu-tải-lên-và-xem-trước-tệp-nén-40"></a>

## Trang đã lưu, tải lên và xem trước tệp nén

Mở **Hồ sơ trang web…** để tạo/sửa hồ sơ rồi chọn trong **Tải xuống** hoặc **Tải lên**. Cổng và IP ảo ở **Cài đặt mạng**. Trong **Tải lên**, nhập **URL thư mục từ xa** rồi dùng **Chọn tệp và tải lên…** chọn một hoặc nhiều tệp cục bộ bằng hộp thoại hệ thống. Giao diện tự thêm tên; API `target_url` chỉ đầy đủ một tệp từ xa. Dán URL tệp nén vào ô chính và bấm **Nội dung tệp nén…** để xem trước mà không bắt đầu tải.

Hồ sơ trang lưu tên, URL gốc, thư mục tải cục bộ, thư mục từ xa và các cổng. Hỗ trợ ẩn danh, HTTP Basic/Bearer/Cookie/header riêng, FTP và SFTP với dấu vân tay SHA-256 máy chủ đã xác minh trước. Nhiều tài khoản trên cùng máy chủ được tách biệt. Thư mục/cổng chỉ định rõ được ưu tiên; URL tương đối dùng URL gốc. Bí mật được mã hóa trong `site-profiles/vault.bin` với khóa cục bộ `site-profiles/key.bin`, chỉ chủ sở hữu có quyền. Người kiểm soát tài khoản và khóa vẫn đọc được. API công khai không trả bí mật. Trường trống chỉ giữ thông tin cũ khi origin và loại xác thực không đổi; `replace_credentials:true` thay thế rõ ràng. Chuyển hướng HTTP sang origin khác không nhận thông tin đăng nhập. Xóa hồ sơ không xóa tác vụ hay tệp cục bộ/từ xa; tác vụ giữ liên kết hồ sơ gốc và không thể đổi tài khoản; nếu hồ sơ gốc đã bị xóa, hãy tạo lại tác vụ với hồ sơ hợp lệ khi cần.

Tải lên và duyệt thư mục từ xa hỗ trợ **FTP, FTPS tường minh và SFTP** qua các cổng đã chọn. Chọn tệp cục bộ thông thường và URL tệp từ xa đầy đủ (`target_url`), không chỉ thư mục. Tệp tạm cho phép tiếp tục sau khi so sánh phần đầu đã có. Thành công yêu cầu đọc lại toàn bộ tệp từ xa và kiểm tra SHA-256; cần quyền **đọc, ghi và đổi tên**, tốn thêm lưu lượng. Sai mã băm hoặc không được đọc là lỗi. Mỗi tệp dùng một cổng tại một thời điểm; các tệp độc lập được phân phối và tải song song. Sau giới hạn không hoạt động có thể chuyển cổng, nhưng không chia một lượt tải lên thành các phần song song. Tạm dừng, tiếp tục và tiến độ được lưu qua khởi động lại. Xóa tác vụ giữ tệp cục bộ và tệp hoàn chỉnh/tạm từ xa.

Mặc định không ghi đè. FTP/SFTP không luôn bảo đảm đổi tên nguyên tử chỉ khi đích chưa tồn tại; ứng dụng khác có thể sửa cùng đích khi công bố. Hãy phối hợp các đích dùng chung. Duyệt thư mục không tải lên hoặc xóa tệp.

Xem trước **ZIP, TAR, 7Z, RAR4 và RAR5** liệt kê tên, đường dẫn, kích thước, cờ thư mục và mã hóa mà không giải nén hay thực thi. Cần cổng đã chọn, khoảng byte và ETag hoặc Last-Modified ổn định. Giới hạn 16 MiB siêu dữ liệu, 10.000 mục, 60 giây; giải mã header 7z tối đa 64 MiB bộ nhớ. Danh sách có thể bị cắt và không xác minh tính toàn vẹn nội dung. Chọn bộ mã gốc nếu tên sai. Mật khẩu cho header 7z mã hóa được hỗ trợ không được lưu. Header RAR mã hóa báo không hỗ trợ/cần mật khẩu; tệp nhiều phần có thể chỉ liệt kê một phần.

API nhận `site_profile_id` cho tải xuống, tải lên, duyệt và xem trước. `GET /api/v1/site-profiles` trả `{profiles:[...]}`; `POST`/`PUT` lưu đối tượng phẳng và trả dữ liệu công khai; `DELETE /api/v1/site-profiles/{id}` chỉ xóa hồ sơ. `POST /api/v1/uploads` nhận `local_path`, `target_url`, `routes`, `site_profile_id` tùy chọn, `overwrite:false`, trả HTTP 201. GET trả `{uploads:[...]}`; có GET/DELETE từng tác vụ và POST `/{id}/pause` / `/{id}/resume`. `POST /api/v1/remote/list` dùng `url`, `route`/`routes`; `POST /api/v1/archives/preview` còn nhận `password`, `charset` tùy chọn. Xem [openapi.yaml](../openapi.yaml). Manifest UTF-8 trong `data/uploads/` lưu ID hồ sơ, không lưu mật khẩu. Xem trước không tạo tác vụ tải.

## Lịch tải xuống và ưu tiên

Đặt `max_active_jobs` (1–32), giới hạn tốc độ chung/theo cổng và khung giờ hằng ngày (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm) trong phần hàng đợi/lịch. Dùng múi giờ máy tính, trả về qua `schedule_timezone`; có thể qua nửa đêm, hai giờ bằng nhau nghĩa là cả ngày. Ngoài giờ, tải trở lại hàng đợi; tạm dừng thủ công vẫn giữ. `priority` -100 đến 100 ưu tiên số cao trước rồi thời điểm tạo, không ngắt tác vụ đang chạy. `start_after_ms` là thời điểm Unix mili giây sớm nhất, 0 bỏ trì hoãn. Cấu hình được lưu và chỉ áp dụng tải xuống, không cho hàng đợi tải lên độc lập. Ứng dụng và máy phải chạy, không ngủ; không đánh thức OS. API: `GET`/`PUT`/`POST /api/v1/queue`, `PUT`/`POST /api/v1/jobs/{id}/options`.

## Tích hợp Chrome và Edge

Mở tích hợp **Chrome / Edge** trong ứng dụng, bật gửi liên kết và mở thư mục tiện ích có trong bộ cài. Vào `chrome://extensions` hoặc `edge://extensions`, bật chế độ nhà phát triển và dùng **Tải tiện ích đã giải nén** với thư mục đó. Giữ nguyên vị trí thư mục đã chọn.

MultiRoute phải chạy, kể cả trong khay. Gửi URL bằng menu liên kết hoặc popup tiện ích. Dùng thư mục, cổng và hồ sơ hiện tại; vẫn hiện lựa chọn repository/torrent khi cần. Cổng mặc định 8765 phải khớp API nếu đổi. Chỉ gửi URL, không sao chép cookie/mật khẩu và không chặn tải khác của trình duyệt. Cấu hình quyền nguồn trong ứng dụng. Chỉ cấp quyền kết nối cục bộ được hỏi cho tiện ích đã chọn này.

Cầu nối mặc định tắt, chỉ nhận `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` tại `/api/v1/browser/status`, `/api/v1/browser/download`; quy tắc API khác không đổi. HTTP 202 chỉ xác nhận gửi tới giao diện, không phải tải xong. Máy chủ headless riêng không mở hộp thoại desktop. Xóa tiện ích hoặc tắt tích hợp để ngừng gửi.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).

## Ảnh chụp giao diện

Các cửa sổ ứng dụng hiện hành được hiển thị bên dưới. Mở ảnh ở kích thước đầy đủ để đọc từng điều khiển. Các giá trị truyền tệp mẫu chỉ nhằm minh họa.

![MultiRoute Downloader — hàng đợi tải xuống và tốc độ cổng mạng](images/v4.3.7/downloads-en-ae5b55131583.png)

*Giao diện hiện hành trên Windows, được chụp với hồ sơ minh họa riêng biệt. Tệp, tiến độ, tốc độ mẫu và các màn hình có tính năng Pro minh họa các điều khiển, không phải số đo hiệu năng.*

<table>
<tr>
<td width="50%" valign="top"><strong>Thiết lập ứng dụng</strong><br /><a href="images/v4.3.7/settings-8abcb12a782d.png"><img src="images/v4.3.7/settings-8abcb12a782d.png" width="420" alt="Thiết lập ứng dụng" /></a></td>
<td width="50%" valign="top"><strong>Giao diện, người giao hàng và âm thanh</strong><br /><a href="images/v4.3.7/appearance-cebf121d5de7.png"><img src="images/v4.3.7/appearance-cebf121d5de7.png" width="420" alt="Giao diện, người giao hàng và âm thanh" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Địa chỉ ứng dụng và DNS được bảo vệ</strong><br /><a href="images/v4.3.7/network-dns-e29dfdba1007.png"><img src="images/v4.3.7/network-dns-e29dfdba1007.png" width="420" alt="Địa chỉ ứng dụng và DNS được bảo vệ" /></a></td>
<td width="50%" valign="top"><strong>Chọn cổng mạng và kiểm tra tốc độ</strong><br /><a href="images/v4.3.7/network-gateways-aee0a7897f48.png"><img src="images/v4.3.7/network-gateways-aee0a7897f48.png" width="420" alt="Chọn cổng mạng và kiểm tra tốc độ" /></a></td>
</tr>
</table>
