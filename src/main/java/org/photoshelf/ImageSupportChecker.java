package org.photoshelf;

import javax.imageio.ImageIO;
import java.util.Arrays;

public class ImageSupportChecker {
    /**
     * Checks if the current Java environment has a registered ImageIO plugin
     * that can read AVIF files.
     *
     * @return true if AVIF support is detected, false otherwise.
     */
    public static boolean isAvifSupported() {
        // ImageIO maintains a list of registered plugins. We can query it for the
        // file suffixes (extensions) it supports.
        String[] supportedReadFormats = ImageIO.getReaderFileSuffixes();
        return Arrays.asList(supportedReadFormats).contains("avif");
    }
}
