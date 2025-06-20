package com.coremedia.csv.studio;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.csv.studio.utils.BaseCSVUtil;
import com.coremedia.rest.cap.jobs.JobFactory;
import com.coremedia.rest.cap.jobs.Job;
import edu.umd.cs.findbugs.annotations.NonNull;

public class CSVExportJobFactory implements JobFactory {
  private final CSVExportAuthorization csvExportAuthorization;
  private final CSVExportSearchService csvExportSearchService;
  private final CSVFileRetriever csvFileRetriever;
  private final ContentRepository contentRepository;
  private final BaseCSVUtil baseCSVUtil;

  public CSVExportJobFactory(CSVExportAuthorization csvExportAuthorization,
                             CSVExportSearchService csvExportSearchService,
                             CSVFileRetriever csvFileRetriever,
                             ContentRepository contentRepository,
                             BaseCSVUtil baseCSVUtil) {
    this.csvExportAuthorization = csvExportAuthorization;
    this.csvExportSearchService = csvExportSearchService;
    this.csvFileRetriever = csvFileRetriever;
    this.contentRepository = contentRepository;
    this.baseCSVUtil = baseCSVUtil;
  }

  @Override
  public boolean accepts(@NonNull String jobType) {
    return jobType.equals("csvReporterExport");
  }

  @NonNull
  @Override
  public Job createJob() {
    return new CSVExportJob(csvExportAuthorization, csvExportSearchService,
            csvFileRetriever, contentRepository, baseCSVUtil);
  }
}
