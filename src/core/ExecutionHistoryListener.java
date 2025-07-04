package core;

/**
 * Interface for components that need to be notified of execution history events.
 * This allows the GUI to update step-back/step-forward button states and other
 * history-related UI elements.
 */
public interface ExecutionHistoryListener {
    
    /**
     * Called when the execution history state changes.
     * @param canStepBack true if stepping back is possible
     * @param canStepForward true if stepping forward is possible
     * @param currentStep current step index in history
     * @param totalSteps total number of steps in history
     */
    void onHistoryStateChanged(boolean canStepBack, boolean canStepForward, 
                              int currentStep, int totalSteps);
    
    /**
     * Called when the execution state is restored from history.
     * @param state the restored execution state
     */
    void onStateRestored(ExecutionState state);
    
    /**
     * Called when a new execution state is recorded.
     * @param state the newly recorded execution state
     */
    void onStateRecorded(ExecutionState state);
    
    /**
     * Called when the execution history is cleared.
     */
    void onHistoryCleared();
}
