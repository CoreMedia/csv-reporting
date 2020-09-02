package com.coremedia.csv.cae.handlers;

import com.coremedia.objectserver.web.links.Link;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * Handles a request to export a content report in CSV format. The request must contain a list of content IDs.
 * Used by the CSV export in studio.
 */
@Link
@RequestMapping
public class ContentSetCSVHandler extends BaseCSVHandler {

  /**
   * The link pattern which this handler will activate upon.
   */
  private static final String CSV_LINK_PATTERN = "/contentsetexport/{template}";

  /**
   * Handles the incoming request. Parses the list of content IDs and passes the request/response info to the
   * utility class.
   *
   * @param contentIds A list of content IDs to include in the export
   * @param request  the HTTP Request, used for building content beans
   * @param response the HTTP Response, used for building content beans and writing CSV
   * @throws IOException if an error occurs writing the CSV
   */
  @PostMapping(value = CSV_LINK_PATTERN,
          produces = "text/csv",
          consumes = "application/json")
  @ResponseBody
  public void handleRequest(@PathVariable("template") String template,
                            @RequestBody int[] contentIds,
                            HttpServletRequest request,
                            HttpServletResponse response)
          throws IOException {
    String templateName = URLDecoder.decode(template, "UTF-8");
    CSVUtil.generateCSV(contentIds, templateName, request, response);

  }
}
