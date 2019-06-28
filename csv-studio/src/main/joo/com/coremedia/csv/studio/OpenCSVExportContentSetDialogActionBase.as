package com.coremedia.csv.studio {

import com.coremedia.cap.content.search.SearchResult;
import com.coremedia.cms.editor.sdk.actions.ContentAction;
import com.coremedia.ui.data.ValueExpression;

public class OpenCSVExportContentSetDialogActionBase extends ContentAction {

  [Bindable]
  public var searchResultsValueExpression:ValueExpression;

  /**
   * @param config
   */
  public function OpenCSVExportContentSetDialogActionBase(config:OpenCSVExportContentSetDialogAction = null) {
    searchResultsValueExpression = config.searchResultsValueExpression;
    super(config);
  }

  override protected function calculateDisabled():Boolean {
    var results:SearchResult = searchResultsValueExpression.getValue();
    // Disable only if the search results value is null or undefined
    return !results;
  }

  override protected function handle():void {
    var results:SearchResult = searchResultsValueExpression.getValue();
    var hits:Array = results.getHits();
    if (!hits) {
      hits = [];
    }
    createExportContentSetDialog(hits);
  }

  private function createExportContentSetDialog(contents:Array):void {
    var itemCount:int = contents.length;
    var message:String = resourceManager.getString("com.coremedia.csv.studio.CSVExportStudioPlugin", "exportDialog_resultCount_text", [itemCount]);
    var dialog:CSVExportDialog = new CSVExportDialog(CSVExportDialog({
      confirmationMessage: message
    }));
    dialog.show();

  }
}
}
