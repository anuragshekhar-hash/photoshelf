package org.photoshelf;

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;

import javax.imageio.spi.IIORegistry;

public class Main {
    public static void main(String[] args) {
        IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
        javax.swing.SwingUtilities.invokeLater(() -> new PhotoShelfUI().setVisible(true));
    }
}