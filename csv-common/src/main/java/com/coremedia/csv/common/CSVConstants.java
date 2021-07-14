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

  /**
   * The content's property name which holds the value of its pictures.
   */
  public static String PROPERTY_PICTURES = "pictures";

  /**
   * Designates that the value of the property belongs in the local settings, in the path following this prefix
   * String.
   */
  public static String PROPERTY_PREFIX_PICTURES = PROPERTY_PICTURES + ".";

  /**
   * The content's property name which holds the value of its Detail Text.
   */
  public static String PROPERTY_DETAIL_TEXT = "detailText";

  /**
   * The name of the column containing a content's Title.
   */
  public static String COLUMN_TITLE = "Title";

  /**
   * The content's property name which holds the value of its Title.
   */
  public static String PROPERTY_TITLE = "title";

  /**
   * The name of the column that tells you if the content is site root document.
   */
  public static String COLUMN_IS_SITE_ROOT_DOCUMENT = "IsSiteRootDocument";

  /**
   * The name of the column containing a content's Keywords.
   */
  public static String COLUMN_KEYWORDS = "Keywords";

  /**
   * The content's property name which holds the value of its Keywords.
   */
  public static String PROPERTY_KEYWORDS = "keywords";

  /**
   * The name of the column containing a content's Teaser Title.
   */
  public static String COLUMN_TEASER_TITLE = "TeaserTitle";

  /**
   * The content's property name which holds the value of its Teaser Title.
   */
  public static String PROPERTY_TEASER_TITLE = "teaserTitle";

  /**
   * The name of the column containing a content's Teaser Text.
   */
  public static String COLUMN_TEASER_TEXT = "TeaserText";

  /**
   * The content's property name which holds the value of its Teaser Text.
   */
  public static String PROPERTY_TEASER_TEXT = "teaserText";

  /**
   * The name of the column containing a content's URL Segment.
   */
  public static String COLUMN_URL_SEGMENT = "URLSegment";

  /**
   * The content's property name which holds the value of its URL Segment.
   */
  public static String PROPERTY_URL_SEGMENT = "segment";

  /**
   * The name of the column containing a content's HTML Segment.
   */
  public static String COLUMN_HTML_TITLE = "HTMLTitle";

  /**
   * The content's property name which holds the value of its HTML Title.
   */
  public static String PROPERTY_HTML_TITLE = "htmlTitle";

  /**
   * The name of the column containing a content's HTML Description.
   */
  public static String COLUMN_HTML_DESCRIPTION = "HTMLDescription";

  /**
   * The content's property name which holds the value of its HTML Description.
   */
  public static String PROPERTY_HTML_DESCRIPTION = "htmlDescription";

  /**
   * The name of the column containing a content's Subject Taxonomies.
   */
  public static String COLUMN_SUBJECT_TAGS = "SubjectTags";

  /**
   * The name of the column containing a content's Externally Displayed Date.
   */
  public static String COLUMN_EXTERNALLY_DISPLAYED_DATE = "ExternallyDisplayedDate";

  /**
   * The content's property name which holds the value of its Externally Displayed Date.
   */

  public static String PROPERTY_EXTERNALLY_DISPLAYED_DATE = "extDisplayedDate";

  /**
   * The name of the column containing a content's Externally Associated Theme.
   */
  public static String COLUMN_EXTERNALLY_ASSOCIATED_THEME = "AssociatedTheme";

  /**
   * The content's property name which holds the value of its Externally Associated Theme.
   */
  public static String PROPERTY_EXTERNALLY_ASSOCIATED_THEME = "theme";

  /**
   * The name of the column containing a content's Externally Associated JavaScript.
   */
  public static String COLUMN_EXTERNALLY_ASSOCIATED_JAVASCRIPT = "AssociatedJavaScript";

  /**
   * The content's property name which holds the value of its Externally Associated JavaScript.
   */
  public static String PROPERTY_EXTERNALLY_ASSOCIATED_JAVASCRIPT = "javaScript";

  /**
   * The name of the column containing a content's Externally Associated CSS.
   */
  public static String COLUMN_EXTERNALLY_ASSOCIATED_CSS = "AssociatedCss";

  /**
   * The content's property name which holds the value of its Externally Associated CSS.
   */
  public static String PROPERTY_EXTERNALLY_ASSOCIATED_CSS = "css";

  /**
   * The name of the column containing a content's External Link Target URL.
   */
  public static String COLUMN_EXTERNAL_LINK_TARGET_URL = "ExternalLinkTargetURL";

  /**
   * The content's property name which holds the value of its External Link Target URL.
   */
  public static String PROPERTY_EXTERNAL_LINK_TARGET_URL = "url";

  /**
   * The name of the column containing a content's Locale.
   */
  public static String COLUMN_LOCALE = "Locale";

  /**
   * The content's property name which holds the value of its Locale.
   */
  public static String PROPERTY_LOCALE = "locale";

  /**
   * The name of the column containing a content's Picture Title.
   */
  public static String COLUMN_PICTURE_TITLE = "PictureTitle";

  /**
   * The string for which the uploader can recognize to access its picture (if it has one) and pull it's title
   * property.
   */
  public static String PROPERTY_PICTURE_TITLE = PROPERTY_PREFIX_PICTURES + "title";

  /**
   * The name of the column containing a content's Picture Caption.
   */
  public static String COLUMN_PICTURE_CAPTION = "PictureCaption";

  /**
   * The string for which the uploader can recognize to access its picture (if it has one) and pull it's
   * caption/detail text property.
   */
  public static String PROPERTY_PICTURE_CAPTION = PROPERTY_PREFIX_PICTURES + PROPERTY_DETAIL_TEXT;

  /**
   * The name of the column containing a content's Alternative Text.
   */
  public static String COLUMN_ALTERNATIVE_TEXT = "AlternativeText";

  /**
   * The content's property name which holds the value of its Alternative Text.
   */
  public static String PROPERTY_ALTERNATIVE_TEXT = "alt";

  /**
   * The name of the column containing the keyword which has been specified for this content. Used in the featured and
   * handcrafted searches.
   */
  public static String COLUMN_KEYWORD = "Keyword";

  /**
   * The name of the column containing the parent collection which holds this content as a featured search result.
   */
  public static String COLUMN_PARENT_COLLECTION = "ParentCollection";

  /**
   * The name of the column containing a content's Data URL. Used specifically with videos & other media content.
   */
  public static String COLUMN_DATA_URL = "DataURL";

  /**
   * The content's property name which holds the value of its Data URL. Used specifically with videos & other media
   * content.
   */
  public static String PROPERTY_DATA_URL = "dataUrl";

  /**
   * The name of the column containing a content's External Id. Used specifically with product teasers.
   */
  public static String COLUMN_EXTERNAL_ID = "ExternalId";

  /**
   * The content's property name which holds the value of its External Id. Used specifically with product teasers.
   */
  public static String PROPERTY_EXTERNAL_ID = "externalId";

  /**
   * The name of the column containing a content's Preview css in the Linked Settings.
   */
  public static String COLUMN_LINKED_SETTINGS = "LinkedSettings";

  /**
   * The content's property name which holds the value of its Linked Settings.
   */
  public static String PROPERTY_LINKED_SETTINGS = "linkedSettings";

}
