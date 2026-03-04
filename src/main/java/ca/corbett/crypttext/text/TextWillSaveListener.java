package ca.corbett.crypttext.text;

import java.io.File;

/**
 * Used with TextManager to listen for, and possibly veto, text save events.
 * This event is fired immediately before a Text instance is saved.
 * If any listener vetoes the save, then the save is cancelled and no TextSavedListener events are fired.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
@FunctionalInterface
public interface TextWillSaveListener {

    /**
     * The given Text instance is about to be saved to the given destination File
     * (which may or may not match the given Text's source file).
     * Return false to veto the save, or true to allow it to proceed.
     * <p>
     *     Note that the supplied Text instance is as it was when the Text was
     *     first loaded, and does not reflect the contents that are about to be
     *     saved. This is because Text instances are immutable, and also because
     *     this operation can be vetoed.
     * </p>
     *
     * @param manager The TextManager that is saving the text.
     * @param text    The Text instance that is about to be saved. (contents are out of date!)
     * @param newContents The new contents that will be written to the file if the save proceeds.
     * @param destFile The destination file that the text will be saved to if the save proceeds.
     * @return true to allow the save to proceed, or false to veto the save.
     */
    boolean textWillSave(TextManager manager, Text text, String newContents, File destFile);

}
