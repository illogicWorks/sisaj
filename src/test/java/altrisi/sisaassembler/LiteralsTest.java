package altrisi.sisaassembler;

import static altrisi.sisaassembler.TestUtils.assertCompiles;
import static altrisi.sisaassembler.TestUtils.assertDoesntCompile;

import org.junit.jupiter.api.Test;

// TODO should maybe test Utils.parseConstant instead
public class LiteralsTest {
	@Test
	void binary() throws Exception {
		assertCompiles("MOVI R0, 0b10001")
			.toSingleInstruction();
	}

	@Test
	void hex() throws Exception {
		assertCompiles("MOVI R0, 0x12")
			.toSingleInstruction();
	}

	@Test
	void decimal() throws Exception {
		assertCompiles("MOVI R0, 12")
			.toSingleInstruction();
	}

	@Test
	void negative() throws Exception {
		assertCompiles("MOVI R0, -12")
			.toSingleInstruction();
	}
	
	@Test
	void unsignedByte() throws Exception {
		assertCompiles("MOVI R0, 180")
			.toSingleInstruction();
	}

	@Test
	void noNegativeByte() throws Exception {
		assertDoesntCompile("MOVI R0, -180");
	}
}
