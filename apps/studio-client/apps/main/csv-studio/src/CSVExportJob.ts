import RemoteJobBase from "@coremedia/studio-client.cap-rest-client-impl/common/impl/RemoteJobBase";
import JobContext from "@coremedia/studio-client.cap-rest-client/common/JobContext";

class CSVExportJob extends RemoteJobBase {
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

}

export default CSVExportJob;
