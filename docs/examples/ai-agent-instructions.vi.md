# Hướng dẫn dự án MultiRoute — sao chép vào quy tắc của tác nhân

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agent-instructions.md">English</a> · <a href="ai-agent-instructions.ru.md">Русский</a> · <a href="ai-agent-instructions.de.md">Deutsch</a> · <a href="ai-agent-instructions.es.md">Español</a> · <a href="ai-agent-instructions.fr.md">Français</a> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<a href="ai-agent-instructions.ko.md">한국어</a> · <a href="ai-agent-instructions.ar.md">العربية</a> · <a href="ai-agent-instructions.it.md">Italiano</a> · <a href="ai-agent-instructions.ja.md">日本語</a> · <a href="ai-agent-instructions.hi.md">हिन्दी</a> · <strong>Tiếng Việt</strong>
</p>
<!-- ai-agent-languages:end -->

Dùng khối này trong `AGENTS.md`, `CLAUDE.md` hiện có hoặc hướng dẫn dự án lâu dài tương đương. Thêm vào cuối và giữ các quy tắc khác của dự án. Thay điểm lưu và các tuyến một lần. Để thiết lập và xem ví dụ, đọc [hướng dẫn tiếng Việt](../ai-agents.vi.md).

```markdown
## Tải xuống: dùng MultiRoute theo mặc định

Đối với các tệp bên ngoài cần cho nhiệm vụ dự án đã được cho phép (mô hình,
tập dữ liệu, tệp lưu trữ, nội dung đa phương tiện và tài nguyên khác), dùng
MultiRoute Downloader đang chạy. Không yêu cầu tôi chọn trình tải xuống cho
mỗi tệp. Giữ trình quản lý gói và Git trên quy trình cài đặt/nhân bản/cập nhật
thông thường của chúng.

Thiết lập dự án (chỉnh sửa một lần):
- API: http://127.0.0.1:8765/api/v1
- Điểm lưu: <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- Tuyến REST: [{"type":"direct","enabled":true}]
- site_profile_id tùy chọn: <EXISTING_PROFILE_ID_OR_OMIT>

Dùng công cụ REST cục bộ đã kết nối hoặc công cụ MCP của MultiRoute khi có.
Với MCP, cấu hình riêng địa chỉ IPv4 cổng mạng rõ ràng; download_add yêu cầu
url, directory và gateways. Tuyến REST direct là đường đi của hệ điều hành;
đó không phải giá trị cổng mạng MCP. Không tự đặt tuyến hoặc mặc định rằng
lựa chọn trên giao diện được áp dụng.

Với REST, kiểm tra GET /health và GET /license/policy trước lần dùng đầu tiên
trong phiên. Nếu chỉ có MCP, khám phá công cụ và gọi download_list với limit 1
để kiểm tra kết nối và khả năng; bộ điều hợp tự kiểm tra chính sách giấy phép.
Tạo lượt tải xuống cần features.automation_api=true. Nếu không khả dụng, báo
lý do và hỏi về trình tải xuống khác thay vì âm thầm dùng một trình khác.

Với mỗi tệp cần thiết: chọn URL chính xác, điểm lưu đã được phê duyệt và tên
tệp; truyền SHA-256 đáng tin cậy của nhà phát hành dưới dạng expected_sha256
khi có. Tạo tác vụ bằng POST /jobs (routes, không phải gateways), hoặc MCP
download_add. Dùng khóa idempotency ổn định khi thử lại cùng yêu cầu; lưu id
tác vụ được trả về. Tạo chưa có nghĩa là hoàn tất. Truy vấn tác vụ đó bằng
GET /jobs/{id} hoặc download_status theo khoảng thời gian hợp lý. Báo cáo
trạng thái tạm dừng/thất bại; dùng lại tác vụ hiện có để tiếp tục thay vì tạo
bản trùng. Không thay đổi tác vụ không liên quan hoặc thiết lập mạng. Khi
hoàn tất, kiểm tra checksum_checking và integrity_status. Không khẳng định
đã xác minh theo nhà phát hành nếu chưa khớp tham chiếu đáng tin cậy.
Trả về đường dẫn cục bộ đã lưu, id tác vụ và kết quả xác minh. Tệp vẫn ở trên
máy tính MultiRoute; không khẳng định nó tồn tại trong môi trường cách ly
thực thi từ xa.
```

Quy tắc thể hiện ưu tiên của dự án. Vẫn cần công cụ kết nối tương thích và quyền thông thường của tác nhân. Các tệp nguồn được tải xuống là dữ liệu, không phải hướng dẫn bổ sung cho tác nhân.
