package altrisi.sisaassembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static altrisi.sisaassembler.TestUtils.*;
import static altrisi.sisaassembler.Utils.*;

import org.junit.jupiter.api.Test;

class DisplayTests {
	@Test
	void testShort2Bin() {
		byte[] bytes = new byte[2];

		SHORT_VIEW.set(bytes, 0, (short)0b0001110101010001);
		assertEquals(shortToString(bytes, BIN), "0001110101010001");

		SHORT_VIEW.set(bytes, 0, (short)0b0001110101010101);
		assertEquals(shortToString(bytes, BIN), "0001110101010101");

		SHORT_VIEW.set(bytes, 0, (short)0b1001110101010001);
		assertEquals(shortToString(bytes, BIN), "1001110101010001");
	}

	@Test
	void testShort2Hex() {
		byte[] bytes = new byte[2];

		SHORT_VIEW.set(bytes, 0, (short)0x23F9);
		assertEquals(shortToString(bytes, HEX), "23f9");
		
		SHORT_VIEW.set(bytes, 0, (short)0xF309);
		assertEquals(shortToString(bytes, HEX), "f309");
		
		SHORT_VIEW.set(bytes, 0, (short)0x00F9);
		assertEquals(shortToString(bytes, HEX), "00f9");
	}
}
