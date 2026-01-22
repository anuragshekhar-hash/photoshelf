package org.photoshelf.ui;

import org.photoshelf.plugin.PhotoShelfPlugin;
import org.photoshelf.service.PluginManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PluginManagementDialog extends JDialog {
    public PluginManagementDialog(Frame owner) {
        super(owner, "Manage Plugins", true);
        setLayout(new BorderLayout());
        setSize(400, 500);
        setLocationRelativeTo(owner);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(listPanel);
        
        PluginManager manager = PluginManager.getInstance();
        List<PhotoShelfPlugin> plugins = manager.getAllPlugins();

        for (PhotoShelfPlugin plugin : plugins) {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            JCheckBox checkBox = new JCheckBox(plugin.getName());
            checkBox.setSelected(manager.isPluginActive(plugin));
            checkBox.addActionListener(e -> {
                manager.setPluginActive(plugin, checkBox.isSelected());
            });
            
            JLabel descLabel = new JLabel("<html><body style='width: 250px'>" + plugin.getDescription() + "</body></html>");
            descLabel.setForeground(Color.GRAY);
            
            JPanel textPanel = new JPanel(new BorderLayout());
            textPanel.add(checkBox, BorderLayout.NORTH);
            textPanel.add(descLabel, BorderLayout.CENTER);
            
            itemPanel.add(textPanel, BorderLayout.CENTER);
            listPanel.add(itemPanel);
            listPanel.add(new JSeparator());
        }

        add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}
