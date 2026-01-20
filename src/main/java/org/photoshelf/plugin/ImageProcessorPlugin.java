package org.photoshelf.plugin;

import java.io.File;
import java.util.List;

/**
 * A plugin that processes individual images.
 * Examples: pHash calculator, Face Detector, EXIF reader.
 */
public interface ImageProcessorPlugin extends PhotoShelfPlugin {
    
    /**
     * Process a single image file.
     * @param imageFile The image file to process.
     */
    void process(File imageFile);
    
    /**
     * Checks if this plugin supports the given file type.
     * @param imageFile The file to check.
     * @return true if supported.
     */
    boolean supports(File imageFile);

    /**
     * Returns a list of file extensions supported by this plugin.
     * @return List of extensions (e.g., "jpg", "png") without the dot.
     */
    default List<String> getSupportedExtensions() {
        return List.of();
    }
}
