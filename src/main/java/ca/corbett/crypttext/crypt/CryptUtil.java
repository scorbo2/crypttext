package ca.corbett.crypttext.crypt;

import ca.corbett.crypttext.DecryptionFailedException;
import ca.corbett.extras.ResourceLoader;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * A utility class for encrypting and decrypting data with a password.
 * A symmetric encryption scheme is used, where the same password is used for both encryption and decryption.
 * The password is used to derive a key using the Argon2 key derivation function,
 * and the data is encrypted using AES in GCM mode for authenticated encryption.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a> (with claude.ai)
 */
public class CryptUtil {
    static final String CRYPT_HEADER = "==CryptText section begins==";
    private static final String TEXT_WRAPPER;

    static {
        // Load our text wrapper once:
        TEXT_WRAPPER = ResourceLoader.getTextResource("ca/corbett/crypttext/file_header.txt");
    }

    private static final int SALT_LENGTH = 16; // generally sufficient for salt
    private static final int IV_LENGTH = 12; // recommended length for GCM IVs
    private static final int TAG_LENGTH = 128; // 128-bit authentication tag

    /**
     * Reports whether the given text contains the expected header string that
     * indicates it is a wrapped crypt text payload.
     * <p>
     * <b>Note!</b> This method does not tell you if the given encrypted payload is
     * valid and can be decrypted successfully. We merely examine the file and
     * check for indicators that it <i>might</i> be a wrapped crypt text payload.
     * False positives are possible! Don't assume decryption will succeed.
     * </p>
     *
     * @param text the text to check for the presence of our payload header
     * @return true if the text is not null and contains the expected header string, false otherwise
     */
    public static boolean isCryptTextWrapped(String text) {
        // Developer note: we can't check for the presence of TEXT_WRAPPER, because the
        // user can freely edit the text above our header line. (Why would they, I dunno,
        // but they could... the payload doesn't start until after our header line).
        // So, we just check for the presence of our header line.
        // This is not great, but it's about as good as we can do without the password.
        return text != null && text.contains(CRYPT_HEADER);
    }

    /**
     * Encrypts the given plaintext with the provided password, performs a Mime Base64 encoding of the result,
     * and then wraps the encoded string into a user-viewable wrapper message.
     *
     * @param password  the password to use for encryption (must not be null or empty)
     * @param plaintext the data to encrypt (must not be null)
     * @return a String containing the wrapper message and Base64-encoded encrypted data, suitable for saving.
     * @throws Exception if encryption fails or if input parameters are invalid
     */
    public static String encryptAndWrap(String password, String plaintext) throws Exception {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }
        return TEXT_WRAPPER + "\n" + encryptAndEncode(password, plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Accepts an encrypted payload that was wrapped via encryptAndWrap(), and returns the decrypted plaintext.
     *
     * @param password the password to use for decryption (must not be null or empty)
     * @param wrapped  a String containing the wrapper message and Base64-encoded payload, produced by encryptAndWrap()
     * @return the decrypted plaintext
     * @throws Exception if decryption fails, if authentication fails, or if input parameters are invalid
     */
    public static String unwrapAndDecrypt(String password, String wrapped) throws Exception {
        if (wrapped == null) {
            throw new IllegalArgumentException("Wrapped data cannot be null");
        }

        // Split the wrapped message into lines:
        String[] lines = wrapped.split("\\r?\\n");

        // Scan until we find our header:
        int line = 0;
        boolean foundHeader = false;
        for (; line < lines.length; line++) {
            if (lines[line].trim().equals(CRYPT_HEADER)) {
                foundHeader = true;
                break;
            }
        }

        // If we didn't find it, the payload is invalid:
        if (!foundHeader) {
            throw new IllegalArgumentException("Wrapped data does not contain expected header");
        }

        // Otherwise, everything after that is our base64 encoded and encrypted payload:
        StringBuilder sb = new StringBuilder();
        for (line++; line < lines.length; line++) {
            sb.append(lines[line]).append("\n");
        }
        return new String(decodeAndDecrypt(password, sb.toString().trim()), StandardCharsets.UTF_8);
    }

    /**
     * Encrypts the given plaintext with the provided password and encodes the result in Base64.
     * The resulting String is in MIME Base64 format, which is suitable for embedding in text files
     * and supports line breaks.
     *
     * @param password  the password to use for encryption (must not be null or empty)
     * @param plaintext the data to encrypt (must not be null)
     * @return a Base64-encoded String containing the salt, IV, and ciphertext (with auth tag)
     * @throws Exception if encryption fails or if input parameters are invalid
     */
    public static String encryptAndEncode(String password, byte[] plaintext) throws Exception {
        return Base64.getMimeEncoder().encodeToString(encrypt(password, plaintext));
    }

    /**
     * Encrypts the given plaintext with the provided password.
     *
     * @param password  the password to use for encryption (must not be null or empty)
     * @param plaintext the data to encrypt (must not be null)
     * @return a byte array containing the salt, IV, and ciphertext (with auth tag)
     * @throws Exception if encryption fails or if input parameters are invalid
     */
    public static byte[] encrypt(String password, byte[] plaintext) throws Exception {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }

        // Generate random salt and IV
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(salt);
        random.nextBytes(iv);

        // Derive key from password using Argon2
        byte[] key = deriveKey(password, salt);

        // Encrypt with AES-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Concatenate: salt + iv + ciphertext (includes auth tag)
        byte[] result = new byte[salt.length + iv.length + ciphertext.length];
        System.arraycopy(salt, 0, result, 0, salt.length);
        System.arraycopy(iv, 0, result, salt.length, iv.length);
        System.arraycopy(ciphertext, 0, result, salt.length + iv.length, ciphertext.length);

        return result;
    }

    /**
     * Takes the given input as Mime Base64-encoded data containing the salt, IV,
     * and ciphertext (with auth tag), decodes it, and decrypts it with the provided password.
     *
     * @param password the password to use for decryption (must not be null or empty)
     * @param encoded  a Base64-encoded String containing the salt, IV, and ciphertext (with auth tag) to decrypt
     * @return a byte array containing the decrypted plaintext
     * @throws Exception if decryption fails, if authentication fails, or if input parameters are invalid
     */
    public static byte[] decodeAndDecrypt(String password, String encoded) throws Exception {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Encoded data cannot be null or empty");
        }
        byte[] encrypted = Base64.getMimeDecoder().decode(encoded);
        return decrypt(password, encrypted);
    }

    /**
     * Decrypts the given encrypted data with the provided password.
     *
     * @param password  the password to use for decryption (must not be null or empty)
     * @param encrypted a byte array containing the salt, IV, and ciphertext (with auth tag) to decrypt
     * @return a byte array containing the decrypted plaintext
     * @throws Exception if decryption fails, if authentication fails, or if input parameters are invalid
     */
    public static byte[] decrypt(String password, byte[] encrypted) throws Exception {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (encrypted == null || encrypted.length < SALT_LENGTH + IV_LENGTH + (TAG_LENGTH / 8)) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }

        // Extract salt, IV, and ciphertext
        byte[] salt = Arrays.copyOfRange(encrypted, 0, SALT_LENGTH);
        byte[] iv = Arrays.copyOfRange(encrypted, SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(encrypted, SALT_LENGTH + IV_LENGTH, encrypted.length);

        // Derive key from password
        byte[] key = deriveKey(password, salt);

        // Decrypt with AES-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        try {
            return cipher.doFinal(ciphertext);
        }
        catch (AEADBadTagException ex) {
            // Authentication failed, which means either the password is wrong or the data is corrupted.
            // Cryptographically, (and by design), we can't distinguish between these two cases.
            // But, we don't want to throw a user-unfriendly "tag mismatch" error, so
            // let's wrap it in something more user-friendly:
            throw new DecryptionFailedException(ex);
        }
    }

    /**
     * Invoked internally to derive a key from the given password and salt using the Argon2 key derivation function.
     *
     * @param password the password to derive the key from
     * @param salt     the salt to use for key derivation
     * @return a byte array containing the derived key (256 bits)
     */
    private static byte[] deriveKey(String password, byte[] salt) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withIterations(2)
                .withMemoryAsKB(65536)
                .withParallelism(1)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] key = new byte[32]; // 256-bit key
        generator.generateBytes(password.toCharArray(), key);
        return key;
    }
}