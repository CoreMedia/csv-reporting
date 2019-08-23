package com.coremedia.csv.studio;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.user.Group;
import com.coremedia.cap.user.User;
import com.coremedia.cap.user.UserRepository;
import com.coremedia.cotopaxi.content.ContentImpl;
import com.coremedia.csv.common.CSVConfig;
import com.coremedia.csv.importer.CSVParserHelper;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Handles Studio API requests for a CSV based on search parameters.
 */
@Path("importcsv/uploadfile")
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

  @POST
  @Consumes({"multipart/form-data"})
  public Response importCSV(@HeaderParam("site") String siteId,
                            @HeaderParam("folderUri") String folderUri,
                            @FormDataParam("contentName") String contentName,
                            @FormDataParam("file") InputStream inputStream,
                            @FormDataParam("file") FormDataContentDisposition fileDetail,
                            @FormDataParam("file") FormDataBodyPart fileBodyPart) throws IOException {
  // Check that the user is a member of the requisite group
    if (restrictToAuthorizedGroups && !isAuthorized()) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    boolean autoPublish = false;
    String template = "default";
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL.withHeader());
    CSVParserHelper handler = new CSVParserHelper(autoPublish, contentRepository, logger);
    handler.parseCSV(parser, csvConfig.getReportHeadersToContentProperties(template));


    return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(handler.getFirstContent()).build();
  }

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
}
