# LEGv8_Simulator

## Giới thiệu

LEGv8 Simulator là một phần mềm mô phỏng hoạt động của kiến trúc LEGv8 – kiến trúc vi xử lý 64-bit đơn giản hóa, thường được giảng dạy trong các môn Kiến trúc máy tính.

Phần mềm hỗ trợ biên dịch và thực thi mã hợp ngữ LEGv8, giúp sinh viên quan sát cách các lệnh được xử lý qua các giai đoạn: Fetch, Decode, Execute, Memory Access, Write Back, cũng như theo dõi giá trị thanh ghi, bộ nhớ, cờ trạng thái và sơ đồ datapath.

---
## Cấu trúc thư mục
src/

├── LEGv8GUI.java # Giao diện chính

├── DatapathGUI.java # Giao diện mô phỏng datapath

├── t1.txt, t2.txt # File test mẫu

├── core/ # Thành phần xử lý logic chính

├── datapath/ # Mô hình phần cứng datapath

├── exceptions/ # Xử lý ngoại lệ

├── instruction/ # Các lớp tương ứng từng lệnh

├── memory/ # RAM, Stack, bộ nhớ mô phỏng

├── util/ # Hàm tiện ích dùng chung

└── images/ # Hình ảnh giao diện

## Hướng dẫn chạy phần mềm

Sử dụng code branch2
1. Clone dự án:
   ```
   git clone https://github.com/DuyMys/LEGv8_Simulator.git
2. Checkout đến branch2
   ```
   git checkout branch2
3. Run code

Chạy file LEGv8GUI.java để sử dụng giao diện đồ họa.

Thay đổi đường dẫn đến file instructions.txt (thuộc folder instruction) trong hàm main.

Run this:
```
javac -d out src/util/*.java src/instruction/*.java
cp src/instruction/instructions.config out/instruction/
java -cp out instruction.TestControlSignals
```
