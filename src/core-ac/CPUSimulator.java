package core;
import instruction.*;
import util.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulates a LEGv8 CPU, executing a list of instructions and displaying register values.
 */
public class CPUSimulator {
    private final InstructionFactory factory;
    private final RegisterFileController registerFile;
    private final ArithmeticLogicUnit alu;
    private final ControlUnit controlUnit;
    private final List<Instruction> program;
    private int pc;

    public CPUSimulator(InstructionConfigLoader configLoader) {
        this.factory = new InstructionFactory(configLoader);
        this.registerFile = new RegisterFileController(new RegisterStorage());
        this.alu = new ArithmeticLogicUnit();
        this.controlUnit = new ControlUnit(configLoader);
        this.program = new ArrayList<>();
        this.pc = 0;
    }

    public void loadProgram(String[] assemblyLines) {
        for (String line : assemblyLines) {
            Instruction instruction = factory.createFromAssembly(line);
            if (instruction != null) {
                program.add(instruction);
            }
        }
    }

    public void executeProgram() {
        while (pc < program.size()) {
            Instruction instruction = program.get(pc);
            executeInstruction(instruction);
            pc++;
        }
    }

    private void executeInstruction(Instruction instruction) {
        ControlSignals signals = controlUnit.generateControlSignals(instruction);
        String mnemonic = instruction.getDefinition().getMnemonic();

        if (instruction instanceof RFormatInstruction) {
            RFormatInstruction rInst = (RFormatInstruction) instruction;
            long rnValue = registerFile.readRegister(rInst.getRn_R());
            long rmValue = registerFile.readRegister(signals.isReg2Loc() ? rInst.getRd_R() : rInst.getRm_R());
            ArithmeticLogicUnit.ALUResult result = alu.execute(rnValue, rmValue, signals.getAluOp(), signals.getOperation());
            if (signals.isRegWrite()) {
                registerFile.writeRegister(rInst.getRd_R(), result.result, true);
            }
            System.out.printf("%s -> X%d=%d\n", instruction.disassemble(), rInst.getRd_R(), result.result);
        } else if (instruction instanceof IMFormatInstruction) {
            IMFormatInstruction imInst = (IMFormatInstruction) instruction;
            long immediate = imInst.getImmediate_IM() << (imInst.getShift_IM() * 16);
            long rnValue = signals.isAluSrc() ? 0 : registerFile.readRegister(imInst.getRd_IM());
            ArithmeticLogicUnit.ALUResult result = alu.execute(rnValue, immediate, signals.getAluOp(), signals.getOperation());
            if (signals.isRegWrite()) {
                registerFile.writeRegister(imInst.getRd_IM(), result.result, true);
            }
            System.out.printf("%s -> X%d=%d\n", instruction.disassemble(), imInst.getRd_IM(), result.result);
        }
    }

    public static void main(String[] args) {
        InstructionConfigLoader configLoader = new InstructionConfigLoader();
        configLoader.loadConfig("D:/LEGv8_Simulator-1/src/instruction/instructions.txt");
        
        CPUSimulator simulator = new CPUSimulator(configLoader);
        String[] program = {
            "MOVZ X1, #5, LSL #0",
            "MOVZ X2, #10, LSL #0",
            "ADD X3, X1, X2"
        };
        simulator.loadProgram(program);
        simulator.executeProgram();
    }
}