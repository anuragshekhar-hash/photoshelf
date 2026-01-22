package org.photoshelf.service;

import org.photoshelf.plugin.PhotoShelfPlugin;

public interface PluginStateListener {
    void onPluginStateChanged(PhotoShelfPlugin plugin, boolean active);
}
