package core;
import datapath.*;

import java.util.Map;

/**
 * Integrates CPUSimulator with DatapathPanel for execution history functionality.
 * This class manages the synchronization between CPU state and visualization state.
 */
public class CPUDatapathIntegrator implements ExecutionHistoryListener {
    private final CPUSimulator cpuSimulator;
    private final DatapathPanel datapathPanel;
    
    public CPUDatapathIntegrator(CPUSimulator cpuSimulator, DatapathPanel datapathPanel) {
        this.cpuSimulator = cpuSimulator;
        this.datapathPanel = datapathPanel;
        
        // Register as listener for CPU's execution history
        cpuSimulator.getExecutionHistory().addListener(this);
    }
    
    /**
     * Performs a step and updates both CPU and datapath panel states.
     */
    public void step() {
        cpuSimulator.step();
        updateDatapathVisualization();
    }
    
    /**
     * Steps back in execution history.
     */
    public boolean stepBack() {
        boolean success = cpuSimulator.stepBack();
        if (success) {
            updateDatapathVisualization();
        }
        return success;
    }
    
    /**
     * Steps forward in execution history.
     */
    public boolean stepForward() {
        boolean success = cpuSimulator.stepForward();
        if (success) {
            updateDatapathVisualization();
        }
        return success;
    }
    
    /**
     * Updates the datapath panel with current CPU state.
     */
    private void updateDatapathVisualization() {
        // Update register values in datapath panel
        for (int i = 0; i < 32; i++) {
            long regValue = cpuSimulator.getRegisterValue(i);
            datapathPanel.updateRegisterValue(i, regValue);
        }
        
        // Update memory state
        Map<Long, Long> memoryState = cpuSimulator.getMemoryState();
        for (Map.Entry<Long, Long> entry : memoryState.entrySet()) {
            datapathPanel.updateMemoryValue(entry.getKey(), entry.getValue());
        }
        
        // Update visualization components and buses
        datapathPanel.setActiveComponentsAndBuses(
            cpuSimulator.getActiveComponents(),
            cpuSimulator.getActiveBuses(),
            cpuSimulator.getBusDataValues()
        );
        
        // Record state in datapath panel's history
        boolean[] flags = {
            cpuSimulator.isZeroFlag(),
            cpuSimulator.isNegativeFlag(),
            cpuSimulator.isOverflowFlag(),
            cpuSimulator.isCarryFlag()
        };
        
        datapathPanel.recordExecutionState(
            cpuSimulator.getCurrentHistoryStepDescription(),
            cpuSimulator.getPc(),
            flags,
            cpuSimulator.getLastExecutedInstruction(),
            cpuSimulator.isFinished(),
            0 // micro-step index - could be enhanced
        );
    }
    
    // --- ExecutionHistoryListener Implementation ---
    
    @Override
    public void onHistoryStateChanged(boolean canStepBack, boolean canStepForward, 
                                    int currentStep, int totalSteps) {
        // Can be used to update GUI button states
        System.out.println(String.format("History: Step %d/%d, Back: %b, Forward: %b",
                                        currentStep + 1, totalSteps, canStepBack, canStepForward));
    }
    
    @Override
    public void onStateRestored(ExecutionState state) {
        // Update datapath panel when state is restored
        updateDatapathVisualization();
        System.out.println("CPU and Datapath state restored: " + state.getStepDescription());
    }
    
    @Override
    public void onStateRecorded(ExecutionState state) {
        // Optional: Log when state is recorded
        System.out.println("State recorded: " + state.getStepDescription());
    }
    
    @Override
    public void onHistoryCleared() {
        // Clear datapath panel history when CPU history is cleared
        datapathPanel.clearHistory();
        System.out.println("Execution history cleared");
    }
    
    // --- Convenience Methods ---
    
    public boolean canStepBack() {
        return cpuSimulator.canStepBack();
    }
    
    public boolean canStepForward() {
        return cpuSimulator.canStepForward();
    }
    
    public String getExecutionStatistics() {
        return cpuSimulator.getExecutionStatistics();
    }
    
    public void reset() {
        cpuSimulator.reset();
        // Datapath panel history will be cleared via the listener
    }
    
    public void loadProgram(String[] assemblyLines) {
        cpuSimulator.loadProgram(assemblyLines);
        updateDatapathVisualization(); // Show initial state
    }
    
    public void executeProgram() {
        cpuSimulator.executeProgram();
        updateDatapathVisualization(); // Show final state
    }
}
