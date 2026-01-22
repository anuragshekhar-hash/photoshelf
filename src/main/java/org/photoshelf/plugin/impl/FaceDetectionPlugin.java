package org.photoshelf.plugin.impl;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.photoshelf.PhotoShelfUI;
import org.photoshelf.PreviewPanelManager;
import org.photoshelf.plugin.UserInterfacePlugin;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption; // Corrected import
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

public class FaceDetectionPlugin implements UserInterfacePlugin {
    private PhotoShelfUI context;
    private CascadeClassifier classifier;
    private File cascadeTempFile;

    private static final String CASCADE_RESOURCE_PATH = "/cascades/haarcascade_frontalface_alt.xml";

    @Override
    public String getName() {
        return "Face Detection";
    }

    @Override
    public String getDescription() {
        return "Detects faces in the selected image.";
    }

    @Override
    public void onEnable() {
        try {
            URL cascadeURL = getClass().getResource(CASCADE_RESOURCE_PATH);
            if (cascadeURL == null) {
                System.err.println("FaceDetectionPlugin: Cascade resource not found: " + CASCADE_RESOURCE_PATH);
                return;
            }

            cascadeTempFile = Files.createTempFile("cascade_", ".xml").toFile();
            Files.copy(cascadeURL.openStream(), cascadeTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            cascadeTempFile.deleteOnExit();

            classifier = new CascadeClassifier(cascadeTempFile.getAbsolutePath());
            if (classifier.empty()) {
                System.err.println("FaceDetectionPlugin: Failed to load cascade classifier from " + cascadeTempFile.getAbsolutePath());
                cascadeTempFile.delete();
                cascadeTempFile = null;
            }
        } catch (Exception e) {
            System.err.println("FaceDetectionPlugin: Error during onEnable: " + e.getMessage());
            e.printStackTrace();
            if (cascadeTempFile != null) {
                cascadeTempFile.delete();
                cascadeTempFile = null;
            }
        }
    }

    @Override
    public void setContext(Object context) {
        if (context instanceof PhotoShelfUI) {
            this.context = (PhotoShelfUI) context;
        }
    }

    @Override
    public JMenuItem getMenuItem() {
        JMenuItem item = new JMenuItem("Detect Faces");
        item.addActionListener(e -> detectFaces());
        return item;
    }

    private void detectFaces() {
        if (context == null) return;
        
        if (classifier == null || classifier.empty()) {
            JOptionPane.showMessageDialog(context, "Face detection classifier is not loaded. Please check logs for errors.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PreviewPanelManager previewManager = context.getPreviewPanelManager();
        File file = previewManager.getCurrentFile();

        if (file == null) {
            JOptionPane.showMessageDialog(context, "No image is being previewed.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                Mat image = imread(file.getAbsolutePath());
                if (image.empty()) {
                    throw new IOException("Could not read image.");
                }
                
                Mat gray = new Mat();
                cvtColor(image, gray, COLOR_BGR2GRAY);
                equalizeHist(gray, gray);
                
                RectVector faces = new RectVector();
                classifier.detectMultiScale(gray, faces);
                
                if (faces.size() == 0) {
                    return null; // No faces found
                }
                
                for (long i = 0; i < faces.size(); i++) {
                    Rect face = faces.get(i);
                    rectangle(image, face, new Scalar(0, 255, 0, 0), 3, 8, 0);
                }
                
                File temp = File.createTempFile("face_detected", ".jpg");
                imwrite(temp.getAbsolutePath(), image);
                BufferedImage result = ImageIO.read(temp);
                temp.delete();
                return result;
            }
            
            @Override
            protected void done() {
                try {
                    BufferedImage result = get();
                    if (result != null) {
                        previewManager.setPreviewImage(result);
                    } else {
                        JOptionPane.showMessageDialog(context, "No faces detected.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(context, "Error detecting faces: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
