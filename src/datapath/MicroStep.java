package datapath;

import java.util.List;
import java.util.Map;

public class MicroStep {
    private final String operation;
    private final String components;
    private final String cpuStateSnapshot;
    private final List<String> activeComponents;
    private final List<String> activeBuses;
    private final Map<String, String> busDataValues;

    public MicroStep(String operation, String components, String cpuStateSnapshot,
                     List<String> activeComponents, List<String> activeBuses,
                     Map<String, String> busDataValues) {
        this.operation = operation;
        this.components = components;
        this.cpuStateSnapshot = cpuStateSnapshot;
        this.activeComponents = activeComponents;
        this.activeBuses = activeBuses;
        this.busDataValues = busDataValues;
    }

    public String getOperation() {
        return operation;
    }

    public String getComponents() {
        return components;
    }

    public String getCpuStateSnapshot() {
        return cpuStateSnapshot;
    }

    public List<String> getActiveComponents() {
        return activeComponents;
    }

    public List<String> getActiveBuses() {
        return activeBuses;
    }

    public Map<String, String> getBusDataValues() {
        return busDataValues;
    }
}
