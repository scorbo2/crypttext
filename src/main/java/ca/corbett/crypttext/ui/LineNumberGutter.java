package ca.corbett.crypttext.ui;


import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

/**
 * A custom gutter component that displays line numbers for a JTextPane.
 * It listens to document changes and caret movements to update the line numbers accordingly.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a> (with claude.ai)
 */
public class LineNumberGutter extends JPanel implements DocumentListener, CaretListener {

    private final JTextPane textPane;
    private static final int PADDING = 5;
    private Font lineNumberFont; // issue #33 will make this customizable... later

    public LineNumberGutter(JTextPane textPane) {
        this.textPane = textPane;
        lineNumberFont = textPane.getFont().deriveFont(Font.PLAIN);
        setBackground(new Color(240, 240, 240)); // hard-coding colors until we get proper themes in place
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
        textPane.getDocument().addDocumentListener(this);
        textPane.addCaretListener(this);
        // Also revalidate on component resize
        textPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                revalidate();
                repaint();
            }
        });
    }

    private int getLineCount() {
        // Count visual lines (respects word wrap) by walking through the view hierarchy
        Element root = textPane.getDocument().getDefaultRootElement();
        return root.getElementCount();
    }

    @Override
    public Dimension getPreferredSize() {
        int lines = getLineCount();
        String widthSample = String.valueOf(Math.max(lines, 99));
        FontMetrics fm = getFontMetrics(lineNumberFont);
        int width = fm.stringWidth(widthSample) + PADDING * 2;
        return new Dimension(width, textPane.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(lineNumberFont);
        g2.setColor(Color.GRAY);

        FontMetrics fm = g2.getFontMetrics();
        Rectangle clip = g.getClipBounds();

        Element root = textPane.getDocument().getDefaultRootElement();
        int lineCount = root.getElementCount();
        int width = getWidth();

        for (int i = 0; i < lineCount; i++) {
            Element lineElement = root.getElement(i);
            int startOffset = lineElement.getStartOffset();
            try {
                Rectangle2D r = textPane.modelToView2D(startOffset);
                if (r == null) { continue; }
                double y = r.getY() + r.getHeight() - fm.getDescent();
                // Only paint lines in the clip region
                if (r.getY() > clip.y + clip.height) { break; }
                if (r.getY() + r.getHeight() < clip.y) { continue; }
                String lineNum = String.valueOf(i + 1);
                int x = width - fm.stringWidth(lineNum) - PADDING;
                g2.drawString(lineNum, x, (int)y);
            }
            catch (BadLocationException ignored) {
                // skip
            }
        }
    }

    // DocumentListener — repaint when document changes
    public void insertUpdate(DocumentEvent ignored) {
        revalidate();
        repaint();
    }

    public void removeUpdate(DocumentEvent ignored) {
        revalidate();
        repaint();
    }

    public void changedUpdate(DocumentEvent ignored) {
        revalidate();
        repaint();
    }

    // CaretListener — repaint to highlight current line number if desired
    public void caretUpdate(CaretEvent ignored) {
        repaint();
    }
}