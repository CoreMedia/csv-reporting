package com.coremedia.csv.studio;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "csv")
@DefaultAnnotation(NonNull.class)
public class CSVConfigurationProperties {
  private String previewRestUrlPrefix = "http://cae-preview:8080/blueprint/servlet";
  private int defaultItemLimit = -1;

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
}
