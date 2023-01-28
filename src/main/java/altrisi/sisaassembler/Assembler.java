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

/**
 * The main (and only other than {@link AssembleException}) class for usage as an API, see the 
 * constructors ({@link #Assembler(Path)} and {@link #Assembler(OutputStream)}), the {@code assemble} 
 * methods ({@link #assemble(Path)} and {@link #assemble(Stream)}), and in order to be able
 * to handle errors override {@link #failedLine(String, int, AssembleException)}.<p>
 * 
 * The {@link Assembler} class is {@link Closeable}: You should use it in a try-with-resources block
 * in order to allow its {@link OutputStream} to be closed.<p>
 * 
 * Tip: You can pass a {@link ByteArrayOutputStream} to {@link #Assembler(OutputStream) the constructor} in order
 * for the assembler to just write to an array.
 * 
 * @author altrisi
 */
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

	/**
	 * Creates an Assembler that will output to a file in the given {@link Path},
	 * creating one if it doesn't exist or truncating it if it does.
	 * @param output The {@link Path} of the file the Assembler should output to
	 * @throws IOException If an I/O exception occurs while opening the file
	 */
	public Assembler(Path output) throws IOException {
		debug("Setting output to file " + output);
		this.out = Files.newOutputStream(output, CREATE, TRUNCATE_EXISTING);
	}

	/**
	 * Creates an Assembler that will output to the given {@link OutputStream}.<p>
	 * The {@link OutputStream} will be closed when invoking the {@link #close()} method
	 * @param out The {@link OutputStream} the Assembler should output to
	 */
	public Assembler(OutputStream out) {
		this.out = Objects.requireNonNull(out);
	}

	/**
	 * Assembles all lines in the given input {@link Path} into this Assembler's {@link OutputStream}.<p>
	 * Like with {@link #assemble(Stream)}, errors will be reported to {@link #failedLine(String, int, AssembleException)}.
	 * @param input        The input {@link Path} to read instructions from
	 * @throws IOException If an I/O exception occurs while reading from the input {@link Path}, or while writing to the {@link OutputStream}
	 */
	public final void assemble(Path input) throws IOException {
		debug("Starting assembly of file " + input);
		try (var lines = Files.lines(input)) {
			assemble(lines);
		} catch (UncheckedIOException e) {
			// the Stream has to throw those as the terminal operation can't throw checked. Propogate it ourselves
			throw e.getCause();
		}
	}

	/**
	 * Assembles the given {@link Stream} of {@link String} instructions into this Assembler's {@link OutputStream}.<p>
	 * Errors will be reported to {@link #failedLine(String, int, AssembleException)}.
	 * @param instructions A {@link Stream} of the instructions to assemble
	 * @throws IOException If an I/O exception occurs while writing to the {@link OutputStream}
	 */
	public final void assemble(Stream<String> instructions) throws IOException {
		debugSeparator();
		int lineNo = 1;
		for (String line : iterate(instructions.map(Utils::trimIncludingComments).filter(not(String::isEmpty)))) {
			parseLine(lineNo, line);
			lineNo++;
			debugSeparator();
		}
		if (!failed()) debug("Finished compilation of " + lineNo + " lines");
	}

	private void parseLine(int lineNo, String str) throws IOException {
		debug("Assembling instruction '" + str + "'" + " in line " + lineNo);

		String[] decomposed = MULTI_WHITESPACE.split(str, 2);
		InstructionAssembler operation = HANDLERS.get(decomposed[0]);

		if (operation == null) {
			failLine(str, lineNo, new AssembleException("Operation '" + decomposed[0] + "' not found"));
		} else if (decomposed.length == 1) {
			failLine(str, lineNo, new AssembleException("Operation '" + decomposed[0] + "' takes arguments, found none"));
		} else {
			debug("Using operator: " + operation);
			try {
				operation.assemble(decomposed[1], instructionBuff);
				if (verbose) // guard for expensive enough 2x String conversion
					debug("Compiled to 0x" + shortToString(instructionBuff, HEX).toUpperCase() + " (" + shortToString(instructionBuff, BIN) + ")");
				out.write(instructionBuff);
			} catch (AssembleException e) {
				failLine(str, lineNo, e);
			}
		}
	}
	
	/**
	 * Gets called when a line fails to compile.<p>
	 * If you're using the assembler as an API, overriding this method is your best bet to be able to handle errors
	 * as you like.<p>
	 * @param line   The line contents, after trimming
	 * @param lineNo The line number, with the proper offsets
	 * @param exception The {@link AssembleException} that caused this failure
	 */
	public void failedLine(String line, int lineNo, AssembleException exception) {
		error("Compilation error in line " + lineNo + ": " + line, exception);
		if (Utils.earlyExit) {
			fatal("Exiting because of early-exit setting");
		}
	}

	// need to increment error counter first
	private void failLine(String line, int lineNo, AssembleException exception) {
		errors++;
		failedLine(line, lineNo, exception);
	}
	
	public final boolean failed() {
		return errors != 0;
	}

	public final int errors() {
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
