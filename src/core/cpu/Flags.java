package core.cpu;

/**
 * Mô phỏng các cờ trạng thái (flags) của CPU LEGv8.
 * Lưu trữ trạng thái Zero, Negative, Overflow, Carry sau các phép toán.
 * Được sử dụng cho các lệnh nhảy có điều kiện (CBZ, CBNZ).
 */
public class Flags {
    private boolean zero;     // Cờ Zero: true nếu kết quả bằng 0
    private boolean negative; // Cờ Negative: true nếu kết quả âm
    private boolean overflow; // Cờ Overflow: true nếu có tràn số
    private boolean carry;    // Cờ Carry: true nếu có carry-out hoặc borrow

    /**
     * Khởi tạo tất cả cờ về false.
     */
    public Flags() {
        reset();
    }

    /**
     * Cập nhật các cờ dựa trên kết quả phép toán.
     * @param result Kết quả phép toán (64-bit).
     * @param hasOverflow Có tràn số hay không.
     * @param hasCarry Có carry-out hoặc borrow hay không.
     */
    public void update(long result, boolean hasOverflow, boolean hasCarry) {
        zero = (result == 0);
        negative = (result < 0);
        overflow = hasOverflow;
        carry = hasCarry;
    }

    /**
     * Lấy trạng thái cờ Zero.
     * @return true nếu cờ Zero được đặt.
     */
    public boolean isZero() {
        return zero;
    }

    /**
     * Lấy trạng thái cờ Negative.
     * @return true nếu cờ Negative được đặt.
     */
    public boolean isNegative() {
        return negative;
    }

    /**
     * Lấy trạng thái cờ Overflow.
     * @return true nếu cờ Overflow được đặt.
     */
    public boolean isOverflow() {
        return overflow;
    }

    /**
     * Lấy trạng thái cờ Carry.
     * @return true nếu cờ Carry được đặt.
     */
    public boolean isCarry() {
        return carry;
    }

    /**
     * Đặt lại tất cả cờ về false.
     */
    public void reset() {
        zero = false;
        negative = false;
        overflow = false;
        carry = false;
    }

    /**
     * Trả về biểu diễn chuỗi của trạng thái các cờ.
     * @return Chuỗi dạng "Z=0, N=0, V=0, C=0".
     */
    @Override
    public String toString() {
        return String.format("Z=%d, N=%d, V=%d, C=%d",
                zero ? 1 : 0, negative ? 1 : 0, overflow ? 1 : 0, carry ? 1 : 0);
    }
}