package com.coremedia.csv.common;

/**
 * Contains all constant string values used for CSV reporting tools.
 */
public class CSVConstants {

  /**
   * The CSV media type value for the Content-Type HTTP header.
   */
  public static String CSV_MEDIA_TYPE = "text/csv";

  /**
   * The Content-Disposition HTTP header key indicating whether the response content is a file attachment.
   */
  public static String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";

  /**
   * Designates that the value of the property belongs in the local settings, in the path following this prefix
   * String.
   */
  public static String PROPERTY_PREFIX_LOCAL_SETTINGS = "localSettings.";

  /**
   * The name of the column containing a CSV record's export status.
   */
  public static String COLUMN_STATUS = "ExportStatus";

  /* ------- Content Metadata Columns ------- */
  /**
   * The name of the column containing a content's Id.
   */
  public static String COLUMN_ID = "Id";

  /**
   * The name of the column containing a content's Name.
   */
  public static String COLUMN_NAME = "Name";

  /**
   * The name of the column containing a content's Path.
   */
  public static String COLUMN_PATH = "Path";

  /**
   * The name of the column containing a content's URL.
   */
  public static String COLUMN_URL = "URL";

  /**
   * The name of the column containing a content's Type.
   */
  public static String COLUMN_TYPE = "Type";

  /**
   * The name of the column containing a content's Creation Date.
   */
  public static String COLUMN_CREATION_DATE = "CreationDate";

  /**
   * The name of the column containing a content's Creator.
   */
  public static String COLUMN_CREATED_BY = "CreatedBy";

  /**
   * The name of the column containing a content's Last Modification Date.
   */
  public static String COLUMN_LAST_MODIFICATION_DATE = "LastModificationDate";

  /**
   * The name of the column containing a content's Last Modifier.
   */
  public static String COLUMN_LAST_MODIFIED_BY = "LastModifiedBy";

  /**
   * The name of the column containing a content's Version Status.
   */
  public static String COLUMN_VERSION_STATUS = "VersionStatus";

  /**
   * The name of the column containing a content's Publication Date.
   */
  public static String COLUMN_PUBLICATION_DATE = "PublicationDate";

  /**
   * The content's property name which holds the value of its Subject Taxonomies.
   */
  public static String PROPERTY_SUBJECT_TAGS = "subjectTaxonomy";

}
