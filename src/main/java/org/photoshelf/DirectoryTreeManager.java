package org.photoshelf;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DirectoryTreeManager {
    private final JTree directoryTree;
    private final DefaultTreeModel treeModel;
    private final PhotoShelfUI mainApp;

    public DirectoryTreeManager(PhotoShelfUI mainApp) {
        this.mainApp = mainApp;

        FileTreeNode root = new FileTreeNode(new File(System.getProperty("user.home")));
        treeModel = new DefaultTreeModel(root);
        directoryTree = new JTree(treeModel);
        directoryTree.setRootVisible(true);
        directoryTree.setCellRenderer(new org.photoshelf.ui.DirectoryTreeCellRenderer());
        addDummyNode(root);

        directoryTree.addTreeSelectionListener(e -> {
            FileTreeNode node = (FileTreeNode) directoryTree.getLastSelectedPathComponent();
            if (node == null) return;
            File dir = node.getFile();
            if (dir.isDirectory()) {
                mainApp.displayImages(dir);
            }
        });

        directoryTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                FileTreeNode node = (FileTreeNode) event.getPath().getLastPathComponent();
                if (!node.isLoaded()) {
                    loadChildrenInBackground(node);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {}
        });
    }

    public JTree getDirectoryTree() {
        return directoryTree;
    }

    public void setSelectedDirectory(File dir) {
        if (dir == null) return;

        List<File> path = new ArrayList<>();
        File treeRootFile = ((FileTreeNode) treeModel.getRoot()).getFile();

        File current = dir;
        while (current != null && !current.equals(treeRootFile.getParentFile())) {
            path.add(current);
            if (current.equals(treeRootFile)) {
                break;
            }
            current = current.getParentFile();
        }
        Collections.reverse(path);

        if (path.isEmpty() || !path.get(0).equals(treeRootFile)) {
            return; // Directory not in the current tree hierarchy
        }

        List<Object> pathObjects = new ArrayList<>();
        pathObjects.add(treeModel.getRoot());

        FileTreeNode currentNode = (FileTreeNode) treeModel.getRoot();

        for (int i = 1; i < path.size(); i++) {
            File targetFile = path.get(i);
            FileTreeNode childNode = findChildNode(currentNode, targetFile);
            if (childNode == null) {
                // Node not found, might not be loaded yet. We can't proceed.
                return;
            }
            pathObjects.add(childNode);
            currentNode = childNode;
        }

        TreePath treePath = new TreePath(pathObjects.toArray());
        directoryTree.expandPath(treePath);
        directoryTree.setSelectionPath(treePath);
        directoryTree.scrollPathToVisible(treePath);
    }

    private FileTreeNode findChildNode(FileTreeNode parent, File file) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) instanceof FileTreeNode) {
                FileTreeNode child = (FileTreeNode) parent.getChildAt(i);
                if (child.getFile().equals(file)) {
                    return child;
                }
            }
        }
        return null;
    }

    private void addDummyNode(FileTreeNode node) {
        // This must be on the EDT
        SwingUtilities.invokeLater(() -> {
            if (node.getFile().isDirectory()) {
                File[] files = node.getFile().listFiles(f -> f.isDirectory() && !f.getName().startsWith("."));
                if (files != null && files.length > 0) {
                    node.add(new DefaultMutableTreeNode("Loading..."));
                }
            }
        });
    }

    private void loadChildrenInBackground(FileTreeNode node) {
        new SwingWorker<List<FileTreeNode>, Void>() {
            @Override
            protected List<FileTreeNode> doInBackground() {
                // 1. Perform slow file I/O on the background thread
                File[] files = node.getFile().listFiles(f -> f.isDirectory() && !f.getName().startsWith("."));
                List<FileTreeNode> children = new ArrayList<>();
                if (files != null) {
                    Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
                    for (File f : files) {
                        children.add(new FileTreeNode(f));
                    }
                }
                return children;
            }

            @Override
            protected void done() {
                try {
                    // 2. Update the UI on the Event Dispatch Thread (EDT)
                    List<FileTreeNode> children = get();
                    node.removeAllChildren(); // Now this is safe
                    for (FileTreeNode child : children) {
                        addDummyNode(child); // Prepare the next level of nodes
                        node.add(child);
                    }
                    node.setLoaded(true);
                    treeModel.reload(node); // Safely reload the model
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}
