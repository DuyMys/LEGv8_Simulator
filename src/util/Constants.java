package util;

public class Constants {
    // Kích thước của một lệnh LEGv8 (byte)
    public static final int INSTRUCTION_SIZE = 4;

    // Số lượng thanh ghi trong LEGv8
    public static final int NUM_REGISTERS = 32;

    // Địa chỉ bộ nhớ tối đa (ví dụ: 64KB)
    public static final long MAX_MEMORY_ADDRESS = 0xFFFF;

    // Thanh ghi XZR (Zero Register)
    public static final int ZERO_REGISTER = 31;

    // Các giá trị opcode mẫu (tùy thuộc vào cách bạn định nghĩa lệnh)
    public static final int OPCODE_ADD = 0x458; // Opcode cho ADD (hex)
    public static final int OPCODE_SUB = 0x658; // Opcode cho SUB
    public static final int OPCODE_LDUR = 0x7C2; // Opcode cho LDUR
    public static final int OPCODE_STUR = 0x7C0; // Opcode cho STUR
    public static final int OPCODE_B = 0x140; // Opcode cho Branch

    // Kích thước bộ nhớ (byte, ví dụ)
    public static final int MEMORY_SIZE = 64 * 1024; // 64KB

    // Ngăn khởi tạo class này
    private Constants() {
        throw new AssertionError("Utility class - cannot instantiate");
    }
}
