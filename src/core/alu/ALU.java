/**
 * @author TrinhHien, DuyMy
 * @version 1.0 
 */
package core.alu;

/**
 * ALU performs basic arithmetic and logic operations on two operands.
 */
public class ALU {
    private boolean carryFlag;
    private boolean overflowFlag;

    /**
     * Performs addition on two 64-bit operands.
     * @param operand1 First operand.
     * @param operand2 Second operand.
     * @return ALUResult containing result and flags.
     */
    public ALUResult add(long operand1, long operand2) {
        long result = operand1 + operand2;
        carryFlag = Long.compareUnsigned(result, operand1) < 0;
        overflowFlag = ((operand1 ^ result) & (operand2 ^ result)) < 0;
        return new ALUResult(
            result,
            result < 0,
            result == 0,
            carryFlag,
            overflowFlag
        );
    }

    /**
     * Performs subtraction on two 64-bit operands.
     * @param operand1 First operand.
     * @param operand2 Second operand.
     * @return ALUResult containing result and flags.
     */
    public ALUResult subtract(long operand1, long operand2) {
        long result = operand1 - operand2;
        carryFlag = Long.compareUnsigned(operand1, operand2) >= 0;
        overflowFlag = ((operand1 ^ operand2) & (operand1 ^ result)) < 0;
        return new ALUResult(
            result,
            result < 0,
            result == 0,
            carryFlag,
            overflowFlag
        );
    }

    /**
     * Performs logical AND on two 64-bit operands.
     * @param operand1 First operand.
     * @param operand2 Second operand.
     * @return ALUResult containing result and flags.
     */
    public ALUResult and(long operand1, long operand2) {
        long result = operand1 & operand2;
        carryFlag = false;
        overflowFlag = false;
        return new ALUResult(
            result,
            result < 0,
            result == 0,
            carryFlag,
            overflowFlag
        );
    }

    /**
     * Performs logical OR on two 64-bit operands.
     * @param operand1 First operand.
     * @param operand2 Second operand.
     * @return ALUResult containing result and flags.
     */
    public ALUResult or(long operand1, long operand2) {
        long result = operand1 | operand2;
        carryFlag = false;
        overflowFlag = false;
        return new ALUResult(
            result,
            result < 0,
            result == 0,
            carryFlag,
            overflowFlag
        );
    }

    /**
     * Performs logical shift left on operand1 by operand2 bits.
     * @param operand1 Value to shift.
     * @param operand2 Number of bits to shift.
     * @return ALUResult containing result and flags.
     */
    public ALUResult shiftLeft(long operand1, long operand2) {
        long result = operand1 << operand2;
        carryFlag = (operand2 > 0) && ((operand1 & (1L << (64 - operand2))) != 0);
        overflowFlag = false;
        return new ALUResult(
            result,
            result < 0,
            result == 0,
            carryFlag,
            overflowFlag
        );
    }

    /**
     * Performs logical shift right on operand1 by operand2 bits.
     * @param operand1 Value to shift.
     * @param operand2 Number of bits to shift.
     * @return ALUResult containing result and flags.
     */
    public ALUResult shiftRight(long operand1, long operand2) {
        long result = operand1 >>> operand2;
        carryFlag = (operand2 > 0) && ((operand1 & (1L << (operand2 - 1))) != 0);
        overflowFlag = false;
        return new ALUResult(
            result,
            result < 0,
            result == 0,
            carryFlag,
            overflowFlag
        );
    }

    /**
     * Checks if the last operation caused an overflow.
     * @return True if overflow occurred.
     */
    public boolean hasOverflow() {
        return overflowFlag;
    }

    /**
     * Checks if the last operation caused a carry.
     * @return True if carry occurred.
     */
    public boolean hasCarry() {
        return carryFlag;
    }

    /**
     * Legacy compute method for compatibility (optional).
     * @param op ALU operation to perform.
     * @param operand1 First operand.
     * @param operand2 Second operand.
     * @return ALUResult containing result and flags.
     */
    public ALUResult compute(ALUOperation op, long operand1, long operand2) {
        switch (op) {
            case ADD:
                return add(operand1, operand2);
            case SUB:
                return subtract(operand1, operand2);
            case AND:
                return and(operand1, operand2);
            case ORR:
                return or(operand1, operand2);
            case LSL:
                return shiftLeft(operand1, operand2);
            case LSR:
                return shiftRight(operand1, operand2);
            default:
                throw new IllegalArgumentException("Unsupported ALUOperation: " + op);
        }
    }
}