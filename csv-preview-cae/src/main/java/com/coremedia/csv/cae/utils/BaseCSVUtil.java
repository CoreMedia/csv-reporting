package com.coremedia.csv.cae.utils;

import com.coremedia.blueprint.base.settings.SettingsService;
import com.coremedia.blueprint.common.contentbeans.CMTaxonomy;
import com.coremedia.cap.common.CapPropertyDescriptor;
import com.coremedia.cap.common.CapPropertyDescriptorType;
import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.common.NoSuchPropertyDescriptorException;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.Version;
import com.coremedia.cap.content.publication.PublicationService;
import com.coremedia.csv.common.CSVConfig;
import com.coremedia.objectserver.beans.ContentBean;
import com.coremedia.objectserver.beans.ContentBeanFactory;
import com.coremedia.objectserver.web.links.LinkFormatter;
import com.coremedia.xml.Markup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.coremedia.csv.common.CSVConstants.*;

/**
 * Base class for CSV utilities.
 */
public abstract class BaseCSVUtil {

  /**
   * The logger for the CSV Exporter Utility class.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(BaseCSVUtil.class);

  /* ------- Constants ------- */
  /**
   * Value to output when an error is encountered.
   */
  private static final String ERROR_VALUE = "Error";

  /**
   * Path separator used when building the taxonomy paths.
   */
  private static final String TAXONOMY_PATH_SEPARATOR = "/";

  /**
   * Version status when the content has been Approved.
   */
  private static final String VERSION_APPROVED = "Approved";

  /**
   * Version status when the content is Checked In.
   */
  private static final String VERSION_CHECKED_IN = "Checked In";

  /**
   * Version status when the content has been Published.
   */
  private static final String VERSION_PUBLISHED = "Published";

  /**
   * Version status when the content has been newly Created.
   */
  private static final String VERSION_CREATED_NEW = "Created";

  /**
   * Version status when the content has been Checked Out.
   */
  private static final String VERSION_CHECKED_OUT = "Checked Out";

  /**
   * The general date format the reporting tool will use when converting dates into Strings.
   */
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

  /* ------- Spring-configured values ------- */
  /**
   * The content repository from which to retrieve content.
   */
  protected ContentRepository contentRepository;

  /**
   * The content bean factory from which to create beans from requested content.
   */
  protected ContentBeanFactory contentBeanFactory;

  /**
   * The settings service from which the settings values of the content beans will be retrieved.
   */
  protected SettingsService settingsService;

  /**
   * The link formatter that will build the URLs for the content beans.
   */
  protected LinkFormatter linkFormatter;

  /**
   * The config class which handles the settings mapping CSV headers to their corresponding content property names.
   */
  protected CSVConfig CSVConfig;

  /**
   * Size of batch for content.
   */
  protected int contentBatchPrefetchSize;

  /**
   * Prefix for the filename, configured via Spring.
   */
  protected String filenamePrefix;

  /**
   * Generates a CSV file based on a list of content ids.
   *
   * @param template the name of the template to use for the CSV file
   * @param request  the HTTP request, used for building content beans
   * @param response the HTTP response, used for building content beans and writing the csv
   * @throws IOException if an error occurs generating the CSV file
   */
  public void generateCSV(int[] contentIds, String template, HttpServletRequest request, HttpServletResponse response) throws IOException {
    ICsvMapWriter csvWriter = null;
    try {
      List<Content> contentList = new ArrayList<>();
      String[] header = CSVConfig.getCSVHeaders(template);
      Map<String, String> propertiesMap = CSVConfig.getReportHeadersToContentProperties(template);
      csvWriter = initializeCSVWriter(createCSVFileName(), header, response);
      for (int contentId : contentIds) {
        Content content = contentRepository.getContent(String.valueOf(contentId));
        if (content != null) {
          contentList.add(content);
          if (contentList.size() == contentBatchPrefetchSize) { // if batch size is reached, write a complete batch
            writeCSV(csvWriter, contentList, header, propertiesMap, request, response);
            contentList.clear();
          }
        }
      }
      if (contentList.size() > 0) { // finish writing last batch
        writeCSV(csvWriter, contentList, header, propertiesMap, request, response);
      }

    } catch (NoSuchPropertyDescriptorException e) {
      LOG.error(e.getMessage());
      throw new RuntimeException("Template configuration is missing or invalid for '" + template + "'");

    } finally {
      closeCSVWriter(csvWriter);
    }

  }

  /**
   * Initializes the CSV writer.
   *
   * @param csvFileName the filename of the CSV to which the content will be written
   * @param header      the column headers for the CSV document of the content properties to write
   * @param response    the http servlet response
   * @return the CSVWriter of the response from the server
   * @throws IOException if an exception occurs initializing the CSV writer
   */
  protected ICsvMapWriter initializeCSVWriter(String csvFileName, String[] header, HttpServletResponse response) throws IOException {
    String headerKey = HTTP_HEADER_CONTENT_DISPOSITION;
    String headerValue = String.format("attachment; filename=\"%s\"",
            csvFileName);
    response.setHeader(headerKey, headerValue);
    response.setCharacterEncoding("UTF-8"); // set the character encoding for internationalized characters

    ICsvMapWriter csvWriter = new CsvMapWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);
    csvWriter.writeHeader(header);
    csvWriter.flush();
    return csvWriter;
  }

  /**
   * Flushes and closes the specified CSV writer.
   *
   * @param csvWriter the writer to close
   * @throws IOException if an exception occurs while closing the CSV writer
   */
  protected void closeCSVWriter(ICsvMapWriter csvWriter) throws IOException {
    if (csvWriter != null) {
      csvWriter.flush();
      csvWriter.close();
    }
  }

  /**
   * Writes a list of content items to a CSV file.
   *
   * @param csvWriter   the writer which writes content to a CSV
   * @param contentList the list of the content which will be written to the CSV
   * @param header      the CSV column headers which will contain the data members of the content written
   * @param request     the HTTP servlet request sent to the handler
   * @param response    the HTTP servlet response sent back to the client
   * @throws IOException if an error occurs writing out the CSV data members
   */
  protected void writeCSV(ICsvMapWriter csvWriter, List<Content> contentList, String[] header,
                          Map<String, String> propertiesMap, HttpServletRequest request,
                          HttpServletResponse response) throws IOException {

    // Prefetch content based on batch size
    Collection<Content> prefetchContentList = contentRepository.withPrefetch(contentList, contentBatchPrefetchSize);
    // Write out every content as a single record in the CSV
    for (Content content : prefetchContentList) {
      try {
        writeCSVRecord(csvWriter, content, header, propertiesMap, request, response);
      } catch (Exception e) {
        LOG.warn("An exception occurred while writing the CSVRecord for " + content, e);
      }
    }
    // Always flush after each session of writing
    csvWriter.flush();
  }

  /**
   * Converts a content object into a single CSV record and writes it to the CSV.
   *
   * @param csvWriter the writer which will write the translated content into the CSV file as a record
   * @param content   the content from which to generate the CSV record
   * @param header    the CSV column headers which determine which members of the content are parsed and written to the
   *                  CSV file
   * @param request   the HTTP request sent to the handler - used for generating links from the content
   * @param response  the HTTP response sent back from the handler - used for generating links from the content
   * @throws IOException if an exception occurs while writing the CSV record
   */
  protected void writeCSVRecord(ICsvMapWriter csvWriter, Content content, String[] header,
                                Map<String, String> propertiesMap, HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
    // Generate the record
    Map<String, String> csvRecord = generateCSVRecord(content, header, propertiesMap, request, response);

    // Write the record to the content
    csvWriter.write(csvRecord, header);
  }

  /**
   * Generates a single CSV record of a specified content object.
   *
   * @param content  the content from which the CSV record will be generated
   * @param header   the CSV column headers which determine which members of the content are parsed and written to the
   *                 CSV record
   * @param request  the HTTP request sent to the handler - used for generating links from the content
   * @param response the HTTP response sent back from the handler - used for generating links from the content
   * @return a map with the keys representing the column headers, and the values representing the data from the
   * content pertaining to their respective header
   */
  protected Map<String, String> generateCSVRecord(Content content, String[] header, Map<String, String> propertiesMap,
                                                  HttpServletRequest request, HttpServletResponse response) {
    // Create the map
    List<String> headerList = Arrays.asList(header);
    Map<String, String> csvRecord = new HashMap<>();

    try {
      // Add static (Metadata) properties
      populateContentMetadataFields(csvRecord, content, headerList, request, response);

      // Add dynamic (Content) properties
      populateContentPropertyFields(csvRecord, content, headerList, propertiesMap);

      // Add custom fields (None by default)
      populateCustomPropertyFields(csvRecord, content, headerList);

      // Update record status to success if all fields were successfully set
      csvRecord.put(COLUMN_STATUS, "success");
    } catch (Exception e) {
      LOG.warn(e.toString());
      handleBadRecord(content, csvRecord);
    }
    return csvRecord;
  }

  /**
   * Populates the CSV record with metadata from the specified content. Metadata must be treated differently than
   * content properties, as metadata are static, none editable properties of the content. Each metadata property must
   * be specifically requested, and cannot be done generically.
   *
   * @param csvRecord  the CSV record to which to populate metadata
   * @param content    the content from which the metadata will be requested and set into the CSV record
   * @param headerList the list of headers which determines which metadata is added to the CSV record and which
   *                   columns will be present in the CSV
   * @param request    the HTTP request sent to the handler - used for generating links from the content
   * @param response   the HTTP response sent back from the handler - used for generating links from the content
   */
  protected void populateContentMetadataFields(Map<String, String> csvRecord, Content content,
                                               List<String> headerList, HttpServletRequest request,
                                               HttpServletResponse response) {
    ContentBean bean = contentBeanFactory.createBeanFor(content);
    String metadataProperty;
    if (headerList.contains(COLUMN_ID)) {
      metadataProperty = getContentIdString(content);
      csvRecord.put(COLUMN_ID, metadataProperty);
    }
    if (headerList.contains(COLUMN_NAME)) {
      metadataProperty = content.getName();
      csvRecord.put(COLUMN_NAME, metadataProperty);
    }
    if (headerList.contains(COLUMN_PATH)) {
      metadataProperty = content.getPath();
      csvRecord.put(COLUMN_PATH, metadataProperty);
    }
    if (headerList.contains(COLUMN_URL)) {
      metadataProperty = getContentURLString(bean, request, response);
      csvRecord.put(COLUMN_URL, metadataProperty);
    }
    if (headerList.contains(COLUMN_TYPE)) {
      metadataProperty = content.getType().getName();
      csvRecord.put(COLUMN_TYPE, metadataProperty);
    }
    if (headerList.contains(COLUMN_CREATION_DATE)) {
      Calendar creationDate = content.getCreationDate();
      metadataProperty = creationDate != null ? dateFormat.format(creationDate.getTime()) : "";
      csvRecord.put(COLUMN_CREATION_DATE, metadataProperty);
    }
    if (headerList.contains(COLUMN_CREATED_BY)) {
      metadataProperty = content.getCreator().getName();
      csvRecord.put(COLUMN_CREATED_BY, metadataProperty);
    }
    if (headerList.contains(COLUMN_LAST_MODIFICATION_DATE)) {
      Calendar lastModDate = content.getModificationDate();
      metadataProperty = lastModDate != null ? dateFormat.format(lastModDate.getTime()) : "";
      csvRecord.put(COLUMN_LAST_MODIFICATION_DATE, metadataProperty);
    }
    if (headerList.contains(COLUMN_LAST_MODIFIED_BY)) {
      metadataProperty = content.getModifier().getName();
      csvRecord.put(COLUMN_LAST_MODIFIED_BY, metadataProperty);
    }
    if (headerList.contains(COLUMN_VERSION_STATUS)) {
      metadataProperty = getContentVersionStatusString(content);
      csvRecord.put(COLUMN_VERSION_STATUS, metadataProperty);
    }
    if (headerList.contains(COLUMN_PUBLICATION_DATE)) {
      PublicationService publicationService = contentRepository.getPublicationService();
      Calendar publicationDate = publicationService.getPublicationDate(content);
      String creationDateStr = publicationDate != null ? dateFormat.format(publicationDate.getTime()) : "";
      csvRecord.put(COLUMN_PUBLICATION_DATE, creationDateStr);
    }
  }

  /**
   * Gets the ID of the content and converts it to a String.
   *
   * @param content the content from which to get the ID
   * @return a String representing the value of the ID of the content
   */
  protected String getContentIdString(Content content) {
    int id = IdHelper.parseContentId(content.getId());
    return Integer.toString(id);
  }

  /**
   * Gets the URL of the specified content bean.
   *
   * @param bean     the content bean from which to generate the URL
   * @param request  the HTTP request sent to the handler
   * @param response the HTTP response sent back from the handler
   * @return a String representing the URL of the specified content
   */
  protected String getContentURLString(ContentBean bean, HttpServletRequest request,
                                       HttpServletResponse response) {
    String link = "";
    try {
      link = linkFormatter.formatLink(bean, null, request, response, false);
    } catch (Exception e) {
      LOG.warn("An error occurred while trying to build the URL for content ID: {}",
              getContentIdString(bean.getContent()), e);
    }
    return link;
  }

  /**
   * Gets the content version status and converts it ot the appropriate String.
   *
   * @param content the content from which to get the version status
   * @return a String representing the version status of the content
   */
  protected String getContentVersionStatusString(Content content) {
    PublicationService publicationService = contentRepository.getPublicationService();
    String status = "";
    if (content.isCheckedOut()) {
      status = VERSION_CHECKED_OUT;
    } else {
      Version version = content.getWorkingVersion();
      if (version == null) {
        version = content.getCheckedInVersion();
      }
      if (version != null) {
        status = VERSION_CHECKED_IN;
        if (publicationService.isApproved(version)) {
          status = VERSION_APPROVED;
        }
        if (publicationService.isPublished(version)) {
          status = VERSION_PUBLISHED;
        }
      }

      if (publicationService.isNew(content)) {
        status = VERSION_CREATED_NEW;
      }
    }
    return status;
  }

  /**
   * Populates the CSV record with properties from the specified content. Properties are editable values of content
   * which are stored in a map of the content itself. Because of this, properties can be handled more generically
   * and dynamically when accessing them.
   *
   * @param csvRecord  the CSV record to which to populate the properties of the content
   * @param content    the content from which the property values will be parsed
   * @param headerList the list of headers which determines which metadata is added to the CSV record and which
   *                   columns will be present in the CSV
   */
  protected void populateContentPropertyFields(Map<String, String> csvRecord, Content content,
                                               List<String> headerList, Map<String, String> propertiesMap) {
    // Iterate through all of the headers to parse through all of the specified properties of the content
    for (String headerField : headerList) {
      String propertyName = propertiesMap.get(headerField);
      Object property;
      if (propertyName != null) {
        // We need to check if this is a local setting value. While local settings can be handled generically,
        // they are different then regular properties. The localSettings struct needs to be parsed for the
        // specified local settings variable
        if (propertyName.startsWith(PROPERTY_PREFIX_LOCAL_SETTINGS)) {
          property = evaluateLocalSettingsVariable(content, propertyName);
        }
        // Otherwise we can treat this as a regular property and parse the value from the content's properties
        else {
          property = evaluateContentProperty(content, propertyName);
        }
        csvRecord.put(headerField, property.toString().trim());
      }
    }
  }

  /**
   * Populates the CSV record with custom properties from the specified content. Custom properties are not innate
   * properties of content and involve some calculation based on other content properties to derive them. This
   * functionality is left up to subclasses (which is determined based on the request) to implement as there are no
   * custom properties to be calculated by default.
   *
   * @param csvRecord  the CSV record to which to populate the properties of the content
   * @param content    the content from which the property values will be parsed
   * @param headerList the list of headers which determines which metadata is added to the CSV record and which
   *                   columns will be present in the CSV
   */
  protected abstract void populateCustomPropertyFields(Map<String, String> csvRecord, Content content,
                                                       List<String> headerList);

  /**
   * Gets the property value of the specified property name from the specified content.
   *
   * @param content      the content from which to get the property
   * @param propertyName the name of the property value to get from the content
   * @return The value of the property from the content. If the property is null, returns an empty String.
   */
  protected Object getContentProperty(Content content, String propertyName) {
    Object property = content.get(propertyName);
    if (property == null) {
      property = "";
    }
    return property;
  }

  /**
   * Determines the value of a local setting variable.
   *
   * @param content      the content from which to determine the specified local setting variable's value
   * @param propertyName the name/path of the local setting variable. NOTE: This should start with the string
   *                     "localSettings" as that is the initial path of any local setting variable
   * @return the value of the local setting variable from the content. If there is no value/local setting variable,
   * returns an empty string. If the request local setting is for the content's product Ids, they are converted into
   * a more readable format
   */
  protected String evaluateLocalSettingsVariable(Content content, String propertyName) {
    try {
      // First need to split off the "localSettings" from the path of the local setting variable
      propertyName = propertyName.substring(PROPERTY_PREFIX_LOCAL_SETTINGS.length());
      String[] values = propertyName.split("\\.");

      // Parse the localSettings struct to get the value at the variable
      Object value = getLocalSettingValue(values, content);

      // If the value doesn't exist - convert to empty String
      if (value == null) {
        value = "";
      }
      return value.toString();
    } catch (Exception e) {
      LOG.warn("Error while evaluating local settings property " + propertyName + " for " + content, e);
      return ERROR_VALUE;
    }
  }

  /**
   * Iterates through the content's settings to determine the value of specified local setting.
   *
   * @param settingPathSegments an array representing the navigational path segments of the variable through the local
   *                            settings of the specified content
   * @param content             the content from which to get the value of the specified local setting variable
   * @return the value of the local setting variable from the content
   */
  private Object getLocalSettingValue(String[] settingPathSegments, Content content) {
    // Get the structure/value of the first local setting variable in the segments
    Object result = settingsService.settingAsMap(settingPathSegments[0], String.class, Object.class, content);

    // If the local setting variable is a direct value and not nested within the settings, then there will only be
    // one segment. In this event, we can return the value at the segment
    if (settingPathSegments.length == 1) {
      return result;
    } else {
      // Otherwise the value is nested in a potential series of maps. We iterate through the maps until we reach
      // the final part of that segment. The end of the segment path will contain the value
      Map map = (Map) result;
      for (int i = 1; i < settingPathSegments.length; i++) {
        result = map.get(settingPathSegments[i]);
        if ((i + 1) < settingPathSegments.length) {
          map = (Map) result;
        }
      }
      return result;
    }
  }

  /**
   * Determines the value of the specified content property.
   *
   * @param content      the content from which to determine the value of the specified property
   * @param propertyName the name of the property of which to get the value from the specified content
   * @return the value of the property in the content. If the content does not contain a property descriptor, then
   * returns an empty String.
   */
  protected Object evaluateContentProperty(Content content, String propertyName) {

    // Set the default value of the property to empty String
    Object property = "";

    // If we have the property, make sure we parse it correctly. Else, skip the property. If there is no property
    // descriptor - there is no need to warn or error, as different content types will have different properties,
    // and a single content object may not contain all of the requested properties
    CapPropertyDescriptor propertyDescriptor = content.getType().getDescriptor(propertyName);

    if (propertyDescriptor != null) {
      CapPropertyDescriptorType type = propertyDescriptor.getType();
      switch (type) {
        case LINK:
          property = evaluateLinkProperty(content, propertyName);
          break;
        case MARKUP:
          property = evaluateMarkupProperty(content, propertyName);
          break;
        case DATE:
          property = getContentProperty(content, propertyName);
          if (property instanceof Calendar) {
            property = dateFormat.format(((Calendar) property).getTime());
          }
          break;
        default:
          property = getContentProperty(content, propertyName);
      }
    }
    return property;
  }

  /**
   * Properly evaluates a Link property. Contains special handling cases if the link is for the Subject Taxonomy
   * property.
   *
   * @param content      the content from which to evaluate the link property
   * @param propertyName the name of the link property
   * @return the value of the specified link property
   */
  private Object evaluateLinkProperty(Content content, String propertyName) {
    Object property;
    if (PROPERTY_SUBJECT_TAGS.equals(propertyName)) {
      property = evaluateTagProperty(content, propertyName);
    } else {
      property = evaluateAssociatedProperty(content, propertyName);
    }
    return property;
  }

  /**
   * Properly evaluates a Link property which is a Tag list.
   *
   * @param content      the content from which the tags will be evaluated
   * @param propertyName the name of the tag list property of the content
   * @return a List of Strings, representing the tag path of each taxonomy in the specified property
   */
  private List<String> evaluateTagProperty(Content content, String propertyName) {
    List<Content> tags = ((List<Content>) content.get(propertyName));
    List<String> tagIds = new ArrayList<>();
    for (Content tag : tags) {
      ContentBean tagBean = contentBeanFactory.createBeanFor(tag);
      if (tagBean instanceof CMTaxonomy) {
        List<? extends CMTaxonomy> taxonomyPathList = ((CMTaxonomy) tagBean).getTaxonomyPathList();
        StringBuilder prefixedCategoryPath = new StringBuilder();

        // append category hierarchy with content names as segments
        for (CMTaxonomy taxonomyPathSegment : taxonomyPathList) {
          prefixedCategoryPath.append(TAXONOMY_PATH_SEPARATOR).append(
                  taxonomyPathSegment.getContent().getName());
        }
        prefixedCategoryPath.append(TAXONOMY_PATH_SEPARATOR);
        tagIds.add(prefixedCategoryPath.toString());
      } else {
        LOG.error(String.format("One of the documents inside of the tag list, %s, in content with Id, %s, was" +
                " null or not a tag.", propertyName, getContentIdString(content)));
      }
    }
    return tagIds;
  }

  /**
   * Properly evaluates a associated property and return the Id.
   *
   * @param content      the content from which the associated property will be evaluated
   * @param propertyName the name of the associated property of the content
   * @return a List of Strings, representing the ID of each associated property
   */
  private List<String> evaluateAssociatedProperty(Content content, String propertyName) {
    List<Content> associateds = ((List<Content>) content.get(propertyName));
    List<String> associatedIds = new ArrayList<>();
    for (Content associated : associateds) {
      associatedIds.add(getContentIdString(associated));
    }
    return associatedIds;
  }

  /**
   * Properly evaluates a Markup property.
   *
   * @param content      the content from which to evaluate the Markup property
   * @param propertyName the name of the Markup property
   * @return the value of the specified Markup property
   */
  protected Object evaluateMarkupProperty(Content content, String propertyName) {
    Object property;
    property = getContentProperty(content, propertyName);

    // We check if its a Markup object here to validate that we got the property.
    if (property instanceof Markup) {
      property = property.toString().trim();
    }
    // At this point, we are guaranteed to have a String
    // Remove carriage returns so that our CSV doesn't error when imported
    property = ((String) property).replaceAll("\n", "");
    property = ((String) property).replaceAll("\r", "");
    return property;
  }


  /**
   * Generates a CSV filename.
   *
   * @return a string that will represent the CSV filename
   */
  protected String createCSVFileName() {
    return filenamePrefix + dateFormat.format(new Date()) + ".csv";
  }

  /**
   * Scrubs csvRecord and sets the content id and status to fail.
   *
   * @param content the content on which the failure occurred
   * @param csvRecord the record for the failed content
   */
  private void handleBadRecord(Content content, Map<String, String> csvRecord) {
    csvRecord.clear();
    csvRecord.put(COLUMN_ID, getContentIdString(content));
    csvRecord.put(COLUMN_STATUS, "fail");
  }

  /* ------- Spring-configured value setters ------- */

  /**
   * Sets the settings service.
   *
   * @param settingsService the settings service to set
   */
  @Required
  public void setSettingsService(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  /**
   * Sets the content bean factory.
   *
   * @param contentBeanFactory the content bean factory to set
   */
  @Required
  public void setContentBeanFactory(ContentBeanFactory contentBeanFactory) {
    this.contentBeanFactory = contentBeanFactory;
  }

  /**
   * Sets the content repository.
   *
   * @param contentRepository the content repository to set
   */
  @Required
  public void setContentRepository(ContentRepository contentRepository) {
    this.contentRepository = contentRepository;
  }

  /**
   * Sets the link formatter.
   *
   * @param linkFormatter the link formatter to set
   */
  @Required
  public void setLinkFormatter(LinkFormatter linkFormatter) {
    this.linkFormatter = linkFormatter;
  }

  /**
   * Sets the CSV configuration handler.
   *
   * @param CSVConfig the CSVConfig to set
   */
  @Required
  public void setCSVConfig(CSVConfig CSVConfig) {
    this.CSVConfig = CSVConfig;
  }

  /**
   * Sets the file name prefix for generated files.
   *
   * @param filenamePrefix the file name prefix for generated files
   */
  @Required
  public void setFilenamePrefix(String filenamePrefix) {
    this.filenamePrefix = filenamePrefix;
  }

  /**
   * Sets the batch size for fetching content.
   *
   * @param contentBatchPrefetchSize the batch size for fetching content
   */
  @Required
  public void setContentBatchPrefetchSize(int contentBatchPrefetchSize) {
    this.contentBatchPrefetchSize = contentBatchPrefetchSize;
  }
}
