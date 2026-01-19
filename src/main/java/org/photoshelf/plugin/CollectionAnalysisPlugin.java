package org.photoshelf.plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * A plugin that analyzes a collection of images.
 * Examples: Duplicate Finder, Face Clustering.
 */
public interface CollectionAnalysisPlugin<T> extends PhotoShelfPlugin {
    
    /**
     * Analyze a list of files.
     * @param files The list of files to analyze.
     * @return The result of the analysis.
     */
    T analyze(List<File> files);
}
