package org.photoshelf.service;

import org.photoshelf.plugin.*;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class PluginManager {
    private static PluginManager instance;
    private final List<PhotoShelfPlugin> allPlugins = new ArrayList<>();
    private final Map<String, Boolean> pluginStates = new HashMap<>();
    private final File configFile = new File(System.getProperty("user.home"), ".photoshelf_cache/plugins.properties");

    private final List<ImageProcessorPlugin> imageProcessors = new ArrayList<>();
    private final List<CollectionAnalysisPlugin<?>> analysisPlugins = new ArrayList<>();
    private final List<ThumbnailProviderPlugin> thumbnailProviders = new ArrayList<>();
    private final List<PreviewProviderPlugin> previewProviders = new ArrayList<>();
    private final List<UserInterfacePlugin> uiPlugins = new ArrayList<>();
    private final List<PluginStateListener> listeners = new ArrayList<>();

    private PluginManager() {
        loadPluginStates();
        loadPlugins();
    }

    public static synchronized PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    private void loadPluginStates() {
        Properties props = new Properties();
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
                for (String key : props.stringPropertyNames()) {
                    pluginStates.put(key, Boolean.parseBoolean(props.getProperty(key)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void savePluginStates() {
        Properties props = new Properties();
        for (Map.Entry<String, Boolean> entry : pluginStates.entrySet()) {
            props.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
        }
        try {
            configFile.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                props.store(out, "PhotoShelf Plugin States");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlugins() {
        ServiceLoader<PhotoShelfPlugin> loader = ServiceLoader.load(PhotoShelfPlugin.class);
        Iterator<PhotoShelfPlugin> iterator = loader.iterator();
        while (iterator.hasNext()) {
            try {
                PhotoShelfPlugin plugin = iterator.next();
                allPlugins.add(plugin);
                
                String className = plugin.getClass().getName();
                boolean isActive = pluginStates.getOrDefault(className, true);
                pluginStates.putIfAbsent(className, true);
                
                if (isActive) {
                    registerPlugin(plugin);
                }
            } catch (Throwable t) {
                System.err.println("Failed to load a plugin: " + t.getMessage());
                t.printStackTrace();
            }
        }
        savePluginStates();
    }

    public void setPluginActive(PhotoShelfPlugin plugin, boolean active) {
        String className = plugin.getClass().getName();
        if (pluginStates.getOrDefault(className, true) == active) return;

        pluginStates.put(className, active);
        if (active) {
            registerPlugin(plugin);
        } else {
            unregisterPlugin(plugin);
        }
        savePluginStates();
        firePluginStateChanged(plugin, active);
    }

    public boolean isPluginActive(PhotoShelfPlugin plugin) {
        return pluginStates.getOrDefault(plugin.getClass().getName(), true);
    }

    public List<PhotoShelfPlugin> getAllPlugins() {
        return new ArrayList<>(allPlugins);
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
            if (plugin instanceof UserInterfacePlugin) {
                uiPlugins.add((UserInterfacePlugin) plugin);
            }
            System.out.println("Registered plugin: " + plugin.getName());
        } catch (Exception e) {
            System.err.println("Failed to enable plugin " + plugin.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unregisterPlugin(PhotoShelfPlugin plugin) {
        try {
            plugin.onDisable();
            if (plugin instanceof ImageProcessorPlugin) {
                imageProcessors.remove(plugin);
            }
            if (plugin instanceof CollectionAnalysisPlugin) {
                analysisPlugins.remove(plugin);
            }
            if (plugin instanceof ThumbnailProviderPlugin) {
                thumbnailProviders.remove(plugin);
            }
            if (plugin instanceof PreviewProviderPlugin) {
                previewProviders.remove(plugin);
            }
            if (plugin instanceof UserInterfacePlugin) {
                uiPlugins.remove(plugin);
            }
            System.out.println("Unregistered plugin: " + plugin.getName());
        } catch (Exception e) {
            System.err.println("Failed to disable plugin " + plugin.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<ImageProcessorPlugin> getImageProcessors() {
        return new ArrayList<>(imageProcessors);
    }

    public List<CollectionAnalysisPlugin<?>> getAnalysisPlugins() {
        return new ArrayList<>(analysisPlugins);
    }
    
    public List<UserInterfacePlugin> getUiPlugins() {
        return new ArrayList<>(uiPlugins);
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
        for (PhotoShelfPlugin plugin : allPlugins) {
            if (plugin instanceof ImageProcessorPlugin && isPluginActive(plugin)) {
                extensions.addAll(((ImageProcessorPlugin) plugin).getSupportedExtensions());
            }
        }
        return extensions;
    }
    
    public void shutdown() {
        for (ImageProcessorPlugin p : imageProcessors) p.onDisable();
        for (CollectionAnalysisPlugin<?> p : analysisPlugins) p.onDisable();
        for (UserInterfacePlugin p : uiPlugins) p.onDisable();
        savePluginStates();
    }
    
    public void addPluginStateListener(PluginStateListener listener) {
        listeners.add(listener);
    }
    
    public void removePluginStateListener(PluginStateListener listener) {
        listeners.remove(listener);
    }
    
    private void firePluginStateChanged(PhotoShelfPlugin plugin, boolean active) {
        for (PluginStateListener listener : listeners) {
            listener.onPluginStateChanged(plugin, active);
        }
    }
}
