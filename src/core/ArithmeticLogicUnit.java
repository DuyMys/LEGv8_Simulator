package core;

/**
 * Simulates the LEGv8 Arithmetic Logic Unit (ALU).
 */
public class ArithmeticLogicUnit {
    public static class ALUResult {
        public final long result;
        public final boolean zero;
        public final boolean negative;
        public final boolean carry;
        public final boolean overflow;

        public ALUResult(long result, boolean zero, boolean negative, boolean carry, boolean overflow) {
            this.result = result;
            this.zero = zero;
            this.negative = negative;
            this.carry = carry;
            this.overflow = overflow;
        }
    }

    public ALUResult execute(long a, long b, int aluOp, int operation) {
        long result = 0;
        boolean zero = false, negative = false, carry = false, overflow = false;

        // ALUOp=0: Immediate operations (MOVZ, LDUR, STUR, ADDI, SUBI)
        if (aluOp == 0) {
            switch (operation) {
                case 0: // MOVZ, LDUR, STUR, ADDI
                    if (b == 0) { // MOVZ
                        result = b;
                    } else { // LDUR, STUR, ADDI
                        result = a + b;
                        overflow = ((a > 0 && b > 0 && result < 0) || (a < 0 && b < 0 && result > 0));
                    }
                    break;
                case 1: // SUBI
                    result = a - b;
                    overflow = ((a > 0 && b < 0 && result < 0) || (a < 0 && b > 0 && result > 0));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported ALU operation: ALUOp=" + aluOp + ", Operation=" + operation);
            }
        }
        // ALUOp=2: Register operations (ADD, SUB, AND, ORR, EOR, MUL, SDIV, UDIV, LSL, LSR, ASR, CMP)
        else if (aluOp == 2) {
            switch (operation) {
                case 0: // AND
                    result = a & b;
                    break;
                case 1: // ORR
                    result = a | b;
                    break;
                case 2: // ADD
                    result = a + b;
                    overflow = ((a > 0 && b > 0 && result < 0) || (a < 0 && b < 0 && result > 0));
                    carry = ((a > 0 && b > 0) && (result < a || result < b));
                    break;
                case 3: // SUB
                    result = a - b;
                    overflow = ((a > 0 && b < 0 && result < 0) || (a < 0 && b > 0 && result > 0));
                    carry = (a >= b); // Carry khi a >= b trong trừ unsigned
                    break;
                case 4: // EOR
                    result = a ^ b;
                    break;
                case 5: // MUL
                    result = a * b;
                    overflow = Math.abs(result) > Long.MAX_VALUE / Math.abs(b); // Kiểm tra tràn số
                    break;
                case 6: // CMP (SUB nhưng không ghi kết quả)
                    result = a - b;
                    overflow = ((a > 0 && b < 0 && result < 0) || (a < 0 && b > 0 && result > 0));
                    carry = (a >= b);
                    break;
                case 7: // SDIV
                    if (b == 0) throw new ArithmeticException("Division by zero");
                    result = a / b; // Chia có dấu
                    break;
                case 8: // UDIV
                    if (b == 0) throw new ArithmeticException("Division by zero");
                    long unsignedA = a & 0xFFFFFFFFL;
                    long unsignedB = b & 0xFFFFFFFFL;
                    result = unsignedA / unsignedB; // Chia không dấu
                    break;
                case 9: // LSL
                    result = a << (int) b; // Dịch trái logic
                    break;
                case 10: // LSR
                    result = a >>> (int) b; // Dịch phải logic
                    break;
                case 11: // ASR
                    result = a >> (int) b; // Dịch phải giữ dấu
                    break;
                
                default:
                    throw new IllegalArgumentException("Unsupported ALU operation: ALUOp=" + aluOp + ", Operation=" + operation);
            }
        } else {
            throw new IllegalArgumentException("Unsupported ALUOp: " + aluOp);
        }

        // Cập nhật cờ
        zero = (result == 0);
        negative = (result < 0);
        // Carry: Chỉ tính cho ADD, SUB, CMP (giả sử unsigned)
        if ((aluOp == 2 && (operation == 2 || operation == 3)) || (aluOp == 0 && operation == 1)) {
            carry = (a >>> 63) != (b >>> 63) && (result >>> 63) == (b >>> 63);
        }
        // Overflow: Đã tính trong từng phép toán

        return new ALUResult(result, zero, negative, carry, overflow);
    }
}