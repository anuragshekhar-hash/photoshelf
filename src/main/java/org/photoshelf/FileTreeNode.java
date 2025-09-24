package org.photoshelf;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class FileTreeNode extends DefaultMutableTreeNode {
    private boolean loaded = false;

    public FileTreeNode(File file) {
        super(file);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public File getFile() {
        return (File) getUserObject();
    }

    @Override
    public String toString() {
        File file = getFile();
        if (file.getParent() == null) {
            return file.getAbsolutePath();
        }
        return file.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileTreeNode that = (FileTreeNode) o;
        try {
            return Objects.equals(this.getFile().getCanonicalPath(), that.getFile().getCanonicalPath());
        } catch (IOException e) {
            return Objects.equals(this.getFile().getAbsolutePath(), that.getFile().getAbsolutePath());
        }
    }

    @Override
    public int hashCode() {
        try {
            return Objects.hash(getFile().getCanonicalPath());
        } catch (IOException e) {
            return Objects.hash(getFile().getAbsolutePath());
        }
    }
}
