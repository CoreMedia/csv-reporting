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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Handles Studio API requests for a CSV based on search parameters.
 */
@RequestMapping
@RestController
public class CSVImportResource {

  /**
   * The content repository from which to retrieve content.
   */
  private ContentRepository contentRepository;

  /**
   * Configuration mapping CSV headers to content properties
   */
  private CSVConfig csvConfig;

  /**
   * Flag indicating whether access to this endpoint should be restricted to authorized groups only
   */
  private boolean restrictToAuthorizedGroups;

  /**
   * The groups that are authorized to access this endpoint.
   */
  private List<String> authorizedGroups;

  /**
   * Import process logger.
   */
  private static Logger logger = LoggerFactory.getLogger(CSVImportResource.class);

  /**
   * Sets the content repository.
   *
   * @param contentRepository the content repository to set
   */
  public void setContentRepository(ContentRepository contentRepository) {
    this.contentRepository = contentRepository;
  }

  /**
   * Sets the CSV configuration
   *
   * @param csvConfig the csv configuration to set
   */
  public void setCsvConfig(CSVConfig csvConfig) {
    this.csvConfig = csvConfig;
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

  @PostMapping(value = "importcsv/uploadfile",
          produces = "text/json",
          consumes = "multipart/form-data")
  public ResponseEntity importCSV(@HeaderParam("site") String siteId,
                                  @HeaderParam("folderUri") String folderUri,
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
