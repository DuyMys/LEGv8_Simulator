package instruction;
import util.ColoredLog;

import java.util.BitSet;

/**
 * InstructionFactory is a factory class responsible for creating instruction objects from bytecode or assembly lines.
 * It uses an InstructionConfigLoader to load instruction definitions and formats.
 */
public class InstructionFactory {
    private final InstructionConfigLoader configLoader;

    /**
     * Constructor for InstructionFactory.
     * @param configLoader The InstructionConfigLoader to use for retrieving instruction definitions.
     */
    public InstructionFactory(InstructionConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    /**
     * Creates an Instruction object from a given bytecode.
     * @param bytecode The bytecode as a BitSet.
     * @return The corresponding Instruction object.
     * @throws IllegalArgumentException if the bytecode is invalid or no matching instruction definition is found.
     */
    public Instruction createFromBytecode(BitSet bytecode) {
        if (bytecode == null) {
            throw new IllegalArgumentException(ColoredLog.WARNING + "Bytecode cannot be null.");
        }

        // Extract opcode bits based on format
        int opcodeBits = Instruction.extractBits(bytecode, 21, 31); // Default for R and D formats
        char format = determineFormat(bytecode);
        switch (format) {
            case 'I':
                opcodeBits = Instruction.extractBits(bytecode, 22, 31);
                break;
            case 'B':
                opcodeBits = Instruction.extractBits(bytecode, 26, 31);
                break;
            // case "CB":
            //     opcodeBits = Instruction.extractBits(bytecode, 24, 31);
            //     break;
            // case "IM":
            //     opcodeBits = Instruction.extractBits(bytecode, 23, 31);
            //     break;
        }

        String opcodeId = String.format("%X", opcodeBits);
        InstructionDefinition def = configLoader.getDefinitionByOpcode(opcodeId, format);

        if (def == null) {
            throw new IllegalArgumentException(ColoredLog.WARNING + "No instruction definition found for opcode " + opcodeId + " and format " + format);
        }

        switch (format) {
            case 'R':
                return new RFormatInstruction(bytecode, def);
            // case 'I':
            //     return new IFormatInstruction(bytecode, def);
            // case 'D':
            //     return new DFormatInstruction(bytecode, def);
            // case 'B':
            //     return new BFormatInstruction(bytecode, def);
            // case "CB":
            //     return new CBFormatInstruction(bytecode, def);
            // case "IM":
            //     return new IMFormatInstruction(bytecode, def);
            default:
                throw new IllegalArgumentException(ColoredLog.WARNING + "Unsupported instruction format: " + format);
        }
    }

    /**
     * Creates an Instruction object from an assembly line.
     * @param assemblyLine The assembly instruction as a string (e.g., "ADD X0, X1, X2").
     * @return The corresponding Instruction object.
     * @throws IllegalArgumentException if the assembly line is invalid or no matching instruction definition is found.
     */
    public Instruction createFromAssembly(String assemblyLine) {
        if (assemblyLine == null || assemblyLine.trim().isEmpty()) {
            throw new IllegalArgumentException(ColoredLog.WARNING + "Assembly line cannot be null or empty.");
        }

        String[] parts = assemblyLine.trim().toUpperCase().split("\\s+");
        if (parts.length == 0) {
            throw new IllegalArgumentException(ColoredLog.WARNING + "Invalid assembly line: " + assemblyLine);
        }

        String mnemonic = parts[0];
        InstructionDefinition def = configLoader.getDefinitionByMnemonic(mnemonic);
        if (def == null) {
            throw new IllegalArgumentException(ColoredLog.WARNING + "No instruction definition found for mnemonic: " + mnemonic);
        }

        BitSet bytecode = assembleBytecode(def, parts);
        return createFromBytecode(bytecode);
    }

    /**
     * Determines the format of an instruction based on its bytecode.
     * @param bytecode The instruction bytecode.
     * @return The format character (R, I, D, B, CB, IM).
     */
    private char determineFormat(BitSet bytecode) {
        int opcode = Instruction.extractBits(bytecode, 21, 31); // Start with largest possible opcode field
        String opcodeId = String.format("%X", opcode);

        // Check all formats to find a match
        for (char format : new char[] {'R', 'I', 'D', 'B'}) {
            if (configLoader.getDefinitionByOpcode(opcodeId, format) != null) {
                return format;
            }
        }

        // Try format-specific opcode extractions
        opcode = Instruction.extractBits(bytecode, 22, 31); // I-format
        opcodeId = String.format("%X", opcode);
        if (configLoader.getDefinitionByOpcode(opcodeId, 'I') != null) {
            return 'I';
        }

        opcode = Instruction.extractBits(bytecode, 26, 31); // B-format
        opcodeId = String.format("%X", opcode);
        if (configLoader.getDefinitionByOpcode(opcodeId, 'B') != null) {
            return 'B';
        }

        // opcode = Instruction.extractBits(bytecode, 24, 31); // CB-format
        // opcodeId = String.format("%X", opcode);
        // if (configLoader.getDefinitionByOpcode(opcodeId, "CB") != null) {
        //     return "CB";
        // }

        // opcode = Instruction.extractBits(bytecode, 23, 31); // IM-format
        // opcodeId = String.format("%X", opcode);
        // if (configLoader.getDefinitionByOpcode(opcodeId, "IM") != null) {
        //     return "IM";
        // }

        throw new IllegalArgumentException(ColoredLog.WARNING + "Unable to determine instruction format for bytecode: " + Instruction.formatBitSet(bytecode));
    }

    /**
     * Assembles a bytecode from an assembly line based on the instruction definition.
     * @param def The InstructionDefinition for the instruction.
     * @param parts The parsed assembly line components.
     * @return The assembled bytecode as a BitSet.
     */
    private BitSet assembleBytecode(InstructionDefinition def, String[] parts) {
        BitSet bytecode = new BitSet(32);
        char format = def.getFormat();
        int opcodeValue = def.getOpcodeIdentifierValue();

        // Set opcode bits based on format
        switch (format) {
            case 'R':
                Instruction.setBits(bytecode, opcodeValue, 21, 31);
                if (parts.length >= 4) {
                    Instruction.setBits(bytecode, parseRegister(parts[1]), 0, 4);  // Rd
                    Instruction.setBits(bytecode, parseRegister(parts[2]), 5, 9);  // Rn
                    Instruction.setBits(bytecode, parseRegister(parts[3]), 16, 20); // Rm
                    if (def.getMnemonic().equals("LSL") || def.getMnemonic().equals("LSR")) {
                        Instruction.setBits(bytecode, parseImmediate(parts[3]), 10, 15); // Shamt
                    }
                }
                break;
            case 'I':
                Instruction.setBits(bytecode, opcodeValue, 22, 31);
                if (parts.length >= 4) {
                    Instruction.setBits(bytecode, parseRegister(parts[1]), 0, 4);  // Rd
                    Instruction.setBits(bytecode, parseRegister(parts[2]), 5, 9);  // Rn
                    Instruction.setBits(bytecode, parseImmediate(parts[3]), 10, 21); // Immediate
                }
                break;
            case 'D':
                Instruction.setBits(bytecode, opcodeValue, 21, 31);
                if (parts.length >= 3) {
                    Instruction.setBits(bytecode, parseRegister(parts[1]), 0, 4);  // Rt
                    Instruction.setBits(bytecode, parseRegister(parts[2]), 5, 9);  // Rn
                    Instruction.setBits(bytecode, parseImmediate(parts[2]), 12, 20); // Address (offset)
                }
                break;
            case 'B':
                Instruction.setBits(bytecode, opcodeValue, 26, 31);
                if (parts.length >= 2) {
                    Instruction.setBits(bytecode, parseImmediate(parts[1]), 0, 25); // Address
                }
                break;
            // case 'CB':
            //     Instruction.setBits(bytecode, opcodeValue, 24, 31);
            //     if (parts.length >= 3) {
            //         Instruction.setBits(bytecode, parseRegister(parts[1]), 0, 4);  // Rt
            //         Instruction.setBits(bytecode, parseImmediate(parts[2]), 5, 23); // Address
            //     }
            //     break;
            // case 'IM':
            //     Instruction.setBits(bytecode, opcodeValue, 23, 31);
            //     if (parts.length >= 3) {
            //         Instruction.setBits(bytecode, parseRegister(parts[1]), 0, 4);  // Rd
            //         Instruction.setBits(bytecode, parseImmediate(parts[2]), 5, 20); // Immediate
            //         Instruction.setBits(bytecode, parseShift(parts[2]), 21, 22);   // Shift
            //     }
            //     break;
            default:
                throw new IllegalArgumentException(ColoredLog.WARNING + "Unsupported format for assembly: " + format);
        }

        return bytecode;
    }

    /**
     * Parses a register identifier (e.g., "X0" or "R0") to its number.
     * @param reg The register string.
     * @return The register number.
     */
    private int parseRegister(String reg) {
        if (reg == null || !reg.matches("[XR][0-3][0-1]?")) {
            throw new IllegalArgumentException(ColoredLog.WARNING + "Invalid register: " + reg);
        }
        return Integer.parseInt(reg.replaceAll("[XR]", ""));
    }

    /**
     * Parses an immediate value (e.g., "#42" or "42").
     * @param imm The immediate string.
     * @return The immediate value.
     */
    private int parseImmediate(String imm) {
        if (imm == null) {
            throw new IllegalArgumentException(ColoredLog.WARNING + "Invalid immediate value: null");
        }
        return Integer.parseInt(imm.replaceAll("[#\\[\\]]", ""));
    }

    /**
     * Parses a shift value for IM-format instructions.
     * @param imm The immediate string (or shift specification).
     * @return The shift value (0 if not specified).
     */
    private int parseShift(String imm) {
        // Simplified: Assume shift is part of immediate or defaults to 0
        return 0; // Adjust based on specific IM-format requirements
    }
}