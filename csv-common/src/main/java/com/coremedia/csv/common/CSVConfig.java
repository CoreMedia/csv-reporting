package com.coremedia.csv.common;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.struct.Struct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVConfig {


  /**
   * Path to the document storing the reporting settings.
   */
  public static final String DEFAULT_SETTINGS_PATH = "/Settings/Options/Settings/ReportingSettings";

  /**
   * Property of a CMSettings document that stores the settings struct
   */
  private static final String SETTINGS_PROPERTY = "settings";

  /**
   * Settings property that stores the set of available templates
   */
  private static final String TEMPLATES_PROPERTY = "templates";

  /**
   * Settings property that stores the headers for CSV reporting
   */
  private static final String HEADERS_LIST_NAME = "csvHeaders";

  /**
   * Settings property that stores the property map for CSV reporting
   */
  private static final String PROPERTIES_STRUCT_NAME = "csvProperties";

  /**
   * The content repository from which to retrieve content.
   */
  private final ContentRepository contentRepository;

  /**
   * The path to the reporting settings document
   */
  private final String settingsPath;

  /**
   * Creates CSV configuration backed by the reporting settings document.
   *
   * @param contentRepository repository used to load the settings document
   * @param settingsPath path to the reporting settings document
   */
  public CSVConfig(ContentRepository contentRepository, String settingsPath) {
    this.contentRepository = contentRepository;
    this.settingsPath = settingsPath;
  }

  /**
   * Get the header columns in the CSV. Used by the CSV writer to determine which properties of beans are needed when
   * writing.
   *
   * @param templateName name of the reporting template
   * @return the header columns in the CSV
   */
  public String[] getCSVHeaders(String templateName) {
    Struct settingsStruct = getReportingSettings(templateName);
    if(settingsStruct != null) {
      List<String> headers = settingsStruct.getStrings(HEADERS_LIST_NAME);
      return headers.toArray(new String[0]);
    }
    return new String[0];
  }

  /**
   * Get a relational map consisting of the names of the CSV headers and their corresponding content property names.
   *
   * @param templateName name of the reporting template
   * @return a map of CSV headers and their corresponding content property names
   */
  public Map<String, String> getReportHeadersToContentProperties(String templateName) {
    Map<String, String> stringsMap = new HashMap<>();
    Struct settingsStruct = getReportingSettings(templateName);
    if(settingsStruct != null) {
      Struct propertiesStruct = settingsStruct.getStruct(PROPERTIES_STRUCT_NAME);
      if (propertiesStruct != null) {
        Map<String, Object> propertiesMap = propertiesStruct.toNestedMaps();
        for (String k : propertiesMap.keySet()) {
          stringsMap.put(k, propertiesMap.get(k).toString());
        }
      }
    }
    return stringsMap;
  }

  /**
   * Gets the reporting settings object.
   *
   * @return the reporting settings object
   */
  private Struct getReportingSettings(String templateName) {
    Content settingsDoc = contentRepository.getChild(settingsPath);
    Struct settings = settingsDoc.getStruct(SETTINGS_PROPERTY);
    Struct templates = settings.getStruct(TEMPLATES_PROPERTY);
    return templates.getStruct(templateName);
  }

}
