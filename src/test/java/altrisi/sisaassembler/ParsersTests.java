package altrisi.sisaassembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ParsersTests {
	@TestFactory
	Stream<DynamicTest> registries() {
		return IntStream.range(0, 8)
			.mapToObj(i -> dynamicTest("Parsing of registry " + i, () -> assertEquals(i, Utils.parseReg("R" + i))));
	}

	@TestFactory
	Stream<DynamicTest> oversizedRegs() {
		return new Random().ints()
				.filter(i -> i > 7 || i < 0)
				.limit(80)
				.mapToObj(i -> dynamicTest("Parsing of registry " + i, () -> assertThrows(AssembleException.class, () -> Utils.parseReg("R" + i))));
	}

	@Test
	void smallOversizedRegs() {
		assertThrows(AssembleException.class, () -> Utils.parseReg("R8"), "R8 passed");
		assertThrows(AssembleException.class, () -> Utils.parseReg("R9"), "R9 passed");
		assertThrows(AssembleException.class, () -> Utils.parseReg("R10"), "R10 passed");
		assertThrows(AssembleException.class, () -> Utils.parseReg("R01"), "R01 passed");
	}
}
