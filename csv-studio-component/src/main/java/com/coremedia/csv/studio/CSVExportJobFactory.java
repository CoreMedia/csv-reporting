package com.coremedia.csv.studio;

import com.coremedia.rest.cap.jobs.JobFactory;
import com.coremedia.rest.cap.jobs.Job;
import edu.umd.cs.findbugs.annotations.NonNull;

public class CSVExportJobFactory implements JobFactory {

  public CSVExportJobFactory() {
  }

  @Override
  public boolean accepts(@NonNull String jobType) {
    return jobType.equals("csvReporterExport");
  }

  @NonNull
  @Override
  public Job createJob() {
    return new CSVExportJob();
  }
}
