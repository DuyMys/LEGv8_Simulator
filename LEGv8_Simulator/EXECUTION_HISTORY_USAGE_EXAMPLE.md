// Example of how to integrate the execution history system with the LEGv8 Simulator

/*
USAGE EXAMPLE FOR EXECUTION HISTORY SYSTEM

1. In your CPU simulator main execution loop, you would record states like this:

// When starting execution or after each instruction
boolean[] flags = {cpuSimulator.isZeroFlag(), cpuSimulator.isNegativeFlag(), 
                   cpuSimulator.isOverflowFlag(), cpuSimulator.isCarryFlag()};

datapathPanel.recordExecutionState(
    "Executed: " + currentInstruction.toString(),  // Step description
    cpuSimulator.getProgramCounter(),               // Current PC
    flags,                                          // CPU flags
    currentInstruction.toString(),                  // Last instruction
    cpuSimulator.isFinished(),                     // Execution finished?
    cpuSimulator.getCurrentMicroStepIndex()        // Micro-step index
);

// When register values change
datapathPanel.updateRegisterValue(registerIndex, newValue);

// When memory values change  
datapathPanel.updateMemoryValue(memoryAddress, newValue);


2. For step-back functionality in your GUI:

// Step Back Button Action
if (datapathPanel.canStepBack()) {
    boolean success = datapathPanel.stepBack();
    if (success) {
        // Update CPU simulator state from the restored state
        ExecutionState restoredState = datapathPanel.getExecutionHistory().getCurrentState();
        restoreCPUFromState(restoredState);
        updateGUIDisplay();
    }
}

// Step Forward Button Action  
if (datapathPanel.canStepForward()) {
    boolean success = datapathPanel.stepForward();
    if (success) {
        // Update CPU simulator state from the restored state
        ExecutionState restoredState = datapathPanel.getExecutionHistory().getCurrentState();
        restoreCPUFromState(restoredState);
        updateGUIDisplay();
    }
}


3. To implement restoreCPUFromState method:

private void restoreCPUFromState(ExecutionState state) {
    // Restore CPU state
    cpuSimulator.setProgramCounter(state.getProgramCounter());
    cpuSimulator.setFlags(state.isZeroFlag(), state.isNegativeFlag(), 
                         state.isOverflowFlag(), state.isCarryFlag());
    cpuSimulator.setLastExecutedInstruction(state.getLastExecutedInstruction());
    cpuSimulator.setFinished(state.isFinished());
    cpuSimulator.setCurrentMicroStepIndex(state.getCurrentMicroStepIndex());
    
    // Restore register values
    Map<Integer, Long> registers = state.getRegisterValues();
    for (Map.Entry<Integer, Long> entry : registers.entrySet()) {
        cpuSimulator.getRegisterFile().write(entry.getKey(), entry.getValue());
    }
    
    // Restore memory values
    Map<Long, Long> memory = state.getModifiedMemoryValues();
    for (Map.Entry<Long, Long> entry : memory.entrySet()) {
        cpuSimulator.getMemory().write(entry.getKey(), entry.getValue(), 8); // 8 bytes
    }
}


4. Button state management using ExecutionHistoryListener:

public class LEGv8GUI implements ExecutionHistoryListener {
    private JButton stepBackButton;
    private JButton stepForwardButton;
    private JLabel historyStatusLabel;
    
    @Override
    public void onHistoryStateChanged(boolean canStepBack, boolean canStepForward, 
                                    int currentStep, int totalSteps) {
        SwingUtilities.invokeLater(() -> {
            stepBackButton.setEnabled(canStepBack);
            stepForwardButton.setEnabled(canStepForward);
            historyStatusLabel.setText(String.format("Step %d of %d", currentStep + 1, totalSteps));
        });
    }
    
    @Override
    public void onStateRestored(ExecutionState state) {
        SwingUtilities.invokeLater(() -> {
            // Update instruction display, register display, etc.
            updateInstructionDisplay(state.getLastExecutedInstruction());
            updatePCDisplay(state.getProgramCounter());
        });
    }
    
    @Override
    public void onStateRecorded(ExecutionState state) {
        // Could update a history log or status display
    }
    
    @Override
    public void onHistoryCleared() {
        SwingUtilities.invokeLater(() -> {
            stepBackButton.setEnabled(false);
            stepForwardButton.setEnabled(false);
            historyStatusLabel.setText("History cleared");
        });
    }
}


5. Advanced features:

// Get execution statistics
String stats = datapathPanel.getExecutionStatistics();
System.out.println(stats);

// Find specific steps in history
ExecutionHistory history = datapathPanel.getExecutionHistory();
List<Integer> addInstructions = history.findStatesByDescription("ADD");

// Jump to a specific step
ExecutionState specificState = history.jumpToState(10);
if (specificState != null) {
    restoreCPUFromState(specificState);
}

// Clear history when starting new program
datapathPanel.clearHistory();


6. Best practices:

- Record state after each significant execution step (instruction completion, micro-steps)
- Update register/memory values as they change during execution
- Use meaningful step descriptions for easier debugging
- Handle the ExecutionHistoryListener events to keep GUI in sync
- Clear history when loading new programs
- Consider memory usage for long-running simulations (adjust max history size)

*/
