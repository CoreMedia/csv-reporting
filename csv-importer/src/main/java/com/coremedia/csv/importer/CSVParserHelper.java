package com.coremedia.csv.importer;

import com.coremedia.cap.common.*;
import com.coremedia.cap.content.*;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructService;
import com.coremedia.xml.Markup;
import com.coremedia.xml.MarkupFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.naming.ConfigurationException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.coremedia.csv.common.CSVConstants.*;

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
   * The root folder which contains all location taxonomies in the content.
   */
  private Content locationTaxonomyRootFolder;

    /**
     * Counter for the number of individual content updates imported.
     */
    private int contentImported = 0;

  /**
   * The first content updated in the CSV import.
   */
  private Content firstContent;

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

        // This is where you put custom property processors. This is the framework for customizations to easily create
        // new classes to process custom properties. See PropertyValueObjectProcessor interface for implementation.
        // Ideally, when implemented, under this line the following can be added:
        // propertyValueObjectProcessors.put(PROPERTY_NAME, new CustomPropertyProcessor(logger));
        // A Spring implementation can also be done for this
        propertyValueObjectProcessors = new HashedMap();
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
    public void parseCSV(CSVParser parser, Map<String, String> reportHeadersToContentProperties) throws Exception {

        instantiateTaxonomyProperties();
        for (CSVRecord record : parser) {

            // reset success boolean - success will be calculated per record
            boolean success;

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

                    // Converts all String properties to their respective objects
                    Map<String, Object> updatedProperties = convertStringProperties(content, recordStringProperties);
                    success = setObjectPropertiesInContent(content, updatedProperties);

                    if (success && !updatedProperties.isEmpty()) {
                      if(firstContent == null) {
                        firstContent = content;
                      }
                        contentImported++;
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
    protected void instantiateTaxonomyProperties() {
        //check tag folder
        String subjectTaxonomyRootPath = "/Settings/Taxonomies/Subject";
        subjectTaxonomyRootFolder = contentRepository.getChild(subjectTaxonomyRootPath);
        if (subjectTaxonomyRootFolder == null || !subjectTaxonomyRootFolder.isFolder()) {
            logger.error("Taxonomy root path (" + subjectTaxonomyRootPath + ") can't be found in repository" +
                    " or is not a folder. (content: " + subjectTaxonomyRootFolder + ")");
        } else {
            logger.info("Taxonomy root path is registered: " + subjectTaxonomyRootPath);
        }

        String locationTaxonomyRootPath = "/Settings/Taxonomies/Location";
        locationTaxonomyRootFolder = contentRepository.getChild(locationTaxonomyRootPath);
        if (locationTaxonomyRootFolder == null || !locationTaxonomyRootFolder.isFolder()) {
          logger.error("Taxonomy root path (" + subjectTaxonomyRootPath + ") can't be found in repository" +
                  " or is not a folder. (content: " + locationTaxonomyRootFolder + ")");
        } else {
          logger.info("Taxonomy root path is registered: " + locationTaxonomyRootPath);
        }

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
                    logger.error("Skipping this CSV record because Unexpected Exception in getting the content " +
                            "using the record Id (id : " + record.get("Id") + " )", e);
                }
            }
            if (content == null) {
                int id = IdHelper.parseContentId(record.get("Id"));
                logger.error(String.format("Content with Id %d does not exist. This record has been skipped.", id));
            }
        }
        else {
            logger.error("Skip parsing CSV for record id (" + record.get("Id") + ") because it is empty" +
                    " or not a valid id.");
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
            if (parser.getHeaderMap().containsKey(COLUMN_SUBJECT_TAGS) &&
                    !listEqualsIgnoreOrder(subjectTaxonomies, existingSubjectTaxonomies)) {
                recordObjectProperties.put(PROPERTY_SUBJECT_TAGS, subjectTaxonomies);
            }
        } else {
            int id = IdHelper.parseContentId(content.getId());
            logger.warn("Subject Taxonomies do not exist for Content with Id {}", id);
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
     * @return true if all properties were converted successfully. Else, false.
     */
    private Map<String, Object> convertStringProperties(Content content, Map<String, String> recordStringProperties)
            throws Exception {

      // This map is the final properties that are to be uploaded to the content
      Map<String, Object> updatedProperties = new HashMap();
      for (Map.Entry<String, String> entry : recordStringProperties.entrySet()) {
            String propertyName = entry.getKey();
            Object propertyValueObject = entry.getValue();
            try {
                // transform the values with regards to the configured mappings
                if (propertyValueObject != null) {
                    Object processedPropertyValueObject = processPropertyValueObject(content, propertyName,
                            propertyValueObject);
                  Object existingProperty = content.get(propertyName);

                  // Here we need to make sure that null and empty are treated the same in regards to updating
                  // That this means is that if the property value object isn't equal to null (which means that the
                  // existing property is null) and if it is empty... then we do nothing, because CSV cannot give us
                  // null values for properties that do not exist and any properties set in the content to null by
                  // default must be set
                  if (existingProperty != null || !processedPropertyValueObject.toString().isEmpty()) {
                    processedPropertyValueObject = convertObjectToPropertyValue(content, propertyName,
                            processedPropertyValueObject);
                    if (!Objects.equals(existingProperty, processedPropertyValueObject)) {
                      updatedProperties.put(propertyName, processedPropertyValueObject);
                    }
                  }
                }
            } catch (Exception e) {
                throw new Exception(String.format("Unexpected Exception in document (id : %s, property: %s)\n%s",
                        content.getId(), propertyName, e.getMessage()));
            } finally {
                logger.debug("End mapping of property " + propertyName);
            }
        }
        return updatedProperties;
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
     * Handles a struct setting property. Ultimately, this will be put into the local settings struct builder map.
     * Assumes that the value is a string that can be turned into an array.
     *
     * @param value        the value of the localSettings property.
     * @return true if the local settings was successfully placed in the struct builder map. Else, false.
     */
    private Struct convertToStruct(Object value) {
      StructService structService = contentRepository.getConnection().getStructService();
      Markup structMarkup = MarkupFactory.fromString((String)value);
      Struct valueStruct = structService.fromMarkup(structMarkup);
      return valueStruct;
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
     * @param value        the value of the content's taxonomies. The toString() method of this object should return a
     *                     String in the form of "[/path/of/tag1/,/path/of/tag2/,/path/of/tag3/]"
     * @return True if all specified tags existed and were set properly in the tags map. else, false.
     */
    private List<Content> convertToTaxonomyList(List<String> value, Content taxonomyRootFolder) throws ConfigurationException {
      List<Content> convertedTaxonomies = new ArrayList<>();
      for (String taxonomyString : value) {
          convertedTaxonomies.add(convertToTaxonomy(taxonomyString.trim(), taxonomyRootFolder));
      }
      return convertedTaxonomies;
    }

    /**
     * Retrieves a taxonomy at the specified path and if it exists, adds it into the specified mapping of tags.
     *
     * @param value        the relative path of the tags, starting from the root tag (i.e. Subjects)
     * @return True if the tag was successfully set into the tags map. Else, false.
     */
    private Content convertToTaxonomy(@NonNull String value, Content taxonomyRootFolder) throws ConfigurationException {
      if (taxonomyRootFolder != null) {
        Content taxonomy = contentHelper.establishTax(value, taxonomyRootFolder);
          if (taxonomy != null) {
            return taxonomy;
          } else {
              // We need to error out if the tag does not exist
            throw new NullPointerException(String.format("Could not find Taxonomy with path %s", value));
          }
      } else {
          throw new ConfigurationException("Taxonomy properties have not been configured correctly. " +
                  "Taxonomy values cannot be updated.");
      }
    }

    /**
     * Handles a regular content property. Find's the property descriptor of the specified property (by name) and then
     * handles how that property should be set from the specified property value object. If the property value object
     * succeeds in being converted to the correct type of the found property (by name), it will add the property name
     * and the converted property object to the specified map.
     *
     * @param content             the content for which to set the property
     * @param propertyName        the name of the property to set
     * @param propertyObject the value of the property to set
     * @return true if it succeeds to find the property, convert the object correctly to the expected type, and add it
     * to the map. Else, false.
     */
    protected Object convertObjectToPropertyValue(Content content, String propertyName, Object propertyObject)
            throws Exception {
      CapPropertyDescriptor propertyDescriptor = content.getType().getDescriptor(propertyName);
      if (propertyDescriptor != null) {
          CapPropertyDescriptorType type = propertyDescriptor.getType();
          switch (type) {
              case MARKUP:
                propertyObject = convertToRichText(propertyObject);
                  break;
              case INTEGER:
                propertyObject = Integer.parseInt(propertyObject.toString());
                  break;
              case LINK:
                propertyObject = convertToLink(propertyName, propertyObject);
                break;
              case DATE:
                propertyObject = convertToDate(propertyObject);
                  break;
              case STRUCT:
                propertyObject = convertToStruct(propertyObject);
                  break;
              default:
                  // Default behavior - its some kind of String so do nothing
                  break;
          }
      }
      return propertyObject;
    }

  /**
   * Converts the propertyObject into a Date. If the property object is empty, then returns null.
   * @param propertyObject the object which to convert into a Date object
   *
   * @return an Object, which is a Date
   */
  private Object convertToDate(Object propertyObject) throws ParseException {
    Calendar calendar = Calendar.getInstance();
    String propertyStringValue = propertyObject.toString();

    // If the data for the column is empty - this means that its equal to null/empty
    if (propertyStringValue.isEmpty()) {
      propertyObject = null;
    } else {
      calendar.setTime(dateFormat.parse(propertyStringValue));
      propertyObject = calendar;
    }
    return propertyObject;
  }

  /**
   * Handles converting a property object into rich text. If the passed in value does not contain
   * the proper XML prefix, it will be automatically added, converted to markup, and returned. Else, if it is a String
   * that is properly formatted CoreMedia XML Grammar, then return the String converted to markup. If an Empty String
   * is passed in, returns null.
   *
   * @param value the property value to which to convert into rich text
   * @return The converted rich text value of the object
   */
    private Markup convertToRichText(Object value) {
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
    private Object convertToLink(String propertyName, Object value) throws Exception {
      List<Content> linkContent = new ArrayList<Content>();
      if (value != null) {
          List<String> links = convertObjectStringToStringList(value);
          if (PROPERTY_SUBJECT_TAGS.equals(propertyName)) {
            linkContent.addAll(convertToTaxonomyList(links, subjectTaxonomyRootFolder));
          }
          else if (PROPERTY_LOCATION_TAGS.equals(propertyName)) {
            linkContent.addAll(convertToTaxonomyList(links, locationTaxonomyRootFolder));
          }
          else {
            for (String valueString : links) {
              if (StringUtils.isNumeric(valueString)) {
                int contentId = IdHelper.parseContentId(valueString);
                Content content = contentRepository.getContent(Integer.toString(contentId));
                if (content != null) {
                  linkContent.add(content);
                }
                else {
                  logger.warn(String.format("Could not find content with ID %s, while trying to import " +
                          "it from the CSV header, %s.", contentId, propertyName));
                }
              }
              else {
                throw new Exception("Link is not a valid id: " + valueString);
              }
            }
          }
      }
      return linkContent;
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

    public Content getFirstContent() {
      return firstContent;
    }
}
