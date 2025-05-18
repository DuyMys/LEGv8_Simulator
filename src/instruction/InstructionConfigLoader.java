package instruction;

import util.ControlSignals;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import util.ColoredLog;

/**
 * InstructionConfigLoader loads and manages instruction definitions for the LEGv8 architecture.
 * It maintains two maps: one for opcode-based lookup and one for mnemonic-based lookup.
 */
public class InstructionConfigLoader {
    private final Map<String, Map<Character, InstructionDefinition>> opcodeDefinitions;
    private final Map<String, InstructionDefinition> mnemonicDefinitions;

    /**
     * Constructor for InstructionConfigLoader.
     */
    public InstructionConfigLoader() {
        this.opcodeDefinitions = new HashMap<>();
        this.mnemonicDefinitions = new HashMap<>();
    }

    /**
     * Loads instruction definitions from a resource file.
     * @param resourcePath The path to the configuration file.
     * @throws RuntimeException if the file cannot be loaded or parsed.
     */
    public void loadConfig(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",");
                if (parts.length != 15) {
                    System.err.println(ColoredLog.WARNING + "Invalid config line: " + line);
                    continue;
                }

                String mnemonic = parts[0].trim().toUpperCase();
                char format = parts[1].trim().charAt(0);
                String opcodeId = parts[2].trim();
                ControlSignals signals = new ControlSignals(
                    Boolean.parseBoolean(parts[3].trim()), // RegW
                    Boolean.parseBoolean(parts[4].trim()), // ALUsrc
                    Boolean.parseBoolean(parts[5].trim()), // MemW
                    Boolean.parseBoolean(parts[6].trim()), // MemR
                    Boolean.parseBoolean(parts[7].trim()), // MemToReg
                    Boolean.parseBoolean(parts[8].trim()), // ZeroB
                    Boolean.parseBoolean(parts[9].trim()), // FlagB
                    Boolean.parseBoolean(parts[10].trim()), // UncondB
                    Boolean.parseBoolean(parts[11].trim()), // Reg2Loc
                    Boolean.parseBoolean(parts[12].trim()), // FlagW
                    parts[13].trim(),                     // ALUOp
                    parts[14].trim()                      // ALUOperation
                );

                InstructionDefinition def = new InstructionDefinition(mnemonic, format, opcodeId, signals);
                opcodeDefinitions.computeIfAbsent(opcodeId, k -> new HashMap<>()).put(format, def);
                mnemonicDefinitions.put(mnemonic, def);
            }
        } catch (Exception e) {
            throw new RuntimeException(ColoredLog.ERROR + "Failed to load instruction config: " + e.getMessage());
        }
    }

    /**
     * Retrieves an InstructionDefinition based on opcode ID and format.
     * @param opcodeId The opcode identifier string (in hexadecimal).
     * @param format The instruction format.
     * @return The corresponding InstructionDefinition, or null if not found.
     */
    public InstructionDefinition getDefinitionByOpcode(String opcodeId, char format) {
        Map<Character, InstructionDefinition> formatMap = opcodeDefinitions.get(opcodeId);
        return formatMap != null ? formatMap.get(format) : null;
    }

    /**
     * Retrieves an InstructionDefinition based on mnemonic.
     * @param mnemonic The instruction mnemonic.
     * @return The corresponding InstructionDefinition, or null if not found.
     */
    public InstructionDefinition getDefinitionByMnemonic(String mnemonic) {
        return mnemonicDefinitions.get(mnemonic.toUpperCase());
    }

    /**
     * Retrieves the detailed definition map.
     * @return A copy of the opcode-based definition map.
     */
    public Map<String, Map<Character, InstructionDefinition>> getDefinitionMap() {
        return new HashMap<>(opcodeDefinitions);
    }
}