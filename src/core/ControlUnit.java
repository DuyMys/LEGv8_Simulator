
import instruction.*;
import util.*;
import java.util.Map;

/**
 * Simulates the LEGv8 Control Unit, generating control signals for an instruction.
 */
/**
 * Simulates the LEGv8 Control Unit.
 */
public class ControlUnit {
    private final InstructionConfigLoader configLoader;

    public ControlUnit(InstructionConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public ControlSignals generateControlSignals(Instruction instruction) {
        return instruction.getDefinition().getControlSignals();
    }
}