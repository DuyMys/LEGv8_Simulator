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

        if (aluOp == 0 && operation == 0) { // MOVZ
            result = b;
        } else if (aluOp == 2 && operation == 2) { // ADD
            result = a + b;
            overflow = ((a > 0 && b > 0 && result < 0) || (a < 0 && b < 0 && result > 0));
        } else {
            throw new IllegalArgumentException("Unsupported ALU operation: ALUOp=" + aluOp + ", Operation=" + operation);
        }

        zero = result == 0;
        negative = result < 0;
        return new ALUResult(result, zero, negative, carry, overflow);
    }
}