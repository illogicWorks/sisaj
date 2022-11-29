package altrisi.sisaassembler;

import joptsimple.*;
import joptsimple.util.PathConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static joptsimple.util.PathProperties.*;
import static altrisi.sisaassembler.Utils.*;
import static altrisi.sisaassembler.Logging.*;

class Main {
	private static final boolean DEV_ENV = false;
	private static final String VERSION = "0.2.0";

	public static void main(String[] args) {
		OptionParser parser = new OptionParser();
		var help = parser.accepts("help", "Displays this help menu and exits").forHelp();
		var isVerbose = parser.accepts("verbose", "Logs additional information");
		var isExcessivelyVerbose = parser.accepts("excessivelyVerbose", "Logs too much additional information, including stack traces for all log lines")
				.availableUnless(isVerbose);
		var instruction = parser.accepts("instruction", "A single instruction to convert and print").withRequiredArg();
		var inFile = parser.accepts("file", "The path to the input file to compile").requiredUnless(instruction)
				.withRequiredArg().withValuesConvertedBy(new PathConverter(READABLE));
		var outFile = parser.accepts("output", "The path to the output binary file")
				.requiredIf(inFile).withRequiredArg().withValuesConvertedBy(new PathConverter());
		var earlyExit = parser.accepts("earlyExit", "Makes compilation stop at the first error");
		OptionSet options;
		try {
			options = DEV_ENV ? parser.parse("-vf", "demo.sisa", "-o", "sisa.bin") : parser.parse(args);
		} catch (OptionException e) {
			fatal("Invalid options passed: " + e.getMessage());
			return; // compiler doesn't know it doesn't return
		}

		// don't want to init Logging yet in case I convert it to fully constants, given we don't have verbosity parsed yet
		System.out.println("SISA Assembler " + VERSION);

		if (options.has(help)) {
			info("Command-line reference:");
			try {
				parser.printHelpOn(System.out);
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			System.exit(0);
		}
		excessivelyVerbose = options.has(isExcessivelyVerbose);
		verbose = excessivelyVerbose || options.has(isVerbose);
		Utils.earlyExit = options.has(earlyExit);
		if (Utils.earlyExit) debug("Enabled early exit");

		if (options.has(instruction)) {
			assembleSingle(options.valueOf(instruction));
		} else {
			Path in = options.valueOf(inFile);
			Path out = options.valueOf(outFile);
			try (var assembler = new Assembler(out)) {
				assembler.assemble(in);
				if (assembler.failed()) {
					try {
						Files.deleteIfExists(out);
					} catch (IOException e) { // not reusing the catch below to not fall outside here and keep the error message
						error("Failed to delete output file for failed execution", e);
					}
					if (!verbose) // verbose already writes a separator here
						info("-----------------------------------");
					fatal("Compilation failed with " + assembler.errors() + " error(s)");
				}
			} catch (IOException e) {
				fatal("Error while reading or writing files: ", e);
				// doesn't return
			}
		}

	}

	private static void assembleSingle(String instruction) {
		var out = new ByteArrayOutputStream(2);
		try (var assembler = new Assembler(out)) {
			assembler.assemble(Stream.of(instruction));
			if (assembler.failed()) System.exit(-1); // assembler already sent logs
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		byte[] assembledInstruction = out.toByteArray();
		assert assembledInstruction.length == 2;
		info("Instruction: " + instruction);
		info("0x" + shortToString(assembledInstruction, HEX).toUpperCase() + " (" + shortToString(assembledInstruction, BIN) + ")");
	}
}
