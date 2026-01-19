package org.photoshelf.plugin.impl;

import org.photoshelf.ImageSupportChecker;
import org.photoshelf.PHashCacheManager;
import org.photoshelf.plugin.ImageProcessorPlugin;

import java.io.File;
import java.io.IOException;

public class PHashPlugin implements ImageProcessorPlugin {
    private final PHashCacheManager cacheManager;

    public PHashPlugin() {
        this.cacheManager = new PHashCacheManager();
    }

    @Override
    public String getName() {
        return "pHash Calculator";
    }

    @Override
    public String getDescription() {
        return "Calculates perceptual hashes for images to detect duplicates.";
    }

    @Override
    public void process(File imageFile) {
        try {
            // This calculates and caches the hash
            cacheManager.getHash(imageFile);
        } catch (IOException e) {
            System.err.println("Failed to calculate pHash for " + imageFile.getName());
        }
    }

    @Override
    public boolean supports(File imageFile) {
        return ImageSupportChecker.isImage(imageFile);
    }
}
