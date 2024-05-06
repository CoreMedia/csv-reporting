package com.coremedia.csv.cae.utils;

import com.coremedia.cae.security.CaeCsrfIgnoringRequestMatcher;

import javax.servlet.http.HttpServletRequest;

public class CSVCaeCsrfIgnoringRequestMatcher implements CaeCsrfIgnoringRequestMatcher {
  @Override
  public boolean matches(HttpServletRequest httpServletRequest) {
    String requestUri = httpServletRequest.getRequestURI();
    return requestUri != null && requestUri.contains("/contentsetexport");
  }

}
