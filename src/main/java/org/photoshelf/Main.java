package org.photoshelf;

import com.formdev.flatlaf.FlatDarkLaf;
import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;

import javax.imageio.spi.IIORegistry;

public class Main {
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--cleanup"))
            CleanupTool.cleanUp(args);
        FlatDarkLaf.setup();
        IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
        javax.swing.SwingUtilities.invokeLater(() -> new PhotoShelfUI().setVisible(true));
    }
}