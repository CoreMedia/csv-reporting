package com.coremedia.csv.importer;

import com.coremedia.cap.content.Content;
import org.slf4j.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 * PropertyValueObjectProcessor that WARNs about invalid commerce references.
 */
public class CommerceReferenceProcessor implements PropertyValueObjectProcessor {
    protected Logger logger;

    public static final String PRODUCT_TYPE = "product";

    public static final String PRODUCT_VARIANT_TYPE = "sku";

    private static final String LIVECONTEXT_SCHEME = "ibm";

    private static final String CATALOG_PREFIX = LIVECONTEXT_SCHEME + ":///catalog/";

    private static final String PRODUCT_VARIANT_ID_PREFIX = CATALOG_PREFIX + PRODUCT_VARIANT_TYPE + "/";

    private static final String PRODUCT_ID_PREFIX = CATALOG_PREFIX + PRODUCT_TYPE + "/";


    public CommerceReferenceProcessor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Object process(Content content, String propertyName, Object propertyValueObject) {
        if(content == null || propertyValueObject == null) {
            return propertyValueObject;
        }
        if (!"CMProductTeaser".equals(content.getType().getName())) {
            return propertyValueObject;
        }
        String value = propertyValueObject.toString();
        if (!StringUtils.isEmpty(value) && !isValid(value)) {
            logger.warn("Encountered invalid value '" + value + "' for " + content + " and property " + propertyName + ", but leaving it");
        }
        return propertyValueObject;
    }

    /**
     * Checks whether the given value is a valid format for the commerce reference.
     * It does _not_ check if the product exists.
     *
     * @param value Value to check
     * @return <code>true</code> of the given value is considered valid
     */
    protected boolean isValid(String value) {
        boolean valueValid = value != null && (value.startsWith(PRODUCT_ID_PREFIX) ||
                value.startsWith(PRODUCT_VARIANT_ID_PREFIX));
        return valueValid;
    }
}
