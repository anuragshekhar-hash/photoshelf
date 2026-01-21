package org.photoshelf.service;

import org.photoshelf.plugin.*;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

public class PluginManager {
    private static PluginManager instance;
    private final List<ImageProcessorPlugin> imageProcessors = new ArrayList<>();
    private final List<CollectionAnalysisPlugin<?>> analysisPlugins = new ArrayList<>();
    private final List<ThumbnailProviderPlugin> thumbnailProviders = new ArrayList<>();
    private final List<PreviewProviderPlugin> previewProviders = new ArrayList<>();

    private PluginManager() {
        loadPlugins();
    }

    public static synchronized PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    private void loadPlugins() {
        ServiceLoader<PhotoShelfPlugin> loader = ServiceLoader.load(PhotoShelfPlugin.class);
        Iterator<PhotoShelfPlugin> iterator = loader.iterator();
        while (iterator.hasNext()) {
            try {
                PhotoShelfPlugin plugin = iterator.next();
                registerPlugin(plugin);
            } catch (Throwable t) {
                System.err.println("Failed to load a plugin: " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    public void registerPlugin(PhotoShelfPlugin plugin) {
        try {
            plugin.onEnable();
            if (plugin instanceof ImageProcessorPlugin) {
                imageProcessors.add((ImageProcessorPlugin) plugin);
            }
            if (plugin instanceof CollectionAnalysisPlugin) {
                analysisPlugins.add((CollectionAnalysisPlugin<?>) plugin);
            }
            if (plugin instanceof ThumbnailProviderPlugin) {
                thumbnailProviders.add((ThumbnailProviderPlugin) plugin);
            }
            if (plugin instanceof PreviewProviderPlugin) {
                previewProviders.add((PreviewProviderPlugin) plugin);
            }
            System.out.println("Registered plugin: " + plugin.getName());
        } catch (Exception e) {
            System.err.println("Failed to enable plugin " + plugin.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<ImageProcessorPlugin> getImageProcessors() {
        return new ArrayList<>(imageProcessors);
    }

    public List<CollectionAnalysisPlugin<?>> getAnalysisPlugins() {
        return new ArrayList<>(analysisPlugins);
    }

    public void processImage(File file) {
        for (ImageProcessorPlugin processor : imageProcessors) {
            if (processor.supports(file)) {
                try {
                    processor.process(file);
                } catch (Exception e) {
                    System.err.println("Error in plugin " + processor.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    public BufferedImage getThumbnail(File file) throws IOException {
        for (ThumbnailProviderPlugin provider : thumbnailProviders) {
            if (provider.supportsThumbnail(file)) {
                return provider.getThumbnail(file);
            }
        }
        return null;
    }

    public Image getPreviewImage(File file) throws IOException {
        for (PreviewProviderPlugin provider : previewProviders) {
            if (provider.supportsPreview(file)) {
                return provider.getPreviewImage(file);
            }
        }
        return null;
    }
    
    public Set<String> getAllSupportedExtensions() {
        Set<String> extensions = new HashSet<>();
        // Default image extensions
        extensions.add("jpg");
        extensions.add("jpeg");
        extensions.add("png");
        extensions.add("gif");
        extensions.add("bmp");
        extensions.add("webp");
        // Add video extensions explicitly as fallbacks
        extensions.add("mp4");
        extensions.add("webm");
        extensions.add("avif");
        
        for (ImageProcessorPlugin plugin : imageProcessors) {
            extensions.addAll(plugin.getSupportedExtensions());
        }
        return extensions;
    }
    
    public void shutdown() {
        for (ImageProcessorPlugin p : imageProcessors) p.onDisable();
        for (CollectionAnalysisPlugin<?> p : analysisPlugins) p.onDisable();
    }
}
