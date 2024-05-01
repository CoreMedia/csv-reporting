import RemoteJobBase from "@coremedia/studio-client.cap-rest-client-impl/common/impl/RemoteJobBase";
import JobContext from "@coremedia/studio-client.cap-rest-client/common/JobContext";
import ValueExpression from "@coremedia/studio-client.client-core/data/ValueExpression";
import ValueExpressionFactory from "@coremedia/studio-client.client-core/data/ValueExpressionFactory";
import BackgroundJob from "@coremedia/studio-client.main.editor-components/sdk/jobs/BackgroundJob";
import {AnyFunction} from "@jangaroo/runtime/types";
import {mixin} from "@jangaroo/runtime";

class CSVExportJob extends RemoteJobBase implements BackgroundJob {
  static readonly #JOB_TYPE_CSV_REPORTER_EXPORT: string = "csvReporterExport";

  protected params: any;

  #ctx: JobContext = null;

  constructor(params: any) {
    super();
    this.params = params;
  }

  override execute(jobContext: JobContext): void {
    this.#ctx = jobContext;
    super.execute(jobContext);
  }

  protected override getJobType(): string {
    return CSVExportJob.#JOB_TYPE_CSV_REPORTER_EXPORT;
  }

  protected override getParams(): any {
    return this.params;
  }

  protected override mayRetry(): boolean {
    return false;
  }

  getNameExpression():ValueExpression {
    return ValueExpressionFactory.createFromValue("A CSVExportJob");
  }

  getIconClsExpression(): ValueExpression {
    return ValueExpressionFactory.createFromValue("xliff");
  }

  getErrorHandler(): AnyFunction {
    return null;
  }

  getSuccessHandler(): AnyFunction {
    return null;
  }

}
mixin(CSVExportJob, BackgroundJob);

export default CSVExportJob;
