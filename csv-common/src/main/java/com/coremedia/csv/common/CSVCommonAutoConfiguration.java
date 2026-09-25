package com.coremedia.csv.common;

import com.coremedia.cap.content.ContentRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CSVCommonAutoConfiguration {

  @Bean
  CSVConfig csvConfig(ContentRepository contentRepository) {
    return new CSVConfig(contentRepository, CSVConfig.DEFAULT_SETTINGS_PATH);
  }
}
