package org.photoshelf.plugin;

import java.io.File;

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
}
