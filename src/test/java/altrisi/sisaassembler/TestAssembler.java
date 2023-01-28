package altrisi.sisaassembler;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;

public class TestAssembler extends Assembler {
	private final ByteArrayOutputStream result;
	private final boolean failOnError;
	private TestAssembler(ByteArrayOutputStream out, boolean failOnError) {
		super(out);
		this.result = out;
		this.failOnError = failOnError;
	}

	public TestAssembler(boolean failOnError) {
		this(new ByteArrayOutputStream(), failOnError);
	}

	@Override
	public void failedLine(String line, int lineNo, AssembleException exception) {
		if (failOnError)
			fail("Assembly failed in instruction " + lineNo + " for '" + line + "'", exception);
	}
	public byte[] result() {
		return result.toByteArray();
	}
}
