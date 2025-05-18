package instruction;

import java.util.BitSet;
import util.ColoredLog;


/**
 * RFormatInstruction represents an R-format instruction in the LEGv8 architecture.
 * It extends the Instruction class and provides specific handling for R-format instructions.
 */
public class RFormatInstruction extends Instruction {

    /**
     * Constructor for RFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public RFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        if (definition.getFormat() != 'R') {
            throw new IllegalArgumentException(ColoredLog.WARNING + "InstructionDefinition must be of R-format for RFormatInstruction.");
        }
    }

    /**
     * Disassembles the R-format instruction into a human-readable assembly string.
     * @return The disassembled instruction as a string.
     */
    @Override
    public String disassemble() {
        StringBuilder sb = new StringBuilder();
        sb.append(definition.getMnemonic()).append(" ");

        // Get register fields
        int rd = getRd_R();  // Destination register (bits 0-4)
        int rn = getRn_R();  // First source register (bits 5-9)
        int rm = getRm_R();  // Second source register (bits 16-20)
        int shamt = getShamt_R(); // Shift amount (bits 10-15, used in LSL/LSR)

        // Format registers as X0, X1, etc.
        sb.append("X").append(rd).append(", ");
        sb.append("X").append(rn);

        // For instructions like LSL/LSR, include shamt; otherwise, include Rm
        if (definition.getMnemonic().equals("LSL") || definition.getMnemonic().equals("LSR")) {
            sb.append(", #").append(shamt);
        } else {
            sb.append(", X").append(rm);
        }

        return sb.toString();
    }
}