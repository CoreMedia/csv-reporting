package com.coremedia.csv.studio;

import com.coremedia.rest.cap.jobs.Job;
import com.coremedia.rest.cap.jobs.JobContext;
import com.coremedia.rest.cap.jobs.JobExecutionException;
import com.coremedia.rest.cap.content.SearchParameterNames;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

import static java.lang.invoke.MethodHandles.lookup;

public class CSVExportJob implements Job {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  private final CSVExportAuthorization csvExportAuthorization;

  private String query;
  private int limit;
  private List<String> sortCriteria;
  private String folderUri;
  private Boolean includeSubFolders;
  private Set<String> contentTypeNames;
  private Boolean includeSubTypes;
  private List<String> filterQueries;
  private List<String> facetFieldCriteria;
  private List<String> facetQueries;
  private String searchHandler;
  private String template;

  public CSVExportJob(CSVExportAuthorization csvExportAuthorization) {
    this.csvExportAuthorization = csvExportAuthorization;
  }

  @Nullable
  @Override
  public Object call(@NonNull JobContext jobContext) throws JobExecutionException {
    // check authorization
    if (!csvExportAuthorization.isAuthorized())
      throw new JobExecutionException(CSVExportJobErrorCode.USER_NOT_AUTHORIZED);

    // check parameters provided
    if (template == null || template.isEmpty())
      throw new JobExecutionException(CSVExportJobErrorCode.PARAM_TEMPLATE_MISSING);


    float progress = 0.0f;
    while (progress < 1.0f) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (jobContext.isAbortRequested()) {
        LOG.info("Job has been aborted - rename doc?");
        return null;
      }
      progress = progress + 0.01f;
      jobContext.notifyProgress(progress);
    }
    return null;
  }

  @JsonProperty(SearchParameterNames.QUERY)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setQuery(String query) {
    this.query = query;
  }

  @JsonProperty(SearchParameterNames.LIMIT)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setLimit(int limit) {
    this.limit = limit;
  }

  @SuppressWarnings("unused") //used by JobsFramework
  @JsonProperty(SearchParameterNames.ORDER_BY)
  public void setSortCriteria(List<String> sortCriteria) {
    this.sortCriteria = sortCriteria;
  }

  @JsonProperty(SearchParameterNames.FOLDER)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setFolderUri(String folderUri) {
    this.folderUri = folderUri;
  }

  @JsonProperty(SearchParameterNames.INCLUDE_SUB_FOLDERS)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setIncludeSubFolders(Boolean includeSubFolders) {
    this.includeSubFolders = includeSubFolders;
  }

  @JsonProperty(SearchParameterNames.CONTENT_TYPE)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setContentTypeNames(Set<String> contentTypeNames) {
    this.contentTypeNames = contentTypeNames;
  }

  @JsonProperty(SearchParameterNames.INCLUDE_SUB_TYPES)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setIncludeSubTypes(Boolean includeSubTypes) {
    this.includeSubTypes = includeSubTypes;
  }

  @JsonProperty(SearchParameterNames.FILTER_QUERY)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setFilterQueries(List<String> filterQueries) {
    this.filterQueries = filterQueries;
  }

  @JsonProperty(SearchParameterNames.FACET_FIELD)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setFacetFieldCriteria(List<String> facetFieldCriteria) {
    this.facetFieldCriteria = facetFieldCriteria;
  }

  @JsonProperty(SearchParameterNames.FACET_QUERY)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setFacetQueries(List<String> facetQueries) {
    this.facetQueries = facetQueries;
  }

  @JsonProperty(SearchParameterNames.SEARCH_HANDLER)
  @SuppressWarnings("unused") //used by JobsFramework
  public void setSearchHandler(String searchHandler) {
    this.searchHandler = searchHandler;
  }

  @SuppressWarnings("unused") //used by JobsFramework
  public void setTemplate(String template) {
    this.template = template;
  }

}
