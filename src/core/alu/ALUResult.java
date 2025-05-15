/**
 * @author TrinhHien, DuyMy
 * @version 1.0 
 */
package legv8.core.alu;

/**
 * ALUResult represents the result of an Arithmetic Logic Unit (ALU) operation.
 * It contains the result and the status flags: Negative (N), Zero (Z), Carry (C), Overflow (V).
 */
public record ALUResult(
    long result,
    boolean negativeFlag,
    boolean zeroFlag,
    boolean carryFlag,
    boolean overflowFlag
) {
    public static ALUResult from(long result, boolean carryOut, boolean overflow) {
        return new ALUResult(
            result,
            result < 0,
            result == 0,
            carryOut,
            overflow
        );
    }

    @Override
    public String toString() {
        return String.format("Res=0x%016X (N=%b Z=%b C=%b V=%b)",
                             result, negativeFlag, zeroFlag, carryFlag, overflowFlag);
    }
}
