import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class Micro86 {
	
	private static int numOfMemory = 60;
	public static int [] memory = new int[numOfMemory];
	private final static String ZEROES = "00000000";
	public static int instructionRegister;
	public static int accumulator;
	public static int instructionPointer;
	public static boolean zeroFlag; // true when the operation resulted in 0
	public static boolean negFlag; // true when it resulted in a negative value.
	public static String fileName;
	public static boolean trace = false;
	public static boolean dump = false;
	public static Scanner myScanner = new Scanner(System.in);
	
	public static void main(String[] args) throws Exception {
		processCommandLine(args);
		
		bootUp();
		loader(fileName);
		fetchExecuteAll();
		
		if(dump)postMortemDump();
	}
	static void processCommandLine(String [] args) {
		boolean sawAnError = false;

		for (String arg : args)
			if (arg.startsWith("-")) {
				if (arg.substring(1).equals("d"))
					dump = true;
				else if (arg.substring(1).equals("t"))
					trace = true;
				else {
					System.err.println("Unknown option " + arg);
					sawAnError = true;
				}
			}
			else
				fileName = arg;

		if (fileName == null) {		// filename MUST be present on command-line
			System.err.println("Missing filename");
			sawAnError = true;
		}
			
		if (sawAnError) {
			System.err.println("Usage: CommandLineProcessor {-c, -d, -t} <filename>");
			System.exit(1);
			
		}
	}
	
	public static void bootUp(){
		System.out.println(header());
		
		accumulator = 0;
		instructionPointer = 0;
		zeroFlag = false;
		negFlag = false;
		
		for(int i = 0; i < numOfMemory; i++){
			memory[i] = 0;
		}
		
		instructionRegister = memory[0];
	}

	public static String header(){
		String header = "================================\n"
				+ "Micro86 Emulator version 1.0\n"
				+ "================================\n"
				+ "Executable file: " + fileName;
		return header;
	}
	
	public static void fetchExecuteAll(){
		complete:
			while(true){
				fetchExecute();
				if(getMnemonic(extractOpCode(instructionRegister)) == "HALT"// ||
				   //getMnemonic(extractOpCode(instructionRegister)) == "UNKOWN_OP_CODE_ERROR"
						){
					break complete;
				}
			}
	}
	
	public static void memoryDump(){
		System.out.println("---Memory---");
		
		for(int i = 0; i < numOfMemory; i++){
			String memoryIndex = Integer.toString(i, 16);
			String value = Integer.toHexString(memory[i]);
			String memIndexPad = memoryIndex.length() <= 10 ? ZEROES.substring(memoryIndex.length()) + memoryIndex : memoryIndex;
			String valuePad = value.length() <= 10 ? ZEROES.substring(value.length()) + value : value;
			System.out.println(memIndexPad.toUpperCase() + ": " + valuePad.toUpperCase());
		}
		System.out.println("----------");
	}
	
	public static String returnRegisters(){
		//	Registers acc: 00000000 ip: 00000000 flags: 00000000 ir: 00000000
		String acc = Integer.toHexString(accumulator);
		String accPad = acc.length() <= 10 ? ZEROES.substring(acc.length()) + acc : acc;
		String reg = Integer.toHexString(instructionRegister);
		String regPad = reg.length() <= 10 ? ZEROES.substring(reg.length()) + reg : reg;
		String ip = Integer.toHexString(instructionPointer);
		String ipPad = ip.length() <= 10 ? ZEROES.substring(ip.length()) + ip : ip;
		String zFlag = Integer.toHexString(boolToInt(zeroFlag));
		String zFlagPad = zFlag.length() <= 10 ? ZEROES.substring(zFlag.length()) + zFlag : zFlag;
		String nFlag = Integer.toHexString(boolToInt(negFlag));
		String nFlagPad = nFlag.length() <= 10 ? ZEROES.substring(nFlag.length()) + nFlag : nFlag;
		String s = "Registers acc: " + accPad.toUpperCase() + " ip: " + ipPad.toUpperCase() + " zFlag: " + zFlagPad + " nFlag: " + nFlagPad.toUpperCase() + " ir: " +  regPad;
		return s;
	}
	
	public static void printRegisters(){
		System.out.println(returnRegisters());
	}
	
	public static void loader(String fileStringName){
		File file = new File(fileStringName);
        Scanner myScanner = null;
        int index = 0;
        
        try {
			myScanner = new Scanner(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Eror: Filename \"" + fileStringName + "\" not found, exiting.");
			System.exit(0);
		}
        
		while(myScanner.hasNextInt(16)){
			memory [index] = myScanner.nextInt(16);
			index++;
			
		}
		dissasembler();
		if (trace) tracing();
	}
	
	public static int boolToInt(boolean b) {
	    return b ? 1 : 0;
	}

	// takes an integer, and viewing it as an instruction, extracts the opcode (upper 16 bits) from it 
	public static int extractOpCode(int instruction){
		int opCode = instruction >>> 16;
		return opCode;
	}

	// accepts an opcode and returns the corresponding mnemonic
	public static String getMnemonic(int opCode){
		String sOp = "";
		// System Control 
		switch(opCode){
		case 0x0100: sOp = "HALT"; //Halt execution 
					 break;
		// Data Movement
		case 0x0202: sOp = "LOAD"; // Move word at specified address in memory into accumulator 
					break;
		case 0x0201: sOp = "LOADI"; // Move immediate value (stored in instruction) into accumulator 
					break;
		case 0x0302: sOp = "STORE";  // Move word in accumulator to specified address
					break;
		// Arithmetic
		case 0x0402: sOp = "ADD"; // Add word at specified address to accumulator, result stored in accumulator 
					break;
		case 0x0401: sOp = "ADDI"; // Add immediate value to accumulator 
					break;
		case 0x0502: sOp = "SUB"; // Subtract word at specified address from accumulator 
					break;
		case 0x0501: sOp = "SUBI"; // Subtract immediate value to accumulator 
					break;
		case 0x0602: sOp = "MUL"; // Multiply accumulator by word at specified address 
					break;
		case 0x0601: sOp = "MULI"; // Multiply accumulator by immediate value 
					break;
		case 0x0702: sOp = "DIV"; // Divide accumulator by word at specified address 
					break;
		case 0x0701: sOp = "DIVI"; // Divide accumulator by immediate value 
					break;
		case 0x0802: sOp = "MOD"; // Take remainder of accumulator divided by word at specified address 
			
		case 0x0801: sOp = "MODI"; // Take remainder of accumulator divided by immediate value 
					break;
		// Comparison 
		case 0x0902: sOp = "CMP"; // Compare accumulator to word at specified address 
					break;
		case 0x0901: sOp = "CMPI"; // Compare accumulator to immediate value 
					break;
		// Branching
		case 0x0A01: sOp = "JMPI"; // Jump to address contained in immediate value 
					break;
		case 0x0B01: sOp = "JEI"; // Jump on equal to address contained in immediate value 
					break;
		case 0x0C01: sOp = "JNEI"; // Jump on not equal to address contained in immediate value  
					break;
		case 0x0D01: sOp = "JLI"; // Jump on less than to address contained in immediate value 
					break;
		case 0x0E01: sOp = "JLEI"; // Jump on less than or equal to address contained in immediate value 
					break;
		case 0x0F01: sOp = "JGI"; // Jump on greater than to address contained in immediate value 
					break;
		case 0x1001: sOp = "JGEI"; // Jump on greater than or equal to address contained in immediate value 
					break;
		// Input/Output 
		case 0x1100: sOp = "IN"; // Input byte from input port to accumulator 
					break;
		case 0x1200: sOp = "OUT"; // Output byte from accumulator to output port 
					break;
		default:
			sOp = "UNKOWN_OP_CODE_ERROR";
					break;
		}
		return sOp;
	}
	
	// takes an integer, and viewing it as an instruction, extracts the operand (lower 16 bits) from it 
	public static int extractOperand (int instruction){
		int operand = instruction & 0x0000FFFF;
		return operand;
	}
	
	// goes through memory taking each word and uses the above methods to get the mnemonic and operand and prints it out
	public static void dissasembler(){
		System.out.println("\n===== Disassembled Code =====");
		for(int i = 0; i < numOfMemory; i++){
			System.out.println("mem[i]: " +memory[i]);

			String index = Integer.toHexString(i);
			String opCode = getMnemonic(extractOpCode(memory[i]));
			String value = Integer.toHexString(extractOperand(memory[i]));
			String lineNum= index.length() <= 8 ? ZEROES.substring(index.length()) + index : index;
			String valueNum= value.length() <= 8 ? ZEROES.substring(value.length()) + value : value;

			System.out.print(lineNum +": ");
			if(opCode == "HALT"){
				System.out.println(opCode);
				System.out.println("...");
				break;
			}else
				System.out.println(opCode + " " + valueNum.toUpperCase());
		}
	}
	
	// fetches the word from memory whose address (index) is contained in the instruction pointer register, 
	// places the word into the instruction register, and then adds 1 to that register (to set up for the next fetch)
	public static void fetchExecute(){
		
		String opCode = getMnemonic(extractOpCode(memory[instructionPointer]));
		instructionRegister = memory[instructionPointer];

		switch (Op.valueOf(opCode)){
			case LOAD: accumulator = extractOperand(memory[extractOperand(instructionRegister)]); // Move word at specified address in memory into accumulator 
						break;
			case STORE: memory[extractOperand(instructionRegister)] = accumulator; // Move word in accumulator to specified address
						break;
			case HALT: System.out.println(returnRegisters()); // Halt Execution
						memoryDump();
						if(trace)printRegisters();
						return;
			case LOADI: accumulator = extractOperand(instructionRegister);// Move immediate value (stored in instruction) into accumulator 
						break;			
			case ADD: accumulator +=  extractOperand(memory[extractOperand(instructionRegister)]); // Add word at specified address to accumulator, result stored in accumulator 
						break;
			case ADDI: accumulator += extractOperand(instructionRegister); // Add immediate value to accumulator 
						break;
			case SUB: accumulator -=  extractOperand(memory[extractOperand(instructionRegister)]); // Subtract word at specified address from accumulator 
						break;
			case SUBI: accumulator -= extractOperand(instructionRegister); // Subtract immediate value to accumulator 
						break;
			case MUL: accumulator *=  extractOperand(memory[extractOperand(instructionRegister)]); // Multiply accumulator by word at specified address 
						break;
			case MULI: accumulator *= extractOperand(instructionRegister); // Multiply accumulator by immediate value 
						break;
			case DIV: accumulator /=  extractOperand(memory[extractOperand(instructionRegister)]); // Divide accumulator by word at specified address 
						break;
			case DIVI: accumulator /= extractOperand(instructionRegister); // Divide accumulator by immediate value
						break;
			case MOD: try{
							accumulator %= extractOperand(memory[extractOperand(instructionRegister)]); // Take remainder of accumulator divided by word at specified address 
						}catch(ArithmeticException e){
							accumulator=0;
						}
						break;
			case MODI: try{
							accumulator %= extractOperand(instructionRegister); // Take remainder of accumulator divided by immediate value 
						}catch(ArithmeticException e){
							accumulator=0;
						}
						break;
			case CMP:  if((accumulator - extractOperand(memory[extractOperand(instructionRegister)]))>0){ // Compare accumulator to word at specified address 
							zeroFlag=false;
							negFlag=false;
						}else if (accumulator - extractOperand(memory[extractOperand(instructionRegister)])==0){
							zeroFlag = true;
							negFlag=false;
						}else{
							zeroFlag=false;
							negFlag = true;
						}
						break;
			case CMPI: if(accumulator - extractOperand(instructionRegister)>0){ // Compare accumulator to immediate value
							zeroFlag=false;
							negFlag=false;
						}else if (accumulator - extractOperand(instructionRegister)==0){
							zeroFlag = true;
							negFlag=false;
						}else{
							zeroFlag=false;
							negFlag = true;
						}
						break;
			case JMPI: instructionPointer = extractOperand(instructionRegister); // Jump to address contained in immediate value 
						instructionRegister = memory[instructionPointer]; 
						if(trace)printRegisters();
						return;
			case JEI: if(zeroFlag){ instructionPointer = extractOperand(instructionRegister); // Jump on equal to address contained in immediate value
							instructionRegister = memory[instructionPointer];
							if(trace)printRegisters();
							return;
						}
						break;
			case JNEI: if(!zeroFlag){ instructionPointer = extractOperand(instructionRegister); // Jump on not equal to address contained in immediate value  
							instructionRegister = memory[instructionPointer];
							if(trace)printRegisters();
							return;
						}
						break;
			case JLI: if(negFlag){ instructionPointer = extractOperand(instructionRegister); // Jump on less than to address contained in immediate value 
							instructionRegister = memory[instructionPointer];
							if(trace)printRegisters();
							return;
						}
						break;
			case JLEI: if(zeroFlag || negFlag){ instructionPointer = extractOperand(instructionRegister); // Jump on less than or equal to address contained in immediate value 
							instructionRegister = memory[instructionPointer];
							if(trace)printRegisters();
							return;
						}
						break;
			case JGI: if(!zeroFlag && !negFlag){ instructionPointer = extractOperand(instructionRegister); // Jump on greater than to address contained in immediate value 
							instructionRegister = memory[instructionPointer];
							if(trace)printRegisters();
							return;
						}
						break;
			case JGEI: if(zeroFlag || (!zeroFlag && !negFlag)){ instructionPointer = extractOperand(instructionRegister); // Jump on greater than or equal to address contained in immediate value
							instructionRegister = memory[instructionPointer];
							if(trace)printRegisters();
							return;
						}
						break;
			case IN:  accumulator = myScanner.nextInt(); // Input byte from input port to accumulator 
						break;
			case OUT: System.out.print((char)accumulator);// Output byte from accumulator to output port 
						break;
		}				
		if(trace)printRegisters();
		
		instructionPointer++;
	}
	
	// takes the instruction from the instruction register, extracts both the opcode and operand (i.e., you are decoding the instruction)
	public static String instructionExtractor(){
		String opCode = getMnemonic(extractOpCode(instructionRegister));
		String value = Integer.toHexString(extractOperand(instructionRegister));
		String result = opCode + " " + value;
		return result;
	}
	public static void postMortemDump(){
		System.out.println("\n===== Post-Mortem Dump (normal termination) =====\n"
				+ "--------------------\n"
				+ returnRegisters());
		memoryDump();
	}
	public static void tracing(){
		System.out.println("===== Execution Trace =====");
		memoryDump();
	}
	
	public enum Op
	 {
		LOAD, LOADI, ADD, ADDI, SUB, SUBI, HALT, STORE, MUL, MULI, DIV, DIVI, MOD, MODI, CMP, CMPI,
	     JMPI, JEI, JNEI, JLI, JLEI, JGI, JGEI, IN, OUT;
	 }
}

