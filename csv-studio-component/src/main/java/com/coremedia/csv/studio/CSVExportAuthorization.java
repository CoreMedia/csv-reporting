package com.coremedia.csv.studio;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.user.Group;
import com.coremedia.cap.user.User;
import com.coremedia.cap.user.UserRepository;

import java.util.List;

public class CSVExportAuthorization {
  private final ContentRepository contentRepository;
  /**
   * Flag indicating whether access to this endpoint should be restricted to authorized groups only
   */
  private final boolean restrictToAuthorizedGroups;

  /**
   * The groups that are authorized to access this endpoint.
   */
  private final List<String> authorizedGroups;

  public CSVExportAuthorization(ContentRepository contentRepository, boolean restrictToAuthorizedGroups, List<String> authorizedGroups) {
    this.contentRepository = contentRepository;
    this.restrictToAuthorizedGroups = restrictToAuthorizedGroups;
    this.authorizedGroups = authorizedGroups;
  }

  /**
   * Checks whether the current user is authorized to initiate a CSV export.
   *
   * @return whether the current user is authorized to initiate a CSV export
   */
  public boolean isAuthorized() {
    if(!restrictToAuthorizedGroups)
      return true;

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
