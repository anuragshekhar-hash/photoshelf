package org.photoshelf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimilarImageFinder {

    private final PHash pHash = new PHash();
    private static final int DEFAULT_THRESHOLD = 5;

    public Map<File, List<File>> findSimilarImages(List<File> files) {
        return findSimilarImages(files, DEFAULT_THRESHOLD);
    }

    public Map<File, List<File>> findSimilarImages(List<File> files, int threshold) {
        Map<File, String> hashes = new HashMap<>();
        for (File file : files) {
            try {
                hashes.put(file, PHash.getHash(file));
            } catch (IOException e) {
                // Ignore images that can't be processed
            }
        }

        Map<File, List<File>> similarImages = new HashMap<>();
        List<File> fileList = new ArrayList<>(files);

        for (int i = 0; i < fileList.size(); i++) {
            for (int j = i + 1; j < fileList.size(); j++) {
                File file1 = fileList.get(i);
                File file2 = fileList.get(j);

                String hash1 = hashes.get(file1);
                String hash2 = hashes.get(file2);

                if (hash1 != null && hash2 != null) {
                    if (PHash.distance(hash1, hash2) <= threshold) {
                        similarImages.computeIfAbsent(file1, k -> new ArrayList<>()).add(file2);
                        similarImages.computeIfAbsent(file2, k -> new ArrayList<>()).add(file1);
                    }
                }
            }
        }
        return similarImages;
    }
}
