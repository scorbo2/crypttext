package ca.corbett.crypttext.ui;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.CryptTextResourceLoader;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * A custom tab header component for our editor tabs, to replace the built-in Java Swing option,
 * which is insufficient. Our custom tab header contains not only the optional icon, and the tab name,
 * but also a close button and an "is dirty" indicator.
 */
public class EditorTabHeader extends JPanel {
    private final EditorTab ownerTab;
    private final JLabel label;
    private final JButton closeButton;

    public EditorTabHeader(EditorTab ownerTab, String name) {
        super(new GridBagLayout());
        setOpaque(false);
        this.ownerTab = ownerTab;

        // The label will hold the optional icon and the tab name.
        // We'll update the label text with an asterisk if the tab is dirty.
        label = new JLabel("", ownerTab.getIcon(), JLabel.LEFT);
        updateLabel(name);
        label.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 5); // add some space between the label and the close button
        label.addMouseListener(new MouseRedispatcher());
        label.addMouseMotionListener(new MouseRedispatcher());
        add(label, gbc);

        // Our close button:
        closeButton = new JButton();
        resetIcon();
        closeButton.setOpaque(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        gbc.gridx = 1;
        closeButton.addMouseListener(new MouseRedispatcher());
        closeButton.addMouseMotionListener(new MouseRedispatcher());
        closeButton.addActionListener(e -> ownerTab.close());
        add(closeButton, gbc);
    }

    public void updateLabel(String newName) {
        String name = ownerTab.isDirty() ? newName + " *" : newName;
        label.setText(name);
        label.setIcon(ownerTab.getIcon()); // this may have changed
    }

    public void resetIcon() {
        int iconSize = AppConfig.getInstance().getTabIconSize();
        ImageIcon icon = ownerTab.isDirty()
                ? CryptTextResourceLoader.getCloseDirtyIcon(iconSize)
                : CryptTextResourceLoader.getCloseCleanIcon(iconSize);
        closeButton.setIcon(icon);
        closeButton.setPreferredSize(new Dimension(iconSize + 4, iconSize + 4));
    }

    /**
     * We need to redispatch certain mouse events from our custom tab header components up
     * to the JTabbedPane, so that look and feel style changes (dynamic background color
     * change on mouseover, for example) works properly.
     */
    private static class MouseRedispatcher implements MouseListener, MouseMotionListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            redispatch(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            redispatch(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            redispatch(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            redispatch(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            redispatch(e);
        }

        @Override
        public void mouseDragged(MouseEvent ignored) {
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            redispatch(e);
        }

        // Redispatches to the great-grandparent of the source component.
        private void redispatch(MouseEvent e) {
            Component source = (Component)e.getSource();
            if (source == null) {
                return; // should never happen, but just in case
            }

            // This is a little non-obvious, but the great-grandparent
            // of the source component is the JTabbedPane that holds our tab headers.
            Component headerPanel = source.getParent();
            if (headerPanel == null) {
                return;
            }
            Component tabContainer = headerPanel.getParent();
            if (tabContainer == null) {
                return; // saw this one happen once when closing a tab
            }
            Component tabbedPane = tabContainer.getParent();
            if (tabbedPane == null) {
                return; // let's just play it safe to avoid ugly NPEs
            }

            // Redispatch it!
            MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, e, tabbedPane);
            tabbedPane.dispatchEvent(parentEvent);
        }
    }
}
