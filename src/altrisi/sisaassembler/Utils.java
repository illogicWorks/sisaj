package altrisi.sisaassembler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	public static final int HEX = 16;
	public static final int BIN = 2;
	public static final Pattern COMMA_SPACE = Pattern.compile("\\s*,\\s*");
	public static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");
	public static boolean earlyExit;

	public static String shortToString(byte[] b, int radix) {
		assert b.length == 2;
		return byteToString(b[1], radix) + byteToString(b[0], radix);
	}

	private static String byteToString(byte b, int radix) {
		String str = Integer.toString(Byte.toUnsignedInt(b), radix);
		int requiredLength = switch (radix) {
			case BIN -> 8;
			case HEX -> 2;
			default -> throw new IllegalArgumentException("radix");
		};
		if (str.length() != requiredLength) {
			return "0".repeat(requiredLength - str.length()) + str;
		} else {
			return str;
		}
	}
	
	/**
	 * Encodes the registry String str in the lsb of the returned byte
	 * @param str The registry string to parse, type "R5"
	 * @return A byte with the 3 lsb being the address of this byte
	 * @throws AssembleException If the passed string isn't a valid registry
	 */
	public static byte parseReg(String str) throws AssembleException {
		if (str.length() != 2 || str.charAt(0) != 'R' || !Character.isDigit(str.charAt(1)))
			throw new AssembleException("Incorrect register declaration '" + str + "'");
		byte ret = (byte)(str.charAt(1) - '0');
		if (ret > 7) {
			throw new AssembleException("Undefined register " + ret);
		}
		return ret;
	}
	
	public static byte parseConstant(String str, boolean sixBits) throws AssembleException {
		int radix;
		if (str.startsWith("0x")) {
			radix = HEX;
		} else if (str.startsWith("0b")) {
			radix = BIN;
		} else {
			return parseDec(str, sixBits);
		}
		
		int asInt;
		try {
			asInt = Integer.parseUnsignedInt(str, 2, str.length(), radix);
		} catch (NumberFormatException e) {
			throw new AssembleException("Invalid constant: " + str, e);
		}
		if (Integer.compareUnsigned(asInt, 2 * Byte.MAX_VALUE) > 0) {
			throw new AssembleException("Oversized constant: " + str);
		}
		byte res = 0;
		res |= asInt; // copy bits
		return res;
	}
	
	private static final byte MAX_6BIT_CONSTANT_POSITIVE = (byte)0b00111111;
	private static final byte MIN_6BIT_CONSTANT = (byte)0b11100000;
	private static byte parseDec(String str, boolean sixBits) throws AssembleException {
		int asInt;
		try {
			asInt = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			throw new AssembleException("Invalid constant: " + str, e);
		}
		if (asInt > (sixBits ? MAX_6BIT_CONSTANT_POSITIVE : 2 * Byte.MAX_VALUE) // we need to handle unsigned constants
				|| asInt < (sixBits ? MIN_6BIT_CONSTANT : Byte.MIN_VALUE))
		{
			throw new AssembleException("Oversized constant: " + str);
		}
		byte res = 0;
		res |= asInt;
		if (sixBits && res < 0) {
			// blank 2 msb that are 1 because of Ca2
			res &= 0b00111111;
		}
		return res;
	}

	public record MemAddress(byte reg, byte offset) {}
	/**
	 * ^ and $ requires to make it exact
	 * (-?\d+) match digits as first group, optionally signed (done in the next one: hex and bin)
	 * \s*  allow spaces after constant, after open paren and before close paren
	 * (R\d) match register as second group, with R to reuse {@link #parseReg(String)}
	 * parens are outside to not match them
	 */
	private static final Pattern ADDRESS_MATCHER = Pattern.compile("^(-?\\d+)\\s*\\(\\s*(R\\d)\\s*\\)$");
	// is regex even worth it anymore?
	private static final Pattern ADDRESS_MATCHER_HEX_BIN = Pattern.compile("^(-?\\d+|0b[01]+|0x[\\dA-F]+)\\s*\\(\\s*(R\\d)\\s*\\)$");
	public static MemAddress parseMemoryAddress(String str) throws AssembleException {
		// N6(Ra)
		Matcher m = ADDRESS_MATCHER_HEX_BIN.matcher(str);
		if (!m.matches()) {
			throw new AssembleException("Invalid memory address: '" + str + "', must be of type N6(Rn)");
		}
		byte offset = parseConstant(m.group(1), true);
		byte reg = parseReg(m.group(2));
		return new MemAddress(reg, offset);
	}
}
