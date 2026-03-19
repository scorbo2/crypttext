package ca.corbett.crypttext.text;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface HandleLoadListener {

    /**
     * The given TextManager is about to load the given file. This method allows the listener to handle the
     * load operation itself, and return a Text instance to be used in place of the default loading behavior.
     * If null is returned, the TextManager will proceed with its normal loading process.
     *
     * @param manager the TextManager that is about to perform the load operation.
     * @param toLoad the file that is about to be loaded.
     * @return non-null to indicate that the load has been handled, or null to pass on the operation.
     * @throws IOException can be thrown if the load fails. Application will handle the exception.
     */
    Text handleFileLoad(TextManager manager, File toLoad) throws IOException;

}
