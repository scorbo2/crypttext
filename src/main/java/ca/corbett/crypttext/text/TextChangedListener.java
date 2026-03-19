package ca.corbett.crypttext.text;

/**
 * Used with TextManager to listen for text change events. These are generated from
 * the saveTextAs() method, when one Text instance is written to the same file
 * as another Text instance.
 * Owners of the stale Text instance should either load the given new value,
 * or prompt the user for what to do with unsaved changes in the stale instance.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
@FunctionalInterface
public interface TextChangedListener {

    /**
     * The given Text instance is out of date - the contents of the file on disk
     * have just been changed, and the Text instance's value is now stale. The new value of the text
     * is provided as a parameter. This is an informational event and cannot be vetoed.
     * You can prevent this event from being fired by using a TextWillSaveListener instead.
     *
     * @param manager   The TextManager that is managing the text.
     * @param staleText The stale Text instance that is now out of date.
     * @param newValue  The new Text instance which has replaced the stale Text instance.
     */
    void textChanged(TextManager manager, Text staleText, Text newValue);
}
