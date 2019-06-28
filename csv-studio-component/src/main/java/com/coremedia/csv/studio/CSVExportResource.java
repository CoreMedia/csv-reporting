package com.coremedia.csv.studio;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.user.Group;
import com.coremedia.cap.user.User;
import com.coremedia.cap.user.UserRepository;
import com.coremedia.csv.common.CSVConstants;
import com.coremedia.rest.cap.content.SearchParameterNames;
import com.coremedia.rest.cap.content.search.CapObjectFormat;
import com.coremedia.rest.cap.content.search.QueryUriResolver;
import com.coremedia.rest.cap.content.search.SearchService;
import com.coremedia.rest.cap.content.search.SearchServiceResult;
import com.coremedia.rest.exception.BadRequestException;
import com.coremedia.rest.linking.AbstractLinkingResource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import java.util.*;

/**
 * Handles Studio API requests for a CSV based on search parameters.
 */
@Path("exportcsv/contentset")
public class CSVExportResource extends AbstractLinkingResource {

  public static final String TEMPLATE_PARAMETER = "template";

  /**
   * Sends a request for a CSV file to the preview CAE.
   */
  private CSVFileRetriever csvFileRetriever;

  /**
   * The content repository from which to retrieve content.
   */
  private ContentRepository contentRepository;

  /**
   * The search service with which to search for content.
   */
  private SearchService searchService;

  /**
   * The formatter for resolving URIs.
   */
  private CapObjectFormat capObjectFormat;

  /**
   * Flag indicating whether access to this endpoint should be restricted to authorized groups only
   */
  private boolean restrictToAuthorizedGroups;

  /**
   * The groups that are authorized to access this endpoint.
   */
  private List<String> authorizedGroups;

  /**
   * Sets the CSV file retriever.
   *
   * @param csvFileRetriever the content repository to set
   */
  public void setCsvFileRetriever(CSVFileRetriever csvFileRetriever) {
    this.csvFileRetriever = csvFileRetriever;
  }

  /**
   * Sets the content repository.
   *
   * @param contentRepository the content repository to set
   */
  public void setContentRepository(ContentRepository contentRepository) {
    this.contentRepository = contentRepository;
  }

  /**
   * Sets the search service.
   *
   * @param searchService the search service to set
   */
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * Sets the CapObjectFormat.
   *
   * @param capObjectFormat the content repository to set
   */
  public void setCapObjectFormat(CapObjectFormat capObjectFormat) {
    this.capObjectFormat = capObjectFormat;
  }

  /**
   * Set the flag indicating whether access to this endpoint should be restricted to authorized groups only.
   *
   * @param restrictToAuthorizedGroups the value to set
   */
  public void setRestrictToAuthorizedGroups(boolean restrictToAuthorizedGroups) {
    this.restrictToAuthorizedGroups = restrictToAuthorizedGroups;
  }

  /**
   * Sets the authorized groups.
   *
   * @param authorizedGroups the authorized groups to set
   */
  public void setAuthorizedGroups(List<String> authorizedGroups) {
    this.authorizedGroups = authorizedGroups;
  }

  /**
   * CSV Export endpoint: parameters are re-used from the /search API endpoint.
   */
  @GET
  public Response exportCSV(@QueryParam(value = SearchParameterNames.QUERY) final String query,
                            @QueryParam(value = SearchParameterNames.LIMIT) final int limit,
                            @QueryParam(value = SearchParameterNames.ORDER_BY) final List<String> sortCriteria,
                            @QueryParam(value = SearchParameterNames.FOLDER) final String folderUri,
                            @QueryParam(value = SearchParameterNames.INCLUDE_SUB_FOLDERS) final Boolean includeSubFolders,
                            @QueryParam(value = SearchParameterNames.CONTENT_TYPE) final Set<String> contentTypeNames,
                            @QueryParam(value = SearchParameterNames.INCLUDE_SUB_TYPES) final Boolean includeSubTypes,
                            @QueryParam(value = SearchParameterNames.FILTER_QUERY) final List<String> filterQueries,
                            @QueryParam(value = SearchParameterNames.FACET_FIELD) final List<String> facetFieldCriteria,
                            @QueryParam(value = SearchParameterNames.FACET_QUERY) final List<String> facetQueries,
                            @QueryParam(value = SearchParameterNames.SEARCH_HANDLER) String searchHandler,
                            @QueryParam(value = TEMPLATE_PARAMETER) String csvTemplate)
          throws BadRequestException, IOException {
    // Check that the user is a member of the requisite group
    if(restrictToAuthorizedGroups && !isAuthorized()) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    // Resolve parameters from request
    // COPIED FROM ContentRepositoryResource.java
    final Collection<ContentType> contentTypes = getContentTypes(contentTypeNames);
    final Content folderFilter = getFolder(folderUri);
    final QueryUriResolver uriResolver = new QueryUriResolver(this, capObjectFormat);
    final List<String> resolvedFilterQueries = uriResolver.resolveUris(filterQueries);
    final List<String> resolvedFacetQueries = uriResolver.resolveUris(facetQueries);
    final List<String> resolvedSortCriteria = uriResolver.resolveUris(sortCriteria);

    boolean includeSubFoldersValue = includeSubFolders == null ? true : includeSubFolders;
    boolean includeSubTypesValue = includeSubTypes == null ? true : includeSubTypes;

    // Query solr with the provided parameters
    SearchServiceResult result = searchService.search(query, limit, resolvedSortCriteria, folderFilter, includeSubFoldersValue,
            contentTypes, includeSubTypesValue, resolvedFilterQueries, facetFieldCriteria, resolvedFacetQueries, searchHandler);

    // Use the CSVFileRetriever to request the file data from the CAE
    CSVFileResponse csvFileResponse = csvFileRetriever.retrieveCSV(csvTemplate, result.getHits());

    // Build response, re-using Content-Disposition header value with file name
    if(csvFileResponse.getStatus() < 300) {
      Response.ResponseBuilder responseBuilder = Response.ok(csvFileResponse.getData(), MediaType.valueOf(CSVConstants.CSV_MEDIA_TYPE));
      responseBuilder.status(csvFileResponse.getStatus());
      if (csvFileResponse.getContentDispositionHeaderValue() != null) {
        responseBuilder.header(CSVConstants.HTTP_HEADER_CONTENT_DISPOSITION, csvFileResponse.getContentDispositionHeaderValue());
      }
      return responseBuilder.build();
    }
    return Response.status(csvFileResponse.getStatus()).entity(csvFileResponse.getData()).build();
  }

  /**
   * Checks whether the current user is authorized to initiate a CSV export.
   *
   * @return whether the current user is authorized to initiate a CSV export
   */
  private boolean isAuthorized() {
    if(this.authorizedGroups == null || this.authorizedGroups.isEmpty())
      return false;

    User user = contentRepository.getConnection().getSession().getUser();
    UserRepository userRepository = contentRepository.getConnection().getUserRepository();
    for(String authorizedGroupName : authorizedGroups) {
      Group group = userRepository.getGroupByName(authorizedGroupName);
      if(group != null && user.isMemberOf(group)) {
        return true;
      }
    }
    return false;
  }


  // ---------- COPIED FROM ContentRepositoryResource.java ----------

  /**
   * Returns {@link ContentType}s for the given content type names.
   *
   * @param contentTypeNames content type names
   * @return content types
   * @throws BadRequestException if at least one of the given content type names is invalid, i.e. no such content type exists
   */
  private Collection<ContentType> getContentTypes(final Set<String> contentTypeNames) {
    if (contentTypeNames == null) {
      return Collections.emptyList();
    }
    final Collection<ContentType> contentTypeFilter = new ArrayList<>();
    for (final String contentTypeName : contentTypeNames) {
      final ContentType contentType = contentRepository.getContentType(contentTypeName);
      if (contentType == null) {
        throw new BadRequestException("invalid content type: " + contentTypeName);
      }
      contentTypeFilter.add(contentType);
    }
    return contentTypeFilter;
  }

  // ---------- COPIED FROM ContentRepositoryResource.java ----------

  private Content getFolder(final String folderUri) {
    if (folderUri == null) {
      return null;
    }
    final Content folderFilter = (Content) resolveLink(folderUri);
    if (!folderFilter.isFolder()) {
      throw new BadRequestException("invalid folderUri uri: " + folderUri);
    }
    return folderFilter;
  }

}
