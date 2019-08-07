package com.coremedia.csv.importer;

import com.coremedia.cap.content.Content;

/**
 * Interface to manipulate/validate property value object.
 */
public interface PropertyValueObjectProcessor {
    /**
     * Processes and potentially manipulates the given propertyValueObject.
     *
     * @param content The content for which to process the property
     * @param propertyName Name of property
     * @param propertyValueObject Value of property
     * @return Resulting value
     */
    Object process(Content content, String propertyName, Object propertyValueObject);
}
