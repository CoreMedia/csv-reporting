package com.coremedia.csv.studio {
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentPropertyNames;
import com.coremedia.cms.editor.sdk.actions.ContentAction;
import com.coremedia.ui.data.ValueExpressionFactory;

public class OpenCSVExportByFolderDialogActionBase extends ContentAction {

  /**
   * @param config
   */
  public function OpenCSVExportByFolderDialogActionBase(config:OpenCSVExportByFolderDialogAction = null) {
    super(config);
  }

  override protected function calculateDisabled():Boolean {
    var contents:Array = getContents();
    if (contents === undefined) {
      return true;
    }
    if (contents.length < 1) {
      return true;
    }

    if (contents.length == 1) {
      var theFolder:Content = contents[0] as Content;
      return !theFolder || theFolder.isDocument();
    }
    return false;
  }

  override protected function handle():void {
    var contents:Array = getContents();
    if (contents.length > 0) {
      var content:Content = contents[0];
      if (content.isDocument()) {
        content = content.getParent();
      }
      createExportByFolderDialog(content);
    }
  }

  private function createExportByFolderDialog(content:Content):void {
    var message:String = (content.getName() && content.getName().length > 1) ?
            resourceManager.getString('com.coremedia.csv.studio.CSVExportStudioPlugin', 'exportDialog_exportFolder_text', [content.getName()]) :
            resourceManager.getString('com.coremedia.csv.studio.CSVExportStudioPlugin', 'exportDialog_exportRootFolder_text');

    ValueExpressionFactory.create(ContentPropertyNames.PATH, content).loadValue(function ():void {
      var dialog:CSVExportDialog = new CSVExportDialog(CSVExportDialog({
        confirmationMessage: message
      }));
      dialog.show();
    });
  }
}
}
