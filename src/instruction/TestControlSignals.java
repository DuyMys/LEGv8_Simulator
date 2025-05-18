package instruction;

public class TestControlSignals {
    public static void main(String[] args) {
        InstructionConfigLoader configLoader = new InstructionConfigLoader();
        configLoader.loadConfig("/instruction/instructions.config");
        InstructionDefinition def = configLoader.getDefinitionByMnemonic("ADD");
        System.out.println(def.getControlSignals());
    }
}