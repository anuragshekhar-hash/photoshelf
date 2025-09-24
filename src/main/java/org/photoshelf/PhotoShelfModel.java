package org.photoshelf;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PhotoShelfModel {
    private final List<JLabel> selectedLabels = new ArrayList<>();
    private final LinkedList<File> recentLocations = new LinkedList<>();
    private File currentDirectory;
    private final KeywordManager keywordManager;

    public PhotoShelfModel(KeywordManager keywordManager) {
        this.keywordManager = keywordManager;
    }

    // Selection Management
    public List<JLabel> getSelectedLabels() {
        return selectedLabels;
    }

    public void clearSelection() {
        selectedLabels.clear();
    }

    public void addToSelection(JLabel label) {
        if (!selectedLabels.contains(label)) {
            selectedLabels.add(label);
        }
    }

    public void removeFromSelection(JLabel label) {
        selectedLabels.remove(label);
    }

    public boolean isSelected(JLabel label) {
        return selectedLabels.contains(label);
    }

    // Recent Locations
    public LinkedList<File> getRecentLocations() {
        return recentLocations;
    }

    public void updateRecentLocations(File newLocation) {
        if (newLocation == null || !newLocation.isDirectory()) {
            return;
        }
        recentLocations.remove(newLocation);
        recentLocations.addFirst(newLocation);
        // Keep the list size at a maximum of 4
        while (recentLocations.size() > 4) {
            recentLocations.removeLast();
        }
    }

    // Current Directory
    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(File currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    // File Operations
    public boolean renameImage(File imgFile, String newName) {
        if (newName != null && !newName.trim().isEmpty()) {
            File newFile = new File(imgFile.getParent(), newName);
            if (imgFile.renameTo(newFile)) {
                keywordManager.renameFile(imgFile, newFile);
                return true;
            }
        }
        return false;
    }

    public void moveImage(File source, File destination) throws IOException {
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        keywordManager.renameFile(source, destination);
    }

    public void copyImage(File source, File destination) throws IOException {
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        keywordManager.copyKeywords(source, destination);
    }

    public void deleteImage(File file) throws IOException {
        Files.delete(file.toPath());
        keywordManager.deleteFile(file);
    }
}
