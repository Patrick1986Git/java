package utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class SecurityUtil {
	private static final SecureRandom RAND = new SecureRandom();
	private static final String ALGO = "PBKDF2WithHmacSHA256";
	private static final int SALT_LEN = 16;
	private static final int ITERATIONS = 100_000; // produkcyjnie min 100k+
	private static final int KEY_LEN = 256;

	private SecurityUtil() {
	}

	public static byte[] generateSalt() {
		byte[] s = new byte[SALT_LEN];
		RAND.nextBytes(s);
		return s;
	}

	public static byte[] hashPassword(char[] password, byte[] salt) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LEN);
			SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGO);
			byte[] hash = skf.generateSecret(spec).getEncoded();
			spec.clearPassword();
			return hash;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean verifyPassword(char[] password, byte[] salt, byte[] expectedHash) {
		byte[] hash = hashPassword(password, salt);
		if (hash.length != expectedHash.length)
			return false;
		int diff = 0;
		for (int i = 0; i < hash.length; i++)
			diff |= (hash[i] ^ expectedHash[i]);
		return diff == 0;
	}

	// convenience for storing as base64 if needed
	public static String toBase64(byte[] b) {
		return Base64.getEncoder().encodeToString(b);
	}

	public static byte[] fromBase64(String s) {
		return Base64.getDecoder().decode(s);
	}
}
