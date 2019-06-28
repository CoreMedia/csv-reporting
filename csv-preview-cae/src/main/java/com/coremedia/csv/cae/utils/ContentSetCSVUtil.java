package com.coremedia.csv.cae.utils;

import com.coremedia.cap.content.Content;

import java.util.List;
import java.util.Map;

/**
 * Utility for generating a CSV based on a set of content items.
 */
public class ContentSetCSVUtil extends BaseCSVUtil {

  /**
   * As this request type does not require custom properties to be processed, this method is a no-op.
   *
   * @param csvRecord  the CSV record to which to populate the properties of the content
   * @param content    the content from which the property values will be parsed
   * @param headerList the list of headers which determines which metadata is added to the CSV record and which
   *                   columns will be present in the CSV
   */
  @Override
  protected void populateCustomPropertyFields(Map<String, String> csvRecord, Content content,
                                              List<String> headerList) {
    // no-op
  }

}
