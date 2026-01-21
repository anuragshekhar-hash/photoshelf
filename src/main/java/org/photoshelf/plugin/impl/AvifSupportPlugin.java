package org.photoshelf.plugin.impl;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.photoshelf.plugin.ImageProcessorPlugin;
import org.photoshelf.plugin.PreviewProviderPlugin;
import org.photoshelf.plugin.ThumbnailProviderPlugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AvifSupportPlugin implements ImageProcessorPlugin, ThumbnailProviderPlugin, PreviewProviderPlugin {

    @Override
    public String getName() {
        return "AVIF Support";
    }

    @Override
    public String getDescription() {
        return "Provides support for AVIF image files using JavaCV (FFmpeg).";
    }

    @Override
    public void onEnable() {
        System.out.println("AVIF Support Plugin Enabled");
    }

    @Override
    public void process(File imageFile) {
        // No-op for now
    }

    @Override
    public boolean supports(File imageFile) {
        return isAvif(imageFile);
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("avif");
    }

    @Override
    public BufferedImage getThumbnail(File file) throws IOException {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
            grabber.start();
            
            Frame frame = grabber.grabImage(); // AVIF is an image, so grab the first (and likely only) frame
            
            if (frame != null) {
                return convertFrameToImageSafe(frame);
            } else {
                grabber.stop();
                throw new IOException("Failed to grab frame from AVIF file");
            }
        } catch (Exception e) {
            System.err.println("JavaCV failed for AVIF " + file.getName() + ": " + e.getMessage());
            return createPlaceholder(file);
        }
    }

    private BufferedImage convertFrameToImageSafe(Frame frame) throws IOException {
        File tempFile = File.createTempFile("javacv_avif_frame", ".png");
        try {
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempFile, frame.imageWidth, frame.imageHeight)) {
                recorder.setFormat("png");
                recorder.start();
                recorder.record(frame);
                recorder.stop();
            }
            return ImageIO.read(tempFile);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private BufferedImage createPlaceholder(File file) {
        int width = 200;
        int height = 200;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "AVIF";
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, x, y);
        
        g2d.dispose();
        return image;
    }

    @Override
    public boolean supportsThumbnail(File file) {
        return isAvif(file);
    }

    @Override
    public Image getPreviewImage(File file) throws IOException {
        return getThumbnail(file);
    }

    @Override
    public boolean supportsPreview(File file) {
        return isAvif(file);
    }

    private boolean isAvif(File file) {
        return file.getName().toLowerCase().endsWith(".avif");
    }
}
