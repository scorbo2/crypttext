package ca.corbett.crypttext.crypt;

import ca.corbett.crypttext.DecryptionFailedException;
import ca.corbett.extras.ResourceLoader;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.io.TextFileDetector;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
     * Given a file, will attempt to encrypt it in place with the given password.
     * If the file is already encrypted, no action is taken.
     * If the file can't be read or written, or if it does not exist, an exception will be thrown.
     *
     * @param file     any file to encrypt (must not be null, must exist, must be a file, and must be readable and writable)
     * @param password the password to use for encryption (must not be null or empty)
     * @throws Exception if the file can't be read or written or does not exist, or if encryption fails.
     */
    public static void encryptInPlace(File file, String password) throws Exception {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead() || !file.canWrite()) {
            throw new IOException("The given file does not exist, is not a file, or cannot be read/written: "
                                          + (file != null ? file.getAbsolutePath() : "null"));
        }

        // If the file is already encrypted, just silently skip it:
        if (isCryptTextWrapped(file)) {
            return;
        }

        // Make sure it's a text file:
        if (!TextFileDetector.isTextFile(file)) {
            throw new IOException("The given file is not a text file: " + file.getAbsolutePath());
        }

        // Now we can encrypt it and save results back to the same file:
        String rawContents = FileSystemUtil.readFileToString(file);
        encryptAndWrap(password, rawContents, file);
    }

    /**
     * Given a file, will attempt to decrypt it in place with the given password.
     * If the file is not encrypted, no action is taken.
     * If the file can't be read or written, or if it does not exist, an exception will be thrown.
     *
     * @param file     any file to decrypt (must not be null, must exist, must be a file, and must be readable and writable)
     * @param password the password to use for decryption (must not be null or empty)
     * @throws Exception if the file can't be read or written or does not exist, if decryption fails, or if authentication fails.
     */
    public static void decryptInPlace(File file, String password) throws Exception {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead() || !file.canWrite()) {
            throw new IOException("The given file does not exist, is not a file, or cannot be read/written: "
                                          + (file != null ? file.getAbsolutePath() : "null"));
        }

        // If the file is not encrypted, just silently skip it:
        if (!isCryptTextWrapped(file)) {
            return;
        }

        // Make sure it's a text file:
        if (!TextFileDetector.isTextFile(file)) {
            throw new IOException("The given file is not a text file: " + file.getAbsolutePath());
        }

        // Now we can decrypt it and save results back to the same file:
        String decrypted = unwrapAndDecrypt(password, file);
        FileSystemUtil.writeStringToFile(decrypted, file);
    }

    /**
     * Reports whether the given file is a text file that looks like it was
     * created with CryptText (that is, it contains our expected header string).
     * This method gives up easily - if any exception occurs while accessing the
     * file, or if the file gives any indication of not being a text file,
     * we return false.
     * <p>
     * <b>Note!</b> This method does not tell you if the given encrypted payload is
     * valid and can be decrypted successfully. We merely examine the file and
     * check for indicators that it <i>might</i> be a wrapped crypt text payload.
     * False positives are possible! Don't assume decryption will succeed.
     * </p>
     *
     * @param file The file to check.
     * @return true ONLY if the file is a valid text file that is readable and contains our expected header.
     */
    public static boolean isCryptTextWrapped(File file) {
        // Handle the easy checks first:
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            return false; // nope
        }

        try {
            // Let's see if it looks like a text file:
            if (!TextFileDetector.isTextFile(file)) {
                return false; // nope
            }

            // We only need the first kilobyte or so to do our check, no need to load the whole file.
            // However, the user is free to modify the wrapper above our header line, so let's be generous:
            byte[] buffer = new byte[4096]; // 4KB should be more than enough to find our header if it's there
            try (FileInputStream fis = new FileInputStream(file)) {
                int bytesRead = fis.read(buffer);
                if (bytesRead != -1) {
                    // Convert only the actual bytes read into a String
                    String firstKB = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    return isCryptTextWrapped(firstKB);
                }
            }
        }
        catch (IOException ioe) {
            // If anything goes wrong here, we just assume it's not a valid wrapped file:
            return false;
        }

        return false; // if we couldn't read anything, it's not valid
    }

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
     * and then wraps the encoded string into a user-viewable wrapper message and writes it to outputFile.
     *
     * @param password   the password to use for encryption (must not be null or empty)
     * @param plaintext  the data to encrypt (must not be null)
     * @param outputFile the file to write the wrapped message to (must not be null)
     * @throws Exception if encryption fails, if writing to the file fails, or if input parameters are invalid
     */
    public static void encryptAndWrap(String password, String plaintext, File outputFile) throws Exception {
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }
        String wrapped = encryptAndWrap(password, plaintext);
        FileSystemUtil.writeStringToFile(wrapped, outputFile);
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
        return TEXT_WRAPPER + "\n" + encryptAndEncode(password, plaintext.getBytes(StandardCharsets.UTF_8)) + "\n";
    }

    /**
     * Given any file, will attempt to extract an encrypted payload from it and return it as plaintext.
     * If the file is not encrypted, this will simply return its contents as a string (if it's a text file).
     * If the given file is not a text file or can't be read, this will throw an exception.
     *
     * @param password the password to use for decryption (must not be null or empty)
     * @param file     any file containing an encrypted payload (must not be null).
     * @return the decrypted plaintext if the file was encrypted, or the file contents if it was not encrypted.
     * @throws Exception if the file can't be read, if it's not a text file, if decryption fails, or if authentication fails.
     */
    public static String unwrapAndDecrypt(String password, File file) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            throw new IOException("The given file does not exist, is not a file, or cannot be read: "
                                          + file.getAbsolutePath());
        }

        // Let's make sure it's a text file:
        if (!TextFileDetector.isTextFile(file)) {
            throw new IOException("The given file is not a text file: " + file.getAbsolutePath());
        }

        String rawContents = FileSystemUtil.readFileToString(file);
        if (isCryptTextWrapped(rawContents)) {
            // It's wrapped, so we need to unwrap and decrypt it:
            return unwrapAndDecrypt(password, rawContents);
        }
        else {
            // It's not wrapped, so we just return the raw contents:
            return rawContents;
        }
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