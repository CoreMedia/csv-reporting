import ValueExpressionFactory from "@coremedia/studio-client.client-core/data/ValueExpressionFactory";
import Config from "@jangaroo/runtime/Config";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import OpenCSVExportByFolderDialogActionBase from "./OpenCSVExportByFolderDialogActionBase";

interface OpenCSVExportByFolderDialogActionConfig extends Config<OpenCSVExportByFolderDialogActionBase> {
}

class OpenCSVExportByFolderDialogAction extends OpenCSVExportByFolderDialogActionBase {
  declare Config: OpenCSVExportByFolderDialogActionConfig;

  static readonly ACTION_ID: string = "openCSVExportDialogAction";

  constructor(config: Config<OpenCSVExportByFolderDialogAction> = null) {
    super(ConfigUtils.apply(Config(OpenCSVExportByFolderDialogAction, {
      actionId: OpenCSVExportByFolderDialogAction.ACTION_ID,
      contentValueExpression: ValueExpressionFactory.createFromValue(),

    }), config));
  }
}

export default OpenCSVExportByFolderDialogAction;
