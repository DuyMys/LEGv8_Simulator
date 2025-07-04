package instruction;
import java.util.BitSet;

/**
 * Represents an IM-format instruction in the LEGv8 architecture (e.g., MOVZ).
 */
public class IMFormatInstruction extends Instruction {
    private final int rd;
    private final int immediate;
    private final int shift;

    /**
     * Constructor for IMFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public IMFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.rd = getRd_IM();
        this.immediate = getImmediate_IM();
        this.shift = getShift_IM();
    }

    /**
     * Disassembles the instruction into a human-readable format.
     * @return The disassembled instruction as a string (e.g., "MOVZ Xd, #imm, LSL #shift").
     */
    @Override
    public String disassemble() {
        return String.format("%-6s X%d, #%d, LSL #%d", 
                            definition.getMnemonic(), rd, immediate, shift * 16);
    }
}