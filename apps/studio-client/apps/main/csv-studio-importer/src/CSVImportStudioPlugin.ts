import StudioAppsImpl from "@coremedia/studio-client.app-context-models/apps/StudioAppsImpl";
import studioApps from "@coremedia/studio-client.app-context-models/apps/studioApps";
import Ext from "@jangaroo/ext-ts";
import Component from "@jangaroo/ext-ts/Component";
import { cast } from "@jangaroo/runtime";
import StudioPlugin from "@coremedia/studio-client.main.editor-components/configuration/StudioPlugin";
import Config from "@jangaroo/runtime/Config";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import CSVImportDialog from "./CSVImportDialog";
import OpenDialogAction from "@coremedia/studio-client.ext.ui-components/actions/OpenDialogAction";

interface CSVImportStudioPluginConfig extends Config<StudioPlugin> {
}

class CSVImportStudioPlugin extends StudioPlugin {
  declare Config: CSVImportStudioPluginConfig;

  static readonly xtype: string = "com.coremedia.csv.studio.config.csvImportStudioPlugin";

  constructor(config: Config<CSVImportStudioPlugin> = null) {
    super(ConfigUtils.apply(Config(CSVImportStudioPlugin), config));
  }

  override init() {
    cast(StudioAppsImpl, studioApps._).getSubAppLauncherRegistry().registerSubAppLauncher("cmCSVImport", (): void => {
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
