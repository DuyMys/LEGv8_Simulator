package core;
import instruction.*;
import util.*;
//import java.util.Map;

/**
 * Simulates the LEGv8 Control Unit, generating control signals for an instruction.
 */
/**
 * Simulates the LEGv8 Control Unit.
 */
public class ControlUnit {
    private final InstructionConfigLoader configLoader;
    private ControlSignals currentControlSignals;

    public ControlUnit(InstructionConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public ControlSignals generateControlSignals(Instruction instruction) {
        return instruction.getDefinition().getControlSignals();
    }
        /**
     * Sets the current control signals for this control unit.
     * This method is used during micro-step execution to update the control unit's state
     * for visualization and debugging purposes.
     * 
     * @param signals The control signals to set
     */
    public void setControlSignals(ControlSignals signals) {
        this.currentControlSignals = signals;
    }

    /**
     * Gets the current control signals.
     * 
     * @return The current control signals, or null if none have been set
     */
    public ControlSignals getCurrentControlSignals() {
        return currentControlSignals;
    }

    /**
     * Clears the current control signals.
     */
    public void clearControlSignals() {
        this.currentControlSignals = null;
    }
    
}