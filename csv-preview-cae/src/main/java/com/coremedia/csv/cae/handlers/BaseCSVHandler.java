package com.coremedia.csv.cae.handlers;

import com.coremedia.csv.common.CSVConfig;
import com.coremedia.csv.cae.utils.BaseCSVUtil;

/**
 * Abstract handler that serves as a parent for all CSV file request handlers.
 */
public abstract class BaseCSVHandler {

  /**
   * The utility class used to generate the CSV file.
   */
  protected final BaseCSVUtil CSVUtil;

  /**
   * The config class which handles the settings determining the CSV column headers
   */
  protected final CSVConfig CSVConfig;

  protected BaseCSVHandler(BaseCSVUtil csvUtil, CSVConfig csvConfig) {
    this.CSVUtil = csvUtil;
    this.CSVConfig = csvConfig;
  }

}
