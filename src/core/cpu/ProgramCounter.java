package core.cpu;

/**
 * Đại diện cho thanh ghi Program Counter (PC).
 * Quản lý địa chỉ của lệnh hiện tại trong bộ nhớ.
 */
public class ProgramCounter {
    private long pc = 0;

    public long get() {
        return pc;
    }

    public void set(long newPC) {
        if (newPC < 0) throw new IllegalArgumentException("PC khong am");
        this.pc = newPC;
    }

    public void increment(int bytes) {
        if (bytes < 0) throw new IllegalArgumentException("Khong tang neu am");
        this.pc += bytes;
    }

    /** Tăng PC lên đúng kích thước 1 lệnh LEGv8 (4 bytes) */
    public void advanceInstruction() {
        increment(4);
    }

    public void reset() {
        this.pc = 0;
    }

    @Override
    public String toString() {
        return String.format("PC = 0x%016X", pc);
    }
}
