package com.coremedia.csv.cae.handlers;

import com.coremedia.csv.common.CSVConfig;
import com.coremedia.csv.cae.utils.CSVUtils;
import org.springframework.beans.factory.annotation.Required;

/**
 * Abstract handler that serves as a parent for all CSV file request handlers.
 */
public abstract class BaseCSVHandler {

  /**
   * The utility class used to generate the CSV file.
   */
  protected CSVUtils CSVUtil;

  /**
   * The config class which handles the settings determining the CSV column headers
   */
  protected CSVConfig CSVConfig;

  /**
   * Sets the CSV Exporter Util. This is done by Spring since all of the spring beans needed are registered in that
   * class as well.
   *
   * @param CSVUtil the Utility class to set
   */
  @Required
  public void setCSVUtil(CSVUtils CSVUtil) {
    this.CSVUtil = CSVUtil;
  }

  /**
   * Sets the CSV configuration handler.
   *
   * @param CSVConfig the CSVConfig to set
   */
  @Required
  public void setCSVConfig(CSVConfig CSVConfig) {
    this.CSVConfig = CSVConfig;
  }

}
