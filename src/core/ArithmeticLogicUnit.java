package core;

import java.util.EnumSet;

/**
 * Simulates the LEGv8 Arithmetic Logic Unit (ALU).
 */
public class ArithmeticLogicUnit {

    // Enum để định nghĩa các phép toán của ALU một cách rõ ràng
    public enum ALUOperation {
        ADD, SUB, AND, ORR, EOR,
        MUL, SMULH, UMULH,
        SDIV, UDIV,
        LSL, LSR, ASR,
        PASS_B // Dùng cho các lệnh như MOVZ, ADDI, nơi chỉ cần chuyển giá trị thứ hai
    }

    public static class ALUResult {
        public final long result;
        public final boolean n; // Negative flag
        public final boolean z; // Zero flag
        public final boolean c; // Carry flag
        public final boolean v; // Overflow flag

        public ALUResult(long result, boolean n, boolean z, boolean c, boolean v) {
            this.result = result;
            this.n = n;
            this.z = z;
            this.c = c;
            this.v = v;
        }
    }

    // Các phép toán cần tính cờ Carry và Overflow
    private static final EnumSet<ALUOperation> FLAG_AFFECTING_ARITHMETIC = EnumSet.of(ALUOperation.ADD, ALUOperation.SUB);

    public ALUResult execute(long a, long b, ALUOperation op) {
        long result = 0;

        switch (op) {
            case ADD:
                result = a + b;
                break;
            case SUB:
                result = a - b;
                break;
            case AND:
                result = a & b;
                break;
            case ORR:
                result = a | b;
                break;
            case EOR:
                result = a ^ b;
                break;
            case MUL:
                result = a * b;
                break;
            case SMULH:
                // Signed multiply high: return upper 64 bits of 128-bit result
                result = Math.multiplyHigh(a, b);
                break;
            case UMULH:
                // Unsigned multiply high: return upper 64 bits of 128-bit result
                // For unsigned multiplication, we can use BigInteger for accuracy
                java.math.BigInteger bigA = new java.math.BigInteger(Long.toUnsignedString(a));
                java.math.BigInteger bigB = new java.math.BigInteger(Long.toUnsignedString(b));
                java.math.BigInteger product = bigA.multiply(bigB);
                result = product.shiftRight(64).longValue();
                break;
            case SDIV:
                if (b == 0) throw new ArithmeticException("Division by zero");
                result = a / b;
                break;
            case UDIV: // SỬA LỖI: Sử dụng phép chia 64-bit không dấu
                if (b == 0) throw new ArithmeticException("Division by zero");
                result = Long.divideUnsigned(a, b);
                break;
            case LSL:
                result = a << b;
                break;
            case LSR:
                result = a >>> b;
                break;
            case ASR:
                result = a >> b;
                break;
            case PASS_B: // Dùng cho MOVZ, ADDI (trong trường hợp rn=XZR)
                result = b;
                break;
            default:
                throw new IllegalArgumentException("Unsupported ALU operation: " + op);
        }

        // --- Cập nhật cờ trạng thái (Flags) ---
        boolean n_flag = (result < 0);
        boolean z_flag = (result == 0);
        boolean c_flag = false;
        boolean v_flag = false;

        // C và V chỉ được tính cho các phép toán số học nhất định
        if (FLAG_AFFECTING_ARITHMETIC.contains(op)) {
            if (op == ALUOperation.ADD) {
                // Carry: xảy ra khi tổng không dấu nhỏ hơn toán hạng ban đầu
                c_flag = Long.compareUnsigned(result, a) < 0;
                // Overflow: xảy ra khi dấu của 2 toán hạng giống nhau và khác dấu kết quả
                v_flag = ((a ^ result) & (b ^ result)) < 0;
            } else if (op == ALUOperation.SUB) {
                // Carry (not-borrow): xảy ra khi a >= b (không dấu)
                c_flag = Long.compareUnsigned(a, b) >= 0;
                // Overflow: xảy ra khi dấu của a và b khác nhau, và dấu kết quả giống b
                v_flag = ((a ^ b) & (a ^ result)) < 0;
            }
        }

        return new ALUResult(result, n_flag, z_flag, c_flag, v_flag);
    }
}