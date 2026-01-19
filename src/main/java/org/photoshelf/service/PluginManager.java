package org.photoshelf.service;

import org.photoshelf.plugin.CollectionAnalysisPlugin;
import org.photoshelf.plugin.ImageProcessorPlugin;
import org.photoshelf.plugin.PhotoShelfPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class PluginManager {
    private static PluginManager instance;
    private final List<ImageProcessorPlugin> imageProcessors = new ArrayList<>();
    private final List<CollectionAnalysisPlugin<?>> analysisPlugins = new ArrayList<>();

    private PluginManager() {
        // In a real modular app, we would use ServiceLoader here.
        // For now, we will manually register built-in plugins.
    }

    public static synchronized PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public void registerPlugin(PhotoShelfPlugin plugin) {
        plugin.onEnable();
        if (plugin instanceof ImageProcessorPlugin) {
            imageProcessors.add((ImageProcessorPlugin) plugin);
        }
        if (plugin instanceof CollectionAnalysisPlugin) {
            analysisPlugins.add((CollectionAnalysisPlugin<?>) plugin);
        }
        System.out.println("Registered plugin: " + plugin.getName());
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
    
    public void shutdown() {
        for (ImageProcessorPlugin p : imageProcessors) p.onDisable();
        for (CollectionAnalysisPlugin<?> p : analysisPlugins) p.onDisable();
    }
}
