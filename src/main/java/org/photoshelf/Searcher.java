package org.photoshelf;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Searcher extends SwingWorker<Void, File> {
    private final File searchRoot;
    private final PhotoShelfUI mainApp;
    private final List<String> supportedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private final AtomicInteger filesFound = new AtomicInteger(0);
    private final SearchParams searchParam;

    /*public Searcher(PhotoShelfUI mainApp, File searchRoot, String searchType, String searchQuery) {
        this.mainApp = mainApp;
        this.searchRoot = searchRoot;
        this.searchType = searchType;
        this.searchQuery = (searchQuery != null) ? searchQuery.toLowerCase() : "";
    }*/
    
    public Searcher(PhotoShelfUI mainAppm, File searchRoot, SearchParams params) {
        this.mainApp = mainAppm;
        this.searchRoot = searchRoot;
        this.searchParam = params;
    }

    @Override
    protected Void doInBackground() {
        searchDirectory(searchRoot);
        return null;
    }

    private void searchDirectory(File directory) {
        int count = 0;

        if (isCancelled()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (isCancelled()) {
                break;
            }

            if (file.isDirectory()) {
                if (!searchParam.isRecursive())
                    continue;;
                searchDirectory(file);
            } else {
                if (!isSupported(file.getName().toLowerCase())) {
                    continue;
                }
                boolean matches = true;
                if (matches && searchParam.hasSearchString()) {
                    matches = file.getName().toLowerCase().contains(searchParam.getSearchString());
                }
                if (matches && searchParam.hasKeyword()) {
                    Set<String> keywords = mainApp.getKeywordManager().getKeywords(file);
                    if (searchParam.isNoKeywords()) {
                        if (keywords.isEmpty()) {
                            matches = true;
                        }
                        else {
                            matches = false;
                        }
                    } else {
                        matches = searchParam.evaluate(keywords);                       }
                    }

                if (matches) {
                    count++;
                    filesFound.incrementAndGet();
                    publish(file);
                }
            }
        }
    }

    private boolean isSupported(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            String ext = fileName.substring(lastDot + 1);
            return supportedExtensions.contains(ext);
        }
        return false;
    }

    @Override
    protected void process(List<File> chunks) {
        for (File file : chunks) {
            if (isCancelled()) break;
            mainApp.addNewImage(file);
        }
        mainApp.updateTotalFile(filesFound.get());
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            mainApp.setSearchStatus("Found " + filesFound.get() + " matching files");
        }
    }
}
