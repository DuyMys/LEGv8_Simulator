package com.arm.legv8simulator.client.cpu;

import java.util.ArrayList;

import com.arm.legv8simulator.client.Error;
import com.arm.legv8simulator.client.instruction.Instruction;
import com.arm.legv8simulator.client.memory.Memory;
import com.arm.legv8simulator.client.memory.SegmentFaultException;

public class CPU{
    public static final int INSTRUCTION_SIZE = 4;
	public static final int NUM_REGISTERS = 32;
	
	public static final int XZR = 31;
	public static final int LR = 30;
	public static final int FP = 29;
	public static final int SP = 28;
	public static final int X27 = 27;
	public static final int X26 = 26;
	public static final int X25 = 25;
	public static final int X24 = 24;
	public static final int X23 = 23;
	public static final int X22 = 22;
	public static final int X21 = 21;
	public static final int X20 = 20;
	public static final int X19 = 19;
	public static final int X18 = 18;
	public static final int IP1 = 17;
	public static final int IP0 = 16;
	public static final int X15 = 15;
	public static final int X14 = 14;
	public static final int X13 = 13;
	public static final int X12 = 12;
	public static final int X11 = 11;
	public static final int X10 = 10;
	public static final int X9 = 9;
	public static final int X8 = 8;
	public static final int X7 = 7;
	public static final int X6 = 6;
	public static final int X5 = 5;
	public static final int X4 = 4;
	public static final int X3 = 3;
	public static final int X2 = 2;
	public static final int X1 = 1;
	public static final int X0 = 0;

    public CPU() {
		registerFile = new long[NUM_REGISTERS];
		for (int i=0; i<NUM_REGISTERS; i++) {
			registerFile[i] = 0L;
		}
		registerFile[SP] = Memory.STACK_BASE;
		Nflag = false;
		Zflag = false;
		Cflag = false;
		Vflag = false;
        
        // Them
        instructionIndex = 0; 
		cpuLog = new StringBuilder(""); 
	}

    public Error executeInstruction(ArrayList<Instruction> cpuInstructions, Memory memory) {
		try {
			execute(cpuInstructions.get(instructionIndex++), memory);
		} catch (SegmentFaultException sfe) {
			return new Error(sfe.getMessage(), cpuInstructions.get(instructionIndex-1).getLineNumber());
		} catch (PCAlignmentException pcae) {
			return new Error(pcae.getMessage(), cpuInstructions.get(instructionIndex-1).getLineNumber());
		} catch (SPAlignmentException spae) {
			return new Error(spae.getMessage(), cpuInstructions.get(instructionIndex-1).getLineNumber());
		}
		return null;
	}

    public Error run(ArrayList<Instruction> cpuInstructions, Memory memory) {
		try {
			while (instructionIndex < cpuInstructions.size()) {
				execute(cpuInstructions.get(instructionIndex++), memory);
			}
		} catch (SegmentFaultException sfe) {
			return new Error(sfe.getMessage(), cpuInstructions.get(instructionIndex-1).getLineNumber());
		} catch (PCAlignmentException pcae) {
			return new Error(pcae.getMessage(), cpuInstructions.get(instructionIndex-1).getLineNumber());
		} catch (SPAlignmentException spae) {
			return new Error(spae.getMessage(), cpuInstructions.get(instructionIndex-1).getLineNumber());
		}
		return null;
	}

    public long getRegister(int index) {
		return registerFile[index];
	}

    public String getCpuLog() {
		return cpuLog.toString();
	}

    public boolean getBranchTaken() {
		return branchTaken;
	}
	
    public boolean getSTXRSucceed() {
		return STXRSucceed;
	}

    public long getPC() {
		return (long) instructionIndex * INSTRUCTION_SIZE + Memory.TEXT_SEGMENT_OFFSET;
	}

    public int getInstructionIndex() {
		return instructionIndex;
	}
	
	public boolean getNflag() {
		return Nflag;
	}

    public boolean getZflag() {
		return Zflag;
	}
	
    public boolean getCflag() {
		return Cflag;
	}

    public boolean getVflag() {
		return Vflag;
	}

    private void setNflag(boolean set) {
		Nflag = set;
	}
	
	private void setZflag(boolean set) {
		Zflag = set;
	}
	
	private void setCflag(boolean set) {
		Cflag = set;
	}
	
	private void setCflag(long result, long op1, long op2) {
		Cflag = ((MSB(~result) + MSB(op1) + MSB(op2)) & 2L) != 0;
	}

    private void setVflag(boolean set) {
		Vflag = set;
	}
	
	private void setVflag(long result, long op1, long op2) {
		Vflag = (((op1^~op2) & (op1^result)) & (1<<63)) != 0;
	}
	
    private long MSB(long value) {
		return value >>> 63;
	}

    private void ADDSetFlags(long result, long op1, long op2) {
		setNflag(result < 0);
		setZflag(result == 0);
		setCflag(result, op1, op2);
		setVflag(result, op1, op2);
	}
	
	private void SUBSetFlags(long result, long op1, long op2) {
		ADDSetFlags(result, op1, op2);
	}

    private void execute(Instruction ins, Memory memory) {
		int[] args = ins.getArgs();

		switch (ins.getMnemonic()) {
		case ADD :
			ADD(args[0], args[1], args[2]);
			break;
		case ADDS :
			ADDS(args[0], args[1], args[2]);
			break;
		case SUB :
			SUB(args[0], args[1], args[2]);
			break;
		case SUBS :
			SUBS(args[0], args[1], args[2]);
			break;
		default : 
			
			cpuLog.append("Attempted to execute unsupported or unknown instruction: " + ins.getMnemonic() + "\n");
			
			break; 
		}
	}

    private void ADD(int destReg, int op1Reg, int op2Reg) {
		if (destReg == XZR) {
			cpuLog.append("Ignored attempted assignment to XZR. \n");
		} else {
			registerFile[destReg] = registerFile[op1Reg] + registerFile[op2Reg];
			cpuLog.append("ADD \t X" + destReg + ", X" + op1Reg + ", X" + op2Reg + "\n");
		}
	}

    private void ADDS(int destReg, int op1Reg, int op2Reg) {
		long result = registerFile[op1Reg] + registerFile[op2Reg];
		if (destReg == XZR) {
			cpuLog.append("Ignored attempted assignment to XZR. \n");
		} else {
			registerFile[destReg] = result;
			cpuLog.append("ADDS \t X" + destReg + ", X" + op1Reg + ", X" + op2Reg + "\n");
		}
		ADDSetFlags(result, registerFile[op1Reg], registerFile[op2Reg]);
		cpuLog.append("Set flags + \n");
	}

    private void SUB(int destReg, int op1Reg, int op2Reg) {
		if (destReg == XZR) {
			cpuLog.append("Ignored attempted assignment to XZR. \n");
		} else {
			registerFile[destReg] = registerFile[op1Reg] - registerFile[op2Reg];
			cpuLog.append("SUB \t X" + destReg + ", X" + op1Reg + ", X" + op2Reg + "\n");
		}
	}

    private void SUBS(int destReg, int op1Reg, int op2Reg) {
		long result = registerFile[op1Reg] - registerFile[op2Reg];
		if (destReg == XZR) {
			cpuLog.append("Ignored attempted assignment to XZR. \n");
		} else {
			registerFile[destReg] = result;
			cpuLog.append("SUBS \t X" + destReg + ", X" + op1Reg + ", X" + op2Reg + "\n");
		}
		SUBSetFlags(result, registerFile[op1Reg], registerFile[op2Reg]);
		cpuLog.append("Set flags + \n");
	}

    private boolean branchTaken = false;
	private boolean STXRSucceed = false;
	private StringBuilder cpuLog = new StringBuilder("");
	private long[] registerFile;
	private long taggedAddress;
	private int instructionIndex;
	private boolean Nflag;
	private boolean Zflag;
	private boolean Cflag;
	private boolean Vflag;

}