package com.coremedia.csv.studio;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.user.Group;
import com.coremedia.cap.user.User;
import com.coremedia.cap.user.UserRepository;
import com.coremedia.csv.importer.CSVUploader;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.InputStream;
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
   * Flag indicating whether access to this endpoint should be restricted to authorized groups only
   */
  private boolean restrictToAuthorizedGroups;

  /**
   * The groups that are authorized to access this endpoint.
   */
  private List<String> authorizedGroups;

  /**
   * Sets the content repository.
   *
   * @param contentRepository the content repository to set
   */
  public void setContentRepository(ContentRepository contentRepository) {
    this.contentRepository = contentRepository;
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
  @Path("create")
  @Consumes({"multipart/form-data"})
  public Response importCSV(@HeaderParam("site") String siteId, @HeaderParam("folderUri") String folderUri, @FormDataParam("contentName") String contentName, @FormDataParam("file") InputStream inputStream, @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("file") FormDataBodyPart fileBodyPart) {
  // Check that the user is a member of the requisite group
    if (restrictToAuthorizedGroups && !isAuthorized()) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    CSVUploader csvuploader = new CSVUploader();
    csvuploader.runFromRequest(inputStream);
    return Response.status(Response.Status.OK).build();
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
