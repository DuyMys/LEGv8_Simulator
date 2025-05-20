// Cồn lỗi nhiều 
package core.cpu;

import instruction.Instruction;
import instruction.InstructionFactory;
import instruction.InstructionDefinition;
import memory.Memory;
import exceptions.InvalidInstructionException;
import util.Constants;
//import execution.RFormatExecutor;
//import execution.InstructionExecutor;
import java.util.BitSet;
import core.alu.ALU;

/**
 * Mô phỏng CPU LEGv8, điều phối các thành phần và thực thi vòng lặp Fetch-Decode-Execute.
 */
public class CPU {
    private Registers registers;
    private ProgramCounter pc;
    private Flags flags;
    private Memory memory;
    private ALU alu;
    private InstructionFactory instructionFactory;
    private boolean halted;

    /**
     * Khởi tạo CPU với các thành phần cần thiết.
     */
    public CPU() {
        registers = new Registers();
        pc = new ProgramCounter();
        flags = new Flags();
        memory = new Memory();
        alu = new ALU();
        instructionFactory = new InstructionFactory(new instruction.InstructionConfigLoader());
        halted = false;
    }

    /**
     * Lấy đối tượng Registers.
     * @return Registers instance.
     */
    public Registers getRegisters() {
        return registers;
    }

    /**
     * Lấy đối tượng ProgramCounter.
     * @return ProgramCounter instance.
     */
    public ProgramCounter getProgramCounter() {
        return pc;
    }

    /**
     * Lấy đối tượng Flags.
     * @return Flags instance.
     */
    public Flags getFlags() {
        return flags;
    }

    /**
     * Lấy đối tượng Memory.
     * @return Memory instance.
     */
    public Memory getMemory() {
        return memory;
    }

    /**
     * Lấy đối tượng ALU.
     * @return ALU instance.
     */
    public ALU getALU() {
        return alu;
    }

    /**
     * Chuyển đổi mã lệnh (int) thành BitSet.
     * @param instructionCode Mã lệnh 32-bit.
     * @return BitSet biểu diễn mã lệnh.
     */
    private BitSet intToBitSet(int instructionCode) {
        BitSet bitSet = new BitSet(32);
        for (int i = 0; i < 32; i++) {
            if ((instructionCode & (1 << i)) != 0) {
                bitSet.set(i);
            }
        }
        return bitSet;
    }

    /**
     * Thực thi một chu kỳ Fetch-Decode-Execute.
     * @throws InvalidInstructionException nếu lệnh không hợp lệ.
     */
    public void step() throws InvalidInstructionException {
        if (halted) {
            return;
        }

        // Fetch: Lấy lệnh từ bộ nhớ
        long pcValue = pc.get();
        int instructionCode = memory.read(pcValue);

        // Decode: Tạo đối tượng lệnh
        BitSet bytecode = intToBitSet(instructionCode);
        Instruction instruction = instructionFactory.createFromBytecode(bytecode);
        if (instruction == null) {
            throw new InvalidInstructionException("Lệnh không hợp lệ tại PC=0x" + Long.toHexString(pcValue));
        }

        // Execute: Thực thi lệnh
        InstructionDefinition def = instruction.getDefinition();
        char format = def.getFormat();
        InstructionExecutor executor;
        switch (format) {
            case 'R':
                executor = new RFormatExecutor();
                break;
            // TODO: Thêm các executor cho các format khác (I, D, B, CB, IM) khi bạn triển khai
            // case 'I':
            //     executor = new IFormatExecutor();
            //     break;
            // case 'D':
            //     executor = new DFormatExecutor();
            //     break;
            // case 'B':
            //     executor = new BFormatExecutor();
            //     break;
            //case 'CB':
            //     executor = new CBFormatExecutor();
            //     break;
            // case 'IM':
            //     executor = new IMFormatExecutor();
            //     break;
            default:
                throw new InvalidInstructionException("Định dạng lệnh không hỗ trợ: " + format);
        }
        executor.execute(instruction, this);

        // Tăng PC (trừ khi lệnh là branch)
        // if (format != 'B' && format != 'CB') {
        //     pc.advanceInstruction();
        // }

        if (format != 'B') {
            pc.advanceInstruction();
        }
    }

    /**
     * Chạy toàn bộ chương trình cho đến khi dừng.
     * @throws InvalidInstructionException nếu gặp lệnh không hợp lệ.
     */
    public void run() throws InvalidInstructionException {
        while (!halted) {
            step();
        }
    }

    /**
     * Đặt trạng thái dừng cho CPU.
     */
    public void halt() {
        halted = true;
    }

    /**
     * Đặt lại trạng thái CPU (PC, thanh ghi, cờ, bộ nhớ).
     */
    public void reset() {
        registers.reset();
        pc.reset();
        flags.reset();
        memory.reset();
        halted = false;
    }

    /**
     * Trả về biểu diễn chuỗi của trạng thái CPU.
     * @return Chuỗi chứa trạng thái PC, thanh ghi, và cờ.
     */
    @Override
    public String toString() {
        return String.format("CPU State:\n%s\n%s\n%s",
                pc.toString(), registers.toString(), flags.toString());
    }
}