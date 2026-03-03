package ca.corbett.crypttext.text;

import ca.corbett.crypttext.Version;
import ca.corbett.extras.io.FileSystemUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A Controller class for creating and managing Text objects.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TextManager {

    private final File scratchDir;
    private final List<Text> textList = new CopyOnWriteArrayList<>();

    private final List<TextWillLoadListener> textWillLoadListeners = new CopyOnWriteArrayList<>();
    private final List<TextWillSaveListener> textWillSaveListeners = new CopyOnWriteArrayList<>();
    private final List<TextLoadedListener> textLoadedListeners = new CopyOnWriteArrayList<>();
    private final List<TextSavedListener> textSavedListeners = new CopyOnWriteArrayList<>();
    private final List<TextChangedListener> textChangedListeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a new TextManager instance using the system temp directory as our scratch dir.
     */
    public TextManager() {
        this(new File(System.getProperty("java.io.tmpdir")));
    }

    /**
     * Creates a new TextManager instance using the given scratch directory.
     * The given directory must exist and be writable.
     */
    public TextManager(File scratchDir) {
        if (scratchDir == null || !scratchDir.exists() || !scratchDir.isDirectory() || !scratchDir.canWrite()) {
            throw new IllegalArgumentException("scratch dir must be a valid, writable directory.");
        }
        this.scratchDir = scratchDir;
    }

    public boolean isEmpty() {
        return textList.isEmpty();
    }

    /**
     * Returns the number of Text instances in our cache.
     */
    public int size() {
        return textList.size();
    }

    /**
     * Removes the given Text instance from our cache, if present.
     */
    public void remove(Text text) throws IOException {
        if (text == null) {
            throw new IllegalArgumentException("Given Text instance cannot be null");
        }
        textList.remove(text);
        if (isScratchFile(text.getSourceFile())) {
            if (!text.getSourceFile().delete()) {
                throw new IOException("Unable to delete scratch file: " + text.getSourceFile().getAbsolutePath());
            }
        }
    }

    /**
     * Empties the cache and performs cleanup of any file in the scratch directory.
     * Does not fire events! Cannot be vetoed!
     */
    public void clear() throws IOException {
        List<Text> copy = new ArrayList<>(textList);
        for (Text text : copy) {
            remove(text);
        }
    }

    /**
     * Synonym for clear()
     */
    public void dispose() throws IOException {
        clear();
    }

    /**
     * Create a new, blank Text object and add it to the list of Texts managed by this class.
     * No events are fired from this method - it is not vetoable.
     * <p>
     * The new Text instance will be associated with a file in the scratch directory.
     * Invoking saveText() on the returned instance will save to that file.
     * A more common use case would be newText() followed by saveTextAs() to specify the save location.
     * </p>
     */
    public Text newText() throws IOException {
        File sourceFile = File.createTempFile(Version.NAME, ".txt", scratchDir);
        Text newText = new Text("", sourceFile);
        textList.add(newText);
        return newText;
    }

    /**
     * If we have loaded a Text instance from the given File, return that instance.
     * Otherwise, return null. No events are fired from this method - it is not vetoable.
     */
    public Text fromCache(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        for (Text text : textList) {
            if (Files.isSameFile(text.getSourceFile().toPath(), file.toPath())) {
                return text;
            }
        }
        return null;
    }

    /**
     * Loads a Text instance from the given File, or returns one from cache if already loaded.
     * A textWillLoadEvent is fired before the load, in the case where the given File is not in cache.
     * This method will return null if any of our listeners veto the load event.
     * A textLoadedEvent is fired after the load.
     * <p>
     * No check is done here to make sure the given file is a valid text file!
     * Such validation can be done ahead of time with the TextFileDetector class.
     * </p>
     */
    public synchronized Text fromFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (!file.exists() || !file.canRead() || file.isDirectory()) {
            throw new IllegalArgumentException("invalid or unreadable file");
        }

        Text newText = fromCache(file);
        if (newText != null) {
            return newText;
        }

        // Give listeners a chance to veto:
        if (!fireTextWillLoadEvent(file)) {
            return null;
        }

        // Load it:
        String text = FileSystemUtil.readFileToString(file);
        newText = new Text(text, file);
        textList.add(newText);

        // Notify listeners:
        fireTextLoadedEvent(newText);

        return newText;
    }

    /**
     * Saves the given Text instance to its associated file, and returns a new Text instance with the updated text.
     * A textWillSaveEvent is triggered before the save happens - this is vetoable.
     * This method may return the input Text instance, in the case where any of our listeners veto the save.
     * A textSavedEvent is fired after the save completes.
     * If a new Text instance is returned, then the old one has been removed from cache and should be discarded.
     * <p>
     * It's valid to pass null for newValue - this is equivalent to empty string.
     * </p>
     */
    public synchronized Text saveText(Text text, String newValue) throws IOException {
        if (text == null) {
            throw new IllegalArgumentException("The given Text instance cannot be null");
        }
        if (!textList.contains(text)) {
            throw new IllegalArgumentException("The given Text instance is not cached in this TextManager");
        }
        if (newValue == null) {
            newValue = "";
        }
        File sourceFile = text.getSourceFile();

        // Give listeners a chance to veto the save:
        if (!fireTextWillSaveEvent(text, sourceFile)) {
            return text;
        }

        // Save it:
        FileSystemUtil.writeStringToFile(newValue, sourceFile);
        Text newText = new Text(newValue, sourceFile);
        textList.remove(text);
        textList.add(newText);

        // Notify listeners:
        fireTextSavedEvent(newText);

        return newText;
    }

    /**
     * Saves the given Text instance to the given destination file.
     * If the given destination file is already associated with some other Text instance in cache,
     * then a textSaved event will be generated for that other instance, to let owners know
     * that the value has changed on disk.
     * <p>
     * The returned Text instance may be the same one that was passed in, if any of our
     * listeners veto the save. In that case, the Text was not saved to the destination file.
     * If a new instance is returned, the given Text instance is removed from our cache
     * and should be discarded.
     * </p>
     * <p>
     * It's valid to pass null for newValue - this is equivalent to empty string.
     * </p>
     * <p>
     * <b>Note:</b> if the given newFile is already present in our cache from some other
     * Text instance, and none of our listeners veto the save operation, then a
     * textChangedEvent will be fired, so that owners of the old, stale Text instance(s)
     * can either refresh themselves with the new value, or prompt the user for what to
     * do with their stale contents. You can avoid this by listening for textWillSaveEvents
     * and watching for saves to your file.
     * </p>
     */
    public synchronized Text saveTextAs(Text text, String newValue, File newFile) throws IOException {
        if (text == null) {
            throw new IllegalArgumentException("Given Text instance cannot be null");
        }
        if (newFile == null) {
            throw new IllegalArgumentException("new file cannot be null");
        }
        if (!newFile.exists() || !newFile.canWrite() || newFile.isDirectory()) {
            throw new IllegalArgumentException("invalid or unwritable file");
        }
        if (!textList.contains(text)) {
            throw new IllegalArgumentException("The given Text instance is not cached in this TextManager");
        }

        // Wonky case: if we're given the same file that it's already associated with, just delegate to saveText():
        if (Files.isSameFile(text.getSourceFile().toPath(), newFile.toPath())) {
            return saveText(text, newValue);
        }

        // Give listeners a chance to veto the save:
        if (!fireTextWillSaveEvent(text, newFile)) {
            return text;
        }

        // Save it:
        FileSystemUtil.writeStringToFile(newValue, newFile);
        Text newText = new Text(newValue, newFile);
        try {
            // remove from cache AND delete its sourceFile if in scratch directory:
            remove(text);

            // If any other of our cached Text instances reference that file,
            // let them know of this change. They can decide what to do with it.
            for (Text staleText : textList) {
                if (Files.isSameFile(staleText.getSourceFile().toPath(), newFile.toPath())) {
                    fireTextChangedEvent(staleText, newText);
                }
            }
        }
        finally {
            // Add the new Text instance to our cache:
            textList.add(newText);

            // Notify listeners:
            fireTextSavedEvent(newText);
        }

        return newText;
    }

    /**
     * Reports if the given file exists within our scratch dir.
     */
    public boolean isScratchFile(File candidate) throws IOException {
        if (candidate == null) {
            return false;
        }
        String tempPath = scratchDir.getAbsolutePath();
        return candidate.getAbsolutePath().startsWith(tempPath);
    }

    public TextManager addTextWillLoadListener(TextWillLoadListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("TextWillLoadListener cannot be null.");
        }
        textWillLoadListeners.add(listener);
        return this;
    }

    public TextManager removeTextWillLoadListener(TextWillLoadListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        textWillLoadListeners.remove(listener);
        return this;
    }

    public TextManager addTextWillSaveListener(TextWillSaveListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("TextWillSaveListener cannot be null.");
        }
        textWillSaveListeners.add(listener);
        return this;
    }

    public TextManager removeTextWillSaveListener(TextWillSaveListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        textWillSaveListeners.remove(listener);
        return this;
    }

    public TextManager addTextLoadedListener(TextLoadedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("TextLoadedListener cannot be null.");
        }
        textLoadedListeners.add(listener);
        return this;
    }

    public TextManager removeTextLoadedListener(TextLoadedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        textLoadedListeners.remove(listener);
        return this;
    }

    public TextManager addTextSavedListener(TextSavedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("TextSavedListener cannot be null.");
        }
        textSavedListeners.add(listener);
        return this;
    }

    public TextManager removeTextSavedListener(TextSavedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        textSavedListeners.remove(listener);
        return this;
    }

    public TextManager addTextChangedListener(TextChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        textChangedListeners.add(listener);
        return this;
    }

    private boolean fireTextWillLoadEvent(File file) {
        for (TextWillLoadListener listener : textWillLoadListeners) {
            if (!listener.textWillLoad(this, file)) {
                return false; // The first one that vetoes, we're done
            }
        }
        return true;
    }

    private boolean fireTextWillSaveEvent(Text text, File destFile) {
        for (TextWillSaveListener listener : textWillSaveListeners) {
            if (!listener.textWillSave(this, text, destFile)) {
                return false; // The first one that vetoes, we're done
            }
        }
        return true;
    }

    private void fireTextLoadedEvent(Text text) {
        for (TextLoadedListener listener : textLoadedListeners) {
            listener.textLoaded(this, text);
        }
    }

    private void fireTextSavedEvent(Text text) {
        for (TextSavedListener listener : textSavedListeners) {
            listener.textSaved(this, text);
        }
    }

    private void fireTextChangedEvent(Text staleText, Text newText) {
        for (TextChangedListener listener : textChangedListeners) {
            listener.textChanged(this, staleText, newText);
        }
    }
}
