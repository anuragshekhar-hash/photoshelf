package org.photoshelf.plugin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * A plugin that can provide a thumbnail image for a file.
 */
public interface ThumbnailProviderPlugin extends PhotoShelfPlugin {
    
    /**
     * Generates or retrieves a thumbnail for the given file.
     * @param file The file to generate a thumbnail for.
     * @return A BufferedImage containing the thumbnail, or null if generation failed.
     * @throws IOException If an I/O error occurs.
     */
    BufferedImage getThumbnail(File file) throws IOException;
    
    /**
     * Checks if this plugin supports generating thumbnails for the given file.
     * @param file The file to check.
     * @return true if supported.
     */
    boolean supportsThumbnail(File file);
}
