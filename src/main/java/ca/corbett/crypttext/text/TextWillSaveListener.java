package ca.corbett.crypttext.text;

import java.io.File;

@FunctionalInterface
public interface TextWillSaveListener {

    /**
     * The given Text instance is about to be saved to the given destination File
     * (which may or may not match the given Text's source file).
     * Return false to veto the save, or true to allow it to proceed.
     *
     * @param manager The TextManager that is saving the text.
     * @param text    The Text instance that is about to be saved.
     * @return true to allow the save to proceed, or false to veto the save.
     */
    boolean textWillSave(TextManager manager, Text text, File destFile);

}
