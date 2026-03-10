package ca.corbett.crypttext.ui;

import javax.swing.Timer;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * A custom {@link UndoableEditListener} that groups rapid successive edits into a single
 * logical undo entry. Each time a new edit arrives, a short timer is (re)started. When
 * the timer fires (i.e. the user has paused typing), the current group is committed to
 * the {@link UndoManager} as one {@link CompoundEdit}. This means a single Ctrl+Z will
 * undo the entire burst of keystrokes rather than reversing them one character at a time.
 *
 * <p>Style-change events ({@code changedUpdate}) are deliberately ignored because Swing
 * fires them for font/color changes that should not appear on the undo stack.</p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>, with claude.ai
 */
public class GroupingUndoableEditListener implements UndoableEditListener {

    /** Milliseconds of idle time before the current group is committed. */
    private static final int COMMIT_DELAY_MS = 500;

    private final UndoManager undoManager;

    /**
     * The compound edit currently being accumulated.
     * Null means no group is in progress.
     */
    private CompoundEdit currentGroup;

    /**
     * Timer that commits {@link #currentGroup} after {@link #COMMIT_DELAY_MS} ms of
     * inactivity. The timer is restarted on every incoming edit.
     */
    private final Timer commitTimer;

    /**
     * Flag controlled by the owning {@link EditorTab} to suppress event processing
     * during programmatic text changes (e.g. font reloads, encryption results).
     */
    private boolean enabled = true;

    public GroupingUndoableEditListener(UndoManager undoManager) {
        if (undoManager == null) {
            throw new IllegalArgumentException("undoManager cannot be null");
        }
        this.undoManager = undoManager;

        // Single-shot timer: fires once after COMMIT_DELAY_MS and commits the current group.
        commitTimer = new Timer(COMMIT_DELAY_MS, e -> commitCurrentGroup());
        commitTimer.setRepeats(false);
    }

    /**
     * Returns whether this listener is currently processing events.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables event processing. While disabled, incoming edits are ignored,
     * and any in-progress group is committed and discarded so that programmatic changes
     * do not pollute the undo stack.
     */
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            // Commit whatever we have now so programmatic changes start with a clean slate.
            commitCurrentGroup();
        }
        this.enabled = enabled;
    }

    /**
     * Forces any in-progress group to be committed immediately.
     * Useful when the document is saved or the tab is reset.
     */
    public void flush() {
        commitCurrentGroup();
    }

    // -------------------------------------------------------------------------
    // UndoableEditListener
    // -------------------------------------------------------------------------

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        if (!enabled) {
            return;
        }

        UndoableEdit edit = e.getEdit();

        // Ignore style-change events – they are not meaningful for undo purposes
        // and would otherwise clutter the undo stack during reloadUI() calls.
        if (!edit.isSignificant()) {
            return;
        }

        // Open a new group if we don't have one yet.
        if (currentGroup == null) {
            currentGroup = new CompoundEdit();
        }

        currentGroup.addEdit(edit);

        // (Re)start the idle timer so that the group is committed only after
        // the user has paused for at least COMMIT_DELAY_MS milliseconds.
        commitTimer.restart();
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    /**
     * Closes the current {@link CompoundEdit} and hands it to the {@link UndoManager}.
     * If there is no current group, this is a no-op.
     */
    private void commitCurrentGroup() {
        commitTimer.stop();

        if (currentGroup == null) {
            return;
        }

        CompoundEdit toCommit = currentGroup;
        currentGroup = null;

        toCommit.end(); // marks the compound edit as finished

        // Only add it to the manager if it actually contains something significant.
        if (toCommit.isSignificant()) {
            undoManager.addEdit(toCommit);
        }
    }
}

