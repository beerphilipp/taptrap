package com.tapjacking.maltapextract;

import com.tapjacking.maltapextract.util.AnimUtils;
import com.tapjacking.maltapextract.util.FileUtil;
import com.tapjacking.maltapextract.util.MiscUtil;
import com.tapjacking.maltapextract.util.XMLUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class is used to resolve references in the resources of an Android app.
 * <p>
 * For analyzing a framework app, any caller should first call the {@link #prepareResolving()} method to parse the values of the resources.
 * Then, they should call the {@link #computeFrameworkInterpolators()} method to resolve the interpolators in the framework resources.
 * Eventually, callers should call the {@link #saveFrameworkResources()} method to save the framework resources to the cache directory.
 *
 * For analyzing a non-framework app, callers should first call the {@link #loadFrameworkResources()} method to load the framework resources from the cache directory.
 * Then, they should call the {@link #prepareResolving()} method to parse the values of the resources and {@link #computeAnimations()} to compute the animations in the app.
 */
public class ResourceResolver {

    private static final Logger logger = LoggerFactory.getLogger(ResourceResolver.class);

    private static final List<String> INTERPOLATOR_TAGS = new ArrayList<>(Arrays.asList("accelerateDecelerateInterpolator", "accelerateInterpolator", "anticipateInterpolator", "anticipateOvershootInterpolator", "bounceInterpolator", "cycleInterpolator", "decelerateInterpolator", "linearInterpolator", "overshootInterpolator", "pathInterpolator"));

    private Map<String, Map<String, ArrayList<String>>> valueResources = new HashMap<>();

    private Map<String, ArrayList<String>> attributeResources = new HashMap<>();
    private final List<ResourceEntry> resourceEntries = new ArrayList<>();

    /**
     * This map is used to store the interpolators that are contained in the framework-res.apk file.
     * The key is the name of the interpolator (e.g., "accelerate_decelerate"), the value is a list of MD5 hashes of the dereferenced XML files.
     */
    private Map<String, ArrayList<String>> frameworkInterpolators = new HashMap<>();

    private final String outputDir;
    private boolean isFrameworkApp = false;
    private final String cacheDirectory;

    /**
     * Creates a new {@link ResourceResolver} to resolve references in the resources of an Android app.
     *
     * @param outputDir The directory of the decompiled resources of the app.
     * @param isFrameworkApp Indicates whether the app is a framework app, such as <code>framework-res.apk</code>.
     * @param cacheDirectory The directory to store cached resources or read them from.
     */
    ResourceResolver(String outputDir, boolean isFrameworkApp, String cacheDirectory) {
        this.outputDir = outputDir;
        this.isFrameworkApp = isFrameworkApp;
        this.cacheDirectory = cacheDirectory;

        Map<String, ArrayList<String>> stringValues = new HashMap<>();
        Map<String, ArrayList<String>> colorValues = new HashMap<>();
        Map<String, ArrayList<String>> boolValues = new HashMap<>();
        Map<String, ArrayList<String>> dimenValues = new HashMap<>();
        Map<String, ArrayList<String>> integerValues = new HashMap<>();

        valueResources.put("string", stringValues);
        valueResources.put("color", colorValues);
        valueResources.put("bool", boolValues);
        valueResources.put("dimen", dimenValues);
        valueResources.put("integer", integerValues);
    }

    /**
     * Prepares the resolving of references in the resources of the app.
     * That means that the values of simple resources (string, color, bool, dimen, integer) are parsed and the references to values and attributes in them are resolved.
     */
    public void prepareResolving() {
        parseValues();
        resolveInitialRefs();
    }

    /**
     * Loads the framework resources from the cache directory.
     * If the app is a framework app, this function does nothing.
     */
    public void loadFrameworkResources() {
        if (this.isFrameworkApp) {
            return;
        }
        FrameworkCacher frameworkCacher = new FrameworkCacher(cacheDirectory);
        Map[] resources = frameworkCacher.deserializeFromFiles();
        this.valueResources = resources[0];
        this.attributeResources = resources[1];
        this.frameworkInterpolators = resources[2];
    }

    /**
     * Saves the framework resources to the cache directory.
     */
    public void saveFrameworkResources() {
        if (!this.isFrameworkApp) {
            return;
        }

        Map<String, Map<String, ArrayList<String>>> newValueResources = new HashMap<>();

        // proceed the value resources with the android: prefix
        for (String type : this.valueResources.keySet()) {
            Map<String, ArrayList<String>> resourcesMap = this.valueResources.get(type);
            Map<String, ArrayList<String>> newResourcesMap = new HashMap<>();
            for (String resourceName : resourcesMap.keySet()) {
                ArrayList<String> values = resourcesMap.get(resourceName);
                String newResourceName = "android:" + resourceName;
                newResourcesMap.put(newResourceName, new ArrayList<>(values));
            }
            newValueResources.put(type, newResourcesMap);
        }

        // proceed the attribute resources with the android: prefix
        Map<String, ArrayList<String>> newAttributeResources = new HashMap<>();
        for (String attributeName : this.attributeResources.keySet()) {
            ArrayList<String> values = this.attributeResources.get(attributeName);
            String newAttributeName = "android:" + attributeName;
            newAttributeResources.put(newAttributeName, new ArrayList<>(values));
        }

        Map<String, ArrayList<String>> newFrameworkInterpolators = new HashMap<>();
        for (String interpolatorName : this.frameworkInterpolators.keySet()) {
            ArrayList<String> uniqueValues = new ArrayList<>(new HashSet<>(this.frameworkInterpolators.get(interpolatorName)));
            newFrameworkInterpolators.put(interpolatorName, uniqueValues);
        }

        FrameworkCacher frameworkCacher = new FrameworkCacher(cacheDirectory);
        frameworkCacher.serializeToFiles(newValueResources, newAttributeResources, newFrameworkInterpolators);
    }

    /**
     * Ensures that the res directory exists and is a directory.
     * If the directory does not exist or is not a directory, a RuntimeException is thrown.
     *
     * @return The res directory as a File object.
     * @throws RuntimeException If the directory does not exist or is not a directory.
     */
    private File ensureResDirectory() {
        File outputDirFile = new File(this.outputDir);
        if (!(outputDirFile.exists() && outputDirFile.isDirectory())) {
            throw new RuntimeException("Output directory does not exist or is not a directory");
        }
        return new File(outputDirFile, "res");
    }

    /**
     * Returns a list of all XML files in the given directory that start with the given prefix.
     * Among others, this function is used to find all XML files in the res/values* directories.
     * @param baseDirectory The base directory to search in.
     * @param dirStartsWith The prefix that the XML files should start with.
     * @return A list of all XML files in the given directory that start with the given prefix.
     */
    private List<File> getXmlFilesInDir(File baseDirectory, String dirStartsWith) {
        ArrayList<File> xmlFiles = new ArrayList<>();
        File[] dirs = baseDirectory.listFiles((dir, name) -> name.startsWith(dirStartsWith) && new File(dir, name).isDirectory());
        if (dirs == null) {
            throw new RuntimeException("No directories found in " + baseDirectory.getAbsolutePath());
        }
        for (File d : dirs) {
            // Get all the files in the directory that end with .xml
            File[] file = d.listFiles((dir, name) -> name.endsWith(".xml"));
            if (file == null) {
                throw new RuntimeException("No files found in directory " + d.getAbsolutePath());
            }
            xmlFiles.addAll(Arrays.asList(file));
        }
        return xmlFiles;
    }


    /**
     * Parses a subset of the simple values (string, color, bool, dimen, integer) inside res/values* directories.
     * The function also parses the style elements inside the values files, necessary to resolve attribute references.
     * <p>
     * This function does <bold>NOT</bold> resolve any references and attributes. That means that the resulting
     * maps may contain references of the form "@string/foo" or "?attr/bar" that need to be resolved separately.
     *
     */
    private void parseValues() {
        logger.info("Parsing values XML files");
        File resDir = ensureResDirectory();
        List<File> valueFiles = getXmlFilesInDir(resDir, "values");
        for (File valueFile : valueFiles) {
            parseValuesXML(valueFile);
        }

        // loop through the valueResources map and get rid of duplicates in the values
        this.valueResources.forEach((type, resourcesMap) ->
                resourcesMap.replaceAll((resourceName, values) -> new ArrayList<>(new HashSet<>(values)))
        );

        this.attributeResources.forEach((attributeName, values) -> {
            ArrayList<String> uniqueValues = new ArrayList<>(new HashSet<>(values));
            this.attributeResources.put(attributeName, uniqueValues);
        });
    }

    /**
     * Parses a complex XML file, e.g., an animation file and resolves all references and attributes.
     * This function must be called after all value resources have been parsed.
     * @param type The type of the resource, e.g., ANIM or INTERPOLATOR.
     * @param xmlFile The XML file to parse.
     * @param tagFilter A list of tags that are acceptable. If the XML file contains tags that are not in this list, the file is skipped.
     * @param onlyConsiderAttributes A list of attributes to consider during resolution. If an attribute is not in this list, it is not resolved. Null if all attributes should be considered.
     * @return A list of MD5 hashes of the dereferenced XML files.
     */
    private List<String> parseComplexXML(ResourceEntry.ResourceType type, File xmlFile, List<String> tagFilter, List<String> onlyConsiderAttributes) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xmlFile);

            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            // get all the tags in the XML file, including children
            List<String> tags = XMLUtil.getAllTags(root);
            if (tagFilter != null && !new HashSet<>(tagFilter).containsAll(tags)) {
                // if the element contains tags that are not acceptable tween animations (or in general, not part of tagFilter), we skip it
                return new ArrayList<>();
            }

            List<Pair<String, String>> references = XMLUtil.getReferences(root, onlyConsiderAttributes);
            Map<String, List<String>> resolvedReferences = new HashMap<>();

            for (Pair<String, String> pair : references) {
                String attributeName = pair.getLeft();
                String attributeValue = pair.getRight();
                List<String> attributeValues = List.of(attributeValue);
                if (attributeValue.startsWith("?")) {
                    attributeValues = this.attributeResources.get(attributeValue.substring(1));
                    if (attributeValues == null) {
                        attributeValues = List.of("!!unknown(" + attributeValue + ")");
                    }
                }
                List<String> allResolvedValues = new ArrayList<>();
                for (String value : attributeValues) {
                    if (MiscUtil.isReference(value)) {
                        List<String> values = resolveValueReference(value);
                        allResolvedValues.addAll(values);
                    } else {
                        allResolvedValues.add(value);
                    }
                }
                resolvedReferences.put(attributeValue, allResolvedValues);
            }

            // make sure that all references have been resolved (even if only with !!unknown)
            for (String key : resolvedReferences.keySet()) {
                if (resolvedReferences.get(key).isEmpty()) {
                    throw new RuntimeException("Reference " + key + " had an empty list when resolving in file " + xmlFile.getAbsolutePath());
                }
            }

            // create a list of all possible combinations of the resolved references. the output is a list of map string - string
            List<Map<String, String>> resolvedCombinations = generateCombinations(resolvedReferences);
            List<Node> dereferencedXMLs = new ArrayList<>();
            if (resolvedCombinations.isEmpty()) {
                Node dereferencedXML = XMLUtil.createDereferencedXMLs(root, new HashMap<>());
                dereferencedXMLs.add(dereferencedXML);
            } else {
                for (Map<String, String> combination : resolvedCombinations) {
                    Node dereferencedXML = XMLUtil.createDereferencedXMLs(root, combination);
                    dereferencedXMLs.add(dereferencedXML);
                }
            }

            ArrayList<String> md5Hashes = new ArrayList<>();
            for (int i = 0; i < dereferencedXMLs.size(); i++) {
                String serializedXML = XMLUtil.serializeXML(dereferencedXMLs.get(i));
                String md5Hash = DigestUtils.md5Hex(serializedXML);
                md5Hashes.add(md5Hash);
                String relativeFilePath = xmlFile.getAbsolutePath().replace(this.outputDir, "");
                resourceEntries.add(new ResourceEntry(type, relativeFilePath, i, md5Hash, serializedXML));
            }
            return md5Hashes;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error("Error parsing complex XML file: {}", xmlFile.getAbsolutePath(), e);
            // As we could not parse this XML file, we skip it
            return new ArrayList<>();
        }
    }

    /**
     * Generates all possible combinations of resolved references.
     * @param resolvedReferences A map of resolved references.
     * @return A list of all possible combinations of resolved references.
     */
    private static List<Map<String, String>> generateCombinations(Map<String, List<String>> resolvedReferences) {
        List<Map<String, String>> results = new ArrayList<>();
        List<String> keys = new ArrayList<>(resolvedReferences.keySet());
        generateCombinationsHelper(resolvedReferences, keys, 0, new HashMap<>(), results);
        return results;
    }

    /**
     * Helper method to generate all possible combinations of resolved references.
     * @param resolvedReferences A map of resolved references, where the key is the attribute name and the value is a list of possible values.
     * @param keys A list of keys from the resolvedReferences map.
     * @param index The current index in the keys list.
     * @param current The current combination being built.
     * @param results A list to store all the generated combinations.
     */
    private static void generateCombinationsHelper(Map<String, List<String>> resolvedReferences, List<String> keys, int index, Map<String, String> current, List<Map<String, String>> results) {
        if (index == keys.size()) {
            results.add(new HashMap<>(current)); // Add a copy of the current combination
            return;
        }

        String key = keys.get(index);
        for (String value : resolvedReferences.get(key)) {
            current.put(key, value);
            generateCombinationsHelper(resolvedReferences, keys, index + 1, current, results);
            current.remove(key); // Backtrack
        }
    }

    /**
     * Parses a values XML file and extracts the values and attributes.
     * @param valueFile The XML file to parse, typically located in res/values* directories.
     */
    private void parseValuesXML(File valueFile) {
        try {
            // Create a DocumentBuilderFactory
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(valueFile);

            doc.getDocumentElement().normalize();

            // The root element is <resources>
            Element resourcesElement = doc.getDocumentElement();

            // Get all child nodes of <resources>
            NodeList children = resourcesElement.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                // Only process element nodes
                if (child.getNodeType() == Node.ELEMENT_NODE) {

                    // There are two ways how to values are encoded here. Either there
                    // is a tag with the name of the resource: <dimen name="m3_sys_elevation_level5">12.0dip</dimen>
                    // or there is an item tag with a type attribute: <item type="dimen" name="m3_sys_motion_easing_emphasized_accelerate_control_x1">0.3</item>

                    Element element = (Element) child;
                    String resourceName = element.getAttribute("name");
                    String value = element.getTextContent().trim();
                    String type;
                    if (element.getTagName().equals("item")) {
                        type = element.getAttribute("type");
                    } else {
                        type = element.getTagName();
                    }

                    switch (type) {
                        case "string", "color", "bool", "dimen", "integer" -> {
                            // Initialize map for resource type if not present
                            valueResources.computeIfAbsent(type, k -> new HashMap<>());
                            // Initialize list for resource name if not present
                            valueResources.get(type).computeIfAbsent(resourceName, k -> new ArrayList<>());
                            // Add value to the list
                            valueResources.get(type).get(resourceName).add(value);
                        }
                        case "style" -> {
                            // Process style elements
                            NodeList styleChildren = element.getChildNodes();
                            for (int j = 0; j < styleChildren.getLength(); j++) {
                                Node styleChild = styleChildren.item(j);
                                if (styleChild.getNodeType() == Node.ELEMENT_NODE) {
                                    Element styleChildElement = (Element) styleChild;
                                    String styleChildTag = styleChildElement.getTagName();
                                    String styleChildAttribute = styleChildElement.getAttribute("name");
                                    String styleChildValue = styleChildElement.getTextContent().trim();
                                    if ("item".equals(styleChildTag)) {
                                        attributeResources.computeIfAbsent(styleChildAttribute, k -> new ArrayList<>());
                                        attributeResources.get(styleChildAttribute).add(styleChildValue);
                                    }
                                }
                            }
                        }
                        //default -> throw new RuntimeException("Unknown resource type: " + type);
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Error parsing values XML file: " + valueFile.getAbsolutePath(), e);
        }
    }

    /**
     * Resolves all references in the value resources.
     * This function is called after all value and attribute resources have been parsed.
     */
    private void resolveInitialRefs() {
        logger.info("Resolving initial references");
        resolveAttributeResources();
        resolveValueResources();
    }

    /**
     * Resolves all references in the value resources.
     */
    private void resolveValueResources() {
        for (String type : this.valueResources.keySet()) {
            for (String resourceName : this.valueResources.get(type).keySet()) {
                ArrayList<String> values = this.valueResources.get(type).get(resourceName);
                ArrayList<String> newValues = new ArrayList<>();
                for (String value : values) {
                    List<String> vs = List.of(value);
                    if (value.startsWith("?")) {
                        // This is a reference to an attribute. All references to attributes have been resolved in a previous step, so we can simply read it.
                        // Note, however, that these can still contain references to other values using @ that have to be resolved.
                        vs = this.attributeResources.get(value.substring(1));
                    }
                    for (String v : vs) {
                        if (MiscUtil.isReference(v)) {
                            newValues.addAll(resolveValueReference(v));
                        } else {
                            newValues.add(v);
                        }
                    }
                }
                this.valueResources.get(type).put(resourceName, newValues);
            }
        }

    }

    /**
     * Resolves all references in the attribute resources.
     * Resolved in this case means that we resolve all references to other attributes.
     * The output therefore contains either a value or a reference to a value resource (using @), but no references to other attributes.
     */
    private void resolveAttributeResources() {
        for (String attributeName : this.attributeResources.keySet()) {
            ArrayList<String> values = this.attributeResources.get(attributeName);
            ArrayList<String> newValues = new ArrayList<>();
            for (String value : values) {
                if (value.startsWith("?")) {
                    newValues.addAll(resolveAttributeReference(value));
                } else {
                    newValues.add(value);
                }
            }
            this.attributeResources.put(attributeName, newValues);
        }
    }

    /**
     * Resolves a reference to an attribute.
     * @param reference The reference to the attribute, starting with a ?.
     * @return A list of values for the given attribute reference.
     */
    private List<String> resolveAttributeReference(String reference) {
        return resolveAttributeReference(reference, 0);
    }

    /**
     * Resolves a reference to an attribute, with a depth limit to prevent infinite recursion.
     * @param reference The reference to the attribute, starting with a ?.
     * @param depth The current depth of recursion. Used to prevent infinite recursion in case of circular references.
     * @return A list of values for the given attribute reference.
     */
    private List<String> resolveAttributeReference(String reference, int depth) {
        if (depth > 50) {
            // We are in a loop.
            // This is probably because the app overwrites an android: attribute with a ?attr reference, but the android:reference is not resolved/not contained in the framework-res.apk
            return List.of("!!unknown(" + reference + ")");
        }
        if (!reference.startsWith("?")) {
            return List.of(reference);
        }
        String name = reference.substring(1);
        List<String> newValues = new ArrayList<>();
        if (this.attributeResources.containsKey(name)) {
            List<String> values = this.attributeResources.get(name);
            for (String value : values) {
                if (value.startsWith("?")) {
                    newValues.addAll(resolveAttributeReference(value, ++depth));
                } else {
                    newValues.add(value);
                }
            }
        }
        return newValues;
    }

    /**
     * Resolves a reference to another resource with a given name.
     * @param resourceName The name of the resource to resolve. Always starts with an @.
     * @return A list of values for the given resource name.
     */
    private List<String> resolveValueReference(String resourceName) {
        // The resource name starts with an @.
        String reference = resourceName.substring(1); // the reference is of the form [package:]type/name
        String packageName = reference.split(":")[0];
        String typeName;
        String type;
        String name;

        if (packageName.equals(reference)) {
            // no package name is given
            typeName = reference;
            type = reference.split("/")[0];
            name = reference.split("/")[1];
        } else {
            typeName = reference.split(":")[1];
            type = typeName.split("/")[0];
            name = packageName + ":" + typeName.split("/")[1];
        }

        ArrayList<String> values = new ArrayList<>();

        switch (type) {
            case "string", "color", "bool", "dimen", "integer":
                if (!this.valueResources.get(type).containsKey(name)) {
                    // The reference could not be resolved
                    values.add("!!unknown(" + resourceName + ")");
                    return values;
                }
                ArrayList<String> refValues = this.valueResources.get(type).get(name);
                values.addAll(refValues);
                break;

            default: // interpolators can be contained in multiple directories
                if (resourceName.startsWith("@android")) {
                    // This is a reference to an android resource.
                    String androidInterpolatorName = resourceName.substring(resourceName.lastIndexOf("/") + 1);
                    List<String> hashes = this.frameworkInterpolators.get(androidInterpolatorName);
                    if (hashes == null) {
                        return List.of("!!unknown(" + resourceName + ")");
                    }
                    return hashes.stream().map(hash -> "@@" + hash).toList();
                }
                List<File> files = FileUtil.getXmlFilesForReference(name, type, this.outputDir);
                List<String> hashes = new ArrayList<>();
                for (File file : files) {
                    List<String> fileHashes = parseComplexXML(ResourceEntry.ResourceType.INTERPOLATOR, file, null, null);
                    hashes.addAll(fileHashes);
                }
                return hashes.stream().map(hash -> "@@" + hash).toList();
        }

        // There may be transitive references, so we need to resolve them as well

        List<String> newValues = new ArrayList<>();
        for (String value : values) {
            if (MiscUtil.isReference(value)) {
                newValues.addAll(resolveValueReference(value));
            } else {
                newValues.add(value);
            }
        }
        return newValues;
    }

    /**
     * Computes all animations in the application.
     * @return A list of all animations and dependent interpolators in the application.
     */
    public List<ResourceEntry> computeAnimations() {

        File resDir = ensureResDirectory();
        // We get all resource XML files here, i.e., even those that are not in the res/anim directory.
        List<File> xmlFiles = FileUtil.getFileWithExtension(resDir.getAbsolutePath(), ".xml", true);
        if (xmlFiles == null) {
            throw new RuntimeException("Error while searching for XML files in " + resDir.getAbsolutePath());
        }

        for (File xmlFile : xmlFiles) {
            parseComplexXML(ResourceEntry.ResourceType.ANIM, xmlFile, AnimUtils.TWEEN_TAGS, AnimUtils.TWEEN_ANIM_ATTRIBUTES);
        }

        // get all the resource entries with type ANIM and INTERPOLATOR
        return resourceEntries.stream()
                .filter(entry -> entry.getType() == ResourceEntry.ResourceType.ANIM || entry.getType() == ResourceEntry.ResourceType.INTERPOLATOR)
                .toList();
    }

    /**
     * Resolves all interpolators in the application.
     * This function should be called after the {@link #prepareResolving()} method has been called.
     * It should also only be called when analyzing a framework app.
     * For non-framework apps, this method is a no-op.
     */
    public List<ResourceEntry> computeFrameworkInterpolators() {
        if (!this.isFrameworkApp) {
            return null;
        }

        File resDir = ensureResDirectory();

        // We search for all XML files in all directories. This is to make sure that we find all interpolators, even if they are not in the res/interpolator or in the res/anim directory.
        List<File> xmlFiles = FileUtil.getFileWithExtension(resDir.getAbsolutePath(), ".xml", true);
        if (xmlFiles == null) {
            throw new RuntimeException("Error while searching for XML files in " + resDir.getAbsolutePath());
        }

        List<String> interpolatorHashes = new ArrayList<>();
        for (File xmlFile : xmlFiles) {
            interpolatorHashes.addAll(
                    parseComplexXML(ResourceEntry.ResourceType.INTERPOLATOR, xmlFile, INTERPOLATOR_TAGS, null)
            );
        }

        List<ResourceEntry> allFrameworkInterpolators = resourceEntries.stream()
                .filter(entry -> entry.getType() == ResourceEntry.ResourceType.INTERPOLATOR)
                .toList();

        for (ResourceEntry resourceEntry : allFrameworkInterpolators) {
            String interpolatorFilePath = resourceEntry.getFile();
            String interpolatorName = interpolatorFilePath.substring(interpolatorFilePath.lastIndexOf(File.separator) + 1, interpolatorFilePath.lastIndexOf("."));
            frameworkInterpolators.computeIfAbsent(interpolatorName, k -> new ArrayList<>());
            frameworkInterpolators.get(interpolatorName).add(resourceEntry.getHash());
        }

        return allFrameworkInterpolators;
    }
}
