package com.coremedia.csv.importer;

import com.coremedia.cap.common.CapPropertyDescriptorType;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructBuilder;
import com.coremedia.xml.Markup;
import com.coremedia.xml.MarkupUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
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

    /**
     * Looks for a struct within the for the given property name. If it can't be found in the given structBuilderMap,
     * a new structBuilder will be created for an empty struct and added it to the structBuilderMap.
     * given structValueKey as key
     *
     * @param structBuilderMap The struct builder map with all the child structs of the root structBuilder 'localSettings'
     * @param structValueKey   The key for the setting
     * @return The structBuilder found in the structBuilderMap for the given key 'structValueKey' or a freshly created
     * structBuilder for this occasion.
     */
    public StructBuilder lookUpStructBuilderMap(Map<String, StructBuilder> structBuilderMap, String structValueKey) {
        //split struct key from property key
        if (StringUtils.countMatches(structValueKey, ".") == 1) {
            String structKey = StringUtils.substringBeforeLast(structValueKey, ".");
            if (structBuilderMap.get(structKey) != null) {
                return structBuilderMap.get(structKey);
            } else {
                Struct newChildStruct = getEmptyStruct();
                StructBuilder newChildStructBuilder = newChildStruct.builder();
                newChildStructBuilder.build();
                structBuilderMap.put(structKey, newChildStructBuilder);
                return newChildStructBuilder;
            }
        }
        logger.error("At this time, only simple top level settings or first level structs are allowed (e.g. author or sharepoint.author). Please check the configuration!");
        return null;
    }

    /**
     * Adds a property to a CoreMedia Struct object. Depending on the given ConfigTargetType, the values must be treated differently.
     *
     * @param structbuilder      The StructBuilder object referencing the struct
     * @param type               The ConfigTargetType value identifying the target property type
     * @param targetPropertyName The name of the struct property to add
     * @param value              The value to add to the struct field
     * @return The structbuilder referencing the struct with the newly added properties
     */
    public StructBuilder addItemToStructBuilder(StructBuilder structbuilder, CapPropertyDescriptorType type,
                                                String targetPropertyName, Object value) {
        return addItemToStructBuilder(structbuilder, type, targetPropertyName, value, false);
    }

    /**
     * Adds a property to a CoreMedia Struct object. Depending on the given ConfigTargetType, the values must be treated differently.
     *
     * @param structbuilder      The StructBuilder object referencing the struct
     * @param type               The ConfigTargetType value identifying the target property type
     * @param targetPropertyName The name of the struct property to add
     * @param value              The value to add to the struct field
     * @param overrideExisting   Triggers, whether list objects should be replaced completely by the value (TRUE), or if the value should added to the existing list values (FALSE)
     * @return The structbuilder referencing the struct with the newly added properties
     */
    public StructBuilder addItemToStructBuilder(StructBuilder structbuilder, CapPropertyDescriptorType type,
                                                String targetPropertyName, Object value, boolean overrideExisting) {
        String cleanPropertyName = StringUtils.substringAfterLast(targetPropertyName, ".");
        Struct struct = structbuilder.build();

        switch (type) {
            case STRING:
                if (value instanceof String) {
                    structbuilder = declareOrUpdateString(structbuilder, struct, cleanPropertyName, 255, (String) value);
                    break;
                }
                if (value instanceof Integer) {
                    structbuilder = declareOrUpdateString(structbuilder, struct, cleanPropertyName, 255, "" + value);
                    break;
                }
                if (value instanceof List) {
                    structbuilder = declareOrUpdateStrings(structbuilder, struct, cleanPropertyName, 255, (List<String>) value, overrideExisting);
                    break;
                }
                logger.error("Unexpected property type (" + value.getClass().getName() + ") for target type STRING in settings object. Property (" + targetPropertyName + ") will be ignored.");
                break;
            case BOOLEAN:
                if (value instanceof Boolean) {
                    structbuilder = structbuilder.declareBoolean(cleanPropertyName, (Boolean) value);
                    break;
                }
                logger.error("Unexpected property type (" + value.getClass().getName() + ") for target type BOOLEAN in settings object. Property (" + targetPropertyName + ") will be ignored.");
                break;
            case DATE:
                if (value instanceof Calendar) {
                    structbuilder = structbuilder.declareDate(cleanPropertyName, (Calendar) value);
                    break;
                }
                logger.error("Unexpected property type (" + value.getClass().getName() + ") for target type DATE in settings object. Property (" + targetPropertyName + ") will be ignored.");
                break;
            case MARKUP:
                if (value instanceof String) {
                    structbuilder = declareOrUpdateString(structbuilder, struct, cleanPropertyName, 255, (String) value);
                    break;
                }
                if (value instanceof Integer) {
                    structbuilder = declareOrUpdateString(structbuilder, struct, cleanPropertyName, 255, "" + value);
                    break;
                }
                if (value instanceof List) {
                    structbuilder = declareOrUpdateStrings(structbuilder, struct, cleanPropertyName, 255, (List<String>) value, overrideExisting);
                    break;
                }
                if (value instanceof Markup) {
                    structbuilder = declareOrUpdateString(structbuilder, struct, cleanPropertyName, 255, MarkupUtil.asPlainText((Markup) value));
                    break;
                }
                logger.error("Unexpected property type (" + value.getClass().getName() + ") for target type RICHTEXT in settings object. Property (" + targetPropertyName + ") will be ignored.");
                break;
            case LINK:
                //TODO Convert Strings to CMTags and attach them as link objects!
                if (value instanceof String) {
                    structbuilder = declareOrUpdateString(structbuilder, struct, cleanPropertyName, 255, (String) value);
                    break;
                }
                if (value instanceof List) {
                    structbuilder = declareOrUpdateStrings(structbuilder, struct, cleanPropertyName, 255, (List<String>) value, overrideExisting);
                    break;
                }
                logger.error("Unexpected property type (" + value.getClass().getName() + ") for target type TAXONOMY in settings object. Property (" + targetPropertyName + ") will be ignored.");
                break;
            default: //do nothing
                logger.error("Unavailable target type (" + type + ") for in settings object. Property (" + targetPropertyName + ") will be ignored.");
        }

        return structbuilder;
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
     * Helper method to declare or update a String-LIst property in a struct
     *
     * @param structBuilder The StructBuilder object referencing the struct
     * @param struct        The struct object of the StructBuilder
     * @param propertyName  The name of the struct property to add
     * @param value         The value to add to the struct field
     * @return The StructBuilder with the added or updated string-list value
     */
    private StructBuilder declareOrUpdateStrings(StructBuilder structBuilder, Struct struct, String propertyName, int length, List<String> value, boolean overrideExisting) {
        if (StringUtils.isBlank(propertyName)) {
            logger.error("Property name can't be empty. Skip adding or updating property. Returning original StringBuilder.");
            return structBuilder;
        }
        if (struct.get(propertyName) != null) {
            Set<String> newValues = new LinkedHashSet<>(value);
            if (!overrideExisting) {
                List<String> existingValues = (List<String>) struct.get(propertyName);
                newValues.addAll(existingValues);
            } else {
                structBuilder.remove(propertyName);
                structBuilder.build();
                //convert to set to remove duplicates
                structBuilder.declareStrings(propertyName, length, new ArrayList<>(new LinkedHashSet<>(value)));
                return structBuilder;
            }
            //convert to set to remove duplicates
            structBuilder.set(propertyName, new ArrayList<>(newValues));
            return structBuilder;
        }
        return structBuilder.declareStrings(propertyName, length, value);
    }


    /**
     * Helper method to read string Lists or String values in a CoreMedia Struct
     *
     * @param settingsKey The key for the settings object in the struct
     * @param content     The Content object to checkl the settings
     * @return A list of strings containing the string values of the struct property
     */
    public List<String> getValueForSetting(String settingsKey, Content content) {
        if (settingsKey == null || StringUtils.countMatches(settingsKey, ".") != 1) {
            logger.error("At this time, only setting properties with level depth 2 are supported (e.g. sharepoint.productKeys)");
            return null;
        }
        List<String> result = new ArrayList<>();
        String structName = StringUtils.substringBefore(settingsKey, ".");
        String propertyName = StringUtils.substringAfter(settingsKey, ".");
        Struct localSettings = (Struct) content.getProperties().get("localSettings");
        if (localSettings == null) {
            logger.debug("Attempt to read a content item with no localSettings (id: " + content.getId() + ", title: " + content.getName() + ", path: " + content.getPath() + ")");
            return result;
        }
        if (localSettings.getProperties() == null) {
            logger.debug("Attempt to read localSettings properties that are not set(id: " + content.getId() + ", title: " + content.getName() + ", path: " + content.getPath() + ")");
            return result;
        }
        if (localSettings.getProperties().containsKey(structName)) {
            Struct struct = (Struct) localSettings.getProperties().get(structName);
            if (struct.getProperties().containsKey(propertyName)) {
                Object property = struct.getProperties().get(propertyName);
                if (property instanceof List) {
                    for (String propertyValue : (List<String>) property) {
                        result.add(propertyValue);
                    }
                } else {
                    if (property instanceof String || property instanceof Boolean || property instanceof Integer) {
                        result.add((String) property);
                    } else {
                        if (property instanceof Calendar) {
                            result.add(((Calendar) property).getTime().toString());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Helper method to read string Lists or String values in a CoreMedia Struct
     *
     * @param settingsKey The key for the settings object in the struct
     * @param content     The Content object to checkl the settings
     * @return A list of strings containing the string values of the struct property
     */
    public String getValueStringForSetting(String settingsKey, @NonNull Content content) {
        if (content == null) {
            logger.error("Content is not allowed to be null");
            return null;
        }
        if (settingsKey == null || (settingsKey != null && StringUtils.countMatches(settingsKey, ".") != 1)) {
            logger.error("At this time, only setting properties with level depth 2 are supported (e.g. sharepoint.productKeys)");
            return null;
        }
        String structName = StringUtils.substringBefore(settingsKey, ".");
        String propertyName = StringUtils.substringAfter(settingsKey, ".");
        Struct localSettings = (Struct) content.getProperties().get("localSettings");
        if (localSettings != null && localSettings.getProperties() != null && localSettings.getProperties().containsKey(structName)) {
            Struct struct = (Struct) localSettings.getProperties().get(structName);
            if (struct.getProperties().containsKey(propertyName)) {
                Object property = struct.getProperties().get(propertyName);
                if (property instanceof String) {
                    return (String) property;
                } else {
                    logger.error("Could not determine String value for settings property (" + settingsKey + "). Return Empty String.");
                }
            }
        }
        return StringUtils.EMPTY;
    }


    public ContentRepository getContentRepository() {
        return contentRepository;
    }
}
