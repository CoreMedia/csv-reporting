import StudioPlugin from "@coremedia/studio-client.main.editor-components/configuration/StudioPlugin";
import CollectionView from "@coremedia/studio-client.main.editor-components/sdk/collectionview/CollectionView";
import Config from "@jangaroo/runtime/Config";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import CSVExportLibraryPlugin from "./CSVExportLibraryPlugin";

interface CSVExportStudioPluginConfig extends Config<StudioPlugin> {
}

class CSVExportStudioPlugin extends StudioPlugin {
  declare Config: CSVExportStudioPluginConfig;

  static readonly OPEN_DIALOG_ACTION_ID: string = "openCSVExportDialogAction";

  static readonly xtype: string = "com.coremedia.csv.studio.config.csvExportStudioPlugin";

  constructor(config: Config<CSVExportStudioPlugin> = null) {
    super(ConfigUtils.apply(Config(CSVExportStudioPlugin, {

      rules: [
        Config(CollectionView, {
          plugins: [
            Config(CSVExportLibraryPlugin),
          ],
        }),
      ],

    }), config));
  }
}

export default CSVExportStudioPlugin;
