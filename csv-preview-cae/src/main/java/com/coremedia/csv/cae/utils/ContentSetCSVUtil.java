package com.coremedia.csv.cae.utils;

import com.coremedia.blueprint.common.contentbeans.CMLinkable;
import com.coremedia.blueprint.common.contentbeans.CMViewtype;
import com.coremedia.cap.content.Content;

import java.util.List;
import java.util.Map;

/**
 * Utility for generating a CSV based on a set of content items.
 */
public class ContentSetCSVUtil extends BaseCSVUtil {

  /**
   * If additional custom report/property fields are required, feel free to extend this class, overwrite populateCustomPropertyFields
   * and replace the contentSetCSVUtil bean.
   *
   * @param csvRecord  the CSV record to which to populate the properties of the content
   * @param content    the content from which the property values will be parsed
   * @param headerList the list of headers which determines which metadata is added to the CSV record and which
   *                   columns will be present in the CSV
   */
  @Override
  protected void populateCustomPropertyFields(Map<String, String> csvRecord, Content content,
                                              List<String> headerList) {
    // custom property to include Layout Variant name
    if(headerList.contains("LayoutVariant")) {
      String layoutProperty = "";
      if(content.getType().isSubtypeOf(CMLinkable.NAME)) {
        Content viewtype = content.getLink(CMLinkable.VIEWTYPE);
        if(viewtype != null)
          layoutProperty = viewtype.getString(CMViewtype.LAYOUT);
      }
      csvRecord.put("LayoutVariant", layoutProperty);
    }
  }

}
