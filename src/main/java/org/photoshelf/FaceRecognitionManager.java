package org.photoshelf;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javax.swing.*;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class FaceRecognitionManager {
    private static boolean isInitialized = false;
    private Net faceDetector;
    private Net faceRecognizer;
    private CascadeClassifier haarDetector;
    private boolean useDeepLearning = false;
    private final FaceEmbeddingCacheManager cacheManager;

    // Paths to models in the cache directory
    private final File modelsDir;
    private final File protoFile;
    private final File modelFile;
    private final File embeddingModel;
    private final File haarCascadeFile;

    // URLs for downloading models if missing
    private static final String PROTO_URL = "https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/deploy.prototxt";
    private static final String MODEL_URL = "https://raw.githubusercontent.com/opencv/opencv_3rdparty/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000.caffemodel";
    private static final String EMBEDDING_URL = "https://github.com/pyannote/pyannote-data/raw/master/openface.nn4.small2.v1.t7";
    private static final String HAAR_URL = "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml";

    public FaceRecognitionManager() {
        String userHome = System.getProperty("user.home");
        modelsDir = new File(userHome, ".photoshelf_cache/models");
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }

        protoFile = new File(modelsDir, "deploy.prototxt");
        modelFile = new File(modelsDir, "res10_300x300_ssd_iter_140000.caffemodel");
        embeddingModel = new File(modelsDir, "openface_nn4.small2.v1.t7");
        haarCascadeFile = new File(modelsDir, "haarcascade_frontalface_default.xml");

        cacheManager = new FaceEmbeddingCacheManager();
        initialize();
    }

    private void initialize() {
        if (isInitialized) return;

        try {
            OpenCV.loadLocally();
            isInitialized = true;
            System.out.println("OpenCV loaded successfully.");

            // Ensure models are present (Extract from JAR or Download)
            ensureModelExists(protoFile, "deploy.prototxt", PROTO_URL);
            ensureModelExists(modelFile, "res10_300x300_ssd_iter_140000.caffemodel", MODEL_URL);
            ensureModelExists(embeddingModel, "openface_nn4.small2.v1.t7", EMBEDDING_URL);
            ensureModelExists(haarCascadeFile, "haarcascade_frontalface_default.xml", HAAR_URL);

            // Try to load Deep Learning models
            if (protoFile.exists() && modelFile.exists() && embeddingModel.exists()) {
                if (embeddingModel.length() < 1000000) {
                    System.err.println("Embedding model file seems too small (" + embeddingModel.length() + " bytes). Skipping DL initialization.");
                } else {
                    faceDetector = Dnn.readNetFromCaffe(protoFile.getAbsolutePath(), modelFile.getAbsolutePath());
                    faceRecognizer = Dnn.readNetFromTorch(embeddingModel.getAbsolutePath());
                    useDeepLearning = true;
                    System.out.println("Deep Learning models loaded.");
                }
            } else {
                // Fallback to Haar Cascade
                if (haarCascadeFile.exists()) {
                    haarDetector = new CascadeClassifier(haarCascadeFile.getAbsolutePath());
                    System.out.println("Haar Cascade loaded (Fallback mode).");
                } else {
                    System.err.println("No face detection models found.");
                }
            }

        } catch (Exception e) {
            System.err.println("Error initializing OpenCV: " + e.getMessage());
            
            if (!useDeepLearning && haarCascadeFile.exists()) {
                try {
                    haarDetector = new CascadeClassifier(haarCascadeFile.getAbsolutePath());
                    System.out.println("Recovered from DL error: Haar Cascade loaded.");
                } catch (Exception ex) {
                    System.err.println("Failed to load Haar Cascade fallback: " + ex.getMessage());
                }
            }
        }
    }

    private void ensureModelExists(File destination, String resourceName, String downloadUrl) {
        if (destination.exists() && destination.length() > 0) return;

        System.out.println("Model missing: " + destination.getName() + ". Attempting to retrieve...");

        // 1. Try to extract from JAR resources
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("models/" + resourceName)) {
            if (is != null) {
                Files.copy(is, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Extracted model from JAR: " + resourceName);
                return;
            }
        } catch (IOException e) {
            System.err.println("Failed to extract model from JAR: " + e.getMessage());
        }

        // 2. If not in JAR, download from URL
        System.out.println("Downloading model from: " + downloadUrl);
        try {
            URL url = new URL(downloadUrl);
            try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                 FileOutputStream fos = new FileOutputStream(destination)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                System.out.println("Downloaded model: " + destination.getName());
            }
        } catch (IOException e) {
            System.err.println("Failed to download model: " + e.getMessage());
            // Clean up partial file
            if (destination.exists()) {
                destination.delete();
            }
        }
    }

    public boolean isReady() {
        return isInitialized && (faceDetector != null || haarDetector != null);
    }

    public synchronized Mat getFaceEmbedding(File imageFile) {
        if (!isReady()) return null;

        float[] cachedEmbedding = cacheManager.getEmbedding(imageFile);
        if (cachedEmbedding != null) {
            Mat mat = new Mat(1, cachedEmbedding.length, CvType.CV_32F);
            mat.put(0, 0, cachedEmbedding);
            return mat;
        }

        Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
        if (image.empty()) return null;

        Rect faceRect = detectFace(image);
        if (faceRect == null) return null;

        Mat embedding = null;
        if (useDeepLearning && faceRecognizer != null) {
            Mat faceRoi = new Mat(image, faceRect);
            Mat blob = Dnn.blobFromImage(faceRoi, 1.0/255, new Size(96, 96), new Scalar(0,0,0), true, false);
            faceRecognizer.setInput(blob);
            embedding = faceRecognizer.forward().clone();
        } else {
            Mat faceRoi = new Mat(image, faceRect);
            Mat hsv = new Mat();
            Imgproc.cvtColor(faceRoi, hsv, Imgproc.COLOR_BGR2HSV);
            Mat hist = new Mat();
            Imgproc.calcHist(List.of(hsv), new MatOfInt(0, 1), new Mat(), hist, new MatOfInt(50, 60), new MatOfFloat(0, 180, 0, 256));
            Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
            embedding = hist;
        }

        if (embedding != null) {
            float[] data = new float[(int) embedding.total()];
            embedding.get(0, 0, data);
            cacheManager.putEmbedding(imageFile, data);
        }

        return embedding;
    }

    private synchronized Rect detectFace(Mat image) {
        if (useDeepLearning) {
            Mat blob = Dnn.blobFromImage(image, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0), false, false);
            faceDetector.setInput(blob);
            Mat detections = faceDetector.forward();
            Mat detectionMat = detections.reshape(1, (int)detections.total() / 7);
            
            float maxConfidence = 0;
            Rect bestFace = null;

            for (int i = 0; i < detectionMat.rows(); i++) {
                double confidence = detectionMat.get(i, 2)[0];
                if (confidence > 0.5) {
                    if (confidence > maxConfidence) {
                        maxConfidence = (float) confidence;
                        int x1 = (int) (detectionMat.get(i, 3)[0] * image.cols());
                        int y1 = (int) (detectionMat.get(i, 4)[0] * image.rows());
                        int x2 = (int) (detectionMat.get(i, 5)[0] * image.cols());
                        int y2 = (int) (detectionMat.get(i, 6)[0] * image.rows());
                        
                        x1 = Math.max(0, x1); y1 = Math.max(0, y1);
                        x2 = Math.min(image.cols(), x2); y2 = Math.min(image.rows(), y2);
                        
                        if (x2 > x1 && y2 > y1) {
                            bestFace = new Rect(x1, y1, x2 - x1, y2 - y1);
                        }
                    }
                }
            }
            return bestFace;
        } else if (haarDetector != null) {
            MatOfRect faceDetections = new MatOfRect();
            haarDetector.detectMultiScale(image, faceDetections);
            Rect[] faces = faceDetections.toArray();
            if (faces.length > 0) {
                Rect largest = faces[0];
                for (Rect r : faces) {
                    if (r.area() > largest.area()) largest = r;
                }
                return largest;
            }
        }
        return null;
    }

    public double calculateSimilarity(Mat embedding1, Mat embedding2) {
        if (embedding1 == null || embedding2 == null) return 0;

        if (useDeepLearning) {
            double dist = Core.norm(embedding1, embedding2, Core.NORM_L2);
            return 1.0 / (1.0 + dist); 
        } else {
            return Imgproc.compareHist(embedding1, embedding2, Imgproc.CV_COMP_CORREL);
        }
    }

    public double calculateSimilarity(float[] emb1, float[] emb2) {
        if (emb1 == null || emb2 == null || emb1.length != emb2.length) return 0;

        if (useDeepLearning) {
            // Euclidean distance
            double sumSq = 0;
            for (int i = 0; i < emb1.length; i++) {
                double diff = emb1[i] - emb2[i];
                sumSq += diff * diff;
            }
            double dist = Math.sqrt(sumSq);
            return 1.0 / (1.0 + dist);
        } else {
            // Fallback to Mat conversion for histogram comparison (complex logic)
            Mat m1 = new Mat(1, emb1.length, CvType.CV_32F);
            m1.put(0, 0, emb1);
            Mat m2 = new Mat(1, emb2.length, CvType.CV_32F);
            m2.put(0, 0, emb2);
            return Imgproc.compareHist(m1, m2, Imgproc.CV_COMP_CORREL);
        }
    }
    
    public void showMissingModelAlert(Component parent) {
        String message = "Facial Recognition models are missing.\n" +
                "The application attempted to extract them from resources and download them, but failed.\n" +
                "Please ensure you have an internet connection and restart the application.\n" +
                "Models directory: " + modelsDir.getAbsolutePath();
        JOptionPane.showMessageDialog(parent, message, "Missing AI Models", JOptionPane.WARNING_MESSAGE);
    }

    public void saveCache() {
        cacheManager.saveCache();
    }
    
    public boolean hasEmbedding(File file) {
        return cacheManager.hasEmbedding(file);
    }

    public Map<String, float[]> getAllCachedEmbeddings() {
        return cacheManager.getAllEmbeddings();
    }
}
