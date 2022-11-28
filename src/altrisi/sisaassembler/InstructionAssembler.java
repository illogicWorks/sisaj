package altrisi.sisaassembler;

import static altrisi.sisaassembler.Utils.*;

@FunctionalInterface
public interface InstructionAssembler {
	void assemble(String args, byte[] buff) throws AssembleException;
	
	/**
	 * @param op Op being in the most-significant bits
	 * @param f  F in the least significant bits
	 */
	record Reg3(byte op, byte f) implements InstructionAssembler {
		@Override
		public void assemble(String args, byte[] buff) throws AssembleException {
			String[] strs = COMMA_SPACE.split(args);
			if (strs.length != 3) {
				throw new AssembleException("Got " + strs.length + " arguments for 3-reg instruction");
			}
			byte dest = parseReg(strs[0]);
			byte regA = parseReg(strs[1]);
			byte regB = parseReg(strs[2]);

			byte left = op;
			left |= regA << 1;
			left |= regB >> 2;

			byte right = f;
			right |= regB << 6;
			right |= dest << 3;

			buff[0] = left;
			buff[1] = right;
		}
	}

	record Reg2(byte op, boolean takeConstant) implements InstructionAssembler {
		@Override
		public void assemble(String args, byte[] buff) throws AssembleException {
			String[] strs = COMMA_SPACE.split(args);
			if (strs.length != (takeConstant ? 3 : 2)) {
				throw new AssembleException("Got " + strs.length + " arguments for 2-reg instruction");
			}

			byte dest = parseReg(strs[0]);
			byte regA = parseReg(strs[1]);

			byte constant = 0;
			if (takeConstant) {
				constant = parseConstant(strs[2], true);
			}

			byte left = op;
			left |= regA << 1;
			left |= dest >> 2;

			byte right = constant;
			right |= dest << 6;

			buff[0] = left;
			buff[1] = right;
		}
	}

	record Reg1(byte op, byte flag, boolean regAtLeft) implements InstructionAssembler {
		@Override
		public void assemble(String args, byte[] buff) throws AssembleException {
			String[] strs = COMMA_SPACE.split(args);
			if (strs.length != 2) {
				throw new AssembleException("Got " + strs.length + " arguments for 2-reg instruction");
			}

			byte reg = parseReg(strs[regAtLeft ? 0 : 1]);
			byte left = op;
			left |= reg << 1;
			left |= flag;

			byte right = parseConstant(strs[regAtLeft ? 1 : 0], false);

			buff[0] = left;
			buff[1] = right;
		}
	}
	
	record Memory(byte op, boolean memoryAtLeft) implements InstructionAssembler {
		@Override
		public void assemble(String args, byte[] buff) throws AssembleException {
			String[] strs = COMMA_SPACE.split(args);
			if (strs.length != 2) {
				throw new AssembleException("Got " + strs.length + " comma-separated arguments for memory instruction, expected 2");
			}

			byte reg = Utils.parseReg(strs[memoryAtLeft ? 1 : 0]);
			MemAddress addr = Utils.parseMemoryAddress(strs[memoryAtLeft ? 0 : 1]);

			byte left = op;
			left |= addr.reg() << 1;
			left |= reg >> 2;

			byte right = addr.offset();
			right |= reg << 6;

			buff[0] = left;
			buff[1] = right;
		}
	}
}
