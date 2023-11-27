import Config from "@jangaroo/runtime/Config";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import CSVImportDialogBase from "./CSVImportDialogBase";

interface CSVImportDialogConfig extends Config<CSVImportDialogBase> {
}

class CSVImportDialog extends CSVImportDialogBase {
  declare Config: CSVImportDialogConfig;

  static override readonly xtype: string = "com.coremedia.csv.studio.config.csvImportDialog";

  static override readonly ID: string = "csvImportDialog";

  constructor(config: Config<CSVImportDialog> = null) {
    super(ConfigUtils.apply(Config(CSVImportDialog, {
    }), config));
  }
}

export default CSVImportDialog;
