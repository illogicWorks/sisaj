package altrisi.sisaassembler;

/**
 * An {@link AssembleException} indicates the cause of a failure in the assembly of an instruction
 * in an {@link Assembler}, as its {@link #getMessage() detail message}. It may also contain a {@link #getCause() cause},
 * though it's not required (it may be {@code null}).<p>
 * 
 * It will be passed to its {@link Assembler#failedLine(String, int, AssembleException)} method.<p>
 * 
 * @author altrisi
 *
 */
@SuppressWarnings("serial")
public class AssembleException extends Exception {
	AssembleException(String msg) {
		super(msg);
	}

	AssembleException(String msg, Throwable cause) {
		super(msg, cause);
	}

	@Override
	public Throwable fillInStackTrace() {
		// skip stack trace generation unless excessively verbose
		return Logging.excessivelyVerbose ? super.fillInStackTrace() : this;
	}
}
