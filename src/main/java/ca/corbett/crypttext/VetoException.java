package ca.corbett.crypttext;

/**
 * An exception that can be thrown by extensions to veto an operation.
 * For example, if an extension wants to prevent a file from being loaded,
 * it can throw a VetoException from TextManager.fromFile(). The application
 * will catch the exception and simply not load the file.
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
