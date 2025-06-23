package datapath; 

import java.util.List;
import java.util.Map;

public class MicroStep {
    private final String description;
    private final List<String> activeComponents;
    private final List<String> activeBuses;
    private final Map<String, String> busDataValues;
    private final Runnable action; 

    public MicroStep(String description, List<String> activeComponents, List<String> activeBuses, Map<String, String> busDataValues, Runnable action) {
        this.description = description;
        this.activeComponents = activeComponents;
        this.activeBuses = activeBuses;
        this.busDataValues = busDataValues;
        this.action = action;
    }

    public String getDescription() { return description; }
    public List<String> getActiveComponents() { return activeComponents; }
    public List<String> getActiveBuses() { return activeBuses; }
    public Map<String, String> getBusDataValues() { return busDataValues; }
    
    public void executeAction() {
        if (action != null) {
            action.run();
        }
    }
}
