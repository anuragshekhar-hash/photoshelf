package org.photoshelf.plugin;

import java.awt.Image;
import java.io.File;
import java.io.IOException;

/**
 * A plugin that provides a full-size preview image for a file.
 * This supports animated images (GIFs) by returning java.awt.Image.
 */
public interface PreviewProviderPlugin extends PhotoShelfPlugin {

    /**
     * Loads the image for preview.
     * @param file The file to load.
     * @return The Image object (BufferedImage or Toolkit Image).
     * @throws IOException If loading fails.
     */
    Image getPreviewImage(File file) throws IOException;

    /**
     * Checks if this plugin supports previewing the given file.
     * @param file The file to check.
     * @return true if supported.
     */
    boolean supportsPreview(File file);
}
