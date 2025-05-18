package util;

/**
 * ControlSignals is a record that represents the control signals used in the LEGv8 architecture.
 * It contains various boolean flags and string values that control the behavior of the processor.
 */
public record ControlSignals(
    boolean regWrite,
    boolean aluSrc,
    boolean memWrite,
    boolean memRead,
    boolean memToReg,
    boolean zeroBranch,
    boolean flagBranch,
    boolean uncondBranch,
    boolean reg2Loc,
    boolean flagWrite,
    String aluOp,
    String operation
) {
    public static final ControlSignals NOP = new ControlSignals(
        false, false, false, false, false,
        false, false, false, false, false,
        "00", "NOP"
    );

    public static final ControlSignals HALT = new ControlSignals(
        true, true, true, true, true,
        true, true, true, true, true,
        "00", "HALT"
    );

    @Override
    public String toString() {
        return String.format(
            "ControlSignals [regWrite=%b, aluSrc=%b, memWrite=%b, memRead=%b, memToReg=%b, " +
            "zeroBranch=%b, flagBranch=%b, uncondBranch=%b, reg2Loc=%b, flagWrite=%b, " +
            "aluOp=%s, operation=%s]",
            regWrite, aluSrc, memWrite, memRead, memToReg,
            zeroBranch, flagBranch, uncondBranch, reg2Loc, flagWrite,
            aluOp, operation
        );
    }
}