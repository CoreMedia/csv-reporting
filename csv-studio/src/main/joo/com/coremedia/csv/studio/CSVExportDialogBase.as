package com.coremedia.csv.studio {

import com.coremedia.cap.content.search.SearchParameters;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cms.editor.sdk.EditorContextImpl;
import com.coremedia.cms.editor.sdk.components.StudioDialog;
import com.coremedia.cms.editor.sdk.editorContext;
import com.coremedia.cms.editor.sdk.util.StudioConfigurationUtil;
import com.coremedia.ui.data.ValueExpression;
import com.coremedia.ui.data.ValueExpressionFactory;
import com.coremedia.ui.data.beanFactory;
import com.coremedia.ui.data.impl.RemoteService;
import com.coremedia.ui.util.ObjectUtils;

import ext.ObjectUtil;

import js.Element;
import js.Range;

public class CSVExportDialogBase extends StudioDialog {

  public static const REPORTING_SETTINGS_NAME:String = "ReportingSettings";
  public static const TEMPLATES_SETTINGS_NAME:String = "templates";

  private var _disabledValueExpression:ValueExpression;
  private var _selectedTemplateValueExpression:ValueExpression;
  private var _templatesExpression:ValueExpression;
  private var _requestURIExpression:ValueExpression;

  [Bindable]
  public var confirmationMessage:String;

  public function CSVExportDialogBase(config:CSVExportDialog = null) {
    super(config);
  }

  protected function getDisabledValueExpression():ValueExpression {
    if (!_disabledValueExpression) {
      _disabledValueExpression = ValueExpressionFactory.createFromFunction(function():Boolean {
        return !getSelectedTemplateValueExpression().getValue();
      });
    }
    return _disabledValueExpression;
  }

  protected function getSelectedTemplateValueExpression():ValueExpression {
    if (!_selectedTemplateValueExpression) {
      _selectedTemplateValueExpression = ValueExpressionFactory.create('selectedTemplate', beanFactory.createLocalBean());
      _selectedTemplateValueExpression.setValue(null);
    }
    return _selectedTemplateValueExpression;
  }

  protected function getTemplatesExpression():ValueExpression {
    if (!_templatesExpression) {
      _templatesExpression = ValueExpressionFactory.createFromFunction(getTemplates);
    }
    return _templatesExpression;
  }

  protected function getRequestURIExpression():ValueExpression {
    if(!_requestURIExpression) {
      _requestURIExpression = ValueExpressionFactory.createFromFunction(function():String {
        return getRequestURI(getSearchParams());
      });
    }
    return _requestURIExpression;
  }

  protected function getTemplates():Array {
    var templatesConfig:Struct = StudioConfigurationUtil.getConfiguration(REPORTING_SETTINGS_NAME, TEMPLATES_SETTINGS_NAME);
    var options:Array = [];

    if (templatesConfig) {
      templatesConfig.getType().getPropertyNames().forEach(function(name:String):void {
        options.push( {
          'name': name
        })
      });
    }
    if(!getSelectedTemplateValueExpression().getValue() && options.length > 0) {
      getSelectedTemplateValueExpression().setValue(options[0].name);
    }
    return options;
  }

  protected function handleExport():void {
    window.open(getRequestURI(getSearchParams()));
    close();
  }

  protected static function getSearchParams():SearchParameters {
    return EditorContextImpl(editorContext).getCollectionViewModel().getSearchParameters();
  }

  protected function getRequestURI(searchParams:SearchParameters):String {
    var url:String = RemoteService.calculateRequestURI('exportcsv/contentset');

    var params:Object = ObjectUtils.removeUndefinedOrNullProperties(searchParams);
    params['template'] = getSelectedTemplateValueExpression().getValue();
    delete params['xclass'];
    var paramString:String = "?" + ObjectUtil.toQueryString(searchParams);

    return url + paramString;
  }

  protected function copyQueryURLToClipboard():void {
    // Create new readonly element with the QueryURL; append to the body as invisible in order to select and copy later
    const cp:Element = window.document.createElement('textarea');
    cp.value = getRequestURI(getSearchParams());
    cp.setAttribute('readonly', '');
    cp.style.position = 'absolute';
    cp.style.left = '-9999px';
    window.document.body.appendChild(cp);
    // Check if there is any content selected previously to restore it after copying
    const selected:Range =
            window.document.getSelection().rangeCount > 0
                    ? window.document.getSelection().getRangeAt(0)
                    : undefined;
    // Select and copy the QueryURL
    cp.select();
    window.document.execCommand('copy');
    // Remove the <textarea> element
    window.document.body.removeChild(cp);
    // If a selection existed before copying, unselect everything on the HTML document and restore the original selection
    if (selected) {
      window.document.getSelection().removeAllRanges();
      window.document.getSelection().addRange(selected);
    }
  }
}
}
