package ca.corbett.crypttext;

/**
 * A custom exception type that we will use to indicate that a decryption operation has failed.
 * The default handling of this is very user-unfriendly, so we will wrap the underlying
 * AEADBadTagException (with its unhelpful "tag mismatch" message) into something more user-friendly.
 * <p>
 * Side note: cryptographically, we actually can't tell the difference between
 * a wrong password and a corrupted ciphertext, since both of those scenarios will
 * cause the decryption to fail with the same error. But, if we phrase our error
 * message carefully, we can hopefully avoid confusing the user.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class DecryptionFailedException extends Exception {

    public static final String MESSAGE = "Decryption failed: incorrect password or corrupted encrypted data.";

    public DecryptionFailedException() {
        super(MESSAGE);
    }

    public DecryptionFailedException(String message) {
        super(message);
    }

    public DecryptionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecryptionFailedException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
