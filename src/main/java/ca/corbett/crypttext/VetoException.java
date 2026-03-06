package ca.corbett.crypttext;

/**
 * An exception that indicates an extension has vetoed an operation.
 * For example, an extension can register {@code willLoad} or {@code willSave}
 * listeners and signal a veto (such as by returning {@code false}) to prevent
 * a file from being loaded or saved. In response, {@code TextManager} throws
 * a {@code VetoException}, which the application catches and uses to cancel
 * the operation (for example, by simply not loading the file).
 * <p>
 * This is a checked exception, so that calling code must
 * handle it explicitly.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class VetoException extends Exception {

    public VetoException() {
        super();
    }

    public VetoException(String message) {
        super(message);
    }

    public VetoException(String message, Throwable cause) {
        super(message, cause);
    }

    public VetoException(Throwable cause) {
        super(cause);
    }
}
