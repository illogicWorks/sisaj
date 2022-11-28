package altrisi.sisaassembler;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import altrisi.sisaassembler.InstructionAssembler.*;

import static java.nio.file.StandardOpenOption.*;
import static java.util.Map.entry;
import static java.util.function.Predicate.not;
import static altrisi.sisaassembler.Logging.*;
import static altrisi.sisaassembler.Utils.*;
import static altrisi.sisaassembler.Instructions.*;

public class Assembler implements Closeable {
	private final OutputStream out;
	private int errors;
	private final byte[] instructionBuff = new byte[2];
	private static final boolean LEFT = true;
	private static final boolean RIGHT = false;
	private static final Map<String, InstructionAssembler> HANDLERS = Map.ofEntries(
			// OPS
			reg3("AND", OPS, AND),
			reg3("OR",  OPS, OR ),
			reg3("XOR", OPS, XOR),
			entry("NOT", (line, bytes) -> new Reg3(OPS, NOT).assemble(line + ", R0", bytes)),
			reg3("ADD", OPS, ADD),
			reg3("SUB", OPS, SUB),
			reg3("SHA", OPS, SHA),
			reg3("SHL", OPS, SHL),
			// CMP
			reg3("CMPLT",  CMP, CMPLT),
			reg3("CMPLE",  CMP, CMPLE),
			reg3("CMPEQ",  CMP, CMPEQ),
			reg3("CMPLTU", CMP, CMPLTU),
			reg3("CMPLEU", CMP, CMPLEU),
			reg2("ADDI", ADDI, true),
			reg2("JALR", JALR, false),
			mem("LD", LD, RIGHT),
			mem("ST", ST, LEFT),
			mem("LDB", LDB, RIGHT),
			mem("STB", STB, LEFT),
			reg1("BZ",    JUMP, 0, LEFT),
			reg1("BNZ",   JUMP, 1, LEFT),
			reg1("MOVI",  MOVE, 0, LEFT),
			reg1("MOVHI", MOVE, 0, LEFT),
			reg1("IN",    IO,   0, LEFT),
			reg1("OUT",   IO,   1, RIGHT)
		);
	
	public Assembler(Path output) throws IOException {
		debug("Setting output to file " + output);
		this.out = Files.newOutputStream(output, CREATE, TRUNCATE_EXISTING);
	}

	public Assembler(OutputStream out) {
		this.out = out;
	}

	public void assemble(Path input) throws IOException {
		debug("Starting assembly of file " + input);
		try (var lines = Files.lines(input)) {
			assemble(lines);
		}
	}

	// @VisibleForTesting
	public void assemble(Stream<String> lines) throws IOException {
		debugSeparator();
		int lineNo = 1;
		for (String line : (Iterable<String>)() -> lines.map(String::strip).filter(not(String::isEmpty)).filter(s -> !s.startsWith(";")).iterator()) {
			parseLine(lineNo, line);
			lineNo++;
			debugSeparator();
		}
		if (!failed()) debug("Finished compilation of " + lineNo + " lines");
	}

	public void parseLine(int lineNo, String str) throws IOException {
		debug("Assembling instruction '" + str + "'" + " in line " + lineNo);

		String[] decomposed = MULTI_WHITESPACE.split(str, 2);
		InstructionAssembler operation = HANDLERS.get(decomposed[0]);

		if (operation == null) {
			failedLine(str, lineNo, new AssembleException("Operation '" + decomposed[0] + "' not found"));
		} else if (decomposed.length == 1) {
			failedLine(str, lineNo, new AssembleException("Operation '" + decomposed[0] + "' takes arguments, found none"));
		} else {
			debug("Using operator: " + operation);
			try {
				operation.assemble(decomposed[1], instructionBuff);
				if (verbose) // guard for expensive enough 2x String conversion
					debug("Compiled to 0x" + shortToString(instructionBuff, HEX).toUpperCase() + " (" + shortToString(instructionBuff, BIN) + ")");
				// TODO assert correct endianness
//				out.write(instructionBuff[1]);
//				out.write(instructionBuff[0]);
				out.write(instructionBuff);
			} catch (AssembleException e) {
				failedLine(str, lineNo, e);
			}
		}
	}
	
	private void failedLine(String line, int lineNo, AssembleException e) {
		errors++;
		error("Compilation error in line " + lineNo + ": " + line, e);
		if (Utils.earlyExit) {
			fatal("Exiting because of early-exit setting");
		}
	}
	
	public boolean failed() {
		return errors != 0;
	}

	public int errors() {
		return errors;
	}

	@Override
	public void close() throws IOException {
		debug("Closing output stream");
		out.close();
	}

	// "Utils" for map creation
	private static Map.Entry<String, InstructionAssembler> reg3(String op, byte c, byte f) {
		return entry(op, new Reg3(c, f));
	}

	private static Map.Entry<String, InstructionAssembler> reg2(String op, byte c, boolean takeConstant) {
		return entry(op, new Reg2(c, takeConstant));
	}

	private static Map.Entry<String, InstructionAssembler> reg1(String op, byte c, int flag, boolean atLeft) {
		return entry(op, new Reg1(c, (byte)flag, atLeft));
	}
	
	private static Map.Entry<String, InstructionAssembler> mem(String op, byte c, boolean atLeft) {
		return entry(op, new Memory(c, atLeft));
	}

}
