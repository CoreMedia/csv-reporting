import SearchParameters from "@coremedia/studio-client.cap-rest-client/content/search/SearchParameters";
import Struct from "@coremedia/studio-client.cap-rest-client/struct/Struct";
import RemoteService from "@coremedia/studio-client.client-core-impl/data/impl/RemoteService";
import ValueExpression from "@coremedia/studio-client.client-core/data/ValueExpression";
import ValueExpressionFactory from "@coremedia/studio-client.client-core/data/ValueExpressionFactory";
import beanFactory from "@coremedia/studio-client.client-core/data/beanFactory";
import ObjectUtils from "@coremedia/studio-client.client-core/util/ObjectUtils";
import StudioDialog from "@coremedia/studio-client.ext.base-components/dialogs/StudioDialog";
import StudioConfigurationUtil from "@coremedia/studio-client.ext.cap-base-components/util/config/StudioConfigurationUtil";
import EditorContextImpl from "@coremedia/studio-client.main.editor-components/sdk/EditorContextImpl";
import editorContext from "@coremedia/studio-client.main.editor-components/sdk/editorContext";
import ObjectUtil from "@jangaroo/ext-ts/Object";
import { bind, cast } from "@jangaroo/runtime";
import Config from "@jangaroo/runtime/Config";
import CSVExportDialog from "./CSVExportDialog";
import Job from "@coremedia/studio-client.cap-rest-client/common/Job";
import jobService from "@coremedia/studio-client.cap-rest-client/common/jobService";
import { AnyFunction } from "@jangaroo/runtime/types";
import CSVExportJob from "./CSVExportJob";
import TrackedJob from "@coremedia/studio-client.cap-rest-client/common/TrackedJob";

interface CSVExportDialogBaseConfig extends Config<StudioDialog>, Partial<Pick<CSVExportDialogBase,
  "confirmationMessage"
>> {
}

class CSVExportDialogBase extends StudioDialog {
  declare Config: CSVExportDialogBaseConfig;

  static readonly REPORTING_SETTINGS_NAME: string = "ReportingSettings";

  static readonly TEMPLATES_SETTINGS_NAME: string = "templates";

  #_disabledValueExpression: ValueExpression = null;

  #_selectedTemplateValueExpression: ValueExpression = null;

  #_templatesExpression: ValueExpression = null;

  #_requestURIExpression: ValueExpression = null;

  #confirmationMessage: string = null;

  get confirmationMessage(): string {
    return this.#confirmationMessage;
  }

  set confirmationMessage(value: string) {
    this.#confirmationMessage = value;
  }

  constructor(config: Config<CSVExportDialog> = null) {
    super(config);
  }

  protected getDisabledValueExpression(): ValueExpression {
    if (!this.#_disabledValueExpression) {
      this.#_disabledValueExpression = ValueExpressionFactory.createFromFunction((): boolean =>
        !this.getSelectedTemplateValueExpression().getValue(),
      );
    }
    return this.#_disabledValueExpression;
  }

  protected getSelectedTemplateValueExpression(): ValueExpression {
    if (!this.#_selectedTemplateValueExpression) {
      this.#_selectedTemplateValueExpression = ValueExpressionFactory.create("selectedTemplate", beanFactory._.createLocalBean());
      this.#_selectedTemplateValueExpression.setValue(null);
    }
    return this.#_selectedTemplateValueExpression;
  }

  protected getTemplatesExpression(): ValueExpression {
    if (!this.#_templatesExpression) {
      this.#_templatesExpression = ValueExpressionFactory.createFromFunction(bind(this, this.getTemplates));
    }
    return this.#_templatesExpression;
  }

  protected getRequestURIExpression(): ValueExpression {
    if (!this.#_requestURIExpression) {
      this.#_requestURIExpression = ValueExpressionFactory.createFromFunction((): string =>
        this.getRequestURI(CSVExportDialogBase.getSearchParams()),
      );
    }
    return this.#_requestURIExpression;
  }

  protected getTemplates(): Array<any> {
    const templatesConfig: Struct = StudioConfigurationUtil.getConfiguration(CSVExportDialogBase.REPORTING_SETTINGS_NAME, CSVExportDialogBase.TEMPLATES_SETTINGS_NAME);
    const options = [];

    if (templatesConfig) {
      templatesConfig.getType().getPropertyNames().forEach((name: string): void => {
        options.push({ "name": name });
      });
    }
    if (!this.getSelectedTemplateValueExpression().getValue() && options.length > 0) {
      this.getSelectedTemplateValueExpression().setValue(options[0].name);
    }
    return options;
  }

  protected handleDirectExport(): void {
    window.open(this.getRequestURI(CSVExportDialogBase.getSearchParams()));
    this.close();
  }

  protected handleBackgroundExport(): void {
    const searchParams: SearchParameters = CSVExportDialogBase.getSearchParams();
    const params = ObjectUtils.removeUndefinedOrNullProperties(searchParams);
    params["template"] = this.getSelectedTemplateValueExpression().getValue();
    delete params["xclass"];
    const successCallback: AnyFunction = (): void => {};
    const job: CSVExportJob = new CSVExportJob(params);
    const trackedJob: TrackedJob = jobService._.executeJob(
            job,
            //on success
            successCallback,
            //on error
            (result: any): void => {},
    );
    job.startedTrackedJob = trackedJob;
    this.close();
  }

  protected static getSearchParams(): SearchParameters {
    return cast(EditorContextImpl, editorContext._).getCollectionViewModel().getSearchParameters();
  }

  protected getRequestURI(searchParams: SearchParameters): string {
    const url = RemoteService.calculateRequestURI("exportcsv/contentset");

    const params = ObjectUtils.removeUndefinedOrNullProperties(searchParams);
    params["template"] = this.getSelectedTemplateValueExpression().getValue();
    delete params["xclass"];
    const paramString = "?" + ObjectUtil.toQueryString(searchParams);

    return url + paramString;
  }

  protected copyQueryURLToClipboard(): void {
    // Create new readonly element with the QueryURL; append to the body as invisible in order to select and copy later
    const cp = window.document.createElement("textarea");
    cp.value = this.getRequestURI(CSVExportDialogBase.getSearchParams());
    cp.setAttribute("readonly", "");
    cp.style.position = "absolute";
    cp.style.left = "-9999px";
    window.document.body.appendChild(cp);
    // Check if there is any content selected previously to restore it after copying
    const selected: Range =
            window.document.getSelection().rangeCount > 0
              ? window.document.getSelection().getRangeAt(0)
              : undefined;
    // Select and copy the QueryURL
    cp.select();
    window.document.execCommand("copy");
    // Remove the <textarea> element
    window.document.body.removeChild(cp);
    // If a selection existed before copying, unselect everything on the HTML document and restore the original selection
    if (selected) {
      window.document.getSelection().removeAllRanges();
      window.document.getSelection().addRange(selected);
    }
  }
}

export default CSVExportDialogBase;
