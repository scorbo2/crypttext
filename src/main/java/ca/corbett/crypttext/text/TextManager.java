package ca.corbett.crypttext.text;

import ca.corbett.crypttext.Version;
import ca.corbett.extras.io.FileSystemUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Controller class for creating and managing Text objects.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TextManager {

    private static final Logger log = Logger.getLogger(TextManager.class.getName());

    private final File scratchDir;
    private final List<Text> textList = new CopyOnWriteArrayList<>();

    private final List<HandleLoadListener> handleLoadListeners = new CopyOnWriteArrayList<>();
    private final List<HandleSaveListener> handleSaveListeners = new CopyOnWriteArrayList<>();
    private final List<TextLoadedListener> textLoadedListeners = new CopyOnWriteArrayList<>();
    private final List<TextSavedListener> textSavedListeners = new CopyOnWriteArrayList<>();
    private final List<TextChangedListener> textChangedListeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a new TextManager instance using the system temp directory to house our scratch dir.
     */
    public TextManager() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        try {
            tempDir = Files.createTempDirectory(tempDir.toPath(), Version.NAME).toFile();
        }
        catch (IOException ioe) {
            log.log(Level.SEVERE, "Unable to create scratch directory; using system temp dir instead.", ioe);
        }

        scratchDir = tempDir;
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
     * Extensions will be given the chance to handle the load details for us.
     * Otherwise, the application's built-in mechanism for loading is used.
     * A textLoadedEvent is fired after a successful.
     * <p>
     * No check is done here to make sure the given file is a valid text file!
     * Such validation can be done ahead of time with the TextFileDetector class.
     * </p>
     *
     * @param file The file to load from. Must be a valid, readable file (not a directory).
     * @return A Text instance representing the given file.
     * @throws IOException If an error occurs while reading the file.
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

        // Give listeners a chance to handle the load for us:
        newText = fireHandleLoad(file);

        // If no extension handled it, then we'll do it ourselves:
        if (newText == null) {
            newText = new Text(FileSystemUtil.readFileToString(file), file);
        }

        // Add to cache:
        textList.add(newText);

        // Notify listeners:
        log.info("Load: file " + newText.getSourceFile().getAbsolutePath() + " was loaded successfully.");
        fireTextLoadedEvent(newText);

        return newText;
    }

    /**
     * Saves the given Text instance to its associated file, and returns a new Text instance with the updated text.
     * Extensions will be given the chance to handle the save details for us.
     * Otherwise, the application's built-in mechanism for saving is used.
     * A textSavedEvent is fired after a save successfully completes.
     * <p>
     * The new Text instance that is returned replaces the one that was passed in, which is now stale.
     * The stale instance has been removed from cache and should be discarded.
     * </p>
     * <p>
     * It's valid to pass null for newValue - this is equivalent to empty string.
     * </p>
     *
     * @param text The Text instance to save. Must be non-null and must be in our cache.
     * @param newValue The new text value to save. If null, this is treated as empty string.
     * @return A new Text instance representing the saved text. This replaces the passed-in (now stale) instance.
     * @throws IOException If an error occurs while writing to the file.
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

        // Give listeners a chance to handle the save for us:
        File sourceFile = text.getSourceFile();
        File destinationFile = fireHandleSave(text, newValue, sourceFile);

        // If nobody volunteered, then we'll do it ourselves:
        if (destinationFile == null) {
            FileSystemUtil.writeStringToFile(newValue, sourceFile);
            destinationFile = sourceFile; // our default behavior is to save back to the same file
        }

        // Our new Text instance references the destination file, which may have changed above:
        Text newText = new Text(newValue, destinationFile);

        // Update our cache:
        textList.remove(text);
        textList.add(newText);

        // Notify listeners:
        log.info("Save: file " + destinationFile.getAbsolutePath() + " was saved successfully.");
        fireTextSavedEvent(newText.getSourceFile(), newText);

        return newText;
    }

    /**
     * Saves the given Text instance to the given destination file.
     * If the given destination file is already associated with some other Text instance in cache,
     * then a textSaved event will be generated for that other instance, to let owners know
     * that the value has changed on disk.
     * <p>
     * The returned Text instance replaces the one that was passed in, which is now stale.
     * The stale instance has been removed from cache and should be discarded.
     * </p>
     * <p>
     * It's valid to pass null for newValue - this is equivalent to empty string.
     * </p>
     * <p>
     * <b>Note:</b> if the given newFile is already present in our cache from some other
     * Text instance, then a textChangedEvent will be fired, so that owners of the old, stale
     * Text instance(s) can either refresh themselves with the new value, or prompt the user for what to
     * do with their stale contents. You can avoid this by listening for textWillSaveEvents
     * and watching for saves to your file.
     * </p>
     * <p>
     *     <b>Warning:</b> if newFile already exists, we will overwrite it.
     *     Callers should check for this and prompt the user for confirmation.
     * </p>
     */
    public synchronized Text saveTextAs(Text text, String newValue, File newFile) throws IOException {
        if (text == null) {
            throw new IllegalArgumentException("Given Text instance cannot be null");
        }
        if (newFile == null) {
            throw new IllegalArgumentException("new file cannot be null");
        }
        if (newFile.isDirectory()) {
            throw new IllegalArgumentException("invalid or unwritable file");
        }
        if (!textList.contains(text)) {
            throw new IllegalArgumentException("The given Text instance is not cached in this TextManager");
        }
        if (newValue == null) {
            newValue = ""; // null means empty string, as per method Javadocs
        }

        // Wonky case: if we're given the same file that it's already associated with, just delegate to saveText():
        if (newFile.exists()) { // Files.isSameFile() will puke if either file doesn't exist; skip this test if so
            if (Files.isSameFile(text.getSourceFile().toPath(), newFile.toPath())) {
                return saveText(text, newValue);
            }
        }

        // Give listeners a chance to handle the save for us:
        Text saveTextAs = new Text(newValue, newFile);
        File destinationFile = fireHandleSave(saveTextAs, newValue, newFile);

        // If no one volunteered, then we'll do it ourselves:
        if (destinationFile == null) {
            // Save it:
            FileSystemUtil.writeStringToFile(newValue, newFile);
            destinationFile = newFile; // our default behavior is to save to the given file
        }

        // The resulting Text instance references the actual destination file, which may not match newFile:
        Text newText = new Text(newValue, destinationFile);
        try {
            // remove from cache AND delete its sourceFile if in scratch directory:
            remove(text);

            // If any other of our cached Text instances reference that file,
            // let them know of this change. They can decide what to do with it.
            for (Text staleText : textList) {
                if (Files.isSameFile(staleText.getSourceFile().toPath(), destinationFile.toPath())) {
                    fireTextChangedEvent(staleText, newText);
                }
            }
        }
        finally {
            // Add the new Text instance to our cache:
            textList.add(newText);

            // Notify listeners:
            log.info("Save: file " + destinationFile.getAbsolutePath() + " was saved successfully.");
            fireTextSavedEvent(destinationFile, newText);
        }

        return newText;
    }

    /**
     * Reports if the given file exists within our scratch dir.
     * Our assumption is that scratch files are always direct children of our scratch dir.
     * (This is, we don't create subdirectories or sub-sub-directories, etc.)
     */
    public boolean isScratchFile(File candidate) throws IOException {
        if (candidate == null || candidate.getParentFile() == null) {
            return false;
        }
        return Files.isSameFile(scratchDir.toPath(), candidate.getParentFile().toPath());
    }

    public TextManager addLoadHandlerListener(HandleLoadListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("HandleLoadListener cannot be null.");
        }
        handleLoadListeners.add(listener);
        return this;
    }

    public TextManager removeLoadHandlerListener(HandleLoadListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        handleLoadListeners.remove(listener);
        return this;
    }

    public TextManager addSaveHandlerListener(HandleSaveListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("HandleSaveListener cannot be null.");
        }
        handleSaveListeners.add(listener);
        return this;
    }

    public TextManager removeSaveHandlerListener(HandleSaveListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        handleSaveListeners.remove(listener);
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

    private Text fireHandleLoad(File file) throws IOException {
        for (HandleLoadListener listener : handleLoadListeners) {
            Text text = listener.handleFileLoad(this, file);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private File fireHandleSave(Text text, String newContents, File destFile) throws IOException {
        for (HandleSaveListener listener : handleSaveListeners) {
            File file = listener.handleFileSave(this, text, newContents, destFile);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private void fireTextLoadedEvent(Text text) {
        for (TextLoadedListener listener : textLoadedListeners) {
            listener.textLoaded(this, text);
        }
    }

    private void fireTextSavedEvent(File source, Text text) {
        for (TextSavedListener listener : textSavedListeners) {
            listener.textSaved(this, source, text);
        }
    }

    private void fireTextChangedEvent(Text staleText, Text newText) {
        for (TextChangedListener listener : textChangedListeners) {
            listener.textChanged(this, staleText, newText);
        }
    }
}
