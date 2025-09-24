package org.photoshelf.ui;

import javax.swing.*;

public interface SelectionCallback {
    void clearSelectionUI();
    void addToSelectionUI(JLabel comp);
}
