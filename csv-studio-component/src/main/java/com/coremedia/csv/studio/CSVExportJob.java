package com.coremedia.csv.studio;

import com.coremedia.cap.common.Blob;
import com.coremedia.cap.common.BlobService;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.user.User;
import com.coremedia.rest.cap.content.search.SearchServiceResult;
import com.coremedia.rest.cap.jobs.Job;
import com.coremedia.rest.cap.jobs.JobContext;
import com.coremedia.rest.cap.jobs.JobExecutionException;
import com.coremedia.rest.cap.content.SearchParameterNames;
import com.coremedia.xml.MarkupFactory;
import com.coremedia.xml.Markup;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.activation.MimeTypeParseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.lang.invoke.MethodHandles.lookup;

public class CSVExportJob implements Job {
  private static final String DIV_NS = "<div xmlns=\"http://www.coremedia.com/2003/richtext-1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">";
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  private final CSVExportAuthorization csvExportAuthorization;

  private final CSVExportSearchService csvExportSearchService;

  private final CSVFileRetriever csvFileRetriever;

  private final ContentRepository contentRepository;

  private String name;
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
  public Content call(@NonNull JobContext jobContext) throws JobExecutionException {
    // check authorization
    if (!csvExportAuthorization.isAuthorized())
      throw new JobExecutionException(CSVExportJobErrorCode.USER_NOT_AUTHORIZED);
    // check parameters provided
    if (template == null || template.isEmpty())
      throw new JobExecutionException(CSVExportJobErrorCode.PARAM_TEMPLATE_MISSING);
    User user = csvExportAuthorization.getCurrentUser();
    long start = System.currentTimeMillis();
    LOG.info("User {} started {}", user.getNameAtDomain(), this);
    // create CMDownload to store export
    Content exportContent = null;
    try {
      // perform search for content to export
      SearchServiceResult result = csvExportSearchService.search(query, limit, sortCriteria, folderUri, includeSubFolders,
              contentTypeNames, includeSubTypes, filterQueries, facetFieldCriteria, facetQueries, searchHandler);
      exportContent = processResult(result, jobContext);
      long duration = (System.currentTimeMillis() - start) / 1000;
      // also record some info in detailText
      StringBuilder detailText = new StringBuilder();
      detailText.append(DIV_NS);
      detailText.append("<p>" + result.getHits().size() + " content item(s) exported in " + duration + " seconds</p>");
      detailText.append("</div>");
      Markup info = MarkupFactory.fromString(detailText.toString());
      exportContent.set("detailText", info);
      LOG.info("CSV Report generation successfully completed for {} content items in {}s for {} ", result.getHits().size(), duration, this);
    } catch (IOException | MimeTypeParseException e) {
      LOG.error("Failed to retrieve CSV", e);
      throw new JobExecutionException(CSVExportJobErrorCode.RETRIEVAL_FAILED);
    } catch (Exception e) {
      LOG.error("Failed to generate CSV", e);
      throw new JobExecutionException(CSVExportJobErrorCode.GENERATION_FAILED);
    }
    // finish up
    contentRepository.getConnection().flush();
    exportContent.checkIn();
    jobContext.notifyProgress(1.0f);
    return exportContent;
  }

  private Content processResult(SearchServiceResult result, JobContext jobContext) throws IOException, MimeTypeParseException {
    InputStream is = csvFileRetriever.getInputStream(template, result.getHits(), jobContext);
    BlobService blobService = contentRepository.getConnection().getBlobService();
    Blob data = blobService.fromInputStream(is, "text/csv");
    // create CMDownload with data
    User user = csvExportAuthorization.getCurrentUser();
    Content homeFolder = user.getHomeFolder();
    String exportContentName = name.replace('/', '-');
    Map<String, Object> properties = new HashMap<>();
    properties.put("title", name);
    Content exportContent = contentRepository.createChild(homeFolder, exportContentName, "CMDownload", properties);
    exportContent.set("data", data);
    return exportContent;
  }

  @SuppressWarnings("unused") //used by JobsFramework
  public void setName(String name) {
    this.name = name;
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
            "name='" + name + '\'' +
            ", query='" + query + '\'' +
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
