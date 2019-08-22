package com.coremedia.csv.studio.importer {
import ext.Action;

public class OpenCSVImportDialogActionBase extends Action {

  /**
   * @param config
   */
  public function OpenCSVImportDialogActionBase(config:OpenCSVImportDialogActionBase = null) {
    super(config);
  }

  override protected function handle():void {
    var dialog:CSVImportDialog = new CSVImportDialog();
    dialog.show();
  }

}
}
