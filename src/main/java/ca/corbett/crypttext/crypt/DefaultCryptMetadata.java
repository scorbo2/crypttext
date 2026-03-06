package ca.corbett.crypttext.crypt;

import java.util.Objects;

/**
 * The default implementation of CryptMetadata, which simply stores a password in memory.
 * This is used if no extension provides a CryptMetadata instance for a given Text instance.
 * <p>
 * <b>NOTE!</b> The raw password is NEVER written to disk! It exists
 * only in memory for as long as the associated Text instance lives.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class DefaultCryptMetadata extends CryptMetadata {

    private String password;

    public DefaultCryptMetadata(boolean wasEncryptedWhenLoaded) {
        super(wasEncryptedWhenLoaded);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DefaultCryptMetadata that)) { return false; }
        return Objects.equals(password, that.password) && wasEncryptedWhenLoaded == that.wasEncryptedWhenLoaded;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wasEncryptedWhenLoaded, password);
    }
}
