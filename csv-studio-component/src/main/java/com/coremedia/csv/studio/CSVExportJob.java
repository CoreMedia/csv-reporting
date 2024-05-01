package com.coremedia.csv.studio;

import com.coremedia.rest.cap.jobs.Job;
import com.coremedia.rest.cap.jobs.JobContext;
import com.coremedia.rest.cap.jobs.JobExecutionException;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.invoke.MethodHandles.lookup;

public class CSVExportJob implements Job {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  private String query;
  private int limit;
  private String template;

  @Nullable
  @Override
  public Object call(@NonNull JobContext jobContext) throws JobExecutionException {
    LOG.info("jobContext " + jobContext);
    float progress = 0.0f;
    while(progress < 1.0f) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(jobContext.isAbortRequested()) {
          LOG.info("Job has been aborted - rename doc?");
          return null;
        }
        progress = progress + 0.01f;
      jobContext.notifyProgress(progress);
    }
    return null;
  }

  @SuppressWarnings("unused") //used by JobsFramework
  public void setTemplate(String template) {
    this.template = template;
  }

  @SuppressWarnings("unused") //used by JobsFramework
   public void setQuery(String query) {
    this.query = query;
  }

  @SuppressWarnings("unused") //used by JobsFramework
  public void setLimit(int limit) {
    this.limit = limit;
  }
}
