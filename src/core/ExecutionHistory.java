package core;

import java.util.*;

/**
 * Manages the execution history for step-back functionality.
 * Maintains a stack of execution states that can be navigated forward and backward.
 */
public class ExecutionHistory {
    private final List<ExecutionState> history;
    private int currentIndex;
    private final int maxHistorySize;
    
    // Statistics
    private int totalSteps;
    private int totalStepBacks;
    
    // Listener support
    private final List<ExecutionHistoryListener> listeners;
    
    public ExecutionHistory() {
        this(10000); // Default max history size
    }
    
    public ExecutionHistory(int maxHistorySize) {
        this.history = new ArrayList<>();
        this.currentIndex = -1;
        this.maxHistorySize = maxHistorySize;
        this.totalSteps = 0;
        this.totalStepBacks = 0;
        this.listeners = new ArrayList<>();
    }
    
    /**
     * Adds a new execution state to the history.
     * This will remove any "future" states if we're not at the end of history.
     */
    public void addState(ExecutionState state) {
        // Remove any states after current index (when stepping back then forward)
        if (currentIndex < history.size() - 1) {
            history.subList(currentIndex + 1, history.size()).clear();
        }
        
        // Add new state
        history.add(state);
        currentIndex++;
        totalSteps++;
        
        // Maintain max history size
        if (history.size() > maxHistorySize) {
            history.remove(0);
            currentIndex--;
        }
        
        // Notify listeners
        notifyStateRecorded(state);
        notifyHistoryStateChanged();
    }
    
    /**
     * Steps back to the previous execution state.
     * @return The previous execution state, or null if already at the beginning.
     */
    public ExecutionState stepBack() {
        if (!canStepBack()) {
            return null;
        }
        
        currentIndex--;
        totalStepBacks++;
        ExecutionState state = getCurrentState();
        
        // Notify listeners
        notifyStateRestored(state);
        notifyHistoryStateChanged();
        
        return state;
    }
    
    /**
     * Steps forward to the next execution state.
     * @return The next execution state, or null if already at the end.
     */
    public ExecutionState stepForward() {
        if (!canStepForward()) {
            return null;
        }
        
        currentIndex++;
        ExecutionState state = getCurrentState();
        
        // Notify listeners
        notifyStateRestored(state);
        notifyHistoryStateChanged();
        
        return state;
    }
    
    /**
     * Gets the current execution state.
     * @return The current execution state, or null if history is empty.
     */
    public ExecutionState getCurrentState() {
        if (history.isEmpty() || currentIndex < 0 || currentIndex >= history.size()) {
            return null;
        }
        return history.get(currentIndex);
    }
    
    /**
     * Checks if we can step back.
     */
    public boolean canStepBack() {
        return currentIndex > 0;
    }
    
    /**
     * Checks if we can step forward.
     */
    public boolean canStepForward() {
        return currentIndex < history.size() - 1;
    }
    
    /**
     * Gets the total number of states in history.
     */
    public int getHistorySize() {
        return history.size();
    }
    
    /**
     * Gets the current position in history (0-based).
     */
    public int getCurrentIndex() {
        return currentIndex;
    }
    
    /**
     * Clears all history.
     */
    public void clear() {
        history.clear();
        currentIndex = -1;
        totalSteps = 0;
        totalStepBacks = 0;
        
        // Notify listeners
        notifyHistoryCleared();
        notifyHistoryStateChanged();
    }
    
    /**
     * Gets a list of all step descriptions in chronological order.
     */
    public List<String> getStepDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (ExecutionState state : history) {
            descriptions.add(state.getStepDescription());
        }
        return descriptions;
    }
    
    /**
     * Gets execution statistics.
     */
    public String getStatistics() {
        return String.format("Total Steps: %d, Step Backs: %d, Current Position: %d/%d",
                           totalSteps, totalStepBacks, currentIndex + 1, history.size());
    }
    
    /**
     * Gets the state at a specific index.
     */
    public ExecutionState getStateAt(int index) {
        if (index < 0 || index >= history.size()) {
            return null;
        }
        return history.get(index);
    }
    
    /**
     * Jumps to a specific state in the history.
     */
    public ExecutionState jumpToState(int index) {
        if (index < 0 || index >= history.size()) {
            return null;
        }
        currentIndex = index;
        return getCurrentState();
    }
    
    /**
     * Gets a range of states for debugging or analysis.
     */
    public List<ExecutionState> getStatesInRange(int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex >= history.size() || startIndex > endIndex) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history.subList(startIndex, endIndex + 1));
    }
    
    /**
     * Finds states by step description pattern.
     */
    public List<Integer> findStatesByDescription(String pattern) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getStepDescription().toLowerCase().contains(pattern.toLowerCase())) {
                indices.add(i);
            }
        }
        return indices;
    }
    
    @Override
    public String toString() {
        return String.format("ExecutionHistory[Size=%d, Current=%d, CanStepBack=%b, CanStepForward=%b]",
                           history.size(), currentIndex, canStepBack(), canStepForward());
    }
    
    // --- Listener Management ---
    
    /**
     * Adds a history listener.
     */
    public void addListener(ExecutionHistoryListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a history listener.
     */
    public void removeListener(ExecutionHistoryListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifies listeners of history state changes.
     */
    private void notifyHistoryStateChanged() {
        for (ExecutionHistoryListener listener : listeners) {
            try {
                listener.onHistoryStateChanged(canStepBack(), canStepForward(), 
                                             currentIndex, history.size());
            } catch (Exception e) {
                System.err.println("Error notifying history listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notifies listeners when a state is restored.
     */
    private void notifyStateRestored(ExecutionState state) {
        for (ExecutionHistoryListener listener : listeners) {
            try {
                listener.onStateRestored(state);
            } catch (Exception e) {
                System.err.println("Error notifying history listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notifies listeners when a state is recorded.
     */
    private void notifyStateRecorded(ExecutionState state) {
        for (ExecutionHistoryListener listener : listeners) {
            try {
                listener.onStateRecorded(state);
            } catch (Exception e) {
                System.err.println("Error notifying history listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notifies listeners when history is cleared.
     */
    private void notifyHistoryCleared() {
        for (ExecutionHistoryListener listener : listeners) {
            try {
                listener.onHistoryCleared();
            } catch (Exception e) {
                System.err.println("Error notifying history listener: " + e.getMessage());
            }
        }
    }
}
