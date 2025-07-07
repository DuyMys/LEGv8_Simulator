package util;
/**
 * Represents the control signals for a LEGv8 instruction.
 */
public class ControlSignals {
    private final boolean reg2Loc;
    private final boolean uncondBranch;
    private final boolean flagBranch;
    private final boolean zeroBranch;
    private final boolean memRead;
    private final boolean memToReg;
    private final boolean memWrite;
    private final boolean flagWrite;
    private final boolean aluSrc;
    private final int aluOp;
    private final boolean regWrite;
    private final int operation;

    public ControlSignals(boolean reg2Loc, boolean uncondBranch, boolean flagBranch, boolean zeroBranch,
                         boolean memRead, boolean memToReg, boolean memWrite, boolean flagWrite,
                         boolean aluSrc, int aluOp, boolean regWrite, int operation) {
        this.reg2Loc = reg2Loc;
        this.uncondBranch = uncondBranch;
        this.flagBranch = flagBranch;
        this.zeroBranch = zeroBranch;
        this.memRead = memRead;
        this.memToReg = memToReg;
        this.memWrite = memWrite;
        this.flagWrite = flagWrite;
        this.aluSrc = aluSrc;
        this.aluOp = aluOp;
        this.regWrite = regWrite;
        this.operation = operation;
    }

    public boolean isReg2Loc() { return reg2Loc; }
    public boolean isUncondBranch() { return uncondBranch; }
    public boolean isFlagBranch() { return flagBranch; }
    public boolean isZeroBranch() { return zeroBranch; }
    public boolean isMemRead() { return memRead; }
    public boolean isMemToReg() { return memToReg; }
    public boolean isMemWrite() { return memWrite; }
    public boolean isFlagWrite() { return flagWrite; }
    public boolean isAluSrc() { return aluSrc; }
    public int getAluOp() { return aluOp; }
    public boolean isRegWrite() { return regWrite; }
    public int getOperation() { return operation; }

    @Override
    public String toString() {
        return String.format("ControlSignals[Reg2Loc=%b, UncondB=%b, FlagB=%b, ZeroB=%b, MemR=%b, MemToReg=%b, MemW=%b, FlagW=%b, ALUSrc=%b, ALUOp=%d, RegW=%b, Operation=%d]",
                reg2Loc, uncondBranch, flagBranch, zeroBranch, memRead, memToReg, memWrite, flagWrite, aluSrc, aluOp, regWrite, operation);
    }
}