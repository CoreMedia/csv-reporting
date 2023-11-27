import Content from "@coremedia/studio-client.cap-rest-client/content/Content";
import ContentPropertyNames from "@coremedia/studio-client.cap-rest-client/content/ContentPropertyNames";
import ValueExpressionFactory from "@coremedia/studio-client.client-core/data/ValueExpressionFactory";
import ContentAction from "@coremedia/studio-client.ext.cap-base-components/actions/ContentAction";
import { as } from "@jangaroo/runtime";
import Config from "@jangaroo/runtime/Config";
import resourceManager from "@jangaroo/runtime/l10n/resourceManager";
import CSVExportDialog from "./CSVExportDialog";
import CSVExportStudioPlugin_properties from "./CSVExportStudioPlugin_properties";
import OpenCSVExportByFolderDialogAction from "./OpenCSVExportByFolderDialogAction";

interface OpenCSVExportByFolderDialogActionBaseConfig extends Config<ContentAction> {
}

class OpenCSVExportByFolderDialogActionBase extends ContentAction {
  declare Config: OpenCSVExportByFolderDialogActionBaseConfig;

  /**
   * @param config
   */
  constructor(config: Config<OpenCSVExportByFolderDialogAction> = null) {
    super(config);
  }

  protected override calculateDisabled(): boolean {
    const contents: Array<any> = this.getContents();
    if (contents === undefined) {
      return true;
    }
    if (contents.length < 1) {
      return true;
    }

    if (contents.length == 1) {
      const theFolder = as(contents[0], Content);
      return !theFolder || theFolder.isDocument();
    }
    return false;
  }

  protected override handle(): void {
    const contents: Array<any> = this.getContents();
    if (contents.length > 0) {
      let content: Content = contents[0];
      if (content.isDocument()) {
        content = content.getParent();
      }
      this.#createExportByFolderDialog(content);
    }
  }

  #createExportByFolderDialog(content: Content): void {
    const message: string = (content.getName() && content.getName().length > 1) ?
      resourceManager.getString(CSVExportStudioPlugin_properties, "exportDialog_exportFolder_text", [content.getName()]) :
      CSVExportStudioPlugin_properties.exportDialog_exportRootFolder_text;

    ValueExpressionFactory.create(ContentPropertyNames.PATH, content).loadValue((): void => {
      const dialog = new CSVExportDialog(Config(CSVExportDialog, { confirmationMessage: message }));
      dialog.show();
    });
  }
}

export default OpenCSVExportByFolderDialogActionBase;
