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
import com.coremedia.rest.linking.LinkResolver;
import com.coremedia.rest.linking.LinkResolverUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

/**
 * Handles Studio API requests for a CSV based on search parameters.
 */
@RequestMapping
@RestController
public class CSVExportResource {

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
   * Resolves URI's to Domain Objects.
   */
  @Autowired
  private LinkResolver linkResolver;

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
  @GetMapping(value="exportcsv/contentset", produces="text/csv")
  public ResponseEntity exportCSV(@RequestParam(value = SearchParameterNames.QUERY, required = false) final String query,
                                       @RequestParam(value = SearchParameterNames.LIMIT, required = false) final int limit,
                                       @RequestParam(value = SearchParameterNames.ORDER_BY, required = false) final List<String> sortCriteria,
                                       @RequestParam(value = SearchParameterNames.FOLDER, required = false) final String folderUri,
                                       @RequestParam(value = SearchParameterNames.INCLUDE_SUB_FOLDERS, required = false) final Boolean includeSubFolders,
                                       @RequestParam(value = SearchParameterNames.CONTENT_TYPE, required = false) final Set<String> contentTypeNames,
                                       @RequestParam(value = SearchParameterNames.INCLUDE_SUB_TYPES, required = false) final Boolean includeSubTypes,
                                       @RequestParam(value = SearchParameterNames.FILTER_QUERY, required = false) final List<String> filterQueries,
                                       @RequestParam(value = SearchParameterNames.FACET_FIELD, required = false) final List<String> facetFieldCriteria,
                                       @RequestParam(value = SearchParameterNames.FACET_QUERY, required = false) final List<String> facetQueries,
                                       @RequestParam(value = SearchParameterNames.SEARCH_HANDLER, required = false) String searchHandler,
                                       @RequestParam(value = TEMPLATE_PARAMETER, required = false) String csvTemplate)
          throws BadRequestException, IOException {

    // Verify that the template has been set, we do this here rather than in the RequestParam so that we can give a
    // better message than just a generic 400
    if (csvTemplate == null || csvTemplate.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No CSV Template Parameter defined.");
    }

    // Check that the user is a member of the requisite group
    if(restrictToAuthorizedGroups && !isAuthorized()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Resolve parameters from request
    // COPIED FROM ContentRepositoryResource.java
    final Collection<ContentType> contentTypes = getContentTypes(contentTypeNames);
    final Content folderFilter = getFolder(folderUri);
    final QueryUriResolver uriResolver = new QueryUriResolver(linkResolver, capObjectFormat);
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
      if (csvFileResponse.getContentDispositionHeaderValue() != null) {
        return ResponseEntity.ok()
                .header(CSVConstants.HTTP_HEADER_CONTENT_DISPOSITION, csvFileResponse.getContentDispositionHeaderValue())
                .contentType(MediaType.valueOf(CSVConstants.CSV_MEDIA_TYPE))
                .body(csvFileResponse.getData());
      }
      return ResponseEntity.ok()
              .contentType(MediaType.valueOf(CSVConstants.CSV_MEDIA_TYPE))
              .body(csvFileResponse.getData());
    }
    return ResponseEntity.status(csvFileResponse.getStatus())
            .contentType(MediaType.valueOf(CSVConstants.CSV_MEDIA_TYPE))
            .body(csvFileResponse.getData());
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
    final Content folderFilter = (Content) LinkResolverUtil.resolveLink(folderUri, linkResolver);
    if (!folderFilter.isFolder()) {
      throw new BadRequestException("invalid folderUri uri: " + folderUri);
    }
    return folderFilter;
  }


}
