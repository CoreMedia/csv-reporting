package com.coremedia.csv.studio.importer {
import com.coremedia.cms.editor.sdk.upload.FileWrapper;
import com.coremedia.cms.editor.sdk.upload.UploadManager;
import com.coremedia.cms.editor.sdk.upload.dialog.FileContainer;
import com.coremedia.cms.editor.sdk.upload.dialog.FileContainersObservable;
import com.coremedia.cms.editor.sdk.upload.dialog.UploadDialog;
import com.coremedia.ui.data.ValueExpression;
import com.coremedia.ui.data.ValueExpressionFactory;
import com.coremedia.ui.util.EventUtil;

import ext.MessageBox;

public class CSVImportDialogBase extends UploadDialog {

  private var fileContainers:FileContainersObservable;
  private var validationExpression:ValueExpression;

  public function CSVImportDialogBase(config:CSVImportDialogBase = null) {
    super(config);
  }

  /**
   * Returns the value expression that enables/disables the upload button.
   * the status of the buttons depends on if all file panels on this dialog are valid.
   * @return
   */
  protected override function getUploadButtonDisabledExpression():ValueExpression {
    if (!validationExpression) {
      validationExpression = ValueExpressionFactory.createFromFunction(function ():Boolean {
        if (!fileContainers) {
          fileContainers = new FileContainersObservable();
          fileContainers.getInvalidityExpression().setValue(true);
        }
        if (fileContainers.getInvalidityExpression().getValue()) {
          return true;
        }
      });
    }
    return validationExpression;
  }

  /**
   * Fired when a file object has been dropped on the target drop area.
   * The file drop plugin fire an event for each file that is dropped
   * and the corresponding action is handled here.
   */
  protected override function handleDrop(files:Array):void {
    MessageBox.show({
      title: resourceManager.getString('com.coremedia.cms.editor.sdk.upload.Upload', 'Upload_progress_title'),
      msg: resourceManager.getString('com.coremedia.cms.editor.sdk.upload.Upload', 'Upload_progress_msg'),
      closable: false,
      width: 300
    });
    EventUtil.invokeLater(function ():void {//otherwise the progress bar does not appear :(
      for (var i:int = 0; i < files.length; i++) {
        var fc:FileContainer = FileContainer({});
        fc.file = files[i];
        fc.settings = settings;
        fc.removeFileHandler = removeFileContainer;
        var fileContainer:FileContainer = new FileContainer(fc);
        fileContainers.add(fileContainer);
      }
      MessageBox.hide();
    });
  }

  protected override function okPressed():void {
    var fileWrappers:Array = fileContainers.getFiles();
    fileWrappers.forEach(function (fileWrapper:FileWrapper):void {
      fileWrapper.setCustomUploadUrl('importcsv/uploadfile');
    });

    close();
    UploadManager.bulkUpload(settings, null, fileWrappers, callback);
  }
}
}
