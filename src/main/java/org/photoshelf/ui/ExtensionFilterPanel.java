package org.photoshelf.ui;

import org.photoshelf.plugin.PhotoShelfPlugin;
import org.photoshelf.service.PluginManager;
import org.photoshelf.service.PluginStateListener;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExtensionFilterPanel extends JPanel implements PluginStateListener {
    private final JButton filterButton;
    private final JPopupMenu popupMenu;
    private final Set<String> selectedExtensions = new HashSet<>();
    private Runnable onFilterChanged;

    public ExtensionFilterPanel() {
        setLayout(new BorderLayout());
        
        filterButton = new JButton("Extensions \u25BC");
        filterButton.setFocusPainted(false);
        add(filterButton, BorderLayout.CENTER);

        popupMenu = new JPopupMenu();
        
        filterButton.addActionListener(e -> showPopup());
        
        PluginManager.getInstance().addPluginStateListener(this);
        
        // Initialize with all supported extensions selected
        refreshExtensions();
    }

    public void setOnFilterChanged(Runnable onFilterChanged) {
        this.onFilterChanged = onFilterChanged;
    }

    public Set<String> getSelectedExtensions() {
        return new HashSet<>(selectedExtensions);
    }

    public void refreshExtensions() {
        // Get the list of file types from the extension list provided by PluginManager
        Set<String> allExtensions = PluginManager.getInstance().getAllSupportedExtensions();
        
        // Reset selection to include all currently supported extensions
        // This ensures that if a new plugin is enabled, its file type is immediately selected/visible.
        selectedExtensions.clear();
        selectedExtensions.addAll(allExtensions);

        popupMenu.removeAll();

        // Add "Select All" / "Select None"
        JMenuItem selectAll = new JMenuItem("Select All");
        selectAll.addActionListener(e -> {
            selectedExtensions.addAll(allExtensions);
            updateMenuState();
            fireFilterChanged();
        });
        popupMenu.add(selectAll);

        JMenuItem selectNone = new JMenuItem("Select None");
        selectNone.addActionListener(e -> {
            selectedExtensions.clear();
            updateMenuState();
            fireFilterChanged();
        });
        popupMenu.add(selectNone);
        popupMenu.add(new JSeparator());

        // Add individual extensions, sorted alphabetically
        List<String> sortedExts = allExtensions.stream().sorted().collect(Collectors.toList());
        for (String ext : sortedExts) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(ext.toUpperCase());
            item.setSelected(selectedExtensions.contains(ext));
            item.addActionListener(e -> {
                if (item.isSelected()) {
                    selectedExtensions.add(ext);
                } else {
                    selectedExtensions.remove(ext);
                }
                fireFilterChanged();
            });
            popupMenu.add(item);
        }
    }
    
    private void updateMenuState() {
        // Skip first 3 components (Select All, Select None, Separator)
        Component[] components = popupMenu.getComponents();
        for (int i = 3; i < components.length; i++) {
            if (components[i] instanceof JCheckBoxMenuItem) {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem) components[i];
                String ext = item.getText().toLowerCase();
                item.setSelected(selectedExtensions.contains(ext));
            }
        }
    }

    private void showPopup() {
        popupMenu.show(filterButton, 0, filterButton.getHeight());
    }

    private void fireFilterChanged() {
        if (onFilterChanged != null) {
            onFilterChanged.run();
        }
    }

    @Override
    public void onPluginStateChanged(PhotoShelfPlugin plugin, boolean active) {
        SwingUtilities.invokeLater(this::refreshExtensions);
    }
}
