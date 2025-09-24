package org.photoshelf.ui;

import javax.swing.*;
import java.awt.*;

public class SelectionPanel extends JPanel {
    private Rectangle selectionRectangle;
    private final SelectionCallback selectionCallback;

    public SelectionPanel(SelectionCallback callback) {
        this.selectionCallback = callback;
    }

    public void setSelectionRectangle(Rectangle rect) {
        this.selectionRectangle = rect;
    }

    public SelectionCallback getSelectionCallback() {
        return selectionCallback;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (selectionRectangle != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            // A nice semi-transparent blue for the fill
            g2d.setColor(new Color(0, 120, 215, 80));
            g2d.fill(selectionRectangle);
            // A solid blue for the border
            g2d.setColor(new Color(0, 120, 215));
            g2d.draw(selectionRectangle);
            g2d.dispose();
        }
    }

    /**
     * Overrides getPreferredSize to ensure WrapLayout wraps correctly within a JScrollPane.
     * When this panel is the view of a JViewport, its preferred width should be constrained
     * to the viewport's width. The WrapLayout will then calculate the appropriate height.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        Container parent = getParent();
        if (parent instanceof JViewport) {
            preferredSize.width = parent.getWidth(); // Constrain preferred width to viewport's width
        }
        return preferredSize;
    }
}