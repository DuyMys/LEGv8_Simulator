package core;
import instruction.*;
import util.*;
import memory.Memory;
//import exceptions.MemoryAccessException;
import java.util.ArrayList;
import java.util.List;
import java.util.BitSet;

/**
 * Simulates a LEGv8 CPU, executing a list of instructions and displaying register values.
 */
public class CPUSimulator {
    private final InstructionFactory factory;
    private final RegisterFileController registerFile;
    private final ArithmeticLogicUnit alu;
    private final ControlUnit controlUnit;
    private final Memory memory;
    private final List<Instruction> program;
    private int pc;

    // Thêm các cờ trạng thái
    private boolean zeroFlag;
    private boolean negativeFlag;
    private boolean overflowFlag;
    private boolean carryFlag;

    public CPUSimulator(InstructionConfigLoader configLoader) {
        this.factory = new InstructionFactory(configLoader);
        this.registerFile = new RegisterFileController(new RegisterStorage());
        this.alu = new ArithmeticLogicUnit();
        this.controlUnit = new ControlUnit(configLoader);
        this.memory = new Memory();
        this.program = new ArrayList<>();
        this.pc = 0;
    }

    public void loadProgram(String[] assemblyLines) {
        program.clear();
        pc = 0;
        for (String line : assemblyLines) {
            Instruction instruction = factory.createFromAssembly(line);
            if (instruction != null) {
                BitSet bytecode = instruction.getBytecode();
                int instructionCode = 0;
                for (int i = 0; i < 32; i++) {
                    if (bytecode.get(i)) {
                        instructionCode |= (1 << i);
                    }
                }
                System.out.printf("Loading instruction: %s -> Bytecode: 0x%08X\n", line, instructionCode);
                program.add(instruction);
            } else {
                System.err.println("Failed to load instruction: " + line);
            }
        }
        System.out.println("Program loaded with " + program.size() + " instruction(s).");
    }

    public void executeProgram() {
        while (pc < program.size()) {
            step();
        }
        System.out.println("Program finished.");
        printState();
    }

    public void step() {
        if (pc >= program.size()) {
            System.out.println("Program has finished.");
            return;
        }
        Instruction instruction = program.get(pc);
        executeInstruction(instruction);
        pc++;
        printState();
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
            // Cập nhật cờ trạng thái
            updateFlags(result);
            System.out.printf("%s -> X%d=%d\n", instruction.disassemble(), rInst.getRd_R(), result.result);
        } else if (instruction instanceof IMFormatInstruction) {
            IMFormatInstruction imInst = (IMFormatInstruction) instruction;
            long immediate = imInst.getImmediate_IM() << (imInst.getShift_IM() * 16);
            long rnValue = signals.isAluSrc() ? 0 : registerFile.readRegister(imInst.getRd_IM());
            ArithmeticLogicUnit.ALUResult result = alu.execute(rnValue, immediate, signals.getAluOp(), signals.getOperation());
            if (signals.isRegWrite()) {
                registerFile.writeRegister(imInst.getRd_IM(), result.result, true);
            }
            // Cập nhật cờ trạng thái
            updateFlags(result);
            System.out.printf("%s -> X%d=%d\n", instruction.disassemble(), imInst.getRd_IM(), result.result);
        } else if (instruction instanceof IFormatInstruction) {
            IFormatInstruction iInst = (IFormatInstruction) instruction;
            long rnValue = registerFile.readRegister(iInst.getRn_I());
            long immediate = iInst.getImmediate_I();
            ArithmeticLogicUnit.ALUResult result = alu.execute(rnValue, immediate, signals.getAluOp(), signals.getOperation());
            if (signals.isRegWrite()) {
                registerFile.writeRegister(iInst.getRd_I(), result.result, true);
            }
            // Cập nhật cờ trạng thái
            updateFlags(result);
            System.out.printf("%s -> X%d=%d\n", instruction.disassemble(), iInst.getRd_I(), result.result);
        } else if (instruction instanceof DFormatInstruction) {
            DFormatInstruction dInst = (DFormatInstruction) instruction;
            long rnValue = registerFile.readRegister(dInst.getRn_D());
            long address = rnValue + dInst.getAddress_D();
            if (mnemonic.equals("LDUR")) {
                long value = memory.readLong(address);
                if (signals.isRegWrite()) {
                    registerFile.writeRegister(dInst.getRt_D(), value, true);
                }
                System.out.printf("%s -> X%d=%d\n", instruction.disassemble(), dInst.getRt_D(), value);
            } else if (mnemonic.equals("STUR")) {
                long value = registerFile.readRegister(dInst.getRt_D());
                memory.writeLong(address, value);
                System.out.printf("%s -> [0x%X]=%d\n", instruction.disassemble(), address, value);
            }
            // Không cập nhật cờ trạng thái cho LDUR/STUR
        } else if (instruction instanceof BFormatInstruction) {
            BFormatInstruction bInst = (BFormatInstruction) instruction;
            long offset = bInst.getAddress_B();
            pc = pc + (int) offset - 1; // -1 vì pc++ trong step()
            System.out.printf("%s -> PC=%d\n", instruction.disassemble(), pc + 1);
            // Không cập nhật cờ trạng thái cho B
        }
    }

    // Phương thức cập nhật cờ trạng thái
    private void updateFlags(ArithmeticLogicUnit.ALUResult result) {
        zeroFlag = result.result == 0;
        negativeFlag = result.result < 0;
        overflowFlag = result.overflow;
        carryFlag = result.carry;
    }

    public void printState() {
        System.out.println("CPU State:");
        System.out.println("PC: " + pc);
        System.out.println("Registers:");
        for (int i = 0; i < 32; i++) {
            System.out.printf("X%-2d: 0x%016X  ", i, registerFile.readRegister(i));
            if ((i + 1) % 4 == 0) System.out.println();
        }
   
        System.out.println("Flags: ZF=" + (zeroFlag ? 1 : 0) + ", NF=" + (negativeFlag ? 1 : 0) +
                ", OF=" + (overflowFlag ? 1 : 0) + ", CF=" + (carryFlag ? 1 : 0));
    }

    public boolean isZeroFlag() {
        return zeroFlag;
    }

    public boolean isNegativeFlag() {
        return negativeFlag;
    }

    public boolean isOverflowFlag() {
        return overflowFlag;
    }

    public boolean isCarryFlag() {
        return carryFlag;
    }

    public List<Instruction> getProgram() {
        return program;
    }

    public InstructionFactory getFactory() {
        return factory;
    }

    public RegisterFileController getRegisterFile() {
        return registerFile;
    }

    public ArithmeticLogicUnit getAlu() {
        return alu;
    }

    public ControlUnit getControlUnit() {
        return controlUnit;
    }

    public Memory getMemory() {
        return memory;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

     public void reset() {
        program.clear();
        pc = 0;
        registerFile.reset();
        memory.reset();
        zeroFlag = false;
        negativeFlag = false;
        overflowFlag = false;
        carryFlag = false;
    }
}