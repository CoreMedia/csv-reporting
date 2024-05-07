package com.coremedia.csv.studio;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
import com.coremedia.rest.cap.content.search.CapObjectFormat;
import com.coremedia.rest.cap.content.search.QueryUriResolver;
import com.coremedia.rest.cap.content.search.SearchService;
import com.coremedia.rest.cap.content.search.SearchServiceResult;
import com.coremedia.rest.exception.BadRequestException;
import com.coremedia.rest.linking.LinkResolver;
import com.coremedia.rest.linking.LinkResolverUtil;

import java.util.*;

public class CSVExportSearchService {
  /**
   * The content repository from which to retrieve content.
   */
  private final ContentRepository contentRepository;

  /**
   * The search service with which to search for content.
   */
  private final SearchService searchService;

  /**
   * The formatter for resolving URIs.
   */
  private final CapObjectFormat capObjectFormat;

  /**
   * Resolves URI's to Domain Objects.
   */
  private final LinkResolver linkResolver;

  private final int defaultItemLimit;

  public CSVExportSearchService(ContentRepository contentRepository,
                                SearchService searchService,
                                CapObjectFormat capObjectFormat,
                                LinkResolver linkResolver,
                                int defaultItemLimit) {
    this.contentRepository = contentRepository;
    this.searchService = searchService;
    this.capObjectFormat = capObjectFormat;
    this.linkResolver = linkResolver;
    this.defaultItemLimit = defaultItemLimit;
  }

  public SearchServiceResult search(String query, int limit, List<String> sortCriteria, String folderUri, Boolean includeSubFolders, Set<String> contentTypeNames, Boolean includeSubTypes, List<String> filterQueries, List<String> facetFieldCriteria, List<String> facetQueries, String searchHandler) {
    // COPIED FROM ContentRepositoryResource.java
    final Collection<ContentType> contentTypes = getContentTypes(contentTypeNames);
    final Content folderFilter = getFolder(folderUri);
    final QueryUriResolver uriResolver = new QueryUriResolver(linkResolver, capObjectFormat);
    final List<String> resolvedFilterQueries = uriResolver.resolveUris(filterQueries);
    // final List<String> resolvedFacetQueries = uriResolver.resolveUris(facetQueries);
    final List<String> resolvedSortCriteria = uriResolver.resolveUris(sortCriteria);

    boolean includeSubFoldersValue = includeSubFolders == null ? true : includeSubFolders;
    boolean includeSubTypesValue = includeSubTypes == null ? true : includeSubTypes;

    if (limit == -1 && defaultItemLimit != -1)
      limit = defaultItemLimit;

    // perform search, no need to pass in facetFieldCriteria/resolvedFacetQueries
    return searchService.search(query, limit, resolvedSortCriteria, folderFilter, includeSubFoldersValue,
            contentTypes, includeSubTypesValue, resolvedFilterQueries, null, null, searchHandler);

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
