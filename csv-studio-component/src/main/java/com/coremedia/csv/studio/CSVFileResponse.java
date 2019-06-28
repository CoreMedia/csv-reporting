package com.coremedia.csv.studio;

/**
 * Captures data from the response from a request to an export endpoint.
 */
public class CSVFileResponse {

  /**
   * The value of the response's Content-Disposition header, which captures the file name.
   */
  private String contentDispositionHeaderValue;

  /**
   * The response body.
   */
  private byte[] data;

  /**
   * The status code of the HTTP response.
   */
  private int status;

  /**
   * Constructor.
   *
   * @param data   The response body
   * @param status The status code of the HTTP response
   * @param header The Content-Disposition header value
   */
  public CSVFileResponse(byte[] data, int status, String header) {
    this.contentDispositionHeaderValue = header;
    this.status = status;
    this.data = data;
  }

  /**
   * Get the Content-Disposition header value.
   *
   * @return The Content-Disposition header value
   */
  public String getContentDispositionHeaderValue() {
    return contentDispositionHeaderValue;
  }

  /**
   * Get the response body.
   *
   * @return The response body
   */
  public byte[] getData() {
    return data;
  }

  /**
   * Get the response status.
   *
   * @return The response status
   */
  public int getStatus() {
    return status;
  }

}
