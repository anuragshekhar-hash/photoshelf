package org.photoshelf;

public class CleanupTool {
    public static void cleanUp(String[] args) {
        System.out.println("Starting cleanup...");

        KeywordManager keywordManager = new KeywordManager();
        HybridCache<String, ?> thumbnailCache = new HybridCache<>("thumbnails", 1);

        int cleanedKeywords = keywordManager.cleanup();
        int cleanedCache = thumbnailCache.cleanup();

        System.out.println("Cleanup complete.");
        System.out.println("Removed " + cleanedKeywords + " orphaned keyword entries.");
        System.out.println("Removed " + cleanedCache + " orphaned cache entries.");

        keywordManager.shutdown();
        thumbnailCache.shutdown();
    }
}
