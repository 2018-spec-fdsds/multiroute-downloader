# MultiRoute cho tác nhân AI — thiết lập một lần, tải xuống theo mặc định

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <a href="ai-agents.de.md">Deutsch</a> · <a href="ai-agents.es.md">Español</a> · <a href="ai-agents.fr.md">Français</a> · <a href="ai-agents.zh.md">中文</a><br />
<a href="ai-agents.ko.md">한국어</a> · <a href="ai-agents.ar.md">العربية</a> · <a href="ai-agents.it.md">Italiano</a> · <a href="ai-agents.ja.md">日本語</a> · <a href="ai-agents.hi.md">हिन्दी</a> · <strong>Tiếng Việt</strong>
</p>
<!-- ai-agent-languages:end -->

[Tổng quan sản phẩm](overview/vi.md) · [Tài liệu API](../API.ru.txt) · [OpenAPI](../openapi.yaml)

Thêm một quy tắc dự án và kết nối công cụ một lần. Sau đó Claude Code, Codex và các tác nhân có công cụ khác có thể giao cho MultiRoute tải trọng số mô hình, tập dữ liệu, tệp lưu trữ và các tệp bên ngoài khác của dự án, lưu ID tác vụ, theo dõi tiến độ và kiểm tra hoàn tất. Bạn có thể yêu cầu: **“Tải mô hình cần cho dự án này vào thư mục tài nguyên của dự án.”** Quy tắc xác định trình tải xuống ưu tiên; bạn không cần lặp lại chi tiết API trong mỗi yêu cầu.

Mô hình có thể chạy trên đám mây hoặc cục bộ. Điều quan trọng là **công cụ của nó thực thi ở đâu**: công cụ phải truy cập được máy tính đang chạy MultiRoute. Một cuộc trò chuyện trong trình duyệt hay môi trường cách ly từ xa không có quyền truy cập PC của bạn chỉ nhờ đọc tệp hướng dẫn.

## 1. Chọn thiết lập mặc định của dự án một lần

1. Cài đặt và khởi động MultiRoute trên máy tính sẽ lưu tệp. Sử dụng cổng API được cấu hình cho phiên bản đang chạy đó; các ví dụ dùng `8765`.
2. Chọn thư mục tài nguyên dự án bằng đường dẫn tuyệt đối, chẳng hạn `C:/Projects/MyProject/assets` trên Windows hoặc `/Users/YOUR_USER/Projects/MyProject/assets` trên macOS. Tài nguyên mạng đã gắn trên Mac dùng `/Volumes/Share/...`.
3. Chọn rõ ràng các tuyến REST. Kết nối thông thường của hệ điều hành dùng `[{"type":"direct","enabled":true}]`. Để sử dụng các cổng mạng độc lập đã cấu hình của MultiRoute, dùng `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]`, thêm các cổng mạng khác đã được phê duyệt nếu cần. Trước tiên hãy cấu hình và kiểm tra các cổng mạng đó trong ứng dụng. Các hộp kiểm trên giao diện không tự động được sao chép vào yêu cầu. Tuyến direct sử dụng định tuyến IP thông thường của hệ điều hành, bao gồm các tuyến VPN hệ thống/TUN áp dụng. Nó không kế thừa cấu hình proxy HTTP/PAC hay tiện ích VPN chỉ dành cho trình duyệt.
4. Việc tạo lượt tải xuống từ bên ngoài cần khả năng **`automation_api`**. Tạo theo lô còn cần `batch_downloads`; các giới hạn khác vẫn có hiệu lực. Kiểm tra `GET /license/policy`, bao gồm `features.automation_api`.
5. Giữ API trên địa chỉ loopback. API không có mã thông báo xác thực từ xa dùng chung; không công khai cổng của nó trên LAN hoặc Internet. Lưu thông tin xác thực nguồn trong hồ sơ trang web của MultiRoute và truyền `site_profile_id` của hồ sơ khi cần.

## 2. Thêm quy tắc dự án có thể tái sử dụng

Sao chép **[khối hướng dẫn dùng ngay](examples/ai-agent-instructions.vi.md)** vào các quy tắc hiện có của dự án. Thay điểm lưu và các tuyến một lần, giữ nguyên các hướng dẫn khác của dự án.

| Tác nhân / môi trường | Nơi đặt quy tắc | Kết nối cần thiết |
| --- | --- | --- |
| Claude Code trên PC của bạn | `CLAUDE.md` trong dự án. Nếu quy tắc đã nằm trong `AGENTS.md`, thêm `@AGENTS.md` vào `CLAUDE.md` hiện có thay vì duy trì hai bản sao. | Quyền truy cập shell/REST cục bộ, hoặc máy chủ MCP stdio bên dưới. |
| Codex trên PC của bạn | `AGENTS.md` trong dự án. | Quyền truy cập shell/REST cục bộ, hoặc máy chủ MCP đã cấu hình của nó. |
| Tác nhân cục bộ khác hoặc phần mềm chạy mô hình cục bộ | Hướng dẫn dự án/hệ thống được lưu lâu dài của nó. | Bật công cụ có thể gọi REST hoặc một máy khách MCP stdio. Chỉ nạp mô hình không bổ sung công cụ. |
| Mô hình đám mây với bộ thực thi tác nhân cục bộ | Quy tắc dự án của tác nhân cục bộ. | Công cụ cục bộ của nó gọi MultiRoute trên cùng PC. Bản thân mô hình không cần chạy cục bộ. |
| ChatGPT web, Claude web hoặc bộ thực thi đám mây từ xa | Hướng dẫn dự án cùng một công cụ kết nối được hỗ trợ. | Cần cầu nối được cấu hình riêng tới máy tính MultiRoute. Bộ điều hợp cục bộ đi kèm ở đây dùng stdio, không phải dịch vụ MCP từ xa công khai. |

Tệp hướng dẫn định hướng hành vi; chúng không vượt qua bước phê duyệt công cụ hay môi trường cách ly của máy khách. Các hạn chế hiện có của máy khách/tài khoản vẫn áp dụng. Xem [hướng dẫn dự án Claude](https://code.claude.com/docs/en/memory), [hướng dẫn dự án Codex](https://developers.openai.com/codex/guides/agents-md/) và [yêu cầu kết nối MCP tùy chỉnh của ChatGPT](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt) chính thức.

## 3. Ví dụ REST — tác nhân cục bộ có quyền truy cập shell

Đây là cách đơn giản nhất khi tác nhân đã có terminal cục bộ. **MultiRoute phải đang chạy sẵn.** Các ví dụ sử dụng URL nguồn minh họa; hãy thay URL đó và thư mục đầu ra. Tạo tác vụ sẽ xếp hàng một lượt tải xuống thực sự.

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'Cần có automation_api của MultiRoute' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # Chỉ thêm expected_sha256 khi có mã băm thập lục phân 64 ký tự đáng tin cậy của nhà phát hành.
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# Lưu $job.id. Truy vấn lại khi tác vụ đang ở trạng thái queued/running/retrying/verifying:
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / shell cục bộ

Lưu JSON sau thành `download.json` với mã hóa UTF-8. Thay `YOUR_USER` và URL nguồn; thêm `expected_sha256` vào cùng đối tượng khi có mã băm đáng tin cậy.

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
# Sau khi xác nhận features.automation_api là true:
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# Thay id ví dụ bằng id trong phản hồi:
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` có nghĩa là tác vụ đã được tạo, chưa có nghĩa tệp đã sẵn sàng. Tiếp tục đọc trạng thái đến khi là `complete`, hoặc báo cáo trạng thái tạm dừng/thất bại. Khi hoàn tất, kiểm tra `checksum_checking`, `integrity_status` và các mã băm có sẵn. Dùng cùng khóa idempotency cho lần thử lại giống hệt; dùng khóa mới cho yêu cầu mới có chủ đích. Giữ việc thăm dò gọn nhẹ và cách quãng hợp lý; tệp lớn có thể tiếp tục hoạt động rất lâu. Trình quản lý gói và Git vẫn dùng quy trình phụ thuộc/nhân bản vốn có.

## 4. Tùy chọn: công cụ MCP cho máy khách cục bộ tương thích

MultiRoute bao gồm **bộ điều hợp MCP stdio**, được khởi chạy dưới dạng tiến trình con Java. Nó chuyển tiếp thao tác tới ứng dụng đang chạy. Không có điểm cuối HTTP `/mcp` tích hợp sẵn. `download_add` hiện tại yêu cầu **địa chỉ IPv4 cổng mạng** rõ ràng; nó không chấp nhận tuyến REST `direct`. Dùng REST ở trên cho tuyến chỉ qua hệ điều hành.

Tìm đường dẫn Java/JAR đã cài đặt chính xác trong **Cài đặt → MCP và CLI…**. Bản cài Windows tiêu chuẩn có `runtime/bin/java.exe` và `app/MultiRoute-Downloader.jar` bên dưới `%LOCALAPPDATA%/MultiRoute Downloader App`. Trên macOS, chúng là `Contents/runtime/Contents/Home/bin/java` và `Contents/app/MultiRoute-Downloader.jar` bên trong `/Applications/MultiRoute Downloader.app`. Đặt đường dẫn có khoảng trắng trong dấu nháy. Các gói cài đặt gốc này không cần cài Java riêng.

Ví dụ mục cấu hình máy khách stdio; thay cả hai đường dẫn tuyệt đối. Các giá trị JSON **không** tự động khai triển `%LOCALAPPDATA%`, `$HOME` hay biến shell:

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

Với Claude Code, hợp nhất mục này vào `.mcp.json` theo [cấu hình dự án trong tài liệu](https://code.claude.com/docs/en/mcp). Với Codex, cấu hình cùng lệnh và danh sách đối số dưới `[mcp_servers.multiroute]` trong cấu hình TOML, hoặc dùng `codex mcp add`; xem [thiết lập MCP chính thức](https://developers.openai.com/codex/mcp/). Các phần mềm chủ khác dùng cấu hình MCP riêng. Đây là cấu hình mẫu, chưa phải kết nối đã cài đặt.

Sau khi máy khách khám phá công cụ, yêu cầu nó liệt kê công cụ MultiRoute. Khi đó các đối số của `download_add` có thể là:

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

Lệnh gọi `download_status` tiếp theo nhận `{"id":"012345abcdef"}`. Các công cụ có sẵn là `download_add`, `download_batch`, `download_list`, `download_status`, `download_pause`, `download_resume` và `download_verify`. Mọi thao tác của bộ điều hợp đều cần `automation_api`; `download_verify` kiểm tra lại tệp đã hoàn tất. Kết quả MCP rút gọn bỏ qua URL nguồn, thông tin xác thực và thư mục đích, vì vậy hãy lưu thư mục từ yêu cầu. `gateways: []` không phải định tuyến tự động.

## 5. Trò chuyện đám mây và môi trường cách ly từ xa

**Chỉ một hướng dẫn dự án không thể kết nối ChatGPT hoặc Claude web với ứng dụng cục bộ của bạn.** `127.0.0.1` trong công cụ đám mây trỏ tới môi trường của chính công cụ đó. Kết nối MCP từ xa của ChatGPT cần dịch vụ từ xa tương thích hoặc cầu nối an toàn được hỗ trợ; URL REST của MultiRoute không phải điểm cuối MCP như vậy. Bản phát hành này không cung cấp cầu nối từ xa đó. Mô hình được lưu trữ trên đám mây nhưng dùng bởi Claude Code, Codex cục bộ hoặc bộ thực thi cục bộ khác có thể dùng các phương pháp cục bộ ở trên. [Yêu cầu kết nối OpenAI](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

Tệp được lưu trên máy tính MultiRoute. Bản dựng từ xa cần bước truyền tệp/lưu trữ dùng chung riêng đã được phê duyệt trước khi có thể sử dụng chúng. Thông tin xác thực phải nằm trong hồ sơ trang web đã cấu hình, không nằm trong khối hướng dẫn hay cuộc trò chuyện.

## Báo cáo mong đợi từ tác nhân

“Đã tải `model.gguf` qua MultiRoute vào thư mục tài nguyên đã cấu hình. Tác vụ: `…`. Trạng thái: complete. SHA-256: …; khớp giá trị tham chiếu của nhà phát hành.” Nếu không có giá trị tham chiếu đáng tin cậy được cung cấp hoặc tìm thấy, báo cáo “đã tính SHA-256 cục bộ; không có khả năng xác minh theo nhà phát hành.” Nếu ứng dụng không khả dụng hoặc trả về `pro_required`, báo cáo điều đó và thống nhất phương án thay thế thay vì âm thầm đổi trình tải xuống.
