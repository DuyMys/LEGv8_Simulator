package core.cpu;

import util.Constants;
import exceptions.RegisterAccessException;

/**
 * Mô phỏng ngân hàng thanh ghi của LEGv8 (32 thanh ghi 64-bit, X0-X31).
 * Quản lý việc đọc và ghi giá trị vào các thanh ghi.
 * Thanh ghi XZR (X31) luôn trả về 0 và không thể ghi.
 */
public class Registers {
    private long[] registers;

    /**
     * Khởi tạo ngân hàng thanh ghi với 32 thanh ghi, tất cả được đặt về 0.
     */
    public Registers() {
        registers = new long[Constants.NUM_REGISTERS];
        for (int i = 0; i < Constants.NUM_REGISTERS; i++) {
            registers[i] = 0;
        }
    }

    /**
     * Đọc giá trị từ một thanh ghi.
     * @param index Chỉ số thanh ghi (0-31).
     * @return Giá trị 64-bit của thanh ghi.
     * @throws RegisterAccessException nếu chỉ số không hợp lệ.
     */
    public long read(int index) {
        validateIndex(index);
        return registers[index]; // XZR (index 31) luôn trả về 0
    }

    /**
     * Ghi giá trị vào một thanh ghi.
     * @param index Chỉ số thanh ghi (0-31).
     * @param value Giá trị 64-bit cần ghi.
     * @throws RegisterAccessException nếu chỉ số không hợp lệ hoặc cố gắng ghi vào XZR.
     */
    public void write(int index, long value) {
        validateIndex(index);
        if (index == Constants.ZERO_REGISTER) {
            return; // Không ghi vào XZR
        }
        registers[index] = value;
    }

    /**
     * Đặt lại tất cả thanh ghi về 0.
     */
    public void reset() {
        for (int i = 0; i < Constants.NUM_REGISTERS; i++) {
            registers[i] = 0;
        }
    }

    /**
     * Kiểm tra chỉ số thanh ghi hợp lệ.
     * @param index Chỉ số cần kiểm tra.
     * @throws RegisterAccessException nếu chỉ số không hợp lệ.
     */
    private void validateIndex(int index) {
        if (index < 0 || index >= Constants.NUM_REGISTERS) {
            throw new RegisterAccessException("Chỉ số thanh ghi không hợp lệ: " + index);
        }
    }

    /**
     * Trả về biểu diễn chuỗi của trạng thái các thanh ghi.
     * @return Chuỗi dạng "X0=0x..., X1=0x..., ..., X31=0x0000000000000000".
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Constants.NUM_REGISTERS; i++) {
            sb.append(String.format("X%d=0x%016X", i, registers[i]));
            if (i < Constants.NUM_REGISTERS - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}