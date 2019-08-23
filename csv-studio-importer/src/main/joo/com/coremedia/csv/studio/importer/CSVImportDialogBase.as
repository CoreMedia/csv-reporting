package com.coremedia.csv.studio.importer {
import com.coremedia.cap.content.Content;
import com.coremedia.cms.editor.sdk.upload.FileWrapper;
import com.coremedia.cms.editor.sdk.upload.UploadManager;
import com.coremedia.cms.editor.sdk.upload.UploadSettings;
import com.coremedia.cms.editor.sdk.upload.dialog.FileContainer;
import com.coremedia.cms.editor.sdk.upload.dialog.FileContainersObservable;
import com.coremedia.cms.editor.sdk.upload.dialog.UploadDialog;
import com.coremedia.cms.editor.sdk.util.MessageBoxUtil;
import com.coremedia.ui.data.ValueExpression;
import com.coremedia.ui.data.ValueExpressionFactory;
import com.coremedia.ui.data.error.RemoteError;
import com.coremedia.ui.data.impl.RemoteService;
import com.coremedia.ui.util.EventUtil;

import ext.Ext;

import ext.MessageBox;
import ext.container.Container;

import js.XMLHttpRequest;

public class CSVImportDialogBase extends UploadDialog {

  private var fileContainers:FileContainersObservable;
  private var validationExpression:ValueExpression;
  private var uploadDropAreaDisabled:Boolean;

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

        // Check that the file is a CSV
        if(fileContainers.getFiles().length != 1) {
          return true;
        }
        var fileType:String = fileContainers.getFiles()[0].get(FileWrapper.FILE_TYPE_PROPERTY);
        if(fileType != 'csv') {
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
    if(!uploadDropAreaDisabled) {
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
          uploadDropAreaDisabled = true;
        }
        MessageBox.hide();
        refreshPanel();
      });
    }
  }

  /**
   * Removes the given file container from the list of uploading files.
   * @param fileContainer
   */
  public override function removeFileContainer(fileContainer:FileContainer):void {
    fileContainers.remove(fileContainer);
    if(fileContainers.isEmpty()) {
      uploadDropAreaDisabled = false;
    }
    refreshPanel();
  }

  /**
   * Rebuilds all panels representing a future upload.
   */
  private function refreshPanel():void {
    var dropArea:Container = Ext.getCmp(UploadDialog.DROP_BOX) as Container;
    if(uploadDropAreaDisabled) {
      dropArea.hide();
    } else {
      dropArea.show();
    }

    //clear and add list of upload containers
    var list:Container = Ext.getCmp(UploadDialog.UPLOAD_LIST) as Container;
    var fileContainer:FileContainer = null;
    for (var i:int = 0; i < fileContainers.size(); i++) {
      fileContainer = fileContainers.getAt(i);
      if (!fileContainer.rendered) {
        list.add(fileContainer);
      }
    }
  }

  protected override function okPressed():void {
    var fileWrappers:Array = fileContainers.getFiles();
    var url = RemoteService.calculateRequestURI('importcsv/uploadfile');
    fileWrappers.forEach(function (fileWrapper:FileWrapper):void {
      //fileWrapper.setCustomUploadUrl('importcsv/uploadfile');
      //fileWrapper.upload(settings, null, onSuccess, uploadError, progress);

      //upload(url, settings, null, onSuccess, uploadError, progress);
      html5upload(url, fileWrapper.getFile());
    });

    close();
    //UploadManager.bulkUpload(settings, null, fileWrappers, callback);
  }

  private function onSuccess():void {
    MessageBoxUtil.showInfo("Import Status", "Successfully updated content");
  }

  protected function uploadError(response:XMLHttpRequest):void {
    var result:RemoteError = RemoteService.createRemoteError(response);
    var message:String = result.message;
    MessageBoxUtil.showError("Import Status", "Import failed: " + message);
  }

  public function html5upload(url:String, file:*, headerParameters:Object = undefined, contentName:String = undefined):void {
    var uploadRequest:XMLHttpRequest;
    var formData:* = new window['FormData']();  // TODO: add FormData class to Jangaroo libs

    var fileName:String = file.name || file.fileName; // safari and chrome use the non std. fileX props

    if (fileName) {
      formData.append('file', file, fileName);
    } else {
      formData.append('file', file);
    }

    if (contentName){
      formData.append('contentName', contentName);
    }

    uploadRequest = new XMLHttpRequest();

    uploadRequest.open('POST', url, true);

    uploadRequest['onload'] = function(e:*):void { uploadCallback(uploadRequest); };

    // TODO[rre]: if we could use Ajax.request here instead of XMLHttpRequest, the header would come for free
    uploadRequest.setRequestHeader(RemoteService.getCsrfTokenHeaderName(), RemoteService.getCsrfTokenValue());

    if (headerParameters) {
      for (var key:String in headerParameters) {
        uploadRequest.setRequestHeader(key, headerParameters[key]);
      }
    }

    uploadRequest.send(formData);
  }

  private function uploadCallback(response:XMLHttpRequest):void {
    var status:int = response.status;
    if (status == 204 || status == 200 || status == 201) {
      onSuccess();
    } else {
      uploadError(response);
    }
  }
}
}
