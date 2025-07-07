package memory;

import util.Constants;
import java.util.HashMap;
import java.util.Map;
import exceptions.MemoryAccessException;

public class Memory {
    private long[] memory; // Sử dụng long[] để hỗ trợ 64-bit trực tiếp

    public Memory() {
        memory = new long[Constants.MEMORY_SIZE / 8]; // Chia 8 vì mỗi phần tử là 8 byte
    }

    /**
     * Đọc dữ liệu từ bộ nhớ với kích thước chỉ định.
     * @param address Địa chỉ 64-bit
     * @param size Kích thước (1, 2, 4, hoặc 8 byte)
     * @return Giá trị đọc được
     * @throws MemoryAccessException Nếu địa chỉ không hợp lệ
     */
    public long read(long address, int size) {
        validateAddress(address, size);
        int index = (int) (address / 8); // Chuyển đổi địa chỉ thành chỉ số mảng
        long value = memory[index];
        switch (size) {
            case 1: // Đọc 1 byte (byte thấp nhất)
                return value & 0xFF;
            case 2: // Đọc 2 byte (short)
                return (value >> ((address % 8) * 8)) & 0xFFFF;
            case 4: // Đọc 4 byte (int)
                return (value >> ((address % 8) * 8)) & 0xFFFFFFFFL;
            case 8: // Đọc 8 byte (long)
                return value;
            default:
                throw new MemoryAccessException("Kich thuoc khong duoc ho tro: " + size + " bytes");
        }
    }

    /**
     * Ghi dữ liệu vào bộ nhớ với kích thước chỉ định.
     * @param address Địa chỉ 64-bit
     * @param value Giá trị cần ghi
     * @param size Kích thước (1, 2, 4, hoặc 8 byte)
     * @throws MemoryAccessException Nếu địa chỉ không hợp lệ
     */
    public void write(long address, long value, int size) {
        validateAddress(address, size);
        int index = (int) (address / 8); // Chuyển đổi địa chỉ thành chỉ số mảng
        long mask = switch (size) {
            case 1 -> 0xFFL << ((address % 8) * 8);
            case 2 -> 0xFFFFL << ((address % 8) * 8);
            case 4 -> 0xFFFFFFFFL << ((address % 8) * 8);
            case 8 -> 0xFFFFFFFFFFFFFFFFL;
            default -> throw new MemoryAccessException("Kich thuoc khong duoc ho tro: " + size + " bytes");
        };
        long clearedValue = memory[index] & ~mask;
        long shiftedValue = (value << ((address % 8) * 8)) & mask;
        memory[index] = clearedValue | shiftedValue;
    }

    /**
     * Đặt lại toàn bộ bộ nhớ về 0.
     */
    public void reset() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0L;
        }
    }

    /**
     * Kiểm tra và xác thực địa chỉ bộ nhớ.
     * @param address Địa chỉ 64-bit
     * @param size Kích thước truy cập
     * @throws MemoryAccessException Nếu địa chỉ không hợp lệ
     */
    private void validateAddress(long address, int size) {
        if (address < 0 || address + size > Constants.MEMORY_SIZE) {
            throw new MemoryAccessException("Dia chi bo nho khong hop le: 0x" + Long.toHexString(address));
        }
        if (address % size != 0) {
            throw new MemoryAccessException("Dia chi phai can chinh theo " + size + " byte: 0x" + Long.toHexString(address));
        }
    }
    
    /**
     * Scans the memory and returns a map of all non-zero 64-bit long values.
     * This is useful for displaying the memory state in the GUI without showing
     * millions of empty addresses.
     *
     * @return A Map where the key is the address and the value is the 64-bit data at that address.
     */
    public Map<Long, Long> getAllData() {
        Map<Long, Long> dataMap = new HashMap<>();
        // Iterate through memory in 8-byte chunks (the size of a long)
        for (long address = 0; address < memory.length; address += 8) {
            // Use the existing readLong method, but handle potential unaligned access at the end
            if (address + 8 <= memory.length) {
                long value = read(address, 8);
                if (value != 0) {
                    dataMap.put(address, value);
                }
            }
        }
        return dataMap;
    }

    /**
     * Trả về chuỗi biểu diễn bộ nhớ (chỉ hiển thị giá trị khác 0).
     * @return Chuỗi định dạng địa chỉ:giá trị
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < memory.length; i++) {
            long value = memory[i];
            if (value != 0L) {
                if (!first) {
                    sb.append(", ");
                }
                long address = (long) i * 8;
                sb.append(String.format("0x%016X: 0x%016X", address, value));
                first = false;
            }
        }
        return sb.length() > 0 ? sb.toString() : "Bo nho rong";
    }

    /**
     * Lấy kích thước bộ nhớ (tính bằng byte).
     * @return Kích thước bộ nhớ
     */
    public long getSize() {
        return (long) memory.length * 8;
    }

    /**
     * Kiểm tra xem một địa chỉ có trống không (giá trị = 0).
     * @param address Địa chỉ 64-bit
     * @return true nếu trống, false nếu đã sử dụng
     */
    public boolean isEmpty(long address) {
        validateAddress(address, 8);
        int index = (int) (address / 8);
        return memory[index] == 0L;
    }
}
