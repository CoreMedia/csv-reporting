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
  public CSVExportAuthorization getCSVExportAuthorization(ContentRepository contentRepository) {
    List<String> authorizedGroups = List.of("csv-reporter", "csv-reporter@cognito");
    return new CSVExportAuthorization(contentRepository, true, authorizedGroups);
  }

  @Bean
  public CSVExportResource csvExportResource(CSVExportAuthorization csvExportAuthorization, CSVFileRetriever csvFileRetriever, ContentRepository contentRepository, SearchService searchService, CapObjectFormat capObjectFormat, LinkResolver linkResolver) {
    return new CSVExportResource(csvExportAuthorization, csvFileRetriever, contentRepository, searchService, capObjectFormat, linkResolver);
  }

  @Bean
  public CSVFileRetriever csvFileRetriever(CSVConfigurationProperties csvConfigurationProperties) {
    return new CSVFileRetriever(csvConfigurationProperties.getPreviewRestUrlPrefix());
  }

  @Bean
  public CSVExportJobFactory csvExportJobFactory(CSVExportAuthorization csvExportAuthorization) {
    return new CSVExportJobFactory(csvExportAuthorization);
  }
}
