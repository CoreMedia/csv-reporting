package com.coremedia.csv.studio;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.csv.common.CSVConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class CSVImportStudioAutoConfiguration {

  @Bean
  CSVImportResource csvImportResource(ContentRepository contentRepository, CSVConfig csvConfig) {
    return new CSVImportResource(contentRepository, csvConfig, true, List.of("csv-importer", "csv-importer@cognito"));
  }
}
