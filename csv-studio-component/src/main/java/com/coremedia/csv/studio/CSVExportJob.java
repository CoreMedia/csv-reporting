package com.coremedia.csv.studio;

import com.coremedia.cap.common.Blob;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.user.User;
import com.coremedia.rest.cap.content.search.SearchServiceResult;
import com.coremedia.rest.cap.jobs.Job;
import com.coremedia.rest.cap.jobs.JobContext;
import com.coremedia.rest.cap.jobs.JobExecutionException;
import com.coremedia.rest.cap.content.SearchParameterNames;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.activation.MimeTypeParseException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodHandles.lookup;

public class CSVExportJob implements Job {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  private final CSVExportAuthorization csvExportAuthorization;

  private final CSVExportSearchService csvExportSearchService;

  private final CSVFileRetriever csvFileRetriever;

  private final ContentRepository contentRepository;

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

  public CSVExportJob(CSVExportAuthorization csvExportAuthorization,
                      CSVExportSearchService csvExportSearchService,
                      CSVFileRetriever csvFileRetriever,
                      ContentRepository contentRepository) {
    this.csvExportAuthorization = csvExportAuthorization;
    this.csvExportSearchService = csvExportSearchService;
    this.csvFileRetriever = csvFileRetriever;
    this.contentRepository = contentRepository;
  }

  @Nullable
  @Override
  // TODO: refactor, getting kind of lengthy...
  public Object call(@NonNull JobContext jobContext) throws JobExecutionException {
    // check authorization
    if (!csvExportAuthorization.isAuthorized())
      throw new JobExecutionException(CSVExportJobErrorCode.USER_NOT_AUTHORIZED);
    // check parameters provided
    if (template == null || template.isEmpty())
      throw new JobExecutionException(CSVExportJobErrorCode.PARAM_TEMPLATE_MISSING);
    User user = csvExportAuthorization.getCurrentUser();
    LOG.info("User {} started {}", user.getNameAtDomain(), this);
    // create CMDownload to store export
    Content exportContent = null;
    try {
      // perform search for content to export
      SearchServiceResult result = csvExportSearchService.search(query, limit, sortCriteria, folderUri, includeSubFolders,
              contentTypeNames, includeSubTypes, filterQueries, facetFieldCriteria, facetQueries, searchHandler);
      CSVFileResponse csvFileResponse = csvFileRetriever.retrieveCSV(template, result.getHits());
      int status = csvFileResponse.getStatus();
      if (status < 300) {
        Content homeFolder = user.getHomeFolder();
        // TODO: extract name generation (with default) to separate method
        String contentDispositionHeader = csvFileResponse.getContentDispositionHeaderValue();
        Pattern pattern = Pattern.compile("attachment; filename=\"(.*)\"");
        Matcher matcher = pattern.matcher(contentDispositionHeader);
        String exportContentName = null;
        if(matcher.matches()) {
          exportContentName = matcher.group(1);
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("title", exportContentName);
        exportContent = contentRepository.createChild(homeFolder, exportContentName, "CMDownload", properties);
        // TODO: some kind of batching (with job progress report) would be nice...
        // TODO: also support abort...
        byte[] data = csvFileResponse.getData();
        Blob blob = contentRepository.getConnection().getBlobService().fromBytes(data, "text/csv");
        exportContent.set("data", blob);
      }
    } catch (IOException | MimeTypeParseException e) {
      LOG.error("Failed to retrieve CSV", e);
      throw new JobExecutionException(CSVExportJobErrorCode.RETRIEVAL_FAILED);
    } catch (Exception e) {
      LOG.error("Failed to generate CSV", e);
      throw new JobExecutionException(CSVExportJobErrorCode.GENERATION_FAILED);
    }
    // finish up
    if (exportContent != null) {
      contentRepository.getConnection().flush();
      exportContent.checkIn();
      jobContext.notifyProgress(1.0f);
    }
    // TODO: can the Job result be shown in Studio for the finished Job?
    return exportContent;
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

  @Override
  public String toString() {
    return "CSVExportJob{" +
            "query='" + query + '\'' +
            ", limit=" + limit +
            ", sortCriteria=" + sortCriteria +
            ", folderUri='" + folderUri + '\'' +
            ", includeSubFolders=" + includeSubFolders +
            ", contentTypeNames=" + contentTypeNames +
            ", includeSubTypes=" + includeSubTypes +
            ", filterQueries=" + filterQueries +
            ", facetFieldCriteria=" + facetFieldCriteria +
            ", facetQueries=" + facetQueries +
            ", searchHandler='" + searchHandler + '\'' +
            ", template='" + template + '\'' +
            '}';
  }
}
