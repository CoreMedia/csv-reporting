package com.coremedia.csv.studio;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.rest.cap.CapRestServiceBaseConfiguration;
import com.coremedia.rest.cap.CapRestServiceSearchConfiguration;
import com.coremedia.rest.cap.content.search.CapObjectFormat;
import com.coremedia.rest.cap.content.search.SearchService;
import com.coremedia.rest.linking.LinkResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Configuration
@Import({
        CapRestServiceBaseConfiguration.class,
        CapRestServiceSearchConfiguration.class
})
@EnableConfigurationProperties({
        CSVConfigurationProperties.class
})
class CSVConfiguration {

  @Bean
  public CSVExportResource csvExportResource(CSVFileRetriever csvFileRetriever, ContentRepository contentRepository, SearchService searchService, CapObjectFormat capObjectFormat, LinkResolver linkResolver) {
    List<String> authorizedGroups = List.of("csv-reporter", "csv-reporter@cognito");
    return new CSVExportResource(csvFileRetriever, contentRepository, searchService, capObjectFormat, true, authorizedGroups, linkResolver);
  }

  @Bean
  public CSVFileRetriever csvFileRetriever(CSVConfigurationProperties csvConfigurationProperties) {
    return new CSVFileRetriever(csvConfigurationProperties.getPreviewRestUrlPrefix());
  }

  @Bean
  public CSVExportJobFactory csvExportJobFactory() {
    return new CSVExportJobFactory();
  }
}
