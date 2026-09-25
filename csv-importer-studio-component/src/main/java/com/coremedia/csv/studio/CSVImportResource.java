package com.coremedia.csv.studio;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.user.Group;
import com.coremedia.cap.user.User;
import com.coremedia.cap.user.UserRepository;
import com.coremedia.csv.common.CSVConfig;
import com.coremedia.csv.importer.CSVParserHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Handles Studio API requests to import content from a CSV file.
 */
@RequestMapping
@RestController
public class CSVImportResource {

  /**
   * The content repository from which to retrieve content.
   */
  private final ContentRepository contentRepository;

  /**
   * Configuration mapping CSV headers to content properties
   */
  private final CSVConfig csvConfig;

  /**
   * Flag indicating whether access to this endpoint should be restricted to authorized groups only
   */
  private final boolean restrictToAuthorizedGroups;

  /**
   * The groups that are authorized to access this endpoint.
   */
  private final List<String> authorizedGroups;

  /**
   * Import process logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(CSVImportResource.class);

  /**
   * Creates the CSV import resource with its repository, mapping, and authorization configuration.
   *
   * @param contentRepository repository used to update content
   * @param csvConfig configuration mapping CSV headers to content properties
   * @param restrictToAuthorizedGroups whether access is restricted to authorized groups
   * @param authorizedGroups groups allowed to access the import endpoint
   */
  public CSVImportResource(ContentRepository contentRepository,
                           CSVConfig csvConfig,
                           boolean restrictToAuthorizedGroups,
                           List<String> authorizedGroups) {
    this.contentRepository = contentRepository;
    this.csvConfig = csvConfig;
    this.restrictToAuthorizedGroups = restrictToAuthorizedGroups;
    this.authorizedGroups = List.copyOf(authorizedGroups);
  }

  @PostMapping(value = "importcsv/uploadfile",
          produces = "text/json",
          consumes = "multipart/form-data")
  public ResponseEntity importCSV(@RequestHeader(value = "site", required = false) String siteId,
                                  @RequestHeader(value = "folderUri", required = false) String folderUri,
                                  @RequestParam("file") MultipartFile file) throws IOException {

    // Check that the user is a member of the requisite group
    if (restrictToAuthorizedGroups && !isAuthorized()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User does not have authorized access");
    }

    boolean autoPublish = false;
    String template = "default";
    BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
    CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL.withHeader());
    CSVParserHelper handler = new CSVParserHelper(autoPublish, contentRepository, logger);
    handler.parseCSV(parser, csvConfig.getReportHeadersToContentProperties(template));

    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(handler.getFirstContent());
  }

  private boolean isAuthorized() {
    if (this.authorizedGroups == null || this.authorizedGroups.isEmpty())
      return false;

    User user = contentRepository.getConnection().getSession().getUser();
    UserRepository userRepository = contentRepository.getConnection().getUserRepository();
    for (String authorizedGroupName : authorizedGroups) {
      Group group = userRepository.getGroupByName(authorizedGroupName);
      if (group != null && user.isMemberOf(group)) {
        return true;
      }
    }
    return false;
  }
}
