package org.photoshelf.plugin;

/**
 * Base interface for all PhotoShelf plugins.
 */
public interface PhotoShelfPlugin {
    /**
     * Returns the unique name of the plugin.
     * @return Plugin name
     */
    String getName();

    /**
     * Returns a description of what the plugin does.
     * @return Plugin description
     */
    String getDescription();

    /**
     * Called when the plugin is loaded.
     */
    default void onEnable() {}

    /**
     * Called when the plugin is unloaded or app shuts down.
     */
    default void onDisable() {}
}
