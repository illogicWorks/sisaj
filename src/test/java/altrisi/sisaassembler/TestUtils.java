package altrisi.sisaassembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public interface TestUtils {
	VarHandle SHORT_VIEW = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

	static CompilationResult assertCompiles(String code) throws IOException {
		try (var assembler = new TestAssembler(true)) {
			assembler.assemble(code.lines());
			return new CompilationResult(assembler.result());
		}
	}
	
	static void assertDoesntCompile(String code) throws IOException {
		try (var assembler = new TestAssembler(false)) {
			assembler.assemble(code.lines());
			assertTrue(assembler.failed());
		}
	}

	record CompilationResult(byte[] result) {
		public void toSingleInstruction() {
			assertEquals(result.length, 2, "Compiled to multiple instructions");
		}

		public void toSingleInstruction(short expected) {
			toSingleInstruction();
			assertEquals(expected, SHORT_VIEW.get(result, 0), "Incorrect compilation result");
		}
	}
}
