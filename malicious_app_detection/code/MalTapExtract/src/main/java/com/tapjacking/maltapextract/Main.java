package com.tapjacking.maltapextract;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.tapjacking.maltapextract.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Main class for the MalTapExtract tool.
 * MalTap is a tool that extracts animations and interpolators from Android apps.
 * It decompiles the app using APKTool, extracts the animations and interpolators, and writes them to a database.
 *
 * Before running the tool on an app, the framework-res.apk file needs to be decompiled and cached first using the -framework flag.
 * This is because animations and interpolators in apps can reference resources from the framework.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Parameter(names = "-apk", description = "APK file to analyze", required = true)
    private String apk;

    @Parameter(names = "-framework", description = "Indicates that the app is a framework app")
    private boolean framework;

    @Parameter(names = "-database", description = "Database file to write animations and interpolators to", required = true)
    private String database;

    @Parameter(names = "-cache", description = "Cache directory to store framework resources", required = true)
    private String cache;

    private String packageName;

    /**
     * Runs APKTool on the given APK file and returns the path to the decompiled resources in the temporary directory.
     * @return The path to the decompiled resources or null if an error occurred.
     */
    private String runApkTool() {
        logger.info("Running APKTool on APK file: {}", apk);
        String tempDirectory = System.getProperty("java.io.tmpdir");
        String tmpDirectory = Paths.get(tempDirectory, "maltap-" + UUID.randomUUID()).toFile().getAbsolutePath();

        try {
            brut.apktool.Main.main(new String[]{"d", apk, "-s", "-o", tmpDirectory});
        } catch (Exception e) {
            logger.error("Error running APKTool", e);
            return null;
        }

        return tmpDirectory;
    }

    /**
     * Retrieves resources from a framework app that may be used by other apps and caches them for later use.
     * @param tmpPath The path to the decompiled resources of the framework app.
     */
    private void handleFramework(String tmpPath) {
        logger.info("Handling framework app");
        ResourceResolver resourceResolver = new ResourceResolver(tmpPath, true, this.cache);
        resourceResolver.prepareResolving();
        List<ResourceEntry> interpolators = resourceResolver.computeFrameworkInterpolators();
        resourceResolver.saveFrameworkResources();

        try (DataWriter writer = new DataWriter(packageName, database)) {
            writer.writeInterpolators(interpolators);
        } catch (Exception e) {
            logger.error("Error writing interpolators to database", e);
            throw new RuntimeException("Error writing interpolators to database", e);
        }
    }

    /**
     * Retrieves animations and interpolators from an app and writes them to the database.
     * @param tmpPath The path to the decompiled resources.
     */
    private void handleRegularApp(String tmpPath) {
        logger.info("Handling app");
        ResourceResolver resourceResolver = new ResourceResolver(tmpPath, false, this.cache);
        resourceResolver.loadFrameworkResources();
        resourceResolver.prepareResolving();
        List<ResourceEntry> resources = resourceResolver.computeAnimations();

        List<ResourceEntry> animations = resources.stream().filter(resourceEntry -> resourceEntry.getType().equals(ResourceEntry.ResourceType.ANIM)).toList();
        List<ResourceEntry> interpolators = resources.stream().filter(resourceEntry -> resourceEntry.getType().equals(ResourceEntry.ResourceType.INTERPOLATOR)).toList();

        try (DataWriter writer = new DataWriter(packageName, database)) {
            writer.writeAnimations(animations);
            writer.writeInterpolators(interpolators);
        } catch (Exception e) {
            logger.error("Error writing animations and interpolators to database", e);
            throw new RuntimeException("Error writing animations and interpolators to database", e);
        }
    }

    /**
     * Starts the MalTapExtract tool.
     */
    public void start() {
        this.apk = Paths.get(apk).toFile().getAbsolutePath();
        this.packageName = Paths.get(apk).getFileName().toString().split(".apk")[0];

        String outputDirectory = null;
        try {
            // Run APKTool to decompile the APK
            outputDirectory = runApkTool();

            if (outputDirectory == null) {
                System.exit(1);
            }

            if (this.framework) {
                // if it is a framework app, i.e., framework-res.apk
                handleFramework(outputDirectory);
            } else {
                handleRegularApp(outputDirectory);
            }
        } finally {
            // Clean up the decompressed APK directory from APKTool
            FileUtil.maybeCleanupDirectory(outputDirectory);
        }

    }

    /**
     * Main method for the MalTapExtract tool.
     * Starts the tool with the given command line arguments.
     * The following arguments are supported:
     * <ul>
     *     <li>-apk apk_file: The APK file to analyze (required)</li>
     *     <li>-framework: Indicates that the app is a framework app</li>
     *     <li>-database database_file: The database file to write animations and interpolators to (required)</li>
     *     <li>-cache cache_dir: The cache directory to store framework resources (required)</li>
     * </ul>
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        Main main = new Main();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(args);
        main.start();
    }
}