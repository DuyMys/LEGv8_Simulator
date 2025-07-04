package instruction;
import util.ControlSignals;

import java.util.Objects;
import java.util.BitSet;

/**
 * Represents the definition of an instruction in the LEGv8 architecture.
 */
public class InstructionDefinition {
    private final String mnemonic;
    private final char format;
    private final String opcodeId;
    private final ControlSignals controlSignals;

    /**
     * Constructor for InstructionDefinition.
     * @param mnemonic The mnemonic of the instruction (e.g., "ADD", "MOVZ").
     * @param format The format of the instruction (e.g., 'R', 'M').
     * @param opcodeId The opcode ID as a binary string (e.g., "10001011000").
     * @param controlSignals The control signals for the instruction.
     */
    public InstructionDefinition(String mnemonic, char format, String opcodeId, ControlSignals controlSignals) {
        this.mnemonic = Objects.requireNonNull(mnemonic, "Mnemonic cannot be null.");
        this.format = format;
        this.opcodeId = Objects.requireNonNull(opcodeId, "Opcode ID cannot be null.");
        this.controlSignals = Objects.requireNonNull(controlSignals, "Control signals cannot be null.");
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public char getFormat() {
        return format;
    }

    public String getOpcodeId() {
        return opcodeId;
    }

    public ControlSignals getControlSignals() {
        return controlSignals;
    }

    /**
     * Checks if the provided opcode matches this instruction's opcode.
     * @param bytecode The instruction bytecode to check.
     * @return True if the opcode matches, false otherwise.
     */
    public boolean matchesOpcode(BitSet bytecode) {
        int opcodeLength = opcodeId.length();
        int extractedOpcode = Instruction.extractBits(bytecode, 31 - opcodeLength + 1, 31);
        String extractedOpcodeStr = Integer.toBinaryString(extractedOpcode);
        while (extractedOpcodeStr.length() < opcodeLength) {
            extractedOpcodeStr = "0" + extractedOpcodeStr;
        }
        return opcodeId.equals(extractedOpcodeStr);
    }

    public String getInstructionName() {
        return mnemonic;
    }

    @Override
    public String toString() {
        return String.format("InstructionDefinition[mnemonic=%s, format=%c, opcodeId=%s, controlSignals=%s]",
                mnemonic, format, opcodeId, controlSignals);
    }
}
