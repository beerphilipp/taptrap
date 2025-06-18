package com.tapjacking.maltapextract.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for file operations.
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    /**
     * Retrieves all files with the given extension in the given directory.
     * @param directory The directory to search in.
     * @param extension The extension of the files to search for.
     * @param recursive Whether to search recursively in subdirectories.
     * @return A list of files with the given extension. If the directory does not exist or is not a directory, null is returned.
     */
    public static List<File> getFileWithExtension(String directory, String extension, boolean recursive) {
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        List<File> filesWithExtension = new ArrayList<>();
        try (var paths = Files.walk(dir.toPath(), recursive ? Integer.MAX_VALUE : 1)) { // Try-with-resources
            paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(extension))
                    .forEach(path -> filesWithExtension.add(path.toFile()));
        } catch (IOException e) {
            logger.error("Error while searching for files with extension: {}", extension, e);
            return null;
        }
        return filesWithExtension;
    }

    /**
     * Retrieves the file with the given name and type in the given directory.
     * Example: Given name = "file", type = "anim", and tmpDir = "/tmp", the method will search for "/tmp/res/[anim|anim-*]/file.xml"
     *
     * @param name The name of the file (exluding the extension).
     * @param type The type of the file (e.g., "anim", "interpolator").
     * @param tmpDirectory The base directory to search in (e.g., "/tmp").
     * @return The file with the given name and type.
     */
    public static List<File> getXmlFilesForReference(String name, String type, String tmpDirectory) {
        File directory = new File(tmpDirectory);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + tmpDirectory);
        }
        String fileName = name + ".xml";

        File resDir = new File(directory, "res");
        if (!resDir.exists() || !resDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + resDir);
        }

        // get all the directories in the res directory
        File[] resDirs = resDir.listFiles(File::isDirectory);
        if (resDirs == null) {
            throw new IllegalArgumentException("Invalid directory: " + resDir);
        }

        List<File> xmlFiles = new ArrayList<>();

        // if the resDir starts with "anim", then we need to search for the file in the anim directory
        for (File resSubDir : resDirs) {
            if (resSubDir.getName().equals(type) || resSubDir.getName().startsWith(type + "-")) {
                // get the files in the directory
                File[] files = resSubDir.listFiles();
                if (files == null) {
                    throw new IllegalArgumentException("Invalid directory: " + resSubDir);
                }
                for (File file : files) {
                    if (file.getName().equals(fileName)) {
                        xmlFiles.add(file);
                    }
                }
            }
        }
        return xmlFiles;
    }

    /**
     * Deletes the given directory if it exists.
     * @param directory The directory to delete.
     */
    public static void maybeCleanupDirectory(String directory) {
        if (directory == null) {
            return;
        }
        File dir = new File(directory);
        try {
            if (dir.exists() && dir.isDirectory()) {
                // delete this directory
                FileUtils.forceDelete(dir);
                logger.info("Temporary directory deleted: {}", directory);
            }
        } catch (Exception e) {
            logger.error("Error deleting temporary directory: {}", directory, e);
        }
    }
}
