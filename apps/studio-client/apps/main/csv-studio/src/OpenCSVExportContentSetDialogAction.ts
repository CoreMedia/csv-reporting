import ValueExpressionFactory from "@coremedia/studio-client.client-core/data/ValueExpressionFactory";
import Config from "@jangaroo/runtime/Config";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import OpenCSVExportContentSetDialogActionBase from "./OpenCSVExportContentSetDialogActionBase";

interface OpenCSVExportContentSetDialogActionConfig extends Config<OpenCSVExportContentSetDialogActionBase> {
}

class OpenCSVExportContentSetDialogAction extends OpenCSVExportContentSetDialogActionBase {
  declare Config: OpenCSVExportContentSetDialogActionConfig;

  static readonly ACTION_ID: string = "openCSVExportContentSetDialogAction";

  constructor(config: Config<OpenCSVExportContentSetDialogAction> = null) {
    super(ConfigUtils.apply(Config(OpenCSVExportContentSetDialogAction, {
      actionId: OpenCSVExportContentSetDialogAction.ACTION_ID,
      contentValueExpression: ValueExpressionFactory.createFromValue(),

    }), config));
  }
}

export default OpenCSVExportContentSetDialogAction;
