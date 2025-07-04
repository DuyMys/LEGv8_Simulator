package core;

/**
 * Helper class to facilitate integration between CPU simulator and DatapathPanel
 * for execution history management.
 */
public class ExecutionStateBuilder {
    private int programCounter = 0;
    private boolean zeroFlag = false;
    private boolean negativeFlag = false;
    private boolean overflowFlag = false;
    private boolean carryFlag = false;
    private String lastExecutedInstruction = "None";
    private boolean isFinished = false;
    private int currentMicroStepIndex = 0;
    private java.util.Map<Integer, Long> registerValues = new java.util.HashMap<>();
    private java.util.Map<Long, Long> modifiedMemoryValues = new java.util.HashMap<>();
    private java.util.List<String> activeComponents = new java.util.ArrayList<>();
    private java.util.List<String> activeBuses = new java.util.ArrayList<>();
    private java.util.Map<String, String> busDataValues = new java.util.HashMap<>();
    private String stepDescription = "";
    
    public ExecutionStateBuilder setProgramCounter(int pc) {
        this.programCounter = pc;
        return this;
    }
    
    public ExecutionStateBuilder setFlags(boolean zero, boolean negative, boolean overflow, boolean carry) {
        this.zeroFlag = zero;
        this.negativeFlag = negative;
        this.overflowFlag = overflow;
        this.carryFlag = carry;
        return this;
    }
    
    public ExecutionStateBuilder setLastExecutedInstruction(String instruction) {
        this.lastExecutedInstruction = instruction;
        return this;
    }
    
    public ExecutionStateBuilder setFinished(boolean finished) {
        this.isFinished = finished;
        return this;
    }
    
    public ExecutionStateBuilder setCurrentMicroStepIndex(int index) {
        this.currentMicroStepIndex = index;
        return this;
    }
    
    public ExecutionStateBuilder setRegisterValues(java.util.Map<Integer, Long> registers) {
        this.registerValues = new java.util.HashMap<>(registers);
        return this;
    }
    
    public ExecutionStateBuilder setModifiedMemoryValues(java.util.Map<Long, Long> memory) {
        this.modifiedMemoryValues = new java.util.HashMap<>(memory);
        return this;
    }
    
    public ExecutionStateBuilder setActiveComponents(java.util.List<String> components) {
        this.activeComponents = new java.util.ArrayList<>(components);
        return this;
    }
    
    public ExecutionStateBuilder setActiveBuses(java.util.List<String> buses) {
        this.activeBuses = new java.util.ArrayList<>(buses);
        return this;
    }
    
    public ExecutionStateBuilder setBusDataValues(java.util.Map<String, String> busData) {
        this.busDataValues = new java.util.HashMap<>(busData);
        return this;
    }
    
    public ExecutionStateBuilder setStepDescription(String description) {
        this.stepDescription = description;
        return this;
    }
    
    public ExecutionState build() {
        return new ExecutionState(
            programCounter,
            zeroFlag,
            negativeFlag,
            overflowFlag,
            carryFlag,
            lastExecutedInstruction,
            isFinished,
            currentMicroStepIndex,
            registerValues,
            modifiedMemoryValues,
            activeComponents,
            activeBuses,
            busDataValues,
            stepDescription
        );
    }
    
    /**
     * Creates a builder from an existing ExecutionState.
     */
    public static ExecutionStateBuilder fromState(ExecutionState state) {
        return new ExecutionStateBuilder()
            .setProgramCounter(state.getProgramCounter())
            .setFlags(state.isZeroFlag(), state.isNegativeFlag(), 
                     state.isOverflowFlag(), state.isCarryFlag())
            .setLastExecutedInstruction(state.getLastExecutedInstruction())
            .setFinished(state.isFinished())
            .setCurrentMicroStepIndex(state.getCurrentMicroStepIndex())
            .setRegisterValues(state.getRegisterValues())
            .setModifiedMemoryValues(state.getModifiedMemoryValues())
            .setActiveComponents(state.getActiveComponents())
            .setActiveBuses(state.getActiveBuses())
            .setBusDataValues(state.getBusDataValues())
            .setStepDescription(state.getStepDescription());
    }
    
    /**
     * Creates a builder with default values for initial state.
     */
    public static ExecutionStateBuilder createInitialState() {
        ExecutionStateBuilder builder = new ExecutionStateBuilder();
        
        // Initialize all 32 registers to 0
        for (int i = 0; i < 32; i++) {
            builder.registerValues.put(i, 0L);
        }
        
        builder.setStepDescription("Initial State")
               .setLastExecutedInstruction("None")
               .setProgramCounter(0);
               
        return builder;
    }
}
