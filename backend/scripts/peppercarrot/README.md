# Dữ liệu demo Pepper&Carrot

Truyện tranh demo **"Hạt Tiêu & Cà Rốt"** (migration `db/demo/V103__demo_peppercarrot.sql`, ảnh ở
`src/main/resources/demo-media/demo/peppercarrot/`) là 6 tập đầu của webcomic
[Pepper&Carrot](https://www.peppercarrot.com).

- **Tác giả:** David Revoy, giấy phép [Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/).
- **Bản dịch tiếng Việt:** Binh Pham.
- **Nguồn:** các kho `Deevad/peppercarrot_epNN_translation` và `Deevad/peppercarrot_fonts` trên GitHub.
- **Thay đổi so với bản gốc:** ghép lớp chữ tiếng Việt vào tranh, thu nhỏ ảnh còn rộng 800px (JPEG), bỏ các trang
  đệm trống. Trang cuối mỗi tập vẫn giữ nguyên phần ghi công và giấy phép của tác giả.

Nhờ giấy phép CC BY 4.0, dự án được dùng, sửa và phân phối lại các tập này, miễn là ghi công như trên. Phần ghi công
cũng nằm trong mô tả truyện và hồ sơ tài khoản `peppercarrot` (tài khoản demo, không phải của tác giả).

## Dựng lại ảnh

Cần Python 3, Node 22+ và Microsoft Edge (dùng chế độ headless để vẽ SVG).

```bash
cd backend/scripts/peppercarrot
python download.py ../../target/peppercarrot 1,2,3,4,5,6        # tải tranh gốc, bản dịch và phông chữ
node render.mjs ../../target/peppercarrot ../../target/pc-out 800 80 1,2,3,4,5,6
python build-demo.py ../../target/pc-out ../../src/main/resources  # chép ảnh và sinh lại V103
```

Lời thoại trong các tệp dịch viết bằng `<flowRoot>` của Inkscape 0.91, thứ trình duyệt không vẽ.
`render.mjs` đổi từng khối đó thành `<foreignObject>` chứa đoạn HTML tự xuống dòng, đặt đúng khung và giữ kiểu chữ,
rồi chụp lại thành JPEG.

V103 đã nạp vào cơ sở dữ liệu dev thì Flyway không chạy lại. Muốn nạp bản mới, xóa cơ sở dữ liệu dev
(`docker compose down -v`) hoặc thêm một migration mới.
