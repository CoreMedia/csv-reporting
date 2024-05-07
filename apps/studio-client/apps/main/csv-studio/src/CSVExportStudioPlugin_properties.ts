
/**
 * Interface values for ResourceBundle "CSVExportStudioPlugin".
 * @see CSVExportStudioPlugin_properties#INSTANCE
 */
interface CSVExportStudioPlugin_properties {

/**
 * Labels
 */
  library_browse_export_btn_tooltip: string;
  library_search_export_btn_tooltip: string;
  requestURLDialog_title: string;
  copyRequestURL_text: string;
  exportDialog_title: string;
  exportDialog_directExportButton_text: string;
  exportDialog_backgroundExportButton_text: string;
  exportDialog_resultCount_text: string;
  exportDialog_exportRootFolder_text: string;
  exportDialog_exportFolder_text: string;
  exportDialog_templateSelector_label: string;
}

/**
 * Singleton for the current user Locale's instance of ResourceBundle "CSVExportStudioPlugin".
 * @see CSVExportStudioPlugin_properties
 */
const CSVExportStudioPlugin_properties: CSVExportStudioPlugin_properties = {
  library_browse_export_btn_tooltip: "Export folder contents as CSV",
  library_search_export_btn_tooltip: "Export search results as CSV",
  requestURLDialog_title: "Query URL",
  copyRequestURL_text: "Copy Query URL",
  exportDialog_title: "Export CSV",
  exportDialog_directExportButton_text: "Direct Export",
  exportDialog_backgroundExportButton_text: "Background Export",
  exportDialog_resultCount_text: "Export {0} items?",
  exportDialog_exportRootFolder_text: "Export all items in folder?",
  exportDialog_exportFolder_text: "Export all items in folder '{0}'?",
  exportDialog_templateSelector_label: "Template",
};

export default CSVExportStudioPlugin_properties;
