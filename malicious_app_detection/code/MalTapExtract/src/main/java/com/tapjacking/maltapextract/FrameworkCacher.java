package com.tapjacking.maltapextract;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to serialize and deserialize framework resources to and from files.
 */
public class FrameworkCacher {

    private static final String VALUE_RESOURCES_FILE = "valueResources.ser";
    private static final String ATTRIBUTE_RESOURCES_FILE = "attributeResources.ser";
    private static final String FRAMEWORK_INTERPOLATORS = "frameworkInterpolators.ser";

    private final String cacheDirectory;

    /**
     * Creates a new FrameworkCacher instance with the specified cache directory.
     * @param cacheDirectory The directory where the cached resources will be stored or retrieved from.
     */
    public FrameworkCacher(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        // make sure that the cache directory exists. if not, create it
        maybeCreateCacheDirectory();
    }

    /**
     * Creates the cache directory if it does not exist.
     */
    private void maybeCreateCacheDirectory() {
        File cacheDir = new File(cacheDirectory);
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new RuntimeException("Failed to create cache directory: " + cacheDirectory);
            }
        } else if (!cacheDir.isDirectory()) {
            throw new IllegalArgumentException("The specified cache path is not a directory: " + cacheDirectory);
        }
    }

    /**
     * Serializes the value and attribute resources to files.
     * @param valueResources The value resources to serialize.
     * @param attributeResources The attribute resources to serialize.
     * @param frameworkInterpolators The framework interpolators to serialize.
     */
    public void serializeToFiles(Map<String, Map<String, ArrayList<String>>> valueResources,
                                 Map<String, ArrayList<String>> attributeResources,
                                 Map<String, ArrayList<String>> frameworkInterpolators) {

        String valueResourcesFile = Paths.get(cacheDirectory, VALUE_RESOURCES_FILE).toString();
        String attributeResourcesFile = Paths.get(cacheDirectory, ATTRIBUTE_RESOURCES_FILE).toString();
        String frameworkInterpolatorsFile = Paths.get(cacheDirectory, FRAMEWORK_INTERPOLATORS).toString();

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(valueResourcesFile))) {
            oos.writeObject(valueResources);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(attributeResourcesFile))) {
            oos.writeObject(attributeResources);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(frameworkInterpolatorsFile))) {
            oos.writeObject(frameworkInterpolators);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deserializes the value and attribute resources from files.
     * @return A map array containing the value and attribute resources.
     */
    public Map[] deserializeFromFiles() {
        String valueResourcesFile = Paths.get(cacheDirectory, VALUE_RESOURCES_FILE).toString();
        String attributeResourcesFile = Paths.get(cacheDirectory, ATTRIBUTE_RESOURCES_FILE).toString();

        Map<String, Map<String, ArrayList<String>>> valueResources = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(valueResourcesFile))) {
            valueResources = (Map<String, Map<String, ArrayList<String>>>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        Map<String, ArrayList<String>> attributeResources = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(attributeResourcesFile))) {
            attributeResources = (Map<String, ArrayList<String>>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        Map<String, ArrayList<String>> frameworkInterpolators = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Paths.get(cacheDirectory, FRAMEWORK_INTERPOLATORS).toString()))) {
            frameworkInterpolators = (Map<String, ArrayList<String>>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        // return the maps as array
        return new Map[]{valueResources, attributeResources, frameworkInterpolators};
    }
}
