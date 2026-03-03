package ca.corbett.crypttext.text;

/**
 * Used with TextManager to listen for text save events.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
@FunctionalInterface
public interface TextSavedListener {

    /**
     * The given Text instance was just saved by the given TextManager.
     * This is informational only - this event can't be vetoed.
     * To prevent text save events, use TextWillSaveListener instead.
     *
     * @param manager The TextManager that is saving the text.
     * @param text    The Text instance that is about to be saved.
     */
    void textSaved(TextManager manager, Text text);
}
