import { studioApps } from "@coremedia/studio-client.app-context-models";
import Ext from "@jangaroo/ext-ts";
import Component from "@jangaroo/ext-ts/Component";
import StudioPlugin from "@coremedia/studio-client.main.editor-components/configuration/StudioPlugin";
import Config from "@jangaroo/runtime/Config";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import OpenDialogAction from "@coremedia/studio-client.ext.ui-components/actions/OpenDialogAction";
import CSVImportDialog from "./CSVImportDialog";

interface CSVImportStudioPluginConfig extends Config<StudioPlugin> {}

class CSVImportStudioPlugin extends StudioPlugin {
  declare Config: CSVImportStudioPluginConfig;

  static readonly xtype: string = "com.coremedia.csv.studio.config.csvImportStudioPlugin";

  constructor(config: Config<CSVImportStudioPlugin> = null) {
    super(ConfigUtils.apply(Config(CSVImportStudioPlugin), config));
  }

  override init() {
    studioApps._.getShortcutRunnerRegistry().registerShortcutRunner("cmCSVImport", (): void => {
      const dialog = Ext.getCmp(CSVImportDialog.ID);
      if (dialog && dialog.rendered) {
        dialog.show();
        dialog.focus();
      } else {
        const openDialogAction = new OpenDialogAction({ dialog: Config(CSVImportDialog) });
        openDialogAction.addComponent(new Component({}));
        openDialogAction.execute();
      }
    });
  }
}

export default CSVImportStudioPlugin;
