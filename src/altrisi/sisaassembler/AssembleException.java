package altrisi.sisaassembler;

@SuppressWarnings("serial")
public class AssembleException extends Exception {
	public AssembleException(String msg) {
		super(msg);
	}

	public AssembleException(String msg, Throwable cause) {
		super(msg, cause);
	}

	@Override
	public Throwable fillInStackTrace() {
		// skip stack trace generation unless excessively verbose
		return Logging.excessivelyVerbose ? super.fillInStackTrace() : this;
	}
}
