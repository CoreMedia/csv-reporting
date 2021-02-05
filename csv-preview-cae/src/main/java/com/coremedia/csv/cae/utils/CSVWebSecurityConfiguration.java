package com.coremedia.csv.cae.utils;

import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * There are Spring 403 issues that crop up when doing post requests with Spring Enabled Web Security. These revolve
 * around CSRF tokens to validate against cross-site vulnerabilities. For the CSV Reporter, which is being called via
 * the Studio into the CAE, we need to disable CSRF for now. A bit of a workaround but this will do the job for now.
 */
@EnableWebSecurity
@Order(1)
public class CSVWebSecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable();
    http.headers().disable();
    http.cors().configurationSource(corsConfigurationSource());
  }

  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.applyPermitDefaultValues();
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
