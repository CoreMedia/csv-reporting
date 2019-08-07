package com.coremedia.csv.importer;

import com.coremedia.cap.content.Content;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertyValueObjectProcessor that removes duplicate commerce references and WARNs about invalid references.
 */
public class CommerceReferencesProcessor extends CommerceReferenceProcessor {

    public CommerceReferencesProcessor(Logger logger) {
        super(logger);
    }

    @Override
    public Object process(Content content, String propertyName, Object propertyValueObject) {
        if(propertyValueObject == null) {
            return propertyValueObject;
        }
        List<String> values = CSVParserHelper.convertObjectStringToStringList(propertyValueObject);
        if(values == null || values.size()==0) {
            return propertyValueObject;
        }
        // validate source values
        List<String> processedValues = new ArrayList<>();
        for (String value : values) {
            if (processedValues.contains(value)) {
                logger.info("Removing duplicate value '" + value + "' for " + content + " and property " + propertyName);
                continue;
            }
            if (!isValid(value)) {
                // only log WARNing for invalid value (opposed to removing them also), which would still allow us to fix it in a subsequent run
                logger.warn("Encountered invalid value '" + value + "' for " + content + " and property " + propertyName + ", but leaving it");
            }
            processedValues.add(value);
        }
        // transform String List to expected String format
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < processedValues.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(processedValues.get(i));
        }
        sb.append(']');
        String result = sb.toString();
        return result;
    }

}
