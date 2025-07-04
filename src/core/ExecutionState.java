package core;

import java.util.*;

/**
 * Represents a complete execution state that can be saved and restored.
 * This includes all CPU state, register values, memory state, and GUI visualization state.
 */
public class ExecutionState {
    // --- CPU State ---
    private final int programCounter;
    private final boolean zeroFlag;
    private final boolean negativeFlag;
    private final boolean overflowFlag;
    private final boolean carryFlag;
    private final String lastExecutedInstruction;
    private final boolean isFinished;
    private final int currentMicroStepIndex;
    
    // --- Register State ---
    private final Map<Integer, Long> registerValues;
    
    // --- Memory State (only modified addresses for efficiency) ---
    private final Map<Long, Long> modifiedMemoryValues;
    
    // --- GUI Visualization State ---
    private final List<String> activeComponents;
    private final List<String> activeBuses;
    private final Map<String, String> busDataValues;
    
    // --- Execution Context ---
    private final String stepDescription;
    private final long timestamp;
    
    /**
     * Creates a new execution state snapshot.
     */
    public ExecutionState(int programCounter, boolean zeroFlag, boolean negativeFlag, 
                         boolean overflowFlag, boolean carryFlag, String lastExecutedInstruction,
                         boolean isFinished, int currentMicroStepIndex,
                         Map<Integer, Long> registerValues, Map<Long, Long> modifiedMemoryValues,
                         List<String> activeComponents, List<String> activeBuses,
                         Map<String, String> busDataValues, String stepDescription) {
        this.programCounter = programCounter;
        this.zeroFlag = zeroFlag;
        this.negativeFlag = negativeFlag;
        this.overflowFlag = overflowFlag;
        this.carryFlag = carryFlag;
        this.lastExecutedInstruction = lastExecutedInstruction;
        this.isFinished = isFinished;
        this.currentMicroStepIndex = currentMicroStepIndex;
        this.registerValues = new HashMap<>(registerValues);
        this.modifiedMemoryValues = new HashMap<>(modifiedMemoryValues);
        this.activeComponents = new ArrayList<>(activeComponents);
        this.activeBuses = new ArrayList<>(activeBuses);
        this.busDataValues = new HashMap<>(busDataValues);
        this.stepDescription = stepDescription;
        this.timestamp = System.currentTimeMillis();
    }
    
    // --- Getters ---
    public int getProgramCounter() { return programCounter; }
    public boolean isZeroFlag() { return zeroFlag; }
    public boolean isNegativeFlag() { return negativeFlag; }
    public boolean isOverflowFlag() { return overflowFlag; }
    public boolean isCarryFlag() { return carryFlag; }
    public String getLastExecutedInstruction() { return lastExecutedInstruction; }
    public boolean isFinished() { return isFinished; }
    public int getCurrentMicroStepIndex() { return currentMicroStepIndex; }
    public Map<Integer, Long> getRegisterValues() { return new HashMap<>(registerValues); }
    public Map<Long, Long> getModifiedMemoryValues() { return new HashMap<>(modifiedMemoryValues); }
    public List<String> getActiveComponents() { return new ArrayList<>(activeComponents); }
    public List<String> getActiveBuses() { return new ArrayList<>(activeBuses); }
    public Map<String, String> getBusDataValues() { return new HashMap<>(busDataValues); }
    public String getStepDescription() { return stepDescription; }
    public long getTimestamp() { return timestamp; }
    
    /**
     * Creates a deep copy of this execution state.
     */
    public ExecutionState copy() {
        return new ExecutionState(programCounter, zeroFlag, negativeFlag, overflowFlag, carryFlag,
                                lastExecutedInstruction, isFinished, currentMicroStepIndex,
                                registerValues, modifiedMemoryValues, activeComponents, 
                                activeBuses, busDataValues, stepDescription);
    }
    
    @Override
    public String toString() {
        return String.format("ExecutionState[PC=%d, Instruction=%s, Timestamp=%d, Description=%s]",
                           programCounter, lastExecutedInstruction, timestamp, stepDescription);
    }
}
