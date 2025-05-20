package core.cpu;

import util.Constants; 

/**
 * Đại diện cho thanh ghi Program Counter (PC).
 * Quản lý địa chỉ của lệnh hiện tại trong bộ nhớ.
 */
public class ProgramCounter {
    private long pc = 0;

    /**
     * Lấy giá trị hiện tại của PC.
     * @return Giá trị PC (địa chỉ 64-bit).
     */
    public long get() {
        return pc;
    }

    /**
     * Đặt giá trị mới cho PC.
     * @param newPC Giá trị PC mới (phải không âm và căn chỉnh 4-byte).
     * @throws IllegalArgumentException nếu newPC không hợp lệ.
     */
    public void set(long newPC) {
        if (newPC < 0) {
            throw new IllegalArgumentException("PC không thể âm");
        }
        if (newPC % Constants.INSTRUCTION_SIZE != 0) {
            throw new IllegalArgumentException("PC phải căn chỉnh theo " + Constants.INSTRUCTION_SIZE + " byte");
        }
        // Kiểm tra giới hạn bộ nhớ (giả sử MAX_MEMORY_ADDRESS được định nghĩa)
        if (newPC > Constants.MAX_MEMORY_ADDRESS) {
            throw new IllegalArgumentException("PC vượt quá giới hạn bộ nhớ");
        }
        this.pc = newPC;
    }

    /**
     * Tăng PC lên một số byte.
     * @param bytes Số byte để tăng (phải không âm và là bội số của 4).
     * @throws IllegalArgumentException nếu bytes không hợp lệ.
     */
    public void increment(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Số byte không thể âm");
        }
        if (bytes % Constants.INSTRUCTION_SIZE != 0) {
            throw new IllegalArgumentException("Số byte phải là bội số của " + Constants.INSTRUCTION_SIZE);
        }
        long newPC = pc + bytes;
        if (newPC > Constants.MAX_MEMORY_ADDRESS) {
            throw new IllegalArgumentException("PC vượt quá giới hạn bộ nhớ");
        }
        this.pc = newPC;
    }

    /**
     * Tăng PC lên đúng kích thước 1 lệnh LEGv8 (4 bytes).
     */
    public void advanceInstruction() {
        increment(Constants.INSTRUCTION_SIZE); // 4 bytes
    }

    /**
     * Đặt lại PC về 0.
     */
    public void reset() {
        this.pc = 0;
    }

    /**
     * Trả về biểu diễn chuỗi của PC dưới dạng hex.
     * @return Chuỗi dạng "PC = 0x<địa chỉ>".
     */
    @Override
    public String toString() {
        return String.format("PC = 0x%016X", pc);
    }
}