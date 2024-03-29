package com.coremedia.csv.studio;

import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.csv.common.CSVConstants;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/**
 * Retrieves a CSV file from the preview CAE
 */
public class CSVFileRetriever {

  /**
   * The URL to use for calls to the preview CAE from the studio REST API.
   */
  private final String previewRestUrlPrefix;

  public CSVFileRetriever(String previewRestUrlPrefix) {
    this.previewRestUrlPrefix = previewRestUrlPrefix;
  }

  /**
   * Queries the CSV export endpoint on the preview CAE and returns a CSV file.
   *
   * @param contents The content items to include in the CSV
   * @return A CSVFileResponse containing the bytes and name information of the returned file
   * @throws IOException Thrown when a request to the CAE fails
   */
  public CSVFileResponse retrieveCSV(String csvTemplate, List<Content> contents) throws IOException {
    // Create a comma-separated list of content IDs for the request body
    StringBuilder contentIdsList = new StringBuilder();
    contentIdsList.append('[');
    Iterator<Content> contentsIterator = contents.iterator();
    while(contentsIterator.hasNext()) {
      String contentId = contentsIterator.next().getId();
      // Use the numeric ID
      int parsed = IdHelper.parseContentId(contentId);
      contentIdsList.append(parsed);
      if(contentsIterator.hasNext())
        contentIdsList.append(",");
    }
    contentIdsList.append(']');

    // Set up a POST request to the content set export endpoint
    CloseableHttpClient client = HttpClients.createDefault();
    String requestUrl = previewRestUrlPrefix + "/contentsetexport/"+ URLEncoder.encode(csvTemplate, StandardCharsets.UTF_8);
    HttpPost httpPost = new HttpPost(requestUrl);
    httpPost.setHeader("Content-Type", "application/json");
    HttpEntity requestEntity = new StringEntity(contentIdsList.toString());
    httpPost.setEntity(requestEntity);

    CloseableHttpResponse response = null;
    try {
      // Execute request and extract info from response
      response = client.execute(httpPost);
      Header contentDispositionHeader = response.getFirstHeader(CSVConstants.HTTP_HEADER_CONTENT_DISPOSITION);
      String headerValue = contentDispositionHeader == null ? null : contentDispositionHeader.getValue();
      HttpEntity responseEntity = response.getEntity();
      byte[] file = responseEntity == null ? null : IOUtils.toByteArray(responseEntity.getContent());
      return new CSVFileResponse(file, response.getStatusLine().getStatusCode(), headerValue);
    } finally {
      if (response != null) {
        response.close();
      }
      client.close();
    }
  }

}
