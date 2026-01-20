package org.photoshelf.plugin.impl;

import org.photoshelf.plugin.ImageProcessorPlugin;
import org.photoshelf.plugin.PreviewProviderPlugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DefaultImagePlugin implements ImageProcessorPlugin, PreviewProviderPlugin {
    private static final List<String> EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");

    @Override
    public String getName() {
        return "Default Image Support";
    }

    @Override
    public String getDescription() {
        return "Provides support for standard image formats.";
    }

    @Override
    public void process(File imageFile) {
        // No-op
    }

    @Override
    public boolean supports(File imageFile) {
        return isSupported(imageFile);
    }

    @Override
    public List<String> getSupportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public Image getPreviewImage(File file) throws IOException {
        if (file.getName().toLowerCase().endsWith(".gif")) {
            return Toolkit.getDefaultToolkit().createImage(file.getAbsolutePath());
        } else {
            return ImageIO.read(file);
        }
    }

    @Override
    public boolean supportsPreview(File file) {
        return isSupported(file);
    }

    private boolean isSupported(File file) {
        String name = file.getName().toLowerCase();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) return false;
        return EXTENSIONS.contains(name.substring(lastDot + 1));
    }
}
