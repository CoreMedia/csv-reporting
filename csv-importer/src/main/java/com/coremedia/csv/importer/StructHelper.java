package com.coremedia.csv.importer;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import java.util.*;

/**
 * Helper class to handle coremedia structs
 */
public class StructHelper {

    private ContentRepository contentRepository;
    private Logger logger;

    public StructHelper(ContentRepository contentRepository, Logger logger) {
        this.contentRepository = contentRepository;
        this.logger = logger;
    }

    /**
     * Returns an empty CoreMedia struct object.
     *
     * @return An empty CoreMedia struct object.
     */
    public Struct getEmptyStruct() {
        return getContentRepository().getConnection().getStructService().createStructBuilder().build();
    }

    public StructBuilder updateLocalSettings(Content content, String propertyName, Object value, Object currentValue, StructBuilder localSettingsStructBuilder) {
      if (currentValue instanceof Boolean) { // Handle Boolean values
        localSettingsStructBuilder.set(propertyName, Boolean.parseBoolean((String) value));
      } else if (currentValue instanceof Integer) { // Handle Integer values
        localSettingsStructBuilder.set(propertyName, Integer.parseInt((String) value));
      } else if (currentValue instanceof String) { // Handle Integer values
        localSettingsStructBuilder.set(propertyName, value);
      }
      // Handle Redirects Struct
      else if (propertyName.equals("redirects")) {
        ArrayList<Struct> updatedRedirects = new ArrayList<>();
        String parsedValue = (String) value;
        if (!parsedValue.isEmpty() && parsedValue.startsWith("[") && parsedValue.endsWith("]")) {
          parsedValue = parsedValue.trim().substring(1, parsedValue.length() - 1); // Remove square brackets from string
          String[] structList = parsedValue.isBlank()?new String[0]:parsedValue.split("(?<=}),"); // Separate into an array based on Structs
          for (String struct : structList) {
            StructBuilder newSB = content.getRepository().getConnection().getStructService().createStructBuilder();
            if (struct.trim().startsWith("Struct{") && struct.endsWith("}")) {
              String[] StructKVPairs = struct.replace("Struct{", "").replace("}", "").split(",");
              for (String pair : StructKVPairs) {
                String[] keyAndValue = pair.split("=");
                if (keyAndValue.length == 2) {
                  String newStructPropertyKey = keyAndValue[0].trim();
                  String newStructPropertyValue = keyAndValue[1].trim();
                  if (newStructPropertyKey.equals("id")) {
                    newSB.set("id", newStructPropertyValue);
                  } else if (newStructPropertyKey.equals("permanent")) {
                    if (newStructPropertyValue.equalsIgnoreCase("true") || newStructPropertyValue.equalsIgnoreCase("false")) {
                      newSB.set("permanent", Boolean.parseBoolean(newStructPropertyValue));
                    }
                  } else {
                    return localSettingsStructBuilder;
                  }
                }
              }
              updatedRedirects.add(newSB.build());
            } else {
              return localSettingsStructBuilder;
            }
          }
          localSettingsStructBuilder.set(propertyName, updatedRedirects);
        }
      }
      return localSettingsStructBuilder;
    }

    /**
     * Helper method to declare or update a String property in a struct
     *
     * @param structBuilder The StructBuilder object referencing the struct
     * @param struct        The struct object of the StructBuilder
     * @param propertyName  The name of the struct property to add
     * @param value         The value to add to the struct field
     * @return The StructBuilder with the added or updated string value
     */
    private StructBuilder declareOrUpdateString(StructBuilder structBuilder, Struct struct, String propertyName, int length, String value) {
        if (StringUtils.isBlank(propertyName)) {
            logger.error("Property name can't be empty. Skip adding or updating property. Returning original StringBuilder.");
            return structBuilder;
        }
        if (struct.get(propertyName) != null) {
            Map<String, Object> newProperties = new HashMap<>();
            Map<String, Object> properties = struct.getProperties();
            newProperties.putAll(properties);
            newProperties.put(propertyName, value);
            structBuilder.setAll(newProperties);
            return structBuilder;
        }

        return structBuilder.declareString(propertyName, length, value);
    }

    /**
     * Helper method to read string Lists or String values in a CoreMedia Struct
     *
     * @param settingsKey The key for the settings object in the struct
     * @param content     The Content object to checkl the settings
     * @return A list of strings containing the string values of the struct property
     */
    public Object getValueForSetting(String settingsKey, Content content) {
        if (settingsKey == null) {
            logger.error("Property name cannot be null");
            return null;
        }
        String structName = StringUtils.substringBefore(settingsKey, ".");
        Struct localSettings = (Struct) content.getProperties().get("localSettings");
        if (localSettings == null) {
            logger.debug("Attempt to read a content item with no localSettings (id: " + content.getId() + ", title: " + content.getName() + ", path: " + content.getPath() + ")");
            return null;
        }
        if (localSettings.getProperties() == null) {
            logger.debug("Attempt to read localSettings properties that are not set(id: " + content.getId() + ", title: " + content.getName() + ", path: " + content.getPath() + ")");
            return null;
        }
        if (localSettings.getProperties().containsKey(structName)) {
              return localSettings.getProperties().get(structName);
            }
        return null;
    }

    public ContentRepository getContentRepository() {
        return contentRepository;
    }
}
