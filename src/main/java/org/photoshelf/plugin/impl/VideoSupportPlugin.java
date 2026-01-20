package org.photoshelf.plugin.impl;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.photoshelf.plugin.ImageProcessorPlugin;
import org.photoshelf.plugin.PreviewProviderPlugin;
import org.photoshelf.plugin.ThumbnailProviderPlugin;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class VideoSupportPlugin implements ImageProcessorPlugin, ThumbnailProviderPlugin, PreviewProviderPlugin {

    @Override
    public String getName() {
        return "Video Support";
    }

    @Override
    public String getDescription() {
        return "Provides basic support for video files (MP4) including thumbnails.";
    }

    @Override
    public void onEnable() {
        System.out.println("Video Support Plugin Enabled");
    }

    @Override
    public void process(File imageFile) {
        // No-op for now
    }

    @Override
    public boolean supports(File imageFile) {
        return isMp4(imageFile);
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("mp4");
    }

    @Override
    public BufferedImage getThumbnail(File file) throws IOException {
        try {
            // Grab a frame at 1 second mark (or 0 if video is short)
            Picture picture = FrameGrab.getFrameFromFile(file, 1);
            return AWTUtil.toBufferedImage(picture);
        } catch (Exception e) {
            System.err.println("JCodec failed to grab frame at 1s for " + file.getName() + ": " + e.getMessage());
            // Fallback to frame 0 if seeking failed
            try {
                Picture picture = FrameGrab.getFrameFromFile(file, 0);
                return AWTUtil.toBufferedImage(picture);
            } catch (Exception ex) {
                System.err.println("JCodec failed to grab frame 0 for " + file.getName() + ": " + ex.getMessage());
                throw new IOException("Failed to grab frame from video", ex);
            }
        }
    }

    @Override
    public boolean supportsThumbnail(File file) {
        return isMp4(file);
    }

    @Override
    public Image getPreviewImage(File file) throws IOException {
        // For now, return the thumbnail as preview
        return getThumbnail(file);
    }

    @Override
    public boolean supportsPreview(File file) {
        return isMp4(file);
    }

    private boolean isMp4(File file) {
        return file.getName().toLowerCase().endsWith(".mp4");
    }
}
