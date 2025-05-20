package memory;

import util.Constants;
import exceptions.MemoryAccessException;

/**
 * Mô phỏng bộ nhớ chính của CPU LEGv8.
 * Lưu trữ mã lệnh và dữ liệu, hỗ trợ đọc và ghi theo địa chỉ 64-bit.
 */
public class Memory {
    private byte[] memory; // Bộ nhớ lưu dưới dạng mảng byte

    /**
     * Khởi tạo bộ nhớ với kích thước được định nghĩa trong Constants.
     */
    public Memory() {
        memory = new byte[Constants.MEMORY_SIZE];
    }

    /**
     * Đọc 4 byte (32-bit) từ bộ nhớ tại địa chỉ đã cho, trả về mã lệnh.
     * @param address Địa chỉ 64-bit để đọc.
     * @return Giá trị 32-bit (mã lệnh).
     * @throws MemoryAccessException nếu địa chỉ không hợp lệ.
     */
    public int read(long address) {
        validateAddress(address, 4);
        // Chuyển 4 byte thành int (big-endian)
        return ((memory[(int) address] & 0xFF) << 24) |
               ((memory[(int) address + 1] & 0xFF) << 16) |
               ((memory[(int) address + 2] & 0xFF) << 8) |
               (memory[(int) address + 3] & 0xFF);
    }

    /**
     * Ghi 4 byte (32-bit) vào bộ nhớ tại địa chỉ đã cho.
     * @param address Địa chỉ 64-bit để ghi.
     * @param value Giá trị 32-bit cần ghi.
     * @throws MemoryAccessException nếu địa chỉ không hợp lệ.
     */
    public void writeInt(long address, int value) {
        validateAddress(address, 4);
        // Ghi 4 byte (big-endian)
        memory[(int) address] = (byte) (value >> 24);
        memory[(int) address + 1] = (byte) (value >> 16);
        memory[(int) address + 2] = (byte) (value >> 8);
        memory[(int) address + 3] = (byte) value;
    }

    /**
     * Đọc 8 byte (64-bit) từ bộ nhớ tại địa chỉ đã cho.
     * @param address Địa chỉ 64-bit để đọc.
     * @return Giá trị 64-bit.
     * @throws MemoryAccessException nếu địa chỉ không hợp lệ.
     */
    public long readLong(long address) {
        validateAddress(address, 8);
        // Chuyển 8 byte thành long (big-endian)
        return ((long) (memory[(int) address] & 0xFF) << 56) |
               ((long) (memory[(int) address + 1] & 0xFF) << 48) |
               ((long) (memory[(int) address + 2] & 0xFF) << 40) |
               ((long) (memory[(int) address + 3] & 0xFF) << 32) |
               ((long) (memory[(int) address + 4] & 0xFF) << 24) |
               ((long) (memory[(int) address + 5] & 0xFF) << 16) |
               ((long) (memory[(int) address + 6] & 0xFF) << 8) |
               (memory[(int) address + 7] & 0xFF);
    }

    /**
     * Ghi 8 byte (64-bit) vào bộ nhớ tại địa chỉ đã cho.
     * @param address Địa chỉ 64-bit để ghi.
     * @param value Giá trị 64-bit cần ghi.
     * @throws MemoryAccessException nếu địa chỉ không hợp lệ.
     */
    public void writeLong(long address, long value) {
        validateAddress(address, 8);
        // Ghi 8 byte (big-endian)
        memory[(int) address] = (byte) (value >> 56);
        memory[(int) address + 1] = (byte) (value >> 48);
        memory[(int) address + 2] = (byte) (value >> 40);
        memory[(int) address + 3] = (byte) (value >> 32);
        memory[(int) address + 4] = (byte) (value >> 24);
        memory[(int) address + 5] = (byte) (value >> 16);
        memory[(int) address + 6] = (byte) (value >> 8);
        memory[(int) address + 7] = (byte) value;
    }

    /**
     * Đặt lại bộ nhớ về 0.
     */
    public void reset() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
    }

    /**
     * Kiểm tra địa chỉ bộ nhớ hợp lệ.
     * @param address Địa chỉ cần kiểm tra.
     * @param size Kích thước dữ liệu (4 hoặc 8 byte).
     * @throws MemoryAccessException nếu địa chỉ không hợp lệ.
     */
    private void validateAddress(long address, int size) {
        if (address < 0 || address + size > Constants.MEMORY_SIZE) {
            throw new MemoryAccessException("Địa chỉ bộ nhớ không hợp lệ: 0x" + Long.toHexString(address));
        }
        if (address % size != 0) {
            throw new MemoryAccessException("Địa chỉ phải căn chỉnh theo " + size + " byte: 0x" + Long.toHexString(address));
        }
    }

    /**
     * Trả về biểu diễn chuỗi của trạng thái bộ nhớ (chỉ hiển thị vùng không rỗng).
     * @return Chuỗi dạng "0x<địa chỉ>: 0x<giá trị>, ...".
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < memory.length; i += 4) {
            int value = read(i); // Đọc 4 byte
            if (value != 0) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(String.format("0x%X: 0x%08X", i, value));
                first = false;
            }
        }
        return sb.length() > 0 ? sb.toString() : "Bộ nhớ rỗng";
    }
}