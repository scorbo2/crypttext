package ca.corbett.crypttext.text;

import java.io.File;

@FunctionalInterface
public interface TextWillLoadListener {

    /**
     * Text is about to be loaded from the given File.
     * Return false to veto the load, or true to allow it to proceed.
     *
     * @param manager The TextManager that is loading the text.
     * @param file    The file that is about to be loaded.
     * @return true to allow the load to proceed, or false to veto the load.
     */
    boolean textWillLoad(TextManager manager, File file);

}
