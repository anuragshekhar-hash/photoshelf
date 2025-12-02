package org.photoshelf;

import org.photoshelf.ui.ImagePanelManager;
import org.photoshelf.ui.SelectionCallback;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.nio.channels.ClosedByInterruptException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;

public class PhotoShelfUI extends JFrame implements SelectionCallback {
    private SwingWorker<?, ?> currentWorker;
    private final PhotoShelfModel model;
    private final DirectoryTreeManager directoryTreeManager;
    private final ImagePanelManager imagePanelManager;
    private final ToolbarManager toolbarManager;
    private final PreviewPanelManager previewPanelManager;
    private final StatusPanelManager statusPanelManager;
    private Thread directoryWatcherThread;
    private final HybridCache<String, ImageIcon> thumbnailCache;
    private final Set<File> duplicateFiles = new HashSet<>();
    private SwingWorker<Void, JLabel> resizerWorker;
    private final KeywordManager keywordManager;

    public PhotoShelfUI() {
        setTitle("PhotoShelf");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        keywordManager = new KeywordManager();
        model = new PhotoShelfModel(keywordManager);
        directoryTreeManager = new DirectoryTreeManager(this);
        imagePanelManager = new ImagePanelManager(this);
        toolbarManager = new ToolbarManager(this);
        previewPanelManager = new PreviewPanelManager(keywordManager);
        statusPanelManager = new StatusPanelManager();
        thumbnailCache = new HybridCache<>("thumbnails", 200);

        setJMenuBar(createMenuBar());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolbarManager.getToolPanel(), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(statusPanelManager.getStatusPanel(), BorderLayout.SOUTH);

        JScrollPane treeScroll = new JScrollPane(directoryTreeManager.getDirectoryTree());

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, previewPanelManager.getPreviewScroll());
        topSplit.setDividerLocation(250);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, imagePanelManager.getPanel());
        mainSplit.setDividerLocation(320);
        add(mainSplit, BorderLayout.CENTER);

        File rootDir = new File(System.getProperty("user.home"));
        model.setCurrentDirectory(rootDir);
        displayImages(model.getCurrentDirectory());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                thumbnailCache.shutdown();
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu selectionMenu = new JMenu("Selection");
        menuBar.add(selectionMenu);

        JMenuItem selectByKeywordItem = new JMenuItem("Select by Keywords...");
        selectByKeywordItem.addActionListener(e -> selectByKeywords());
        selectionMenu.add(selectByKeywordItem);

        JMenuItem selectByNoKeywordItem = new JMenuItem("Select with No Keywords");
        selectByNoKeywordItem.addActionListener(e -> selectWithNoKeywords());
        selectionMenu.add(selectByNoKeywordItem);

        selectionMenu.add(new JSeparator());

        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener(e -> selectAllVisible());
        selectionMenu.add(selectAllItem);

        JMenuItem deselectAllItem = new JMenuItem("Deselect All");
        deselectAllItem.addActionListener(e -> clearSelectionUI());
        selectionMenu.add(deselectAllItem);

        return menuBar;
    }

    private void selectByKeywords() {
        String keywordsStr = JOptionPane.showInputDialog(this, "Enter keywords, separated by commas:", "Select by Keywords", JOptionPane.PLAIN_MESSAGE);
        if (keywordsStr == null || keywordsStr.trim().isEmpty()) {
            return;
        }

        List<String> keywordsToMatch = Arrays.stream(keywordsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (keywordsToMatch.isEmpty()) {
            return;
        }

        clearSelectionUI();
        JPanel imagePanel = imagePanelManager.getImagePanel();
        for (Component comp : imagePanel.getComponents()) {
            if (comp instanceof JLabel && comp.isVisible()) {
                JLabel label = (JLabel) comp;
                File file = (File) label.getClientProperty("imageFile");
                if (file != null) {
                    Set<String> fileKeywords = keywordManager.getKeywords(file);
                    if (fileKeywords.containsAll(keywordsToMatch)) {
                        addToSelectionUI(label);
                    }
                }
            }
        }
    }

    private void selectWithNoKeywords() {
        clearSelectionUI();
        JPanel imagePanel = imagePanelManager.getImagePanel();
        for (Component comp : imagePanel.getComponents()) {
            if (comp instanceof JLabel && comp.isVisible()) {
                JLabel label = (JLabel) comp;
                File file = (File) label.getClientProperty("imageFile");
                if (file != null) {
                    if (keywordManager.getKeywords(file).isEmpty()) {
                        addToSelectionUI(label);
                    }
                }
            }
        }
    }

    private void selectAllVisible() {
        JPanel imagePanel = imagePanelManager.getImagePanel();
        for (Component comp : imagePanel.getComponents()) {
            // Only select items that are currently visible
            if (comp instanceof JLabel && comp.isVisible()) {
                JLabel label = (JLabel) comp;
                // Add to selection without clearing others
                addToSelectionUI(label);
            }
        }
    }

    public void filterToSelection(boolean addFilter) {
        JPanel imagePanel = imagePanelManager.getImagePanel();
        Set<JLabel> selectedLabels = new HashSet<>();
        selectedLabels.addAll(model.getSelectedLabels());
        if (selectedLabels.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No images are selected.", "Filter to Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int visibleCount = 0;
        for (Component comp : imagePanel.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (!addFilter) {
                    label.setVisible(true);
                    visibleCount++;
                } else if (selectedLabels.contains(label)) {
                    label.setVisible(true);
                    visibleCount++;
                } else {
                    label.setVisible(false);
                }
            }
        }
        imagePanel.revalidate();
        imagePanel.repaint();
        statusPanelManager.updateTotalFiles(visibleCount);
        setSearchStatus("Filtered to " + visibleCount + " selected images.");
        toolbarManager.setFilteredToSelection(addFilter);
    }

    public void displayImages(File dir) {
        displayImage(dir);
    }

    private void displayImage(File dir) {
        setSearchStatus(null);
        model.setCurrentDirectory(dir);
        directoryTreeManager.setSelectedDirectory(dir);
        toolbarManager.setFilteredToSelection(false);
        prepareForNewTask();
        duplicateFiles.clear();

        ImageLoader imageLoader = new ImageLoader(this, imagePanelManager.getImagePanel(), model.getCurrentDirectory(), toolbarManager.getFilterText(), toolbarManager.getSortCriteria(), toolbarManager.isSortDescending(), toolbarManager.isShowDuplicates(), imagePanelManager.getThumbnailSize());
        currentWorker = imageLoader;
        imageLoader.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                statusPanelManager.updateTotalFiles(imagePanelManager.getImagePanel().getComponentCount());
            }
        });
        imageLoader.execute();

        directoryWatcherThread = new Thread(new DirectoryWatcher(this, dir));
        directoryWatcherThread.start();
    }

    public void executeSearch(SearchParams params, boolean filterCurrentView) {
        if (filterCurrentView) {
            filterImageGrid(params);
        } else {
            search(params);
        }
    }

    private void filterImageGrid(SearchParams params) {
        JPanel imagePanel = imagePanelManager.getImagePanel();
        int visibleCount = 0;
        for (Component comp : imagePanel.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                File file = (File) label.getClientProperty("imageFile");
                if (file != null) {
                    if (checkFileMatches(file, params)) {
                        label.setVisible(true);
                        visibleCount++;
                    } else {
                        label.setVisible(false);
                    }
                }
            }
        }
        imagePanel.revalidate();
        imagePanel.repaint();
        statusPanelManager.updateTotalFiles(visibleCount);
        setSearchStatus("Filtered view: " + visibleCount + " images found.");
    }

    private boolean checkFileMatches(File file, SearchParams params) {
        if (params.getSearchString() != null && !params.getSearchString().isEmpty()) {
            if (!file.getName().toLowerCase().contains(params.getSearchString().toLowerCase())) {
                return false;
            }
        }
        if (params.isNoKeywords()) {
            return keywordManager.getKeywords(file).isEmpty();
        }
        if (params.hasKeyword()) {
            return params.evaluate(keywordManager.getKeywords(file));
        } else if (params.getKeywords() != null && !params.getKeywords().isEmpty()) {
            Set<String> fileKeywords = keywordManager.getKeywords(file);
            return fileKeywords.containsAll(params.getKeywords());
        } else if (params.isNoKeywords()) {
            return keywordManager.getKeywords(file).isEmpty();
        }

        return true;
    }

    public void search(SearchParams params) {
        prepareForNewTask();
        setSearchStatus("Searching...");
        String filter = toolbarManager.getFilterText().trim();
        Searcher searcher = new Searcher(this, getCurrentDirectory(), params, filter);
        currentWorker = searcher;
        searcher.execute();
    }

    public void addNewImage(File imgFile) {
        try {
            int thumbnailSize = imagePanelManager.getThumbnailSize();
            ImageIcon icon = createDisplayIcon(imgFile, thumbnailSize, thumbnailSize);
            String name = imgFile.getName();
            String shortName = name.length() > 20 ? name.substring(0, 17) + "..." : name;
            JLabel label = new JLabel(shortName, icon, JLabel.CENTER);
            label.setHorizontalTextPosition(JLabel.CENTER);
            label.setVerticalTextPosition(JLabel.BOTTOM);
            label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.putClientProperty("imageFile", imgFile);
            label.setPreferredSize(new java.awt.Dimension(thumbnailSize + 8, thumbnailSize + 40));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            label.addMouseListener(createImageMouseListener());
            imagePanelManager.addImageLabel(label);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not create thumbnail for new file: " + e.getMessage());
        }
    }

    public File getCurrentDirectory() {
        return model.getCurrentDirectory();
    }

    MouseAdapter createImageMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JLabel clickedLabel = (JLabel) e.getSource();
                File imgFile = (File) clickedLabel.getClientProperty("imageFile");
                e.consume();

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (!model.isSelected(clickedLabel)) {
                        clearSelectionUI();
                        addToSelectionUI(clickedLabel);
                    }
                    showImageOptions();
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    //directoryTreeManager.setSelectedDirectory(imgFile.getParentFile());
                    previewPanelManager.showImagePreview(imgFile);
                    statusPanelManager.updatePreviewFile(imgFile.getName());
                    if (e.isControlDown() || e.isMetaDown()) {
                        toggleSelectionUI(clickedLabel);
                    } else {
                        clearSelectionUI();
                        addToSelectionUI(clickedLabel);
                    }
                }
            }
        };
    }

    public void clearSelectionUI() {
        for (JLabel label : model.getSelectedLabels()) {
            if (isDuplicate((File) label.getClientProperty("imageFile"))) {
                label.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            } else {
                label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            }
        }
        model.clearSelection();
        updateSelectionStatus();
    }

    public void addToSelectionUI(JLabel label) {
        model.addToSelection(label);
        label.setBorder(BorderFactory.createLineBorder(SystemColor.controlDkShadow, 2));
        updateSelectionStatus();
    }

    private void toggleSelectionUI(JLabel label) {
        if (model.isSelected(label)) {
            model.removeFromSelection(label);
            if (isDuplicate((File) label.getClientProperty("imageFile"))) {
                label.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            } else {
                label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            }
        } else {
            model.addToSelection(label);
            label.setBorder(BorderFactory.createLineBorder(SystemColor.controlDkShadow, 2));
        }
        updateSelectionStatus();
    }

    private void updateSelectionStatus() {
        statusPanelManager.updateSelectionCount(model.getSelectedLabels().size());
        long totalSize = 0;
        for (JLabel label : model.getSelectedLabels()) {
            File file = (File) label.getClientProperty("imageFile");
            totalSize += file.length();
        }
        statusPanelManager.updateSelectionSize(totalSize);
    }

    ImageIcon createDisplayIcon(File imgFile, int maxWidth, int maxHeight) throws IOException {
        String cacheKey = imgFile.getAbsolutePath() + "_" + imgFile.lastModified() + "_" + maxWidth + "x" + maxHeight;
        ImageIcon cachedIcon = thumbnailCache.get(cacheKey);
        if (cachedIcon != null) {
            return cachedIcon;
        }

        BufferedImage originalImage = ImageIO.read(imgFile);
        if (originalImage == null) {
            originalImage = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = originalImage.createGraphics();
            g2d.setFont(new Font("Serif", Font.BOLD, 24));
            g2d.setColor(Color.RED);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawString("Failed", 0, maxHeight/2);
            System.out.println("Unsupported image format: " + imgFile.getName());
            g2d.dispose();
        }

        int imgWidth = originalImage.getWidth();
        int imgHeight = originalImage.getHeight();

        ImageIcon icon;
        if (maxWidth >= imgWidth && maxHeight >= imgHeight) {
            icon = new ImageIcon(originalImage);
        } else {
            double scale = Math.min((double) maxWidth / imgWidth, (double) maxHeight / imgHeight);
            int newWidth = (int) (imgWidth * scale);
            int newHeight = (int) (imgHeight * scale);

            // Ensure dimensions are at least 1x1 to prevent exceptions when creating the image
            newWidth = Math.max(1, newWidth);
            newHeight = Math.max(1, newHeight);

            BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            icon = new ImageIcon(scaledImage);
        }

        thumbnailCache.put(cacheKey, icon);
        return icon;
    }

    private class ThumbnailResizer extends SwingWorker<Void, JLabel> {
        private final int newSize;

        ThumbnailResizer(int newSize) {
            this.newSize = newSize;
        }

        @Override
        protected Void doInBackground() throws Exception {
            Component[] components = imagePanelManager.getImagePanel().getComponents();
            for (Component com : components) {
                if (isCancelled()) break;

                if (com instanceof JLabel) {
                    JLabel label = (JLabel) com;
                    try {
                        ImageIcon newIcon = createDisplayIcon((File) label.getClientProperty("imageFile"), newSize, newSize);
                        if (newIcon == null) return null;
                        label.setIcon(newIcon);
                        label.setPreferredSize(new Dimension(newSize + 8, newSize + 40));
                        publish(label);
                    } catch (ClosedByInterruptException e) {
                        // This is expected if the resize operation is cancelled by the user.
                        // We can safely ignore it and let the worker thread terminate.
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Failed to resize thumbnail: " + e.getMessage());
                    }
                }
            }
            return null;
        }

        @Override
        protected void done() {
            // Re-layout the panel once all icons are resized
            imagePanelManager.getImagePanel().revalidate();
            imagePanelManager.getImagePanel().repaint();
        }
    }

    public void resizeView() {
        // Cancel any previous resize operation that is still running
        if (resizerWorker != null && !resizerWorker.isDone()) {
            resizerWorker.cancel(true);
        }

        // Start a new resize worker
        int newSize = imagePanelManager.getThumbnailSize();
        resizerWorker = new ThumbnailResizer(newSize);
        resizerWorker.execute();
    }

    private void showImageOptions() {
        if (model.getSelectedLabels().isEmpty()) {
            return;
        }

        List<JLabel> selectedLabels = new ArrayList<>(model.getSelectedLabels());

        JPopupMenu menu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem moveItem = new JMenuItem("Move");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem copyPathItem = new JMenuItem("Copy Path");
        JMenuItem manageKeywordsItem = new JMenuItem("Manage Keywords");

        renameItem.setEnabled(selectedLabels.size() == 1);
        moveItem.setEnabled(!selectedLabels.isEmpty());
        copyItem.setEnabled(!selectedLabels.isEmpty());
        deleteItem.setEnabled(!selectedLabels.isEmpty());
        copyPathItem.setEnabled(!selectedLabels.isEmpty());
        manageKeywordsItem.setEnabled(!selectedLabels.isEmpty());

        renameItem.addActionListener(e -> handleRenameImage(selectedLabels.get(0)));
        moveItem.addActionListener(e -> handleMoveImages(selectedLabels));
        copyItem.addActionListener(e -> handleCopyImages(selectedLabels));
        deleteItem.addActionListener(e -> handleDeleteImages(selectedLabels));

        copyPathItem.addActionListener(e -> {
            File firstSelectedFile = (File) selectedLabels.get(0).getClientProperty("imageFile");
            StringSelection selection = new StringSelection(firstSelectedFile.getAbsolutePath());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        });

        manageKeywordsItem.addActionListener(e -> handleManageKeywords(selectedLabels));

        menu.add(renameItem);
        menu.add(moveItem);
        menu.add(copyItem);
        menu.add(deleteItem);
        menu.add(copyPathItem);
        menu.add(new JSeparator());
        menu.add(manageKeywordsItem);

        File firstSelectedFile = (File) selectedLabels.get(0).getClientProperty("imageFile");
        if (firstSelectedFile.getName().toLowerCase().endsWith(".webp")) {
            JMenuItem convertToJpegItem = new JMenuItem("Convert to JPEG");
            convertToJpegItem.addActionListener(e -> handleConvertToJpeg(selectedLabels.get(0)));
            menu.add(convertToJpegItem);
        }

        menu.show(imagePanelManager.getImagePanel(), MouseInfo.getPointerInfo().getLocation().x - imagePanelManager.getImagePanel().getLocationOnScreen().x,
                MouseInfo.getPointerInfo().getLocation().y - imagePanelManager.getImagePanel().getLocationOnScreen().y);
    }

    private void handleConvertToJpeg(JLabel label) {
        File webpFile = (File) label.getClientProperty("imageFile");
        try {
            BufferedImage image = ImageIO.read(webpFile);
            if (image == null) {
                JOptionPane.showMessageDialog(this, "Could not read WebP image.", "Conversion Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String newName = webpFile.getName().substring(0, webpFile.getName().lastIndexOf('.')) + ".jpg";
            File newFile = new File(webpFile.getParentFile(), newName);
            ImageIO.write(image, "jpg", newFile);

            Set<String> keywords = keywordManager.getKeywords(webpFile);
            keywordManager.addKeywords(newFile, new ArrayList<>(keywords));

            displayImages(model.getCurrentDirectory());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error converting to JPEG: " + e.getMessage(), "Conversion Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleManageKeywords(List<JLabel> labels) {
        if (labels.isEmpty()) {
            return;
        }

        List<File> files = labels.stream()
                .map(label -> (File) label.getClientProperty("imageFile"))
                .collect(Collectors.toList());

        KeywordManagementDialog dialog = new KeywordManagementDialog(this, keywordManager, files);
        dialog.setVisible(true);
    }

    private void handleRenameImage(JLabel labelToRename) {
        File imgFile = (File) labelToRename.getClientProperty("imageFile");
        String newName = JOptionPane.showInputDialog(this, "Enter new name:", imgFile.getName());
        if (newName != null && !newName.trim().isEmpty()) {
            if (model.renameImage(imgFile, newName)) {
                thumbnailCache.remove(imgFile.getAbsolutePath() + "_" + imgFile.lastModified() + "_" + imagePanelManager.getThumbnailSize() + "x" + imagePanelManager.getThumbnailSize());
                displayImages(model.getCurrentDirectory());
            } else {
                JOptionPane.showMessageDialog(this, "Rename failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleMoveImages(List<JLabel> labelsToMove) {
        File destDir = getDestinationDirectoryFromUser();
        if (destDir == null) return;

        JPanel imagePanel = imagePanelManager.getImagePanel();
        for (JLabel label : labelsToMove) {
            File imgFile = (File) label.getClientProperty("imageFile");
            File destFile = getDestinationFileWithPrompt(destDir, imgFile.getName());
            if (destFile == null) continue;
            try {
                model.moveImage(imgFile, destFile);
                imagePanel.remove(label);
                thumbnailCache.remove(imgFile.getAbsolutePath() + "_" + imgFile.lastModified() + "_" + imagePanelManager.getThumbnailSize() + "x" + imagePanelManager.getThumbnailSize());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Move failed for " + imgFile.getName() + ": " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        clearSelectionUI();
        imagePanel.revalidate();
        imagePanel.repaint();
        statusPanelManager.updateTotalFiles(imagePanel.getComponentCount());
    }

    private void handleCopyImages(List<JLabel> labelsToCopy) {
        File destDir = getDestinationDirectoryFromUser();
        if (destDir == null) return;

        int successCount = 0;
        for (JLabel label : labelsToCopy) {
            File imgFile = (File) label.getClientProperty("imageFile");
            File destFile = getDestinationFileWithPrompt(destDir, imgFile.getName());
            if (destFile == null) continue;
            try {
                model.copyImage(imgFile, destFile);
                successCount++;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Copy failed for " + imgFile.getName() + ": " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        JOptionPane.showMessageDialog(this, "Copied " + successCount + " of " + labelsToCopy.size() + " files to " + destDir.getAbsolutePath(), "Copy Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleDeleteImages(List<JLabel> labelsToDelete) {
        String message = labelsToDelete.size() == 1
                ? "Are you sure you want to permanently delete this file?\n" + ((File) labelsToDelete.get(0).getClientProperty("imageFile")).getName()
                : "Are you sure you want to permanently delete these " + labelsToDelete.size() + " files?";

        int result = JOptionPane.showConfirmDialog(this, message, "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            JPanel imagePanel = imagePanelManager.getImagePanel();
            for (JLabel label : labelsToDelete) {
                File imgFile = (File) label.getClientProperty("imageFile");
                try {
                    model.deleteImage(imgFile);
                    imagePanel.remove(label);
                    thumbnailCache.remove(imgFile.getAbsolutePath() + "_" + imgFile.lastModified() + "_" + imagePanelManager.getThumbnailSize() + "x" + imagePanelManager.getThumbnailSize());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Delete failed for " + imgFile.getName() + ": " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            clearSelectionUI();
            imagePanel.revalidate();
            imagePanel.repaint();
            statusPanelManager.updateTotalFiles(imagePanel.getComponentCount());
        }
    }

    private File getDestinationDirectoryFromUser() {
        final File[] selectedDirectory = new File[1];

        JDialog dialog = new JDialog(this, "Select Destination", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Select a recent destination or browse:"));

        for (File location : model.getRecentLocations()) {
            JButton button = new JButton(location.getAbsolutePath());
            button.addActionListener(e -> {
                selectedDirectory[0] = location;
                dialog.dispose();
            });
            panel.add(button);
        }

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(model.getCurrentDirectory());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                selectedDirectory[0] = chooser.getSelectedFile();
            }
            dialog.dispose();
        });
        panel.add(browseButton);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (selectedDirectory[0] != null) {
            model.updateRecentLocations(selectedDirectory[0]);
        }

        return selectedDirectory[0];
    }

    private File getDestinationFileWithPrompt(File destDir, String originalName) {
        File destFile = new File(destDir, originalName);
        while (destFile.exists()) {
            int option = JOptionPane.showConfirmDialog(this, "File '" + originalName + "' already exists in the destination. Overwrite?", "Overwrite Warning", JOptionPane.YES_NO_CANCEL_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                break;
            } else if (option == JOptionPane.NO_OPTION) {
                String newName = JOptionPane.showInputDialog(this, "Enter new name for '" + originalName + "':", originalName);
                if (newName == null || newName.trim().isEmpty()) return null; // User cancelled input
                destFile = new File(destDir, newName);
            } else { // CANCEL_OPTION or dialog closed
                return null;
            }
        }
        return destFile;
    }

    private void prepareForNewTask() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
        if (directoryWatcherThread != null && directoryWatcherThread.isAlive()) {
            directoryWatcherThread.interrupt();
        }

        imagePanelManager.clearImagePanel();
        clearSelectionUI();
    }

    public void setSearchStatus(String status) {
        statusPanelManager.setSearchStatus(status);
    }

    public boolean isDuplicate(File file) {
        return duplicateFiles.contains(file);
    }

    public void setDuplicateFiles(Set<File> duplicates) {
        this.duplicateFiles.clear();
        this.duplicateFiles.addAll(duplicates);
    }

    public Set<File> getDuplicateFiles() {
        return duplicateFiles;
    }

    public KeywordManager getKeywordManager() {
        return keywordManager;
    }

    public ImagePanelManager getImagePanelManager() {
        return imagePanelManager;
    }

    public void updateTotalFile(int count) {
        statusPanelManager.updateTotalFiles(count);
    }
}
