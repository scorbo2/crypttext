package ca.corbett.crypttext.ui;

import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * A custom "block" cursor, for that old-school terminal feel.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class BlockCursor extends DefaultCaret {

    private final Timer blinkTimer;

    /**
     * Creates a new BlockCursor with the specified blink rate in milliseconds.
     * A value of zero or less means "don't blink".
     *
     * @param blinkRate The blink rate in milliseconds, or zero/negative for no blinking.
     */
    public BlockCursor(int blinkRate) {
        // The parent class's timer has strange behavior where the blink rate slows
        // down noticeably when no mouse or keyboard activity is happening in the text pane.
        // We can try to work around this by creating our own timer and bypassing
        // the RepaintManager's timer coalescing behavior, but in my testing,
        // this does not do much to fix the problem. It remains unsolved for now.
        setBlinkRate(0); // disable parent class's timer... we'll make our own

        if (blinkRate > 0) {
            blinkTimer = new Timer(blinkRate, e -> {
                if (getComponent() != null && getComponent().hasFocus()) {
                    setVisible(!isVisible());
                    getComponent().paintImmediately(x, y, width, height);
                }
            });
            blinkTimer.setCoalesce(false); // don't skip missed ticks
        }
        else {
            blinkTimer = null; // no timer needed if we're not blinking
        }
    }

    @Override
    public void install(JTextComponent c) {
        super.install(c);
        if (blinkTimer != null) {
            blinkTimer.start();
        }
    }

    @Override
    public void deinstall(JTextComponent c) {
        if (blinkTimer != null) {
            blinkTimer.stop();
        }
        super.deinstall(c);
    }

    /**
     * Adjusts the damaged area to cover the full character width.
     */
    @Override
    public void damage(Rectangle r) {
        if (r == null) {
            return;
        }
        x = r.x;
        y = r.y;
        width = getComponent().getFontMetrics(getComponent().getFont()).charWidth('m');
        height = r.height;
        repaint(); // calls getComponent().repaint(x, y, width, height)
    }

    /**
     * Calculates the width based on character metrics and fills
     * the given rectangle to render the block cursor.
     * If the dot position is invalid, it fails silently.
     */
    @Override
    public void paint(Graphics g) {
        if (isVisible()) {
            try {
                // We want the block cursor to be 75% opaque:
                Color cursorColor = getComponent().getCaretColor();
                Color newColor = new Color(cursorColor.getRed(), cursorColor.getGreen(), cursorColor.getBlue(), 192);

                // Get the target rectangle and set our color:
                Rectangle r = getComponent().getUI().modelToView(getComponent(), getDot());
                g.setColor(newColor);

                // In my testing, I find that r.width is always 0, and I don't know why.
                // So, let's set something reasonable instead:
                int width = getComponent().getFontMetrics(getComponent().getFont()).charWidth('m');
                g.fillRect(r.x, r.y, width, r.height);
            }
            catch (BadLocationException e) {
                // can't render cursor
            }
        }
    }
}
