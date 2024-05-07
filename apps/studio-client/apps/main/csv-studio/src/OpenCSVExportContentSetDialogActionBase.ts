import SearchResult from "@coremedia/studio-client.cap-rest-client/content/search/SearchResult";
import ValueExpression from "@coremedia/studio-client.client-core/data/ValueExpression";
import ContentAction from "@coremedia/studio-client.ext.cap-base-components/actions/ContentAction";
import Config from "@jangaroo/runtime/Config";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import resourceManager from "@jangaroo/runtime/l10n/resourceManager";
import CSVExportDialog from "./CSVExportDialog";
import CSVExportStudioPlugin_properties from "./CSVExportStudioPlugin_properties";
import OpenCSVExportContentSetDialogAction from "./OpenCSVExportContentSetDialogAction";

interface OpenCSVExportContentSetDialogActionBaseConfig extends Config<ContentAction>, Partial<Pick<OpenCSVExportContentSetDialogActionBase,
  "searchResultsValueExpression"
>> {
}

class OpenCSVExportContentSetDialogActionBase extends ContentAction {
  declare Config: OpenCSVExportContentSetDialogActionBaseConfig;

  #searchResultsValueExpression: ValueExpression = null;

  get searchResultsValueExpression(): ValueExpression {
    return this.#searchResultsValueExpression;
  }

  set searchResultsValueExpression(value: ValueExpression) {
    this.#searchResultsValueExpression = value;
  }

  /**
   * @param config
   */
  constructor(config: Config<OpenCSVExportContentSetDialogAction> = null) {
    super(((): Config<OpenCSVExportContentSetDialogActionBase> => ConfigUtils.apply(Config(OpenCSVExportContentSetDialogAction, {}), config))());
    this.searchResultsValueExpression = config.searchResultsValueExpression;
  }

  protected override calculateDisabled(): boolean {
    const results: SearchResult = this.searchResultsValueExpression.getValue();
    // Disable only if the search results value is null or undefined
    return !results;
  }

  protected override handle(): void {
    const results: SearchResult = this.searchResultsValueExpression.getValue();
    let hits = results.getHits();
    if (!hits) {
      hits = [];
    }
    this.#createExportContentSetDialog(hits);
  }

  #createExportContentSetDialog(contents: Array<any>): void {
    // let's not how the count, the CSV exporter might export more depending on its configuration (see csv.defaultItemLimit)
    // const itemCount: int = contents.length;
    const message = resourceManager.getString(CSVExportStudioPlugin_properties, "exportDialog_exportSearchResult_text");
    const dialog = new CSVExportDialog(Config(CSVExportDialog, { confirmationMessage: message }));
    dialog.show();

  }
}

export default OpenCSVExportContentSetDialogActionBase;
