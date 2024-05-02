package com.coremedia.csv.studio;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
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
   * Determines whether export is allowed for current user.
   */
  private final CSVExportAuthorization csvExportAuthorization;

  /**
   * Wrapper for SearchService with some extra handling for input parameters.
   */
  private final CSVExportSearchService csvExportSearchService;

  /**
   * Sends a request for a CSV file to the preview CAE.
   */
  private final CSVFileRetriever csvFileRetriever;

  public CSVExportResource(CSVExportAuthorization csvExportAuthorization, CSVExportSearchService csvExportSearchService, CSVFileRetriever csvFileRetriever) {
    this.csvExportAuthorization = csvExportAuthorization;
    this.csvExportSearchService = csvExportSearchService;
    this.csvFileRetriever = csvFileRetriever;
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
    if(!csvExportAuthorization.isAuthorized()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Query SearchService with the provided parameters
    SearchServiceResult result = csvExportSearchService.search(query, limit, sortCriteria, folderUri, includeSubFolders,
            contentTypeNames, includeSubTypes, filterQueries, facetFieldCriteria, facetQueries, searchHandler);

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

}
