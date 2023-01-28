package altrisi.sisaassembler;

import static altrisi.sisaassembler.TestUtils.*;

import org.junit.jupiter.api.Test;

class CommentTest {
	@Test
	void fullLineComments() throws Exception {
		assertCompiles("""
				; this is a full line comment
				ADD R0, R0, R0
				; this is another full line comment
				""")
			.toSingleInstruction();
	}
	
	@Test
	void inlineComments() throws Exception {
		assertCompiles("""
				ADD R0, R0, R0 ; this is an inline comment
				""")
			.toSingleInstruction();
	}
}
