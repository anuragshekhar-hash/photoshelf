package org.photoshelf.ui;

import org.photoshelf.FileTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class DirectoryTreeCellRenderer extends DefaultTreeCellRenderer {
    private final Icon folderIcon;

    public DirectoryTreeCellRenderer() {
        folderIcon = UIManager.getIcon("FileView.directoryIcon");
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof FileTreeNode) {
            setIcon(folderIcon);
        }
        return c;
    }
}
