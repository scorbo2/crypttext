package ca.corbett.crypttext.crypt;

/**
 * A simple data class to couple an encrypted text string with its associated metadata.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class EncryptedText {
    private String text;
    private CryptMetadata cryptMetadata;

    public EncryptedText(String text, CryptMetadata cryptMetadata) {
        this.text = text;
        this.cryptMetadata = cryptMetadata;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public CryptMetadata getCryptMetadata() {
        return cryptMetadata;
    }

    public void setCryptMetadata(CryptMetadata metadata) {
        this.cryptMetadata = metadata;
    }
}
