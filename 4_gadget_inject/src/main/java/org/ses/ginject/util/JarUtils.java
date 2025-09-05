package org.ses.ginject.util;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarUtils {

    private static final Logger logger = LogManager.getLogger(JarUtils.class.getName());

    public static void extractJar(String jarFilePath, String outputDir) throws IOException {
        File jarFile = new File(jarFilePath);
        File destDir = new File(outputDir);

        if (destDir.exists()) {
            FileUtils.deleteDirectory(destDir);
            logger.info("Removing contents of " + destDir);
        }
        destDir.mkdirs();

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                File entryFile = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    entryFile.getParentFile().mkdirs();
                    try (InputStream is = jar.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(entryFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
        logger.info("JAR extracted successfully to: " + outputDir);
    }

    public static void packJar(String outputJarPath, String inputDir) throws IOException {
        File sourceDir = new File(inputDir);
        try (FileOutputStream fos = new FileOutputStream(outputJarPath);
             JarOutputStream jos = new JarOutputStream(fos)) {

            addFilesToJar(sourceDir, sourceDir, jos);
        }
        logger.info("JAR repacked successfully: " + outputJarPath);
    }

    private static void addFilesToJar(File rootDir, File source, JarOutputStream jos) throws IOException {
        if (source.isDirectory()) {
            for (File file : source.listFiles()) {
                addFilesToJar(rootDir, file, jos);
            }
        } else {
            String entryName = rootDir.toURI().relativize(source.toURI()).getPath();
            jos.putNextEntry(new JarEntry(entryName));

            try (FileInputStream fis = new FileInputStream(source)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    jos.write(buffer, 0, bytesRead);
                }
            }
            jos.closeEntry();
        }
    }

}
