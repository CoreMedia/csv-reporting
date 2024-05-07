import RemoteJobBase from "@coremedia/studio-client.cap-rest-client-impl/common/impl/RemoteJobBase";
import JobContext from "@coremedia/studio-client.cap-rest-client/common/JobContext";
import ValueExpression from "@coremedia/studio-client.client-core/data/ValueExpression";
import ValueExpressionFactory from "@coremedia/studio-client.client-core/data/ValueExpressionFactory";
import BackgroundJob from "@coremedia/studio-client.main.editor-components/sdk/jobs/BackgroundJob";
import TrackedJob from "@coremedia/studio-client.cap-rest-client/common/TrackedJob";
import {AnyFunction} from "@jangaroo/runtime/types";
import {mixin} from "@jangaroo/runtime";
import ShowInRepositoryAction
  from "@coremedia/studio-client.ext.library-services-toolkit/actions/ShowInRepositoryAction";
import Config from "@jangaroo/runtime/Config";

class CSVExportJob extends RemoteJobBase implements BackgroundJob {

  static readonly #JOB_TYPE_CSV_REPORTER_EXPORT: string = "csvReporterExport";

  #params: any;
  #name: string;
  #ctx: JobContext = null;
  // workaround for CMS-24848
  #startedTrackedJob: TrackedJob = null;

  constructor(params: any) {
    super();
    this.#name = this.generateName();
    this.#params = params;
    this.#params["name"] = this.#name;
  }

  override execute(jobContext: JobContext): void {
    this.#ctx = jobContext;
    super.execute(jobContext);
  }

  protected override getJobType(): string {
    return CSVExportJob.#JOB_TYPE_CSV_REPORTER_EXPORT;
  }

  protected override getParams(): any {
    return this.#params;
  }

  protected override mayRetry(): boolean {
    return false;
  }

  getNameExpression(): ValueExpression {
    return ValueExpressionFactory.createFromValue(this.#name);
  }

  generateName(): string {
    let baseName: string = "CSV Export";
    let time: string = new Date().toLocaleString();
    return `${baseName} ${time}`;
  }

  getIconClsExpression(): ValueExpression {
    // use a common icon for now...
    return ValueExpressionFactory.createFromValue("longRunningProcess");
  }

  getErrorHandler(): AnyFunction {
    return null;
  }

  getSuccessHandler(): AnyFunction {
    return (): void => {
      const result: any = this.startedTrackedJob.getResult();
      const showInRepositoryAction = new ShowInRepositoryAction(Config(ShowInRepositoryAction, {contentValueExpression: ValueExpressionFactory.createFromValue(result)}));
      showInRepositoryAction.execute();
    };
  }

  get startedTrackedJob(): TrackedJob {
    return this.#startedTrackedJob;
  }

  set startedTrackedJob(value: TrackedJob) {
    this.#startedTrackedJob = value;
  }

}

mixin(CSVExportJob, BackgroundJob);

export default CSVExportJob;
