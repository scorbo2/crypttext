package ca.corbett.crypttext.text;

/**
 * Used with TextManager to listen for text load events.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
@FunctionalInterface
public interface TextLoadedListener {

    /**
     * The given Text instance was just loaded by the given TextManager.
     * This is informational only - this event can't be vetoed.
     * To prevent text load events, use TextWillLoadListener instead.
     *
     * @param manager The TextManager that is loading the text.
     * @param text    the Text instance that was just loaded.
     */
    void textLoaded(TextManager manager, Text text);
}
