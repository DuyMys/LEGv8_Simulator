package instruction;

import java.util.BitSet;

/**
 * RFormatInstruction is a class that represents a register format instruction
 * in the LEGv8 architecture.
 * It extends the Instruction class and provides methods to disassemble the
 * instruction and extract its components.
 */
public class RFormatInstruction extends Instruction {
    private final int rd, rn, rm, shamt;

    // --- Constructor ---
    /**
     * Constructor for RFormatInstruction.
     * 
     * @param bytecode   The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public RFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.rm = getRm_R();
        this.shamt = getShamt_R();
        this.rn = getRn_R();
        this.rd = getRd_R();
    }

    // --- Instruction Methods ---
    /**
     * @return The instruction as assembled string.
     *         The string is formatted as "mnemonic Xn, Xm, Xn" or "mnemonic Xn, Xn,
     *         #immediate".
     */
    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();

        switch (mnemonic) {
            case "LSL":
            case "LSR":
            case "ASR":
                return String.format("%-6s X%d, X%d, #%d", mnemonic, rd, rn, shamt);
            case "BR":
                return String.format("%-6s X%d", mnemonic, rn);
            case "ADD":
            case "ADDS":
            case "SUB":
            case "SUBS":
            case "AND":
            case "ANDS":
            case "ORR":
            case "EOR":
            case "MUL":
            case "SMULH":
            case "UMULH":
            case "SDIV":
            case "UDIV":
                return String.format("%-6s X%d, X%d, X%d", mnemonic, rd, rn, rm);
            case "CMP":
                return String.format("%-6s X%d, X%d", mnemonic, rn, rm);
            default:
                return String.format("%-6s X%d, X%d, X%d ; (shamt=%d)", mnemonic, rd, rn, rm, shamt);
        }
    }

    /**
     * @return The shift amount for the instruction.
     */
    public int getShift() {
        return shamt;
    }
}