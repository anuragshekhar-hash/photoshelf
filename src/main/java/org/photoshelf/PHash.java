package org.photoshelf;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;

public class PHash {

    public static String getHash(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Unsupported image format");
        }
        return getHash(img);
    }

    public static String getHash(BufferedImage img) {
        // 1. Resize to 8x8
        BufferedImage resizedImg = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = resizedImg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(img, 0, 0, 8, 8, null);
        g2d.dispose();

        // 2. Calculate average pixel value
        long sum = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                sum += resizedImg.getRGB(x, y) & 0xFF;
            }
        }
        long avg = sum / 64;

        // 3. Generate hash
        StringBuilder hash = new StringBuilder();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if ((resizedImg.getRGB(x, y) & 0xFF) >= avg) {
                    hash.append('1');
                } else {
                    hash.append('0');
                }
            }
        }
        return hash.toString();
    }

    public static int distance(String hash1, String hash2) {
        if (hash1.length() != hash2.length()) {
            throw new IllegalArgumentException("Hashes must be of equal length");
        }
        int distance = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }
}
