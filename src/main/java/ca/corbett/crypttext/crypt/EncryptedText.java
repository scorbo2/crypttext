package ca.corbett.crypttext.crypt;

/**
 * A simple data class to couple an encrypted text string with its associated metadata.
 * Instance of this class are immutable.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class EncryptedText {
    private final String text;
    private final CryptMetadata cryptMetadata;

    public EncryptedText(String text, CryptMetadata cryptMetadata) {
        this.text = text;
        this.cryptMetadata = cryptMetadata;
    }

    public String getText() {
        return text;
    }

    public CryptMetadata getCryptMetadata() {
        return cryptMetadata;
    }
}
