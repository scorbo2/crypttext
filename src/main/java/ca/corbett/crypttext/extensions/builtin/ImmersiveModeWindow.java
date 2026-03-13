package ca.corbett.crypttext.extensions.builtin;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.ui.BlockCursor;
import ca.corbett.crypttext.ui.EditorTab;
import ca.corbett.crypttext.ui.EditorTabPane;
import ca.corbett.crypttext.ui.MainWindow;

import javax.swing.JWindow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

/**
 * Represents a full-screen window that shows a single editor tab with no UI clutter,
 * for an immersive reading or writing experience. This is used internally by the
 * ImmersiveModeExtension.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
class ImmersiveModeWindow extends JWindow {

    private static final Logger log = Logger.getLogger(ImmersiveModeWindow.class.getName());

    private final EditorTab sourceTab;
    private final EditorTab clonedTab;
    private EditorTab.ContentChangeListener sourceTabListener;
    private EditorTab.ContentChangeListener clonedTabListener;
    private final EditorTabPane tabPane;

    /**
     * Creates a new ImmersiveModeWindow that clones the given sourceTab
     * and attempts to display it full-screen on the given monitor index
     * (0-based, where 0 is the primary monitor). If the given monitor
     * index is invalid, the primary is used as a fallback.
     */
    public ImmersiveModeWindow(MainWindow owner, EditorTab sourceTab, int monitorIndex) {
        super(owner, getGraphicsConfig(monitorIndex));
        tabPane = new EditorTabPane();
        tabPane.setTabHeaderVisible(false);
        this.sourceTab = sourceTab;
        this.clonedTab = new EditorTab(tabPane, sourceTab.getTabName(), sourceTab.getMemoryContents());
        tabPane.addTab(sourceTab.getTabName(), clonedTab);
        setLayout(new BorderLayout());
        add(tabPane, BorderLayout.CENTER);

        copyTextPaneConfiguration();
        configurePosition(monitorIndex);
        configureTextListeners();
    }

    /**
     * Sets up cosmetic properties in the cloned tab to match the source tab.
     */
    private void copyTextPaneConfiguration() {
        final Color fg = sourceTab.getTextPane().getForeground();
        clonedTab.getTextPane().setFont(sourceTab.getTextPane().getFont());
        clonedTab.getTextPane().setBackground(sourceTab.getTextPane().getBackground());
        clonedTab.getTextPane().setForeground(fg);

        // Check for custom Caret settings:
        final AppConfig appConfig = AppConfig.getInstance();
        if (appConfig.isUseBlockCursor()) {
            clonedTab.getTextPane().setCaret(new BlockCursor(appConfig.getCursorBlinkRate(), fg));
        }
        else {
            clonedTab.getTextPane().setCaretColor(sourceTab.getTextPane().getForeground());
            clonedTab.getTextPane().getCaret().setBlinkRate(appConfig.getCursorBlinkRate());
        }
    }

    /**
     * Invoked internally to make a best attempt to position ourselves full-screen
     * on the given target monitor. There is no guarantee that this will work,
     * because the given monitor index may no longer be valid. If we can't find
     * it, we'll fall back to the primary monitor, which <i>should</i> always work.
     */
    private void configurePosition(int monitorIndex) {
        // Figure out our target monitor:
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        if (monitorIndex >= gs.length) {
            monitorIndex = 0; // Default to primary monitor if index is out of bounds
        }

        // For debugging monitor selection:
        log.fine("I see that you have " + gs.length + " monitor(s) connected. " +
                         "Attempting to position immersive window on monitor index " + monitorIndex + ".");

        // Position ourselves for the whole screen:
        GraphicsDevice gd = gs[monitorIndex];
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Rectangle bounds = gc.getBounds();
        setLocation(bounds.x, bounds.y);
        setSize(bounds.width, bounds.height);

        // More monitor debugging:
        log.fine("I've set location to (" + bounds.x + ", " + bounds.y + ") " +
                         "and size to (" + bounds.width + "x" + bounds.height + ").");
    }

    @Override
    public void dispose() {
        // Tear down content listeners between source and cloned tabs to avoid leaks.
        if (sourceTab != null && sourceTabListener != null) {
            sourceTab.removeContentChangeListener(sourceTabListener);
            sourceTabListener = null;
        }
        if (clonedTab != null) {
            if (clonedTabListener != null) {
                clonedTab.removeContentChangeListener(clonedTabListener);
                clonedTabListener = null;
            }
            // Ensure the cloned tab releases its own resources.
            clonedTab.dispose();
        }

        // Break references to the temporary EditorTabPane so any associated listeners
        // can be garbage-collected once this window is disposed.
        if (tabPane != null) {
            tabPane.removeAll();
        }

        super.dispose();
    }

    /**
     * Make sure our two text panes are kept in sync with one another.
     */
    private void configureTextListeners() {
        // Here's what I want to do:
        // sourceTab.addContentChangeListener(clonedTab::setMemoryContents);
        // clonedTab.addContentChangeListener(sourceTab::setMemoryContents);
        // The problem is that this will cause infinite loops.
        // So, we have to be a little more clever than that.

        // When the source tab changes, turn off the cloned tab's listener,
        // update it, and then start listening again:
        sourceTabListener = new EditorTab.ContentChangeListener() {
            @Override
            public void onContentChange(String newContent) {
                clonedTab.removeContentChangeListener(clonedTabListener);
                clonedTab.setMemoryContents(newContent);
                clonedTab.addContentChangeListener(clonedTabListener);
            }
        };

        // Do the same in the other direction for the cloned tab:
        clonedTabListener = new EditorTab.ContentChangeListener() {
            @Override
            public void onContentChange(String newContent) {
                sourceTab.removeContentChangeListener(sourceTabListener);
                sourceTab.setMemoryContents(newContent);
                sourceTab.addContentChangeListener(sourceTabListener);
            }
        };

        // Start listening:
        sourceTab.addContentChangeListener(sourceTabListener);
        clonedTab.addContentChangeListener(clonedTabListener);

        // Also listen for the source tab closing, so we can close ourselves if that happens:
        final EditorTab.TabClosedListener tabClosedListener = e -> {
            // Close this immersive window; cleanup will run in the windowClosed handler.
            dispose();
        };
        sourceTab.addTabClosedListener(tabClosedListener);

        // Ensure that all listeners are removed when this window is disposed/closed,
        // regardless of how it is closed (F11/ESC, tab close, etc.).
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // Remove content-change listeners on both tabs:
                if (sourceTabListener != null) {
                    sourceTab.removeContentChangeListener(sourceTabListener);
                }
                if (clonedTabListener != null) {
                    clonedTab.removeContentChangeListener(clonedTabListener);
                }
                // Remove the tab-closed listener from the source tab:
                sourceTab.removeTabClosedListener(tabClosedListener);
            }
        });
    }

    private static GraphicsConfiguration getGraphicsConfig(int monitorIndex) {
        GraphicsDevice[] gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (monitorIndex >= gs.length) {
            monitorIndex = 0;
        }
        return gs[monitorIndex].getDefaultConfiguration();
    }
}
