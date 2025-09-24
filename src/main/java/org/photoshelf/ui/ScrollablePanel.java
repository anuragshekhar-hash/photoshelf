package org.photoshelf.ui;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * A custom JPanel that implements the Scrollable interface.
 * This allows it to be used within a JScrollPane and have its content wrap
 * vertically instead of creating a horizontal scrollbar.
 */
public class ScrollablePanel extends JPanel implements Scrollable {

    public ScrollablePanel() {
        super();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16; // A reasonable value for arrow key scrolling
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height; // For page up/down scrolling
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true; // This is the magic! It forces the panel's width to match the viewport's width.
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false; // We want the panel to be as tall as needed, so we need a vertical scrollbar.
    }
}