package altrisi.sisaassembler;

import java.io.PrintStream;

class Logging {
	public static boolean verbose;
	public static boolean excessivelyVerbose;
	public static void debug(String str) {
		if (verbose) System.out.println("[DEBUG]: " + str);
		if (excessivelyVerbose) {
			printStack(System.out);
		}
	}

	public static void debugSeparator() {
		if (verbose) {
			System.out.println("-----------------------------------");
		}
	}

	public static void error(String str) {
		if (excessivelyVerbose) {
			System.err.println("[ERROR]: " + str);
			printStack(System.err);
		} else {
			System.err.println(str);
		}
	}

	public static void error(String str, Throwable t) {
		if (excessivelyVerbose) {
			System.err.println("[ERROR]: " + str + ": " + t.getMessage());
			printStack(System.err);
		} else {
			System.err.println(str + ": " + t.getMessage());
		}
	}

	public static void fatal(String str) {
		if (excessivelyVerbose) {
			throw new RuntimeException(str);
		} else {
			System.err.println(str);
			System.exit(-1);
		}
	}

	public static void fatal(String str, Throwable t) {
		if (excessivelyVerbose) {
			throw new RuntimeException(str, t);
		} else {
			System.err.println(str + ": " + t.getMessage());
			t.printStackTrace();
			System.exit(-1);
		}
	}

	public static void info(Object str) {
		if (excessivelyVerbose) {
			System.out.println("[INFO]: " + str);
			printStack(System.out);
		} else {
			System.out.println(str);
		}
	}
	
	/**
	 * Prints the current stack trace, other than this method and the caller method
	 * @param out The {@link PrintStream} to print to
	 */
	// @CallerSensitive
	private static void printStack(PrintStream out) {
		StackWalker.getInstance().walk(stream -> {
			stream.skip(2).forEach(element -> out.println("\t" + element));
			return null;
		});
	}
}
