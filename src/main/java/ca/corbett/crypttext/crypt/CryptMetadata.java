package ca.corbett.crypttext.crypt;

/**
 * An abstract base class for metadata related to encryption of a Text instance.
 * Extensions can subclass this to add additional fields as needed.
 * If no extension provides a CryptMetadata instance for a given Text instance,
 * then DefaultCryptMetadata will be used.
 * <p>
 * <b>NOTE!</b> If extensions subclass this to provide their own key handling
 * mechanism, then it is MOST STRONGLY RECOMMENDED that the key information
 * should not be written to disk in raw form.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public abstract class CryptMetadata {

    protected final boolean wasEncryptedWhenLoaded;

    /**
     * Creates a new CryptMetadata instance with the given value for whether the text was encrypted when it was loaded.
     * The password is null initially, and must be provided by the user for decryption/encryption to work.
     *
     * @param wasEncryptedWhenLoaded true if the text was encrypted when it was loaded.
     */
    public CryptMetadata(boolean wasEncryptedWhenLoaded) {
        this.wasEncryptedWhenLoaded = wasEncryptedWhenLoaded;
    }

    /**
     * Reports whether the text was encrypted when it was loaded.
     */
    public boolean isWasEncryptedWhenLoaded() {
        return wasEncryptedWhenLoaded;
    }
}
