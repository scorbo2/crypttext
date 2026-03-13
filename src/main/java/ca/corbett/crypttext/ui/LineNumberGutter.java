package ca.corbett.crypttext.ui;


import ca.corbett.crypttext.AppConfig;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
    private Font lineNumberFont;
    private Color lineNumberColor;
    private int dragStartLine = -1;

    public LineNumberGutter(JTextPane textPane) {
        this.textPane = textPane;
        lineNumberFont = AppConfig.getInstance().getGutterFont();
        textPane.getDocument().addDocumentListener(this);
        textPane.addCaretListener(this);
        // Also revalidate on component resize
        textPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                revalidate();
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    textPane.selectAll();
                    textPane.requestFocusInWindow();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    dragStartLine = getLineAtY(e.getY());
                    selectLine(dragStartLine);
                    textPane.requestFocusInWindow();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartLine >= 0) {
                    int currentLine = getLineAtY(e.getY());
                    selectLineRange(dragStartLine, currentLine);
                }
            }
        });

        updateColors();
    }

    /**
     * Invoke this to force an update of our background and foreground and border
     * colors based on whatever is currently set in application preferences.
     */
    public void updateColors() {
        Color bg = AppConfig.getInstance().getGutterBackgroundColor();
        Color fg = AppConfig.getInstance().getGutterForegroundColor();
        setBackground(bg);
        lineNumberColor = fg;
        Color borderColor;
        if (bg.getRed() < 128 && bg.getGreen() < 128 && bg.getBlue() < 128) {
            // If the background is dark, use a lighter border color
            borderColor = bg.brighter();
        }
        else {
            borderColor = bg.darker();
        }
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor));
        repaint();
    }

    /**
     * Updates the font used for displaying line numbers.
     * If null is supplied, we'll revert to the default provided by AppConfig.
     */
    public void setLineNumberFont(Font newFont) {
        if (newFont == null) {
            newFont = AppConfig.DEFAULT_GUTTER_FONT;
        }
        this.lineNumberFont = newFont;
        revalidate();
        repaint();
    }

    private int getLineCount() {
        // Count logical (document) lines, not word-wrapped visual lines
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
        g2.setColor(lineNumberColor);

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

    /**
     * Returns the zero-based line index in the document that is nearest to the given
     * Y coordinate (in this component's coordinate space).
     */
    private int getLineAtY(int y) {
        Element root = textPane.getDocument().getDefaultRootElement();
        int lineCount = root.getElementCount();
        int offset = textPane.viewToModel2D(new java.awt.Point(0, y));
        int line = root.getElementIndex(offset);
        return Math.max(0, Math.min(line, lineCount - 1));
    }

    /**
     * Selects the entire content of the given zero-based line in the text pane.
     */
    private void selectLine(int lineIndex) {
        Element root = textPane.getDocument().getDefaultRootElement();
        int clamped = Math.max(0, Math.min(lineIndex, root.getElementCount() - 1));
        Element lineElement = root.getElement(clamped);
        int docLength = textPane.getDocument().getLength();
        textPane.setSelectionStart(lineElement.getStartOffset());
        textPane.setSelectionEnd(Math.min(lineElement.getEndOffset(), docLength));
    }

    /**
     * Selects all lines between fromLine and toLine (inclusive), in any order.
     */
    private void selectLineRange(int fromLine, int toLine) {
        Element root = textPane.getDocument().getDefaultRootElement();
        int lineCount = root.getElementCount();
        int startLine = Math.max(0, Math.min(fromLine, toLine));
        int endLine = Math.min(lineCount - 1, Math.max(fromLine, toLine));
        int docLength = textPane.getDocument().getLength();
        int start = root.getElement(startLine).getStartOffset();
        int end = Math.min(root.getElement(endLine).getEndOffset(), docLength);
        textPane.setSelectionStart(start);
        textPane.setSelectionEnd(end);
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