package org.photoshelf;

import java.io.File;
import java.util.*;

public class DuplicateImageFinder {

    private final SimilarImageFinder similarImageFinder = new SimilarImageFinder();

    public Set<File> findDuplicates(List<File> files) {
        Set<File> duplicateFiles = new HashSet<>();
        Map<String, List<File>> filesBySignature = new HashMap<>();

        for (File file : files) {
            String signature = createFileSignature(file);
            if (signature != null) {
                filesBySignature.computeIfAbsent(signature, k -> new ArrayList<>()).add(file);
            }
        }

        for (List<File> group : filesBySignature.values()) {
            if (group.size() > 1) {
                duplicateFiles.addAll(group);
            }
        }
        return duplicateFiles;
    }

    public Map<File, List<File>> findSimilarImages(List<File> files, int threshold) {
        return similarImageFinder.findSimilarImages(files, threshold);
    }

    private String createFileSignature(File file) {
        try {
            String name = file.getName().toLowerCase();
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                String ext = name.substring(lastDot + 1);
                long size = file.length();
                return size + ":" + ext;
            }
        } catch (Exception e) {
            System.err.println("Could not create signature for file: " + file.getAbsolutePath());
        }
        return null;
    }
}
