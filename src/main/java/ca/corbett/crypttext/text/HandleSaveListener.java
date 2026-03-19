package ca.corbett.crypttext.text;

import java.io.File;
import java.io.IOException;

/**
 * Used with TextManager to listen for, and possibly handle, text save events.
 * This event is fired when TextManager wants to save the given Text instance.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
@FunctionalInterface
public interface HandleSaveListener {

    /**
     * The given TextManager wants to save the given Text instance with the given resolved text.
     * Listeners can choose to handle the save themselves, and return a File instance to indicate
     * that the save has been handled.
     *
     * @param manager The TextManager that is saving the text.
     * @param toSave the Text instance that is being saved.
     * @param resolvedText the text that is being saved. May not match toSave's memory contents.
     * @param destinationFile May not match toSave.getSourceFile() if this is a "save as" operation.
     * @return Non-null to indicate that the save has been handled. Null to pass on the save operation.
     */
    File handleFileSave(TextManager manager, Text toSave, String resolvedText, File destinationFile) throws IOException;

}
