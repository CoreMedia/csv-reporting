package com.coremedia.csv.studio.importer {
import com.coremedia.cms.editor.sdk.upload.dialog.UploadDialog;

public class CSVImportDialogBase extends UploadDialog {
    public function CSVImportDialogBase(config:CSVImportDialogBase = null) {
      super(config);
    }

  protected override function okPressed():void {
    trace("Hello");
  }
}
}
