# macOS: cài đặt, khởi động và quyền truy cập

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · **Tiếng Việt**
<!-- macos-languages:end -->

## Cài đặt và mở

Trước khi cập nhật, chờ truyền xong và chọn **Quit/Exit** trong menu MultiRoute trên thanh menu. Nút đỏ có thể chỉ ẩn cửa sổ. Tải đúng PKG Apple Silicon hoặc Intel từ [Tải xuống](../../downloads/README.md), kiểm tra SHA-256 rồi mở. Xác nhận cài đặt trong hộp thoại quản trị macOS. Mở `/Applications/MultiRoute Downloader.app` bằng Finder hoặc Spotlight; không cần Java riêng hay Terminal.

## Cho phép mở

Nếu macOS không xác minh được nhà phát triển, hãy thử mở PKG hoặc ứng dụng trước. Chỉ với tệp đã kiểm tra và đáng tin: **Cài đặt hệ thống → Quyền riêng tư & bảo mật → Open Anyway → Open**; tên có thể khác theo phiên bản. Khi cảnh báo phần mềm độc hại hoặc tệp hỏng, dừng và kiểm tra tệp. [Hướng dẫn Apple](https://support.apple.com/en-us/102445).

## Mạng và mật khẩu

Khởi động ứng dụng lần đầu hoặc áp dụng IP ảo, DHCP hay MAC có thể cần quyền quản trị để mở Ethernet BPF. Đổi cấu hình hoặc khởi động lại thành phần có thể hỏi lại. Kiểm tra và đo tốc độ thông thường dùng lại thành phần đã chuẩn bị, không hỏi mật khẩu cho từng tệp/cổng mạng. Tuyến hệ điều hành và MAC vật lý không đổi.

Nếu hiện yêu cầu **Mạng cục bộ**, cho phép mục MultiRoute nhận diện được trong Quyền riêng tư & bảo mật → Mạng cục bộ. Không phải cài đặt nào cũng hiện yêu cầu. [Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy).

Chỉ nhập mật khẩu quản trị Mac vào hộp thoại hệ thống, không phải ô giấy phép/Hugging Face. Bên yêu cầu có thể là `DesktopLauncher`/Java: chỉ xác nhận thao tác MultiRoute tin cậy bạn vừa thực hiện. Sau khi hủy, chủ động kiểm tra lại và đọc nhật ký. Không tắt Gatekeeper/SIP toàn hệ thống.

[MultiRoute Downloader — Tiếng Việt](../vi.md) · [macOS — English / 12 languages](README.md)
