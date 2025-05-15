/**
 * @author TrinhHien, DuyMy
 * @version 1.0 
 */
package core.alu;

/**
 * Alu performs basic arithmetic and logic operations on two class maths.
 */
public class ALU {

    public ALUResult compute(ALUOperation op, long operand1, long operand2) {
        long result = 0;
        boolean carry = false;
        boolean overflow = false;

        switch (op) {
            case ADD -> {
                result = operand1 + operand2;
                carry = Long.compareUnsigned(result, operand1) < 0;
                overflow = ((operand1 ^ result) & (operand2 ^ result)) < 0;
            }
            case SUB, CMP -> {
                result = operand1 - operand2;
                carry = Long.compareUnsigned(operand1, operand2) >= 0;
                overflow = ((operand1 ^ operand2) & (operand1 ^ result)) < 0;
            }
            case AND -> result = operand1 & operand2;
            case ORR -> result = operand1 | operand2;
            case EOR -> result = operand1 ^ operand2;
            case LSL -> result = operand1 << operand2;
            case LSR -> result = operand1 >>> operand2;
            case ASR -> result = operand1 >> operand2;
            case MUL -> result = operand1 * operand2;
            case PASS -> result = operand1;
        }

        boolean isCmp = (op == ALUOperation.CMP);
        return new ALUResult(
            isCmp ? 0 : result, 
            result < 0,
            result == 0,
            carry,
            overflow
        );
    }
}
