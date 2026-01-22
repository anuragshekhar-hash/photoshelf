package org.photoshelf.plugin;

import javax.swing.JMenuItem;

/**
 * A plugin that interacts with the User Interface.
 */
public interface UserInterfacePlugin extends PhotoShelfPlugin {
    
    /**
     * Sets the application context (PhotoShelfUI).
     * @param context The main application window.
     */
    void setContext(Object context);

    /**
     * Returns a menu item to be added to the application's menu bar.
     * @return The JMenuItem, or null.
     */
    JMenuItem getMenuItem();
}
