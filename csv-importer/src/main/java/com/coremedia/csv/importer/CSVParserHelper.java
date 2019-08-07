package com.coremedia.csv.importer;

import com.coremedia.blueprint.common.contentbeans.CMLinkable;
import com.coremedia.cap.common.*;
import com.coremedia.cap.content.*;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructBuilder;
import com.coremedia.xml.Markup;
import com.coremedia.xml.MarkupFactory;
import com.coremedia.csv.constants.CSVConstants;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.coremedia.csv.constants.CSVConstants.*;

/**
 * Helper class which handles the parsing of the CSV file and operations done when translating the text into meaningful
 * properties for content.
 */
public class CSVParserHelper {

    /**
     * The logger for this class.
     */
    private Logger logger;

    /**
     * The content repository.
     */
    private ContentRepository contentRepository;

    /**
     * Helper class which handles operations done in the content repository on content.
     */
    private CSVContentHelper contentHelper;

    /**
     * The root folder which contains all subject taxonomies in the content.
     */
    private Content subjectTaxonomyRootFolder;

    /**
     * Counter for the number of individual content updates imported.
     */
    private int contentImported = 0;

    /**
     * The list of currently imported contents that have not yet been published. These contents will be published later
     * so that it is more efficient.
     */
    @NonNull
    protected final Collection<Content> importedContents = Collections.synchronizedSet(new LinkedHashSet<Content>());

    /**
     * Each content that will eventually be imported may be in one of two states:
     * - un-imported (only as a file on disk)
     * - imported
     * The lock entry in the "transitionLockByPath" map needs to be held during each state transition.
     * While holding a transition lock, no other transition lock may be held by the same thread.
     * The map itself should be synchronized only while retrieving or adding a lock object, so that actual
     * content creation can run in parallel.
     **/
    protected final Map<String, Object> transitionLockByPath = new HashMap<>();

    /**
     * Contains the map of struct objects under localSettings to add the properties accordingly.
     */
    protected Map<String, StructBuilder> structBuilderMap = new HashMap<>();

    /**
     * The general date format the reporting tool will use when converting dates into Strings.
     */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

    /**
     * The number of documents that must be imported before performing a publish.
     */
    private static final int BULK_PUBLISH_INT = 100;

    /**
     * Mapping of property name to PropertyValueObjectProcessor.
     */
    private Map<String, PropertyValueObjectProcessor> propertyValueObjectProcessors;

    /**
     * Some content don't have local settings - we need to check for this.
     */
    private boolean hasLocalSettings;

    /**
     * Constructor.
     *
     * @param autoPublish               if updated content should be automatically be published if prior version was published
     * @param originalContentRepository the content repository to upload & edit content
     * @param logger                    the logger for the CSV Uploader
     */
    public CSVParserHelper(boolean autoPublish, ContentRepository originalContentRepository, Logger logger) {
        contentRepository = originalContentRepository;
        this.logger = logger;
        contentHelper = new CSVContentHelper(autoPublish, contentRepository, logger);
        propertyValueObjectProcessors = new HashedMap();
        propertyValueObjectProcessors.put(PROPERTY_PRODUCT_IDS, new CommerceReferencesProcessor(logger));
        propertyValueObjectProcessors.put(PROPERTY_EXTERNAL_ID, new CommerceReferenceProcessor(logger));
    }

    /**
     * Executes the CSV Parser line-by-line and uploads each records into the content as updates to current content if
     * applicable. This will only update properties and not metadata, and will not update content or content with
     * properties that have content which do not exist. If a record fails to update, this function will log the error
     * and continue to the next record until all records in the CSV are completed.
     *
     * @param parser                           the CSV Parser which will parse the CSV
     * @param reportHeadersToContentProperties the map of all possible CSV headers and the properites mapped to into the
     *                                         content
     * @return true if the entire CSV contents were updated and imported successfully. Else, false.
     */
    public void parseCSV(CSVParser parser, Map<String, String> reportHeadersToContentProperties) {

        instantiateTaxonomyProperties();
        for (CSVRecord record : parser) {

            // reset success boolean - success will be calculated per record
            boolean success = true;

            // reset hasLocalSettings - hasLocalSettings will be calculated per record
            hasLocalSettings = true;

            // Want to check if the content this record refers to even exists
            Content content = fetchContentFromRecord(record, contentRepository);
            if (content != null) {

                // Next we need to verify that the content types are the same. If they aren't, then we want to skip.
                // We do not want the users to think they can update the type in the CSV and have the content
                // magically change types. This needs to be logged and skipped - because properties change between
                // different types of content
                if (verifyContentType(content, record)) {

                    checkPublishImportedContent();
                    logger.info("Started parsing CSV for content with ID " + content.getId());

                    // Currently we have 1. the Map of the CSV record containing the A) column headers and B) values
                    // of the properties to upload and 2. the Map of A) CSV headers to B) property keys. So we need
                    // to match the values from the columns headers to their respective property keys.
                    Map<String, String> recordStringProperties = generateRecordPropertiesMap(
                            reportHeadersToContentProperties, record.toMap());

                    // This map is the final properties that are to be uploaded to the content
                    Map<String, Object> recordObjectProperties = new HashedMap();

                    // This is the map of tags. Currently this map will only contain Subject taxonomies, but if this
                    // changes we will want to add more keys to this map
                    Map<String, Set<Content>> tagsMap = new HashMap<>();

                    // Grab the initial localSettings values - as we don't want to overwrite ALL of the local
                    // settings, just the ones for Product Ids and Unmapped Product Keys
                    Struct localSettings = null;
                    Struct originalLocalSettings = null;
                    StructBuilder localSettingsStructBuilder = null;
                    int id = IdHelper.parseContentId(content.getId());

                    // Some content object do not have local settings, so we must account for this as getStruct will
                    // throw an exception if this is the case and fail the import
                    if (content.getType().isSubtypeOf(CMLinkable.NAME)) {
                        try {
                            localSettings = content.getStruct(PROPERTY_LOCAL_SETTINGS);

                            if (localSettings == null) {
                                localSettings = contentHelper.getStructHelper().getEmptyStruct();
                            }
                            originalLocalSettings = localSettings;
                            localSettingsStructBuilder = localSettings.builder();

                            //reset structBuilderMap for every document
                            initializeStructBuilderMap(localSettings);
                        } catch (Exception e) {
                            logger.error("Content with id {} failed to access it local settings.", id, e.getMessage());
                            success = false;
                        }
                    } else {
                        hasLocalSettings = false;
                        logger.debug("Content with id {} does not have a local settings.", id);
                    }

                    if (success) {
                        // Converts all String properties to their respective objects
                        success = convertStringProperties(content, recordStringProperties, recordObjectProperties,
                                tagsMap);
                    }

                    if (success) {

                        // We need to perform calculations on the more complex property values
                        if (hasLocalSettings) {
                            updateLocalSettings(recordObjectProperties, localSettings, originalLocalSettings,
                                    localSettingsStructBuilder);
                        }
                        updateTaxonomies(content, recordObjectProperties, parser, tagsMap);

                         success = setObjectPropertiesInContent(content, recordObjectProperties);

                        if (success && !recordObjectProperties.isEmpty()) {
                            contentImported++;
                        }
                    }
                }
            }
        }
        performFinalImport();
    }

    /**
     * Instantiates the taxonomy properties that are needed when updating taxonomies in the content. Checks to verify
     * that the head folder for subject taxonomies exist and saves the content object of that folder.
     *
     * @return true if the configured content is present for taxonomies. Else, false.
     */
    protected boolean instantiateTaxonomyProperties() {
        boolean success = true;
        //check tag folder
        String subjectTaxonomyRootPath = "/Settings/Taxonomies/Subject";
        subjectTaxonomyRootFolder = contentRepository.getChild(subjectTaxonomyRootPath);
        if (subjectTaxonomyRootFolder == null || !subjectTaxonomyRootFolder.isFolder()) {
            logger.error("Taxonomy root path (" + subjectTaxonomyRootPath + ") can't be found in repository" +
                    " or is not a folder. (content: " + subjectTaxonomyRootFolder + ")");
            success = false;
        } else {
            logger.info("Taxonomy root path is registered: " + subjectTaxonomyRootPath);
        }
        return success;
    }

    /**
     * Fetches the Content from CoreMedia specified by the Id column in the CSVRecord, if available.
     *
     * @param record     the CSVRecord from which to pull the Content Id
     * @param repository the repository to search for the Content Id
     * @return the content corresponding to the CSV record, or null if not found.
     */
    private Content fetchContentFromRecord(CSVRecord record, ContentRepository repository) {
        Content content = null;
        String contentId = record.get(COLUMN_ID);
        //Validate if the contentId is a string and not empty
        if (StringUtils.isNumeric(contentId) && !contentId.isEmpty()) {
            if (contentId != null && !contentId.isEmpty()) {
                try {
                    content = repository.getContent(contentId);
                } catch (Exception e){
                    logger.error("Skipping this CSV record because Unexpected Exception in getting the content using the record Id (id : " + record.get("Id") + " )", e);
                }
            }
            if (content == null) {
                int id = IdHelper.parseContentId(record.get("Id"));
                logger.error(String.format("Content with Id %d does not exist. This record has been skipped.", id));
            }
        }
        else {
            logger.error("Skip parsing CSV for record id (" + record.get("Id") + ") because it is empty or not a valid id.");
        }
        return content;
    }

    /**
     * Verifies that the Content Type of the Content in the Content Repository matches that of the type gathered from
     * the CSV Record.
     *
     * @param content the Content in the Content Repository
     * @param record  the CSV Record containing the corresponding content
     * @return True if the Content Types in the record and the repository match. Else, false
     */
    private boolean verifyContentType(Content content, CSVRecord record) {
        boolean success;
        ContentType currentContentType = content.getType();
        ContentType expectedContentType = contentRepository.getContentType(record.get("Type"));
        if (currentContentType != null) {
            success = currentContentType.equals(expectedContentType);
            if (!success) {
                int id = IdHelper.parseContentId(record.get("Id"));
                logger.error(String.format("Content with Id %d is not of the expected type. Expected: %s." +
                                " Actual: %s. This record has been skipped.", id, expectedContentType.getName(),
                        currentContentType.getName()));
            }
        } else {
            success = false;
            int id = IdHelper.parseContentId(record.get("Id"));
            logger.error(String.format("Could not ascerrtain the Content Type of content with ID {}. This record has " +
                    "been skipped", id));
        }
        return success;
    }

    /**
     * Checks to see if the number of imported content has reached its threshold to be published. Because publishing
     * content can be an expensive operation, we do this in bulk to prevent hangs.
     */
    private void checkPublishImportedContent() {
        // Publish only when we hit out bulk publish amount
        if (contentImported > 0 && (contentImported % BULK_PUBLISH_INT) == 0) {

            // publish in between, because the overall process takes to long.
            // //Try to finish up what you can.
            contentHelper.applyPreviousState(importedContents);
        }
    }

    /**
     * Takes the map of the CSV record containing the A) column headers and B) values of the properties to upload and
     * the Map of A) CSV headers to B) property keys. Converts this into a single map of the property keys and their
     * string values.
     *
     * @param reportHeadersToContentProperties the map of CSV headers with the matching property keys
     * @param recordMap                        the map of the CSV record containing the CSV headers and the String
     *                                         values of the properties
     * @return a Map containing the property keys and the property values, gathered by the two specified maps
     */
    private Map<String, String> generateRecordPropertiesMap(Map<String, String> reportHeadersToContentProperties,
                                                            Map<String, String> recordMap) {
        Map<String, String> contentPropertiesAndValues = new HashMap<>();
        Set<String> reportHeadersOfProperties = reportHeadersToContentProperties.keySet();

        for (String reportHeaderOfProperty : reportHeadersOfProperties) {
            String contentPropertyName = reportHeadersToContentProperties.get(reportHeaderOfProperty);
            String contentPropertyValue = recordMap.get(reportHeaderOfProperty);
            contentPropertiesAndValues.put(contentPropertyName, contentPropertyValue);
        }

        return contentPropertiesAndValues;
    }

    /**
     * Compares the content's local settings for commerce values with the translated values from the record and adds the
     * updated values to the map (if needed).
     *
     * @param recordObjectProperties    the map of values which will be used to determine which properties of the content
     *                                  need to be updated
     * @param localSettings             these are the local settings that have been gathered from the CSV Record which will need to
     *                                  be set on the content
     * @param originalLocalSettings     these are the original local settings values for all of the content
     * @param localSettingStructBuilder the localSettingsBuilder responsible for both holding the old values which do
     *                                  not need to be overwritten, and the new values which need to be added
     */
    private void updateLocalSettings(Map<String, Object> recordObjectProperties, Struct localSettings,
                                     Struct originalLocalSettings, StructBuilder localSettingStructBuilder) {
        // First we want to gather all of the original properties, and then piecemeal replace all properties which
        // were updated/added, from the structbuilder map
        Map<String, Object> newProperties = new HashMap<>();
        Map<String, Object> properties = localSettings.getProperties();
        newProperties.putAll(properties);
        for (String structKey : structBuilderMap.keySet()) {
            StructBuilder toAdd = structBuilderMap.get(structKey);
            if (localSettings.get(structKey) != null) {
                newProperties.put(structKey, toAdd.build());
            } else {
                // We don't want to add an empty map if none existed previously
                if (!toAdd.currentStruct().getProperties().isEmpty()) {
                    localSettingStructBuilder = localSettingStructBuilder.declareStruct(structKey,
                            toAdd.build());
                }
            }
        }
        localSettingStructBuilder.setAll(newProperties);
        localSettings = localSettingStructBuilder.build();
        // We need to verify that the local settings are different before adding them to the update
        // content list
        if (!localSettings.equals(originalLocalSettings)) {
            recordObjectProperties.put(PROPERTY_LOCAL_SETTINGS, localSettings);
        }
    }


    /**
     * Compares the taxonomies calculated from the CSV record to the taxonomies of the actual content and verifies
     * whether they have changed and need to be updated. If the content's taxonomies do need to be updated, they will
     * be added to the map.
     *
     * @param content                the respective content in the repository to the CSV record
     * @param recordObjectProperties the map of properties which to update the content
     * @param parser                 the CSV parser which verifies the record entry for the content's taxonomies
     * @param tagsMap                the map of taxonomies calculated from the CSV record
     */
    private void updateTaxonomies(Content content, Map<String, Object> recordObjectProperties, CSVParser parser,
                                  Map<String, Set<Content>> tagsMap) {
        // We need to verify that the tags are different before adding them to the update content
        // list
        List<Content> subjectTaxonomies = contentHelper.flattenTagsMap(tagsMap);
        List<Content> existingSubjectTaxonomies = (List<Content>) content.get(PROPERTY_SUBJECT_TAGS);
        if (existingSubjectTaxonomies != null) {
            if (parser.getHeaderMap().containsKey(CSVConstants.COLUMN_SUBJECT_TAGS) &&
                    !listEqualsIgnoreOrder(subjectTaxonomies, existingSubjectTaxonomies)) {
                recordObjectProperties.put(PROPERTY_SUBJECT_TAGS, subjectTaxonomies);
            }
        } else {
            int id = IdHelper.parseContentId(content.getId());
            logger.warn("Subject Taxonomies do not exist for Content with Id {}", id);
        }
    }


    /**
     * Initializes the struct builder map with localSettings of the current content.
     *
     * @param localSettings the localSettings of the content from which to populate the struct builder map
     */
    private void initializeStructBuilderMap(Struct localSettings) {
        structBuilderMap = new HashMap<>();
        for (Map.Entry<String, Object> property : localSettings.getProperties().entrySet()) {
            String propertyName = property.getKey();
            Object propertyValue = property.getValue();
            if (propertyValue instanceof Struct) {
                StructBuilder builder = ((Struct) propertyValue).builder();
                structBuilderMap.put(propertyName, builder);
            }
        }
    }

    /**
     * For the specified content, converts a mapping of properties their string values into their respective objects and
     * populates another mapping, containing the property name and the object values. If any property is the same value
     * as the property which currently exists in the content, it will not be added to the map. If an error is
     * encountered while setting the properties, this will stop updating any more properties which have not yet been
     * set.
     *
     * @param content                the content for which to update its properties
     * @param recordStringProperties the mapping of the property names and their string values, which are to be
     *                               converted
     * @param objectProperties       the mapping of property names and their respective object values, which is to be
     *                               populated
     * @param tagsMap                the mapping of tags for the specified content object that is to be updated
     * @return true if all properties were converted successfully. Else, false.
     */
    private boolean convertStringProperties(Content content, Map<String, String> recordStringProperties,
                                            Map<String, Object> objectProperties, Map<String, Set<Content>> tagsMap) {
        boolean success = true;
        for (Map.Entry<String, String> entry : recordStringProperties.entrySet()) {
            String propertyName = entry.getKey();
            Object propertyValueObject = entry.getValue();
            try {
                // transform the values with regards to the configured mappings
                if (propertyValueObject != null) {
                    Object processedPropertyValueObject = processPropertyValueObject(content, propertyName, propertyValueObject);

                    // Properties which require special handling...
                    if (propertyName.contains(PROPERTY_PREFIX_PICTURES)) {
                        success = handlePicture(content, propertyName, processedPropertyValueObject);
                    } else if (propertyName.contains(PROPERTY_PREFIX_LOCAL_SETTINGS)) {
                        if(!hasLocalSettings && !processedPropertyValueObject.toString().isEmpty()){
                            success = false;
                            logger.error("CSV Content has a local setting but the content type has no local settings.");
                        }
                        else{
                            success = handleLocalSetting(content, propertyName, processedPropertyValueObject);
                        }
                    } else if (propertyName.equals(PROPERTY_SUBJECT_TAGS)) {
                        success = handleTaxonomies(propertyName, tagsMap, processedPropertyValueObject);
                    } else {
                        success = handleRegularProperty(content, propertyName, processedPropertyValueObject, objectProperties);
                    }
                }
                // If any property fails to set - we want to break out of this loop
                if (!success) {
                    break;
                }
            } catch (Exception e) {
                logger.error("Unexpected Exception in document (id : " + content.getId() + ", property: " +
                        propertyName + ")", e);
                success = false;
            } finally {
                logger.debug("End mapping of property " + propertyName);
            }
        }
        if (!success) {
            logger.error(String.format("An error has occurred while updating content %s. This record" +
                    " has been skipped.", content.getId()));
        }
        return success;
    }

    /**
     * Manipulates/validates property value object if PropertyValueObjectProcessor is configured for given property name.
     *
     * @param content             The content for which to process the property
     * @param propertyName        Name of property
     * @param propertyValueObject Value of property
     * @return Resulting value
     */
    private Object processPropertyValueObject(Content content, String propertyName, Object propertyValueObject) {
        PropertyValueObjectProcessor propertyValueObjectProcessor = propertyValueObjectProcessors.get(propertyName);
        if (propertyValueObjectProcessor == null) {
            return propertyValueObject;
        }
        return propertyValueObjectProcessor.process(content, propertyName, propertyValueObject);
    }

    /**
     * Handles a local setting property. Truncates the property name down to the direct name of the local setting
     * property and handles the remaining property name as a Struct.
     *
     * @param content             the content to which to set the new local settings property value
     * @param propertyName        the name of the local settings property to set. Should be in the form,
     *                            "localSettings.PROPERTY_NAME.PROPERTY_NAME_2"
     * @param propertyValueObject the value of that local setting property
     * @return True if succeeded to set the local settings property in the Struct map. Else, false.
     */
    private boolean handleLocalSetting(Content content, String propertyName, Object propertyValueObject) {
        String localSettingStructName = propertyName.substring(propertyName.indexOf('.') + 1);
        return handleStructSetting(content, localSettingStructName, propertyValueObject);
    }

    /**
     * Handles a struct setting property. Ultimately, this will be put into the local settings struct builder map.
     * Assumes that the value is a string that can be turned into an array.
     *
     * @param content      the content to which to set the new local settings property value
     * @param propertyName the name of the local settings property to set
     * @param value        the value of the localSettings property. Because rigth now we are only updating Product Ids and
     *                     Unmapped Product Keys, we convert this to a String List
     * @return true if the local settings was successfully placed in the struct builder map. Else, false.
     */
    private boolean handleStructSetting(Content content, String propertyName, Object value) {
        boolean success = true;
        if (null != value) {
            StructHelper structHelper = contentHelper.getStructHelper();
            StructBuilder structBuilder = structHelper.lookUpStructBuilderMap(structBuilderMap,
                    propertyName);
            if (structBuilder != null) {
                // NOTE: If any other properties are added to the report to be uploaded for local settings - THIS WILL
                // NEED TO BE UPDATED. We automatically convert to a String List since all current local settings
                // properties we set ad String Lists. commerce.references and sharepoint.productkeys
                List<String> valueArray = convertObjectStringToStringList(value);
                List<String> existingValueArray = structHelper.getValueForSetting(propertyName,
                        content);
                // Only update if the list is not already equal to the values
                if (!listEqualsIgnoreOrder(valueArray, existingValueArray)) {
                    structBuilder = structHelper.addItemToStructBuilder(structBuilder,
                            CapPropertyDescriptorType.STRING, propertyName, valueArray, true);
                    structBuilder.build();
                }
            } else {
                logger.error("Unexpected error while creating or getting the structbuilder for property." +
                        " Property (" + propertyName + ") will be ignored.");
                success = false;
            }
        }
        return success;
    }

    /**
     * Compares 2 lists if they are equal, regardless of order
     *
     * @param list1 the first list to compare
     * @param list2 the second list to compare
     * @param <T>   the common list type shared between the two lists
     * @return true if they are equal. Else, false.
     */
    private static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    /**
     * Converts an object into a String List.
     *
     * @param value the Object to convert. The toString() method of this object should return a String in the form of
     *              "[example1,example2,example3]"
     * @return a list of string, converted from the specified Object's toString() method
     */
    public static List<String> convertObjectStringToStringList(Object value) {
        String stringValue = value.toString();
        stringValue = stringValue.replaceAll("\\[", "");
        stringValue = stringValue.replaceAll("\\]", "");
        List<String> stringList = new ArrayList<>(Arrays.asList(stringValue.split(",")));
        // trim spaces
        List<String> result = new ArrayList<>();
        for (String s : stringList) {
            result.add(s.trim());
        }
        // Remove all empty elements
        result.removeAll(Arrays.asList("", null, " "));
        return result;
    }

    /**
     * Sets the value of a taxonomies property.
     *
     * @param propertyName the name of the property which to set in the content
     * @param tagsMap      the map of tags which consolidate all tags and is set to the content
     * @param value        the value of the content's taxonomies. The toString() method of this object should return a
     *                     String in the form of "[/path/of/tag1/,/path/of/tag2/,/path/of/tag3/]"
     * @return True if all specified tags existed and were set properly in the tags map. else, false.
     */
    private boolean handleTaxonomies(String propertyName, Map<String, Set<Content>> tagsMap, Object value) {
        boolean success = true;
        if (value != null) {
            List<String> taxonomies = convertObjectStringToStringList(value);
            for (String taxonomyString : taxonomies) {
                success = handleTaxonomy(propertyName, tagsMap, taxonomyString.trim());
                // If any taxonomy fails to be set, we need to break and return false.
                if (!success) {
                    break;
                }
            }
        }
        return success;
    }

    /**
     * Retrieves a taxonomy at the specified path and if it exists, adds it into the specified mapping of tags.
     *
     * @param propertyName the name of the property to have its tags set
     * @param tagsMap      the map of tags which will be updated with a new taxonomy
     * @param value        the relative path of the tags, starting from the root tag (i.e. Subjects)
     * @return True if the tag was successfully set into the tags map. Else, false.
     */
    private boolean handleTaxonomy(String propertyName, Map<String, Set<Content>> tagsMap, String value) {
        boolean success = true;
        if (null != value) {
            //get correct tags list, based on target Property
            Set<Content> tagListForTargetProperty = tagsMap.get(propertyName);
            if (tagListForTargetProperty == null) {
                tagListForTargetProperty = new HashSet<>();
            }
            if (subjectTaxonomyRootFolder != null) {
                Content taxonomy = contentHelper.establishTax(value, subjectTaxonomyRootFolder);
                if (taxonomy != null) {
                    tagListForTargetProperty.add(taxonomy);
                    tagsMap.put(propertyName, tagListForTargetProperty);
                } else {
                    // We need to error out if the tag does not exist
                    logger.error(String.format("Could not find Taxonomy with path %s.", value));
                    success = false;
                }
            } else {
                logger.error("Taxonomy properties have not been configured correctly. " +
                        "Taxonomy values cannot be updated.");
                success = false;
            }
        }
        return success;
    }

    /**
     * Handles a regular content property. Find's the property descriptor of the specified property (by name) and then
     * handles how that property should be set from the specified property value object. If the property value object
     * succeeds in being converted to the correct type of the found property (by name), it will add the property name
     * and the converted property object to the specified map.
     *
     * @param content             the content to which to set the property
     * @param propertyName        the name of the property to set
     * @param propertyValueObject the value of the property to set
     * @param objectProperties    the mapping of properties that will be used to update the content
     * @return true if it succeeds to find the property, convert the object correctly to the expected type, and add it
     * to the map. Else, false.
     * @throws ParseException               if an exception occurs when parsing a date property
     * @throws UnsupportedEncodingException if an exception occurs while parsing a Markup/rich text property
     */
    private boolean handleRegularProperty(Content content, String propertyName, Object propertyValueObject,
                                          Map<String, Object> objectProperties) throws ParseException,
            UnsupportedEncodingException {
        boolean success = true;
        Object existingProperty = content.get(propertyName);

        // Here we need to make sure that null and empty are treated the same in regards to updating
        // That this means is that if the property value object isn't equal to null (which means that the
        // existing property is null) and if it is empty... then we do nothing, because CSV cannot give us
        // null values for properties that do not exist and any properties set in the content to null by
        // default must be set
        if (existingProperty != null || !propertyValueObject.toString().isEmpty()) {
            CapPropertyDescriptor propertyDescriptor = content.getType().getDescriptor(propertyName);
            if (propertyDescriptor != null) {
                CapPropertyDescriptorType type = propertyDescriptor.getType();
                switch (type) {
                    case MARKUP:
                        propertyValueObject = handleRichText(propertyValueObject);
                        break;
                    case INTEGER:
                        propertyValueObject = new Integer(propertyValueObject.toString());
                        break;
                    case LINK:
                        // We set the property, however if he property is still null after we set it,
                        // that means that the content repository could not find this content
                        // This is an error, and should be reported
                        String valueForError = propertyValueObject.toString();
                        propertyValueObject = handleLink(propertyValueObject);
                        if (propertyValueObject == null) {
                            logger.error(String.format("Could not find Link property, %s, with value %s, for " +
                                    "content %s.", propertyName, valueForError, content.getId()));
                            success = false;
                        }
                        break;
                    case DATE:
                        // If the data for the column is empty - this means that its equal to null/empty
                        String propertyStringValue = propertyValueObject.toString();
                        if (propertyStringValue.isEmpty()) {
                            propertyValueObject = null;
                        } else {
                            propertyValueObject = dateFormat.format(propertyStringValue);
                        }
                        break;
                    default:
                        // Default behavior - its some kind of String so do nothing
                        break;
                }
                // Only add if we succeeded in getting the property
                if (success) {
                    // only apply the setting if we know they are different values
                    if (!Objects.equals(existingProperty, propertyValueObject)) {
                        objectProperties.put(propertyName, propertyValueObject);
                    }
                }
            }
        }
        return success;
    }

    /**
     * Handles converting a property object into rich text. If the passed in value does not contain
     * the proper XML prefix, it will be automatically added, converted to markup, and returned. Else, if it is a String
     * that is properly formatted CoreMedia XML Grammar, then return the String converted to markup. If an Empty String
     * is passed in, returns null.
     *
     * @param value the property value to which to convert into rich text
     * @return The converted rich text value of the object
     * @throws UnsupportedEncodingException if an error occurs converting the Object value into rich text.
     */
    private Markup handleRichText(Object value) throws UnsupportedEncodingException {
        Markup markup = null;
        if (value != null) {
            String encodedValue = value.toString();
            if (!encodedValue.isEmpty()) {
                String markupPrefix = "<div xmlns=\"http://www.coremedia.com/2003/richtext-1.0\" " +
                        "xmlns:xlink=\"http://www.w3.org/1999/xlink\">";
                if (!encodedValue.contains(markupPrefix)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(markupPrefix);
                    stringBuilder.append(encodedValue);
                    stringBuilder.append("</div>");
                    encodedValue = stringBuilder.toString();
                }
                markup = MarkupFactory.fromString(encodedValue).withGrammar("coremedia-richtext-1.0");
            }
        }
        return markup;
    }

    /**
     * Handles converting a property object into list of contents.
     *
     * @param value the id of the content for which to fetch in the content repository
     * @return the list with the specified link content. Returns null if no content is found.
     */
    private Object handleLink(Object value) {
        List<Content> linkContent = new ArrayList<Content>();
        if (value != null) {
            List<String> links = convertObjectStringToStringList(value);
            for (String valueString : links) {
                if (StringUtils.isNumeric(valueString)) {
                    int contentId = IdHelper.parseContentId(valueString);
                    linkContent.add(contentRepository.getContent(Integer.toString(contentId)));
                }
                else {
                    logger.warn(String.format("Link is not a valid id: " + valueString));
                }
            }
        }

        return linkContent;
    }

    /**
     * Handles setting a property on the main picture of the content.
     *
     * @param content      The content which will have its main picture updated
     * @param propertyName the name of the property of the picture. Should be in the form "pictures.PICTURE_PROPERTY"
     * @param value        the value of the picture property
     */
    private boolean handlePicture(Content content, String propertyName, Object value) {
        boolean success = true;
        List<Content> pictures = (List<Content>) content.get(PROPERTY_PICTURES);

        if (pictures != null) {
            if (!pictures.isEmpty()) {
                logger.info(String.format("Retrieving the picture of content with Id %s.", content.getId()));
                propertyName = propertyName.substring(propertyName.indexOf(".") + 1);
                Content mainPicture = pictures.get(0);
                if (mainPicture != null) {

                    // In the same fashion, we can call the same conversion and setting subroutines that we are using
                    // for the parent content on the picture.
                    Map<String, String> pictureStringProperties = new HashMap<>();
                    pictureStringProperties.put(propertyName, value.toString());
                    Map<String, Object> pictureProperties = new HashMap<>();
                    Map<String, Set<Content>> pictureTagsMap = new HashMap<>();
                    logger.info(String.format("Setting property %s on the main picture (id: %s) of content (id: %s)",
                            propertyName, mainPicture.getId(), content.getId()));
                    success = convertStringProperties(mainPicture, pictureStringProperties, pictureProperties,
                            pictureTagsMap);
                    if (success) {
                        success = setObjectPropertiesInContent(mainPicture, pictureProperties);
                        if (!success) {
                            logger.error(String.format("An error occurred setting properties on picture content with" +
                                            " id %s.",
                                    content.getId()));
                        }
                    }
                }
                // Edge case: if the CSV has a value for a pictures property - but the content itself's picture has been
                // set to null - then this is an error and should be reported back
                else if (value != null && !value.toString().isEmpty()) {
                    logger.error(String.format("Could not set pictures property on the content's picture." +
                                    "\nReason: Content Id %s has a null picture - cannot set properties.",
                            content.getId()));
                    success = false;
                }
            }
            // Edge case: if the CSV has a value for a pictures property - but the content itself has no pictures to
            // which to set properties - then this is an error and should be reported back
            else if (value != null && !value.toString().isEmpty()) {
                logger.error(String.format("Could not set pictures property on the content's picture." +
                                "\nReason: Content Id %s does not have any pictures for which to set properties.",
                        content.getId()));
                success = false;
            }
        }
        // Edge case: if the CSV has a value for a pictures property - but the content itself does not have a pictures
        // property - then this is an error and should be reported back
        else if (value != null && !value.toString().isEmpty()) {
            logger.error(String.format("Could not set pictures property on the content's picture." +
                            "\nReason: Content Id %s does not a picture property for which to set values.",
                    content.getId()));
            success = false;
        }
        return success;
    }

    /**
     * Updates a content's properties to match a specified mapping of property names to values in the content.
     *
     * @param content          the content for which to update properties
     * @param objectProperties the mapping of property name to the new values to which to update the specified content
     * @return True if all properties were updated properly. Else, false.
     */
    protected boolean setObjectPropertiesInContent(Content content, Map<String, Object> objectProperties) {
        boolean success = true;
        // only write if we have properties to write
        if (objectProperties.size() > 0) {
            try {
                String contentPath = content.getPath();
                synchronized (contentHelper.getTransitionLock(contentPath + "_" + content.getId(),
                        transitionLockByPath)) {
                    contentHelper.importContent(content, objectProperties, importedContents);
                    logger.info("Successfully written document.");
                }

            } catch (CheckedOutByOtherException ex) {
                logger.error("ContentCheckedOutByOtherException (id: " + content.getId() + ")", ex.getMessage());
                success = false;
            } catch (ContentException ce) {
                logger.error("ContentException (id: " + content.getId() + ")", ce);
                success = false;
            } catch (InvalidNameException ine) {
                logger.error("InvalidNameException (id: " + content.getId() + ")", ine);
                success = false;
            } catch (DuplicateNameException dne) {
                logger.error("DuplicateNameException (id: " + content.getId() + ")", dne);
                success = false;
            } catch (NoSuchTypeException nse) {
                logger.error("NoSuchTypeException (id: " + content.getId() + ")", nse);
                success = false;
            } catch (Exception e) {
                logger.error("Unexpected Exception (id: " + content.getId() + ")", e);
                success = false;
            }
        } else {
            logger.info("Skip writing content because of no properties to write (id: " +
                    content.getId() + ")");
        }
        return success;
    }

    /**
     * Performs the final import of all remaining content and logging for the CSV Importer.
     */
    private void performFinalImport() {
        // approve/publish the remaining documents
        contentHelper.applyPreviousState(importedContents);
        logger.info("Documents written: " + contentImported);
        logger.info("WritingHandler: executing finished.");
    }
}
