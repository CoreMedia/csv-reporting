package com.coremedia.csv.studio;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "csv")
@DefaultAnnotation(NonNull.class)
public class CSVConfigurationProperties {
  private String previewRestUrlPrefix = "http://cae-preview:8080/blueprint/servlet";

  // limit of content items to export.
  // -1 defaults to the default of the Studio's SearchService (5000)
  private int defaultItemLimit = -1;

  // number of rows/contents to fetch in a batch when using async Studio Job
  private int batchSize = 100;

  public String getPreviewRestUrlPrefix() {
    return previewRestUrlPrefix;
  }

  public void setPreviewRestUrlPrefix(String previewRestUrlPrefix) {
    this.previewRestUrlPrefix = previewRestUrlPrefix;
  }

  public int getDefaultItemLimit() {
    return defaultItemLimit;
  }

  public void setDefaultItemLimit(int defaultItemLimit) {
    this.defaultItemLimit = defaultItemLimit;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }
}
